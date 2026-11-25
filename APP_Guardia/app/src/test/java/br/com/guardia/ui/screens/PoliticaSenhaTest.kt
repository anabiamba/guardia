package br.com.guardia.ui.screens

import org.junit.Assert.*
import org.junit.Test

class PoliticaSenhaTest {
    @Test fun aceitaOitoCaracteresComLetraNumeroESimbolo() {
        listOf("abcd123!", "ABCD123!", "Ábcd123!", "Abcd١٢٣!", "Abcd123$", "Ab1! abc").forEach {
            assertNull(erroNovaSenha(it))
        }
    }
    @Test fun exigeTodasCategoriasEEspacosNaoContamComoSimbolo() {
        listOf("", "Abc12!x", "somenteletras", "12345678", "Abcdefg!", "1234567!", "Abcd123 ", "Abcd123\u00a0").forEach {
            assertNotNull(erroNovaSenha(it))
        }
    }
    @Test fun limiteUtf8NaoPermiteCorteSilencioso() {
        assertNull(erroNovaSenha("a".repeat(70)+"1!"))
        assertNotNull(erroNovaSenha("a".repeat(71)+"1!"))
        assertNull(erroNovaSenha("á".repeat(34)+"A1!x"))
        assertNotNull(erroNovaSenha("á".repeat(34)+"A1!xx"))
    }
    @Test fun contaUnicodeCorretamenteERejeitaControleETextoMalformado() {
        assertNotNull(erroNovaSenha("A1!"+"🙂".repeat(4)))
        assertNull(erroNovaSenha("A1!"+"🙂".repeat(5)))
        assertNotNull(erroNovaSenha("Abcd12!\n"))
        assertNotNull(erroNovaSenha("Abcd12!\u0000"))
        assertNotNull(erroNovaSenha("Abcd12!\uD800"))
    }
    @Test fun senhaAtualAntigaPodeSerUsadaNaTrocaSemMudarRegrasDaNova() {
        assertNull(validarTrocaSenha("somenteletras", "abcd123!", "abcd123!"))
        assertNotNull(validarTrocaSenha("somenteletras", "somenteletras", "somenteletras"))
        assertNotNull(validarTrocaSenha("somenteletras", "abcd123!", "diferente1!"))
        assertNotNull(validarTrocaSenha("abcd123!", "abcd123!", "abcd123!"))
    }
}
