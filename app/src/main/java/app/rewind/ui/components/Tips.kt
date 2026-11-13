package app.rewind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rewind.BuildConfig
import app.rewind.R
import app.rewind.container
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType
import kotlinx.coroutines.launch

/** A screen's short how-to from the clerk: shown the first time the screen opens, and again from its «?». */
enum class Tip(val key: String, val title: Int, val lines: List<Int>) {
    STORE("store", R.string.tip_store, listOf(R.string.tip_store_1, R.string.tip_store_2, R.string.tip_store_3, R.string.tip_store_4)),
    ADD("add", R.string.tip_add, listOf(R.string.tip_add_1, R.string.tip_add_2, R.string.tip_add_3, R.string.tip_add_4)),
    WRAPPED("wrapped", R.string.tip_wrapped, listOf(R.string.tip_wrapped_1, R.string.tip_wrapped_2, R.string.tip_wrapped_3)),
    CARD("card", R.string.tip_card, listOf(R.string.tip_card_1, R.string.tip_card_2, R.string.tip_card_3)),
    CATALOG("catalog", R.string.tip_catalog, listOf(R.string.tip_catalog_1, R.string.tip_catalog_2, R.string.tip_catalog_3)),
    BIN("bin", R.string.tip_bin, listOf(R.string.tip_bin_1, R.string.tip_bin_2, R.string.tip_bin_3)),
    TAPE("tape", R.string.tip_tape, listOf(R.string.tip_tape_1, R.string.tip_tape_2, R.string.tip_tape_3)),
    RETURN("return", R.string.tip_return, listOf(R.string.tip_return_1, R.string.tip_return_2, R.string.tip_return_3)),
}

/** Which tip the user asked to see again with «?». */
private val reopened = mutableStateOf<Tip?>(null)

/** The «?» that brings a screen's tip back. */
@Composable
fun TipButton(tip: Tip) {
    IconButton(onClick = { reopened.value = tip }) {
        Icon(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.tip_help), tint = U.c.muted)
    }
}

/** The tip itself, once per screen (never in the screenshot build), or whenever its «?» is tapped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipSheet(tip: Tip) {
    val container = LocalContext.current.container
    val seen by remember { container.settings.tipsSeen }.collectAsState(initial = null)
    val again = reopened.value == tip
    val first = seen != null && tip.key !in seen!! && !BuildConfig.SHOTS
    if (!again && !first) return
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun close() {
        reopened.value = null
        scope.launch { container.settings.sawTip(tip.key) }
    }
    ModalBottomSheet(onDismissRequest = ::close, sheetState = state, containerColor = U.c.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(U.c.bg), contentAlignment = Alignment.Center) { Text("☺", style = UType.Title) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.clerk).uppercase(), style = UType.Eyebrow.copy(color = U.c.cyan))
                    Text(stringResource(tip.title), style = UType.Title)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tip.lines.forEachIndexed { i, line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(U.c.bg), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = UType.Small.copy(color = U.c.neon), textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(line), style = UType.Body, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.tip_ok), {
                scope.launch { state.hide() }.invokeOnCompletion { close() }
            }, Modifier.fillMaxWidth())
        }
    }
}
