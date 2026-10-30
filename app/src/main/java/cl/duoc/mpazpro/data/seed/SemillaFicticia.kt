package cl.duoc.mpazpro.data.seed

import androidx.room.withTransaction
import cl.duoc.mpazpro.data.local.MPazDatabase
import cl.duoc.mpazpro.data.local.entity.AsignacionEntity
import cl.duoc.mpazpro.data.local.entity.AsignaturaEntity
import cl.duoc.mpazpro.data.local.entity.ContenidoEntity
import cl.duoc.mpazpro.data.local.entity.HistorialEntity
import cl.duoc.mpazpro.data.local.entity.IntentoEntity
import cl.duoc.mpazpro.data.local.entity.ObjetivoEntity
import cl.duoc.mpazpro.data.local.entity.PreguntaEntity
import cl.duoc.mpazpro.data.local.entity.RespuestaEntity
import cl.duoc.mpazpro.data.local.entity.UnidadEntity
import cl.duoc.mpazpro.data.local.entity.UsuarioEntity
import cl.duoc.mpazpro.domain.model.Dificultad
import cl.duoc.mpazpro.domain.model.Dificultad.BASICA
import cl.duoc.mpazpro.domain.model.Dificultad.DESAFIO
import cl.duoc.mpazpro.domain.model.Dificultad.MEDIA
import cl.duoc.mpazpro.domain.model.EstadoContenido
import cl.duoc.mpazpro.domain.model.Rol
import cl.duoc.mpazpro.domain.model.TipoDesafio
import cl.duoc.mpazpro.domain.model.TipoDesafio.ASOCIACION
import cl.duoc.mpazpro.domain.model.TipoDesafio.COMPLETACION
import cl.duoc.mpazpro.domain.model.TipoDesafio.SECUENCIA
import cl.duoc.mpazpro.domain.model.TipoDesafio.SELECCION
import java.time.LocalDate

/**
 * Datos de prueba 100 % FICTICIOS (restricción 5.1 del caso).
 *  - Ningún nombre, RUT ni credencial corresponde a personas reales: los
 *    estudiantes usan apodos de animales y los PIN son solo de demostración.
 *  - Todos los textos, cuentos y preguntas son de autoría propia del equipo
 *    (restricción de derechos de autor). Los códigos de OA son referenciales y
 *    deben contrastarse con el material oficial que entregue el colegio.
 */
object SemillaFicticia {

    // ---- Usuarios de prueba (PIN documentados en el README) ----
    const val UTP_ID = 100L
    const val DOCENTE_A_ID = 200L
    const val DOCENTE_B_ID = 201L

    private val usuarios = listOf(
        UsuarioEntity(UTP_ID, "UTP Demo", Rol.UTP, pin = "1000", avatar = "🗂️"),
        UsuarioEntity(DOCENTE_A_ID, "Docente Demo 2°A", Rol.DOCENTE, curso = "2°A", pin = "2000", avatar = "🍎"),
        UsuarioEntity(DOCENTE_B_ID, "Docente Demo 2°B", Rol.DOCENTE, curso = "2°B", pin = "2001", avatar = "📐"),
        UsuarioEntity(301, "Zorro Azul", Rol.ESTUDIANTE, "2°A", DOCENTE_A_ID, "1234", "🦊"),
        UsuarioEntity(302, "Búho Verde", Rol.ESTUDIANTE, "2°A", DOCENTE_A_ID, "1234", "🦉"),
        UsuarioEntity(303, "Tortuga Roja", Rol.ESTUDIANTE, "2°A", DOCENTE_A_ID, "1234", "🐢"),
        UsuarioEntity(304, "Delfín Amarillo", Rol.ESTUDIANTE, "2°A", DOCENTE_A_ID, "1234", "🐬"),
        UsuarioEntity(311, "Pingüino Lila", Rol.ESTUDIANTE, "2°B", DOCENTE_B_ID, "1234", "🐧"),
        UsuarioEntity(312, "Huemul Café", Rol.ESTUDIANTE, "2°B", DOCENTE_B_ID, "1234", "🦌"),
    )

