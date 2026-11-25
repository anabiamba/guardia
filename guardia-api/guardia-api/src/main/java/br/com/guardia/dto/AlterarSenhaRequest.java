package br.com.guardia.dto;

import java.nio.charset.StandardCharsets;
import jakarta.validation.constraints.*;

public record AlterarSenhaRequest(
        @NotBlank @Size(max = 72) String senhaAtual,
        @NotBlank @Size(max = 72) String novaSenha) {
    @AssertTrue public boolean isLimiteBcryptValido() {
        return (senhaAtual == null || senhaAtual.getBytes(StandardCharsets.UTF_8).length <= 72)
                && (novaSenha == null || novaSenha.getBytes(StandardCharsets.UTF_8).length <= 72);
    }
    @Override public String toString() { return "AlterarSenhaRequest"; }
    @AssertTrue(message = br.com.guardia.util.PoliticaSenha.DESCRICAO)
    public boolean isNovaSenhaConformePolitica() {
        return br.com.guardia.util.PoliticaSenha.erro(novaSenha) == null;
    }
}
