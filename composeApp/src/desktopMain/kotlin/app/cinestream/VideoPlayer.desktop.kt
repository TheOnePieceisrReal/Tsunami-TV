package app.cinestream.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

@Composable
actual fun VideoPlayer(url: String, userAgent: String?, modifier: Modifier) {
    val comp = remember { EmbeddedMediaPlayerComponent() }
    SwingPanel(factory = { comp }, modifier = modifier)
    DisposableEffect(url) {
        val opts = listOfNotNull(":network-caching=1500", ":http-reconnect", userAgent?.let { ":http-user-agent=$it" })
        comp.mediaPlayer().media().play(url, *opts.toTypedArray())
        onDispose { comp.mediaPlayer().controls().stop() }
    }
    DisposableEffect(Unit) { onDispose { comp.release() } }
}
