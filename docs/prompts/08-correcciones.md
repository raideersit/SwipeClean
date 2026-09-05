[# Etapa 8 — Correcciones de robustez

**Punto de control obligatorio.** Los puntos 1 y 2 son bloqueantes: hasta que no
estén resueltos la app crashea en cualquier teléfono real con Android 11 o menos.
Verifica el 1 en un emulador API 30 con más de mil fotos en la galería antes de
tocar nada más.

Esta etapa no agrega funcionalidad. Corrige defectos encontrados en una revisión
del código ya escrito en las etapas 1–7.

```
Vas a corregir una lista de defectos en SwipeClean. Para cada punto: primero
diagnostica leyendo el código real, luego propón el enfoque, y recién después
escribe. No apliques la primera solución que se te ocurra sin haber medido el
costo de las alternativas.

Trabaja los puntos en orden. Después de cada bloque (P0, P1, P2, P3) compila y
reporta el resultado real de ./gradlew assembleDebug.


═══ P0 — CRASH AL ARRANQUE ═══

1. ReviewedMediaDao.deleteOrphans revienta con galerías normales.

   La consulta es:
     @Query("DELETE FROM reviewed_media WHERE mediaId NOT IN (:existingMediaIds)")

   Room expande esa lista a un parámetro de enlace por elemento. Quien la llama
   es MediaRepositoryImpl.pruneOrphanReviews(), que le pasa el resultado de
   MediaStoreDataSource.getAllMediaIds(), o sea TODAS las fotos y videos del
   dispositivo.

   El tope de SQLITE_MAX_VARIABLE_NUMBER es 999 en el SQLite que trae Android
   API 26–30, y 32766 desde API 31. En un teléfono con mil fotos y Android 11 se
   lanza SQLiteException: too many SQL variables.

   Peor aún el momento: pruneOrphanReviews corre en SwipeCleanApp.onCreate()
   dentro de appScope, que es CoroutineScope(SupervisorJob() + Dispatchers.IO)
   sin CoroutineExceptionHandler. La excepción sube al handler del hilo y mata el
   proceso. Y como hay una guarda `if (existingIds.isEmpty()) return`, el primer
   arranque sobrevive (sin permiso la lista viene vacía) y el crash aparece en el
   arranque SIGUIENTE, una vez concedido el permiso. La app queda inarrancable
   hasta borrar datos.

   Qué quiero:
   - Que la limpieza de huérfanos funcione con cualquier tamaño de galería, en
     todo el rango minSdk 26 → targetSdk actual.
   - Evalúa al menos dos enfoques y justifica el elegido: invertir la consulta
     (traer los mediaId del historial, que son muchos menos, y comprobar cuáles
     ya no existen en MediaStore) versus lotear. Ten en cuenta que el historial
     crece sin techo con el uso, así que "es más chico" no es una garantía
     permanente.
   - Independientemente del arreglo: appScope necesita un CoroutineExceptionHandler.
     Una tarea de mantenimiento en background jamás debe poder tumbar onCreate().


═══ P1 — CONTRATO Y CORRECCIÓN ═══

2. En API 26–29 el borrado es SIEMPRE permanente y el ajuste no hace nada.

   README y CLAUDE.md declaran "papelera antes que borrado permanente" como
   decisión de diseño, y el switch de ajustes se llama "Borrado permanente" con
   la advertencia de que las fotos no se podrán recuperar. Pero
   createTrashRequest no existe antes de API 30: MediaDeleterImpl cae a
   ContentResolver.delete() directo, que es definitivo. El valor de
   permanentDelete ni se consulta en esa rama.

   O sea que en Android 8, 9 y 10 el usuario borra irreversiblemente creyendo
   que va a la papelera, con el switch apagado.

   Además en API 26–28 el SecurityException se traga con un Log.w y la foto se
   omite en silencio: el usuario cree que borró y no borró nada.

   Qué quiero: que el comportamiento real y lo que la UI promete coincidan en
   todo el rango de versiones. Decide y justifica: o la UI refleja la limitación
   por versión, o se implementa una papelera propia, o se sube el minSdk. No me
   des las tres, dame la que defiendas y por qué.
   Las Uris omitidas por SecurityException tienen que llegar a la UI de alguna
   forma, no morir en un log.

3. La pantalla de resumen dice "espacio liberado" cuando no se liberó nada.

   createTrashRequest manda a la papelera del sistema: el archivo se queda en
   disco, renombrado a .trashed-<expiración>-<nombre> con IS_TRASHED = 1, y
   expira solo a los ~30 días. Los bytes siguen ocupados. SummaryViewModel.onConfirmed
   los suma a recordSession() como liberados igual.

   Qué quiero: que el mensaje sea honesto en ambos caminos (papelera vs borrado
   definitivo) sin volverse un párrafo de disclaimers.

4. Carrera entre markReviewed y la paginación en SwipeViewModel.

   onDecision lanza `viewModelScope.launch { repository.markReviewed(...) }` sin
   esperarlo, y sigue de inmediato a maybeRefill(). fetchNextPage calcula
   `offset = buffer.size - cursor` asumiendo que todo lo ya decidido está en Room
   y por lo tanto excluido por la cláusula NOT IN de la consulta.

   Si el refill gana la carrera a la escritura, el conjunto de excluidos está
   desactualizado y el offset queda corrido. El HashSet `known` de fetchNextPage
   atrapa los duplicados, pero no las fotos SALTADAS, que es el fallo silencioso:
   el usuario nunca ve esas fotos y no hay forma de que se entere.

   Qué quiero: la invariante "ninguna foto se salta ni se repite" garantizada por
   construcción, no por suerte de scheduling. Y tests unitarios que la cubran
   (ver punto 9).

5. onConfirmed pierde la sesión si el proceso muere durante el diálogo del sistema.

   SummaryViewModel.pendingItems es un campo normal del ViewModel. El comentario
   dice que el enfoque es resistente a rotación — cierto, el ViewModel sobrevive.
   Pero el diálogo de borrado del sistema es otra Activity: si el sistema mata el
   proceso mientras está arriba, al volver pendingItems está vacío, no se registra
   nada en recordSession y forgetReviewed no limpia el historial.

6. deleteWithRecovery (API 29) descarta lo que sí borró.

   Cuando salta RecoverableSecurityException se retorna Confirm(remaining) y la
   lista `deleted` acumulada hasta ese punto se pierde. Revisa si eso importa
   dado que el resumen reverifica contra MediaStore, y si no importa, di por qué
   y deja constancia en un comentario.


═══ P2 — RENDIMIENTO Y ESCALA ═══

7. getAllReviewedIds() se llama en cada consulta y se serializa entera.

   MediaRepositoryImpl lo invoca en getMediaPage, countMedia, getBuckets y
   getMonthGroups. Cada llamada carga la tabla completa y la interpola dentro de
   un `_ID NOT IN (...)` como literales. Con 20.000 fotos ya revisadas eso es una
   cadena de más de 100 KB armada y enviada por Binder a MediaProvider en cada
   página de swipe y en cada refresco de la Home.

   El techo duro está en el tamaño de transacción Binder (~1 MB, unos 140.000
   IDs), inalcanzable en la práctica; el problema es la degradación mucho antes.

   Qué quiero: que el costo por consulta deje de crecer linealmente con el
   historial. Considera cachear el conjunto en memoria invalidándolo por
   escritura, y considera si el filtrado tiene que seguir ocurriendo del lado de
   MediaStore. Ojo: CLAUDE.md dice explícitamente que el filtrado va en la
   consulta SQL y nunca en memoria. Si tu solución contradice esa regla,
   argumenta el cambio y actualiza CLAUDE.md; no la rompas en silencio.

8. La Home reescanea la galería entera sin debounce.

   getBuckets y getMonthGroups recorren el cursor completo sin paginar, y están
   colgados de observeMediaChanges(), que registra un ContentObserver sin
   debounce ni conflate. Un borrado en lote dispara el observer varias veces
   seguidas y encima varios escaneos completos.

   Nota que callbackFlow usa trySend sobre un canal RENDEZVOUS, así que hoy se
   están descartando emisiones por accidente, no por diseño. Hazlo explícito.


═══ P3 — CIERRE ═══

9. Tests. Hoy hay un solo test instrumentado real y los dos de ejemplo del
   template. La lógica más delicada de la app — la aritmética de buffer, cursor
   y offset de SwipeViewModel — no tiene ni una prueba. Cubre como mínimo:
   avanzar, deshacer, refill en el borde de página, y el punto 4 de arriba.

10. screenshotsOnly filtra por LIKE '%Screenshot%'. En un teléfono en español la
    carpeta puede llamarse "Capturas de pantalla" o "Capturas". El filtro no
    encuentra nada y la UI no distingue "no hay capturas" de "el filtro falló".

11. getBuckets asigna como portada el primer elemento del cursor y el comentario
    dice que es "el más reciente". Solo es cierto con sortOrder DATE_DESC; con
    SIZE_DESC es el más pesado. Corrige el código o el comentario.

12. Deriva de documentación: README.md y CLAUDE.md dicen targetSdk 35, el
    build.gradle.kts está en 37. Revisa todo el README y CLAUDE.md contra el
    código real y corrige lo que ya no calce, incluyendo lo que cambies en esta
    etapa.
```]
