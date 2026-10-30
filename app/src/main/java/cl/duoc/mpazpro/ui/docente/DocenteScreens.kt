package cl.duoc.mpazpro.ui.docente

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.mpazpro.MPazApp
import cl.duoc.mpazpro.data.local.entity.AsignacionDetalle
import cl.duoc.mpazpro.data.repository.ResumenOaCurso
import cl.duoc.mpazpro.data.repository.ResultadoOa
import cl.duoc.mpazpro.domain.rules.Calificador
import cl.duoc.mpazpro.domain.rules.comoMarcaDeTiempo
import cl.duoc.mpazpro.domain.rules.formatoCorto
import cl.duoc.mpazpro.ui.components.BarraSuperior
import cl.duoc.mpazpro.ui.components.BotonGrande
import cl.duoc.mpazpro.ui.components.CampoValidado
import cl.duoc.mpazpro.ui.components.Cargando
import cl.duoc.mpazpro.ui.components.ChipNivel
import cl.duoc.mpazpro.ui.components.EstadoVacio
import cl.duoc.mpazpro.ui.components.ProgresoAnimado
import cl.duoc.mpazpro.ui.mpazViewModel
import cl.duoc.mpazpro.ui.theme.Rojo
import cl.duoc.mpazpro.ui.theme.Verde
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ================================ PANEL DOCENTE ================================

@Composable
fun DocenteHomeScreen(alAsignar: () -> Unit, alVerInforme: (Long) -> Unit, alSalir: () -> Unit) {
    val vm = mpazViewModel { c, _ -> DocenteViewModel(c) }
    val asignaciones by vm.asignaciones.collectAsStateWithLifecycle()
    val estudiantes by vm.estudiantes.collectAsStateWithLifecycle()
    val resumen by vm.resumen.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            BarraSuperior("${vm.docente.avatar} ${vm.docente.nombreFicticio}", acciones = {
                IconButton(onClick = { vm.cerrarSesion(); alSalir() }) { Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = alAsignar, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Asignar OA") })
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = pestana) {
                Tab(pestana == 0, { pestana = 0 }, text = { Text("Mi curso") }, icon = { Icon(Icons.Filled.Groups, null) })
                Tab(pestana == 1, { pestana = 1 }, text = { Text("Asignaciones") }, icon = { Icon(Icons.Filled.AssignmentTurnedIn, null) })
            }
            // Transición animada entre pestañas
            AnimatedContent(targetState = pestana, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pestanas") { p ->
                when (p) {
                    0 -> PestanaCurso(resumen, estudiantes, alVerInforme)
                    else -> PestanaAsignaciones(asignaciones)
                }
            }
        }
    }
}

@Composable
private fun PestanaCurso(
    resumen: List<ResumenOaCurso>?,
    estudiantes: List<cl.duoc.mpazpro.data.local.entity.UsuarioEntity>,
    alVerInforme: (Long) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Rendimiento por objetivo de aprendizaje", style = MaterialTheme.typography.titleLarge) }
        when {
            resumen == null -> item { Text("Calculando…") }
            resumen.isEmpty() -> item { Text("Aún no hay OA asignados a este curso.") }
            else -> items(resumen, key = { "oa-${it.oa.oaId}" }) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${r.oa.asignaturaIcono} ${r.oa.asignaturaNombre} · ${r.oa.codigo}", style = MaterialTheme.typography.titleMedium)
                        Text(r.oa.descripcion, style = MaterialTheme.typography.bodyMedium)
                        ProgresoAnimado(
                            (r.promedio ?: 0) / 100f,
                            etiqueta = "Promedio: ${r.promedio?.let { "$it %" } ?: "—"} · rindieron ${r.rindieron}/${r.totalEstudiantes}",
                        )
                        ChipNivel(Calificador.nivel(r.promedio))
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Estudiantes (informes reservados)", style = MaterialTheme.typography.titleLarge)
        }
        items(estudiantes, key = { "est-${it.id}" }) { e ->
            Card(Modifier.fillMaxWidth().clickable { alVerInforme(e.id) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(e.avatar, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Text(e.nombreFicticio, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, "Ver informe")
                }
            }
        }
    }
}

