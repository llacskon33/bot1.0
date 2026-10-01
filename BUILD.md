# Compilar Droid Windows

## Android Studio

Abre el repositorio y selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**. La compilación descarga Chaquopy y crea un APK con Python 3.11 para `arm64-v8a` y `armeabi-v7a`.

## Gradle

Requiere Android SDK, Java 17 y Gradle 8.7:

```sh
gradle --no-daemon :app:assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. Chaquopy instala paquetes compatibles en tiempo de compilación; `pip install` dentro de la app no está habilitado.

## GitHub Actions e instalación

El workflow **Android debug APK** compila en cada push, pull request y ejecución manual. En **Actions**, abre la ejecución completada y descarga el artefacto `droid-windows-debug-apk`. Descomprímelo, transfiere el APK a Android y ábrelo para instalarlo. Si Android lo solicita, permite la instalación desde la aplicación que abrió el archivo.
