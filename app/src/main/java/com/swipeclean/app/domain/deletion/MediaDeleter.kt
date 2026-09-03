package com.swipeclean.app.domain.deletion

import android.content.IntentSender
import android.net.Uri

/**
 * Borrado en lote de fotos de la galería.
 *
 * Contrato pensado para que los ViewModels no toquen nada de `Activity`:
 *
 * 1. El ViewModel llama a [prepare] con TODAS las Uris de la sesión y recibe un
 *    [DeletionRequest].
 * 2. Si es [DeletionRequest.Confirm], el ViewModel emite el `intentSender` como
 *    evento; la UI lo lanza con `StartIntentSenderForResult` (ver
 *    `rememberDeletionLauncher`) y devuelve el [DeletionResult].
 * 3. Si es [DeletionRequest.Completed], no hubo diálogo del sistema y el resultado
 *    ya está: equivale a un [DeletionResult.Confirmed].
 *
 * Regla crítica: en API 30+ es UNA sola llamada con toda la lista, un único
 * diálogo del sistema. Nunca foto por foto.
 */
interface MediaDeleter {

    /**
     * Prepara el borrado de [uris]. Las Uris que ya no existen en MediaStore se
     * ignoran sin fallar.
     *
     * @param permanent `true` usa `createDeleteRequest` (borrado definitivo);
     *   `false` (por defecto) usa `createTrashRequest` (papelera del sistema).
     * @throws Exception si la preparación falla de forma irrecuperable; el llamante
     *   debe traducirlo a [DeletionResult.Failed].
     */
    suspend fun prepare(uris: List<Uri>, permanent: Boolean = false): DeletionRequest
}

/** Qué debe hacer la UI tras [MediaDeleter.prepare]. */
sealed interface DeletionRequest {

    /**
     * Hay que lanzar [intentSender] y esperar la confirmación del usuario.
     * [targetUris] son las Uris que el diálogo va a afectar (ya filtradas).
     */
    data class Confirm(
        val intentSender: IntentSender,
        val targetUris: List<Uri>,
    ) : DeletionRequest

    /**
     * El borrado ya se ejecutó sin diálogo (ruta directa de API 29 y menos, o no
     * quedaba ninguna Uri válida). [deletedUris] puede venir vacía.
     */
    data class Completed(val deletedUris: List<Uri>) : DeletionRequest
}

/** Desenlace del borrado. La UI lo produce a partir del resultado del IntentSender. */
sealed interface DeletionResult {

    /** El usuario confirmó. Solo aquí se marca como eliminado y se suma el espacio. */
    data class Confirmed(val deletedUris: List<Uri>) : DeletionResult

    /** El usuario canceló el diálogo del sistema. No se marca ni se suma nada. */
    data object Cancelled : DeletionResult

    /** Falló al preparar o al lanzar la petición. */
    data class Failed(val cause: Throwable) : DeletionResult
}
