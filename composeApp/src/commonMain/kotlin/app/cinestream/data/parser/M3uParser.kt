package app.cinestream.data.parser

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line

enum class ContentType { LIVE, MOVIE, SERIES }

data class ParsedChannel(
    val name: String,
    val url: String,
    val tvgId: String?,
    val logo: String?,
    val group: String?,
    val userAgent: String?,
    val type: ContentType,
)

/** Streaming line-by-line M3U/M3U8 parser; entries are emitted as soon as they complete. */
class M3uParser {
    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    suspend fun parse(source: ByteReadChannel, onChannel: suspend (ParsedChannel) -> Unit): Int {
        var count = 0
        val state = LineState()
        while (true) {
            val line = source.readUTF8Line() ?: break
            val entry = state.feed(line.trim()) ?: continue
            onChannel(entry); count++
        }
        return count
    }

    inner class LineState {
        private var name: String? = null
        private var attrs: Map<String, String> = emptyMap()
        private var group: String? = null
        private var userAgent: String? = null

        fun feed(line: String): ParsedChannel? {
            when {
                line.isEmpty() || line.startsWith("#EXTM3U") -> {}
                line.startsWith("#EXTINF") -> {
                    attrs = attrRegex.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    name = line.substringAfterLast(',', "").trim().ifEmpty { attrs["tvg-name"] }
                    group = attrs["group-title"]
                }
                line.startsWith("#EXTGRP:") -> group = line.removePrefix("#EXTGRP:").trim()
                line.startsWith("#EXTVLCOPT:http-user-agent=") ->
                    userAgent = line.substringAfter("http-user-agent=").trim()
                line.startsWith("#") -> {}
                else -> {
                    val n = name
                    if (n != null) {
                        val out = ParsedChannel(
                            name = n, url = line,
                            tvgId = attrs["tvg-id"]?.ifBlank { null },
                            logo = attrs["tvg-logo"]?.ifBlank { null },
                            group = group?.ifBlank { null },
                            userAgent = userAgent,
                            type = classify(line, group),
                        )
                        name = null; attrs = emptyMap(); group = null; userAgent = null
                        return out
                    }
                }
            }
            return null
        }
    }

    private fun classify(url: String, group: String?): ContentType {
        val u = url.lowercase(); val g = group?.lowercase().orEmpty()
        return when {
            "/movie/" in u || "vod" in g || "movie" in g -> ContentType.MOVIE
            "/series/" in u || "series" in g -> ContentType.SERIES
            else -> ContentType.LIVE
        }
    }
}
