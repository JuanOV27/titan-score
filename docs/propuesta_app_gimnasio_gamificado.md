# IronQuest — Propuesta de Proyecto: App de Trazabilidad de Entrenamiento con Gamificación

> Nombre tentativo — puede ajustarse más adelante.

**Curso:** Móviles 2
**Programa:** Ingeniería de Sistemas
**Institución:** Universidad Popular del Cesar (UPC), Seccional Aguachica
**Autor:** Juan
**Periodo académico:** _(completar)_

---

## 1. Planteamiento del problema

Gran parte de las personas que inician un proceso de entrenamiento físico en el gimnasio lo abandonan en las primeras semanas o meses. Esto ocurre con frecuencia no por falta de herramientas para registrar el entrenamiento, sino por falta de estructura, motivación sostenida y una sensación tangible de progreso.

Actualmente existen múltiples aplicaciones de trazabilidad de entrenamiento (como Strong, Hevy o Jefit) que cumplen bien su función de registro: series, repeticiones, peso, historial. Sin embargo, son herramientas fundamentalmente utilitarias: asumen que el usuario ya tiene la disciplina para abrir la app y registrar cada sesión, pero no hacen nada activo por construir o sostener esa disciplina.

El problema, entonces, no es la ausencia de una herramienta de registro, sino la ausencia de un mecanismo que convierta la constancia en algo gratificante de forma inmediata y visible, más allá de una gráfica que sube con el tiempo.

## 2. Justificación

Este proyecto aborda ese vacío mediante una capa de gamificación construida sobre un sistema de trazabilidad de entrenamiento: en lugar de que el usuario solo vea números y gráficas, su progreso físico real se traduce en la evolución de un personaje dentro de la aplicación.

Desde el punto de vista académico, el proyecto permite cubrir aspectos centrales de un curso de desarrollo móvil: manejo de estado, persistencia de datos, autenticación, visualización de datos, notificaciones y diseño de una lógica de progresión no trivial. Desde el punto de vista personal, es un proyecto con aplicación directa a un hábito real, lo cual facilita hacer pruebas con datos y uso genuinos durante el semestre.

## 3. Objetivos

### 3.1 Objetivo general

Desarrollar una aplicación móvil de trazabilidad de entrenamientos físicos que, mediante mecánicas de gamificación basadas en un personaje evolutivo, fomente la disciplina y la adherencia del usuario a su rutina de ejercicio.

### 3.2 Objetivos específicos

- Diseñar e implementar un módulo de registro de rutinas y sesiones de entrenamiento (ejercicios, series, repeticiones, peso, fecha).
- Diseñar un sistema de progresión (experiencia, niveles, atributos) que traduzca la actividad física registrada en la evolución de un personaje dentro de la app.
- Implementar una representación visual del personaje en etapas discretas, correspondientes al nivel de progreso del usuario.
- Incorporar un sistema de rachas de constancia como refuerzo motivacional adicional.
- Validar la usabilidad y el efecto motivacional percibido del sistema mediante pruebas con usuarios reales.

## 4. Descripción de la solución propuesta

La aplicación permite al usuario crear y seguir rutinas de entrenamiento, registrando cada sesión (ejercicio, series, repeticiones, peso). A partir de esos datos reales, un motor de progresión calcula experiencia (XP) y actualiza los atributos de un personaje dentro de la app (por ejemplo: fuerza, resistencia, disciplina).

A medida que el personaje sube de nivel, cambia su apariencia visual, pasando por etapas discretas de evolución. La gamificación, deliberadamente, vive principalmente en la lógica y los datos (cálculo de XP, niveles, rachas) y no en animación o arte elaborado — esto mantiene el proyecto viable dentro de un semestre sin sacrificar el concepto. Cabe mencionar que el autor cuenta con experiencia previa en modelado y diseño de personajes en Blender, lo que reduce el riesgo asociado a la creación de los assets visuales en la fase correspondiente. El personaje base sigue un estilo simplificado (proporciones cortas, cabeza grande, tipo chibi), elegido por ser visualmente más carismático y por no depender de la altura real del usuario para verse bien.

