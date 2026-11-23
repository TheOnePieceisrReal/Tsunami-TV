package app.cinestream

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import app.cinestream.data.repo.PlaylistRepository
import app.cinestream.data.repo.createDb
import app.cinestream.ui.App
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import io.ktor.client.HttpClient

fun main() = application {
    val db = createDb(); val repo = PlaylistRepository(db, HttpClient())
    Window(onCloseRequest = ::exitApplication, title = "Tsunami TV", icon = BitmapPainter(useResource("logo.png", ::loadImageBitmap)), state = rememberWindowState(placement = WindowPlacement.Maximized)) { App(db, repo) }
}
