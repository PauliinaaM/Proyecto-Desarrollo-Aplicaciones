package cl.duoc.mpazpro.data.repository

import cl.duoc.mpazpro.data.local.MPazDatabase
import cl.duoc.mpazpro.data.local.entity.IntentoEntity
import cl.duoc.mpazpro.data.local.entity.OaDetalle
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.domain.model.NivelLogro
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.rules.Calificador
import kotlinx.coroutines.flow.first

data class DetalleRespuesta(
    val enunciado: String,
    val seleccionada: String,
    val correcta: String,
    val esCorrecta: Boolean,
)

data class ResultadoOa(
    val oa: OaDetalle,
    val intentosUsados: Int,
    val mejorIntento: IntentoEntity?,
    val porcentaje: Int?,
    val nivel: NivelLogro,
    val desgloseUltimo: List<DetalleRespuesta>,
    val actividadesPractica: Int,
    val practicaCorrectas: Int,
)

data class InformeEstudiante(
    val estudiante: UsuarioEntity,
    val resultados: List<ResultadoOa>,
    val porcentajeGlobal: Int?,
    val fortalezas: List<String>,
    val refuerzos: List<String>,
    val emitidoEn: Long,
)

data class ResumenOaCurso(val oa: OaDetalle, val rindieron: Int, val totalEstudiantes: Int, val promedio: Int?)

/**
 * Informes reservados al perfil docente. Cada método exige el rol DOCENTE,
 * de modo que aunque una pantalla de estudiante intentara llamarlo, falla.
 */
class InformeRepository(private val db: MPazDatabase, private val sesion: SesionRepository) {

    suspend fun informeDe(estudianteId: Long): InformeEstudiante {
        sesion.exigirRol(Rol.DOCENTE)
        val estudiante = requireNotNull(db.usuarioDao().porId(estudianteId))
        val intentos = db.evaluacionDao().intentosDe(estudianteId)
        val curriculo = db.curriculoDao()

        // OA a reportar: los asignados al curso del estudiante + cualquiera con intentos
        val asignados = intentos.map { it.oaId }.toSet() +
            (estudiante.curso?.let { asignacionesOa(it) } ?: emptySet())

        val resultados = asignados.mapNotNull { oaId ->
            val oa = curriculo.oaDetalle(oaId) ?: return@mapNotNull null
            val delOa = intentos.filter { it.oaId == oaId }
            val mejor = delOa.maxByOrNull { Calificador.porcentaje(it.puntaje, it.puntajeMaximo) }
            val ultimo = delOa.maxByOrNull { it.rendidoEn }
            val pct = mejor?.let { Calificador.porcentaje(it.puntaje, it.puntajeMaximo) }
            val practica = db.evaluacionDao().progresoEnOa(estudianteId, oaId)
            ResultadoOa(
                oa = oa, intentosUsados = delOa.size, mejorIntento = mejor, porcentaje = pct,
                nivel = Calificador.nivel(pct), desgloseUltimo = ultimo?.let { desglose(it) }.orEmpty(),
                actividadesPractica = practica.size, practicaCorrectas = practica.count { it.correcta },
            )
        }.sortedBy { it.oa.asignaturaId }

        val conDatos = resultados.mapNotNull { it.mejorIntento }
        val global = if (conDatos.isEmpty()) null else
            Calificador.porcentaje(conDatos.sumOf { it.puntaje }, conDatos.sumOf { it.puntajeMaximo })

        return InformeEstudiante(
            estudiante = estudiante, resultados = resultados, porcentajeGlobal = global,
            fortalezas = resultados.filter { it.nivel == NivelLogro.FORTALEZA }.map { "${it.oa.asignaturaNombre} · ${it.oa.codigo}" },
            refuerzos = resultados.filter { it.nivel == NivelLogro.REFUERZO }.map { "${it.oa.asignaturaNombre} · ${it.oa.codigo}" },
            emitidoEn = System.currentTimeMillis(),
        )
    }

    /** Rendimiento agrupado por OA para el curso del docente. */
    suspend fun resumenCurso(curso: String, estudiantes: List<UsuarioEntity>): List<ResumenOaCurso> {
        sesion.exigirRol(Rol.DOCENTE)
        val ids = estudiantes.map { it.id }.toSet()
        val intentos = ids.flatMap { db.evaluacionDao().intentosDe(it) }
        return asignacionesOa(curso).mapNotNull { oaId ->
            val oa = db.curriculoDao().oaDetalle(oaId) ?: return@mapNotNull null
            val mejoresPorEstudiante = intentos.filter { it.oaId == oaId }
                .groupBy { it.estudianteId }
                .mapValues { (_, l) -> l.maxOf { Calificador.porcentaje(it.puntaje, it.puntajeMaximo) } }
            ResumenOaCurso(
                oa = oa, rindieron = mejoresPorEstudiante.size, totalEstudiantes = ids.size,
                promedio = mejoresPorEstudiante.values.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            )
        }
    }

    private suspend fun asignacionesOa(curso: String): Set<Long> {
        val docente = sesion.usuario.value ?: return emptySet()
        // Se toma una sola lectura de las asignaciones del docente, filtrada por curso.
        return db.asignacionDao().observarDeDocente(docente.id).first()
            .filter { it.asignacion.curso == curso }
            .map { it.asignacion.oaId }
            .toSet()
    }

    private suspend fun desglose(intento: IntentoEntity): List<DetalleRespuesta> {
        val respuestas = db.evaluacionDao().respuestasDe(intento.id)
        val preguntas = db.curriculoDao().preguntasPorIds(respuestas.map { it.preguntaId }).associateBy { it.id }
        return respuestas.mapNotNull { r ->
            val p = preguntas[r.preguntaId] ?: return@mapNotNull null
            p.orden to DetalleRespuesta(
                enunciado = p.enunciado,
                seleccionada = p.alternativas.getOrElse(r.indiceSeleccionado) { "—" },
                correcta = p.alternativas[p.indiceCorrecto],
                esCorrecta = r.correcta,
            )
        }.sortedBy { it.first }.map { it.second }
    }
}
