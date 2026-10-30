package cl.duoc.mpazpro.di

import android.content.Context
import cl.duoc.mpazpro.data.local.MPazDatabase
import cl.duoc.mpazpro.data.repository.AsignacionRepository
import cl.duoc.mpazpro.data.repository.AuthRepository
import cl.duoc.mpazpro.data.repository.CurriculoRepository
import cl.duoc.mpazpro.data.repository.EvaluacionRepository
import cl.duoc.mpazpro.data.repository.InformeRepository
import cl.duoc.mpazpro.data.repository.ObservacionRepository
import cl.duoc.mpazpro.data.repository.SesionRepository
import cl.duoc.mpazpro.data.seed.SemillaFicticia
import cl.duoc.mpazpro.platform.ExportadorInforme
import cl.duoc.mpazpro.platform.LectorVoz
import cl.duoc.mpazpro.platform.Notificador
import cl.duoc.mpazpro.platform.RetroalimentacionSensorial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias manual (sin Hilt, para mantener el MVP simple).
 * Una única instancia vive en MPazApp y los ViewModels la reciben por su factory.
 */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: MPazDatabase = MPazDatabase.crear(app)
    val sesion = SesionRepository()

    val auth = AuthRepository(db, sesion)
    val curriculo = CurriculoRepository(db, sesion)
    val asignaciones = AsignacionRepository(db, sesion)
    val evaluacion = EvaluacionRepository(db, sesion)
    val informes = InformeRepository(db, sesion)
    val observaciones = ObservacionRepository(db, sesion)

    // Recursos nativos
    val lectorVoz by lazy { LectorVoz(app) }
    val sensorial by lazy { RetroalimentacionSensorial(app) }
    val notificador by lazy { Notificador(app) }
    val exportador by lazy { ExportadorInforme(app) }

    init {
        alcance.launch { SemillaFicticia.poblarSiVacia(db) }
    }
}
