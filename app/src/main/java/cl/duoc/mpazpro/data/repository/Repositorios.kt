package cl.duoc.mpazpro.data.repository

import cl.duoc.mpazpro.data.local.MPazDatabase
import cl.duoc.mpazpro.data.local.entity.AsignacionDetalle
import cl.duoc.mpazpro.data.local.entity.AsignacionEntity
import cl.duoc.mpazpro.data.local.entity.ContenidoEntity
import cl.duoc.mpazpro.data.local.entity.ContenidoVistoEntity
import cl.duoc.mpazpro.data.local.entity.HistorialEntity
import cl.duoc.mpazpro.data.local.entity.IntentoEntity
import cl.duoc.mpazpro.data.local.entity.ObjetivoEntity
import cl.duoc.mpazpro.data.local.entity.ObservacionEntity
import cl.duoc.mpazpro.data.local.entity.OaDetalle
import cl.duoc.mpazpro.data.local.entity.PreguntaEntity
import cl.duoc.mpazpro.data.local.entity.ProgresoEntity
import cl.duoc.mpazpro.data.local.entity.RespuestaEntity
import cl.duoc.mpazpro.data.local.entity.UnidadEntity
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.rules.Habilitacion
import cl.duoc.mpazpro.domain.rules.PoliticaIntentos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** Sesión en memoria. El rol de la sesión decide qué rutas y datos se pueden ver. */
class SesionRepository {
    private val _usuario = MutableStateFlow<UsuarioEntity?>(null)
    val usuario: StateFlow<UsuarioEntity?> = _usuario.asStateFlow()

    fun iniciar(u: UsuarioEntity) { _usuario.value = u }
    fun cerrar() { _usuario.value = null }

    /** Bloqueo de seguridad: lanza si el rol actual no está autorizado. */
    fun exigirRol(vararg roles: Rol): UsuarioEntity {
        val u = _usuario.value ?: throw SecurityException("Sesión no iniciada")
        if (u.rol !in roles) throw SecurityException("El rol ${u.rol.etiqueta} no tiene acceso a esta sección")
        return u
    }
}

sealed interface ResultadoLogin {
    data class Exito(val usuario: UsuarioEntity) : ResultadoLogin
    data object PinIncorrecto : ResultadoLogin
}

class AuthRepository(private val db: MPazDatabase, private val sesion: SesionRepository) {
    val perfiles: Flow<List<UsuarioEntity>> = db.usuarioDao().observarTodos()

    suspend fun ingresar(usuarioId: Long, pin: String): ResultadoLogin {
        val u = db.usuarioDao().porId(usuarioId) ?: return ResultadoLogin.PinIncorrecto
        return if (u.pin == pin) {
            sesion.iniciar(u); ResultadoLogin.Exito(u)
        } else ResultadoLogin.PinIncorrecto
    }

    suspend fun usuario(id: Long) = db.usuarioDao().porId(id)
    fun estudiantesDeCurso(curso: String) = db.usuarioDao().observarPorRolYCurso(Rol.ESTUDIANTE, curso)
}

class CurriculoRepository(private val db: MPazDatabase, private val sesion: SesionRepository) {
    private val dao = db.curriculoDao()

    val asignaturas = dao.observarAsignaturas()
    val unidades = dao.observarUnidades()
    val historial = db.historialDao().observar()
    val oaVisiblesDocente: Flow<List<OaDetalle>> = dao.observarOaVisiblesDocente()

    suspend fun unidad(id: Long) = dao.unidadPorId(id)
    suspend fun objetivosDeUnidad(id: Long) = dao.objetivosDeUnidad(id)
    suspend fun oa(id: Long) = dao.oaDetalle(id)
    suspend fun contenidos(oaId: Long): List<ContenidoEntity> = dao.contenidosDeOa(oaId)
    suspend fun practica(oaId: Long): List<PreguntaEntity> = dao.preguntasDeOa(oaId, esFinal = false)
    suspend fun pruebaFinal(oaId: Long): List<PreguntaEntity> = dao.preguntasDeOa(oaId, esFinal = true)
    suspend fun preguntas(ids: List<Long>) = dao.preguntasPorIds(ids)

