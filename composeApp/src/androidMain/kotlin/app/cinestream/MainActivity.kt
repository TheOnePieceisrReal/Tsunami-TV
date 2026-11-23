package app.cinestream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.cinestream.data.repo.*
import app.cinestream.ui.App
import io.ktor.client.HttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.ctx = applicationContext
        val db = createDb(); val repo = PlaylistRepository(db, HttpClient())
        setContent { App(db, repo) }
    }
}
