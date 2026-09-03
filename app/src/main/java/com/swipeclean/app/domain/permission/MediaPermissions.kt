package com.swipeclean.app.domain.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.swipeclean.app.domain.model.MediaAccessLevel

/**
 * Resuelve qué permisos de lectura de medios pedir y en qué [MediaAccessLevel] está
 * la app, encapsulando las diferencias entre versiones de Android.
 *
 * Es un helper puro: solo lee el estado de permisos vía [ContextCompat]. La noción
 * de "denegado permanentemente" (que exige `shouldShowRequestPermissionRationale`,
 * ligado a una `Activity`) se resuelve en la capa de UI, no aquí.
 */
object MediaPermissions {

    /**
     * Permisos a solicitar según la versión:
     * - API 34+: `READ_MEDIA_IMAGES` (+ `READ_MEDIA_VIDEO`) y además
     *   `READ_MEDIA_VISUAL_USER_SELECTED` para habilitar la opción "Seleccionar fotos".
     * - API 33: `READ_MEDIA_IMAGES` (+ `READ_MEDIA_VIDEO`).
     * - API 32 y menos: `READ_EXTERNAL_STORAGE`, el único disponible.
     *
     * @param includeVideos añade el permiso de video en API 33+. Por defecto `true`,
     *   coherente con la capa de datos que consulta imágenes y videos juntos.
     */
    fun required(includeVideos: Boolean = true): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            if (includeVideos) add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }.toTypedArray()

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> buildList {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            if (includeVideos) add(Manifest.permission.READ_MEDIA_VIDEO)
        }.toTypedArray()

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** Nivel de acceso vigente, leído directamente del sistema. */
    fun accessLevel(context: Context): MediaAccessLevel = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> when {
            granted(context, Manifest.permission.READ_MEDIA_IMAGES) -> MediaAccessLevel.FULL
            granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccessLevel.PARTIAL
            else -> MediaAccessLevel.NONE
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            if (granted(context, Manifest.permission.READ_MEDIA_IMAGES)) {
                MediaAccessLevel.FULL
            } else {
                MediaAccessLevel.NONE
            }

        else ->
            if (granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                MediaAccessLevel.FULL
            } else {
                MediaAccessLevel.NONE
            }
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
