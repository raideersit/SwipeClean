# Etapa 3 — Room y repositorio

El filtrado de fotos ya revisadas es lo que hace que la app sirva a largo plazo.

```
Agrega la persistencia y el repositorio que une MediaStore + Room.

1. Entidad ReviewedMediaEntity: mediaId (Long, PK), decision (enum KEPT/DELETED
   guardado como String), reviewedAt (Long), sizeBytes (Long).
2. Entidad SessionStatsEntity: id autogenerado, fecha, fotosEliminadas,
   bytesLiberados.
3. DAOs con las consultas necesarias, incluyendo:
   - SELECT mediaId FROM reviewed_media  (para filtrar)
   - Borrado de todo el historial (para el reset en ajustes)
   - SUM(bytesLiberados) como Flow, para el contador global
4. AppDatabase con TypeConverters y la migración inicial.
5. Módulo Hilt que provee la base de datos y los DAOs.
6. MediaRepository (interfaz en domain, implementación en data):
   - getBuckets(): Flow<List<MediaBucket>>
   - getMediaPage(filtro, offset, limit): List<MediaItem>, ya EXCLUYENDO los
     mediaId presentes en reviewed_media. El filtrado se hace en la consulta,
     no en memoria.
   - markReviewed(mediaId, decision, sizeBytes)
   - undoLastReview(mediaId)
   - recordSession(fotos, bytes)
   - observeTotalFreedBytes(): Flow<Long>
   - clearHistory()

Importante: si el usuario borra una foto por fuera de la app, el registro en
reviewed_media queda huérfano. Agrega una limpieza que elimine registros cuyos
mediaId ya no existan en MediaStore, ejecutada al iniciar la app.
```
