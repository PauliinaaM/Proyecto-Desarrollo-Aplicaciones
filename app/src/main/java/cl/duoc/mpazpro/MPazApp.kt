package cl.duoc.mpazpro

import android.app.Application
import cl.duoc.mpazpro.di.AppContainer

class MPazApp : Application() {
    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
    }
}
