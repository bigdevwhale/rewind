package app.rewind.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.rewind.container
import app.rewind.data.Kind
import app.rewind.data.Outcome
import app.rewind.data.Place
import app.rewind.data.Rental
import app.rewind.data.Settings
import app.rewind.data.Tape
import app.rewind.domain.Rules
import app.rewind.domain.Shares
import app.rewind.ru
import app.rewind.ui.add.AddScreen
import app.rewind.ui.add.SearchScreen
import app.rewind.ui.bin.BinScreen
import app.rewind.ui.card.CardScreen
import app.rewind.ui.catalog.CatalogScreen
import app.rewind.ui.components.rememberNow
import app.rewind.ui.mystery.MysteryScreen
import app.rewind.ui.onboarding.WelcomeScreen
import app.rewind.ui.rent.ReceiptScreen
import app.rewind.ui.rent.ReturnScreen
import app.rewind.ui.settings.SettingsScreen
import app.rewind.ui.store.StoreScreen
import app.rewind.ui.tape.TapeScreen
import app.rewind.ui.theme.U
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Where a tape is right now. */
enum class Status { SHELF, OUT, WINDOW, BIN }

/** Why a tape can't go home right now, if it can't. */
enum class Block { CLOSED, HANDS_FULL, IN_WINDOW, OUT, NONE }

/** Everything stored, plus what is derived from it, at one moment. Screens read through here. */
@Immutable
class Home(
    val tapes: List<Tape>,
    val rentals: List<Rental>,
    val settings: Settings,
    val now: Long,
    val ru: Boolean,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val clock: LocalDateTime = Instant.ofEpochMilli(now).atZone(zone).toLocalDateTime()

    private val byTape = rentals.groupBy { it.tapeId }
    private val byId = tapes.associateBy { it.id }

    fun tape(id: Long) = byId[id]
    fun rentals(id: Long) = byTape[id].orEmpty().sortedByDescending { it.outAt }
    fun rental(id: Long) = rentals.firstOrNull { it.id == id }

    /** At home now, the soonest due first. */
    val out: List<Rental> = rentals.filter { it.out }.sortedBy { it.dueAt }
    private val outIds = out.map { it.tapeId }.toSet()
    fun isOut(t: Tape) = t.id in outIds
    fun outRental(t: Tape) = out.firstOrNull { it.tapeId == t.id }

    private val store = tapes.filter { it.placeOf == Place.STORE }
    val window: List<Tape> = store.filter { it.readyAt > now }.sortedBy { it.readyAt }
    val shelf: List<Tape> = store.filter { it.readyAt <= now && !isOut(it) }
    val bin: List<Tape> = tapes.filter { it.placeOf == Place.BIN }.sortedByDescending { it.binAt }

    /** The aisles, each newest first, so what came lately is at hand. */
    fun aisle(k: Kind): List<Tape> = shelf.filter { it.kindOf == k && it.from.isBlank() }.sortedByDescending { it.readyAt }

    /** The store's hits: what got four or five stars, best first. */
    val hits: List<Tape> = shelf.filter { stars(it) >= 4 }.sortedWith(compareByDescending<Tape> { stars(it) }.thenByDescending { rated(it)?.backAt ?: 0 })

    /** Friends' picks, by friend, on the shelf or still in the window. */
    val picks: List<Pair<String, List<Tape>>> = shelf.filter { it.from.isNotBlank() }
        .groupBy { it.from.trim() }.toList().sortedByDescending { (_, l) -> l.maxOf { it.readyAt } }

    fun watched(t: Tape) = rentals(t.id).count { it.outcomeOf == Outcome.WATCHED }
    fun taken(t: Tape) = rentals(t.id).size

    /** The latest slip with stars on it: how it went, last time. */
    fun rated(t: Tape): Rental? = rentals(t.id).firstOrNull { !it.out && it.stars > 0 }
    fun stars(t: Tape): Int = rated(t)?.stars ?: 0

    /** The latest slip at all, the one a second thought about the stars goes on. */
    fun lastSlip(t: Tape): Rental? = rentals(t.id).firstOrNull { !it.out }

    fun touched(t: Tape) = Rules.touched(t, rentals(t.id), now)

    fun status(t: Tape): Status = when {
        isOut(t) -> Status.OUT
        t.placeOf == Place.BIN -> Status.BIN
        t.readyAt > now -> Status.WINDOW
        else -> Status.SHELF
    }
    fun wear(t: Tape) = Rules.wear(watched(t))
    fun fade(t: Tape) = if (isOut(t) || t.placeOf == Place.BIN) 0f else Rules.fade(Rules.touched(t, rentals(t.id), now), now)

    /** On the shelf so long untouched that the sun has bleached them: the clerk suggests marking them down. */
    val bleached: List<Tape> = shelf.filter { fade(it) >= 1f }

    val open: Boolean = settings.hours.open(clock)
    val opensAt: LocalDateTime = settings.hours.nextOpening(clock)
    val closesAt: LocalDateTime? = settings.hours.closing(clock)

    fun block(t: Tape): Block = when {
        isOut(t) -> Block.OUT
        t.placeOf == Place.STORE && t.readyAt > now -> Block.IN_WINDOW
        !open -> Block.CLOSED
        out.size >= Rules.MAX_OUT -> Block.HANDS_FULL
        else -> Block.NONE
    }

    val overdue: List<Rental> = out.filter { it.overdue(now) }
    val returned: List<Rental> = rentals.filter { !it.out }.sortedByDescending { it.backAt }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun tape(id: Long) = nav.navigate("tape/$id") { launchSingleTop = true }
    fun add() = nav.navigate("add/0") { launchSingleTop = true }
    fun edit(id: Long) = nav.navigate("add/$id") { launchSingleTop = true }
    fun search() = nav.navigate("search") { launchSingleTop = true }
    fun receipt(rental: Long) = nav.navigate("receipt/$rental") { popUpTo("main") }
    fun giveBack(rental: Long) = nav.navigate("return/$rental") { launchSingleTop = true }
    fun mystery() = nav.navigate("mystery") { launchSingleTop = true }
    fun card() = nav.navigate("card") { launchSingleTop = true }
    fun catalog() = nav.navigate("catalog") { launchSingleTop = true }
    fun bin() = nav.navigate("bin") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun back() { nav.popBackStack() }
    fun home() { nav.popBackStack("main", false) }
}