    private val asignaturas = listOf(
        AsignaturaEntity(1, "Lenguaje y Comunicación", "Lenguaje", 0xFFD9480F, "📖"),
        AsignaturaEntity(2, "Matemática", "Matemática", 0xFF1864AB, "🔢"),
        AsignaturaEntity(3, "Ciencias Naturales", "Ciencias", 0xFF2B8A3E, "🌱"),
        AsignaturaEntity(4, "Historia, Geografía y Ciencias Sociales", "Historia", 0xFF862E9C, "🗺️"),
    )

    // ---- Estructura auxiliar para declarar el contenido de forma legible ----
    private class C(val titulo: String, val explicacion: String, val audio: String, val ejemplo: String, val visual: String)
    private class P(
        val tipo: TipoDesafio, val enunciado: String, val alternativas: List<String>,
        val correcta: Int, val explicacion: String, val dificultad: Dificultad = BASICA, val visual: String = "",
    )
    private class OaSemilla(
        val codigo: String, val descripcion: String, val eje: String,
        val contenidos: List<C>, val practica: List<P>, val prueba: List<P>,
    )
    private class UnidadSemilla(
        val asignaturaId: Long, val titulo: String, val textoReferencia: String,
        val estado: EstadoContenido, val oas: List<OaSemilla>,
    )

