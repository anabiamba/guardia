package br.com.guardia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.guardia.model.Usuario;
import br.com.guardia.repository.UsuarioRepository;
import br.com.guardia.service.NotificacaoService;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;

    public NotificacaoController(
            UsuarioRepository usuarioRepository,
            NotificacaoService notificacaoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.notificacaoService = notificacaoService;
    }

    @PostMapping("/teste/{usuarioId}")
    public ResponseEntity<?> testarNotificacao(
            @PathVariable Long usuarioId
    ) {

        Usuario usuario =
                usuarioRepository.findById(usuarioId)
                        .orElse(null);

        if (usuario == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        if (
                usuario.getTokenFcm() == null ||
                usuario.getTokenFcm().isBlank()
        ) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "A usuária não possui token FCM cadastrado."
                    );
        }

        try {

            String resposta =
                    notificacaoService.enviarNotificacao(
                            usuario.getTokenFcm(),
                            "🚨 Teste Guardiã",
                            "Esta é uma notificação de teste do Guardiã."
                    );

            return ResponseEntity.ok(
                    "Notificação enviada com sucesso. " +
                            "Resposta Firebase: " +
                            resposta
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}