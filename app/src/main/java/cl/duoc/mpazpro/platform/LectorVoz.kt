package cl.duoc.mpazpro.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * RECURSO NATIVO 1 — Motor Text-to-Speech del dispositivo.
 * Lee en voz alta instrucciones y explicaciones para lectores iniciales.
 * Es reproducción pura (passthrough): no usa micrófono, tal como exige el caso.
 */
class LectorVoz(context: Context) {

    enum class Estado { INICIANDO, LISTO, HABLANDO, NO_DISPONIBLE }

    private val _estado = MutableStateFlow(Estado.INICIANDO)
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                _estado.value = Estado.NO_DISPONIBLE
                return@TextToSpeech
            }
            val motor = tts ?: return@TextToSpeech
            // Preferimos español de Chile; si el dispositivo no lo tiene, español genérico.
            val idioma = listOf(Locale("es", "CL"), Locale("es", "ES"), Locale("es"))
                .firstOrNull { motor.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
            if (idioma == null) {
                _estado.value = Estado.NO_DISPONIBLE
                return@TextToSpeech
            }
            motor.language = idioma
            motor.setSpeechRate(0.85f) // más lento: apoyo para lectores iniciales
            motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { _estado.value = Estado.HABLANDO }
                override fun onDone(id: String?) { _estado.value = Estado.LISTO }
                @Deprecated("Requerido por la API") override fun onError(id: String?) { _estado.value = Estado.LISTO }
            })
            _estado.value = Estado.LISTO
        }
    }

    fun leer(texto: String) {
        val motor = tts ?: return
        if (_estado.value == Estado.NO_DISPONIBLE || _estado.value == Estado.INICIANDO) return
        motor.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "mpaz-${texto.hashCode()}")
    }

    fun detener() {
        tts?.stop()
        if (_estado.value == Estado.HABLANDO) _estado.value = Estado.LISTO
    }

    fun liberar() {
        tts?.shutdown()
        tts = null
    }
}
