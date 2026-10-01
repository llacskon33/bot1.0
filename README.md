# Bot 1.0 - Android

## Estado actual
- La app solicita permiso del usuario para capturar la pantalla mediante MediaProjection.
- Un servicio en primer plano procesa fotogramas capturados y los envía al flujo de decisiones del bot.
- La visión y los controles del juego siguen pendientes: `VisionEngine` aún no detecta elementos y no se automatizan toques.

Este proyecto es una base Android independiente inspirada en las funciones de Pyla-RL; no contiene código del proyecto enlazado. No es todavía un bot jugable. Para completarlo hacen falta detección visual específica del juego y un mecanismo de control compatible con Android.

## Compilación
Abre el proyecto en Android Studio y selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
