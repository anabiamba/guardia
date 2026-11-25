package br.com.guardia.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class HistoricoDataTest {
    @Test fun preservaHorarioLocalDoServidorComFracaoDeSegundos() {
        val anterior = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+09:00"))
            assertEquals("22/11/2026 13:05", dataHistorico("2026-11-22T13:05:42.123456"))
            assertEquals("22/11/2026 13:05", dataHistorico("2026-11-22T13:05:42"))
        } finally {
            TimeZone.setDefault(anterior)
        }
    }
    @Test fun formatoInvalidoNaoDerrubaATela() {
        assertEquals("Data indisponível", dataHistorico(""))
        assertEquals("Data indisponível", dataHistorico("2026-02-30T12:00:00"))
    }
}