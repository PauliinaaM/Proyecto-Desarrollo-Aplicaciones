package cl.duoc.mpazpro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.mpazpro.domain.model.Dificultad
import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.model.TipoDesafio

/*
 * Todas las entidades guardan datos 100 % FICTICIOS (restricción 5.1 del caso).
 * Fechas de calendario: epochDay (Long). Marcas de tiempo: epoch millis (Long),
 * mostradas como "YYYY-MM-DD HH:MM" en la UI.
 */

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: Long,
    val nombreFicticio: String,
    val rol: Rol,
    val curso: String? = null,          // solo estudiantes y docentes, ej. "2°A"
    val docenteId: Long? = null,        // docente asignado (estudiantes)
    val pin: String,                    // PIN de prueba de 4 dígitos (no es credencial real)
    val avatar: String = "🙂",
)

@Entity(tableName = "asignaturas")
data class AsignaturaEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    val nombreCorto: String,
    val colorHex: Long,
    val icono: String,                  // emoji como apoyo visual grande
)

@Entity(
    tableName = "unidades",
    foreignKeys = [ForeignKey(
        entity = AsignaturaEntity::class, parentColumns = ["id"], childColumns = ["asignaturaId"],
    )],
    indices = [Index("asignaturaId")],
)
data class UnidadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val asignaturaId: Long,
    val titulo: String,
    val textoReferencia: String,
    val estado: EstadoContenido = EstadoContenido.BORRADOR,
    val utpResponsableId: Long,
    val creadaEn: Long,
    val modificadaEn: Long,
)

@Entity(
    tableName = "objetivos",
    foreignKeys = [ForeignKey(
        entity = UnidadEntity::class, parentColumns = ["id"], childColumns = ["unidadId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("unidadId")],
)
data class ObjetivoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unidadId: Long,
    val codigo: String,                 // ej. "MA02 OA 09"
    val descripcion: String,
    val eje: String,
)

@Entity(
    tableName = "contenidos",
    foreignKeys = [ForeignKey(
        entity = ObjetivoEntity::class, parentColumns = ["id"], childColumns = ["oaId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("oaId")],
)
data class ContenidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oaId: Long,
    val titulo: String,
    val explicacion: String,
    val instruccionAudio: String,       // texto que se lee en voz alta (TTS)
    val ejemploGuiado: String,
    val apoyoVisual: String,            // emoji / ilustración simple
    val orden: Int,
    val origen: String,                 // "Propio", "Dominio público", "Mineduc (licencia educativa)"
)

@Entity(
    tableName = "preguntas",
    foreignKeys = [ForeignKey(
        entity = ObjetivoEntity::class, parentColumns = ["id"], childColumns = ["oaId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("oaId")],
)
data class PreguntaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oaId: Long,
    val esPruebaFinal: Boolean,         // false = actividad/desafío de práctica
    val tipo: TipoDesafio,
    val enunciado: String,
    val apoyoVisual: String = "",
    val alternativas: List<String>,
    val indiceCorrecto: Int,
    val explicacion: String,
    val dificultad: Dificultad,
    val orden: Int,
    val puntaje: Int = 1,
)

@Entity(
    tableName = "asignaciones",
    foreignKeys = [ForeignKey(
        entity = ObjetivoEntity::class, parentColumns = ["id"], childColumns = ["oaId"],
    )],
    indices = [Index("oaId"), Index("curso")],
)
data class AsignacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oaId: Long,
    val docenteId: Long,
    val curso: String,
    val desdeEpochDay: Long,
    val hastaEpochDay: Long,
    val maxIntentos: Int,
    val creadaEn: Long,
)

@Entity(
    tableName = "intentos",
    indices = [Index("estudianteId"), Index("asignacionId")],
)
data class IntentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val estudianteId: Long,
    val asignacionId: Long,
    val oaId: Long,
    val numero: Int,
    val puntaje: Int,
    val puntajeMaximo: Int,
    val rendidoEn: Long,
)

@Entity(tableName = "respuestas", indices = [Index("intentoId")])
data class RespuestaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val intentoId: Long,
    val preguntaId: Long,
    val indiceSeleccionado: Int,
    val correcta: Boolean,
)

/** Progreso de práctica: se guarda en tiempo real al responder cada actividad. */
@Entity(tableName = "progreso", primaryKeys = ["estudianteId", "preguntaId"])
data class ProgresoEntity(
    val estudianteId: Long,
    val preguntaId: Long,
    val oaId: Long,
    val correcta: Boolean,
    val completadoEn: Long,
)

/** Contenidos (explicaciones) vistos por el estudiante. */
@Entity(tableName = "contenidos_vistos", primaryKeys = ["estudianteId", "contenidoId"])
data class ContenidoVistoEntity(
    val estudianteId: Long,
    val contenidoId: Long,
    val oaId: Long,
    val vistoEn: Long,
)

@Entity(tableName = "observaciones", indices = [Index("estudianteId")])
data class ObservacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val estudianteId: Long,
    val oaId: Long,
    val docenteId: Long,
    val texto: String,
    val validado: Boolean,
    val creadaEn: Long,
)

/** Trazabilidad para UTP: quién cambió qué y cuándo. */
@Entity(tableName = "historial")
data class HistorialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entidad: String,
    val entidadId: Long,
    val accion: String,
    val usuarioId: Long,
    val fecha: Long,
)
