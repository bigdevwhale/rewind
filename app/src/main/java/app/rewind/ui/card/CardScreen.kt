package app.rewind.ui.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.data.Outcome
import app.rewind.data.Rental
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.Dashes
import app.rewind.ui.art.Stamp
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.art.zigzag
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.tape.Slip
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType
import java.time.Instant
import java.time.YearMonth

/**
 * The member's card: a stamp for every tape brought back, the month's favourite, and the year's
 * till roll of everything taken home.
 */
@Composable
fun CardScreen(h: Home, go: Go) {
    val c = ctx()
    val back = h.returned
    val month = YearMonth.from(h.clock)
    fun ym(at: Long) = YearMonth.from(Instant.ofEpochMilli(at).atZone(h.zone))
    // The month's hit: the tape taken most this month, the latest on a tie; else last month's.
    val hit = listOf(month, month.minusMonths(1)).firstNotNullOfOrNull { m ->
        back.filter { ym(it.backAt) == m && it.outcomeOf == Outcome.WATCHED }.groupBy { it.tapeId }.maxByOrNull { (_, l) -> l.size * 1e13 + l.maxOf { it.backAt } }?.let { m to it.key }
    }
    val year = h.clock.year
    val roll = h.rentals.filter { Instant.ofEpochMilli(it.outAt).atZone(h.zone).year == year }.sortedBy { it.outAt }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(stringResource(R.string.card_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // The card itself.
            Column(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.paper).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.app_name).uppercase(), style = UType.Title.copy(color = U.c.stamp))
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.card_member), style = UType.Receipt)
                }
                Text(h.settings.name.ifBlank { stringResource(R.string.card_no_name) }, style = UType.Note, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(10.dp))
                Stamps(back.reversed())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat(stringResource(R.string.stat_taken), h.rentals.size)
                    Stat(stringResource(R.string.stat_seen), back.count { it.outcomeOf == Outcome.WATCHED })
                    Stat(stringResource(R.string.stat_rewound), back.count { it.outcomeOf == Outcome.REWOUND })
                    Stat(stringResource(R.string.stat_late), back.count { it.late })
                }
            }
            hit?.let { (m, id) ->
                val t = h.tape(id) ?: return@let
                SectionTitle(stringResource(if (m == month) R.string.hit_month else R.string.hit_last_month))
                Row(Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).clickable { go.tape(id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    TapeBox(t, rememberCover(t.cover), width = 70.dp, wear = h.wear(t))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("★ " + t.title, style = UType.Title)
                        h.rentals(id).firstOrNull { it.words.isNotBlank() }?.let { Text(it.words, style = UType.Note.copy(color = U.c.amber)) }
                    }
                }
            }
            if (back.isNotEmpty()) {
                SectionTitle(stringResource(R.string.slips_recent))
                back.take(5).forEach {
                    Slip(it, h.tape(it.tapeId)?.title)
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (roll.isNotEmpty()) {
                SectionTitle(stringResource(R.string.roll_title, year))
                Roll(roll, h)
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(24.dp))
        }
    }
}

/** A grid of stamps, one per return: blue on time, red when late or rewound, empty boxes after. */
@Composable
private fun Stamps(back: List<Rental>) {
    val cols = 6
    val shown = maxOf(18, ((back.size + cols) / cols) * cols)
    val line = U.c.paperLine
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        (0 until shown / cols).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                (0 until cols).forEach { col ->
                    val r = back.getOrNull(row * cols + col)
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), 1.5f)
                            drawLine(line, Offset(size.width, 0f), Offset(size.width, size.height), 1.5f)
                        }
                        if (r != null) {
                            val red = r.late || r.outcomeOf == Outcome.REWOUND
                            Stamp(
                                T.short(r.backAt),
                                if (red) U.c.stamp else U.c.pen,
                                Modifier.size(44.dp),
                                angle = ((r.id * 37) % 24 - 12).toFloat(),
                                faint = r.outcomeOf == Outcome.REWOUND,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, n: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n.toString(), style = UType.Title.copy(color = U.c.pen))
        Text(label, style = UType.Receipt.copy(color = Color(0xFF6A6372)))
    }
}

/** The year on a till roll: date, title, how it came back. */
@Composable
private fun Roll(roll: List<Rental>, h: Home) {
    val c = ctx()
    val paper = U.c.paper
    Canvas(Modifier.fillMaxWidth().height(8.dp)) { zigzag(paper, top = true) }
    Column(Modifier.fillMaxWidth().background(paper).padding(horizontal = 16.dp, vertical = 8.dp)) {
        roll.forEach { r ->
            val t = h.tape(r.tapeId) ?: return@forEach
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(T.date(c, r.outAt).padEnd(7).take(7), style = UType.Receipt, modifier = Modifier.width(72.dp))
                Text(t.title, style = UType.Receipt, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    when {
                        r.out -> "▶"
                        r.outcomeOf == Outcome.REWOUND -> "◀◀"
                        r.late -> "!"
                        else -> "✓"
                    },
                    style = UType.Receipt.copy(fontWeight = FontWeight.Bold, color = if (r.late) U.c.stamp else UType.Receipt.color),
                    textAlign = TextAlign.End, modifier = Modifier.width(28.dp),
                )
            }
        }
        Dashes(Modifier.padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.roll_total), style = UType.Receipt.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            Text(T.plural(c, R.plurals.tapes, roll.size), style = UType.Receipt.copy(fontWeight = FontWeight.Bold))
        }
    }
    Canvas(Modifier.fillMaxWidth().height(8.dp)) { zigzag(paper, top = false) }
}
