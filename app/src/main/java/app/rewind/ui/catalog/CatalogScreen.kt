package app.rewind.ui.catalog

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.data.Kind
import app.rewind.data.Outcome
import app.rewind.data.Tape
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.Status
import app.rewind.ui.T
import app.rewind.ui.art.Stamp
import app.rewind.ui.art.Stars
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.components.Chip
import app.rewind.ui.components.Field
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType
import java.text.Collator
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How the drawer is ordered; each order also has its own divider cards. */
enum class Sort { TITLE, RATING, TAKEN, NEWEST, WAITING, YEAR, LENGTH }

/** Which cards are in the drawer. */
enum class Show { ALL, SHELF, OUT, WINDOW, NEVER, SEEN, BIN }

/**
 * The card catalogue: one index card for every tape the store has ever had, in a wooden drawer.
 * Sort it by title, stars, how often it's been taken, when it came in, how long it's waited, year or
 * length, with divider cards between the groups; narrow it by kind and by where the tape is.
 */
@Composable
fun CatalogScreen(h: Home, go: Go) {
    val c = ctx()
    var sort by rememberSaveable { mutableStateOf(Sort.TITLE) }
    var show by rememberSaveable { mutableStateOf(Show.ALL) }
    var kind by rememberSaveable { mutableStateOf<Kind?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    val cards = remember(h, sort, show, kind, query) {
        val q = query.trim().lowercase()
        val picked = h.tapes.filter { t ->
            (kind == null || t.kindOf == kind) &&
                when (show) {
                    Show.ALL -> true
                    Show.SHELF -> h.status(t) == Status.SHELF
                    Show.OUT -> h.status(t) == Status.OUT
                    Show.WINDOW -> h.status(t) == Status.WINDOW
                    Show.BIN -> h.status(t) == Status.BIN
                    Show.NEVER -> h.taken(t) == 0
                    Show.SEEN -> h.watched(t) > 0
                } &&
                (q.isEmpty() || listOf(t.title, t.creator, t.from, t.blurb).any { it.lowercase().contains(q) })
        }
        sorted(picked, sort, h)
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        TopBar(stringResource(R.string.catalog_title), go::back)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Field(query, { query = it }, stringResource(R.string.catalog_search))
            Spacer(Modifier.height(10.dp))
            Strip {
                Text(stringResource(R.string.catalog_sort).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(end = 4.dp))
                Sort.entries.forEach { s -> Chip(stringResource(sortLabel(s)), sort == s) { sort = s } }
            }
            Strip {
                Chip(stringResource(R.string.wrap_any), kind == null) { kind = null }
                Kind.entries.forEach { k -> Chip(T.aisle(c, k), kind == k) { kind = if (kind == k) null else k } }
            }
            Strip {
                Show.entries.forEach { s -> Chip(stringResource(showLabel(s)), show == s) { show = s } }
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        ) {
            item { Drawer(T.plural(c, R.plurals.cards, cards.size)) }
            var last: String? = null
            cards.forEach { t ->
                val section = section(c, t, sort, h)
                if (section != last) {
                    last = section
                    item(key = "s-$section") { Divider(section) }
                }
                item(key = t.id) { IndexCard(t, h) { go.tape(t.id) } }
            }
            if (cards.isEmpty()) item {
                Text(stringResource(R.string.catalog_empty), style = UType.Muted, modifier = Modifier.padding(20.dp))
            }
            item { Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) }
        }
    }
}

@Composable
private fun Strip(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

private fun sorted(tapes: List<Tape>, sort: Sort, h: Home): List<Tape> {
    // In the phone's language's alphabetical order, ignoring case and accents.
    val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
    val title = Comparator<Tape> { a, b -> collator.compare(a.title, b.title) }
    return when (sort) {
        Sort.TITLE -> tapes.sortedWith(title)
        Sort.RATING -> tapes.sortedWith(compareByDescending<Tape> { h.stars(it) }.then(title))
        Sort.TAKEN -> tapes.sortedWith(compareByDescending<Tape> { h.taken(it) }.then(title))
        Sort.NEWEST -> tapes.sortedByDescending { it.addedAt }
        // What has waited longest untouched first: the ones about to fade.
        Sort.WAITING -> tapes.sortedBy { if (h.status(it) == Status.OUT) Long.MAX_VALUE else h.touched(it) }
        Sort.YEAR -> tapes.sortedWith(compareBy<Tape> { it.year ?: Int.MAX_VALUE }.then(title))
        Sort.LENGTH -> tapes.sortedWith(compareBy<Tape> { it.length }.then(title))
    }
}

/** The divider card a tape falls behind, for the order in use. */
private fun section(c: Context, t: Tape, sort: Sort, h: Home): String {
    return when (sort) {
        Sort.TITLE -> t.title.trim().firstOrNull()?.let { if (it.isLetter()) it.uppercaseChar().toString() else "#" } ?: "#"
        Sort.RATING -> h.stars(t).let { if (it == 0) c.getString(R.string.sec_no_stars) else "★".repeat(it) }
        Sort.TAKEN -> h.taken(t).let {
            when {
                it == 0 -> c.getString(R.string.sec_never)
                it == 1 -> c.getString(R.string.sec_once)
                it < 5 -> c.getString(R.string.sec_few)
                else -> c.getString(R.string.sec_many)
            }
        }
        Sort.NEWEST -> Instant.ofEpochMilli(t.addedAt).atZone(h.zone).format(DateTimeFormatter.ofPattern("LLLL yyyy", c.resources.configuration.locales[0]))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
        Sort.WAITING -> when {
            h.status(t) == Status.OUT -> c.getString(R.string.sec_out)
            h.fade(t) >= 1f || h.status(t) == Status.BIN -> c.getString(R.string.sec_bleached)
            h.fade(t) > 0f -> c.getString(R.string.sec_fading)
            else -> c.getString(R.string.sec_fresh)
        }
        Sort.YEAR -> t.year?.let { c.getString(R.string.sec_decade, it / 10 * 10) } ?: c.getString(R.string.sec_no_year)
        Sort.LENGTH -> when {
            t.length <= 60 -> c.getString(R.string.sec_short)
            t.length <= 150 -> c.getString(R.string.sec_evening)
            else -> c.getString(R.string.sec_long)
        }
    }
}

/** The drawer's front: wood, a brass label holder, the count on a slip in it. */
@Composable
private fun Drawer(count: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(U.RSm).background(Color(0xFF5A3A24)).padding(14.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val grain = Color(0xFF4A2E1B)
            listOf(0.18f, 0.42f, 0.7f, 0.88f).forEach { y -> drawLine(grain, Offset(0f, size.height * y), Offset(size.width, size.height * y + 6f), 3f) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFC9A55A)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                Box(Modifier.background(U.c.paper).padding(horizontal = 18.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.catalog_title).uppercase() + " · " + count, style = UType.Receipt)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(64.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFC9A55A)))
        }
    }
}

