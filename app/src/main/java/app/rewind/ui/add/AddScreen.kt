package app.rewind.ui.add

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rewind.R
import app.rewind.container
import app.rewind.data.Kind
import app.rewind.domain.Draft
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.art.TapeBox
import app.rewind.ui.art.rememberCover
import app.rewind.ui.art.rememberRemote
import app.rewind.ui.components.Field
import app.rewind.ui.components.PrimaryButton
import app.rewind.ui.components.Segmented
import app.rewind.ui.components.SectionTitle
import app.rewind.ui.components.SoftButton
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType
import kotlinx.coroutines.launch

/**
 * A new tape (or a tape's details, with [id]): what it is, found in the catalogue or typed, and the
 * back of the box in your own words.
 */
@Composable
fun AddScreen(id: Long, h: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val editing = h.tape(id)
    val incoming by container.draft.collectAsStateWithLifecycle()
    var d by remember {
        mutableStateOf(
            editing?.let { Draft(it.kindOf, it.title, it.creator, it.year, it.minutes, it.blurb, it.from, it.link) } ?: Draft(),
        )
    }
    // A pick from the catalogue (or a share) fills in what it knows, and keeps what you wrote.
    LaunchedEffect(incoming) {
        val i = incoming ?: return@LaunchedEffect
        container.draft.value = null
        d = d.copy(
            kind = i.kind, title = i.title, creator = i.creator.ifBlank { d.creator }, year = i.year ?: d.year, minutes = i.minutes ?: d.minutes,
            link = i.link ?: d.link, coverUrl = i.coverUrl ?: d.coverUrl,
            blurb = i.blurb.ifBlank { d.blurb }, from = i.from.ifBlank { d.from },
        )
    }
    var saving by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        TopBar(stringResource(if (editing != null) R.string.edit else R.string.bring_in), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Segmented(Kind.entries, d.kind, { T.kind(c, it) }, { d = d.copy(kind = it) })
            Spacer(Modifier.height(14.dp))
            SoftButton(stringResource(R.string.find_in_catalog), {
                container.draft.value = null
                Search.kind = d.kind
                Search.query = d.title
                go.search()
            }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Search)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Preview(d, editing?.cover, editing?.seed ?: d.title.hashCode().toLong())
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(d.title, { d = d.copy(title = it) }, stringResource(R.string.f_title))
                    Field(d.creator, { d = d.copy(creator = it) }, stringResource(creatorLabel(d.kind)))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Number(d.year, { d = d.copy(year = it) }, stringResource(R.string.f_year), Modifier.weight(1f))
                        Number(d.minutes, { d = d.copy(minutes = it) }, stringResource(R.string.f_minutes), Modifier.weight(1f))
                    }
                }
            }
            SectionTitle(stringResource(R.string.back_of_box))
            Text(stringResource(R.string.back_hint), style = UType.Muted, modifier = Modifier.padding(bottom = 8.dp))
            Field(d.blurb, { d = d.copy(blurb = it) }, stringResource(R.string.f_blurb), singleLine = false, minLines = 3, style = UType.Note)
            Spacer(Modifier.height(10.dp))
            Field(d.from, { d = d.copy(from = it) }, stringResource(R.string.f_from))
            Text(stringResource(R.string.from_hint), style = UType.Small, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            if (editing == null && h.settings.windowDays > 0) Text(
                stringResource(R.string.window_note, T.plural(c, R.plurals.days, h.settings.windowDays)),
                style = UType.Muted.copy(color = U.c.cyan), modifier = Modifier.padding(top = 18.dp),
            )
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.fillMaxWidth().background(U.c.surface).padding(horizontal = 20.dp, vertical = 14.dp).windowInsetsPadding(WindowInsets.navigationBars)) {
            PrimaryButton(
                stringResource(if (saving) R.string.saving else if (editing != null) R.string.save else R.string.put_on_shelf),
                {
                    saving = true
                    scope.launch {
                        if (editing != null) container.store.edit(id, d) else container.store.add(d)
                        go.back()
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = d.title.isNotBlank() && !saving,
            )
        }
    }
}

private fun creatorLabel(k: Kind) = when (k) {
    Kind.FILM -> R.string.f_director
    Kind.SERIES -> R.string.f_network
    Kind.ALBUM -> R.string.f_artist
    Kind.BOOK, Kind.COMIC -> R.string.f_author
}

/** The box as it'll stand on the shelf: the catalogue's picture if one was picked. */
@Composable
private fun Preview(d: Draft, saved: String?, seed: Long) {
    val remote = rememberRemote(d.coverUrl)
    val cover = remote ?: rememberCover(saved)
    TapeBox(app.rewind.data.Tape(kind = d.kind.name, title = d.title.ifBlank { "?" }, creator = d.creator, seed = seed), cover, width = 112.dp)
}

@Composable
private fun Number(value: Int?, onChange: (Int?) -> Unit, label: String, modifier: Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { s -> onChange(s.filter(Char::isDigit).take(4).toIntOrNull()) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        shape = U.RMd,
        textStyle = UType.Body,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = U.c.neon, unfocusedBorderColor = U.c.line, focusedLabelColor = U.c.neon, unfocusedLabelColor = U.c.faint,
            cursorColor = U.c.neon, focusedContainerColor = U.c.surface, unfocusedContainerColor = U.c.surface,
        ),
    )
}

/** Where the catalogue search starts from, set by the form that opens it. */
object Search {
    var kind: Kind = Kind.FILM
    var query: String = ""
}

/** A catalogue result, for the search list. */
@Composable
fun Hit(d: Draft, onClick: () -> Unit) {
    val c = ctx()
    val pic = rememberRemote(d.coverUrl)
    Row(Modifier.fillMaxWidth().clip(U.RMd).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(52.dp).aspectRatio(0.7f).clip(U.RSm).background(U.c.surface2)) {
            if (pic != null) Image(pic, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(d.title, style = UType.Strong, maxLines = 2)
            val meta = listOfNotNull(d.year?.toString(), d.creator.ifBlank { null }, d.minutes?.let { T.minutes(c, it) }).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, style = UType.Muted, maxLines = 2)
        }
    }
}
