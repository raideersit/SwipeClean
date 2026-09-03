package com.swipeclean.app

import android.app.Application
import com.swipeclean.app.domain.repository.MediaRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SwipeCleanApp : Application() {

    @Inject
    lateinit var mediaRepository: MediaRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Limpia el historial de registros huérfanos: fotos decididas en la app que
        // luego se borraron desde fuera. Sin permiso de medios es un no-op seguro.
        appScope.launch { mediaRepository.pruneOrphanReviews() }
    }
}