El registro de entrenamiento sigue un modelo de confirmación rápida, no de captura desde cero: cada serie se pre-llena con los valores planeados de la rutina (repeticiones, peso), y el usuario solo confirma con un toque o ajusta si hizo más o menos de lo planeado. Esto mantiene la precisión de datos que necesita el sistema de XP/PRs/metas sin exigir atención constante al teléfono, y el mismo flujo funciona igual de bien si el usuario confirma serie por serie durante los descansos, o de corrido al terminar toda la sesión — no hace falta construir dos modos distintos.

## 5. Alcance del proyecto

Para evitar que el proyecto se extienda más allá de lo viable en un semestre, el alcance se define explícitamente por fases, con un criterio claro: **toda funcionalidad debe servir directamente al ciclo "registro de entrenamiento → progreso del personaje"; lo que no aporte a ese ciclo queda fuera del alcance base.**

### 5.1 Funcionalidades incluidas

**Fase 1 — Núcleo funcional**
- Registro / inicio de sesión de usuario
- Catálogo base de ejercicios
- Creación y gestión de rutinas (conjunto de ejercicios), organizadas como una secuencia rotativa (Día 1, Día 2, Día 3...) que avanza cada vez que el usuario entrena — no atada a días específicos del calendario, así que un entrenamiento tardío o fuera de orden no requiere ajuste manual. El usuario puede reordenar la secuencia manualmente cuando quiera cambiar la rotación de verdad (no solo por un desvío de un día), y define una frecuencia objetivo semanal (ej. 5 días), independiente del número de rutinas creadas
- Registro de sesiones de entrenamiento (ejercicio, series, repeticiones, peso, fecha), permitiendo sustituir un ejercicio planeado por otro del catálogo o agregar uno no planeado (la sesión no está obligada a espejar la rutina exactamente; se guarda una referencia opcional a qué ejercicio planeado fue sustituido)
- Duración de la sesión: hora de inicio (pre-llenada al abrir Entrenar) y hora de fin (pre-llenada al llegar al Resumen), siguiendo el mismo patrón de confirmar-o-ajustar que las series — evita tanto la fricción de captura manual desde cero como el problema de una duración auto-capturada sin oportunidad de corregirla si el usuario registra todo de corrido al final. El contador visible durante la sesión activa se deriva de la hora de inicio confirmada, sin lógica aparte
- Historial de sesiones

**Fase 2 — Sistema de gamificación (lógica)**
- Cálculo de XP y niveles a partir de las sesiones registradas
- Atributos del personaje (fuerza, resistencia, disciplina)
- Sistema de rachas de constancia: se mantiene mientras no pasen más de 7 días sin registrar una sesión — no exige entrenar todos los días ni cumplir exactamente la frecuencia objetivo esa semana, solo evita huecos largos de inactividad. La frecuencia objetivo queda como dato de planeación aparte, no como condición para conservar la racha
- Gráficas de progreso (peso levantado en el tiempo, frecuencia semanal)
- Sistema de metas: peso objetivo, medida corporal objetivo o PR objetivo en un ejercicio (con repeticiones configurables, sugerido 8 por defecto por seguridad), con seguimiento de progreso mediante una barra normalizada (0-100% sin importar si el valor real sube o baja: |actual−inicial| / |objetivo−inicial|). Al cumplirse, otorga una insignia y un bono de XP hacia el nivel del personaje — reutiliza datos y sistemas ya existentes, no requiere captura de datos nueva

**Fase 3 — Capa visual del personaje**
- Representación gráfica del personaje en etapas discretas según nivel
- Pantalla de perfil / estado del personaje

### 5.2 Funcionalidades fuera de alcance

- ❌ Nutrición y alimentación (dominio de datos distinto; no aporta al ciclo núcleo)
- ❌ Red social, amigos o rankings globales (requiere backend social aparte)
- ❌ Personalización visual profunda del personaje (ropa, accesorios)
- ❌ Integración con wearables o smartwatches
- ❌ Generación de rutinas mediante IA

