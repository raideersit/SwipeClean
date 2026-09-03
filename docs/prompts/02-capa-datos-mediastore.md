# Etapa 2 — Capa de datos: MediaStore

```
Implementa el acceso a la galería vía MediaStore. Solo lectura, sin Room aún.

1. Modelo de dominio MediaItem: id (Long), uri (Uri), displayName, sizeBytes,
   dateAddedMillis, bucketId, bucketName, mimeType, isVideo.
2. Modelo MediaBucket: id, nombre, cantidad, pesoTotalBytes, uri de portada.
3. MediaStoreDataSource:
   - Consulta en Dispatchers.IO con proyección mínima:
     _ID, DISPLAY_NAME, SIZE, DATE_ADDED, BUCKET_ID, BUCKET_DISPLAY_NAME,
     MIME_TYPE.
   - Uris construidos con ContentUris.withAppendedId.
   - Paginación real de a 100 (limit/offset). En API 30+ usa el Bundle de
     query args (QUERY_ARG_LIMIT / QUERY_ARG_OFFSET); en versiones anteriores,
     el sortOrder con LIMIT. Encapsula esa diferencia en una sola función.
   - Filtros: por bucket, por rango de fechas (mes/año), por tamaño mínimo,
     y flag para incluir o excluir videos.
   - Función que agrupe y devuelva la lista de MediaBucket con conteo y peso.
   - ContentObserver expuesto como Flow<Unit> que emite cuando cambia
     MediaStore.Images.Media.EXTERNAL_CONTENT_URI.
4. Utilidad para formatear bytes a "1.2 GB" / "340 MB" en español.

Escribe cada archivo completo con su ruta. Sin TODOs.
```
