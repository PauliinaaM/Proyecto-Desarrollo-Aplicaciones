package cl.duoc.mpazpro.data.local

import androidx.room.TypeConverter
import cl.duoc.mpazpro.domain.model.Dificultad
import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.model.TipoDesafio

class Converters {
    // Separador poco común para no chocar con el texto de las alternativas
    private val sep = "␟"

    @TypeConverter fun listaATexto(v: List<String>): String = v.joinToString(sep)
    @TypeConverter fun textoALista(v: String): List<String> = if (v.isEmpty()) emptyList() else v.split(sep)

    @TypeConverter fun rolATexto(v: Rol): String = v.name
    @TypeConverter fun textoARol(v: String): Rol = Rol.valueOf(v)

    @TypeConverter fun estadoATexto(v: EstadoContenido): String = v.name
    @TypeConverter fun textoAEstado(v: String): EstadoContenido = EstadoContenido.valueOf(v)

    @TypeConverter fun tipoATexto(v: TipoDesafio): String = v.name
    @TypeConverter fun textoATipo(v: String): TipoDesafio = TipoDesafio.valueOf(v)

    @TypeConverter fun dificultadATexto(v: Dificultad): String = v.name
    @TypeConverter fun textoADificultad(v: String): Dificultad = Dificultad.valueOf(v)
}
