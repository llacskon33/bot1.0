# Compilar Droid Windows

## Android Studio

Abre el repositorio y selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Gradle

```sh
gradle --no-daemon :app:assembleDebug
```

El APK instalable de depuración se encuentra en `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

El workflow **Android debug APK** compila en cada push, pull request y ejecución manual. Descarga el artefacto `droid-windows-debug-apk` desde la ejecución de Actions.
