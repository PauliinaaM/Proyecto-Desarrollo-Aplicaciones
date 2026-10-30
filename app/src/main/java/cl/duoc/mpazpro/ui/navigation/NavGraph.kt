package cl.duoc.mpazpro.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.mpazpro.data.repository.SesionRepository
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.ui.docente.AsignacionFormScreen
import cl.duoc.mpazpro.ui.docente.DocenteHomeScreen
import cl.duoc.mpazpro.ui.docente.InformeScreen
import cl.duoc.mpazpro.ui.estudiante.AprenderScreen
import cl.duoc.mpazpro.ui.estudiante.EstudianteHomeScreen
import cl.duoc.mpazpro.ui.estudiante.PracticaScreen
import cl.duoc.mpazpro.ui.estudiante.PruebaScreen
import cl.duoc.mpazpro.ui.estudiante.RevisionScreen
import cl.duoc.mpazpro.ui.login.LoginScreen
import cl.duoc.mpazpro.ui.utp.HistorialScreen
import cl.duoc.mpazpro.ui.utp.UnidadFormScreen
import cl.duoc.mpazpro.ui.utp.UtpHomeScreen

/** Rutas agrupadas por rol: el prefijo indica qué perfil puede abrirlas. */
object Rutas {
    const val LOGIN = "login"

    const val UTP = "utp"
    const val UTP_UNIDAD = "utp/unidad?id={id}"
    const val UTP_HISTORIAL = "utp/historial"
    fun utpUnidad(id: Long?) = "utp/unidad?id=${id ?: -1L}"

    const val DOCENTE = "docente"
    const val DOCENTE_ASIGNAR = "docente/asignar"
    const val DOCENTE_INFORME = "docente/informe/{estudianteId}"
    fun docenteInforme(id: Long) = "docente/informe/$id"

    const val ESTUDIANTE = "estudiante"
    const val EST_APRENDER = "estudiante/aprender/{asignacionId}"
    const val EST_PRACTICA = "estudiante/practica/{asignacionId}"
    const val EST_PRUEBA = "estudiante/prueba/{asignacionId}"
    const val EST_REVISION = "estudiante/revision/{intentoId}"
    fun aprender(id: Long) = "estudiante/aprender/$id"
    fun practica(id: Long) = "estudiante/practica/$id"
    fun prueba(id: Long) = "estudiante/prueba/$id"
    fun revision(id: Long) = "estudiante/revision/$id"

    fun inicioDe(rol: Rol) = when (rol) {
        Rol.UTP -> UTP
        Rol.DOCENTE -> DOCENTE
        Rol.ESTUDIANTE -> ESTUDIANTE
    }
}

private val argAsignacion = listOf(navArgument("asignacionId") { type = NavType.LongType })

