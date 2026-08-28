# Prompt para Claude Code — MVP de prueba (IronQuest)

Copia y pega todo lo que sigue directamente en Claude Code.

---

## Contexto

Necesito una versión **mínima y funcional** de una app Android de trazabilidad de entrenamiento de fuerza, para probarla yo mismo mañana en el gimnasio. No es la versión final del proyecto — es un prototipo de prueba rápida. Prioriza que **compile sin errores y genere un APK instalable** por encima de cualquier pulido visual o funcionalidad extra.

## Alcance — MUY IMPORTANTE, no te salgas de aquí

**Sí incluir:**
- Crear y editar rutinas: nombre + lista de ejercicios, cada uno con series, repeticiones y peso objetivo
- Catálogo simple de ~15-20 ejercicios comunes precargados (pecho, espalda, pierna, brazo, hombro) + opción de agregar un ejercicio personalizado que no esté en la lista
- Iniciar una sesión desde una rutina existente: se muestran los ejercicios de la rutina con sus series **pre-llenadas** con el peso/reps objetivo, y el usuario **confirma cada serie con un toque** o **ajusta el valor** si hizo más o menos de lo planeado (steppers +/-, no teclado). Debe poder agregar una serie extra a un ejercicio si hace falta.
- Hora de inicio y fin de la sesión: se pre-llenan automáticamente (inicio al abrir la sesión, fin al finalizarla), sin pedir captura manual
- Guardar la sesión completa al finalizar
- Historial simple: lista de sesiones pasadas con fecha, rutina y duración

**No incluir bajo ninguna circunstancia — fuera de alcance total para esta versión:**
- Avatar, personaje, gamificación, XP, niveles, rachas
- Metas, PRs, objetivos
- Pantallas de estadísticas o gráficas
- Login, registro o cualquier autenticación (es una app de un solo usuario local, sin cuentas)
- Secuencia rotativa de rutinas por días — por ahora el usuario elige manualmente qué rutina hacer cada vez que abre la app
- Sustitución de ejercicios dentro de una sesión

Si algo no está en la lista de "sí incluir", no lo agregues aunque te parezca fácil o útil. La prioridad absoluta es tener algo instalable y funcional para mañana.

## Stack técnico

- **Java** (no Kotlin), Android Studio
- **Sin backend, sin base de datos** — toda la persistencia es un único archivo JSON en el almacenamiento interno de la app (`context.getFilesDir()`), leído/escrito completo en cada operación. Usa **Gson** (`com.google.code.gson:gson`) para serializar y deserializar.
- `minSdkVersion 26`, `targetSdkVersion` el más reciente estable disponible
- Arquitectura simple a propósito: Activities con Intents entre pantallas está bien. No uses Navigation Component, MVVM, Room, ni Dependency Injection — cualquier cosa que añada superficie de error para un build de una noche, evítala.
- **No es prioridad el estilo visual.** Usa los componentes de Material por defecto, sin necesidad de un tema oscuro ni una paleta de colores personalizada. Que funcione es lo único que importa en esta versión.

## Modelo de datos (JSON)

Usa esta estructura como base para el archivo `datos.json`:

```json
{
  "ejercicios": [
    { "id": "ex1", "nombre": "Press banca", "grupoMuscular": "Pecho" }
  ],
  "rutinas": [
    {
      "id": "r1",
      "nombre": "Pecho y tríceps",
      "ejercicios": [
        { "ejercicioId": "ex1", "series": 4, "repeticiones": 10, "peso": 40.0 }
      ]
    }
  ],
  "sesiones": [
    {
      "id": "s1",
      "rutinaId": "r1",
      "fechaHoraInicio": "2026-08-25T18:03:00",
      "fechaHoraFin": "2026-08-25T18:55:00",
      "ejercicios": [
        {
          "ejercicioId": "ex1",
          "series": [
            { "numero": 1, "peso": 40.0, "repeticiones": 10, "completada": true }
          ]
        }
      ]
    }
  ]
}
```

## Pantallas necesarias

1. **Lista de rutinas** — pantalla inicial. Lista las rutinas creadas, botón para crear una nueva, tocar una rutina inicia una sesión con ella.
2. **Crear/editar rutina** — campo de nombre, agregar ejercicios del catálogo (o uno personalizado) con series/reps/peso objetivo, guardar.
3. **Sesión activa** — la pantalla central de esta prueba. Por cada ejercicio de la rutina, muestra sus series pre-llenadas con check para confirmar o steppers para ajustar. Botón para agregar serie extra. Botón "Finalizar sesión" que guarda todo en el JSON.
4. **Historial** — lista simple de sesiones pasadas (fecha, rutina, duración).

## Entregable final

1. Verifica que el proyecto **compila sin errores** antes de terminar.
2. Genera un **APK de debug instalable** (`./gradlew assembleDebug`, o indica cómo hacerlo desde Android Studio con Build > Build APK(s)).
3. Al final, dame instrucciones claras y en español de cómo instalar ese APK en mi celular (habilitar instalación de orígenes desconocidos y transferir el archivo, o instalar por USB con `adb install`).
