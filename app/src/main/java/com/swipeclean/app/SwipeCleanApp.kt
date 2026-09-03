package com.swipeclean.app

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.swipeclean.app.domain.repository.MediaRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SwipeCleanApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var mediaRepository: MediaRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
}
