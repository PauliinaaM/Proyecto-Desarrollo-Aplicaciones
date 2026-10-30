package cl.duoc.mpazpro.domain

import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.model.NivelLogro
import cl.duoc.mpazpro.domain.rules.Calificador
import cl.duoc.mpazpro.domain.rules.Habilitacion
import cl.duoc.mpazpro.domain.rules.PoliticaIntentos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReglasEvaluacionTest {

    private val hoy = LocalDate.of(2026, 10, 24)
    private val desde = hoy.minusDays(3)
    private val hasta = hoy.plusDays(3)

    @Test fun `primer intento permitido`() {
        val r = PoliticaIntentos.verificar(usados = 0, maximo = 2, hoy, desde, hasta)
        assertEquals(Habilitacion.Permitida(intentoNumero = 1, restantes = 1), r)
    }

    @Test fun `bloquea al agotar intentos`() {
        val r = PoliticaIntentos.verificar(usados = 2, maximo = 2, hoy, desde, hasta)
        assertEquals(Habilitacion.Bloqueada("Ya usaste tus 2 intentos"), r)
    }

    @Test fun `bloquea antes de la fecha de inicio`() =
        assertTrue(PoliticaIntentos.verificar(0, 2, hoy, hoy.plusDays(1), hasta) is Habilitacion.Bloqueada)

    @Test fun `bloquea despues del cierre`() =
        assertTrue(PoliticaIntentos.verificar(0, 2, hoy, desde, hoy.minusDays(1)) is Habilitacion.Bloqueada)

    @Test fun `porcentaje redondeado`() {
        assertEquals(67, Calificador.porcentaje(2, 3))
        assertEquals(0, Calificador.porcentaje(0, 0))
    }

    @Test fun `niveles de logro por umbral`() {
        assertEquals(NivelLogro.FORTALEZA, Calificador.nivel(80))
        assertEquals(NivelLogro.EN_DESARROLLO, Calificador.nivel(60))
        assertEquals(NivelLogro.REFUERZO, Calificador.nivel(59))
        assertEquals(NivelLogro.SIN_DATOS, Calificador.nivel(null))
    }

    @Test fun `flujo de estados de UTP`() {
        assertEquals(listOf(EstadoContenido.EN_REVISION), EstadoContenido.BORRADOR.siguientesPermitidos())
        assertFalse(EstadoContenido.BORRADOR.visibleParaDocente)
        assertTrue(EstadoContenido.AUTORIZADO.visibleParaDocente)
        assertTrue(EstadoContenido.PUBLICADO.visibleParaDocente)
        assertFalse(EstadoContenido.DESHABILITADO.visibleParaDocente)
    }
}
