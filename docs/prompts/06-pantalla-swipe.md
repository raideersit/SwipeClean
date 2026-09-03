# Etapa 6 — Pantalla de swipe

```
Implementa la pantalla principal de deslizamiento. Es el corazón de la app.

SwipeViewModel:
- Carga paginada: mantiene un buffer, pide la siguiente página cuando quedan
  menos de 20 elementos por revisar.
- Estado: foto actual, siguientes 2 para el apilado, índice actual, total,
  cantidad y bytes marcados para eliminar.
- Stack de historial de al menos 10 decisiones para deshacer.
- Sobrevive a rotación con SavedStateHandle.
- Las decisiones se guardan en Room al momento (KEPT queda firme; DELETED es
  provisional hasta confirmar en el resumen).

SwipeScreen:
- Foto a pantalla completa, ContentScale.Fit, fondo casi negro.
- Máximo 3 cartas apiladas: la activa arriba, las de atrás con leve escala y
  desplazamiento vertical.
- Gesto con detectHorizontalDragGestures sobre un Modifier.offset animado:
  · rotación proporcional al desplazamiento, tope 15°
  · umbral de decisión: 30% del ancho o velocidad de fling alta
  · si no supera el umbral, vuelve al centro con animación de resorte
  · si lo supera, sale de pantalla en la dirección correspondiente
- Overlays de feedback: rojo con ícono de basurero a la izquierda, verde con
  check a la derecha, alpha proporcional al avance del arrastre.
- Háptico corto (HapticFeedbackType.LongPress) al cruzar el umbral.
- Precarga de las siguientes 3 imágenes con Coil (ImageRequest sin destino
  visible) para evitar parpadeo.
- Top bar: contador "12 / 214", peso marcado para borrar, botón de cerrar.
- Botonera inferior: eliminar, deshacer, conservar. Deben ejecutar exactamente
  la misma lógica que el gesto, con las mismas animaciones.
- Todos los controles con contentDescription; la pantalla debe ser usable solo
  con los botones para quien no pueda hacer el gesto.
- Al agotarse los elementos, navega al resumen.

Extrae la lógica del gesto a un componente SwipeableCard reutilizable y con
preview propia.
```
