package cl.duoc.mpazpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.duoc.mpazpro.ui.navigation.MPazNavGraph
import cl.duoc.mpazpro.ui.theme.MPazTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val contenedor = (application as MPazApp).contenedor
        setContent {
            MPazTema {
                Surface(Modifier.fillMaxSize()) {
                    MPazNavGraph(sesion = contenedor.sesion)
                }
            }
        }
    }

    override fun onDestroy() {
        // Libera el TTS si la actividad se cierra definitivamente
        if (isFinishing) (application as MPazApp).contenedor.lectorVoz.detener()
        super.onDestroy()
    }
}
