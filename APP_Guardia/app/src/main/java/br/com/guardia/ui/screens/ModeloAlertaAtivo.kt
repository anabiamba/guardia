package br.com.guardia.ui.screens

import kotlin.math.*

internal data class ModeloAlertaAtivo(
    val id: Long,
    val latitude: Double = Double.NaN,
    val longitude: Double = Double.NaN,
    val criadaEm: String = "",
    val segundosInformados: Long? = null,
    val consultaIniciadaEm: Long = 0L
)

// Estimativa visual baseada na duração do servidor e tempo monotônico, nunca no relógio civil do telefone.
internal fun segundosAlerta(modelo: ModeloAlertaAtivo, agoraMonotonico: Long): Long? {
    val inicial = modelo.segundosInformados?.takeIf { it in 0L..3600L } ?: return null
    val decorridos = ((agoraMonotonico - modelo.consultaIniciadaEm).coerceAtLeast(0L) / 1000L)
    return (inicial - decorridos).coerceAtLeast(0L)
}

internal fun horarioInicioAlerta(valor: String): String =
    if (valor.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"))) valor.substring(11, 16)
    else "Horário indisponível"

/** Anel geodésico de 1 km, apenas ilustração; a elegibilidade continua no backend. */
internal fun anelRaioAlerta(ponto: PontoMapa): List<Pair<Double, Double>> {
    val lat = Math.toRadians(ponto.latitude)
    val lon = Math.toRadians(ponto.longitude)
    val distancia = 1000.0 / 6371000.0
    val pontos = (0 until 72).map { indice ->
        val rumo = indice * 2.0 * PI / 72.0
        val latitude = asin(sin(lat) * cos(distancia) + cos(lat) * sin(distancia) * cos(rumo))
        val longitude = lon + atan2(sin(rumo) * sin(distancia) * cos(lat), cos(distancia) - sin(lat) * sin(latitude))
        Math.toDegrees(latitude) to ((Math.toDegrees(longitude) + 540.0) % 360.0 - 180.0)
    }
    return pontos + pontos.first()
}
