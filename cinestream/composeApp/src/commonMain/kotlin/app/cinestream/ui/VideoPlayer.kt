package app.cinestream.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Platform video surface: Media3 ExoPlayer on Android TV, vlcj on Windows. */
@Composable
expect fun VideoPlayer(url: String, userAgent: String?, modifier: Modifier = Modifier)
