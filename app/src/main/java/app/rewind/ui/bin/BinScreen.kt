package app.rewind.ui.bin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.container
import app.rewind.data.Tape
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.TextLink
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.store.Clerk
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType

/**
 * The bargain bin. Tapes the sun has bleached on the shelf are offered to it; what's in it can be
 * rescued (back on the shelf, fresh), taken home straight from it, or thrown out for good.
 */
@Composable
fun BinScreen(h: Home, go: Go) {
    val c = ctx()
    val container = c.container
    var throwing by rememberSaveable { mutableLongStateOf(0L) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(stringResource(R.string.bin_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Clerk(stringResource(if (h.bin.isEmpty() && h.bleached.isEmpty()) R.string.bin_clerk_empty else R.string.bin_clerk))
            if (h.bleached.isNotEmpty()) {
                SectionTitle(stringResource(R.string.bin_bleached))
                h.bleached.forEach { t ->
                    Item(t, h, go, T.date(c, h.rentals(t.id).firstOrNull()?.backAt?.takeIf { it > 0 } ?: t.readyAt)) {
                        TextLink(stringResource(R.string.mark_down), { container.write { store.markDown(t.id) } }, color = U.c.neon)
                    }
                }
            }
            if (h.bin.isNotEmpty()) {
                SectionTitle(stringResource(R.string.bin_in))
                h.bin.forEach { t ->
                    Item(t, h, go, stringResource(R.string.bin_since, T.date(c, t.binAt))) {
                        Row {
                            TextLink(stringResource(R.string.rescue), { container.write { store.rescue(t.id) } }, color = U.c.cyan)
                            TextLink(stringResource(R.string.throw_out), { throwing = t.id })
                        }
                    }
                }
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(24.dp))
        }
    }
    h.tape(throwing)?.let { t ->
        AlertDialog(
            onDismissRequest = { throwing = 0L },
            title = { Text(stringResource(R.string.throw_title, t.title)) },
            text = { Text(stringResource(R.string.throw_text)) },
            confirmButton = { TextLink(stringResource(R.string.throw_out), { container.write { store.forget(t.id) }; throwing = 0L }, color = U.c.stamp) },
            dismissButton = { TextLink(stringResource(R.string.cancel), { throwing = 0L }) },
            containerColor = U.c.surface,
        )
    }
}

@Composable
private fun Item(t: Tape, h: Home, go: Go, sub: String, actions: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(U.RMd).background(U.c.surface).clickable { go.tape(t.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TapeBox(t, rememberCover(t.cover), width = 58.dp, fade = maxOf(h.fade(t), if (t.placeOf == app.rewind.data.Place.BIN) 1f else 0f), wear = h.wear(t), sticker = if (t.placeOf == app.rewind.data.Place.BIN) "½" else null)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.title, style = UType.Strong)
            Text(sub, style = UType.Small)
            actions()
        }
    }
}
