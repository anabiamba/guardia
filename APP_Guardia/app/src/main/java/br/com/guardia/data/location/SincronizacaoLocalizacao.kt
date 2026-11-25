package br.com.guardia.data.location

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class Posicao(val latitude: Double, val longitude: Double)
internal enum class ResultadoLocalizacao { ATUALIZADA, INDISPONIVEL, DESCARTADA }
internal fun coordenadasValidas(latitude: Double, longitude: Double) = latitude.isFinite() && longitude.isFinite() &&
    latitude in -90.0..90.0 && longitude in -180.0..180.0
internal fun localizacaoRecente(medidaNanos: Long, agoraNanos: Long) =
    medidaNanos > 0 && agoraNanos >= medidaNanos && agoraNanos - medidaNanos <= 30_000_000_000L

internal suspend fun sincronizarLocalizacao(
    obter: suspend () -> Posicao?, vigente: () -> Boolean, enviar: suspend (Posicao) -> Boolean
): ResultadoLocalizacao {
    if (!vigente()) return ResultadoLocalizacao.DESCARTADA
    val posicao = obter() ?: return ResultadoLocalizacao.INDISPONIVEL
    currentCoroutineContext().ensureActive()
    if (!vigente()) return ResultadoLocalizacao.DESCARTADA
    if (!coordenadasValidas(posicao.latitude, posicao.longitude)) return ResultadoLocalizacao.INDISPONIVEL
    val aceita = enviar(posicao)
    currentCoroutineContext().ensureActive()
    if (!vigente()) return ResultadoLocalizacao.DESCARTADA
    return if (aceita) ResultadoLocalizacao.ATUALIZADA else ResultadoLocalizacao.INDISPONIVEL
}
