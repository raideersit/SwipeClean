# Etapa 5 — Navegación y pantalla Home

```
Implementa la navegación y la pantalla Home.

1. NavHost con rutas: onboarding, home, swipe/{filtroSerializado}, summary,
   settings. Usa type-safe navigation con @Serializable.
2. HomeViewModel: expone un UiState (Loading / Empty / Content / NoPermission)
   con la lista de buckets, los grupos por mes/año, y el total liberado
   histórico. Refresca al emitir el ContentObserver.
3. HomeScreen:
   - Header con el espacio liberado acumulado.
   - Fila de chips: "Capturas de pantalla", "Fotos más pesadas",
     "Videos" (si están habilitados).
   - Sección "Carpetas": grid de 2 columnas, cada tarjeta con portada cargada
     con Coil, nombre, cantidad y peso.
   - Sección "Por fecha": lista de meses con cantidad y peso.
   - Cada elemento navega a la pantalla de swipe con su filtro.
   - Icono de ajustes en la top bar.
   - Estados de carga con placeholders (shimmer o similar), no un spinner solo.
   - Estado vacío cuando ya se revisó todo, con mensaje y acceso al reset.

Todos los composables con preview y sin lógica de negocio adentro.
```
