package app.rewind

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.rewind.data.DemoSeed
import app.rewind.data.SettingsStore
import app.rewind.data.Store
import app.rewind.data.StoreDatabase
import app.rewind.domain.Draft
import app.rewind.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val db = StoreDatabase.create(context)
    val settings = SettingsStore(context)
    val store = Store(context, db, settings)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Text shared into the app (a link, or a friend's card), waiting to be looked at. */
    val shared = MutableStateFlow<Pair<String, String?>?>(null)

    /** A tape on its way into the store, between a share, the catalogue search and the form. */
    val draft = MutableStateFlow<Draft?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class RewindApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { DemoSeed.seedIfEmpty(this@RewindApp, container) }
        Reminders.schedule(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as RewindApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
