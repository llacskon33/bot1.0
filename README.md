# Droid Windows para Android

Aplicación Android nativa con un escritorio inspirado en Windows 11, menú de inicio, explorador para el espacio de archivos privado de la app y una terminal integrada.

## Primera versión

- **Escritorio:** accesos al explorador y la terminal, con barra de tareas y menú Inicio.
- **Explorador:** navega, crea carpetas y archivos, muestra archivos de texto e imágenes compatibles y abre documentos del dispositivo mediante el selector de Android.
- **Terminal:** comandos locales `help`, `pwd`, `ls`, `cd`, `mkdir`, `touch`, `cat`, `echo` y `clear`. Los comandos funcionan solo dentro del espacio privado de Droid Windows; no ejecuta comandos arbitrarios del sistema.
- **Compatibilidad futura:** la interfaz `RuntimeProvider` reserva un punto de extensión para proveedores de Python y de compatibilidad con programas de Windows. Esas funciones aún no están implementadas y no se incluye Wine.

Compatible desde Android 7 (API 24), incluido Android 13. Los archivos creados por la app se guardan en su directorio específico; el acceso a documentos externos se realiza con el selector de archivos de Android.

## Compilar

Abre el proyecto en Android Studio y genera el APK desde **Build > Build Bundle(s) / APK(s) > Build APK(s)**. También se puede compilar con Gradle:

```sh
gradle --no-daemon :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. El workflow **Android debug APK** compila la aplicación en pushes, pull requests y ejecuciones manuales, y publica un artefacto `droid-windows-debug-apk`.
