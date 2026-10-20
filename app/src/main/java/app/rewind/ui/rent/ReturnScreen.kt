package app.rewind.ui.rent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.container
import app.rewind.data.Kind
import app.rewind.data.Outcome
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.Art
import app.rewind.ui.art.Reels
import app.rewind.ui.components.Field
import app.rewind.ui.components.PrimaryButton
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.Segmented
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType
import kotlinx.coroutines.delay

/**
 * Bringing a tape back. Be kind: rewind it first, with a finger round the reel or by holding REW,
 * and while it winds, write the slip: three words and the moment that stayed. A tape brought back
 * unfinished is fine; it just says why.
 */
@Composable
fun ReturnScreen(id: Long, h: Home, go: Go) {
    val r = h.rental(id) ?: return
    val t = h.tape(r.tapeId) ?: return
    val c = ctx()
    var outcome by rememberSaveable { mutableStateOf(Outcome.WATCHED) }
    var words by rememberSaveable { mutableStateOf("") }
    var scene by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var wound by rememberSaveable { mutableFloatStateOf(if (Art.still) 0.62f else 0f) }
    var spin by rememberSaveable { mutableFloatStateOf(0f) }
    var holding by rememberSaveable { mutableStateOf(false) }
    // Holding REW winds it in about six seconds.
    LaunchedEffect(holding) {
        while (holding && wound < 1f) {
            delay(16)
            wound = (wound + 0.016f / 6f).coerceAtMost(1f)
            spin += 9f
        }
    }
    val late = r.overdue(h.now)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        TopBar(stringResource(R.string.give_back), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(t.title, style = UType.Display)
            Text(
                if (late) stringResource(R.string.return_late, T.left(c, h.now - r.dueAt)) else stringResource(R.string.return_on_time),
                style = UType.Muted.copy(color = if (late) U.c.amber else U.c.cyan),
            )
            Spacer(Modifier.height(16.dp))
            Segmented(Outcome.entries, outcome, { stringResource(label(it, t.kindOf)) }, { outcome = it })
            SectionTitle(stringResource(R.string.rewind_title))
            Reels(wound, { d -> wound = (wound + d).coerceAtMost(1f); spin += d * 4000f }, spin = spin)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(U.RSm).background(if (holding) U.c.neon else U.c.neonSoft).border(1.dp, U.c.neon, U.RSm)
                        .pointerInput(Unit) { detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }) }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) { Text("◀◀ REW", style = UType.Button.copy(color = if (holding) U.c.onNeon else U.c.neon)) }
                Spacer(Modifier.width(14.dp))
                LinearProgressIndicator(progress = { wound }, Modifier.weight(1f).height(6.dp).clip(U.RSm), color = U.c.cyan, trackColor = U.c.surface2)
            }
            Text(
                stringResource(if (wound >= 1f) R.string.rewound_done else R.string.rewind_hint),
                style = UType.Small.copy(color = if (wound >= 1f) U.c.cyan else U.c.faint), modifier = Modifier.padding(top = 6.dp),
            )
            SectionTitle(stringResource(R.string.slip_title))
            Field(words, { words = it }, stringResource(R.string.f_words), style = UType.Note)
            Spacer(Modifier.height(8.dp))
            Field(scene, { scene = it }, stringResource(R.string.f_scene), singleLine = false, minLines = 2, style = UType.Note)
            if (outcome == Outcome.REWOUND) {
                Spacer(Modifier.height(8.dp))
                Field(note, { note = it }, stringResource(R.string.f_why_not), singleLine = false, style = UType.Note)
            }
            Text(stringResource(R.string.slip_hint), style = UType.Small, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp), textAlign = TextAlign.Start)
        }
        Box(Modifier.fillMaxWidth().background(U.c.surface).padding(horizontal = 20.dp, vertical = 14.dp).windowInsetsPadding(WindowInsets.navigationBars)) {
            PrimaryButton(
                stringResource(if (wound >= 1f) R.string.hand_in else R.string.rewind_first),
                {
                    c.container.write { store.giveBack(id, outcome, words, scene, note) }
                    go.home()
                },
                Modifier.fillMaxWidth(),
                enabled = wound >= 1f,
            )
        }
    }
}

private fun label(o: Outcome, k: Kind) = when (o) {
    Outcome.WATCHED -> when (k) {
        Kind.FILM, Kind.SERIES -> R.string.out_watched
        Kind.ALBUM -> R.string.out_heard
        Kind.BOOK -> R.string.out_read
    }
    Outcome.REWOUND -> R.string.out_rewound
}
