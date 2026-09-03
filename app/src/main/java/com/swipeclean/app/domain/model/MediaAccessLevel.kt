package com.swipeclean.app.domain.model

/**
 * Nivel de acceso a la galería que el sistema le ha concedido a la app.
 *
 * - [FULL]: acceso a todas las fotos (y videos). Es el estado normal de trabajo.
 * - [PARTIAL]: solo API 34+. El usuario concedió `READ_MEDIA_VISUAL_USER_SELECTED`
 *   pero no `READ_MEDIA_IMAGES`: la app solo ve las fotos que él seleccionó a mano.
 *   La UI lo trata como un estado propio (banner + opción de ampliar la selección).
 * - [NONE]: sin ningún permiso de medios. Hay que mostrar el onboarding.
 */
enum class MediaAccessLevel {
    FULL,
    PARTIAL,
    NONE,
}
