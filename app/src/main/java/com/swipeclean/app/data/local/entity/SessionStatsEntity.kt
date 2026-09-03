package com.swipeclean.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estadísticas de una sesión de revisión terminada. La suma de [bytesLiberados]
 * sobre todas las filas alimenta el contador global de espacio liberado.
 *
 * [fecha] es epoch en milisegundos.
 */
@Entity(tableName = "session_stats")
data class SessionStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long,
    val fotosEliminadas: Int,
    val bytesLiberados: Long,
)
