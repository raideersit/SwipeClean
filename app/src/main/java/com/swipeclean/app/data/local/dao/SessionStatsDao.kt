package com.swipeclean.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.swipeclean.app.data.local.entity.SessionStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStatsDao {

    @Insert
    suspend fun insert(entity: SessionStatsEntity)

    /** Suma de bytes liberados en todas las sesiones. `COALESCE` para devolver 0 sin filas. */
    @Query("SELECT COALESCE(SUM(bytesLiberados), 0) FROM session_stats")
    fun observeTotalFreedBytes(): Flow<Long>

    /** Reset total de estadísticas (ajustes). */
    @Query("DELETE FROM session_stats")
    suspend fun clearAll()
}
