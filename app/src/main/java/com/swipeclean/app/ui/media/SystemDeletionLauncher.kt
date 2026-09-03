package com.swipeclean.app.ui.media

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.swipeclean.app.domain.deletion.DeletionRequest
import com.swipeclean.app.domain.deletion.DeletionResult

/**
 * Puente entre el evento de borrado que emite un ViewModel y el diálogo del
 * sistema. El ViewModel no conoce nada de `Activity`: emite [DeletionRequest] y
 * esta capa lo lanza y devuelve el [DeletionResult].
 *
 * Uso típico en una pantalla:
 * ```
 * val deletionLauncher = rememberDeletionLauncher(onResult = viewModel::onDeletionResult)
 * LaunchedEffect(Unit) {
 *     viewModel.deletionRequests.collect(deletionLauncher::launch)
 * }
 * ```
 */
class DeletionLauncher internal constructor(
    private val onRequest: (DeletionRequest) -> Unit,
) {
    fun launch(request: DeletionRequest) = onRequest(request)
}

@Composable
fun rememberDeletionLauncher(onResult: (DeletionResult) -> Unit): DeletionLauncher {
    val currentOnResult by rememberUpdatedState(onResult)
    // Uris que afectará el diálogo en curso; se recupera en el callback del contrato.
    var targetUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val outcome = if (result.resultCode == Activity.RESULT_OK) {
            DeletionResult.Confirmed(targetUris)
        } else {
            // El usuario canceló el diálogo: no se marca nada como eliminado.
            DeletionResult.Cancelled
        }
        currentOnResult(outcome)
    }

    return remember(launcher) {
        DeletionLauncher { request ->
            when (request) {
                is DeletionRequest.Confirm -> {
                    targetUris = request.targetUris
                    try {
                        launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    } catch (e: Exception) {
                        currentOnResult(DeletionResult.Failed(e))
                    }
                }

                is DeletionRequest.Completed ->
                    currentOnResult(DeletionResult.Confirmed(request.deletedUris))
            }
        }
    }
}
