package cl.duoc.mpazpro.ui.docente

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.mpazpro.data.local.entity.AsignacionDetalle
import cl.duoc.mpazpro.data.local.entity.OaDetalle
import cl.duoc.mpazpro.data.local.entity.ObservacionEntity
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.data.repository.InformeEstudiante
import cl.duoc.mpazpro.data.repository.ResumenOaCurso
import cl.duoc.mpazpro.di.AppContainer
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.validation.ValidadorAsignacion
import cl.duoc.mpazpro.domain.validation.ValidadorObservacion
import cl.duoc.mpazpro.domain.validation.error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

// ------------------------------ Panel del docente ------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class DocenteViewModel(private val c: AppContainer) : ViewModel() {
    // Bloqueo de seguridad: si no es docente, el ViewModel no se puede construir.
    val docente: UsuarioEntity = c.sesion.exigirRol(Rol.DOCENTE)
    private val curso = docente.curso.orEmpty()

    val asignaciones: StateFlow<List<AsignacionDetalle>?> = c.asignaciones.deDocente(docente.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val estudiantes: StateFlow<List<UsuarioEntity>> = c.auth.estudiantesDeCurso(curso)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Rendimiento agrupado por OA; se recalcula si cambian asignaciones o intentos. */
    val resumen: StateFlow<List<ResumenOaCurso>?> =
        combine(c.asignaciones.deDocente(docente.id), c.auth.estudiantesDeCurso(curso)) { _, est -> est }
            .flatMapLatest { est ->
                if (est.isEmpty()) flowOf(est) else
                    // Cualquier intento nuevo de un estudiante del curso dispara el recálculo
                    combine(est.map { c.evaluacion.intentosObservados(it.id) }) { est }
            }
            .mapLatest { est -> c.informes.resumenCurso(curso, est) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cerrarSesion() = c.sesion.cerrar()
}

// --------------------------- Formulario de asignación ---------------------------

data class AsignacionFormState(
    val oas: List<OaDetalle> = emptyList(),
    val oaId: Long? = null,
    val curso: String = "",
    val desde: LocalDate? = LocalDate.now(),
    val hasta: LocalDate? = LocalDate.now().plusDays(14),
    val maxIntentos: String = "2",
    val errorOa: String? = null,
    val errorCurso: String? = null,
    val errorFechas: String? = null,
    val errorIntentos: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val avisoEnviado: Boolean? = null,
)

class AsignacionFormViewModel(private val c: AppContainer) : ViewModel() {
    private val docente = c.sesion.exigirRol(Rol.DOCENTE)
    private val _ui = MutableStateFlow(AsignacionFormState(curso = docente.curso.orEmpty()))
    val ui: StateFlow<AsignacionFormState> = _ui.asStateFlow()
    private var validarEnVivo = false

    init {
        viewModelScope.launch {
            c.curriculo.oaVisiblesDocente.collect { l -> _ui.update { it.copy(oas = l) } }
        }
    }

    fun oa(id: Long) = actualizar { it.copy(oaId = id) }
    fun curso(v: String) = actualizar { it.copy(curso = v.uppercase().take(3)) }
    fun desde(d: LocalDate) = actualizar { it.copy(desde = d) }
    fun hasta(d: LocalDate) = actualizar { it.copy(hasta = d) }
    fun maxIntentos(v: String) = actualizar { it.copy(maxIntentos = v.filter(Char::isDigit).take(1)) }
    fun sumarIntento(delta: Int) {
        val n = (_ui.value.maxIntentos.toIntOrNull() ?: 0) + delta
        maxIntentos(n.coerceIn(ValidadorAsignacion.MIN_INTENTOS, ValidadorAsignacion.MAX_INTENTOS).toString())
    }

    private fun actualizar(cambio: (AsignacionFormState) -> AsignacionFormState) =
        _ui.update { s -> cambio(s).let { if (validarEnVivo) validar(it) else it } }

    private fun validar(s: AsignacionFormState) = s.copy(
        errorOa = ValidadorAsignacion.oa(s.oaId).error,
        errorCurso = ValidadorAsignacion.curso(s.curso).error,
        errorFechas = ValidadorAsignacion.rangoFechas(s.desde, s.hasta, LocalDate.now()).error,
        errorIntentos = ValidadorAsignacion.maxIntentos(s.maxIntentos).error,
    )

    fun guardar() {
        validarEnVivo = true
        val s = validar(_ui.value)
        _ui.value = s
        if (listOf(s.errorOa, s.errorCurso, s.errorFechas, s.errorIntentos).any { it != null }) return

        viewModelScope.launch {
            _ui.update { it.copy(guardando = true) }
            // Regla de negocio que requiere la BD: no duplicar un OA vigente en el mismo curso.
            val duplicado = ValidadorAsignacion.noDuplicada(c.asignaciones.vigentesMismoOa(s.curso, s.oaId!!)).error
            if (duplicado != null) {
                _ui.update { it.copy(guardando = false, errorOa = duplicado) }
                return@launch
            }
            val id = c.asignaciones.crear(s.oaId, s.curso, s.desde!!, s.hasta!!, s.maxIntentos.toInt())
            val oa = s.oas.first { it.oaId == s.oaId }
            val avisado = c.notificador.avisar(
                id.toInt(), "Actividad asignada a ${s.curso}",
                "${oa.asignaturaIcono} ${oa.asignaturaNombre}: ${oa.codigo}. Disponible hasta ${s.hasta}.",
            )
            _ui.update { it.copy(guardando = false, guardado = true, avisoEnviado = avisado) }
        }
    }
}

// ---------------------------------- Informe ----------------------------------

data class InformeState(
    val cargando: Boolean = true,
    val informe: InformeEstudiante? = null,
    val observaciones: List<ObservacionEntity> = emptyList(),
    val oaObservacion: Long? = null,
    val texto: String = "",
    val confirmado: Boolean = false,
    val errorTexto: String? = null,
    val errorConfirmado: String? = null,
    val mensaje: String? = null,
    val pdf: File? = null,
    val accionPdf: AccionPdf? = null,
    val error: String? = null,
) { enum class AccionPdf { COMPARTIR, GUARDAR } }

class InformeViewModel(private val c: AppContainer, estado: SavedStateHandle) : ViewModel() {
    private val estudianteId: Long = checkNotNull(estado["estudianteId"])
    private val _ui = MutableStateFlow(InformeState())
    val ui: StateFlow<InformeState> = _ui.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            c.observaciones.de(estudianteId).collect { o -> _ui.update { it.copy(observaciones = o) } }
        }
    }

    private fun cargar() = viewModelScope.launch {
        runCatching { c.informes.informeDe(estudianteId) }
            .onSuccess { inf ->
                _ui.update { it.copy(cargando = false, informe = inf, oaObservacion = it.oaObservacion ?: inf.resultados.firstOrNull()?.oa?.oaId) }
            }
            .onFailure { e -> _ui.update { it.copy(cargando = false, error = e.message) } }
    }

    fun texto(v: String) = _ui.update {
        it.copy(texto = v.take(ValidadorObservacion.MAX), errorTexto = if (it.errorTexto != null) ValidadorObservacion.texto(v).error else null)
    }
    fun confirmado(v: Boolean) = _ui.update { it.copy(confirmado = v, errorConfirmado = null) }
    fun oaObservacion(id: Long) = _ui.update { it.copy(oaObservacion = id) }

    fun guardarObservacion() {
        val s = _ui.value
        val eT = ValidadorObservacion.texto(s.texto).error
        val eC = ValidadorObservacion.validacionConfirmada(s.confirmado).error
        _ui.update { it.copy(errorTexto = eT, errorConfirmado = eC) }
        if (eT != null || eC != null || s.oaObservacion == null) return
        viewModelScope.launch {
            c.observaciones.guardar(estudianteId, s.oaObservacion, s.texto)
            _ui.update { it.copy(texto = "", confirmado = false, mensaje = "Observación registrada e informe validado") }
        }
    }

    fun exportar(accion: InformeState.AccionPdf) = viewModelScope.launch {
        val s = _ui.value
        val inf = s.informe ?: return@launch
        val archivo = withContext(Dispatchers.IO) {
            c.exportador.generarPdf(inf, s.observaciones)
        }
        _ui.update { it.copy(pdf = archivo, accionPdf = accion) }
    }

    fun accionPdfConsumida() = _ui.update { it.copy(accionPdf = null) }
    fun mensaje(m: String?) = _ui.update { it.copy(mensaje = m) }
}
