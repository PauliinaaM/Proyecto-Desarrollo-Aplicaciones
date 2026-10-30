package cl.duoc.mpazpro.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.ui.components.BotonGrande
import cl.duoc.mpazpro.ui.components.CampoValidado
import cl.duoc.mpazpro.ui.mpazViewModel

@Composable
fun LoginScreen(alIngresar: (Rol) -> Unit) {
    val vm = mpazViewModel { c, _ -> LoginViewModel(c.auth) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.ingresoComo) {
        ui.ingresoComo?.let { alIngresar(it); vm.navegacionConsumida() }
    }

    // Animación de "sacudida" cuando el ingreso falla: retroalimentación sin texto extra.
    val sacudida = remember { Animatable(0f) }
    LaunchedEffect(ui.intentosFallidos) {
        if (ui.intentosFallidos > 0) {
            sacudida.animateTo(0f, keyframes {
                durationMillis = 400
                -18f at 50; 18f at 120; -12f at 190; 12f at 260; -6f at 330; 0f at 400
            })
        }
    }

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("📚⭐", style = MaterialTheme.typography.displaySmall)
            Text("MPAZ PRO 2.º", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Prototipo con datos ficticios",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 1) ¿Quién eres?
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rol.entries.reversed().forEach { rol ->
                    FilterChip(
                        selected = ui.rolFiltro == rol, onClick = { vm.elegirRol(rol) },
                        label = { Text(rol.etiqueta) },
                    )
                }
            }

            // 2) Elige tu perfil (tarjetas grandes con avatar)
            Text("¿Quién eres?", style = MaterialTheme.typography.titleLarge)
            SelectorPerfiles(ui.perfilesFiltrados, ui.seleccionado, vm::elegirPerfil)
            AnimatedVisibility(ui.errorPerfil != null) {
                Text(ui.errorPerfil.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            // 3) PIN (aparece solo cuando ya eligió perfil: menos carga por pantalla)
            AnimatedVisibility(
                visible = ui.seleccionado != null,
                enter = fadeIn() + slideInVertically { it / 2 },
            ) {
                Column(
                    Modifier.graphicsLayer { translationX = sacudida.value },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CampoValidado(
                        valor = ui.pin, alCambiar = vm::cambiarPin, etiqueta = "Tu PIN (4 números)",
                        icono = Icons.Filled.Pin, error = ui.errorPin, mostrarOk = ui.pinValido,
                        teclado = KeyboardType.NumberPassword, oculto = true,
                        ayuda = "Pídelo a tu profesor/a",
                    )
                    BotonGrande("Entrar", vm::ingresar, icono = Icons.AutoMirrored.Filled.Login, cargando = ui.cargando)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorPerfiles(perfiles: List<UsuarioEntity>, seleccionado: Long?, alElegir: (Long) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        perfiles.forEach { p ->
            val activo = p.id == seleccionado
            val escala by animateFloatAsState(if (activo) 1.06f else 1f, label = "escalaPerfil")
            val fondo by animateColorAsState(
                if (activo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                label = "fondoPerfil",
            )
            Card(
                onClick = { alElegir(p.id) },
                colors = CardDefaults.cardColors(containerColor = fondo),
                border = if (activo) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.width(140.dp).scale(escala),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.avatar, style = MaterialTheme.typography.displaySmall)
                    Text(p.nombreFicticio, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    p.curso?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}
