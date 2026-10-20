package app.rewind.ui.tape

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.container
import app.rewind.data.Outcome
import app.rewind.data.Place
import app.rewind.data.Rental
import app.rewind.domain.Shares
import app.rewind.ui.Block
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.Stamp
import app.rewind.ui.art.Stars
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.components.PrimaryButton
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.SoftButton
import app.rewind.ui.components.TextLink
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType

/** One tape: the front, the back in the owner's words, its slips, and the counter. */
@Composable
fun TapeScreen(id: Long, h: Home, go: Go) {
    val t = h.tape(id) ?: return
    val c = ctx()
    val container = c.container
    val slips = h.rentals(id).filter { !it.out }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(T.kind(c, t.kindOf), go::back) {
            IconButton(onClick = {
                val text = Shares.card(t.kindOf, t.title, t.creator, t.year, t.minutes, t.blurb, h.settings.name)
                c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
            }) { Icon(Icons.Rounded.Share, stringResource(R.string.recommend), tint = U.c.muted) }
            IconButton(onClick = { go.edit(id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                TapeBox(t, rememberCover(t.cover), width = 150.dp, fade = h.fade(t), wear = h.wear(t))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = UType.Display)
                    if (t.creator.isNotBlank()) Text(t.creator, style = UType.Sub)
                    Spacer(Modifier.height(8.dp))
                    Text(listOfNotNull(t.year?.toString(), T.minutes(c, t.length).takeIf { t.minutes != null }).joinToString(" · "), style = UType.Small)
                    Text(stringResource(R.string.loan_for, T.loan(c, t.kindOf)), style = UType.Small.copy(color = U.c.cyan))
                    val seen = h.taken(t)
                    if (seen > 0) Text(stringResource(R.string.seen_times, T.plural(c, R.plurals.times, seen)), style = UType.Small)
                    // Stars from the latest slip; tap to think again.
                    h.lastSlip(t)?.let { slip ->
                        Spacer(Modifier.height(8.dp))
                        Stars(h.stars(t), size = 24.dp, color = U.c.amber, onChange = { n -> container.write { store.rate(slip.id, n) } })
                    }
                }
            }
            // The back of the box.
            SectionTitle(stringResource(R.string.back_of_box))
            Box(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.paper).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Column {
                    Text(t.blurb.ifBlank { stringResource(R.string.back_blank) }, style = UType.Note.copy(color = if (t.blurb.isBlank()) U.c.pen.copy(alpha = 0.4f) else U.c.pen))
                    if (t.from.isNotBlank()) Text("— " + t.from, style = UType.Note.copy(fontSize = UType.Note.fontSize * 0.85f), modifier = Modifier.align(Alignment.End))
                }
            }
            if (t.link != null) TextLink(stringResource(R.string.open_link), { runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(t.link))) } }, Modifier.padding(top = 4.dp))
            if (slips.isNotEmpty()) {
                SectionTitle(stringResource(R.string.slips))
                slips.forEach {
                    Slip(it)
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (t.placeOf == Place.STORE && h.fade(t) >= 1f) {
                Spacer(Modifier.height(16.dp))
                SoftButton(stringResource(R.string.mark_down), { container.write { store.markDown(id) }; go.back() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Inventory2)
            }
            Spacer(Modifier.height(20.dp))
        }
        // The counter.
        Column(Modifier.fillMaxWidth().background(U.c.surface).padding(horizontal = 20.dp, vertical = 14.dp).windowInsetsPadding(WindowInsets.navigationBars)) {
            val out = h.outRental(t)
            when (h.block(t)) {
                Block.OUT -> PrimaryButton(stringResource(R.string.give_back), { out?.let { go.giveBack(it.id) } }, Modifier.fillMaxWidth())
                Block.NONE -> PrimaryButton(stringResource(R.string.take_home), { scope.launch { go.receipt(container.store.rent(id)) } }, Modifier.fillMaxWidth())
                Block.CLOSED -> Reason(stringResource(R.string.why_closed, T.time(c, h.opensAt)))
                Block.HANDS_FULL -> Reason(stringResource(R.string.why_full))
                Block.IN_WINDOW -> Reason(stringResource(R.string.why_window, T.left(c, t.readyAt - h.now)))
            }
            if (t.placeOf == Place.BIN) Text(stringResource(R.string.in_bin_note), style = UType.Small, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun Reason(text: String) {
    Box(Modifier.fillMaxWidth().height(56.dp).clip(U.RLg).border(1.dp, U.c.line, U.RLg), contentAlignment = Alignment.Center) {
        Text(text, style = UType.Muted, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

/** A return slip: the date, three words, the moment, and the till's stamp. */
@Composable
fun Slip(r: Rental, title: String? = null) {
    val c = ctx()
    val rewound = r.outcomeOf == Outcome.REWOUND
    Box(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.paper).padding(14.dp)) {
        Column(Modifier.padding(end = 64.dp)) {
            Text(T.date(c, r.backAt).uppercase() + if (r.mystery) "  ·  " + stringResource(R.string.slip_wrapped) else "", style = UType.Receipt.copy(color = Color(0xFF6A6372)))
            if (title != null) Text(title, style = UType.Receipt.copy(fontSize = UType.Receipt.fontSize * 1.1f))
            if (r.stars > 0) Stars(r.stars, Modifier.padding(vertical = 4.dp), size = 16.dp)
            if (r.words.isNotBlank()) Text(r.words, style = UType.Note)
            if (r.scene.isNotBlank()) Text("«" + r.scene + "»", style = UType.Note.copy(fontSize = UType.Note.fontSize * 0.85f))
            if (r.note.isNotBlank()) Text(r.note, style = UType.Note.copy(fontSize = UType.Note.fontSize * 0.85f, color = U.c.stamp))
        }
        Stamp(
            stringResource(if (rewound) R.string.stamp_rewound else if (r.late) R.string.stamp_late else R.string.stamp_back),
            if (r.late || rewound) U.c.stamp else U.c.pen,
            Modifier.align(Alignment.TopEnd).size(58.dp),
            angle = if (rewound) 8f else -6f,
        )
    }
}
