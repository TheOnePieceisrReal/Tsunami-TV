package app.cinestream.data.repo

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cinestream.db.CineDb

object AppContext { lateinit var ctx: Context }

actual fun createDriver(): SqlDriver = AndroidSqliteDriver(CineDb.Schema, AppContext.ctx, "cine.db")
