package cl.duoc.mpazpro.ui.estudiante

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.mpazpro.MPazApp
import cl.duoc.mpazpro.data.local.entity.PreguntaEntity
import cl.duoc.mpazpro.domain.rules.Habilitacion
import cl.duoc.mpazpro.platform.LectorVoz
import cl.duoc.mpazpro.ui.components.BarraSuperior
import cl.duoc.mpazpro.ui.components.BotonEscuchar
import cl.duoc.mpazpro.ui.components.BotonGrande
import cl.duoc.mpazpro.ui.components.Cargando
import cl.duoc.mpazpro.ui.components.EstadoVacio
import cl.duoc.mpazpro.ui.components.ProgresoAnimado
import cl.duoc.mpazpro.ui.mpazViewModel
import cl.duoc.mpazpro.ui.theme.Rojo
import cl.duoc.mpazpro.ui.theme.RojoClaro
import cl.duoc.mpazpro.ui.theme.Verde
import cl.duoc.mpazpro.ui.theme.VerdeClaro

/** Acceso al lector de voz nativo; se detiene al salir de la pantalla. */
@Composable
private fun rememberLector(): Pair<LectorVoz, LectorVoz.Estado> {
    val ctx = LocalContext.current
    val lector = remember { (ctx.applicationContext as MPazApp).contenedor.lectorVoz }
    val estado by lector.estado.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { lector.detener() } }
    return lector to estado
}

// ===================================== INICIO =====================================

