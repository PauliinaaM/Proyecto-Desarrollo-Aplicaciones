package cl.duoc.mpazpro.platform

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import cl.duoc.mpazpro.data.local.entity.ObservacionEntity
import cl.duoc.mpazpro.data.repository.InformeEstudiante
import cl.duoc.mpazpro.domain.rules.comoMarcaDeTiempo
import java.io.File

/**
 * RECURSO NATIVO 5 — Almacenamiento y compartición de archivos.
 *  - Genera el informe en PDF con la API nativa PdfDocument (sin librerías externas).
 *  - "Guardar": el docente elige dónde dejarlo con el selector del sistema (SAF),
 *    sin pedir permisos amplios de almacenamiento.
 *  - "Compartir": usa FileProvider, que entrega un URI temporal en vez de exponer
 *    rutas internas (integración segura).
 */
class ExportadorInforme(private val context: Context) {

    fun generarPdf(informe: InformeEstudiante, observaciones: List<ObservacionEntity>): File {
        val carpeta = File(context.cacheDir, "informes").apply { mkdirs() }
        val archivo = File(carpeta, "informe_${informe.estudiante.id}_${System.currentTimeMillis()}.pdf")

        val doc = PdfDocument()
        val ancho = 595; val alto = 842; val margen = 40f // A4 en puntos
        val titulo = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(31, 78, 121) }
        val subtitulo = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
        val texto = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val ok = Paint(texto).apply { color = Color.rgb(43, 138, 62) }
        val mal = Paint(texto).apply { color = Color.rgb(201, 42, 42) }

        var numPagina = 1
        var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numPagina).create())
        var y = margen + 10

        fun nuevaLineaSiFalta(espacio: Float) {
            if (y + espacio > alto - margen) {
                doc.finishPage(pagina)
                numPagina++
                pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numPagina).create())
                y = margen + 10
            }
        }
        fun linea(t: String, p: Paint = texto, salto: Float = 16f) {
            // Corte simple por ancho para que el texto no se salga de la hoja
            val palabras = t.split(" ")
            var actual = ""
            for (w in palabras) {
                val prueba = if (actual.isEmpty()) w else "$actual $w"
                if (p.measureText(prueba) > ancho - 2 * margen) {
                    nuevaLineaSiFalta(salto); pagina.canvas.drawText(actual, margen, y, p); y += salto
                    actual = w
                } else actual = prueba
            }
            nuevaLineaSiFalta(salto); pagina.canvas.drawText(actual, margen, y, p); y += salto
        }

        linea("MPAZ PRO 2.º — Informe pedagógico (reservado docente)", titulo, 26f)
        linea("Datos FICTICIOS de prueba · MVP académico DSY1105")
        linea("Estudiante: ${informe.estudiante.nombreFicticio}   Curso: ${informe.estudiante.curso ?: "—"}")
        linea("Emitido: ${informe.emitidoEn.comoMarcaDeTiempo()}")
        linea("Puntaje global: ${informe.porcentajeGlobal?.let { "$it %" } ?: "sin pruebas rendidas"}", subtitulo, 22f)

        linea("Diagnóstico por reglas (no automático por IA)", subtitulo, 20f)
        linea("Fortalezas: ${informe.fortalezas.ifEmpty { listOf("—") }.joinToString()}")
        linea("Áreas de refuerzo: ${informe.refuerzos.ifEmpty { listOf("—") }.joinToString()}")
        y += 6

        for (r in informe.resultados) {
            linea("${r.oa.asignaturaNombre} · ${r.oa.codigo}", subtitulo, 18f)
            linea(r.oa.descripcion)
            linea("Mejor resultado: ${r.porcentaje?.let { "$it %" } ?: "—"} (${r.nivel.etiqueta}) · Intentos: ${r.intentosUsados}")
            linea("Práctica: ${r.practicaCorrectas}/${r.actividadesPractica} actividades correctas")
            r.desgloseUltimo.forEach { d ->
                linea("${if (d.esCorrecta) "✔" else "✘"} ${d.enunciado} → ${d.seleccionada}" +
                    if (d.esCorrecta) "" else " (correcta: ${d.correcta})", if (d.esCorrecta) ok else mal)
            }
            y += 6
        }

        linea("Observaciones del docente", subtitulo, 20f)
        if (observaciones.isEmpty()) linea("Sin observaciones registradas.")
        observaciones.forEach { linea("• ${it.creadaEn.comoMarcaDeTiempo()} — ${it.texto}") }

        doc.finishPage(pagina)
        archivo.outputStream().use { doc.writeTo(it) }
        doc.close()
        return archivo
    }

    /** Intent para compartir con FileProvider (permiso de lectura temporal). */
    fun intentCompartir(archivo: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Informe MPAZ PRO 2.º (datos ficticios)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(envio, "Compartir informe")
    }

    /** Copia el PDF al destino que eligió el docente con el selector del sistema. */
    fun guardarEn(archivo: File, destino: Uri) {
        context.contentResolver.openOutputStream(destino)?.use { out ->
            archivo.inputStream().use { it.copyTo(out) }
        } ?: error("No se pudo abrir el destino")
    }
}
