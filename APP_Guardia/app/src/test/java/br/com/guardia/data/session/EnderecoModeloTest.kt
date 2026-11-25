package br.com.guardia.data.session

import br.com.guardia.data.location.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class EnderecoModeloTest {
    private fun imediato(partes: PartesEndereco?): (Double, Double, (PartesEndereco?) -> Unit) -> (() -> Unit) =
        { _, _, concluir -> concluir(partes); val cancelar: () -> Unit = {}; cancelar }

    @Test fun formataRuaNumeroERegiaoSemInventarDados() {
        assertEquals("Rua Fictícia, 123 — Centro — Cidade Fictícia — UF",
            formatarEndereco(PartesEndereco("Rua Fictícia", "123", "Centro", "Cidade Fictícia", "UF")))
    }
    @Test fun numeroAusenteNaoViraZeroOuNull() {
        assertEquals("Rua Fictícia — Centro", formatarEndereco(PartesEndereco("Rua Fictícia", bairro = "Centro")))
        assertEquals("Rua Fictícia", formatarEndereco(PartesEndereco("Rua Fictícia", "null")))
    }
    @Test fun semRuaNaoApresentaNumeroOuCidadeComoEnderecoPreciso() {
        assertNull(formatarEndereco(null))
        assertNull(formatarEndereco(PartesEndereco(null, "123", cidade = "Cidade Fictícia")))
    }
    @Test fun normalizaSomenteApresentacaoEDeduplicaRegiao() {
        assertEquals("Rua Fictícia — Cidade Fictícia", formatarEndereco(
            PartesEndereco(" Rua Fictícia ", bairro = "Cidade Fictícia", cidade = "cidade fictícia")))
    }
    @Test fun coordenadaInvalidaNaoChamaConsultor() = runBlocking<Unit> {
        var chamou = false
        assertNull(consultarEndereco(Double.NaN, 0.0, { _, _, _ -> chamou = true; ({}) }))
        assertNull(consultarEndereco(91.0, 0.0, { _, _, _ -> chamou = true; ({}) }))
        assertFalse(chamou)
    }
    @Test fun falhaOuAusenciaDeResultadoMantemFallback() = runBlocking<Unit> {
        assertNull(consultarEndereco(0.0, 0.0, imediato(null)))
        assertNull(consultarEndereco(0.0, 0.0, { _, _, _ -> throw java.io.IOException("Falha simulada") }))
        assertEquals("Rua Fictícia", consultarEndereco(0.0, 0.0, imediato(PartesEndereco("Rua Fictícia"))))
    }
    @Test fun prazoLimitaConsultaECancelaEsperaLocal() = runBlocking<Unit> {
        var cancelou = false
        var resposta: ((PartesEndereco?) -> Unit)? = null
        val resultado = consultarEndereco(0.0, 0.0, { _, _, callback ->
            resposta = callback
            val cancelar: () -> Unit = { cancelou = true }; cancelar
        }, timeoutMillis = 30)
        assertNull(resultado); assertTrue(cancelou)
        resposta?.invoke(PartesEndereco("Resposta tardia"))
    }
    @Test fun cancelamentoDaConsultaAnteriorNaoContaminaNova() = runBlocking<Unit> {
        var respostaAntiga: ((PartesEndereco?) -> Unit)? = null
        var cancelou = false
        val antiga = async(start = CoroutineStart.UNDISPATCHED) {
            consultarEndereco(0.0, 0.0, { _, _, callback ->
                respostaAntiga = callback
                val cancelar: () -> Unit = { cancelou = true }; cancelar
            })
        }
        antiga.cancelAndJoin()
        val nova = consultarEndereco(1.0, 1.0, imediato(PartesEndereco("Rua Nova Fictícia")))
        respostaAntiga?.invoke(PartesEndereco("Rua Antiga Fictícia"))
        assertTrue(cancelou); assertTrue(antiga.isCancelled); assertEquals("Rua Nova Fictícia", nova)
    }
    @Test fun callbacksDuplicadosNaoSubstituemPrimeiroResultado() = runBlocking<Unit> {
        val resultado = consultarEndereco(0.0, 0.0, { _, _, callback ->
            callback(PartesEndereco("Primeira Rua Fictícia"))
            callback(PartesEndereco("Outra Rua Fictícia"))
            val cancelar: () -> Unit = {}; cancelar
        })
        assertEquals("Primeira Rua Fictícia", resultado)
    }
}

