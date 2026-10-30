package cl.duoc.mpazpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import cl.duoc.mpazpro.data.local.entity.AsignacionDetalle
import cl.duoc.mpazpro.data.local.entity.AsignacionEntity
import cl.duoc.mpazpro.data.local.entity.AsignaturaEntity
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
import cl.duoc.mpazpro.data.local.entity.UnidadResumen
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.domain.model.Rol
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios ORDER BY rol, nombreFicticio")
    fun observarTodos(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun porId(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE rol = :rol AND curso = :curso ORDER BY nombreFicticio")
    fun observarPorRolYCurso(rol: Rol, curso: String): Flow<List<UsuarioEntity>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuarios: List<UsuarioEntity>)
}

private const val SELECT_OA_DETALLE = """
    SELECT o.id AS oaId, o.codigo, o.descripcion, o.eje,
           u.id AS unidadId, u.titulo AS unidadTitulo, u.estado AS unidadEstado,
           a.id AS asignaturaId, a.nombre AS asignaturaNombre, a.icono AS asignaturaIcono,
           a.colorHex AS asignaturaColor
    FROM objetivos o
    JOIN unidades u ON u.id = o.unidadId
    JOIN asignaturas a ON a.id = u.asignaturaId
"""

@Dao
interface CurriculoDao {
    @Query("SELECT * FROM asignaturas ORDER BY id")
    fun observarAsignaturas(): Flow<List<AsignaturaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAsignaturas(lista: List<AsignaturaEntity>)

    @Query("""
        SELECT u.*, a.nombre AS asignaturaNombre, a.icono AS asignaturaIcono,
               a.colorHex AS asignaturaColor,
               (SELECT COUNT(*) FROM objetivos o WHERE o.unidadId = u.id) AS cantidadOa
        FROM unidades u JOIN asignaturas a ON a.id = u.asignaturaId
        ORDER BY a.id, u.titulo
    """)
    fun observarUnidades(): Flow<List<UnidadResumen>>

    @Query("SELECT * FROM unidades WHERE id = :id")
    suspend fun unidadPorId(id: Long): UnidadEntity?

    @Insert suspend fun insertarUnidad(u: UnidadEntity): Long
    @Update suspend fun actualizarUnidad(u: UnidadEntity)

    @Query("SELECT * FROM objetivos WHERE unidadId = :unidadId ORDER BY codigo")
    suspend fun objetivosDeUnidad(unidadId: Long): List<ObjetivoEntity>

    @Insert suspend fun insertarObjetivo(o: ObjetivoEntity): Long
    @Update suspend fun actualizarObjetivo(o: ObjetivoEntity)

    @Query("$SELECT_OA_DETALLE WHERE o.id = :oaId")
    suspend fun oaDetalle(oaId: Long): OaDetalle?

    /** Solo OA cuya unidad fue autorizada o publicada por UTP (flujo UTP → Docente). */
    @Query("$SELECT_OA_DETALLE WHERE u.estado IN ('AUTORIZADO','PUBLICADO') ORDER BY a.id, o.codigo")
    fun observarOaVisiblesDocente(): Flow<List<OaDetalle>>

    @Query("$SELECT_OA_DETALLE ORDER BY a.id, o.codigo")
    fun observarTodosLosOa(): Flow<List<OaDetalle>>

    @Query("SELECT * FROM contenidos WHERE oaId = :oaId ORDER BY orden")
    suspend fun contenidosDeOa(oaId: Long): List<ContenidoEntity>

    @Insert suspend fun insertarContenidos(lista: List<ContenidoEntity>)

    @Query("SELECT * FROM preguntas WHERE oaId = :oaId AND esPruebaFinal = :esFinal ORDER BY orden")
    suspend fun preguntasDeOa(oaId: Long, esFinal: Boolean): List<PreguntaEntity>

    @Query("SELECT * FROM preguntas WHERE id IN (:ids)")
    suspend fun preguntasPorIds(ids: List<Long>): List<PreguntaEntity>

    @Insert suspend fun insertarPreguntas(lista: List<PreguntaEntity>): List<Long>
}

private const val SELECT_ASIGNACION_DETALLE = """
    SELECT s.*, o.codigo AS oaCodigo, o.descripcion AS oaDescripcion,
           a.nombre AS asignaturaNombre, a.icono AS asignaturaIcono, a.colorHex AS asignaturaColor
    FROM asignaciones s
    JOIN objetivos o ON o.id = s.oaId
    JOIN unidades u ON u.id = o.unidadId
    JOIN asignaturas a ON a.id = u.asignaturaId
"""

