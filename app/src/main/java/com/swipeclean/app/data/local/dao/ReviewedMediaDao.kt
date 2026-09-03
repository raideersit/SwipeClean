package com.swipeclean.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swipeclean.app.data.local.entity.ReviewedMediaEntity

@Dao
interface ReviewedMediaDao {

    /** Alta o reemplazo del registro de una foto revisada. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReviewedMediaEntity)

    /** IDs ya decididos, para excluirlos de la consulta a MediaStore. */
    @Query("SELECT mediaId FROM reviewed_media")
    suspend fun getAllReviewedIds(): List<Long>

    /** Deshacer una decisión concreta. */
    @Query("DELETE FROM reviewed_media WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)

    /** Reset total del historial (ajustes). */
    @Query("DELETE FROM reviewed_media")
    suspend fun clearAll()

    /**
     * Elimina los registros cuyo `mediaId` no está en [existingMediaIds] (los que
     * siguen existiendo en MediaStore). Limpieza de huérfanos al iniciar la app.
     */
    @Query("DELETE FROM reviewed_media WHERE mediaId NOT IN (:existingMediaIds)")
    suspend fun deleteOrphans(existingMediaIds: List<Long>)
}
