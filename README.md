# Droid Windows para Android

Aplicación Android nativa con un escritorio inspirado en Windows 11. Este proyecto mantiene la interfaz de escritorio, el menú Inicio, el explorador y la terminal existentes, y añade una primera fase de Python y gestión de archivos.

## Funciones implementadas

- **Escritorio:** accesos al Explorador, Terminal y Python, barra de tareas y menú Inicio. La interfaz se adapta a móviles; todavía no usa ventanas flotantes ni admite varias ventanas simultáneas.
- **Explorador:** navega por el espacio privado de la aplicación, crea archivos y carpetas, permite renombrar, copiar, mover y eliminar elementos, edita texto y código Python e importa archivos desde el selector de Android.
- **Python:** incluye el intérprete real Python 3.11 para `arm64-v8a` y `armeabi-v7a`. La aplicación permite crear, editar, guardar y ejecutar archivos `.py`; la terminal admite `python --version`, `python -c "..."` y `python archivo.py`.
- **Terminal:** ejecuta operaciones reales del espacio de trabajo con `pwd`, `ls`, `dir`, `cd`, `mkdir`, `touch`, `cat`, `type`, `echo`, redirección `>` y `>>`, `cp`, `mv`, `rm`, `del`, `rmdir`, `python` y `clear`. Los comandos de archivos quedan confinados al espacio privado de Droid Windows; no se ejecuta un shell arbitrario del sistema Android.

Los scripts Python se ejecutan dentro del proceso de la aplicación y heredan los permisos de Android de esta; ejecuta únicamente código de confianza. La ejecución puede consumir CPU y memoria del teléfono.

## Límites y siguientes fases

- `pip install` interactivo no está disponible dentro de la aplicación. Chaquopy incorpora paquetes compatibles durante la compilación; los paquetes deben declararse en Gradle y compilarse junto con la app. Los paquetes con código nativo solo funcionan si existe una distribución compatible con Android y la ABI seleccionada.
- No hay descargas HTTP integradas, gestor de descargas, extracción ZIP ni instalación de APK desde la app todavía. Se pueden importar archivos mediante el selector de Android y abrir formatos para los que haya otra aplicación instalada.
- No se incluye Wine ni QEMU y no se ejecutan programas `.exe`. Ejecutar software de PC requiere emulación/traducción de CPU, acceso al sistema operativo y recursos que no se pueden garantizar en Android; el rendimiento y la compatibilidad variarían mucho según el programa y el dispositivo.

El `minSdk` es Android 7 (API 24), y el `targetSdk` es API 34; Android 13 está incluido. Los archivos se guardan en el directorio específico de la app, por lo que no se solicita acceso general al almacenamiento.

Para incluir un paquete compatible al compilar, añádelo al bloque `python` de `app/build.gradle` y vuelve a generar el APK:

```groovy
python {
    version = "3.11"
    pip {
        install "requests"
    }
}
```

## Compilar y descargar el APK

Con Android Studio, abre el repositorio y selecciona **Build > Build Bundle(s) / APK(s) > Build APK(s)**. También se puede compilar en un entorno configurado con Android SDK y Gradle 8.7:

```sh
gradle --no-daemon :app:assembleDebug
```

El archivo de instalación se genera en `app/build/outputs/apk/debug/app-debug.apk`. El workflow **Android debug APK** compila en cada push y pull request y publica el artefacto `droid-windows-debug-apk`: abre **Actions**, selecciona la ejecución completada y descarga ese artefacto. Descomprímelo, transfiere el APK al teléfono y ábrelo; Android puede solicitar que autorices temporalmente la instalación desde esa fuente. Revisa el origen del APK antes de instalarlo.
