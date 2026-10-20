package app.rewind.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.data.Kind
import app.rewind.data.Rental
import app.rewind.data.Tape
import app.rewind.domain.Rules
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.NeonSign
import app.rewind.ui.art.ShelfLedge
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.SoftButton
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType

/** The store: the sign, the clerk, what's at home, the window, the aisles, friends' shelves. */
@Composable
fun StoreScreen(h: Home, go: Go) {
    val c = ctx()
    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(U.c.bg, U.c.bg, U.c.carpet.copy(alpha = 0.55f)))),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = go::card) { Icon(Icons.Rounded.Style, stringResource(R.string.card_title), tint = U.c.muted) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
                }
                NeonSign(
                    stringResource(R.string.app_name),
                    stringResource(if (h.open) R.string.sign_open else R.string.sign_closed),
                    lit = h.open,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(hoursLine(h), style = UType.Small, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
                Clerk(clerkLine(h), Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp))
            }
        }
        item { Counter(h, go) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.bring_in_short), go::add, Modifier.weight(1f), icon = Icons.Rounded.Add)
                SoftButton(stringResource(R.string.wrapped), go::mystery, Modifier.weight(1f), icon = Icons.Rounded.CardGiftcard, enabled = h.shelf.isNotEmpty())
            }
        }
        if (h.window.isNotEmpty()) item {
            Aisle(stringResource(R.string.window_title), h.window, h, go) { t -> stringResource(R.string.window_in, T.left(c, t.readyAt - h.now)) }
        }
        Kind.entries.forEach { k ->
            val tapes = h.aisle(k)
            if (tapes.isNotEmpty()) item(key = "aisle-$k") { Aisle(T.aisle(c, k), tapes, h, go) }
        }
        h.picks.forEach { (who, tapes) ->
            item(key = "pick-$who") { Aisle(stringResource(R.string.picks_of, who), tapes, h, go, hand = true) }
        }
        if (h.tapes.isEmpty()) item { Empty() }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(
                    stringResource(R.string.bin_title) + if (h.bin.size + h.bleached.size > 0) " · ${h.bin.size + h.bleached.size}" else "",
                    go::bin, Modifier.weight(1f), icon = Icons.Rounded.Inventory2,
                )
                SoftButton(stringResource(R.string.card_short), go::card, Modifier.weight(1f), icon = Icons.Rounded.Style)
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun hoursLine(h: Home): String {
    val c = ctx()
    if (!h.settings.hoursOn) return stringResource(R.string.hours_always)
    h.closesAt?.let { return stringResource(R.string.hours_until, T.time(c, it)) }
    val opens = h.opensAt
    return if (opens.toLocalDate() == h.clock.toLocalDate()) stringResource(R.string.hours_opens_today, T.time(c, opens))
    else stringResource(R.string.hours_opens_tomorrow, T.time(c, opens))
}

/** What the clerk behind the counter says, from the state of things. */
@Composable
private fun clerkLine(h: Home): String {
    val c = ctx()
    h.overdue.firstOrNull()?.let { r -> return stringResource(R.string.clerk_late, h.tape(r.tapeId)?.title ?: "") }
    if (h.tapes.isEmpty()) return stringResource(R.string.clerk_empty)
    if (!h.open) return stringResource(R.string.clerk_closed, T.time(c, h.opensAt))
    if (h.out.size >= Rules.MAX_OUT) return stringResource(R.string.clerk_full)
    if (h.bleached.isNotEmpty()) return stringResource(R.string.clerk_bleached, h.bleached.first().title)
    h.window.firstOrNull { it.readyAt - h.now < Rules.DAY }?.let { return stringResource(R.string.clerk_soon, it.title) }
    if (h.out.isEmpty()) return stringResource(R.string.clerk_idle)
    return stringResource(R.string.clerk_one_more)
}

@Composable
fun Clerk(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(U.c.surface2), contentAlignment = Alignment.Center) {
            Text("☺", style = UType.Title.copy(color = U.c.cyan))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(stringResource(R.string.clerk).uppercase(), style = UType.Eyebrow.copy(color = U.c.cyan))
            Text(text, style = UType.Body)
        }
    }
}

/** What's at home: up to two tapes with their time left, and an empty slot for each free hand. */
@Composable
private fun Counter(h: Home, go: Go) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle(stringResource(R.string.at_home, h.out.size, Rules.MAX_OUT))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            (0 until Rules.MAX_OUT).forEach { i ->
                val r = h.out.getOrNull(i)
                val t = r?.let { h.tape(it.tapeId) }
                if (r != null && t != null) OutSlot(t, r, h, go, Modifier.weight(1f)) else FreeSlot(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OutSlot(t: Tape, r: Rental, h: Home, go: Go, modifier: Modifier) {
    val c = ctx()
    val late = r.overdue(h.now)
    Column(
        modifier.clip(U.RMd).background(U.c.surface).border(1.dp, if (late) U.c.amber else U.c.cyan.copy(alpha = 0.5f), U.RMd).clickable { go.giveBack(r.id) }.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TapeBox(t, rememberCover(t.cover), width = 72.dp, wear = h.wear(t))
        Spacer(Modifier.height(8.dp))
        Text(t.title, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (late) stringResource(R.string.late_by, T.left(c, h.now - r.dueAt)) else stringResource(R.string.due_in, T.left(c, r.dueAt - h.now)),
            style = UType.Small.copy(color = if (late) U.c.amber else U.c.cyan),
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.give_back), style = UType.Button.copy(color = U.c.neon))
    }
}

@Composable
private fun FreeSlot(modifier: Modifier) {
    Box(
        modifier.height(196.dp).clip(U.RMd).background(U.c.surface.copy(alpha = 0.4f)).border(1.5.dp, U.c.line, U.RMd),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.free_hand), style = UType.Muted.copy(color = U.c.faint), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
    }
}

/** A shelf of boxes face out, scrolling sideways, on its plank. */
@Composable
private fun Aisle(title: String, tapes: List<Tape>, h: Home, go: Go, hand: Boolean = false, tag: (@Composable (Tape) -> String)? = null) {
    Column(Modifier.padding(top = 8.dp)) {
        if (hand) Text(title, style = UType.Note.copy(color = U.c.amber), modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp).rotate(-1.5f))
        else SectionTitle(title, Modifier.padding(start = 20.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
            items(tapes, key = { it.id }) { t ->
                Column(Modifier.width(104.dp).clickable { go.tape(t.id) }) {
                    TapeBox(t, rememberCover(t.cover), fade = h.fade(t), wear = h.wear(t), sticker = if (h.watched(t) == 0 && t.readyAt > h.now - 7 * Rules.DAY && tag == null) stringResource(R.string.sticker_new) else null)
                    Spacer(Modifier.height(6.dp))
                    Text(tag?.invoke(t) ?: t.title, style = UType.Small.copy(color = if (tag != null) U.c.cyan else U.c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        ShelfLedge(Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun Empty() {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.empty_title), style = UType.Title)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.empty_sub), style = UType.Muted, textAlign = TextAlign.Center)
    }
}
