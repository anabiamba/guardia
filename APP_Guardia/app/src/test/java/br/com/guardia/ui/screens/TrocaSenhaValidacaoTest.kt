package br.com.guardia.ui.screens

import org.junit.Test
import org.junit.Assert.*

class TrocaSenhaValidacaoTest {
    @Test fun exigeAtualConfirmacaoEDiferenca() {
        assertNotNull(validarTrocaSenha("", "nova-ficticia-26", "nova-ficticia-26"))
        assertNotNull(validarTrocaSenha("atual-ficticia-26", "nova-ficticia-26", "outra"))
        assertNotNull(validarTrocaSenha("atual-ficticia-26", "atual-ficticia-26", "atual-ficticia-26"))
        assertNull(validarTrocaSenha("atual-ficticia-26", "nova-ficticia-26", "nova-ficticia-26"))
    }
    @Test fun respeitaLimiteDeBytesBcrypt() {
        val longa = "á".repeat(40)
        assertNotNull(validarTrocaSenha("atual-ficticia-26", longa, longa))
        assertNotNull(validarTrocaSenha(longa, "nova-ficticia-26", "nova-ficticia-26"))
        assertNotNull(validarTrocaSenha("atual-ficticia-26", "curta", "curta"))
    }
}