    /** Crea o edita la unidad y su OA principal. Toda modificación queda en el historial. */
    suspend fun guardarUnidad(
        unidadId: Long?, asignaturaId: Long, titulo: String, textoReferencia: String,
        codigoOa: String, descripcionOa: String, eje: String, origen: String,
    ): Long {
        val utp = sesion.exigirRol(Rol.UTP)
        val ahora = System.currentTimeMillis()
        val id: Long
        if (unidadId == null) {
            id = dao.insertarUnidad(
                UnidadEntity(
                    asignaturaId = asignaturaId, titulo = titulo.trim(), textoReferencia = textoReferencia.trim(),
                    estado = EstadoContenido.BORRADOR, utpResponsableId = utp.id, creadaEn = ahora, modificadaEn = ahora,
                ),
            )
            dao.insertarObjetivo(ObjetivoEntity(unidadId = id, codigo = codigoOa.trim().uppercase(), descripcion = descripcionOa.trim(), eje = eje.trim()))
            registrar("Unidad", id, "Creada (origen: $origen)", utp.id)
        } else {
            val actual = requireNotNull(dao.unidadPorId(unidadId))
            dao.actualizarUnidad(
                actual.copy(
                    asignaturaId = asignaturaId, titulo = titulo.trim(), textoReferencia = textoReferencia.trim(),
                    modificadaEn = ahora,
                    // Editar algo ya aprobado lo devuelve a revisión: UTP debe volver a autorizar.
                    estado = if (actual.estado == EstadoContenido.BORRADOR) actual.estado else EstadoContenido.EN_REVISION,
                ),
            )
            val oa = dao.objetivosDeUnidad(unidadId).firstOrNull()
            if (oa == null) {
                dao.insertarObjetivo(ObjetivoEntity(unidadId = unidadId, codigo = codigoOa.trim().uppercase(), descripcion = descripcionOa.trim(), eje = eje.trim()))
            } else {
                dao.actualizarObjetivo(oa.copy(codigo = codigoOa.trim().uppercase(), descripcion = descripcionOa.trim(), eje = eje.trim()))
            }
            id = unidadId
            registrar("Unidad", id, "Editada (origen: $origen)", utp.id)
        }
        return id
    }

    suspend fun cambiarEstado(unidadId: Long, nuevo: EstadoContenido) {
        val utp = sesion.exigirRol(Rol.UTP)
        val u = requireNotNull(dao.unidadPorId(unidadId))
        require(nuevo in u.estado.siguientesPermitidos()) { "Transición no permitida" }
        dao.actualizarUnidad(u.copy(estado = nuevo, modificadaEn = System.currentTimeMillis()))
        registrar("Unidad", unidadId, "${u.estado.etiqueta} → ${nuevo.etiqueta}", utp.id)
    }

    private suspend fun registrar(entidad: String, id: Long, accion: String, usuarioId: Long) =
        db.historialDao().registrar(HistorialEntity(0, entidad, id, accion, usuarioId, System.currentTimeMillis()))
}

class AsignacionRepository(private val db: MPazDatabase, private val sesion: SesionRepository) {
    private val dao = db.asignacionDao()

    fun deDocente(docenteId: Long): Flow<List<AsignacionDetalle>> = dao.observarDeDocente(docenteId)
    fun vigentesDeCurso(curso: String): Flow<List<AsignacionDetalle>> =
        dao.observarVigentesDeCurso(curso, LocalDate.now().toEpochDay())

    suspend fun porId(id: Long) = dao.porId(id)
    suspend fun vigentesMismoOa(curso: String, oaId: Long) =
        dao.contarVigentesMismoOa(curso, oaId, LocalDate.now().toEpochDay())

    suspend fun crear(oaId: Long, curso: String, desde: LocalDate, hasta: LocalDate, maxIntentos: Int): Long {
        val docente = sesion.exigirRol(Rol.DOCENTE)
        val oa = requireNotNull(db.curriculoDao().oaDetalle(oaId))
        require(oa.unidadEstado.visibleParaDocente) { "La unidad no está autorizada por UTP" }
        val id = dao.insertar(
            AsignacionEntity(
                oaId = oaId, docenteId = docente.id, curso = curso, desdeEpochDay = desde.toEpochDay(),
                hastaEpochDay = hasta.toEpochDay(), maxIntentos = maxIntentos, creadaEn = System.currentTimeMillis(),
            ),
        )
        db.historialDao().registrar(
            HistorialEntity(0, "Asignación", id, "OA ${oa.codigo} asignado a $curso ($maxIntentos intentos)", docente.id, System.currentTimeMillis()),
        )
        return id
    }
}

