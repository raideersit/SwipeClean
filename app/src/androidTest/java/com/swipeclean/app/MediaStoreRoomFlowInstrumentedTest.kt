package com.swipeclean.app

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swipeclean.app.data.local.AppDatabase
import com.swipeclean.app.data.mediastore.MediaStoreDataSource
import com.swipeclean.app.data.repository.MediaRepositoryImpl
import com.swipeclean.app.domain.model.MediaAccessLevel
import com.swipeclean.app.domain.model.MediaQuery
import com.swipeclean.app.domain.model.ReviewDecision
import com.swipeclean.app.domain.permission.MediaPermissions
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica contra el dispositivo real el flujo completo de las etapas 2 (MediaStore)
 * y 3 (Room + repositorio): paginación sin duplicados entre páginas y exclusión de
 * fotos ya revisadas.
 *
 * Requiere permiso de lectura de medios ya concedido a mano antes de correr el test
 * (no lo solicita: si no hay acceso, el test se salta con [assumeTrue] en vez de
 * fallar).
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreRoomFlowInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var mediaStore: MediaStoreDataSource
    private lateinit var repository: MediaRepositoryImpl

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Base en memoria: aísla el test del historial real que pueda tener el
        // dispositivo, pero ejerce el mismo esquema y DAOs que la app.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        mediaStore = MediaStoreDataSource(context)
        repository = MediaRepositoryImpl(mediaStore, db.reviewedMediaDao(), db.sessionStatsDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun paginaSinDuplicadosYExclusionDeRevisados() = runBlocking {
        assumeTrue(
            "Se necesita permiso de lectura de medios concedido para correr este test",
            MediaPermissions.accessLevel(context) != MediaAccessLevel.NONE,
        )

        val query = MediaQuery()
        val pageSize = MediaQuery.PAGE_SIZE

        // 1. Primera página directa desde MediaStore, sin exclusiones.
        val firstPage = mediaStore.getPage(query, offset = 0, limit = pageSize)
        println("SwipeClean[test]: primera página trae ${firstPage.size} elemento(s)")
        firstPage.take(3).forEach {
            println("SwipeClean[test]:  - ${it.displayName} (${it.sizeBytes} bytes)")
        }
        assumeTrue(
            "La galería del dispositivo está vacía, no se puede verificar la paginación",
            firstPage.isNotEmpty(),
        )

        // 2. Segunda página: no debe repetir ningún _ID de la primera.
        val secondPage = mediaStore.getPage(query, offset = pageSize, limit = pageSize)
        val firstIds = firstPage.map { it.id }.toSet()
        val secondIds = secondPage.map { it.id }.toSet()
        val overlap = firstIds intersect secondIds
        assertTrue("La segunda página repite IDs de la primera: $overlap", overlap.isEmpty())

        // 3. Marcar los tres primeros elementos como KEPT en Room.
        val toKeep = firstPage.take(3)
        toKeep.forEach { item ->
            repository.markReviewed(item.id, ReviewDecision.KEPT, item.sizeBytes)
        }

        // 4. Repetir la primera página a través del repositorio: los tres marcados ya
        // no deben aparecer, porque la exclusión ocurre en la propia consulta SQL.
        val firstPageAfterReview = repository.getMediaPage(query, offset = 0, limit = pageSize)
        val idsAfterReview = firstPageAfterReview.map { it.id }.toSet()
        toKeep.forEach { item ->
            assertTrue(
                "El elemento ${item.id} (${item.displayName}) sigue apareciendo tras marcarlo KEPT",
                item.id !in idsAfterReview,
            )
        }
    }
}
