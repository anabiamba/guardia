package br.com.guardia.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoAlertaRecebidoTest {
    @Test
    fun confirmacaoAtrasadaNaoConfirmaOutroAlerta() {
        val estado = EstadoAlertaRecebido().confirmar(10L)
        assertFalse(estado.confirmadoPara(11L))
        assertTrue(estado.confirmadoPara(10L))
        assertTrue(estado.limpar(11L).confirmadoPara(10L))
        assertFalse(estado.limpar(10L).confirmadoPara(10L))
    }
}
