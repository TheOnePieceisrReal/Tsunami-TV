package app.cinestream.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinestream.data.parser.XtreamHelper
import app.cinestream.data.repo.PlaylistRepository
import app.cinestream.db.CineDb
import app.cinestream.db.Channel
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF141414); private val Card = Color(0xFF1F1F1F)
private val Muted = Color(0xFFB3B3B3); private val Red = Color(0xFFE50914)

@Composable
fun App(db: CineDb, repo: PlaylistRepository) {
    val q = db.schemaQueries
    var playlistId by remember { mutableStateOf(q.allPlaylists().executeAsList().firstOrNull()?.id) }
    var playing by remember { mutableStateOf<Channel?>(null) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Bg, surface = Card)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            val pid = playlistId
            when {
                pid == null -> SetupScreen(repo) { playlistId = it }
                playing != null -> PlayerScreen(playing!!) { playing = null }
                else -> HomeScreen(db, pid, onPlay = { playing = it }, onReset = {
                    q.clearChannels(pid); q.deletePlaylist(pid); playlistId = null
                })
            }
        }
    }
}

@Composable
fun SetupScreen(repo: PlaylistRepository, onDone: (Long) -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val scope = rememberCoroutineScope()
    var xtream by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("My IPTV") }
    var url by remember { mutableStateOf("") }; var epg by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }; var status by remember { mutableStateOf("") }
    Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TSUNAMI TV", color = Red, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("Bring your own playlist. Nothing is preloaded.", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!xtream, { xtream = false }, { Text("M3U URL") })
            FilterChip(xtream, { xtream = true }, { Text("Xtream Codes") })
        }
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(url, { url = it }, label = { Text(if (xtream) "Server URL (http://host:port)" else "M3U URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (xtream) {
            OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        } else OutlinedTextField(epg, { epg = it }, label = { Text("EPG (XMLTV) URL – optional") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && url.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Red), onClick = {
            busy = true; status = "Loading…"
            scope.launch {
                try {
                    val m3u = if (xtream) XtreamHelper.m3uUrl(url, user, pass) else url.trim()
                    val e = if (xtream) XtreamHelper.epgUrl(url, user, pass) else epg.ifBlank { null }
                    val id = repo.addPlaylist(name, if (xtream) "XTREAM" else "M3U", url.trim(), user.ifBlank { null }, pass.ifBlank { null }, e)
                    repo.refreshChannels(id, m3u) { status = "Imported $it channels…" }
                    onDone(id)
                } catch (t: Throwable) { status = "Failed: ${t.message}"; busy = false }
            }
        }) { Text(if (busy) "Please wait" else "Connect") }
        Text(status, color = Muted)
    }
}


private val sections = listOf("Home" to null, "Live TV" to "LIVE", "Movies" to "MOVIE", "Series" to "SERIES")