@Composable
fun MPazNavGraph(sesion: SesionRepository, nav: NavHostController = rememberNavController()) {
    val salir: () -> Unit = {
        sesion.cerrar()
        nav.navigate(Rutas.LOGIN) { popUpTo(0) { inclusive = true } }
    }

    NavHost(
        navController = nav,
        startDestination = Rutas.LOGIN,
        // Transiciones animadas entre pantallas (fluidez, IE 2.2.2)
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) + fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) + fadeOut(tween(300)) },
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(alIngresar = { rol ->
                nav.navigate(Rutas.inicioDe(rol)) { popUpTo(Rutas.LOGIN) { inclusive = true } }
            })
        }

        // ------------------------------- UTP -------------------------------
        composable(Rutas.UTP) {
            RequiereRol(sesion, Rol.UTP, salir) {
                UtpHomeScreen(
                    alEditar = { nav.navigate(Rutas.utpUnidad(it)) },
                    alVerHistorial = { nav.navigate(Rutas.UTP_HISTORIAL) },
                    alSalir = salir,
                )
            }
        }
        composable(Rutas.UTP_UNIDAD, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
            RequiereRol(sesion, Rol.UTP, salir) { UnidadFormScreen(alVolver = { nav.popBackStack() }) }
        }
        composable(Rutas.UTP_HISTORIAL) {
            RequiereRol(sesion, Rol.UTP, salir) { HistorialScreen(alVolver = { nav.popBackStack() }) }
        }

        // ----------------------------- DOCENTE -----------------------------
        composable(Rutas.DOCENTE) {
            RequiereRol(sesion, Rol.DOCENTE, salir) {
                DocenteHomeScreen(
                    alAsignar = { nav.navigate(Rutas.DOCENTE_ASIGNAR) },
                    alVerInforme = { nav.navigate(Rutas.docenteInforme(it)) },
                    alSalir = salir,
                )
            }
        }
        composable(Rutas.DOCENTE_ASIGNAR) {
            RequiereRol(sesion, Rol.DOCENTE, salir) { AsignacionFormScreen(alVolver = { nav.popBackStack() }) }
        }
        composable(Rutas.DOCENTE_INFORME, arguments = listOf(navArgument("estudianteId") { type = NavType.LongType })) {
            RequiereRol(sesion, Rol.DOCENTE, salir) { InformeScreen(alVolver = { nav.popBackStack() }) }
        }

        // ---------------------------- ESTUDIANTE ----------------------------
        composable(Rutas.ESTUDIANTE) {
            RequiereRol(sesion, Rol.ESTUDIANTE, salir) {
                EstudianteHomeScreen(
                    alAprender = { nav.navigate(Rutas.aprender(it)) },
                    alPracticar = { nav.navigate(Rutas.practica(it)) },
                    alPrueba = { nav.navigate(Rutas.prueba(it)) },
                    alSalir = salir,
                )
            }
        }
        composable(Rutas.EST_APRENDER, arguments = argAsignacion) { entrada ->
            val id = entrada.arguments?.getLong("asignacionId") ?: 0L
            RequiereRol(sesion, Rol.ESTUDIANTE, salir) {
                AprenderScreen(
                    alVolver = { nav.popBackStack() },
                    alPracticar = { nav.navigate(Rutas.practica(id)) { popUpTo(Rutas.ESTUDIANTE) } },
                )
            }
        }
        composable(Rutas.EST_PRACTICA, arguments = argAsignacion) {
            RequiereRol(sesion, Rol.ESTUDIANTE, salir) { PracticaScreen(alVolver = { nav.popBackStack(Rutas.ESTUDIANTE, false) }) }
        }
        composable(Rutas.EST_PRUEBA, arguments = argAsignacion) {
            RequiereRol(sesion, Rol.ESTUDIANTE, salir) {
                PruebaScreen(
                    alVolver = { nav.popBackStack() },
                    // Al entregar, la prueba sale de la pila: no se puede "volver" a cambiar respuestas.
                    alTerminar = { intento -> nav.navigate(Rutas.revision(intento)) { popUpTo(Rutas.ESTUDIANTE) } },
                )
            }
        }
        composable(Rutas.EST_REVISION, arguments = listOf(navArgument("intentoId") { type = NavType.LongType })) {
            RequiereRol(sesion, Rol.ESTUDIANTE, salir) { RevisionScreen(alInicio = { nav.popBackStack(Rutas.ESTUDIANTE, false) }) }
        }
    }
}

/**
 * Bloqueo de seguridad por rol: si la sesión no corresponde (p. ej. un estudiante
 * intentando abrir un informe), no se dibuja el contenido y se vuelve al ingreso.
 */
@Composable
private fun RequiereRol(sesion: SesionRepository, rol: Rol, alDenegar: () -> Unit, contenido: @Composable () -> Unit) {
    val usuario by sesion.usuario.collectAsStateWithLifecycle()
    if (usuario?.rol == rol) {
        contenido()
    } else {
        LaunchedEffect(Unit) { alDenegar() }
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Text("🔒", style = MaterialTheme.typography.displaySmall)
            Text("Sección reservada", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        }
    }
}