    private val unidades = listOf(
        // -------------------------------- LENGUAJE --------------------------------
        UnidadSemilla(
            1, "Leo y comprendo cuentos breves",
            textoReferencia = "El paraguas de Benjamín\n\nBenjamín fue al parque con su abuela. " +
                "De pronto empezó a llover. La abuela abrió un paraguas amarillo y caminaron juntos " +
                "a casa. Al llegar, tomaron leche caliente mirando la lluvia por la ventana.",
            estado = EstadoContenido.PUBLICADO,
            oas = listOf(
                OaSemilla(
                    "LE02 OA 05", "Comprender cuentos breves: reconocer personajes, lugar y hechos principales.", "Lectura",
                    contenidos = listOf(
                        C(
                            "¿Qué es un cuento?",
                            "Un cuento es una historia corta. Tiene personajes, un lugar y algo que pasa.",
                            "Escucha con atención. Un cuento es una historia corta. Tiene personajes, un lugar y algo que pasa.",
                            "Lila buscó a su gato Tomás por toda la casa. Lo encontró dormido en una caja.\n" +
                                "• Personajes: Lila y Tomás.\n• Lugar: la casa.\n• ¿Qué pasó? Tomás se perdió y Lila lo encontró.",
                            "📚",
                        ),
                        C(
                            "Los personajes",
                            "Los personajes son quienes viven la historia. Pueden ser personas o animales.",
                            "Los personajes son quienes viven la historia. Pregúntate: ¿quién aparece en el cuento?",
                            "En el cuento de Lila, los personajes son Lila y su gato Tomás.",
                            "🐱",
                        ),
                        C(
                            "El lugar y los hechos",
                            "El lugar es dónde pasa el cuento. Los hechos son lo que ocurre, en orden.",
                            "Para encontrar el lugar, pregúntate: ¿dónde pasa? Para los hechos: ¿qué pasó primero y qué pasó después?",
                            "Primero, Tomás se perdió. Después, Lila lo buscó. Al final, lo encontró en una caja.",
                            "🏠",
                        ),
                    ),
                    practica = listOf(
                        P(SELECCION, "En el cuento de Lila, ¿a quién buscaba Lila?", listOf("A su gato", "A su abuela", "A su perro"), 0, "Lila buscaba a su gato Tomás.", visual = "🐱"),
                        P(SELECCION, "¿Dónde encontró Lila a Tomás?", listOf("En el patio", "En una caja", "En la cama"), 1, "Tomás estaba dormido dentro de una caja."),
                        P(COMPLETACION, "Completa: Los ____ son quienes viven la historia.", listOf("lugares", "personajes", "títulos"), 1, "Los personajes son quienes viven la historia.", MEDIA),
                        P(SECUENCIA, "¿Qué pasó PRIMERO en el cuento de Lila?", listOf("Lila encontró a Tomás", "Tomás se perdió", "Tomás durmió en la caja"), 1, "Primero Tomás se perdió; después Lila lo buscó y lo encontró.", MEDIA),
                    ),
                    prueba = listOf(
                        P(SELECCION, "Lee el cuento de arriba. ¿Con quién fue Benjamín al parque?", listOf("Con su abuela", "Con su papá", "Con un amigo"), 0, "Benjamín fue al parque con su abuela."),
                        P(SELECCION, "¿Dónde empieza el cuento?", listOf("En la escuela", "En el parque", "En la playa"), 1, "El cuento empieza en el parque."),
                        P(SELECCION, "¿De qué color era el paraguas?", listOf("Rojo", "Azul", "Amarillo"), 2, "El paraguas era amarillo.", visual = "☂️"),
                        P(SECUENCIA, "¿Qué hicieron AL FINAL?", listOf("Abrieron el paraguas", "Tomaron leche caliente", "Fueron al parque"), 1, "Al llegar a casa tomaron leche caliente.", MEDIA),
                    ),
                ),
            ),
        ),
        // ------------------------------- MATEMÁTICA -------------------------------
        UnidadSemilla(
            2, "Sumo y resto hasta el 100", textoReferencia = "",
            estado = EstadoContenido.PUBLICADO,
            oas = listOf(
                OaSemilla(
                    "MA02 OA 09", "Sumar y restar números hasta 100 usando estrategias como descomponer en decenas y unidades.", "Números y operaciones",
                    contenidos = listOf(
                        C(
                            "Decenas y unidades",
                            "Un número como 23 tiene 2 decenas y 3 unidades. 2 decenas son 20.",
                            "Mira el número veintitrés. Tiene dos decenas, que son veinte, y tres unidades.",
                            "23 = 20 + 3\n🟦🟦 + 🔸🔸🔸",
                            "🧮",
                        ),
                        C(
                            "Sumar descomponiendo",
                            "Para sumar, junta las decenas con las decenas y las unidades con las unidades.",
                            "Para sumar, primero suma las decenas. Luego suma las unidades. Al final, junta todo.",
                            "23 + 15\nDecenas: 20 + 10 = 30\nUnidades: 3 + 5 = 8\nResultado: 30 + 8 = 38",
                            "➕",
                        ),
                        C(
                            "Restar descomponiendo",
                            "Para restar, quita las decenas y luego quita las unidades.",
                            "Para restar, primero quita las decenas. Después quita las unidades.",
                            "47 − 12\nDecenas: 40 − 10 = 30\nUnidades: 7 − 2 = 5\nResultado: 30 + 5 = 35",
                            "➖",
                        ),
                    ),
                    practica = listOf(
                        P(SELECCION, "¿Cuánto es 20 + 30?", listOf("40", "50", "60"), 1, "2 decenas + 3 decenas = 5 decenas = 50.", visual = "🟦🟦 + 🟦🟦🟦"),
                        P(SELECCION, "¿Cuánto es 34 + 12?", listOf("46", "44", "56"), 0, "30 + 10 = 40 y 4 + 2 = 6. Total: 46."),
                        P(SELECCION, "¿Cuánto es 58 − 25?", listOf("23", "33", "43"), 1, "50 − 20 = 30 y 8 − 5 = 3. Total: 33.", MEDIA),
                        P(COMPLETACION, "Completa: 15 + ___ = 20", listOf("5", "10", "3"), 0, "Desde 15 faltan 5 para llegar a 20.", MEDIA),
                        P(SECUENCIA, "¿Qué número sigue? 10, 20, 30, ___", listOf("31", "35", "40"), 2, "La secuencia avanza de 10 en 10.", BASICA, "🔟"),
                    ),
                    prueba = listOf(
                        P(SELECCION, "¿Cuánto es 41 + 26?", listOf("67", "57", "65"), 0, "40 + 20 = 60 y 1 + 6 = 7. Total: 67."),
                        P(SELECCION, "¿Cuánto es 75 − 31?", listOf("46", "44", "34"), 1, "70 − 30 = 40 y 5 − 1 = 4. Total: 44."),
                        P(COMPLETACION, "Completa: 50 + ___ = 80", listOf("20", "30", "40"), 1, "De 50 a 80 hay 3 decenas: 30.", MEDIA),
                        P(SECUENCIA, "¿Qué número sigue? 5, 10, 15, ___", listOf("16", "20", "25"), 1, "La secuencia avanza de 5 en 5.", BASICA),
                        P(SELECCION, "Tenía 36 láminas y regalé 14. ¿Cuántas me quedan?", listOf("22", "50", "24"), 0, "36 − 14: 30 − 10 = 20 y 6 − 4 = 2. Quedan 22.", DESAFIO, "🃏"),
                    ),
                ),
            ),
        ),
        // -------------------------------- CIENCIAS --------------------------------
        UnidadSemilla(
            3, "Los animales y sus hábitats", textoReferencia = "",
            estado = EstadoContenido.AUTORIZADO,
            oas = listOf(
                OaSemilla(
                    "CN02 OA 03", "Relacionar animales de Chile con el hábitat donde viven y lo que necesitan para vivir.", "Ciencias de la vida",
                    contenidos = listOf(
                        C(
                            "¿Qué es un hábitat?",
                            "El hábitat es el lugar donde vive un animal. Ahí encuentra comida, agua y refugio.",
                            "El hábitat es el lugar donde vive un animal. Allí encuentra comida, agua y un lugar para protegerse.",
                            "El pez vive en el agua. El agua es su hábitat.",
                            "🏞️",
                        ),
                        C(
                            "Animales de Chile",
                            "En Chile hay animales en el mar, en la cordillera y en los bosques.",
                            "Conoce tres animales de Chile: el pingüino vive en la costa, el cóndor en la cordillera y el pudú en el bosque.",
                            "🐧 Pingüino → costa y mar\n🦅 Cóndor → cordillera\n🦌 Pudú → bosque del sur",
                            "🇨🇱",
                        ),
                    ),
                    practica = listOf(
                        P(ASOCIACION, "¿Dónde vive el pingüino?", listOf("En el desierto", "En la costa y el mar", "En la ciudad"), 1, "El pingüino vive en la costa y busca su comida en el mar.", visual = "🐧"),
                        P(ASOCIACION, "¿Dónde vive el cóndor?", listOf("En la cordillera", "En un lago", "Bajo la tierra"), 0, "El cóndor vuela alto en la cordillera.", visual = "🦅"),
                        P(SELECCION, "¿Qué encuentra un animal en su hábitat?", listOf("Juguetes", "Comida, agua y refugio", "Ropa"), 1, "El hábitat le da comida, agua y refugio.", MEDIA),
                    ),
                    prueba = listOf(
                        P(ASOCIACION, "El pudú vive en…", listOf("el bosque", "el mar", "el desierto"), 0, "El pudú vive en los bosques del sur.", visual = "🦌"),
                        P(SELECCION, "El hábitat es…", listOf("lo que come un animal", "el lugar donde vive un animal", "el nombre de un animal"), 1, "El hábitat es el lugar donde vive."),
                        P(ASOCIACION, "¿Qué animal vive en el agua?", listOf("El cóndor", "El pez", "El pudú"), 1, "El pez vive en el agua.", visual = "🐟"),
                    ),
                ),
            ),
        ),
        // -------------------------------- HISTORIA --------------------------------
        UnidadSemilla(
            4, "Paisajes de Chile", textoReferencia = "",
            estado = EstadoContenido.PUBLICADO,
            oas = listOf(
                OaSemilla(
                    "HI02 OA 08", "Reconocer paisajes del norte, centro y sur de Chile y algunas de sus características.", "Geografía",
                    contenidos = listOf(
                        C(
                            "Chile es largo",
                            "Chile es un país largo. Tiene norte, centro y sur. Cada zona tiene paisajes distintos.",
                            "Chile es un país muy largo. Lo dividimos en norte, centro y sur.",
                            "Norte ☀️  •  Centro 🍇  •  Sur 🌧️",
                            "🗺️",
                        ),
                        C(
                            "Tres paisajes",
                            "En el norte hay desierto y poca lluvia. En el centro hay valles. En el sur llueve mucho y hay bosques.",
                            "En el norte hay desierto. En el centro hay valles con cultivos. En el sur llueve mucho y hay bosques.",
                            "🏜️ Norte: desierto\n🌄 Centro: valles\n🌲 Sur: bosques y lluvia",
                            "🏔️",
                        ),
                    ),
                    practica = listOf(
                        P(ASOCIACION, "¿Dónde está el desierto?", listOf("En el norte", "En el sur", "En el centro"), 0, "El desierto está en el norte de Chile.", visual = "🏜️"),
                        P(SELECCION, "¿Cómo es el clima del sur?", listOf("Muy seco", "Con mucha lluvia", "Sin nubes"), 1, "En el sur llueve mucho.", visual = "🌧️"),
                        P(SELECCION, "¿Qué hay en el centro de Chile?", listOf("Valles con cultivos", "Hielo todo el año", "Solo arena"), 0, "En el centro hay valles con cultivos.", MEDIA),
                    ),
                    prueba = listOf(
                        P(ASOCIACION, "Bosques y mucha lluvia. ¿Qué zona es?", listOf("Norte", "Centro", "Sur"), 2, "El sur tiene bosques y mucha lluvia.", visual = "🌲"),
                        P(ASOCIACION, "Desierto y poca lluvia. ¿Qué zona es?", listOf("Norte", "Centro", "Sur"), 0, "El norte tiene desierto."),
                        P(SELECCION, "Chile se divide en…", listOf("norte, centro y sur", "solo norte y sur", "este y oeste"), 0, "Estudiamos tres zonas: norte, centro y sur.", MEDIA),
                    ),
                ),
            ),
        ),
        // Unidad en borrador: permite demostrar el flujo de aprobación de UTP.
        UnidadSemilla(
            4, "Mi comunidad y sus trabajos", textoReferencia = "",
            estado = EstadoContenido.BORRADOR,
            oas = listOf(
                OaSemilla(
                    "HI02 OA 11", "Identificar trabajos de la comunidad y su aporte a la vida diaria.", "Formación ciudadana",
                    contenidos = emptyList(), practica = emptyList(), prueba = emptyList(),
                ),
            ),
        ),
    )

