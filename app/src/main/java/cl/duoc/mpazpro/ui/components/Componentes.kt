package cl.duoc.mpazpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.model.NivelLogro
import cl.duoc.mpazpro.platform.LectorVoz

/**
 * Campo de formulario con validación visual por campo (IE 2.1.2):
 *  - ícono de contexto a la izquierda,
 *  - ícono de error o de "ok" a la derecha,
 *  - mensaje bajo el campo que aparece con animación.
 * El campo NO valida: recibe el error ya calculado por el ViewModel.
 */
@Composable
fun CampoValidado(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    mostrarOk: Boolean = false,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false,
    lineas: Int = 1,
    contador: Int? = null,
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            label = { Text(etiqueta) },
            leadingIcon = { Icon(icono, contentDescription = null) },
            trailingIcon = {
                when {
                    error != null -> Icon(Icons.Filled.ErrorOutline, "Error", tint = MaterialTheme.colorScheme.error)
                    mostrarOk -> Icon(Icons.Filled.CheckCircle, "Correcto", tint = MaterialTheme.colorScheme.tertiary)
                }
            },
            isError = error != null,
            singleLine = lineas == 1,
            minLines = lineas,
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            visualTransformation = if (oculto) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
        if (error == null && (ayuda != null || contador != null)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(ayuda.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (contador != null) Text("${valor.length}/$contador", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Botón principal grande (mín. 64 dp) para dedos pequeños. */
@Composable
fun BotonGrande(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Button(
        onClick = alPulsar,
        enabled = habilitado && !cargando,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            if (icono != null) { Icon(icono, null); Spacer(Modifier.width(10.dp)) }
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Botón "Escuchar" ligado al TTS nativo. Late suavemente mientras habla,
 * para que el estudiante vea que el audio está sonando.
 */
@Composable
fun BotonEscuchar(texto: String, estado: LectorVoz.Estado, lector: LectorVoz, modifier: Modifier = Modifier) {
    val hablando = estado == LectorVoz.Estado.HABLANDO
    val escala by animateFloatAsState(
        targetValue = if (hablando) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escalaEscuchar",
    )
    FilledTonalButton(
        onClick = { if (hablando) lector.detener() else lector.leer(texto) },
        enabled = estado != LectorVoz.Estado.NO_DISPONIBLE && estado != LectorVoz.Estado.INICIANDO,
        shape = RoundedCornerShape(50),
        modifier = modifier.heightIn(min = 56.dp).scale(escala).semantics { contentDescription = if (hablando) "Detener audio" else "Escuchar" },
    ) {
        Icon(if (hablando) Icons.Filled.StopCircle else Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            when (estado) {
                LectorVoz.Estado.NO_DISPONIBLE -> "Audio no disponible"
                LectorVoz.Estado.INICIANDO -> "Preparando audio…"
                LectorVoz.Estado.HABLANDO -> "Detener"
                LectorVoz.Estado.LISTO -> "Escuchar"
            },
        )
    }
}

/** Barra de progreso que se llena con animación. */
@Composable
fun ProgresoAnimado(fraccion: Float, modifier: Modifier = Modifier, etiqueta: String? = null) {
    val animada by animateFloatAsState(fraccion, tween(700), label = "progreso")
    Column(modifier) {
        if (etiqueta != null) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }
        LinearProgressIndicator(
            progress = { animada },
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(12.dp),
        )
    }
}

@Composable
fun ChipEstado(estado: EstadoContenido) {
    val (fondo, textoColor) = when (estado) {
        EstadoContenido.BORRADOR -> Color(0xFFE9ECEF) to Color(0xFF495057)
        EstadoContenido.EN_REVISION -> Color(0xFFFFF3BF) to Color(0xFF7A5D00)
        EstadoContenido.AUTORIZADO -> Color(0xFFD0EBFF) to Color(0xFF1864AB)
        EstadoContenido.PUBLICADO -> Color(0xFFD3F9D8) to Color(0xFF2B8A3E)
        EstadoContenido.DESHABILITADO -> Color(0xFFFFE3E3) to Color(0xFFC92A2A)
    }
    AssistChip(
        onClick = {}, label = { Text(estado.etiqueta) },
        colors = AssistChipDefaults.assistChipColors(containerColor = fondo, labelColor = textoColor),
        border = null,
    )
}

@Composable
fun ChipNivel(nivel: NivelLogro) {
    val (fondo, textoColor) = when (nivel) {
        NivelLogro.FORTALEZA -> Color(0xFFD3F9D8) to Color(0xFF2B8A3E)
        NivelLogro.EN_DESARROLLO -> Color(0xFFFFF3BF) to Color(0xFF7A5D00)
        NivelLogro.REFUERZO -> Color(0xFFFFE3E3) to Color(0xFFC92A2A)
        NivelLogro.SIN_DATOS -> Color(0xFFE9ECEF) to Color(0xFF495057)
    }
    AssistChip(
        onClick = {}, label = { Text(nivel.etiqueta) },
        colors = AssistChipDefaults.assistChipColors(containerColor = fondo, labelColor = textoColor),
        border = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(titulo: String, alVolver: (() -> Unit)? = null, acciones: @Composable () -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (alVolver != null) IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
        },
        actions = { acciones() },
    )
}

@Composable
fun EstadoVacio(emoji: String, texto: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Cargando() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
