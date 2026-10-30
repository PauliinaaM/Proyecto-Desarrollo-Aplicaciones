package cl.duoc.mpazpro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cl.duoc.mpazpro.data.local.dao.AsignacionDao
import cl.duoc.mpazpro.data.local.dao.CurriculoDao
import cl.duoc.mpazpro.data.local.dao.EvaluacionDao
import cl.duoc.mpazpro.data.local.dao.HistorialDao
import cl.duoc.mpazpro.data.local.dao.ObservacionDao
import cl.duoc.mpazpro.data.local.dao.UsuarioDao
import cl.duoc.mpazpro.data.local.entity.AsignacionEntity
import cl.duoc.mpazpro.data.local.entity.AsignaturaEntity
import cl.duoc.mpazpro.data.local.entity.ContenidoEntity
import cl.duoc.mpazpro.data.local.entity.ContenidoVistoEntity
import cl.duoc.mpazpro.data.local.entity.HistorialEntity
import cl.duoc.mpazpro.data.local.entity.IntentoEntity
import cl.duoc.mpazpro.data.local.entity.ObjetivoEntity
import cl.duoc.mpazpro.data.local.entity.ObservacionEntity
import cl.duoc.mpazpro.data.local.entity.PreguntaEntity
import cl.duoc.mpazpro.data.local.entity.ProgresoEntity
import cl.duoc.mpazpro.data.local.entity.RespuestaEntity
import cl.duoc.mpazpro.data.local.entity.UnidadEntity
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class, AsignaturaEntity::class, UnidadEntity::class, ObjetivoEntity::class,
        ContenidoEntity::class, PreguntaEntity::class, AsignacionEntity::class, IntentoEntity::class,
        RespuestaEntity::class, ProgresoEntity::class, ContenidoVistoEntity::class,
        ObservacionEntity::class, HistorialEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MPazDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun curriculoDao(): CurriculoDao
    abstract fun asignacionDao(): AsignacionDao
    abstract fun evaluacionDao(): EvaluacionDao
    abstract fun observacionDao(): ObservacionDao
    abstract fun historialDao(): HistorialDao

    companion object {
        fun crear(context: Context): MPazDatabase =
            Room.databaseBuilder(context, MPazDatabase::class.java, "mpaz_pro.db")
                // MVP académico: si cambia el esquema se recrea con la semilla ficticia.
                .fallbackToDestructiveMigration()
                .build()
    }
}
