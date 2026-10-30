package cl.duoc.mpazpro.ui.estudiante

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.mpazpro.data.local.entity.AsignacionDetalle
import cl.duoc.mpazpro.data.local.entity.AsignacionEntity
import cl.duoc.mpazpro.data.local.entity.ContenidoEntity
import cl.duoc.mpazpro.data.local.entity.OaDetalle
import cl.duoc.mpazpro.data.local.entity.PreguntaEntity
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.di.AppContainer
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.rules.Calificador
import cl.duoc.mpazpro.domain.rules.Habilitacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ----------------------------- Inicio del estudiante -----------------------------

data class TarjetaOa(val detalle: AsignacionDetalle, val avance: Float)

class EstudianteHomeViewModel(private val c: AppContainer) : ViewModel() {
    val estudiante: UsuarioEntity = c.sesion.exigirRol(Rol.ESTUDIANTE)

    /** Solo las asignaciones vigentes de SU curso: no ve nada que el docente no haya asignado. */
    val tarjetas: StateFlow<List<TarjetaOa>?> = combine(
        c.asignaciones.vigentesDeCurso(estudiante.curso.orEmpty()),
        c.evaluacion.progreso(estudiante.id),
        c.evaluacion.contenidosVistos(estudiante.id),
    ) { asignaciones, progreso, vistos ->
        asignaciones.map { a ->
            val oaId = a.asignacion.oaId
            val total = c.curriculo.contenidos(oaId).size + c.curriculo.practica(oaId).size
            val hechos = vistos.count { it.oaId == oaId } + progreso.count { it.oaId == oaId }
            TarjetaOa(a, Calificador.avance(hechos, total))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cerrarSesion() = c.sesion.cerrar()
}

// ------------------------- Aprender: explicaciones del OA -------------------------

data class AprenderState(
    val cargando: Boolean = true,
    val oa: OaDetalle? = null,
    val contenidos: List<ContenidoEntity> = emptyList(),
    val indice: Int = 0,
) {
    val actual get() = contenidos.getOrNull(indice)
    val esUltimo get() = indice >= contenidos.lastIndex
}

class AprenderViewModel(private val c: AppContainer, estado: SavedStateHandle) : ViewModel() {
    private val asignacionId: Long = checkNotNull(estado["asignacionId"])
    private val _ui = MutableStateFlow(AprenderState())
    val ui: StateFlow<AprenderState> = _ui.asStateFlow()

    init {
        c.sesion.exigirRol(Rol.ESTUDIANTE)
        viewModelScope.launch {
            val a = c.asignaciones.porId(asignacionId) ?: return@launch
            val contenidos = c.curriculo.contenidos(a.oaId)
            _ui.value = AprenderState(cargando = false, oa = c.curriculo.oa(a.oaId), contenidos = contenidos)
            contenidos.firstOrNull()?.let { c.evaluacion.marcarContenidoVisto(it) }
        }
    }

    fun ir(delta: Int) {
        val s = _ui.value
        val nuevo = (s.indice + delta).coerceIn(0, (s.contenidos.size - 1).coerceAtLeast(0))
        _ui.update { it.copy(indice = nuevo) }
        // Guardado en tiempo real del avance
        s.contenidos.getOrNull(nuevo)?.let { viewModelScope.launch { c.evaluacion.marcarContenidoVisto(it) } }
    }
}

// ------------------------- Práctica: actividades y desafíos -------------------------

enum class Feedback { NINGUNO, CORRECTO, INCORRECTO }

data class PracticaState(
    val cargando: Boolean = true,
    val preguntas: List<PreguntaEntity> = emptyList(),
    val indice: Int = 0,
    val seleccion: Int? = null,
    val feedback: Feedback = Feedback.NINGUNO,
    val correctasPrimerIntento: Int = 0,
    val terminado: Boolean = false,
) {
    val actual get() = preguntas.getOrNull(indice)
    val fraccion get() = if (preguntas.isEmpty()) 0f else (indice + if (feedback == Feedback.CORRECTO) 1 else 0).toFloat() / preguntas.size
}

class PracticaViewModel(private val c: AppContainer, estado: SavedStateHandle) : ViewModel() {
    private val asignacionId: Long = checkNotNull(estado["asignacionId"])
    private val _ui = MutableStateFlow(PracticaState())
    val ui: StateFlow<PracticaState> = _ui.asStateFlow()
    private var falloEnEsta = false

    init {
        c.sesion.exigirRol(Rol.ESTUDIANTE)
        viewModelScope.launch {
            val a = c.asignaciones.porId(asignacionId) ?: return@launch
            _ui.value = PracticaState(cargando = false, preguntas = c.curriculo.practica(a.oaId))
        }
    }

    fun elegir(indice: Int) {
        val s = _ui.value
        val p = s.actual ?: return
        if (s.feedback == Feedback.CORRECTO) return
        val ok = indice == p.indiceCorrecto
        if (ok) {
            c.sensorial.acierto()
        } else {
            c.sensorial.error()
        }
        _ui.update {
            it.copy(
                seleccion = indice,
                feedback = if (ok) Feedback.CORRECTO else Feedback.INCORRECTO,
                correctasPrimerIntento = it.correctasPrimerIntento + if (ok && !falloEnEsta) 1 else 0,
            )
        }
        // El primer resultado de cada actividad se guarda al instante (guardado en tiempo real)
        if (ok || !falloEnEsta) viewModelScope.launch { c.evaluacion.guardarRespuestaPractica(p, ok && !falloEnEsta) }
        if (!ok) falloEnEsta = true
    }

    fun reintentar() = _ui.update { it.copy(seleccion = null, feedback = Feedback.NINGUNO) }

    fun siguiente() {
        falloEnEsta = false
        val s = _ui.value
        if (s.indice >= s.preguntas.lastIndex) {
            c.sensorial.logro()
            _ui.update { it.copy(terminado = true) }
        } else {
            _ui.update { it.copy(indice = it.indice + 1, seleccion = null, feedback = Feedback.NINGUNO) }
        }
    }
}

// ------------------------------- Prueba final -------------------------------

data class PruebaState(
    val cargando: Boolean = true,
    val asignacion: AsignacionEntity? = null,
    val textoReferencia: String = "",
    val habilitacion: Habilitacion? = null,
    val preguntas: List<PreguntaEntity> = emptyList(),
    val respuestas: Map<Long, Int> = emptyMap(),   // preguntaId -> alternativa
    val indice: Int = 0,
    val revisando: Boolean = false,
    val enviando: Boolean = false,
    val intentoId: Long? = null,
    val error: String? = null,
) {
    val actual get() = preguntas.getOrNull(indice)
    val completa get() = preguntas.isNotEmpty() && preguntas.all { it.id in respuestas }
}

class PruebaViewModel(private val c: AppContainer, estado: SavedStateHandle) : ViewModel() {
    private val asignacionId: Long = checkNotNull(estado["asignacionId"])
    private val _ui = MutableStateFlow(PruebaState())
    val ui: StateFlow<PruebaState> = _ui.asStateFlow()

    init {
        c.sesion.exigirRol(Rol.ESTUDIANTE)
        viewModelScope.launch {
            val a = c.asignaciones.porId(asignacionId) ?: return@launch
            val oa = c.curriculo.oa(a.oaId)
            val texto = oa?.let { c.curriculo.unidad(it.unidadId)?.textoReferencia }.orEmpty()
            _ui.value = PruebaState(
                cargando = false, asignacion = a, textoReferencia = texto,
                habilitacion = c.evaluacion.verificarHabilitacion(a), preguntas = c.curriculo.pruebaFinal(a.oaId),
            )
        }
    }

    fun responder(preguntaId: Long, alternativa: Int) =
        _ui.update { it.copy(respuestas = it.respuestas + (preguntaId to alternativa)) }

    fun ir(delta: Int) = _ui.update { it.copy(indice = (it.indice + delta).coerceIn(0, it.preguntas.lastIndex)) }
    fun irA(i: Int) = _ui.update { it.copy(indice = i, revisando = false) }
    fun revisar(v: Boolean) = _ui.update { it.copy(revisando = v) }

    fun enviar() {
        val s = _ui.value
        val a = s.asignacion ?: return
        if (!s.completa || s.enviando) return  // evita doble envío
        viewModelScope.launch {
            _ui.update { it.copy(enviando = true) }
            runCatching {
                c.evaluacion.rendirPrueba(a, s.preguntas.associateWith { s.respuestas.getValue(it.id) })
            }.onSuccess { id ->
                c.sensorial.logro()
                _ui.update { it.copy(enviando = false, intentoId = id) }
            }.onFailure { e ->
                _ui.update { it.copy(enviando = false, error = e.message) }
            }
        }
    }
}

// ------------------------- Revisión del intento (estudiante) -------------------------

data class FilaRevision(val pregunta: PreguntaEntity, val elegida: Int, val correcta: Boolean)

data class RevisionState(
    val cargando: Boolean = true,
    val puntaje: Int = 0,
    val maximo: Int = 0,
    val numero: Int = 0,
    val filas: List<FilaRevision> = emptyList(),
)

class RevisionViewModel(private val c: AppContainer, estado: SavedStateHandle) : ViewModel() {
    private val intentoId: Long = checkNotNull(estado["intentoId"])
    private val _ui = MutableStateFlow(RevisionState())
    val ui: StateFlow<RevisionState> = _ui.asStateFlow()

    init {
        val est = c.sesion.exigirRol(Rol.ESTUDIANTE)
        viewModelScope.launch {
            val intento = c.evaluacion.intento(intentoId) ?: return@launch
            // Solo puede revisar sus propios intentos
            check(intento.estudianteId == est.id) { "Intento ajeno" }
            val respuestas = c.evaluacion.respuestasDe(intentoId)
            val preguntas = c.curriculo.preguntas(respuestas.map { it.preguntaId }).associateBy { it.id }
            _ui.value = RevisionState(
                cargando = false, puntaje = intento.puntaje, maximo = intento.puntajeMaximo, numero = intento.numero,
                filas = respuestas.mapNotNull { r -> preguntas[r.preguntaId]?.let { FilaRevision(it, r.indiceSeleccionado, r.correcta) } }
                    .sortedBy { it.pregunta.orden },
            )
        }
    }
}
