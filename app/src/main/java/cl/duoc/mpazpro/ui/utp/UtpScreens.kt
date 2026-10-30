package cl.duoc.mpazpro.ui.utp

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.mpazpro.data.local.entity.UnidadResumen
import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.rules.comoMarcaDeTiempo
import cl.duoc.mpazpro.ui.components.BarraSuperior
import cl.duoc.mpazpro.ui.components.BotonGrande
import cl.duoc.mpazpro.ui.components.CampoValidado
import cl.duoc.mpazpro.ui.components.Cargando
import cl.duoc.mpazpro.ui.components.ChipEstado
import cl.duoc.mpazpro.ui.components.EstadoVacio
import cl.duoc.mpazpro.ui.mpazViewModel
import cl.duoc.mpazpro.ui.utp.UnidadFormState.Campo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UtpHomeScreen(alEditar: (Long?) -> Unit, alVerHistorial: () -> Unit, alSalir: () -> Unit) {
    val vm = mpazViewModel { c, _ -> UtpViewModel(c.curriculo) }
    val unidades by vm.unidades.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmar by remember { mutableStateOf<Pair<UnidadResumen, EstadoContenido>?>(null) }

    LaunchedEffect(mensaje) { mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() } }

    Scaffold(
        topBar = {
            BarraSuperior("Jefatura UTP · 2.º Básico", acciones = {
                IconButton(onClick = alVerHistorial) { Icon(Icons.Filled.History, "Historial de cambios") }
                IconButton(onClick = alSalir) { Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { alEditar(null) }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nueva unidad") })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        val lista = unidades
        when {
            lista == null -> Cargando()
            lista.isEmpty() -> EstadoVacio("🗂️", "Aún no hay unidades")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                lista.groupBy { it.asignaturaNombre }.forEach { (asignatura, delGrupo) ->
                    stickyHeader {
                        Text(
                            "${delGrupo.first().asignaturaIcono}  $asignatura",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        )
                    }
                    items(delGrupo, key = { it.unidad.id }) { u ->
                        TarjetaUnidad(
                            u, alEditar = { alEditar(u.unidad.id) },
                            alCambiar = { nuevo -> confirmar = u to nuevo },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    confirmar?.let { (u, nuevo) ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text("Cambiar estado") },
            text = { Text("«${u.unidad.titulo}» pasará de ${u.unidad.estado.etiqueta} a ${nuevo.etiqueta}. Este cambio queda registrado en el historial.") },
            confirmButton = { TextButton(onClick = { vm.cambiarEstado(u.unidad.id, nuevo); confirmar = null }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaUnidad(u: UnidadResumen, alEditar: () -> Unit, alCambiar: (EstadoContenido) -> Unit, modifier: Modifier) {
    Card(modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(u.unidad.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = alEditar) { Icon(Icons.Filled.Edit, "Editar unidad") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipEstado(u.unidad.estado)
                Text("${u.cantidadOa} OA · modificada ${u.unidad.modificadaEn.comoMarcaDeTiempo()}", style = MaterialTheme.typography.labelMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                u.unidad.estado.siguientesPermitidos().forEach { destino ->
                    OutlinedButton(onClick = { alCambiar(destino) }) {
                        Text(
                            when (destino) {
                                EstadoContenido.EN_REVISION -> "Enviar a revisión"
                                EstadoContenido.AUTORIZADO -> "Autorizar"
                                EstadoContenido.PUBLICADO -> "Publicar"
                                EstadoContenido.DESHABILITADO -> "Deshabilitar"
                                EstadoContenido.BORRADOR -> "Volver a borrador"
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnidadFormScreen(alVolver: () -> Unit) {
    val vm = mpazViewModel { c, h -> UnidadFormViewModel(c.curriculo, h) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.guardado) { if (ui.guardado) alVolver() }

    Scaffold(topBar = { BarraSuperior(if (ui.esNueva) "Nueva unidad" else "Editar unidad", alVolver) }) { pad ->
        if (ui.cargando) { Cargando(); return@Scaffold }
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (ui.avisoRevision) {
                Card { Text("Al guardar cambios, la unidad vuelve a «En revisión» y debe autorizarse otra vez.", Modifier.padding(12.dp)) }
            }

            Text("Asignatura", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.asignaturas.forEach { a ->
                    FilterChip(selected = ui.asignaturaId == a.id, onClick = { vm.asignatura(a.id) }, label = { Text("${a.icono} ${a.nombreCorto}") })
                }
            }
            ErrorTexto(ui.errores[Campo.ASIGNATURA])

            CampoValidado(ui.titulo, vm::titulo, "Título de la unidad", Icons.Filled.Title, ui.errores[Campo.TITULO],
                mostrarOk = vm.campoOk(Campo.TITULO), contador = 80)
            CampoValidado(ui.codigoOa, vm::codigo, "Código del OA", Icons.Filled.Tag, ui.errores[Campo.CODIGO],
                mostrarOk = vm.campoOk(Campo.CODIGO), ayuda = "Ejemplo: MA02 OA 09")
            CampoValidado(ui.descripcionOa, vm::descripcion, "Descripción del OA", Icons.AutoMirrored.Filled.Notes, ui.errores[Campo.DESCRIPCION],
                mostrarOk = vm.campoOk(Campo.DESCRIPCION), lineas = 3, contador = 300)
            CampoValidado(ui.eje, vm::eje, "Eje curricular", Icons.Filled.Category, ui.errores[Campo.EJE],
                mostrarOk = vm.campoOk(Campo.EJE), ayuda = "Ej: Lectura, Geometría")
            CampoValidado(ui.textoReferencia, vm::textoReferencia, "Texto de referencia (opcional)", Icons.AutoMirrored.Filled.Notes, null,
                lineas = 4, ayuda = "Solo textos propios, de dominio público o con licencia", contador = 1200)

            Text("Origen del material", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.origenes.forEach { o -> FilterChip(selected = ui.origen == o, onClick = { vm.origen(o) }, label = { Text(o) }) }
            }
            ErrorTexto(ui.errores[Campo.ORIGEN])

            Spacer(Modifier.height(8.dp))
            BotonGrande("Guardar como borrador", vm::guardar, icono = Icons.Filled.Save, cargando = ui.guardando)
        }
    }
}

@Composable
private fun ErrorTexto(error: String?) {
    androidx.compose.animation.AnimatedVisibility(error != null) {
        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun HistorialScreen(alVolver: () -> Unit) {
    val vm = mpazViewModel { c, _ -> UtpViewModel(c.curriculo) }
    val historial by vm.historial.collectAsStateWithLifecycle()
    Scaffold(topBar = { BarraSuperior("Historial de cambios", alVolver) }) { pad ->
        if (historial.isEmpty()) { EstadoVacio("🕓", "Sin cambios registrados", Modifier.padding(pad)); return@Scaffold }
        LazyColumn(Modifier.padding(pad), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            items(historial, key = { it.id }) { h ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("${h.entidad} #${h.entidadId}: ${h.accion}", style = MaterialTheme.typography.bodyLarge)
                        Text("${h.fecha.comoMarcaDeTiempo()} · usuario ${h.usuarioId}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    }
                }
            }
        }
    }
}