class EvaluacionRepository(private val db: MPazDatabase, private val sesion: SesionRepository) {
    private val dao = db.evaluacionDao()

    fun progreso(estudianteId: Long) = dao.observarProgreso(estudianteId)
    fun contenidosVistos(estudianteId: Long) = dao.observarContenidosVistos(estudianteId)
    fun intentosObservados(estudianteId: Long) = dao.observarIntentosDe(estudianteId)
    fun intentosUsados(estudianteId: Long, asignacionId: Long) = dao.observarIntentosUsados(estudianteId, asignacionId)

    /** Guardado en tiempo real: se llama en cada respuesta de práctica. */
    suspend fun guardarRespuestaPractica(pregunta: PreguntaEntity, correcta: Boolean) {
        val est = sesion.exigirRol(Rol.ESTUDIANTE)
        dao.guardarProgreso(ProgresoEntity(est.id, pregunta.id, pregunta.oaId, correcta, System.currentTimeMillis()))
    }

    suspend fun marcarContenidoVisto(c: ContenidoEntity) {
        val est = sesion.exigirRol(Rol.ESTUDIANTE)
        dao.marcarContenidoVisto(ContenidoVistoEntity(est.id, c.id, c.oaId, System.currentTimeMillis()))
    }

    suspend fun verificarHabilitacion(asignacion: AsignacionEntity): Habilitacion {
        val est = sesion.exigirRol(Rol.ESTUDIANTE)
        return PoliticaIntentos.verificar(
            usados = dao.intentosUsados(est.id, asignacion.id), maximo = asignacion.maxIntentos,
            hoy = LocalDate.now(), desde = LocalDate.ofEpochDay(asignacion.desdeEpochDay),
            hasta = LocalDate.ofEpochDay(asignacion.hastaEpochDay),
        )
    }

    /**
     * Registra la prueba final. Vuelve a verificar los intentos justo antes de
     * guardar, para que el límite no se pueda saltar (ej. doble toque).
     */
    suspend fun rendirPrueba(asignacion: AsignacionEntity, respuestas: Map<PreguntaEntity, Int>): Long {
        val est = sesion.exigirRol(Rol.ESTUDIANTE)
        val hab = verificarHabilitacion(asignacion)
        check(hab is Habilitacion.Permitida) { (hab as Habilitacion.Bloqueada).motivo }
        val detalle = respuestas.map { (p, sel) ->
            RespuestaEntity(intentoId = 0, preguntaId = p.id, indiceSeleccionado = sel, correcta = sel == p.indiceCorrecto)
        }
        return dao.registrarIntento(
            IntentoEntity(
                estudianteId = est.id, asignacionId = asignacion.id, oaId = asignacion.oaId, numero = hab.intentoNumero,
                puntaje = respuestas.entries.filter { it.value == it.key.indiceCorrecto }.sumOf { it.key.puntaje },
                puntajeMaximo = respuestas.keys.sumOf { it.puntaje }, rendidoEn = System.currentTimeMillis(),
            ),
            detalle,
        )
    }

    suspend fun intento(id: Long) = dao.intentoPorId(id)
    suspend fun respuestasDe(intentoId: Long) = dao.respuestasDe(intentoId)
    suspend fun intentosDe(estudianteId: Long): List<IntentoEntity> = dao.intentosDe(estudianteId)
    suspend fun progresoEnOa(estudianteId: Long, oaId: Long) = dao.progresoEnOa(estudianteId, oaId)
}

class ObservacionRepository(private val db: MPazDatabase, private val sesion: SesionRepository) {
    fun de(estudianteId: Long): Flow<List<ObservacionEntity>> = db.observacionDao().observarDe(estudianteId)

    suspend fun guardar(estudianteId: Long, oaId: Long, texto: String) {
        val docente = sesion.exigirRol(Rol.DOCENTE)
        db.observacionDao().insertar(
            ObservacionEntity(
                estudianteId = estudianteId, oaId = oaId, docenteId = docente.id, texto = texto.trim(),
                validado = true, creadaEn = System.currentTimeMillis(),
            ),
        )
    }
}
