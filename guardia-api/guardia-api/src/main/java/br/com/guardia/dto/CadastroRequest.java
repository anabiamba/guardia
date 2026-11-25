package br.com.guardia.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 72) String senha) {

    @Override
    public String toString() {
        return "CadastroRequest";
    }

    @AssertTrue
    public boolean isSenhaDentroDoLimiteBcrypt() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    @AssertTrue(message = br.com.guardia.util.PoliticaSenha.DESCRICAO)
    public boolean isNovaSenhaConformePolitica() {
        return br.com.guardia.util.PoliticaSenha.erro(senha) == null;
    }
}