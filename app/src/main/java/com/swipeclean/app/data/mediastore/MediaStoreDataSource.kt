package com.swipeclean.app.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.swipeclean.app.domain.model.MediaBucket
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.domain.model.MediaQuery
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso de solo lectura a la galería vía MediaStore.
 *
 * Toda consulta corre en [Dispatchers.IO]. Los elementos se resuelven a [Uri] de
 * contenido con [ContentUris.withAppendedId]; nunca se exponen rutas de archivo.
 *
 * Se consulta la colección unificada `MediaStore.Files` filtrando por `MEDIA_TYPE`
 * para poder incluir imágenes y videos en una sola query.
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val resolver: ContentResolver get() = context.contentResolver

    /** Colección unificada de archivos del volumen externo (imágenes + videos). */
    private val collectionUri: Uri = MediaStore.Files.getContentUri("external")

    private val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.MIME_TYPE,
    )

    /**
     * Devuelve una página de [limit] elementos a partir de [offset], ordenados por
     * fecha de alta descendente.
     *
     * [excludedIds] son `mediaId` que se descartan en la propia consulta mediante
     * `_ID NOT IN (...)`; nunca se filtran después en memoria.
     */
    suspend fun getPage(
        query: MediaQuery,
        offset: Int,
        limit: Int,
        excludedIds: Collection<Long> = emptyList(),
    ): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val safeOffset = offset.coerceAtLeast(0)
            val safeLimit = limit.coerceAtLeast(0)
            val items = ArrayList<MediaItem>(safeLimit.coerceAtMost(512))
            queryMedia(query, safeLimit, safeOffset, excludedIds)?.use { cursor ->
                val cols = Columns(cursor)
                while (cursor.moveToNext()) {
                    items += cursor.toMediaItem(cols)
                }
            }
            items
        }

    /**
     * Todos los `_ID` de imágenes y videos del volumen externo, sin filtros. Sirve
     * para detectar registros huérfanos en el historial local (fotos que ya no
     * existen en MediaStore).
     */
    suspend fun getAllMediaIds(): List<Long> =
        withContext(Dispatchers.IO) {
            val ids = ArrayList<Long>()
            val idProjection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            )
            resolver.query(collectionUri, idProjection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idColumn)
                }
            }
            ids
        }

    /**
     * Agrupa todos los elementos que cumplen [query] por álbum y devuelve la lista
     * de [MediaBucket] con conteo, peso total y portada (elemento más reciente).
     */
    suspend fun getBuckets(query: MediaQuery): List<MediaBucket> =
        withContext(Dispatchers.IO) {
            val porBucket = LinkedHashMap<Long, MutableBucket>()
            queryMedia(query, limit = null, offset = 0, excludedIds = emptyList())?.use { cursor ->
                val cols = Columns(cursor)
                while (cursor.moveToNext()) {
                    val item = cursor.toMediaItem(cols)
                    // El primer elemento de cada bucket es el más reciente (orden DATE_ADDED DESC).
                    val acc = porBucket.getOrPut(item.bucketId) {
                        MutableBucket(item.bucketId, item.bucketName, item.uri)
                    }
                    acc.cantidad++
                    acc.pesoTotalBytes += item.sizeBytes
                }
            }
            porBucket.values.map {
                MediaBucket(
                    id = it.id,
                    nombre = it.nombre,
                    cantidad = it.cantidad,
                    pesoTotalBytes = it.pesoTotalBytes,
                    coverUri = it.coverUri,
                )
            }
        }

    /**
     * Emite [Unit] cada vez que cambia el contenido de imágenes o videos del
     * volumen externo. El prompt de la etapa pide observar imágenes; se añade
     * también video porque la app maneja ambos tipos.
     */
    fun observeMediaChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        resolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    /**
     * Ejecuta la consulta paginada encapsulando la diferencia de API:
     * - API 30+: `Bundle` de query args con `QUERY_ARG_LIMIT` / `QUERY_ARG_OFFSET`.
     * - API 26-29: `LIMIT`/`OFFSET` embebidos en el `sortOrder`.
     *
     * Con [limit] nulo se devuelve el conjunto completo (sin paginar).
     */
    private fun queryMedia(
        query: MediaQuery,
        limit: Int?,
        offset: Int,
        excludedIds: Collection<Long>,
    ): Cursor? {
        val (selection, selectionArgs) = buildSelection(query, excludedIds)
        val sortColumn = MediaStore.Files.FileColumns.DATE_ADDED

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val args = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(sortColumn))
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
                )
                if (limit != null) {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                }
            }
            resolver.query(collectionUri, projection, args, null)
        } else {
            val sortOrder = buildString {
                append(sortColumn).append(" DESC")
                if (limit != null) {
                    append(" LIMIT ").append(limit).append(" OFFSET ").append(offset)
                }
            }
            resolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)
        }
    }

    /** Construye el `WHERE` y sus argumentos a partir de los filtros de [query]. */
    private fun buildSelection(
        query: MediaQuery,
        excludedIds: Collection<Long>,
    ): Pair<String, Array<String>> {
        val clauses = ArrayList<String>()
        val args = ArrayList<String>()

        val typeColumn = MediaStore.Files.FileColumns.MEDIA_TYPE
        if (query.includeVideos) {
            clauses += "$typeColumn IN (?, ?)"
            args += MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
            args += MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        } else {
            clauses += "$typeColumn = ?"
            args += MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
        }

        query.bucketId?.let {
            clauses += "${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
            args += it.toString()
        }

        // DATE_ADDED se almacena en segundos desde epoch; el rango llega en millis.
        query.dateFromMillis?.let {
            clauses += "${MediaStore.Files.FileColumns.DATE_ADDED} >= ?"
            args += (it / 1000L).toString()
        }
        query.dateToMillis?.let {
            clauses += "${MediaStore.Files.FileColumns.DATE_ADDED} < ?"
            args += (it / 1000L).toString()
        }

        if (query.minSizeBytes > 0L) {
            clauses += "${MediaStore.Files.FileColumns.SIZE} >= ?"
            args += query.minSizeBytes.toString()
        }

        if (excludedIds.isNotEmpty()) {
            // Los `mediaId` son Long generados por MediaStore: interpolarlos como
            // literales es seguro y evita el tope de ~999 argumentos de enlace de
            // SQLite cuando el historial crece.
            clauses += "${MediaStore.Files.FileColumns._ID} NOT IN (${excludedIds.joinToString(",")})"
        }

        return clauses.joinToString(" AND ") to args.toTypedArray()
    }

    private fun Cursor.toMediaItem(cols: Columns): MediaItem {
        val id = getLong(cols.id)
        val mimeType = getStringOrEmpty(cols.mimeType)
        val isVideo = mimeType.startsWith("video/")
        val baseUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return MediaItem(
            id = id,
            uri = ContentUris.withAppendedId(baseUri, id),
            displayName = getStringOrEmpty(cols.name),
            sizeBytes = getLong(cols.size),
            dateAddedMillis = getLong(cols.dateAdded) * 1000L,
            bucketId = getLong(cols.bucketId),
            bucketName = getStringOrEmpty(cols.bucketName),
            mimeType = mimeType,
            isVideo = isVideo,
        )
    }

    private fun Cursor.getStringOrEmpty(columnIndex: Int): String =
        if (isNull(columnIndex)) "" else getString(columnIndex).orEmpty()

    /** Índices de columna resueltos una sola vez por cursor. */
    private class Columns(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val name = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val size = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
        val bucketId = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
        val bucketName =
            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
        val mimeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
    }

    /** Acumulador mutable para agregar un bucket mientras se recorre el cursor. */
    private class MutableBucket(
        val id: Long,
        val nombre: String,
        val coverUri: Uri?,
    ) {
        var cantidad: Int = 0
        var pesoTotalBytes: Long = 0L
    }
}
