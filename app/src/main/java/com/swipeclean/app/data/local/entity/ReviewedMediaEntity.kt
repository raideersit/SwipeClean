package com.swipeclean.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swipeclean.app.domain.model.ReviewDecision

/**
 * Un `mediaId` ya decidido en alguna sesión. Su presencia en esta tabla es lo que
 * excluye a la foto de futuras consultas.
 *
 * [decision] se persiste como `String` mediante el `TypeConverter` de la base.
 */
@Entity(tableName = "reviewed_media")
data class ReviewedMediaEntity(
    @PrimaryKey val mediaId: Long,
    val decision: ReviewDecision,
    val reviewedAt: Long,
    val sizeBytes: Long,
)
