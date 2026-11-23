package app.cinestream.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(url: String, userAgent: String?, modifier: Modifier) {
    val ctx = LocalContext.current
    val player = remember(url) {
        val http = DefaultHttpDataSource.Factory().setUserAgent(userAgent ?: "TsunamiTV/1.0").setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(ctx, DefaultRenderersFactory(ctx).setEnableDecoderFallback(true))
            .setMediaSourceFactory(DefaultMediaSourceFactory(http)).build().apply {
                setMediaItem(MediaItem.fromUri(url)); playWhenReady = true; prepare()
                addListener(object : Player.Listener {      // live-stream auto-retry
                    override fun onPlayerError(error: PlaybackException) {
                        postDelayed({ prepare() }, 2000)
                    }
                })
            }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(modifier = modifier, factory = { PlayerView(it).apply { this.player = player; useController = true } })
}

private fun ExoPlayer.postDelayed(r: () -> Unit, ms: Long) {
    android.os.Handler(applicationLooper).postDelayed(r, ms)
}
