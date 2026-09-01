# Etapa 4 — Permisos y borrado del sistema

**Punto de control obligatorio.** Prueba el borrado con dos o tres fotos en un
emulador antes de pasar a la etapa 5. Si el `createTrashRequest` está mal armado,
descubrirlo con 300 líneas de UI encima cuesta mucho más.

```
Implementa el manejo de permisos y la eliminación en lote. Esta es la parte
más delicada, hazla exacta.

1. PermissionState / helper que resuelva qué permisos pedir según la versión:
   - API 33+: READ_MEDIA_IMAGES (+ READ_MEDIA_VIDEO si se incluyen videos)
   - API 32 y menos: READ_EXTERNAL_STORAGE
   - API 34+: detectar acceso PARCIAL (READ_MEDIA_VISUAL_USER_SELECTED
     concedido pero no READ_MEDIA_IMAGES) y exponerlo como un estado propio.
2. Pantalla de onboarding: explicación breve, botón para conceder, estado
   denegado con botón que abre los ajustes de la app vía Intent
   ACTION_APPLICATION_DETAILS_SETTINGS.
3. Banner reutilizable para acceso parcial: "Estás viendo solo las fotos que
   seleccionaste" + botón para ampliar la selección.
4. MediaDeleter:
   - API 30+: MediaStore.createTrashRequest(resolver, listaDeUris, true)
     lanzado con ActivityResultContracts.StartIntentSenderForResult.
     TODAS las Uris en UNA sola llamada — un único diálogo del sistema.
   - Opción alternativa createDeleteRequest() cuando el usuario activa borrado
     permanente en ajustes.
   - API 29 y menos: ContentResolver.delete() directo, capturando
     RecoverableSecurityException y relanzando el IntentSender.
   - El resultado debe distinguir tres casos: confirmado, cancelado por el
     usuario, y error. Si el usuario cancela, NO se marca nada como eliminado
     ni se suma al contador de espacio liberado.
   - Ignorar sin fallar las Uris de archivos que ya no existen.
5. Expón todo esto como un contrato que los ViewModels puedan usar sin conocer
   detalles de Activity: el ViewModel emite un evento con la lista de Uris y la
   UI se encarga de lanzar el IntentSender y devolver el resultado.
```
