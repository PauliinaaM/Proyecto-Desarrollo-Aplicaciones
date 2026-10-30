package cl.duoc.mpazpro.data.local.entity

import androidx.room.Embedded
import cl.duoc.mpazpro.domain.model.EstadoContenido

/** Resultados de consultas con JOIN (no son tablas). */

data class UnidadResumen(
    @Embedded val unidad: UnidadEntity,
    val asignaturaNombre: String,
    val asignaturaIcono: String,
    val asignaturaColor: Long,
    val cantidadOa: Int,
)

data class OaDetalle(
    val oaId: Long,
    val codigo: String,
    val descripcion: String,
    val eje: String,
    val unidadId: Long,
    val unidadTitulo: String,
    val unidadEstado: EstadoContenido,
    val asignaturaId: Long,
    val asignaturaNombre: String,
    val asignaturaIcono: String,
    val asignaturaColor: Long,
)

data class AsignacionDetalle(
    @Embedded val asignacion: AsignacionEntity,
    val oaCodigo: String,
    val oaDescripcion: String,
    val asignaturaNombre: String,
    val asignaturaIcono: String,
    val asignaturaColor: Long,
)
