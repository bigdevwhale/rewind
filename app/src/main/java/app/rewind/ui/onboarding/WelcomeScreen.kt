package app.rewind.ui.onboarding

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.container
import app.rewind.ui.art.NeonSign
import app.rewind.ui.components.IconDisc
import app.rewind.ui.components.PrimaryButton
import app.rewind.ui.ctx
import app.rewind.ui.theme.UType

/** The story, then the house rules, then the store opens. */
@Composable
fun WelcomeScreen(done: () -> Unit) {
    val c = ctx()
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(36.dp))
            if (page == 0) {
                NeonSign(stringResource(R.string.app_name), stringResource(R.string.sign_open), lit = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(36.dp))
                Text(stringResource(R.string.welcome_title), style = UType.Hero)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.welcome_sub), style = UType.Sub)
            } else {
                Text(stringResource(R.string.rules_title), style = UType.Display)
                Spacer(Modifier.height(18.dp))
                Rule(Icons.Rounded.PanTool, R.string.rule_two, R.string.rule_two_sub)
                Rule(Icons.Rounded.FastRewind, R.string.rule_due, R.string.rule_due_sub)
                Rule(Icons.Rounded.NightsStay, R.string.rule_hours, R.string.rule_hours_sub)
                Rule(Icons.Rounded.Storefront, R.string.rule_window, R.string.rule_window_sub)
                Rule(Icons.Rounded.CardGiftcard, R.string.rule_wrapped, R.string.rule_wrapped_sub)
                Rule(Icons.Rounded.Inventory2, R.string.rule_bin, R.string.rule_bin_sub)
            }
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp).windowInsetsPadding(WindowInsets.navigationBars), horizontalAlignment = Alignment.CenterHorizontally) {
            PrimaryButton(stringResource(if (page == 0) R.string.next else R.string.open_store), {
                if (page == 0) page = 1 else {
                    c.container.write { settings.update { it.copy(onboarded = true) } }
                    done()
                }
            }, Modifier.fillMaxWidth())
            Text(stringResource(R.string.welcome_free), style = UType.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun Rule(icon: ImageVector, title: Int, sub: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Start) {
        IconDisc(icon, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = UType.Strong)
            Text(stringResource(sub), style = UType.Muted)
        }
    }
}
