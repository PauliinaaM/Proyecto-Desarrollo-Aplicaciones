# MPAZ PRO 2.º

Aplicación móvil Android de apoyo didáctico para **Segundo Básico** de la Escuela Escritora Marcela Paz (San Bernardo).
MVP académico desarrollado para **DSY1105 · Desarrollo de Aplicaciones Móviles** (Duoc UC) — Evaluación Parcial 2.

> ⚠️ Todos los datos de la app son **100 % ficticios**: no hay nombres, RUT ni credenciales reales.
> Los textos, cuentos, preguntas, sonidos e ícono son de **autoría propia** del equipo.

## Integrantes

| Integrante | Rol en el proyecto |
|---|---|
| _Nombre Apellido 1_ | Datos y lógica: Room, repositorios, validadores, pruebas unitarias, recursos nativos |
| _Nombre Apellido 2_ | Interfaz: Compose, navegación, formularios, animaciones, accesibilidad |

Tablero de planificación: _pegar aquí el enlace público de Trello_

---

## Problema que resuelve

Hoy las guías de Lenguaje, Matemática, Ciencias e Historia se imprimen, se aplican y se corrigen a mano, y los resultados quedan dispersos en papel. MPAZ PRO 2.º digitaliza ese ciclo con un flujo de tres perfiles:

```
UTP autoriza contenido  →  Docente asigna OA al curso  →  Estudiante aprende, practica y rinde
                                      ↓
                    Docente revisa informe reservado y registra observación
```

## Funcionalidades implementadas

**Jefatura UTP**
- Lista de unidades agrupadas por asignatura, con su estado (Borrador → En revisión → Autorizado → Publicado / Deshabilitado).
- Formulario validado para crear y editar unidad + objetivo de aprendizaje (código OA, descripción, eje, texto de referencia, origen del material).
- Cambio de estado con confirmación; editar una unidad aprobada la devuelve a revisión.
- Historial de modificaciones y aprobaciones (trazabilidad).

**Docente**
- Ve solo los OA autorizados o publicados por UTP.
- Formulario validado de asignación: OA, curso, rango de fechas (selector de calendario) y máximo de intentos (1 a 5). Impide asignar dos veces el mismo OA vigente.
- Rendimiento del curso agrupado por OA.
- Informe por estudiante: puntaje global y por OA, desglose de respuestas, fortalezas y áreas de refuerzo por reglas fijas (sin IA).
- Formulario validado de observación pedagógica con confirmación de validación.
- Descarga del informe en PDF (selector de archivos del sistema) o compartir.

**Estudiante**
- Ingreso con perfil ficticio y PIN de 4 dígitos.
- Solo ve las asignaturas y OA que su docente le asignó, vigentes hoy.
- **Aprender:** explicaciones paso a paso con apoyo visual, ejemplo guiado y botón **Escuchar** (texto a voz).
- **Practicar:** actividades de selección, completación, asociación y secuencia, con retroalimentación inmediata (color, sonido, vibración y explicación). El progreso se guarda al instante.
- **Prueba final:** respeta las fechas y el límite de intentos del docente; al entregar, revisa sus respuestas correctas e incorrectas.
- No tiene acceso a informes ni métricas consolidadas (bloqueo por rol en navegación, ViewModels y repositorios).

## Recursos nativos del dispositivo

El caso indica que **no** se usan cámara, GPS, micrófono ni biometría. Por eso se eligieron recursos coherentes con un lector inicial:

| # | Recurso | Para qué | Archivo |
|---|---|---|---|
| 1 | **Text-to-Speech** | Lee instrucciones, explicaciones, preguntas y textos (sin micrófono) | `platform/LectorVoz.kt` |
| 2 | **Salida de audio (SoundPool)** | Sonidos originales de acierto, reintento y logro | `platform/RetroalimentacionSensorial.kt` |
| 3 | **Vibrador** (permiso `VIBRATE`) | Retroalimentación háptica al responder | `platform/RetroalimentacionSensorial.kt` |
| 4 | **Notificaciones** (permiso en tiempo de ejecución `POST_NOTIFICATIONS`) | Aviso al asignar un OA | `platform/Notificador.kt` |
| 5 | **Almacenamiento y compartir** (SAF + `FileProvider`) | Guardar y compartir el informe PDF sin exponer rutas internas | `platform/ExportadorInforme.kt` |

