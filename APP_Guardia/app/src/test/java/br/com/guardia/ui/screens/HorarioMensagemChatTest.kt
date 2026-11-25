package br.com.guardia.ui.screens
import org.junit.Assert.*
import org.junit.Test
class HorarioMensagemChatTest {
    @Test fun usaHorarioDaApiSemInventarHoraAtual() {
        assertEquals("12:29", horarioMensagemChat("2026-11-24T12:29:30.123456"))
        assertEquals("00:05", horarioMensagemChat("2026-11-24T00:05:00"))
    }
    @Test fun horarioAusenteOuInvalidoFicaOculto() {
        listOf("", "null", "2026-11-24T29:99:00", "2026-02-30T12:00:00").forEach {
            assertNull(horarioMensagemChat(it))
        }
    }
}
