package br.com.guardia.controller;

import br.com.guardia.config.AcessoApiConfig;
import br.com.guardia.dto.AlterarSenhaRequest;
import br.com.guardia.service.AlterarSenhaService;
import br.com.guardia.service.SessaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestController
@RequestMapping("/api/v2/usuarios/me/senha")
public class SenhaController {
    private final AlterarSenhaService service;
    public SenhaController(AlterarSenhaService service) { this.service = service; }
    @PutMapping public ResponseEntity<Void> alterar(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade,
            @Valid @RequestBody AlterarSenhaRequest request) {
        service.alterar(identidade, request);
        return ResponseEntity.noContent().build();
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> invalido() {
        return ResponseEntity.badRequest().body("Informe a senha atual e uma nova senha com no mínimo 8 caracteres, letra, número e caractere especial, até 72 bytes.");
    }
}
