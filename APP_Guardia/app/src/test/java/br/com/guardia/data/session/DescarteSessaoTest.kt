package br.com.guardia.data.session

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DescarteSessaoTest {
    @Test
    fun logoutOfflineMantemFilaEReenviaDepoisDaTrocaDeConta() = runBlocking {
        val fila = mutableMapOf<String, String>()
        descartarSessao({ fila["antiga"] = "credencial-ficticia" }, {}, { true })
        assertFalse(entregarRevogacoes(fila.keys.toList(), fila::get,
            { throw IOException() }, { fila.remove(it) }))
        assertTrue(fila.containsKey("antiga"))

        val sessaoAtual = "nova"
        assertTrue(entregarRevogacoes(fila.keys.toList(), fila::get,
            { if (it == "credencial-ficticia") 204 else 401 },
            { id -> assertFalse(id == sessaoAtual); fila.remove(id) }))
        assertTrue(fila.isEmpty())
    }

    @Test
    fun logoutOnlineMantemSolicitacaoSeConfirmacaoLocalFalhar() = runBlocking {
        val fila = mutableMapOf("sessao" to "credencial-ficticia")
        assertFalse(entregarRevogacoes(fila.keys.toList(), fila::get,
            { 204 }, { throw IOException() }))
        assertTrue(fila.isNotEmpty())
        assertTrue(entregarRevogacoes(fila.keys.toList(), fila::get,
            { 204 }, { fila.remove(it) }))
        assertTrue(fila.isEmpty())
    }

    @Test
    fun gravaFilaAntesDeInvalidarASessao() {
        val eventos = mutableListOf<String>()
        val resultado = descartarSessao(
            { eventos += "fila" },
            { eventos += "chave" },
            { eventos += "sessao"; true }
        )
        assertEquals(ResultadoDescarte.AGENDAVEL, resultado)
        assertEquals(listOf("fila", "chave", "sessao"), eventos)
    }

    @Test
    fun filaDuravelBloqueiaRestauracaoMesmoComFalhaNosDoisOutrosMecanismos() {
        var filaGravada = false
        val resultado = descartarSessao(
            { filaGravada = true },
            { throw IOException() },
            { false }
        )
        assertTrue(filaGravada)
        assertEquals(ResultadoDescarte.AGENDAVEL, resultado)
    }

    @Test
    fun chaveDestruidaImpedeRestauracaoSeGravacoesFalharem() {
        var chaveExiste = true
        val resultado = descartarSessao(
            { throw IOException() },
            { chaveExiste = false },
            { false }
        )
        assertFalse(chaveExiste)
        assertEquals(ResultadoDescarte.SEM_FILA, resultado)
    }

    @Test
    fun falhaTotalNaoConfirmaSaidaDuravel() {
        val resultado = descartarSessao({ throw IOException() }, { throw IOException() }, { false })
        assertEquals(ResultadoDescarte.FALHA_LOCAL, resultado)
    }
}
