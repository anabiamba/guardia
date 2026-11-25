package br.com.guardia.service;

import br.com.guardia.dto.CadastroRequest;
import br.com.guardia.dto.AlterarSenhaRequest;
import br.com.guardia.util.PoliticaSenha;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoliticaSenhaTest {
    @Test void aceitaOitoCaracteresComLetraNumeroEPontuacaoSemExigirMaiuscula() {
        for (String s : new String[]{"abcd123!", "ABCD123!", "Ábcd123!", "Abcd١٢٣!", "Abcd123$", "Ab1! abc"})
            assertNull(PoliticaSenha.erro(s));
    }
    @Test void rejeitaAusenciaDeCadaCategoriaEEspacoNaoValeComoSimbolo() {
        for (String s : new String[]{null, "", "Abc12!x", "somenteletras", "12345678", "Abcdefg!", "1234567!", "Abcd123 ", "Abcd123\u00a0"})
            assertNotNull(PoliticaSenha.erro(s));
    }
    @Test void respeitaBytesAsciiEAcentosSemTruncar() {
        assertNull(PoliticaSenha.erro("a".repeat(70)+"1!"));
        assertNotNull(PoliticaSenha.erro("a".repeat(71)+"1!"));
        assertNull(PoliticaSenha.erro("á".repeat(34)+"A1!x"));
        assertNotNull(PoliticaSenha.erro("á".repeat(34)+"A1!xx"));
    }
    @Test void contaCodepointsEmVezDeDobrarEmojis() {
        assertNotNull(PoliticaSenha.erro("A1!"+"🙂".repeat(4)));
        assertNull(PoliticaSenha.erro("A1!"+"🙂".repeat(5)));
    }
    @Test void rejeitaControlesESurrogatesInvalidos() {
        assertNotNull(PoliticaSenha.erro("Abcd12!\n"));
        assertNotNull(PoliticaSenha.erro("Abcd12!\u0000"));
        assertNotNull(PoliticaSenha.erro("Abcd12!\uD800"));
    }
    @Test void dtoAplicaRegraSoANovaSenha() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var v = factory.getValidator();
            assertFalse(v.validate(new CadastroRequest("Fictícia", "a@example.test", "somenteletras")).isEmpty());
            assertTrue(v.validate(new CadastroRequest("Fictícia", "a@example.test", "abcd123!")).isEmpty());
            assertTrue(v.validate(new AlterarSenhaRequest("somenteletras", "abcd123!")).isEmpty());
            assertFalse(v.validate(new AlterarSenhaRequest("somenteletras", "12345678")).isEmpty());
        }
    }
}
