package cl.duoc.mpazpro.domain

import cl.duoc.mpazpro.domain.validation.Resultado
import cl.duoc.mpazpro.domain.validation.ValidadorAsignacion
import cl.duoc.mpazpro.domain.validation.ValidadorLogin
import cl.duoc.mpazpro.domain.validation.ValidadorObservacion
import cl.duoc.mpazpro.domain.validation.ValidadorUnidad
import cl.duoc.mpazpro.domain.validation.error
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidacionTest {

    private val hoy = LocalDate.of(2026, 10, 24)

    // ---------- Login ----------
    @Test fun `pin valido de 4 digitos`() = assertEquals(Resultado.Valido, ValidadorLogin.pin("1234"))
    @Test fun `pin vacio`() = assertEquals("Escribe tu PIN", ValidadorLogin.pin("").error)
    @Test fun `pin con letras`() = assertEquals("El PIN solo lleva números", ValidadorLogin.pin("12a4").error)
    @Test fun `pin corto`() = assertEquals("El PIN tiene 4 números", ValidadorLogin.pin("123").error)
    @Test fun `perfil no elegido`() = assertTrue(ValidadorLogin.perfil(null) is Resultado.Invalido)

    // ---------- Unidad / OA ----------
    @Test fun `codigo OA valido`() {
        assertEquals(Resultado.Valido, ValidadorUnidad.codigoOa("MA02 OA 09"))
        assertEquals(Resultado.Valido, ValidadorUnidad.codigoOa("le02 oa 5")) // se normaliza a mayúsculas
    }
    @Test fun `codigo OA con formato incorrecto`() =
        assertEquals("Formato esperado: MA02 OA 09", ValidadorUnidad.codigoOa("OA9").error)

    @Test fun `titulo fuera de rango`() {
        assertEquals("Mínimo 5 caracteres", ValidadorUnidad.titulo("Sum").error)
        assertEquals("Máximo 80 caracteres", ValidadorUnidad.titulo("x".repeat(81)).error)
        assertNull(ValidadorUnidad.titulo("Sumo y resto hasta el 100").error)
    }
    @Test fun `origen del material obligatorio`() = assertTrue(ValidadorUnidad.origen(" ") is Resultado.Invalido)

    // ---------- Asignación ----------
    @Test fun `curso con formato correcto`() {
        assertEquals(Resultado.Valido, ValidadorAsignacion.curso("2°A"))
        assertEquals("Formato de curso: 2°A", ValidadorAsignacion.curso("3°A").error)
    }
    @Test fun `rango de fechas invertido`() = assertEquals(
        "El término debe ser igual o posterior al inicio",
        ValidadorAsignacion.rangoFechas(hoy.plusDays(5), hoy.plusDays(1), hoy).error,
    )
    @Test fun `fecha de termino pasada`() = assertEquals(
        "La fecha de término ya pasó",
        ValidadorAsignacion.rangoFechas(hoy.minusDays(10), hoy.minusDays(1), hoy).error,
    )
    @Test fun `rango demasiado largo`() = assertTrue(
        ValidadorAsignacion.rangoFechas(hoy, hoy.plusDays(91), hoy) is Resultado.Invalido,
    )
    @Test fun `rango valido`() = assertEquals(Resultado.Valido, ValidadorAsignacion.rangoFechas(hoy, hoy.plusDays(14), hoy))

    @Test fun `intentos entre 1 y 5`() {
        assertEquals(Resultado.Valido, ValidadorAsignacion.maxIntentos("3"))
        assertEquals("Entre 1 y 5 intentos", ValidadorAsignacion.maxIntentos("0").error)
        assertEquals("Entre 1 y 5 intentos", ValidadorAsignacion.maxIntentos("6").error)
        assertEquals("Debe ser un número", ValidadorAsignacion.maxIntentos("tres").error)
    }
    @Test fun `asignacion duplicada`() = assertTrue(ValidadorAsignacion.noDuplicada(1) is Resultado.Invalido)

    // ---------- Observación ----------
    @Test fun `observacion corta`() = assertEquals("Mínimo 10 caracteres", ValidadorObservacion.texto("Bien").error)
    @Test fun `observacion requiere confirmacion`() =
        assertTrue(ValidadorObservacion.validacionConfirmada(false) is Resultado.Invalido)
}
