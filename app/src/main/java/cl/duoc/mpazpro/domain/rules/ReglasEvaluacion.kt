package cl.duoc.mpazpro.domain.rules

import cl.duoc.mpazpro.domain.model.NivelLogro
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Resultado de la verificación previa a rendir la prueba final. */
sealed interface Habilitacion {
    data class Permitida(val intentoNumero: Int, val restantes: Int) : Habilitacion
    data class Bloqueada(val motivo: String) : Habilitacion
}

/** Control estricto de intentos parametrizado por el docente (criterio de éxito 4.1). */
object PoliticaIntentos {
    fun verificar(usados: Int, maximo: Int, hoy: LocalDate, desde: LocalDate, hasta: LocalDate): Habilitacion = when {
        hoy.isBefore(desde) -> Habilitacion.Bloqueada("La prueba se abre el ${desde.formatoCorto()}")
        hoy.isAfter(hasta) -> Habilitacion.Bloqueada("La prueba cerró el ${hasta.formatoCorto()}")
        usados >= maximo -> Habilitacion.Bloqueada("Ya usaste tus $maximo intentos")
        else -> Habilitacion.Permitida(intentoNumero = usados + 1, restantes = maximo - usados - 1)
    }
}

/**
 * Diagnóstico por reglas fijas y transparentes. El caso prohíbe diagnósticos
 * automáticos por IA, por eso los umbrales son explícitos y el docente siempre
 * agrega su observación antes de validar el informe.
 */
object Calificador {
    const val UMBRAL_FORTALEZA = 80
    const val UMBRAL_DESARROLLO = 60

    fun porcentaje(puntaje: Int, maximo: Int): Int =
        if (maximo <= 0) 0 else Math.round(puntaje * 100f / maximo)

    fun nivel(porcentaje: Int?): NivelLogro = when {
        porcentaje == null -> NivelLogro.SIN_DATOS
        porcentaje >= UMBRAL_FORTALEZA -> NivelLogro.FORTALEZA
        porcentaje >= UMBRAL_DESARROLLO -> NivelLogro.EN_DESARROLLO
        else -> NivelLogro.REFUERZO
    }

    /** Avance de práctica: contenidos vistos + actividades respondidas sobre el total. */
    fun avance(hechos: Int, total: Int): Float =
        if (total <= 0) 0f else (hechos.toFloat() / total).coerceIn(0f, 1f)
}

private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Marca de tiempo en el formato pedido por el caso: YYYY-MM-DD HH:MM. */
fun Long.comoMarcaDeTiempo(zona: ZoneId = ZoneId.systemDefault()): String =
    FORMATO_FECHA_HORA.format(Instant.ofEpochMilli(this).atZone(zona))

fun LocalDate.formatoCorto(): String = FORMATO_FECHA.format(this)
