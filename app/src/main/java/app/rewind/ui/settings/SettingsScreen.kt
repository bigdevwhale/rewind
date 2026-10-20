package app.rewind.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.rewind.R
import app.rewind.container
import app.rewind.data.Settings
import app.rewind.notify.Reminders
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.Language
import app.rewind.ui.T
import app.rewind.ui.components.Field
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.Segmented
import app.rewind.ui.components.Toggle
import app.rewind.ui.components.TopBar
import app.rewind.ui.components.UCard
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType

@Composable
fun SettingsScreen(h: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val s = h.settings
    fun set(change: (Settings) -> Settings) = container.write { settings.update(change); Reminders.schedule(c) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { set { it.copy(reminders = true) } }
    var name by rememberSaveable { mutableStateOf(s.name) }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionTitle(stringResource(R.string.s_hours))
            UCard {
                Toggle(stringResource(R.string.s_hours_on), stringResource(R.string.s_hours_on_sub), s.hoursOn) { on -> set { it.copy(hoursOn = on) } }
                if (s.hoursOn) {
                    Stepper(stringResource(R.string.s_weekday), hour(s.weekdayOpen), { set { it.copy(weekdayOpen = (it.weekdayOpen + 23) % 24) } }, { set { it.copy(weekdayOpen = (it.weekdayOpen + 1) % 24) } })
                    Stepper(stringResource(R.string.s_weekend), hour(s.weekendOpen), { set { it.copy(weekendOpen = (it.weekendOpen + 23) % 24) } }, { set { it.copy(weekendOpen = (it.weekendOpen + 1) % 24) } })
                    Stepper(stringResource(R.string.s_close), hour(s.closeAt), { set { it.copy(closeAt = (it.closeAt + 23) % 24) } }, { set { it.copy(closeAt = (it.closeAt + 1) % 24) } })
                }
            }
            SectionTitle(stringResource(R.string.s_window))
            UCard {
                Text(stringResource(R.string.s_window_sub), style = UType.Muted, modifier = Modifier.padding(bottom = 10.dp))
                Segmented(listOf(0, 1, 3, 7), s.windowDays, { if (it == 0) stringResource(R.string.off) else T.plural(c, R.plurals.days, it) }, { d -> set { it.copy(windowDays = d) } })
            }
            SectionTitle(stringResource(R.string.s_reminders))
            UCard {
                Toggle(stringResource(R.string.s_reminders_on), stringResource(R.string.s_reminders_sub), s.reminders) { on ->
                    val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    if (on && !granted) ask.launch(Manifest.permission.POST_NOTIFICATIONS) else set { it.copy(reminders = on) }
                }
            }
            SectionTitle(stringResource(R.string.s_name))
            UCard {
                Text(stringResource(R.string.s_name_sub), style = UType.Muted, modifier = Modifier.padding(bottom = 10.dp))
                Field(name, { name = it; set { st -> st.copy(name = it.trim()) } }, stringResource(R.string.s_name_field))
            }
            SectionTitle(stringResource(R.string.s_language))
            Segmented(listOf(Language.SYSTEM, Language.EN, Language.RU), Language.current(), {
                when (it) {
                    Language.EN -> "English"
                    Language.RU -> "Русский"
                    else -> stringResource(R.string.s_lang_system)
                }
            }, { Language.set(it) })
            SectionTitle(stringResource(R.string.s_about))
            Text(stringResource(R.string.s_about_text), style = UType.Muted)
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(28.dp))
        }
    }
}

private fun hour(h: Int) = "%02d:00".format(h)

@Composable
private fun Stepper(label: String, value: String, minus: () -> Unit, plus: () -> Unit) {
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        Text(label, style = UType.Body, modifier = Modifier.weight(1f))
        IconButton(onClick = minus) { Icon(Icons.Rounded.Remove, null, tint = U.c.muted) }
        Text(value, style = UType.Title.copy(color = U.c.cyan), modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = plus) { Icon(Icons.Rounded.Add, null, tint = U.c.muted) }
    }
}
