package cl.duoc.mpazpro.ui.utp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.mpazpro.data.local.entity.AsignaturaEntity
import cl.duoc.mpazpro.data.local.entity.HistorialEntity
import cl.duoc.mpazpro.data.local.entity.UnidadResumen
import cl.duoc.mpazpro.data.repository.CurriculoRepository
import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.validation.Resultado
import cl.duoc.mpazpro.domain.validation.ValidadorUnidad
import cl.duoc.mpazpro.domain.validation.error
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UtpViewModel(private val curriculo: CurriculoRepository) : ViewModel() {

    val unidades: StateFlow<List<UnidadResumen>?> =
        curriculo.unidades.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val historial: StateFlow<List<HistorialEntity>> =
        curriculo.historial.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun cambiarEstado(unidadId: Long, nuevo: EstadoContenido) = viewModelScope.launch {
        runCatching { curriculo.cambiarEstado(unidadId, nuevo) }
            .onSuccess { _mensaje.value = "Unidad movida a «${nuevo.etiqueta}»" }
            .onFailure { _mensaje.value = it.message ?: "No se pudo cambiar el estado" }
    }

    fun mensajeMostrado() { _mensaje.value = null }
}

// ------------------------- Formulario de unidad + OA -------------------------

data class UnidadFormState(
    val esNueva: Boolean = true,
    val cargando: Boolean = true,
    val asignaturas: List<AsignaturaEntity> = emptyList(),
    val asignaturaId: Long? = null,
    val titulo: String = "",
    val textoReferencia: String = "",
    val codigoOa: String = "",
    val descripcionOa: String = "",
    val eje: String = "",
    val origen: String? = null,
    val errores: Map<Campo, String> = emptyMap(),
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val avisoRevision: Boolean = false,
) {
    enum class Campo { ASIGNATURA, TITULO, CODIGO, DESCRIPCION, EJE, ORIGEN }
}

class UnidadFormViewModel(
    private val curriculo: CurriculoRepository,
    estado: SavedStateHandle,
) : ViewModel() {

    private val unidadId: Long? = estado.get<Long>("id")?.takeIf { it > 0 }
    private val _ui = MutableStateFlow(UnidadFormState(esNueva = unidadId == null))
    val ui: StateFlow<UnidadFormState> = _ui.asStateFlow()
    private var validarEnVivo = false

    val origenes = listOf("Propio (equipo docente)", "Dominio público", "Mineduc (licencia educativa)", "Autorizado por terceros")

    init {
        viewModelScope.launch {
            curriculo.asignaturas.collect { a -> _ui.update { it.copy(asignaturas = a) } }
        }
        viewModelScope.launch {
            if (unidadId != null) {
                val u = curriculo.unidad(unidadId)
                val oa = curriculo.objetivosDeUnidad(unidadId).firstOrNull()
                if (u != null) _ui.update {
                    it.copy(
                        asignaturaId = u.asignaturaId, titulo = u.titulo, textoReferencia = u.textoReferencia,
                        codigoOa = oa?.codigo.orEmpty(), descripcionOa = oa?.descripcion.orEmpty(), eje = oa?.eje.orEmpty(),
                        avisoRevision = u.estado != EstadoContenido.BORRADOR,
                    )
                }
            }
            _ui.update { it.copy(cargando = false) }
        }
    }

    fun asignatura(id: Long) = actualizar { it.copy(asignaturaId = id) }
    fun titulo(v: String) = actualizar { it.copy(titulo = v.take(80)) }
    fun textoReferencia(v: String) = actualizar { it.copy(textoReferencia = v.take(1200)) }
    fun codigo(v: String) = actualizar { it.copy(codigoOa = v.uppercase().take(12)) }
    fun descripcion(v: String) = actualizar { it.copy(descripcionOa = v.take(300)) }
    fun eje(v: String) = actualizar { it.copy(eje = v.take(60)) }
    fun origen(v: String) = actualizar { it.copy(origen = v) }

    private fun actualizar(cambio: (UnidadFormState) -> UnidadFormState) {
        _ui.update { s -> cambio(s).let { n -> if (validarEnVivo) n.copy(errores = validar(n)) else n } }
    }

    /** Toda la validación vive aquí y en ValidadorUnidad: la pantalla solo la muestra. */
    private fun validar(s: UnidadFormState): Map<UnidadFormState.Campo, String> = buildMap {
        ValidadorUnidad.asignatura(s.asignaturaId).error?.let { put(UnidadFormState.Campo.ASIGNATURA, it) }
        ValidadorUnidad.titulo(s.titulo).error?.let { put(UnidadFormState.Campo.TITULO, it) }
        ValidadorUnidad.codigoOa(s.codigoOa).error?.let { put(UnidadFormState.Campo.CODIGO, it) }
        ValidadorUnidad.descripcionOa(s.descripcionOa).error?.let { put(UnidadFormState.Campo.DESCRIPCION, it) }
        ValidadorUnidad.eje(s.eje).error?.let { put(UnidadFormState.Campo.EJE, it) }
        ValidadorUnidad.origen(s.origen).error?.let { put(UnidadFormState.Campo.ORIGEN, it) }
    }

    fun campoOk(campo: UnidadFormState.Campo): Boolean {
        val s = _ui.value
        val r = when (campo) {
            UnidadFormState.Campo.TITULO -> ValidadorUnidad.titulo(s.titulo)
            UnidadFormState.Campo.CODIGO -> ValidadorUnidad.codigoOa(s.codigoOa)
            UnidadFormState.Campo.DESCRIPCION -> ValidadorUnidad.descripcionOa(s.descripcionOa)
            UnidadFormState.Campo.EJE -> ValidadorUnidad.eje(s.eje)
            else -> Resultado.Invalido("")
        }
        return r == Resultado.Valido
    }

    fun guardar() {
        validarEnVivo = true
        val s = _ui.value
        val errores = validar(s)
        _ui.update { it.copy(errores = errores) }
        if (errores.isNotEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(guardando = true) }
            curriculo.guardarUnidad(
                unidadId, s.asignaturaId!!, s.titulo, s.textoReferencia, s.codigoOa, s.descripcionOa, s.eje, s.origen!!,
            )
            _ui.update { it.copy(guardando = false, guardado = true) }
        }
    }
}
