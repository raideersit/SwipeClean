# SwipeClean

App Android para limpiar la galería con gestos. Deslizar izquierda marca la foto
para eliminar, deslizar derecha la conserva. El borrado se ejecuta en lote al
final de cada sesión.

## Stack

- Kotlin, Jetpack Compose (Material 3), sin XML de layouts
- minSdk 26 / targetSdk 35 / compileSdk 35
- MVVM + Clean Architecture: `data/` · `domain/` · `ui/`
- Hilt, Coroutines + Flow, Room (KSP), Coil 3, MediaStore API
- Package base: `com.swipeclean.app`

## Reglas del proyecto

- Los composables no contienen lógica de negocio. Todo estado viene del
  ViewModel como un único `UiState`.
- Nada de `LiveData`. Solo `StateFlow` / `SharedFlow`.
- Toda consulta a MediaStore corre en `Dispatchers.IO`.
- Acceso a fotos solo por MediaStore y `ContentUris`. Nunca rutas de archivo.
- Los strings van en `strings.xml`, en español. Nada hardcodeado en composables.
- Comentarios en español, y solo donde la lógica no sea evidente.
- Cada composable de pantalla lleva su `@Preview`.

## Restricciones críticas

**Borrado.** En API 30+ se usa `MediaStore.createTrashRequest()` con TODAS las
Uris en una sola llamada, lanzado con `StartIntentSenderForResult`. Nunca borrar
foto por foto: el sistema muestra un diálogo por cada llamada.

Por defecto va a la papelera del sistema (`createTrashRequest`), no borrado
permanente. `createDeleteRequest()` solo si el usuario lo activa en ajustes.

Si el usuario cancela el diálogo del sistema: no se marca nada como eliminado
ni se suma al contador de espacio liberado.

**Historial.** La tabla `reviewed_media` guarda cada `mediaId` ya decidido. El
filtrado de esos IDs se hace en la consulta SQL, nunca en memoria. Sin esto la
app vuelve a mostrar siempre las mismas fotos ya descartadas.

**Permisos.** API 33+ `READ_MEDIA_IMAGES`; API 32 y menos
`READ_EXTERNAL_STORAGE`; API 34+ además manejar acceso parcial
(`READ_MEDIA_VISUAL_USER_SELECTED`) como estado propio de la UI.

## Plan de trabajo

Los prompts por etapa están en `docs/prompts/`, numerados en orden de ejecución.
Una etapa a la vez. No adelantarse a la siguiente sin que la actual compile.

## Comandos

```bash
./gradlew assembleDebug      # compilar
./gradlew installDebug       # instalar en dispositivo/emulador conectado
./gradlew lint               # análisis estático
```

## Qué no hacer

- No dejar funciones con `TODO()` ni pseudocódigo.
- No inventar APIs ni coordenadas de dependencias. Si algo cambió de nombre,
  usar la versión vigente y decirlo.
- No tocar `local.properties` ni el Gradle wrapper.
- No agregar dependencias nuevas sin justificarlo antes.
