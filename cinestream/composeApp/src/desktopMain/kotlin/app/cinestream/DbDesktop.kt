package app.cinestream.data.repo

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cinestream.db.CineDb
import java.io.File

actual fun createDriver(): SqlDriver {
    val dir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "TsunamiTV").apply { mkdirs() }
    val file = File(dir, "cine.db")
    val fresh = !file.exists()
    return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { if (fresh) CineDb.Schema.create(it) }
}