    suspend fun poblarSiVacia(db: MPazDatabase) {
        if (db.usuarioDao().contar() > 0) return
        db.withTransaction { poblar(db) }
    }

    private suspend fun poblar(db: MPazDatabase) {
        val ahora = System.currentTimeMillis()
        val hoy = LocalDate.now().toEpochDay()
        val curriculo = db.curriculoDao()

        db.usuarioDao().insertar(usuarios)
        curriculo.insertarAsignaturas(asignaturas)

        // oaId por código, y preguntas de prueba final por código (para la semilla de intentos)
        val oaPorCodigo = mutableMapOf<String, Long>()
        val pruebaPorCodigo = mutableMapOf<String, List<Pair<Long, Int>>>() // (preguntaId, correcta)

        for (u in unidades) {
            val unidadId = curriculo.insertarUnidad(
                UnidadEntity(
                    asignaturaId = u.asignaturaId, titulo = u.titulo, textoReferencia = u.textoReferencia,
                    estado = u.estado, utpResponsableId = UTP_ID, creadaEn = ahora, modificadaEn = ahora,
                ),
            )
            db.historialDao().registrar(HistorialEntity(0, "Unidad", unidadId, "Creada en estado ${u.estado.etiqueta}", UTP_ID, ahora))

            for (oa in u.oas) {
                val oaId = curriculo.insertarObjetivo(ObjetivoEntity(unidadId = unidadId, codigo = oa.codigo, descripcion = oa.descripcion, eje = oa.eje))
                oaPorCodigo[oa.codigo] = oaId
                curriculo.insertarContenidos(
                    oa.contenidos.mapIndexed { i, c ->
                        ContenidoEntity(
                            oaId = oaId, titulo = c.titulo, explicacion = c.explicacion, instruccionAudio = c.audio,
                            ejemploGuiado = c.ejemplo, apoyoVisual = c.visual, orden = i + 1, origen = "Propio (equipo MPAZ PRO)",
                        )
                    },
                )
                curriculo.insertarPreguntas(oa.practica.mapIndexed { i, p -> p.aEntidad(oaId, false, i + 1) })
                val idsPrueba = curriculo.insertarPreguntas(oa.prueba.mapIndexed { i, p -> p.aEntidad(oaId, true, i + 1) })
                pruebaPorCodigo[oa.codigo] = idsPrueba.zip(oa.prueba.map { it.correcta })
            }
        }

        // ---- Asignaciones del docente (Ciencias queda sin asignar para demostrar el formulario) ----
        val asignaciones = db.asignacionDao()
        fun asig(codigo: String, docente: Long, curso: String, max: Int) = AsignacionEntity(
            oaId = oaPorCodigo.getValue(codigo), docenteId = docente, curso = curso,
            desdeEpochDay = hoy - 7, hastaEpochDay = hoy + 21, maxIntentos = max, creadaEn = ahora,
        )
        val asigLenguajeA = asignaciones.insertar(asig("LE02 OA 05", DOCENTE_A_ID, "2°A", 2))
        val asigMateA = asignaciones.insertar(asig("MA02 OA 09", DOCENTE_A_ID, "2°A", 3))
        asignaciones.insertar(asig("HI02 OA 08", DOCENTE_A_ID, "2°A", 2))
        asignaciones.insertar(asig("MA02 OA 09", DOCENTE_B_ID, "2°B", 2))

        // ---- Intentos ficticios para que el informe docente tenga datos desde el inicio ----
        val evaluacion = db.evaluacionDao()
        suspend fun intento(est: Long, asigId: Long, codigo: String, aciertos: List<Boolean>, haceDias: Int) {
            val preguntas = pruebaPorCodigo.getValue(codigo)
            val respuestas = preguntas.zip(aciertos).map { (p, ok) ->
                val (preguntaId, correcta) = p
                RespuestaEntity(intentoId = 0, preguntaId = preguntaId, indiceSeleccionado = if (ok) correcta else (correcta + 1) % 3, correcta = ok)
            }
            evaluacion.registrarIntento(
                IntentoEntity(
                    estudianteId = est, asignacionId = asigId, oaId = oaPorCodigo.getValue(codigo), numero = 1,
                    puntaje = aciertos.count { it }, puntajeMaximo = aciertos.size,
                    rendidoEn = ahora - haceDias * 86_400_000L,
                ),
                respuestas,
            )
        }
        intento(301, asigMateA, "MA02 OA 09", listOf(true, true, true, false, true), 2)
        intento(302, asigMateA, "MA02 OA 09", listOf(true, true, true, true, true), 3)
        intento(302, asigLenguajeA, "LE02 OA 05", listOf(true, false, true, false), 1)
        intento(303, asigMateA, "MA02 OA 09", listOf(true, false, false, true, false), 1)
    }

    private fun P.aEntidad(oaId: Long, esFinal: Boolean, orden: Int) = PreguntaEntity(
        oaId = oaId, esPruebaFinal = esFinal, tipo = tipo, enunciado = enunciado, apoyoVisual = visual,
        alternativas = alternativas, indiceCorrecto = correcta, explicacion = explicacion,
        dificultad = dificultad, orden = orden,
    )
}
