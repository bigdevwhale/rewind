package app.rewind.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.data.Kind
import app.rewind.ui.T
import app.rewind.ui.ctx
import app.rewind.ui.theme.U

/** What a tape is, as chips that wrap: seven kinds no longer fit one segmented row (#2). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KindPicker(selected: Kind, onSelect: (Kind) -> Unit, modifier: Modifier = Modifier) {
    val c = ctx()
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Kind.entries.forEach { k -> Chip(T.kind(c, k), selected == k) { onSelect(k) } }
    }
}

/**
 * The system photo picker, for an own cover (#1): no storage permission, and the owner sees only
 * their gallery. Returns what opens it.
 */
@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPicked(uri) }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/**
 * Under a box: pick an own cover, or with one on, pick another or drop it; [catalogue] says
 * whether dropping it brings back the catalogue's picture or the drawn box.
 */
@Composable
fun OwnCoverLinks(own: Boolean, catalogue: Boolean, onPick: () -> Unit, onDrop: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (!own) TextLink(stringResource(R.string.cover_own), onPick, color = U.c.cyan)
        else {
            TextLink(stringResource(R.string.cover_change), onPick, color = U.c.cyan)
            TextLink(stringResource(if (catalogue) R.string.cover_reset else R.string.cover_remove), onDrop)
        }
    }
}
