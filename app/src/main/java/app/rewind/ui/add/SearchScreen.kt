package app.rewind.ui.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rewind.R
import app.rewind.container
import app.rewind.data.Catalog
import app.rewind.data.Kind
import app.rewind.domain.Draft
import app.rewind.ui.Go
import app.rewind.ui.Home
import app.rewind.ui.T
import app.rewind.ui.components.Field
import app.rewind.ui.components.Segmented
import app.rewind.ui.components.TopBar
import app.rewind.ui.ctx
import app.rewind.ui.theme.U
import app.rewind.ui.theme.UType
import kotlinx.coroutines.delay

/** Looking a title up in the free catalogues; a tap takes it back to the form. */
@Composable
fun SearchScreen(h: Home, go: Go) {
    val c = ctx()
    var kind by rememberSaveable { mutableStateOf(Search.kind) }
    var query by rememberSaveable { mutableStateOf(Search.query) }
    var hits by remember { mutableStateOf<List<Draft>?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(kind, query) {
        if (query.trim().length < 2) {
            hits = null
            return@LaunchedEffect
        }
        // Wait for the typing to stop.
        delay(450)
        busy = true
        hits = Catalog.search(kind, query, h.ru)
        busy = false
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        TopBar(stringResource(R.string.find_in_catalog), go::back)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Segmented(Kind.entries, kind, { T.kind(c, it) }, { kind = it })
            Spacer(Modifier.height(10.dp))
            Field(query, { query = it }, stringResource(R.string.search_hint))
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            if (busy) item { CircularProgressIndicator(Modifier.padding(24.dp), color = U.c.neon) }
            val list = hits
            if (!busy && list != null && list.isEmpty()) item {
                Text(stringResource(R.string.search_none), style = UType.Muted, modifier = Modifier.padding(16.dp))
            }
            items(list.orEmpty()) { d ->
                Hit(d) {
                    c.container.draft.value = d
                    go.back()
                }
            }
            item { Text(stringResource(R.string.search_sources), style = UType.Small, modifier = Modifier.padding(16.dp).fillMaxWidth()) }
        }
    }
}
