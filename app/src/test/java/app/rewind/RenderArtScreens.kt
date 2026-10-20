package app.rewind

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.rewind.data.DemoSeed
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.art.Art
import app.rewind.ui.card.CardScreen
import app.rewind.ui.mystery.MysteryScreen
import app.rewind.ui.onboarding.WelcomeScreen
import app.rewind.ui.rent.ReceiptScreen
import app.rewind.ui.rent.ReturnScreen
import app.rewind.ui.store.StoreScreen
import app.rewind.ui.tape.TapeScreen
import app.rewind.ui.theme.RewindTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every main screen over the demo store, drawn to PNGs (run with `-Prender`), in both languages:
 * a look without a phone. The clock stands at the demo's evening, so the store is open.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() {
        Art.still = true
    }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<RewindApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        val dao = c.db.dao()
        Home(dao.tapes(), dao.rentals(), c.settings.now(), DemoSeed.base(), app.ru)
    }

    private fun shot(name: String, h: Home, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            RewindTheme { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(app.rewind.ui.theme.U.c.bg)) { content(h, Go(nav)) } }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** The most taken tape in the store. */
    private fun loved(h: Home) = h.tapes.maxBy { h.watched(it) }

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun store() = home().let { shot("1-store", it) { h, go -> StoreScreen(h, go) } }
    @Test fun tape() = home().let { h -> shot("2-tape", h) { hh, go -> TapeScreen(hh.tapes.first { it.year == 1979 }.id, hh, go) } }
    @Test fun giveBack() = home().let { h -> shot("3-return", h) { hh, go -> ReturnScreen(hh.out.first().id, hh, go) } }
    @Test fun receipt() = home().let { h -> shot("4-receipt", h) { hh, go -> ReceiptScreen(hh.out.last().id, hh, go) } }
    @Test fun wrapped() = home().let { shot("5-wrapped", it) { h, go -> MysteryScreen(h, go) } }
    @Test fun card() = home().let { h -> shot("6-card", h) { hh, go -> CardScreen(hh, go) } }
    @Test fun lovedTape() = home().let { h -> shot("7-loved", h) { hh, go -> TapeScreen(loved(hh).id, hh, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
