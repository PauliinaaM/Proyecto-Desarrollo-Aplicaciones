package cl.duoc.mpazpro.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.data.repository.AuthRepository
import cl.duoc.mpazpro.data.repository.ResultadoLogin
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.validation.Resultado
import cl.duoc.mpazpro.domain.validation.ValidadorLogin
import cl.duoc.mpazpro.domain.validation.error
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val perfiles: List<UsuarioEntity> = emptyList(),
    val rolFiltro: Rol = Rol.ESTUDIANTE,
    val seleccionado: Long? = null,
    val pin: String = "",
    val errorPerfil: String? = null,
    val errorPin: String? = null,
    val cargando: Boolean = false,
    val intentosFallidos: Int = 0,   // dispara la animación de "sacudir"
    val ingresoComo: Rol? = null,    // evento de navegación
) {
    val perfilesFiltrados get() = perfiles.filter { it.rol == rolFiltro }
    val pinValido get() = ValidadorLogin.pin(pin) == Resultado.Valido
}

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    private var intentoEnviar = false

    init {
        viewModelScope.launch {
            auth.perfiles.collect { lista -> _ui.update { it.copy(perfiles = lista) } }
        }
    }

    fun elegirRol(rol: Rol) = _ui.update { it.copy(rolFiltro = rol, seleccionado = null, pin = "", errorPin = null, errorPerfil = null) }

    fun elegirPerfil(id: Long) = _ui.update { it.copy(seleccionado = id, errorPerfil = null) }

    fun cambiarPin(valor: String) {
        val limpio = valor.take(4)
        // Tras el primer intento de ingreso, el error se recalcula mientras escribe.
        _ui.update { it.copy(pin = limpio, errorPin = if (intentoEnviar) ValidadorLogin.pin(limpio).error else null) }
    }

    fun ingresar() {
        intentoEnviar = true
        val s = _ui.value
        val ePerfil = ValidadorLogin.perfil(s.seleccionado).error
        val ePin = ValidadorLogin.pin(s.pin).error
        _ui.update { it.copy(errorPerfil = ePerfil, errorPin = ePin) }
        if (ePerfil != null || ePin != null) {
            _ui.update { it.copy(intentosFallidos = it.intentosFallidos + 1) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true) }
            when (val r = auth.ingresar(s.seleccionado!!, s.pin)) {
                is ResultadoLogin.Exito -> _ui.update { it.copy(cargando = false, ingresoComo = r.usuario.rol) }
                ResultadoLogin.PinIncorrecto -> _ui.update {
                    it.copy(cargando = false, errorPin = "PIN incorrecto. Pide ayuda a tu profesor/a", intentosFallidos = it.intentosFallidos + 1)
                }
            }
        }
    }

    fun navegacionConsumida() {
        intentoEnviar = false
        _ui.update { it.copy(ingresoComo = null, pin = "", seleccionado = null) }
    }
}
