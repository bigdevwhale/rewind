package app.rewind.ui.rent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.container
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.Dashes
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.art.zigzag
import app.rewind.ui.components.PrimaryButton
import app.rewind.ui.components.TextLink
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType

/** The till's receipt for a tape taken home: what, when it's due, and the house's one request. */
@Composable
fun ReceiptScreen(id: Long, h: Home, go: Go) {
    val r = h.rental(id) ?: return
    val t = h.tape(r.tapeId) ?: return
    val c = ctx()
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        TapeBox(t, rememberCover(t.cover), width = 120.dp, wear = h.wear(t))
        Spacer(Modifier.height(24.dp))
        val paper = U.c.paper
        Canvas(Modifier.fillMaxWidth().height(8.dp)) { zigzag(paper, top = true) }
        Column(Modifier.fillMaxWidth().background(paper).padding(horizontal = 20.dp, vertical = 10.dp)) {
            val bold = UType.Receipt.copy(fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.app_name).uppercase(), style = bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.receipt_head), style = UType.Receipt, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Dashes(Modifier.padding(vertical = 8.dp))
            Line(stringResource(R.string.receipt_item), t.title)
            Line(stringResource(R.string.receipt_kind), T.kind(c, t.kindOf))
            Line(stringResource(R.string.receipt_out), T.when_(c, r.outAt))
            Line(stringResource(R.string.receipt_due), T.when_(c, r.dueAt), strong = true)
            Line(stringResource(R.string.receipt_price), stringResource(R.string.receipt_free))
            Dashes(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.be_kind), style = bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.receipt_foot), style = UType.Receipt, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) { zigzag(paper, top = false) }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(stringResource(R.string.receipt_done), go::home, Modifier.fillMaxWidth())
        TextLink(stringResource(R.string.receipt_undo), {
            c.container.write { store.undoRent(id) }
            go.home()
        }, Modifier.padding(top = 6.dp))
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(20.dp))
    }
}

@Composable
private fun Line(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = UType.Receipt)
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text(value, style = if (strong) UType.Receipt.copy(fontWeight = FontWeight.Bold, color = U.c.stamp) else UType.Receipt, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