@Dao
interface AsignacionDao {
    @Insert suspend fun insertar(a: AsignacionEntity): Long

    @Query("SELECT * FROM asignaciones WHERE id = :id")
    suspend fun porId(id: Long): AsignacionEntity?

    @Query("$SELECT_ASIGNACION_DETALLE WHERE s.docenteId = :docenteId ORDER BY s.creadaEn DESC")
    fun observarDeDocente(docenteId: Long): Flow<List<AsignacionDetalle>>

    /**
     * Lo que ve el estudiante: asignaciones de su curso, vigentes hoy y cuya unidad
     * siga habilitada (si UTP la deshabilita, desaparece para el estudiante).
     */
    @Query("""
        $SELECT_ASIGNACION_DETALLE
        WHERE s.curso = :curso AND :hoy BETWEEN s.desdeEpochDay AND s.hastaEpochDay
          AND u.estado IN ('AUTORIZADO','PUBLICADO')
        ORDER BY a.id
    """)
    fun observarVigentesDeCurso(curso: String, hoy: Long): Flow<List<AsignacionDetalle>>

    @Query("SELECT COUNT(*) FROM asignaciones WHERE curso = :curso AND oaId = :oaId AND hastaEpochDay >= :hoy")
    suspend fun contarVigentesMismoOa(curso: String, oaId: Long, hoy: Long): Int
}

@Dao
interface EvaluacionDao {
    @Query("SELECT COUNT(*) FROM intentos WHERE estudianteId = :estudianteId AND asignacionId = :asignacionId")
    suspend fun intentosUsados(estudianteId: Long, asignacionId: Long): Int

    @Query("SELECT COUNT(*) FROM intentos WHERE estudianteId = :estudianteId AND asignacionId = :asignacionId")
    fun observarIntentosUsados(estudianteId: Long, asignacionId: Long): Flow<Int>

    @Insert suspend fun insertarIntento(i: IntentoEntity): Long
    @Insert suspend fun insertarRespuestas(r: List<RespuestaEntity>)

    /** Guarda intento + respuestas de forma atómica. */
    @Transaction
    suspend fun registrarIntento(intento: IntentoEntity, respuestas: List<RespuestaEntity>): Long {
        val id = insertarIntento(intento)
        insertarRespuestas(respuestas.map { it.copy(intentoId = id) })
        return id
    }

    @Query("SELECT * FROM intentos WHERE estudianteId = :estudianteId ORDER BY rendidoEn DESC")
    fun observarIntentosDe(estudianteId: Long): Flow<List<IntentoEntity>>

    @Query("SELECT * FROM intentos WHERE estudianteId = :estudianteId ORDER BY rendidoEn DESC")
    suspend fun intentosDe(estudianteId: Long): List<IntentoEntity>

    @Query("SELECT * FROM intentos WHERE id = :id")
    suspend fun intentoPorId(id: Long): IntentoEntity?

    @Query("SELECT * FROM respuestas WHERE intentoId = :intentoId")
    suspend fun respuestasDe(intentoId: Long): List<RespuestaEntity>

    @Upsert suspend fun guardarProgreso(p: ProgresoEntity)
    @Upsert suspend fun marcarContenidoVisto(c: ContenidoVistoEntity)

    @Query("SELECT * FROM progreso WHERE estudianteId = :estudianteId AND oaId = :oaId")
    suspend fun progresoEnOa(estudianteId: Long, oaId: Long): List<ProgresoEntity>

    @Query("SELECT * FROM progreso WHERE estudianteId = :estudianteId")
    fun observarProgreso(estudianteId: Long): Flow<List<ProgresoEntity>>

    @Query("SELECT * FROM contenidos_vistos WHERE estudianteId = :estudianteId")
    fun observarContenidosVistos(estudianteId: Long): Flow<List<ContenidoVistoEntity>>

    @Insert suspend fun insertarProgresoSemilla(p: List<ProgresoEntity>)
}

@Dao
interface ObservacionDao {
    @Insert suspend fun insertar(o: ObservacionEntity): Long

    @Query("SELECT * FROM observaciones WHERE estudianteId = :estudianteId ORDER BY creadaEn DESC")
    fun observarDe(estudianteId: Long): Flow<List<ObservacionEntity>>
}

@Dao
interface HistorialDao {
    @Insert suspend fun registrar(h: HistorialEntity)

    @Query("SELECT * FROM historial ORDER BY fecha DESC LIMIT 200")
    fun observar(): Flow<List<HistorialEntity>>
}
