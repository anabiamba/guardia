package br.com.guardia.controller;

import java.util.UUID;

import br.com.guardia.config.AcessoApiConfig;
import br.com.guardia.dto.LoginRequest;
import br.com.guardia.dto.UsuarioResponse;
import br.com.guardia.service.SessaoService;
import br.com.guardia.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/sessoes")
public class SessaoController {

    private final br.com.guardia.service.LoginSessaoService loginSeguro;
    private final SessaoService sessoes;

    public SessaoController(br.com.guardia.service.LoginSessaoService loginSeguro, SessaoService sessoes) {
        this.loginSeguro = loginSeguro;
        this.sessoes = sessoes;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestHeader("X-Instalacao-Id") UUID instalacaoId,
                                   @RequestHeader(value = "X-Instalacao-Prova", required = false)
                                   String provaInstalacao, @Valid @RequestBody LoginRequest request) {
        if (instalacaoId.version() != 4 || instalacaoId.variant() != 2) {
            return ResponseEntity.badRequest().body("Informe um UUID v4 de instalação.");
        }
        var usuario = loginSeguro.login(request.getEmail(), request.getSenha(), instalacaoId, provaInstalacao);
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                    .body("E-mail ou senha inválidos.");
        }
        var conta = usuario.get().usuario();
        var credencial = usuario.get().credencial();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new LoginComSessao(UsuarioResponse.fromUsuario(conta),
                        credencial.token(), credencial.expiraEm().toString(), credencial.tokenRevogacao()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> entradaInvalida() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body("Dados de login inválidos.");
    }

    @PostMapping("/revogacoes")
    public ResponseEntity<Void> revogarComCredencial(@RequestBody RevogacaoRequest request) {
        sessoes.revogarComCredencial(request.tokenRevogacao());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    public record RevogacaoRequest(String tokenRevogacao) {
        @Override
        public String toString() {
            return "RevogacaoRequest";
        }
    }

    @DeleteMapping("/atual")
    public ResponseEntity<Void> revogar(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        sessoes.revogar(identidade);
        return ResponseEntity.noContent().build();
    }

    public record LoginComSessao(UsuarioResponse usuario, String token, String expiraEm, String tokenRevogacao) {
        @Override
        public String toString() {
            return "LoginComSessao";
        }
    }
}
