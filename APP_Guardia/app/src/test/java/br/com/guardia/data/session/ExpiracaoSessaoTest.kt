package br.com.guardia.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpiracaoSessaoTest {
    @Test
    fun interpretaUtcComOuSemFracao() {
        assertEquals(0L, ExpiracaoSessao.emMillis("1970-01-01T00:00:00Z"))
        assertEquals(100L, ExpiracaoSessao.emMillis("1970-01-01T00:00:00.1Z"))
        assertEquals(123L, ExpiracaoSessao.emMillis("1970-01-01T00:00:00.123456789Z"))
    }

    @Test
    fun rejeitaDataForaDoContrato() {
        assertThrows(IllegalArgumentException::class.java) {
            ExpiracaoSessao.emMillis("1970-01-01T00:00:00")
        }
        assertThrows(java.text.ParseException::class.java) {
            ExpiracaoSessao.emMillis("2026-02-30T00:00:00Z")
        }
    }
}
