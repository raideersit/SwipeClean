package com.swipeclean.app

import android.app.Application
import android.os.Build
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.swipeclean.app.domain.repository.MediaRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SwipeCleanApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var mediaRepository: MediaRepository

    // Una tarea de mantenimiento en background jamás debe poder tumbar onCreate():
    // sin este handler la excepción sube al hilo y mata el proceso.
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            Log.e(TAG, "Fallo en tarea de background del appScope", error)
        },
    )

    override fun onCreate() {
        super.onCreate()
        // Limpia el historial de registros huérfanos: fotos decididas en la app que
        // luego se borraron desde fuera. Sin permiso de medios es un no-op seguro.
        appScope.launch { mediaRepository.pruneOrphanReviews() }
    }

    /**
     * `ImageLoader` global con soporte de GIF animado: `ImageDecoder` en API 28+ y
     * el decoder por software en API 26–27.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

    private companion object {
        const val TAG = "SwipeCleanApp"
    }
}
