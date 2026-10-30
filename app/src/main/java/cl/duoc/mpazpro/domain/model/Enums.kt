package cl.duoc.mpazpro.domain.model

/** Perfiles del caso. Cada rol ve un grafo de navegación distinto. */
enum class Rol(val etiqueta: String) {
    UTP("Jefatura UTP"),
    DOCENTE("Docente"),
    ESTUDIANTE("Estudiante"),
}

/** Ciclo de vida de una unidad u OA, tal como lo define el caso (3.2). */
enum class EstadoContenido(val etiqueta: String) {
    BORRADOR("Borrador"),
    EN_REVISION("En revisión"),
    AUTORIZADO("Autorizado"),
    PUBLICADO("Publicado"),
    DESHABILITADO("Deshabilitado");

    /** Estados a los que UTP puede mover el contenido desde el estado actual. */
    fun siguientesPermitidos(): List<EstadoContenido> = when (this) {
        BORRADOR -> listOf(EN_REVISION)
        EN_REVISION -> listOf(AUTORIZADO, BORRADOR)
        AUTORIZADO -> listOf(PUBLICADO, DESHABILITADO)
        PUBLICADO -> listOf(DESHABILITADO)
        DESHABILITADO -> listOf(BORRADOR)
    }

    /** El docente solo puede asignar contenido autorizado o publicado por UTP. */
    val visibleParaDocente: Boolean get() = this == AUTORIZADO || this == PUBLICADO
}

enum class TipoDesafio(val etiqueta: String) {
    SELECCION("Selección"),
    COMPLETACION("Completación"),
    ASOCIACION("Asociación"),
    SECUENCIA("Secuencia"),
}

enum class Dificultad(val etiqueta: String) { BASICA("Básica"), MEDIA("Media"), DESAFIO("Desafío") }

/** Diagnóstico por reglas fijas (el caso prohíbe diagnósticos automáticos por IA). */
enum class NivelLogro(val etiqueta: String) {
    FORTALEZA("Fortaleza"),
    EN_DESARROLLO("En desarrollo"),
    REFUERZO("Área de refuerzo"),
    SIN_DATOS("Sin datos"),
}
