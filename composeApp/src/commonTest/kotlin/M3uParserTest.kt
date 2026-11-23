import app.cinestream.data.parser.*
import kotlin.test.*

class M3uParserTest {
    @Test fun parsesEntry() {
        val s = M3uParser().LineState()
        assertNull(s.feed("""#EXTINF:-1 tvg-id="a.b" tvg-logo="http://x/l.png" group-title="News",Channel One"""))
        val c = s.feed("http://host/live/u/p/1.ts")!!
        assertEquals("Channel One", c.name); assertEquals("News", c.group); assertEquals(ContentType.LIVE, c.type)
    }
}
