package cl.duoc.mpazpro.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import cl.duoc.mpazpro.R

/**
 * RECURSO NATIVO 2 — Salida de audio (SoundPool) y RECURSO NATIVO 3 — Vibrador.
 * Dan una confirmación inmediata y multisensorial al responder: útil para
 * estudiantes que aún no leen con autonomía. Los sonidos son originales
 * (generados por el equipo) y la vibración requiere el permiso VIBRATE.
 */
class RetroalimentacionSensorial(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sCorrecto = soundPool.load(context, R.raw.sonido_correcto, 1)
    private val sReintentar = soundPool.load(context, R.raw.sonido_reintentar, 1)
    private val sLogro = soundPool.load(context, R.raw.sonido_logro, 1)

    private val vibrador: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    var sonidoActivado = true
    var vibracionActivada = true

    fun acierto() {
        if (sonidoActivado) soundPool.play(sCorrecto, 1f, 1f, 1, 0, 1f)
        vibrar(longArrayOf(0, 40))
    }

    fun error() {
        if (sonidoActivado) soundPool.play(sReintentar, 0.8f, 0.8f, 1, 0, 1f)
        vibrar(longArrayOf(0, 60, 80, 60)) // doble pulso suave: "inténtalo otra vez"
    }

    fun logro() {
        if (sonidoActivado) soundPool.play(sLogro, 1f, 1f, 1, 0, 1f)
        vibrar(longArrayOf(0, 30, 50, 30, 50, 90))
    }

    private fun vibrar(patron: LongArray) {
        val v = vibrador ?: return
        if (!vibracionActivada || !v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(patron, -1))
    }

    fun liberar() = soundPool.release()
}
