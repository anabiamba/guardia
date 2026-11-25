package br.com.guardia.ui.screens
import org.junit.Assert.assertEquals
import org.junit.Test
class SolicitanteRotuloTest {
    @Test fun nomeExibePrimeiroTokenSemSobrenome() {
        assertEquals("Ana (solicitante)", rotuloSolicitante(" Ana Beatriz "))
        assertEquals("Érica (solicitante)", rotuloSolicitante("\u00a0Érica\u00a0Silva\u00a0"))
    }
    @Test fun ausenteNaoExibeNullOuIdentidadeInventada() {
        assertEquals("Solicitante do alerta", rotuloSolicitante(null))
        assertEquals("Solicitante do alerta", rotuloSolicitante("null"))
        assertEquals("Solicitante do alerta", rotuloSolicitante("  "))
    }
}
