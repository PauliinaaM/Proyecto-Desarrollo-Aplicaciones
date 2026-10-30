# Convención de commits y flujo Git

La pauta pide commits **técnicos, bien descritos y distribuidos**. Usamos [Conventional Commits](https://www.conventionalcommits.org/es/) en español:

```
<tipo>(<ámbito>): <qué cambia, en imperativo>

<por qué / detalle opcional>
Refs: #<número de tarjeta o issue>
```

| Tipo | Uso |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de un error |
| `refactor` | Cambio interno sin alterar comportamiento |
| `test` | Pruebas unitarias |
| `style` | Formato, sin cambios de lógica |
| `docs` | README, planificación |
| `build` | Gradle, dependencias, firma |

Ámbitos: `data`, `domain`, `ui-login`, `ui-utp`, `ui-docente`, `ui-estudiante`, `nativo`, `nav`, `tema`.

❌ Evitar: `cambios`, `avance`, `arreglos varios`, `subo todo`.

## Flujo de ramas

- `main`: solo lo entregable. Se actualiza con PR desde `develop`.
- `develop`: integración.
- `feature/<tarea>`: una rama por tarjeta de Trello. Se cierra con un PR revisado por el otro integrante.

## Secuencia sugerida (para construir el historial de forma ordenada)

El código de este repositorio ya está completo. Para que el historial refleje el trabajo real, agreguen los archivos en este orden, cada integrante desde su propia cuenta, revisando y entendiendo cada parte antes de subirla (en la defensa se pregunta por el código).

**Integrante A**
1. `build: configura proyecto Compose con version catalog y KSP`
2. `feat(domain): agrega enums de rol, estado de contenido y tipo de desafío`
3. `feat(data): crea entidades Room y convertidores de tipos`
4. `feat(data): agrega DAOs con consultas por rol y vigencia`
5. `feat(data): agrega semilla ficticia de las cuatro asignaturas`
6. `feat(domain): agrega validadores de login, unidad, asignación y observación`
7. `test(domain): cubre validadores y control de intentos`
8. `feat(data): agrega repositorios con verificación de rol`
9. `feat(nativo): integra Text-to-Speech para lectura de instrucciones`
10. `feat(nativo): agrega sonido y vibración de retroalimentación`
11. `feat(nativo): notifica nuevas asignaciones con permiso en tiempo de ejecución`
12. `feat(data): genera informe por estudiante y resumen por OA`
13. `feat(nativo): exporta informe PDF con SAF y FileProvider`

**Integrante B**
1. `feat(tema): define tema Material 3 con tipografía grande`
2. `feat(ui): agrega componentes CampoValidado, BotonGrande y ProgresoAnimado`
3. `feat(ui-login): crea ingreso por perfil y PIN con validación visual`
4. `feat(nav): define grafo por rol y bloqueo de secciones`
5. `feat(ui-utp): lista unidades por asignatura con cambio de estado`
6. `feat(ui-utp): agrega formulario de unidad y OA`
7. `feat(ui-utp): muestra historial de cambios`
8. `feat(ui-docente): crea panel con pestañas de curso y asignaciones`
9. `feat(ui-docente): agrega formulario de asignación con selector de fechas`
10. `feat(ui-estudiante): crea inicio con tarjetas por asignatura`
11. `feat(ui-estudiante): agrega pantallas aprender y practicar`
12. `feat(ui-estudiante): agrega prueba final y revisión de respuestas`
13. `feat(ui-docente): muestra informe y formulario de observación`
14. `style(ui): agrega animaciones de transición y retroalimentación`

**Ambos:** `docs: actualiza README con funcionalidades y pasos de ejecución`