/** A divider card, its tab sticking up with the group's name on it. */
@Composable
private fun Divider(label: String) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Box(Modifier.padding(start = 18.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(Color(0xFFD8C39A)).padding(horizontal = 14.dp, vertical = 4.dp)) {
            Text(label, style = UType.Title.copy(color = Color(0xFF3B2A14)))
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFD8C39A)))
    }
}

/** One index card: ruled paper, a red line at the top, the hole for the drawer rod. */
@Composable
private fun IndexCard(t: Tape, h: Home, onClick: () -> Unit) {
    val c = ctx()
    val paper = U.c.paper
    val rule = Color(0xFFBFD3E6)
    val red = Color(0xFFD9707F)
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(3.dp)).background(paper).clickable(onClick = onClick)) {
        Canvas(Modifier.matchParentSize()) {
            drawLine(red, Offset(0f, 30.dp.toPx()), Offset(size.width, 30.dp.toPx()), 1.5f)
            var y = 30.dp.toPx() + 22.dp.toPx()
            while (y < size.height - 4) { drawLine(rule, Offset(0f, y), Offset(size.width, y), 1f); y += 22.dp.toPx() }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 20.dp), verticalAlignment = Alignment.Top) {
            TapeBox(t, rememberCover(t.cover), width = 46.dp, fade = h.fade(t), wear = h.wear(t))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, style = UType.Receipt.copy(fontSize = UType.Receipt.fontSize * 1.15f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                val meta = listOfNotNull(T.kind(c, t.kindOf), t.creator.ifBlank { null }, t.year?.toString()).joinToString(" · ")
                Text(meta, style = UType.Note.copy(fontSize = UType.Note.fontSize * 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val stars = h.stars(t)
                    if (stars > 0) Stars(stars, size = 14.dp, color = U.c.stamp) else Text(stringResource(R.string.no_stars), style = UType.Note.copy(fontSize = UType.Note.fontSize * 0.7f, color = U.c.pen.copy(alpha = 0.45f)))
                    Spacer(Modifier.width(10.dp))
                    val taken = h.taken(t)
                    if (taken > 0) Text(stringResource(R.string.taken_n, T.plural(c, R.plurals.times, taken)), style = UType.Note.copy(fontSize = UType.Note.fontSize * 0.7f))
                }
                h.rentals(t.id).firstOrNull { it.words.isNotBlank() && it.outcomeOf == Outcome.WATCHED }?.let {
                    Text(it.words, style = UType.Note.copy(fontSize = UType.Note.fontSize * 0.75f, color = U.c.pen.copy(alpha = 0.8f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val status = h.status(t)
            if (status != Status.SHELF) Stamp(
                stringResource(
                    when (status) {
                        Status.OUT -> R.string.st_out
                        Status.WINDOW -> R.string.st_window
                        else -> R.string.st_bin
                    },
                ),
                if (status == Status.OUT) U.c.pen else U.c.stamp,
                Modifier.size(52.dp),
                angle = 10f,
            )
        }
        // The rod hole.
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(10.dp).clip(CircleShape).background(U.c.bg))
    }
}

private fun sortLabel(s: Sort) = when (s) {
    Sort.TITLE -> R.string.sort_title
    Sort.RATING -> R.string.sort_rating
    Sort.TAKEN -> R.string.sort_taken
    Sort.NEWEST -> R.string.sort_newest
    Sort.WAITING -> R.string.sort_waiting
    Sort.YEAR -> R.string.sort_year
    Sort.LENGTH -> R.string.sort_length
}

private fun showLabel(s: Show) = when (s) {
    Show.ALL -> R.string.show_all
    Show.SHELF -> R.string.show_shelf
    Show.OUT -> R.string.show_out
    Show.WINDOW -> R.string.show_window
    Show.NEVER -> R.string.show_never
    Show.SEEN -> R.string.show_seen
    Show.BIN -> R.string.show_bin
}
