package br.com.guardia.data.session

import br.com.guardia.data.location.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LocalizacaoSincronizacaoTest {
    @Test fun aceitaSomenteCoordenadasValidasEMedidasRecentes() {
        assertTrue(coordenadasValidas(0.0, 0.0))
        assertFalse(coordenadasValidas(Double.NaN, 0.0))
        assertFalse(coordenadasValidas(91.0, 0.0))
        assertTrue(localizacaoRecente(1L, 30_000_000_001L))
        assertFalse(localizacaoRecente(1L, 30_000_000_002L))
        assertFalse(localizacaoRecente(5L, 4L))
        assertFalse(localizacaoRecente(0L, 4L))
    }
    @Test fun trocaContaDescartaResultadoAtrasadoSemEnviar() = runBlocking {
        var vigente = true
        var envios = 0
        val resultado = sincronizarLocalizacao(
            obter = { vigente = false; Posicao(0.0, 0.0) }, vigente = { vigente },
            enviar = { envios++; true })
        assertEquals(ResultadoLocalizacao.DESCARTADA, resultado)
        assertEquals(0, envios)
    }
    @Test fun cancelamentoDuranteCapturaNaoEnviaPosicao() = runBlocking {
        val iniciou = CompletableDeferred<Unit>()
        var envios = 0
        val job = launch {
            sincronizarLocalizacao(obter = { iniciou.complete(Unit); awaitCancellation() },
                vigente = { true }, enviar = { envios++; true })
        }
        iniciou.await()
        job.cancelAndJoin()
        assertEquals(0, envios)
    }
    @Test fun falhaNaoAfirmaAtualizacaoERespostaDeSessaoAntigaEDescartada() = runBlocking {
        assertEquals(ResultadoLocalizacao.INDISPONIVEL, sincronizarLocalizacao(
            obter = { null }, vigente = { true }, enviar = { error("Não deve enviar") }))
        var vigente = true
        assertEquals(ResultadoLocalizacao.DESCARTADA, sincronizarLocalizacao(
            obter = { Posicao(0.0, 0.0) }, vigente = { vigente }, enviar = { vigente = false; true }))
        assertEquals(ResultadoLocalizacao.ATUALIZADA, sincronizarLocalizacao(
            obter = { Posicao(0.0, 0.0) }, vigente = { true }, enviar = { true }))
    }
}
