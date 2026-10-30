package cl.duoc.mpazpro.domain.validation

import java.time.LocalDate

/**
 * Capa de validación PURA: no depende de Android ni de Compose.
 * Los ViewModels la llaman y exponen el resultado como estado; las pantallas
 * solo pintan el error. Así la lógica queda desacoplada de la UI (IE 2.2.1)
 * y se prueba con JUnit sin emulador.
 */
sealed interface Resultado {
    data object Valido : Resultado
    data class Invalido(val mensaje: String) : Resultado
}

val Resultado.error: String? get() = (this as? Resultado.Invalido)?.mensaje

private fun invalido(m: String) = Resultado.Invalido(m)

object ValidadorLogin {
    fun perfil(idSeleccionado: Long?): Resultado =
        if (idSeleccionado == null) invalido("Elige tu perfil para continuar") else Resultado.Valido

    fun pin(pin: String): Resultado = when {
        pin.isBlank() -> invalido("Escribe tu PIN")
        !pin.all(Char::isDigit) -> invalido("El PIN solo lleva números")
        pin.length != 4 -> invalido("El PIN tiene 4 números")
        else -> Resultado.Valido
    }
}

object ValidadorUnidad {
    /** Formato referencial: sigla de asignatura + nivel + "OA" + número. Ej: "MA02 OA 09". */
    val PATRON_CODIGO_OA = Regex("^[A-Z]{2}0?2 OA \\d{1,2}$")

    fun asignatura(id: Long?): Resultado =
        if (id == null) invalido("Selecciona una asignatura") else Resultado.Valido

    fun titulo(t: String): Resultado {
        val v = t.trim()
        return when {
            v.isEmpty() -> invalido("El título es obligatorio")
            v.length < 5 -> invalido("Mínimo 5 caracteres")
            v.length > 80 -> invalido("Máximo 80 caracteres")
            else -> Resultado.Valido
        }
    }

    fun codigoOa(c: String): Resultado {
        val v = c.trim().uppercase()
        return when {
            v.isEmpty() -> invalido("El código del OA es obligatorio")
            !PATRON_CODIGO_OA.matches(v) -> invalido("Formato esperado: MA02 OA 09")
            else -> Resultado.Valido
        }
    }

    fun descripcionOa(d: String): Resultado {
        val v = d.trim()
        return when {
            v.isEmpty() -> invalido("Describe el objetivo de aprendizaje")
            v.length < 15 -> invalido("Mínimo 15 caracteres")
            v.length > 300 -> invalido("Máximo 300 caracteres")
            else -> Resultado.Valido
        }
    }

    fun eje(e: String): Resultado =
        if (e.trim().length < 3) invalido("Indica el eje curricular") else Resultado.Valido

    /** Derechos de autor (restricción 5.1): el origen del material debe declararse. */
    fun origen(o: String?): Resultado =
        if (o.isNullOrBlank()) invalido("Declara el origen del material") else Resultado.Valido
}

object ValidadorAsignacion {
    const val MIN_INTENTOS = 1
    const val MAX_INTENTOS = 5
    private val PATRON_CURSO = Regex("^2°[A-Z]$")

    fun oa(id: Long?): Resultado =
        if (id == null) invalido("Selecciona un objetivo de aprendizaje") else Resultado.Valido

    fun curso(c: String): Resultado = when {
        c.isBlank() -> invalido("Indica el curso")
        !PATRON_CURSO.matches(c.trim()) -> invalido("Formato de curso: 2°A")
        else -> Resultado.Valido
    }

    fun rangoFechas(desde: LocalDate?, hasta: LocalDate?, hoy: LocalDate): Resultado = when {
        desde == null || hasta == null -> invalido("Elige fecha de inicio y de término")
        hasta.isBefore(desde) -> invalido("El término debe ser igual o posterior al inicio")
        hasta.isBefore(hoy) -> invalido("La fecha de término ya pasó")
        desde.plusDays(90).isBefore(hasta) -> invalido("El rango no puede superar 90 días")
        else -> Resultado.Valido
    }

    fun maxIntentos(texto: String): Resultado {
        val n = texto.trim().toIntOrNull()
        return when {
            texto.isBlank() -> invalido("Indica el número de intentos")
            n == null -> invalido("Debe ser un número")
            n < MIN_INTENTOS || n > MAX_INTENTOS -> invalido("Entre $MIN_INTENTOS y $MAX_INTENTOS intentos")
            else -> Resultado.Valido
        }
    }

    fun noDuplicada(vigentesMismoOa: Int): Resultado =
        if (vigentesMismoOa > 0) invalido("Este OA ya está asignado y vigente para el curso") else Resultado.Valido
}

object ValidadorObservacion {
    const val MAX = 500

    fun texto(t: String): Resultado {
        val v = t.trim()
        return when {
            v.isEmpty() -> invalido("Escribe la observación pedagógica")
            v.length < 10 -> invalido("Mínimo 10 caracteres")
            v.length > MAX -> invalido("Máximo $MAX caracteres")
            else -> Resultado.Valido
        }
    }

    fun validacionConfirmada(confirmado: Boolean): Resultado =
        if (!confirmado) invalido("Confirma que revisaste el informe") else Resultado.Valido
}
