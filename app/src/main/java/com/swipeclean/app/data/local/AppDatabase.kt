package com.swipeclean.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.swipeclean.app.data.local.dao.ReviewedMediaDao
import com.swipeclean.app.data.local.dao.SessionStatsDao
import com.swipeclean.app.data.local.entity.ReviewedMediaEntity
import com.swipeclean.app.data.local.entity.SessionStatsEntity

/**
 * Base de datos local del historial de revisiones y estadísticas de sesión.
 *
 * Versión 1: esquema inicial, sin migraciones. El builder del módulo Hilt queda
 * listo para `addMigrations(...)` cuando el esquema evolucione.
 */
@Database(
    entities = [ReviewedMediaEntity::class, SessionStatsEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reviewedMediaDao(): ReviewedMediaDao

    abstract fun sessionStatsDao(): SessionStatsDao

    companion object {
        const val NAME = "swipeclean.db"
    }
}
