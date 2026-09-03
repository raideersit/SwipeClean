# Etapa 7 — Resumen, ajustes y cierre

```
Cierra la app con las pantallas finales y el pulido.

1. SummaryScreen:
   - Grid de miniaturas de las fotos marcadas para eliminar.
   - Toque sobre una miniatura la deselecciona (vuelve a KEPT) con feedback
     visual claro.
   - Texto dinámico: "Vas a liberar 340 MB (23 fotos)".
   - Botón "Eliminar" que dispara el flujo de MediaDeleter en lote.
   - Manejo de los tres resultados: confirmado (registra en session_stats y
     muestra éxito), cancelado (vuelve al resumen intacto), error (snackbar).
   - Pantalla de éxito con el espacio liberado y dos acciones: seguir limpiando
     o volver al inicio.

2. SettingsScreen:
   - Switch: papelera del sistema (por defecto) vs. borrado permanente, con
     advertencia clara en el segundo caso.
   - Switch: incluir videos.
   - Tema: claro / oscuro / sistema, persistido con DataStore.
   - Botón "Restablecer historial de revisadas" con diálogo de confirmación.
   - Espacio total liberado y cantidad de fotos eliminadas históricamente.

3. Casos borde, revisa que todos estén cubiertos:
   - Galería vacía, permiso denegado, acceso parcial.
   - Archivo inexistente al eliminar.
   - HEIC, GIF animado, imagen corrupta (placeholder de error en Coil).
   - Rotación de pantalla durante la sesión.
   - Escalado de fuente hasta 200%.
   - Modo oscuro en todas las pantallas.

4. Revisión final: lista los archivos donde quede lógica de negocio dentro de
   composables o llamadas a MediaStore fuera de Dispatchers.IO, y corrígelos.
   Entrega el strings.xml completo en español.
```