## Arquitectura

MVVM con capas separadas. La validación vive en `domain` (Kotlin puro, sin Android), los ViewModels exponen `StateFlow` y las pantallas solo dibujan el estado.

```
app/src/main/java/cl/duoc/mpazpro/
├── domain/                 ← reglas puras, testeables con JUnit
│   ├── model/              enums: Rol, EstadoContenido, TipoDesafio, NivelLogro
│   ├── validation/         validadores de cada formulario
│   └── rules/              control de intentos, calificación, formato de fechas
├── data/
│   ├── local/              Room: entidades, DAOs, convertidores, base de datos
│   ├── repository/         acceso a datos + control de rol
│   └── seed/               semilla 100 % ficticia (4 asignaturas)
├── platform/               recursos nativos
├── di/                     contenedor de dependencias manual
└── ui/
    ├── components/         campo validado, botón grande, botón Escuchar, progreso
    ├── navigation/         grafo por rol + bloqueo de seguridad
    ├── login/  utp/  docente/  estudiante/
    └── theme/              Material 3, tipografía grande
```

**Stack:** Kotlin 2.0 · Jetpack Compose · Material Design 3 · Navigation Compose · Room 2.6 (KSP) · Coroutines/Flow · JUnit 4.
`minSdk 26` · `targetSdk 35`.

## Cómo ejecutar

1. Clonar el repositorio y abrirlo en **Android Studio** (Ladybug o superior).
2. Esperar la sincronización de Gradle (descarga dependencias la primera vez).
3. Ejecutar en un emulador o dispositivo Android 8.0+ (`Run ▶ app`).
4. Ingresar con alguno de estos perfiles de prueba:

| Perfil | PIN |
|---|---|
| UTP Demo | `1000` |
| Docente Demo 2°A | `2000` |
| Docente Demo 2°B | `2001` |
| Estudiantes (Zorro Azul, Búho Verde, …) | `1234` |

La base de datos se llena sola en el primer inicio. Para reiniciar los datos: *Ajustes → Apps → MPAZ PRO 2.º → Borrar datos*.

**Recorrido sugerido para la demo:** UTP envía «Mi comunidad y sus trabajos» a revisión → autoriza → Docente 2°A asigna «Ciencias» → Estudiante Zorro Azul aprende, practica y rinde → Docente revisa su informe y registra una observación.

### Pruebas unitarias

```bash
./gradlew testDebugUnitTest
```

Cubren validadores de los cuatro formularios, control de intentos, umbrales de logro y el flujo de estados de UTP (25 pruebas).

### APK firmado (release)

1. Crear un keystore: *Build → Generate Signed App Bundle / APK → APK → Create new…*
2. Crear `keystore.properties` en la raíz (**no se sube a GitHub**, ya está en `.gitignore`):
   ```properties
   storeFile=mpaz-release.jks
   storePassword=********
   keyAlias=mpaz
   keyPassword=********
   ```
3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`

## Correspondencia con la rúbrica

| Indicador | Dónde se evidencia |
|---|---|
| IE 2.1.1 Interfaz y navegación | `ui/navigation/NavGraph.kt`, grafo separado por rol; tipografía grande en `ui/theme/Tema.kt` |
| IE 2.1.2 Formularios validados | Login, Unidad/OA, Asignación, Observación — `ui/components/CampoValidado` con ícono y mensaje por campo |
| IE 2.2.1 Validación desacoplada | `domain/validation/Validacion.kt` + ViewModels; la UI no contiene reglas |
| IE 2.2.2 Animaciones | Transiciones de navegación, `AnimatedContent` entre pasos, sacudida en error, rebote en acierto, progreso animado, celebración |
| IE 2.3.1 Modularidad + persistencia | Capas `domain / data / platform / ui`, Room con 13 tablas |
| IE 2.3.2 GitHub + Trello | Ver `docs/PLANIFICACION.md` y `docs/COMMITS.md` |
| IE 2.4.1 Recursos nativos | Tabla de recursos nativos (arriba) |

## Derechos de autor y datos

- Contenidos pedagógicos: redactados por el equipo. Los códigos de OA son referenciales y deben contrastarse con el material oficial que entregue el colegio.
- Sonidos: generados por el equipo (tonos sintéticos). Ícono: diseño vectorial propio.
- Sin integración con el Libro de Clases, bases institucionales ni IA generativa.