@Composable
private fun PestanaAsignaciones(asignaciones: List<AsignacionDetalle>?) {
    when {
        asignaciones == null -> Cargando()
        asignaciones.isEmpty() -> EstadoVacio("📋", "Todavía no asignas objetivos")
        else -> LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(asignaciones, key = { it.asignacion.id }) { a ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${a.asignaturaIcono} ${a.oaCodigo} · ${a.asignacion.curso}", style = MaterialTheme.typography.titleMedium)
                        Text(a.oaDescripcion, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Del ${LocalDate.ofEpochDay(a.asignacion.desdeEpochDay).formatoCorto()} al " +
                                "${LocalDate.ofEpochDay(a.asignacion.hastaEpochDay).formatoCorto()} · máx. ${a.asignacion.maxIntentos} intentos",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

// ============================== FORMULARIO ASIGNAR ==============================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AsignacionFormScreen(alVolver: () -> Unit) {
    val vm = mpazViewModel { c, _ -> AsignacionFormViewModel(c) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val notificador = remember { (contexto.applicationContext as MPazApp).contenedor.notificador }
    var permisoNotif by remember { mutableStateOf(notificador.tienePermiso()) }
    var fechaAbierta by remember { mutableStateOf<String?>(null) } // "desde" | "hasta"

    // Permiso en tiempo de ejecución (Android 13+) para el aviso de asignación
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> permisoNotif = ok }

    LaunchedEffect(ui.guardado) { if (ui.guardado) alVolver() }

    Scaffold(topBar = { BarraSuperior("Asignar objetivo", alVolver) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!permisoNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Activa los avisos para notificar nuevas asignaciones.", Modifier.weight(1f))
                        TextButton(onClick = { pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Activar") }
                    }
                }
            }

            Text("Objetivo de aprendizaje (autorizado por UTP)", style = MaterialTheme.typography.titleMedium)
            ui.oas.forEach { oa ->
                val activo = ui.oaId == oa.oaId
                Card(
                    onClick = { vm.oa(oa.oaId) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (activo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${oa.asignaturaIcono} ${oa.codigo} · ${oa.unidadTitulo}", style = MaterialTheme.typography.titleMedium)
                        if (activo) Text(oa.descripcion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            ErrorAnimado(ui.errorOa)

            CampoValidado(ui.curso, vm::curso, "Curso", Icons.Filled.Groups, ui.errorCurso, ayuda = "Formato 2°A")

            Text("Disponibilidad", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { fechaAbierta = "desde" }, Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(6.dp))
                    Text("Desde ${ui.desde?.formatoCorto() ?: "—"}")
                }
                OutlinedButton(onClick = { fechaAbierta = "hasta" }, Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(6.dp))
                    Text("Hasta ${ui.hasta?.formatoCorto() ?: "—"}")
                }
            }
            ErrorAnimado(ui.errorFechas)

            Text("Intentos permitidos en la prueba final", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledIconButton(onClick = { vm.sumarIntento(-1) }) { Icon(Icons.Filled.Remove, "Menos intentos") }
                Text(ui.maxIntentos.ifEmpty { "—" }, style = MaterialTheme.typography.headlineMedium)
                FilledIconButton(onClick = { vm.sumarIntento(1) }) { Icon(Icons.Filled.Add, "Más intentos") }
            }
            ErrorAnimado(ui.errorIntentos)

            BotonGrande("Asignar al curso", vm::guardar, icono = Icons.Filled.AssignmentTurnedIn, cargando = ui.guardando)
        }
    }

    fechaAbierta?.let { cual ->
        val inicial = (if (cual == "desde") ui.desde else ui.hasta) ?: LocalDate.now()
        val estado = rememberDatePickerState(initialSelectedDateMillis = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { fechaAbierta = null },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (cual == "desde") vm.desde(d) else vm.hasta(d)
                    }
                    fechaAbierta = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { fechaAbierta = null }) { Text("Cancelar") } },
        ) { DatePicker(estado) }
    }
}

@Composable
private fun ErrorAnimado(error: String?) {
    AnimatedVisibility(error != null) {
        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

// =================================== INFORME ===================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InformeScreen(alVolver: () -> Unit) {
    val vm = mpazViewModel { c, h -> InformeViewModel(c, h) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val exportador = remember { (contexto.applicationContext as MPazApp).contenedor.exportador }
    val snackbar = remember { SnackbarHostState() }

    // Selector de archivos del sistema (SAF): el docente elige dónde guardar el PDF
    val guardarComo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val pdf = ui.pdf
        if (uri != null && pdf != null) {
            runCatching { exportador.guardarEn(pdf, uri) }
                .onSuccess { vm.mensaje("Informe guardado") }
                .onFailure { vm.mensaje("No se pudo guardar el informe") }
        }
    }

    LaunchedEffect(ui.accionPdf) {
        val pdf = ui.pdf ?: return@LaunchedEffect
        when (ui.accionPdf) {
            InformeState.AccionPdf.COMPARTIR -> try {
                contexto.startActivity(exportador.intentCompartir(pdf))
            } catch (e: ActivityNotFoundException) {
                vm.mensaje("No hay apps para compartir")
            }
            InformeState.AccionPdf.GUARDAR -> guardarComo.launch(pdf.name)
            null -> {}
        }
        vm.accionPdfConsumida()
    }
    LaunchedEffect(ui.mensaje) { ui.mensaje?.let { snackbar.showSnackbar(it); vm.mensaje(null) } }

    Scaffold(
        topBar = { BarraSuperior("Informe pedagógico", alVolver) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        val inf = ui.informe
        when {
            ui.cargando -> Cargando()
            inf == null -> EstadoVacio("🔒", ui.error ?: "No se pudo cargar el informe", Modifier.padding(pad))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(inf.estudiante.avatar, style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(inf.estudiante.nombreFicticio, style = MaterialTheme.typography.headlineSmall)
                            Text("Curso ${inf.estudiante.curso} · emitido ${inf.emitidoEn.comoMarcaDeTiempo()}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Puntaje global: ${inf.porcentajeGlobal?.let { "$it %" } ?: "sin pruebas"}", style = MaterialTheme.typography.titleLarge)
                            ProgresoAnimado((inf.porcentajeGlobal ?: 0) / 100f)
                            Text("Fortalezas: ${inf.fortalezas.ifEmpty { listOf("—") }.joinToString()}")
                            Text("Refuerzo: ${inf.refuerzos.ifEmpty { listOf("—") }.joinToString()}")
                            Text(
                                "Diagnóstico por reglas fijas (≥${Calificador.UMBRAL_FORTALEZA} % fortaleza, <${Calificador.UMBRAL_DESARROLLO} % refuerzo). Sin IA.",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.exportar(InformeState.AccionPdf.GUARDAR) }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Download, null); Spacer(Modifier.width(6.dp)); Text("Descargar PDF")
                        }
                        OutlinedButton(onClick = { vm.exportar(InformeState.AccionPdf.COMPARTIR) }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Share, null); Spacer(Modifier.width(6.dp)); Text("Compartir")
                        }
                    }
                }
                items(inf.resultados, key = { it.oa.oaId }) { r -> TarjetaResultadoOa(r) }

                // ---- Observación pedagógica (formulario validado) ----
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Validar y registrar observación", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                inf.resultados.forEach { r ->
                                    FilterChip(ui.oaObservacion == r.oa.oaId, { vm.oaObservacion(r.oa.oaId) }, label = { Text(r.oa.codigo) })
                                }
                            }
                            CampoValidado(
                                ui.texto, vm::texto, "Observación pedagógica", Icons.AutoMirrored.Filled.Notes, ui.errorTexto,
                                lineas = 3, contador = 500,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(ui.confirmado, vm::confirmado)
                                Text("Revisé el informe y lo valido", Modifier.weight(1f))
                            }
                            ErrorAnimado(ui.errorConfirmado)
                            BotonGrande("Guardar observación", vm::guardarObservacion)
                        }
                    }
                }
                if (ui.observaciones.isNotEmpty()) {
                    item { Text("Observaciones registradas", style = MaterialTheme.typography.titleMedium) }
                    items(ui.observaciones, key = { "obs-${it.id}" }) { o ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(o.texto, style = MaterialTheme.typography.bodyLarge)
                            Text("${o.creadaEn.comoMarcaDeTiempo()} · ${if (o.validado) "validado" else "pendiente"}", style = MaterialTheme.typography.labelMedium)
                            HorizontalDivider(Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaResultadoOa(r: ResultadoOa) {
    var abierto by rememberSaveable(r.oa.oaId) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().animateContentSize().clickable { abierto = !abierto }, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${r.oa.asignaturaIcono} ${r.oa.asignaturaNombre} · ${r.oa.codigo}", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipNivel(r.nivel)
                Text("${r.porcentaje?.let { "$it %" } ?: "—"} · ${r.intentosUsados} intento(s)", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Práctica: ${r.practicaCorrectas}/${r.actividadesPractica} correctas", style = MaterialTheme.typography.labelMedium)
            if (abierto) {
                if (r.desgloseUltimo.isEmpty()) Text("Sin prueba final rendida.")
                r.desgloseUltimo.forEach { d ->
                    Text(
                        "${if (d.esCorrecta) "✔" else "✘"} ${d.enunciado}\n   Respondió: ${d.seleccionada}" +
                            if (d.esCorrecta) "" else " · Correcta: ${d.correcta}",
                        color = if (d.esCorrecta) Verde else Rojo,
                        fontWeight = if (d.esCorrecta) FontWeight.Normal else FontWeight.SemiBold,
                    )
                }
            } else {
                Text("Toca para ver el desglose de respuestas", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
        }
    }
}