@Composable
fun HomeScreen(db: CineDb, pid: Long, onPlay: (Channel) -> Unit, onReset: () -> Unit) {
    val q = db.schemaQueries
    var section by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var hero by remember { mutableStateOf<Channel?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val rows by produceState<List<Pair<String, List<Channel>>>>(emptyList(), section, query) {
        value = withContext(Dispatchers.Default) {
            if (query.isNotBlank()) listOf("Results" to q.searchChannels(pid, query).executeAsList())
            else {
                val types = sections[section].second?.let { listOf(it) } ?: listOf("LIVE", "MOVIE", "SERIES")
                val progress = q.continueWatching(pid).executeAsList()
                val cont = progress.map { Channel(it.id, it.playlistId, it.name, it.streamUrl, it.tvgId, it.logo, it.groupTitle, it.contentType, it.userAgent, it.isFavorite) }
                (if (cont.isNotEmpty() && section == 0) listOf("Continue Watching" to cont) else emptyList()) +
                    types.flatMap { t -> q.groupsByType(pid, t).executeAsList().filterNotNull().take(if (sections[section].second == null) 4 else 60)
                        .map { g -> g to q.channelsByGroup(pid, t, g, 40).executeAsList() } }
            }
        }
    }
    LaunchedEffect(rows) { if (hero == null) hero = rows.firstOrNull()?.second?.firstOrNull() }

    Row(Modifier.fillMaxSize()) {
        // Collapsible sidebar: expands when any child has focus
        Column(Modifier.width(if (expanded) 200.dp else 64.dp).fillMaxHeight().background(Color(0xCC000000))
            .onFocusChanged { expanded = it.hasFocus }.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sections.forEachIndexed { i, (label, _) ->
                Focusable(Modifier.fillMaxWidth(), onClick = { section = i; query = "" }) { f ->
                    Text(if (expanded) label else label.take(1), color = if (section == i || f) Color.White else Muted,
                        fontWeight = if (section == i) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Focusable(Modifier.fillMaxWidth(), onClick = onReset) { _ -> Text(if (expanded) "Change playlist" else "⚙", color = Muted, modifier = Modifier.padding(12.dp)) }
        }
        Column(Modifier.fillMaxSize()) {
            Hero(hero, onPlay, query) { query = it }
            LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                items(rows) { (title, list) ->
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)) {
                        items(list, key = { it.id }) { ch -> ChannelCard(ch, onFocus = { hero = ch }, onClick = { onPlay(ch) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(ch: Channel?, onPlay: (Channel) -> Unit, query: String, onQuery: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        AsyncImage(ch?.logo, null, Modifier.fillMaxSize().padding(80.dp), contentScale = ContentScale.Fit, alpha = 0.35f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66141414), Color(0xCC141414), Bg))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Bg, Color.Transparent))))
        OutlinedTextField(query, onQuery, placeholder = { Text("Search") }, singleLine = true, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(260.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(ch?.contentType ?: "") ; ch?.groupTitle?.let { Badge(it) }
            }
            Text(ch?.name ?: "Welcome", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
            if (ch != null) Focusable(Modifier.width(140.dp), shape = RoundedCornerShape(4.dp), onClick = { onPlay(ch) }, bg = Red) { _ ->
                Text("▶  Play", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable private fun Badge(t: String) =
    Text(t, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color(0x55FFFFFF), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp))

@Composable
private fun ChannelCard(ch: Channel, onFocus: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f)
    Column(Modifier.width(200.dp).scale(scale)
        .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
        .border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
        .background(Card, RoundedCornerShape(6.dp)).clip6()
        .clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(112.dp).background(Color(0xFF2A2A2A)), contentAlignment = Alignment.Center) {
            AsyncImage(ch.logo, ch.name, Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
        }
        Text(ch.name, color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(8.dp))
        if (focused) Text(ch.groupTitle ?: ch.contentType, color = Muted, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
    }
}
private fun Modifier.clip6() = this.then(Modifier.background(Color.Transparent, RoundedCornerShape(6.dp)))

/** D-pad / keyboard / mouse focusable item. */
@Composable
private fun Focusable(modifier: Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(6.dp), bg: Color = Color.Transparent,
                      onClick: () -> Unit, content: @Composable (Boolean) -> Unit) {
    var f by remember { mutableStateOf(false) }
    Box(modifier.onFocusChanged { f = it.isFocused }.background(if (f) Color(0x33FFFFFF) else bg, shape)
        .border(if (f) 2.dp else 0.dp, Color.White, shape).clickable(onClick = onClick)) { content(f) }
}

@Composable
fun PlayerScreen(ch: Channel, onBack: () -> Unit) {
    val focus = remember { FocusRequester() }
    var osd by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(osd) { if (osd) { kotlinx.coroutines.delay(4000); osd = false } }
    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(focus).focusable()
        .onKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) false
            else when (e.key) {
                Key.Escape, Key.Back, Key.Backspace -> { onBack(); true }
                else -> { osd = true; false }
            }
        }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { osd = true }) {
        VideoPlayer(ch.streamUrl, ch.userAgent, Modifier.fillMaxSize())
        if (osd) Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)))).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Badge("LIVE".takeIf { ch.contentType == "LIVE" } ?: "VOD")
            Text(ch.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f)); Text("Back / Esc to exit", color = Muted, fontSize = 12.sp)
        }
    }
}
