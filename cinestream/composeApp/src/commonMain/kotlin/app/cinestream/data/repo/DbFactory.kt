package app.cinestream.data.repo

import app.cash.sqldelight.db.SqlDriver
import app.cinestream.db.CineDb

expect fun createDriver(): SqlDriver
fun createDb(): CineDb = CineDb(createDriver())