@Composable
fun RewindRoot(settings: Settings) {
    val context = ctx()
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val dao = container.db.dao()
    val tapes by dao.observeTapes().collectAsStateWithLifecycle(initialValue = emptyList())
    val rentals by dao.observeRentals().collectAsStateWithLifecycle(initialValue = emptyList())
    // Due times, the window and the store's hours move with the clock.
    val now = rememberNow(60_000)
    val ru = context.ru
    val home = remember(tapes, rentals, settings, now, ru) { Home(tapes, rentals, settings, now, ru) }
    val go = remember(nav) { Go(nav) }

    // Onboarded elsewhere (the demo seed): leave the welcome screens.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    // Something shared into the app: a new tape with it filled in.
    val shared by container.shared.collectAsStateWithLifecycle()
    LaunchedEffect(shared, settings.onboarded) {
        val (text, subject) = shared ?: return@LaunchedEffect
        if (!settings.onboarded) return@LaunchedEffect
        container.shared.value = null
        container.draft.value = Shares.parse(text, subject)
        go.add()
    }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            val id = listOf(navArgument("id") { type = NavType.LongType })
            composable("welcome") { WelcomeScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } } }
            composable("main") { StoreScreen(home, go) }
            composable("tape/{id}", arguments = id) { e -> TapeScreen(e.arguments?.getLong("id") ?: 0L, home, go) }
            composable("add/{id}", arguments = id) { e -> AddScreen(e.arguments?.getLong("id") ?: 0L, home, go) }
            composable("search") { SearchScreen(home, go) }
            composable("receipt/{id}", arguments = id) { e -> ReceiptScreen(e.arguments?.getLong("id") ?: 0L, home, go) }
            composable("return/{id}", arguments = id) { e -> ReturnScreen(e.arguments?.getLong("id") ?: 0L, home, go) }
            composable("mystery") { MysteryScreen(home, go) }
            composable("card") { CardScreen(home, go) }
            composable("catalog") { CatalogScreen(home, go) }
            composable("bin") { BinScreen(home, go) }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
