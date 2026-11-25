package br.com.guardia.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.guardia.dto.CadastroRequest;
import br.com.guardia.dto.FcmTokenRequest;
import br.com.guardia.dto.LocalizacaoRequest;
import br.com.guardia.dto.LoginRequest;
import br.com.guardia.dto.UsuarioResponse;
import br.com.guardia.model.Usuario;
import br.com.guardia.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CadastroRequest request) {

        try {

            Usuario usuarioCriado =
                    usuarioService.cadastrar(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                            UsuarioResponse.fromUsuario(
                                    usuarioCriado
                            )
                    );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{email}")
    public ResponseEntity<?> buscarPorEmail(
            @PathVariable String email
    ) {

        return usuarioService.buscarPorEmail(email)
                .<ResponseEntity<?>>map(usuario ->
                        ResponseEntity.ok(
                                UsuarioResponse.fromUsuario(usuario)
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/teste")
    public String teste() {

        return "API Guardiã funcionando!";
    }

    @PutMapping("/{id}/localizacao")
    public ResponseEntity<?> atualizarLocalizacao(
            @PathVariable Long id,
            @RequestBody LocalizacaoRequest request
    ) {

        try {

            Usuario usuario =
                    usuarioService.atualizarLocalizacao(
                            id,
                            request.getLatitude(),
                            request.getLongitude()
                    );

            return ResponseEntity.ok(
                    UsuarioResponse.fromUsuario(usuario)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    @PutMapping("/{id}/token-fcm")
    public ResponseEntity<?> atualizarTokenFcm(
            @PathVariable Long id,
            @RequestBody FcmTokenRequest request
    ) {

        if (
                request.getTokenFcm() == null ||
                request.getTokenFcm().isBlank()
        ) {

            return ResponseEntity
                    .badRequest()
                    .body("O token FCM é obrigatório.");
        }

        try {

            Usuario usuario =
                    usuarioService.atualizarTokenFcm(
                            id,
                            request.getTokenFcm()
                    );

            return ResponseEntity.ok(
                    UsuarioResponse.fromUsuario(usuario)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> entradaInvalida(MethodArgumentNotValidException e) {
        boolean senhaInvalida = e.getBindingResult().getTarget() instanceof CadastroRequest
                && e.getBindingResult().getAllErrors().stream()
                .anyMatch(erro -> erro instanceof org.springframework.validation.FieldError campo
                        && (campo.getField().equals("senha") || campo.getField().equals("novaSenhaConformePolitica")
                            || campo.getField().equals("senhaDentroDoLimiteBcrypt")));
        return ResponseEntity.badRequest().body(senhaInvalida
                ? br.com.guardia.util.PoliticaSenha.DESCRICAO + " Limite de 72 bytes."
                : "Dados de cadastro ou login inválidos.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {

        return usuarioService.autenticar(
                loginRequest.getEmail(),
                loginRequest.getSenha()
        ).<ResponseEntity<?>>map(usuario ->
                ResponseEntity.ok(
                        UsuarioResponse.fromUsuario(usuario)
                )
        ).orElseGet(() ->
                ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("E-mail ou senha inválidos.")
        );
    }
}
