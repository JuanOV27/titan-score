# Titan Score (nombre de trabajo: IronQuest)

App Android de trazabilidad de entrenamiento de fuerza con gamificación progresiva. Repositorio privado de desarrollo: código fuente, historial de cambios y build.

> El nombre oficial del proyecto es **Titan Score**. El paquete y varios identificadores internos todavía usan **IronQuest** como nombre de trabajo hasta completar una versión más pulida; se renombrará más adelante.

## Stack

- Java, Android Studio, Gradle
- `minSdkVersion 26`, `compileSdk`/`targetSdk` más reciente estable
- Sin backend: persistencia en un único `datos.json` (Gson) en almacenamiento interno de la app
- Arquitectura simple: Activities + Intents, sin Navigation Component, Room ni DI

## Estructura

- `app/` — código de la aplicación
- `docs/` — documentos de planeación del proyecto (propuesta académica y prompt original del MVP)

## Compilar

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Publicación

Las versiones compiladas y las notas de cada actualización se publican en el repositorio público
[titan-score-app](https://github.com/JuanOV27/titan-score-app), que aloja la página de descargas.
