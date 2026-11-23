package app.cinestream.data.repo

import app.cinestream.data.parser.M3uParser
import app.cinestream.data.parser.ParsedChannel
import app.cinestream.db.CineDb
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class PlaylistRepository(private val db: CineDb, private val http: HttpClient) {
    private val q get() = db.schemaQueries
    private companion object { const val BATCH = 1000 }

    fun addPlaylist(name: String, type: String, url: String, user: String?, pass: String?, epg: String?): Long =
        db.transactionWithResult {
            q.insertPlaylist(name, type, url, user, pass, epg)
            q.lastInsertId().executeAsOne()
        }

    /** Streams the playlist from the network straight into SQLite in batched transactions. */
    suspend fun refreshChannels(playlistId: Long, m3uUrl: String, onProgress: (Int) -> Unit = {}): Int =
        withContext(Dispatchers.Default) {
            q.clearChannels(playlistId)
            val buffer = ArrayList<ParsedChannel>(BATCH)
            var total = 0
            fun flush() {
                if (buffer.isEmpty()) return
                db.transaction {
                    buffer.forEach { c ->
                        q.insertChannel(playlistId, c.name, c.url, c.tvgId, c.logo, c.group, c.type.name, c.userAgent)
                    }
                }
                total += buffer.size; buffer.clear(); onProgress(total)
            }
            http.prepareGet(m3uUrl) { header("User-Agent", "TsunamiTV/1.0") }.execute { resp ->
                M3uParser().parse(resp.bodyAsChannel()) { c ->
                    buffer += c
                    if (buffer.size >= BATCH) flush()
                }
            }
            flush()
            q.touchPlaylist(Clock.System.now().toEpochMilliseconds(), playlistId)
            total
        }
}