@Composable
fun EstudianteHomeScreen(alAprender: (Long) -> Unit, alPracticar: (Long) -> Unit, alPrueba: (Long) -> Unit, alSalir: () -> Unit) {
    val vm = mpazViewModel { c, _ -> EstudianteHomeViewModel(c) }
    val tarjetas by vm.tarjetas.collectAsStateWithLifecycle()
    val (lector, estadoVoz) = rememberLector()
    val saludo = "Hola, ${vm.estudiante.nombreFicticio}. Elige qué quieres aprender hoy."

    Scaffold(
        topBar = {
            BarraSuperior("${vm.estudiante.avatar} ${vm.estudiante.nombreFicticio}", acciones = {
                IconButton(onClick = { vm.cerrarSesion(); alSalir() }) { Icon(Icons.AutoMirrored.Filled.Logout, "Salir") }
            })
        },
    ) { pad ->
        val lista = tarjetas
        when {
            lista == null -> Cargando()
            lista.isEmpty() -> EstadoVacio("🌈", "Tu profesor/a aún no te asigna actividades", Modifier.padding(pad))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("¿Qué aprendemos hoy?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        BotonEscuchar(saludo, estadoVoz, lector)
                    }
                }
                items(lista, key = { it.detalle.asignacion.id }) { t ->
                    val color = Color(t.detalle.asignaturaColor)
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    // Entrada escalonada de las tarjetas
                    AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                            border = BorderStroke(2.dp, color),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(t.detalle.asignaturaIcono, style = MaterialTheme.typography.displaySmall)
                                    Spacer(Modifier.width(12.dp))
                                    Text(t.detalle.asignaturaNombre, style = MaterialTheme.typography.titleLarge, color = color)
                                }
                                ProgresoAnimado(t.avance, etiqueta = "Mi avance: ${(t.avance * 100).toInt()} %")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BotonAccion("Aprender", Icons.AutoMirrored.Filled.MenuBook, color, Modifier.weight(1f)) { alAprender(t.detalle.asignacion.id) }
                                    BotonAccion("Practicar", Icons.Filled.Extension, color, Modifier.weight(1f)) { alPracticar(t.detalle.asignacion.id) }
                                    BotonAccion("Prueba", Icons.Filled.Quiz, color, Modifier.weight(1f)) { alPrueba(t.detalle.asignacion.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonAccion(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, alPulsar: () -> Unit) {
    FilledTonalButton(
        onClick = alPulsar, modifier = modifier.heightIn(min = 72.dp), shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, null, Modifier.size(28.dp), tint = color)
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ==================================== APRENDER ====================================

@Composable
fun AprenderScreen(alVolver: () -> Unit, alPracticar: () -> Unit) {
    val vm = mpazViewModel { c, h -> AprenderViewModel(c, h) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val (lector, estadoVoz) = rememberLector()

    Scaffold(topBar = { BarraSuperior(ui.oa?.let { "${it.asignaturaIcono} ${it.asignaturaNombre}" } ?: "Aprender", alVolver) }) { pad ->
        if (ui.cargando) { Cargando(); return@Scaffold }
        if (ui.contenidos.isEmpty()) { EstadoVacio("📭", "Este objetivo aún no tiene explicaciones", Modifier.padding(pad)); return@Scaffold }
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            ProgresoAnimado((ui.indice + 1f) / ui.contenidos.size, etiqueta = "Paso ${ui.indice + 1} de ${ui.contenidos.size}")
            Spacer(Modifier.height(12.dp))
            // Cada explicación entra deslizándose según la dirección de avance
            AnimatedContent(
                targetState = ui.indice,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
                },
                label = "contenido",
                modifier = Modifier.weight(1f),
            ) { i ->
                val c = ui.contenidos[i]
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(c.apoyoVisual, style = MaterialTheme.typography.displaySmall.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize * 1.6f))
                    Text(c.titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    BotonEscuchar(c.instruccionAudio, estadoVoz, lector)
                    Text(c.explicacion, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("👀 Ejemplo guiado", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(c.ejemploGuiado, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Text("Fuente: ${c.origen}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                OutlinedButton(onClick = { vm.ir(-1) }, enabled = ui.indice > 0, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Atrás")
                }
                if (ui.esUltimo) {
                    BotonGrande("¡A practicar!", alPracticar, Modifier.weight(1f), icono = Icons.Filled.Extension)
                } else {
                    BotonGrande("Siguiente", { vm.ir(1) }, Modifier.weight(1f), icono = Icons.AutoMirrored.Filled.ArrowForward)
                }
            }
        }
    }
}

// ==================================== PRÁCTICA ====================================

@Composable
fun PracticaScreen(alVolver: () -> Unit) {
    val vm = mpazViewModel { c, h -> PracticaViewModel(c, h) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val (lector, estadoVoz) = rememberLector()

    Scaffold(topBar = { BarraSuperior("Practicar", alVolver) }) { pad ->
        if (ui.cargando) { Cargando(); return@Scaffold }
        if (ui.preguntas.isEmpty()) { EstadoVacio("🧩", "No hay actividades todavía", Modifier.padding(pad)); return@Scaffold }
        if (ui.terminado) {
            Celebracion("¡Terminaste la práctica!", "Acertaste ${ui.correctasPrimerIntento} de ${ui.preguntas.size} al primer intento.", Modifier.padding(pad), alVolver)
            return@Scaffold
        }
        val p = ui.actual ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProgresoAnimado(ui.fraccion, etiqueta = "Actividad ${ui.indice + 1} de ${ui.preguntas.size} · ${p.tipo.etiqueta}")
            AnimatedContent(ui.indice, transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) }, label = "pregunta") { i ->
                EnunciadoConAudio(ui.preguntas[i], lector, estadoVoz)
            }
            p.alternativas.forEachIndexed { i, alt ->
                val estado = when {
                    ui.seleccion != i -> EstadoAlternativa.NORMAL
                    ui.feedback == Feedback.CORRECTO -> EstadoAlternativa.CORRECTA
                    ui.feedback == Feedback.INCORRECTO -> EstadoAlternativa.INCORRECTA
                    else -> EstadoAlternativa.NORMAL
                }
                Alternativa(alt, ('A' + i).toString(), estado, habilitada = ui.feedback == Feedback.NINGUNO) { vm.elegir(i) }
            }
            // Retroalimentación inmediata y personalizada (sin texto largo)
            AnimatedVisibility(ui.feedback != Feedback.NINGUNO, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                val ok = ui.feedback == Feedback.CORRECTO
                Card(colors = CardDefaults.cardColors(containerColor = if (ok) VerdeClaro else RojoClaro), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (ok) "⭐ ¡Muy bien!" else "🤔 Casi… mira otra vez", style = MaterialTheme.typography.titleLarge, color = if (ok) Verde else Rojo)
                        if (ok) Text(p.explicacion, style = MaterialTheme.typography.bodyLarge)
                        if (ok) {
                            BotonGrande(if (ui.indice == ui.preguntas.lastIndex) "Terminar" else "Siguiente", vm::siguiente, icono = Icons.AutoMirrored.Filled.ArrowForward, color = Verde)
                        } else {
                            BotonGrande("Intentar otra vez", vm::reintentar, icono = Icons.Filled.Refresh)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnunciadoConAudio(p: PreguntaEntity, lector: LectorVoz, estadoVoz: LectorVoz.Estado) {
    val lectura = p.enunciado + ". " + p.alternativas.mapIndexed { i, a -> "${'A' + i}: $a" }.joinToString(". ")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (p.apoyoVisual.isNotBlank()) Text(p.apoyoVisual, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Text(p.enunciado, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        BotonEscuchar(lectura, estadoVoz, lector)
    }
}

private enum class EstadoAlternativa { NORMAL, ELEGIDA, CORRECTA, INCORRECTA }

@Composable
private fun Alternativa(texto: String, letra: String, estado: EstadoAlternativa, habilitada: Boolean, alPulsar: () -> Unit) {
    val fondo by animateColorAsState(
        when (estado) {
            EstadoAlternativa.CORRECTA -> VerdeClaro
            EstadoAlternativa.INCORRECTA -> RojoClaro
            EstadoAlternativa.ELEGIDA -> MaterialTheme.colorScheme.primaryContainer
            EstadoAlternativa.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "fondoAlternativa",
    )
    // Sacudida horizontal en respuestas incorrectas
    val sacudida = remember { Animatable(0f) }
    LaunchedEffect(estado) {
        if (estado == EstadoAlternativa.INCORRECTA) {
            repeat(3) { sacudida.animateTo(12f, tween(50)); sacudida.animateTo(-12f, tween(50)) }
            sacudida.animateTo(0f, tween(50))
        }
    }
    Card(
        onClick = alPulsar, enabled = habilitada,
        colors = CardDefaults.cardColors(containerColor = fondo, disabledContainerColor = fondo, disabledContentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(18.dp),
        border = when (estado) {
            EstadoAlternativa.CORRECTA -> BorderStroke(3.dp, Verde)
            EstadoAlternativa.INCORRECTA -> BorderStroke(3.dp, Rojo)
            EstadoAlternativa.ELEGIDA -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            EstadoAlternativa.NORMAL -> null
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp)
            .graphicsLayer { translationX = sacudida.value },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(letra, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(14.dp))
            Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            when (estado) {
                EstadoAlternativa.CORRECTA -> Text("✔", color = Verde, style = MaterialTheme.typography.headlineSmall)
                EstadoAlternativa.INCORRECTA -> Text("✘", color = Rojo, style = MaterialTheme.typography.headlineSmall)
                else -> {}
            }
        }
    }
}

@Composable
private fun Celebracion(titulo: String, detalle: String, modifier: Modifier, alVolver: () -> Unit) {
    val giro = rememberInfiniteTransition(label = "estrella")
    val angulo by giro.animateFloat(-12f, 12f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "angulo")
    val escala = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { escala.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFFB300), modifier = Modifier.size(140.dp).scale(escala.value).rotate(angulo))
        Text(titulo, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(detalle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        BotonGrande("Volver al inicio", alVolver, icono = Icons.Filled.Home)
    }
}

// ================================== PRUEBA FINAL ==================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PruebaScreen(alVolver: () -> Unit, alTerminar: (Long) -> Unit) {
    val vm = mpazViewModel { c, h -> PruebaViewModel(c, h) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val (lector, estadoVoz) = rememberLector()
    var empezada by rememberSaveable { mutableStateOf(false) }
    var confirmarEnvio by remember { mutableStateOf(false) }
    var verTexto by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(ui.intentoId) { ui.intentoId?.let(alTerminar) }

    Scaffold(topBar = { BarraSuperior("Prueba final", alVolver) }) { pad ->
        if (ui.cargando) { Cargando(); return@Scaffold }
        val hab = ui.habilitacion
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                // Control estricto de intentos: si está bloqueada, no se muestra ninguna pregunta.
                hab is Habilitacion.Bloqueada -> {
                    Spacer(Modifier.height(32.dp))
                    Icon(Icons.Filled.Lock, null, Modifier.size(96.dp).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.outline)
                    Text(hab.motivo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    BotonGrande("Volver", alVolver, icono = Icons.Filled.Home)
                }
                !empezada && hab is Habilitacion.Permitida -> {
                    Text("📝", style = MaterialTheme.typography.displaySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Intento ${hab.intentoNumero}", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                    val aviso = "Esta es la prueba final. Tiene ${ui.preguntas.size} preguntas. " +
                        if (hab.restantes == 0) "Es tu último intento." else "Después de este te quedan ${hab.restantes} intentos."
                    Text(aviso, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    BotonEscuchar(aviso, estadoVoz, lector, Modifier.align(Alignment.CenterHorizontally))
                    BotonGrande("Comenzar", { empezada = true }, icono = Icons.Filled.Quiz)
                }
                ui.revisando -> {
                    Text("Revisa tus respuestas", style = MaterialTheme.typography.headlineSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ui.preguntas.forEachIndexed { i, p ->
                            val lista = p.id in ui.respuestas
                            FilledTonalButton(onClick = { vm.irA(i) }, shape = CircleShape, modifier = Modifier.size(64.dp), contentPadding = PaddingValues(0.dp)) {
                                Text(if (lista) "${i + 1} ✔" else "${i + 1}")
                            }
                        }
                    }
                    if (!ui.completa) Text("Te faltan preguntas por responder.", color = MaterialTheme.colorScheme.error)
                    ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    BotonGrande("Entregar prueba", { confirmarEnvio = true }, icono = Icons.Filled.Send, habilitado = ui.completa, cargando = ui.enviando, color = Verde)
                }
                else -> {
                    val p = ui.actual ?: return@Column
                    if (ui.textoReferencia.isNotBlank()) {
                        OutlinedCard(onClick = { verTexto = !verTexto }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text("📄 Texto para leer ${if (verTexto) "▲" else "▼"}", style = MaterialTheme.typography.titleMedium)
                                AnimatedVisibility(verTexto) {
                                    Column {
                                        Text(ui.textoReferencia, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                                        BotonEscuchar(ui.textoReferencia, estadoVoz, lector, Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                    ProgresoAnimado((ui.indice + 1f) / ui.preguntas.size, etiqueta = "Pregunta ${ui.indice + 1} de ${ui.preguntas.size}")
                    AnimatedContent(ui.indice, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "preguntaPrueba") { i ->
                        EnunciadoConAudio(ui.preguntas[i], lector, estadoVoz)
                    }
                    p.alternativas.forEachIndexed { i, alt ->
                        Alternativa(alt, ('A' + i).toString(), if (ui.respuestas[p.id] == i) EstadoAlternativa.ELEGIDA else EstadoAlternativa.NORMAL, true) {
                            vm.responder(p.id, i)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.ir(-1) }, enabled = ui.indice > 0, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) { Text("Atrás") }
                        if (ui.indice == ui.preguntas.lastIndex) {
                            BotonGrande("Revisar", { vm.revisar(true) }, Modifier.weight(1f), icono = Icons.Filled.Check)
                        } else {
                            BotonGrande("Siguiente", { vm.ir(1) }, Modifier.weight(1f), habilitado = p.id in ui.respuestas, icono = Icons.AutoMirrored.Filled.ArrowForward)
                        }
                    }
                }
            }
        }
    }

    if (confirmarEnvio) {
        AlertDialog(
            onDismissRequest = { confirmarEnvio = false },
            title = { Text("¿Entregar la prueba?") },
            text = { Text("Después de entregar no podrás cambiar tus respuestas.") },
            confirmButton = { TextButton(onClick = { confirmarEnvio = false; vm.enviar() }) { Text("Sí, entregar") } },
            dismissButton = { TextButton(onClick = { confirmarEnvio = false }) { Text("Seguir revisando") } },
        )
    }
}

// ============================== REVISIÓN DEL ESTUDIANTE ==============================

@Composable
fun RevisionScreen(alInicio: () -> Unit) {
    val vm = mpazViewModel { c, h -> RevisionViewModel(c, h) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(topBar = { BarraSuperior("Mis respuestas") }) { pad ->
        if (ui.cargando) { Cargando(); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (ui.puntaje * 2 >= ui.maximo) "🌟" else "💪", style = MaterialTheme.typography.displaySmall)
                    Text("Respondiste bien ${ui.puntaje} de ${ui.maximo}", style = MaterialTheme.typography.headlineSmall)
                    Text("Intento ${ui.numero}", style = MaterialTheme.typography.labelMedium)
                }
            }
            items(ui.filas, key = { it.pregunta.id }) { f ->
                Card(colors = CardDefaults.cardColors(containerColor = if (f.correcta) VerdeClaro else RojoClaro), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${if (f.correcta) "✔" else "✘"} ${f.pregunta.enunciado}", style = MaterialTheme.typography.titleMedium, color = if (f.correcta) Verde else Rojo)
                        Text("Tu respuesta: ${f.pregunta.alternativas.getOrElse(f.elegida) { "—" }}")
                        if (!f.correcta) Text("Respuesta correcta: ${f.pregunta.alternativas[f.pregunta.indiceCorrecto]}")
                        Text(f.pregunta.explicacion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { BotonGrande("Volver al inicio", alInicio, icono = Icons.Filled.Home) }
        }
    }
}
