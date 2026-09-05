# SwipeClean

App Android para limpiar la galería de fotos con gestos: deslizar a la izquierda
marca la foto para eliminar, a la derecha la conserva. El borrado se ejecuta en
lote al final de cada sesión.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- minSdk 30 (Android 11) / targetSdk 37
- MVVM + Clean Architecture (`data` / `domain` / `ui`)
- Hilt, Coroutines + Flow
- Room (historial de fotos ya revisadas)
- Coil 3
- MediaStore API

## Decisiones de diseño

**Borrado en lote.** En Android 11+ no se puede eliminar media ajena sin un
diálogo del sistema. Si se borrara foto por foto al deslizar, aparecería un
popup cada dos segundos. Por eso las decisiones se acumulan y se confirman
todas juntas con una sola llamada a `createTrashRequest()`.

**Papelera antes que borrado permanente.** `createTrashRequest()` manda a la
papelera del sistema (recuperable ~30 días). El borrado definitivo queda como
opción en ajustes.

Esa API no existe antes de Android 11, y sin ella el borrado sería siempre
permanente: ahí se decidió el minSdk 30 en vez de mantener una segunda ruta
destructiva bajo la misma UI. Como la papelera deja el archivo en disco hasta
que el sistema la vacía, el resumen dice "enviaste X a la papelera" y reserva
"liberaste X" para el borrado definitivo.

**Historial de revisadas.** Room guarda cada `mediaId` decidido. Las fotos
conservadas no vuelven a aparecer en sesiones futuras. Sin esto, la app
mostraría siempre las mismas miles de fotos ya descartadas.

## Plan de desarrollo

Los prompts están en `docs/prompts/`, en orden de ejecución:

| Etapa | Archivo | Contenido |
|---|---|---|
| 00 | `00-contexto.md` | Bloque de contexto para sesiones nuevas |
| 01 | `01-esqueleto-y-configuracion.md` | Estructura, Gradle, manifiesto, tema |
| 02 | `02-capa-datos-mediastore.md` | Lectura de galería, paginación, filtros |
| 03 | `03-room-y-repositorio.md` | Persistencia y repositorio |
| 04 | `04-permisos-y-borrado.md` | Permisos por versión y borrado en lote |
| 05 | `05-pantalla-home.md` | Navegación y pantalla principal |
| 06 | `06-pantalla-swipe.md` | Gesto de deslizamiento |
| 07 | `07-resumen-ajustes-y-cierre.md` | Confirmación, ajustes, casos borde |

Ejecutar una etapa a la vez, verificando que compile antes de seguir.
Entre la 04 y la 05 conviene probar el borrado real en un emulador.

## Estado

En desarrollo.
