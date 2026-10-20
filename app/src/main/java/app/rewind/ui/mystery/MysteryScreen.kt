package app.rewind.ui.mystery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.container
import app.rewind.data.Kind
import app.rewind.domain.Mystery
import app.rewind.domain.Time
import app.rewind.ui.Block
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.Parcel
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.components.Chip
import app.rewind.ui.components.PrimaryButton
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.TextLink
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.store.Clerk
import app.rewind.ui.theme.UType
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Can't choose? Tell the clerk how much time there is and, if you like, what kind of thing, and get
 * a tape wrapped in brown paper. Tap it open at home, so to speak.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MysteryScreen(h: Home, go: Go) {
    val c = ctx()
    var time by rememberSaveable { mutableStateOf(Time.EVENING) }
    var kind by rememberSaveable { mutableStateOf<Kind?>(null) }
    var picked by rememberSaveable { mutableLongStateOf(0L) }
    var taps by rememberSaveable { mutableIntStateOf(0) }
    var tries by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val tape = h.tape(picked)

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(stringResource(R.string.wrapped), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (tape == null) {
                Clerk(stringResource(R.string.wrap_clerk))
                SectionTitle(stringResource(R.string.wrap_time), Modifier.fillMaxWidth())
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Time.entries.forEach { t -> Chip(stringResource(timeLabel(t)), time == t) { time = t } }
                }
                SectionTitle(stringResource(R.string.wrap_kind), Modifier.fillMaxWidth())
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.wrap_any), kind == null) { kind = null }
                    Kind.entries.forEach { k -> Chip(T.kind(c, k), kind == k) { kind = k } }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                val torn by animateFloatAsState((taps / 3f).coerceAtMost(1f), tween(350), label = "torn")
                Box(Modifier.width(210.dp), contentAlignment = Alignment.Center) {
                    if (torn >= 1f) TapeBox(tape, rememberCover(tape.cover), width = 210.dp, fade = h.fade(tape), wear = h.wear(tape))
                    else Parcel(
                        stringResource(R.string.wrap_hint, T.kind(c, tape.kindOf).lowercase(), T.minutes(c, tape.length)),
                        torn,
                        Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) { taps++ },
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (torn >= 1f) {
                    Text(tape.title, style = UType.Display, textAlign = TextAlign.Center)
                    if (tape.blurb.isNotBlank()) Text(tape.blurb, style = UType.Note, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                } else Text(stringResource(R.string.wrap_tear), style = UType.Muted)
            }
            Spacer(Modifier.height(20.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).windowInsetsPadding(WindowInsets.navigationBars), horizontalAlignment = Alignment.CenterHorizontally) {
            if (tape == null) {
                val candidates = h.shelf
                var none by remember { mutableStateOf(false) }
                if (none) Text(stringResource(R.string.wrap_none), style = UType.Muted, modifier = Modifier.padding(bottom = 10.dp))
                PrimaryButton(stringResource(R.string.wrap_ask), {
                    val t = Mystery.pick(candidates, time, kind, h::fade, Random(h.now + tries))
                    none = t == null
                    if (t != null) {
                        picked = t.id
                        taps = 0
                    }
                }, Modifier.fillMaxWidth())
            } else {
                val block = h.block(tape)
                if (block == Block.NONE) PrimaryButton(stringResource(R.string.take_home), {
                    scope.launch { go.receipt(c.container.store.rent(tape.id, mystery = true)) }
                }, Modifier.fillMaxWidth(), enabled = taps >= 3)
                else Text(stringResource(if (block == Block.CLOSED) R.string.why_closed_short else R.string.why_full), style = UType.Muted)
                TextLink(stringResource(R.string.wrap_again), {
                    tries++
                    picked = 0L
                })
            }
        }
    }
}

private fun timeLabel(t: Time) = when (t) {
    Time.SHORT -> R.string.time_short
    Time.EVENING -> R.string.time_evening
    Time.LONG -> R.string.time_long
}
