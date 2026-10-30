package cl.duoc.mpazpro.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.duoc.mpazpro.R

/**
 * RECURSO NATIVO 4 — Notificaciones del sistema.
 * En Android 13+ requiere el permiso en tiempo de ejecución POST_NOTIFICATIONS;
 * si el usuario lo niega, la app sigue funcionando y solo omite el aviso.
 */
class Notificador(private val context: Context) {

    companion object { const val CANAL = "avisos_actividades" }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL, context.getString(R.string.canal_avisos_nombre), NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.canal_avisos_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun tienePermiso(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Devuelve false si no se pudo notificar (permiso denegado). El permiso se verifica en tienePermiso(). */
    @SuppressLint("MissingPermission")
    fun avisar(id: Int, titulo: String, texto: String): Boolean {
        if (!tienePermiso()) return false
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(id, n)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
