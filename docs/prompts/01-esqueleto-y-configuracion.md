# Etapa 1 — Esqueleto y configuración

**Antes de seguir:** el proyecto debe compilar y abrir con un Scaffold vacío.

```
Crea la base del proyecto SwipeClean (ver contexto arriba). Solo configuración,
todavía sin lógica ni pantallas.

Entrega:
1. Árbol completo de carpetas y paquetes bajo com.swipeclean.app, separando
   data/ (local, mediastore, repository), domain/ (model, repository, usecase),
   ui/ (theme, navigation, screens, components), di/.
2. libs.versions.toml con todas las dependencias y versiones concretas
   (Compose BOM, Hilt, Room con KSP, Coil 3, Navigation Compose,
   accompanist-permissions o la alternativa vigente).
3. build.gradle.kts de proyecto y de módulo app, con KSP y Hilt configurados.
4. AndroidManifest.xml completo:
   - READ_MEDIA_IMAGES (33+), READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED
     (34+), READ_EXTERNAL_STORAGE con maxSdkVersion="32".
   - Application class con @HiltAndroidApp.
   - MainActivity con @AndroidEntryPoint.
5. Tema Material 3 con soporte claro/oscuro y dynamic color, paleta pensada
   para visor de fotos (fondo muy oscuro, acentos rojo/verde para las
   decisiones).
6. MainActivity con enableEdgeToEdge() y un Scaffold vacío que compile.

No inventes APIs. Si una dependencia cambió de nombre o coordenada, usa la
vigente y explícalo en una línea.
```