### 5.3 Posibles mejoras futuras

Si el tiempo lo permite, una vez completado el alcance base:
- Notificaciones push para mantener la racha
- Modo oscuro
- Exportar o compartir el progreso/logro como imagen
- Personaje en 3D reflejando medidas corporales reales (post-semestre): malla low-poly segmentada por partes (torso sup/inf, brazo sup/inf, pierna sup/inf, cuello), deformada por escala no uniforme por hueso/pieza (o Shape Keys si se necesita cambiar forma y no solo grosor) según la medida registrada — evita el problema de combinaciones múltiples visto antes. Medidas primarias (pecho, cintura, bíceps, muslos) dan una aproximación básica; secundarias (cadera, antebrazos, pantorrillas, cuello) añaden precisión, ambas opcionales de forma progresiva. Altura + peso se usan para calcular IMC, no para escalar la malla. Para el semestre, el personaje se mantiene como assets pre-renderizados (ver sección 4); el registro mensual de medidas puede vivir como dato/gráfica en Progreso desde ya
- Tienda de cosméticos para el avatar con moneda gastable (post-semestre): reabre la exclusión de personalización visual profunda (sección 5.2) y se combina mal con el sistema de deformación por medidas corporales, ya que los cosméticos tendrían que ajustarse a un cuerpo variable en vez de a un modelo fijo. Si se retoma, evaluar cosméticos 2D (marcos, insignias visuales) antes que geometría 3D adicional

## 6. Usuarios objetivo

Personas que entrenan o desean empezar a entrenar en el gimnasio y que históricamente han tenido dificultad para mantener la constancia. No está dirigida exclusivamente a atletas avanzados, sino principalmente a principiantes e intermedios que se benefician de un refuerzo motivacional adicional al simple registro de datos.

## 7. Cronograma tentativo

| Periodo | Entregable |
|---|---|
| Corte 1 | Fase 1 — Núcleo funcional |
| Corte 2 | Fase 2 — Sistema de gamificación + inicio de Fase 3 |
| Corte 3 | Fase 3 — Capa visual + pruebas con usuarios + ajustes finales |

*(Cronograma sujeto al calendario oficial de cortes del curso.)*

## 8. Consideraciones técnicas preliminares

- **Stack:** Java, desarrollado en Android Studio (definido por el curso de Móviles 2)
- **Arquitectura de navegación:** una Activity principal con Fragments para las 4 pestañas (Inicio, Rutinas, Entrenar, Progreso) mediante BottomNavigationView, más Activities separadas para el flujo de autenticación (Login, Registro)
- **Persistencia de datos:** requiere un modelo con entidades como `Usuario`, `Ejercicio`, `Rutina`, `Sesión` y `Personaje` — probablemente con Room (SQLite), dado el stack en Java/Android nativo
- **Notificaciones locales:** necesarias si se implementa el recordatorio de rachas (mejora futura)
- **Paleta de colores:** fondo oscuro (estilo gimnasio/juego nocturno), con naranja (#FF6B35) como color de marca/acento primario y mapeo funcional estricto: XP en ámbar (#FFB800), racha en rojo-naranja (#FF4500), éxito/meta cumplida en verde (#22C55E) — inspirado en la disciplina de color de Duolingo (pocos colores, cada uno con un significado fijo) más que en su paleta clara literal

## 9. Conclusión y próximos pasos

El proyecto plantea una solución diferenciada frente a las apps de trazabilidad de gimnasio existentes, centrada en la adherencia y motivación del usuario más que en el registro de datos por sí solo. El alcance definido por fases permite entregar una aplicación funcional y evaluable incluso si el componente visual de la Fase 3 no se completa en su totalidad.

Como siguiente paso, se recomienda definir el modelo de datos (entidades, atributos y relaciones) y un diagrama básico de la arquitectura de la aplicación.
