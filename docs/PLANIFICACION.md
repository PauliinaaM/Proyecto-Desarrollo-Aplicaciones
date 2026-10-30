# Planificación — MPAZ PRO 2.º (EP2, semanas 6 a 11)

Tablero sugerido en Trello con 5 listas: **Backlog · Por hacer (semana) · En progreso · En revisión (PR) · Hecho**.
Cada tarjeta lleva: responsable, etiqueta de indicador (IE 2.x.x), checklist y el enlace al PR o commit que la cierra.
El archivo `trello_tareas.csv` tiene todas las tarjetas listas para copiar o importar.

**Integrante A** = datos y lógica · **Integrante B** = interfaz y experiencia.
Ambos hacen commits todas las semanas: la rúbrica (IE 2.3.2, 20 %) revisa actividad distribuida, no solo el resultado final.

## Semana 6 — Base del proyecto
| Tarea | Resp. | IE |
|---|---|---|
| Crear repo GitHub, `.gitignore`, rama `develop`, proteger `main` | A | 2.3.2 |
| Crear proyecto Compose + version catalog + paquetes MVVM | A | 2.3.1 |
| Tema Material 3 con tipografía grande y paleta accesible | B | 2.1.1 |
| Componentes base: `BotonGrande`, `CampoValidado`, `BarraSuperior` | B | 2.1.2 |
| Tablero Trello + README inicial | A + B | 2.3.2 |

## Semana 7 — Datos y validación
| Tarea | Resp. | IE |
|---|---|---|
| Entidades Room, convertidores y DAOs | A | 2.3.1 |
| Semilla ficticia de las 4 asignaturas | A | 2.3.1 |
| Validadores en `domain/validation` + pruebas JUnit | A | 2.2.1 |
| Pantalla de ingreso (perfil + PIN) con validación visual | B | 2.1.2 |
| Navegación por rol y bloqueo de rutas | B | 2.1.1 |

## Semana 8 — Flujo UTP y Docente
| Tarea | Resp. | IE |
|---|---|---|
| Repositorio de currículo + historial de cambios | A | 2.3.1 |
| Panel UTP y cambio de estados | B | 2.1.1 |
| Formulario Unidad/OA validado | B | 2.1.2 |
| Formulario de asignación (fechas, intentos, duplicados) | A + B | 2.1.2 / 2.2.1 |
| Notificaciones con permiso en tiempo de ejecución | A | 2.4.1 |

## Semana 9 — Flujo Estudiante
| Tarea | Resp. | IE |
|---|---|---|
| Text-to-Speech (botón Escuchar) | A | 2.4.1 |
| Sonido + vibración de retroalimentación | A | 2.4.1 |
| Pantallas Aprender y Practicar | B | 2.1.1 |
| Control de intentos en prueba final + pruebas | A | 2.2.1 |
| Pantalla de prueba final y revisión | B | 2.1.1 |

## Semana 10 — Informes y animaciones
| Tarea | Resp. | IE |
|---|---|---|
| Informe por estudiante y resumen por OA | A | 2.3.1 |
| Exportar PDF (SAF) y compartir (FileProvider) | A | 2.4.1 |
| Formulario de observación pedagógica | B | 2.1.2 |
| Animaciones: transiciones, sacudida, celebración, progreso | B | 2.2.2 |

## Semana 11 — Cierre y entrega
| Tarea | Resp. | IE |
|---|---|---|
| Prueba completa en emulador y dispositivo físico | A + B | — |
| Revisión de accesibilidad (tamaños, contraste, TalkBack) | B | 2.1.1 |
| README final + capturas | A + B | 2.3.2 |
| Subir a AVA grupal y activar AVA individual | A + B | — |
| **Congelar el repositorio** (no más commits tras la entrega) | A + B | — |

> La pauta asigna nota 1.0 si hay cambios en el repositorio después de la fecha de entrega. Después de entregar, nadie hace push, ni siquiera para corregir un typo.
