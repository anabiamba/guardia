package br.com.guardia.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.guardia.dto.AjudaAlertaRequest;
import br.com.guardia.dto.AlertaRequest;
import br.com.guardia.dto.AlertaStatusResponse;
import br.com.guardia.dto.EncerrarAlertaRequest;
import br.com.guardia.dto.UsuarioResponse;
import br.com.guardia.model.Alerta;
import br.com.guardia.model.Usuario;
import br.com.guardia.service.AlertaService;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    // ============================================
    // CRIAR ALERTA
    // ============================================

    @PostMapping
    public ResponseEntity<?> criarAlerta(
            @RequestBody AlertaRequest request
    ) {

        if (request.getUsuarioId() == null
                || request.getLatitude() == null
                || request.getLongitude() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Usuário, latitude e longitude " +
                            "são obrigatórios."
                    );
        }

        try {

            Alerta alerta = alertaService.criarAlerta(
                    request.getUsuarioId(),
                    request.getLatitude(),
                    request.getLongitude()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(alerta);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    // ============================================
    // LISTAR ALERTAS ATIVOS
    // ============================================

    @GetMapping("/ativos")
    public ResponseEntity<?> listarAlertasAtivos() {

        return ResponseEntity.ok(
                alertaService.listarAlertasAtivos()
        );
    }

    // ============================================
    // BUSCAR USUÁRIAS PRÓXIMAS DE UM ALERTA
    // ============================================

    @GetMapping("/{id}/proximas")
    public ResponseEntity<?> buscarUsuariosProximas(
            @PathVariable Long id
    ) {

        try {

            List<Usuario> usuarios =
                    alertaService.buscarUsuariosProximas(id);

            List<UsuarioResponse> respostas =
                    usuarios.stream()
                            .map(UsuarioResponse::fromUsuario)
                            .collect(Collectors.toList());

            return ResponseEntity.ok(respostas);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // ============================================
    // BUSCAR ALERTAS PRÓXIMOS DE UMA USUÁRIA
    // ============================================

    @GetMapping("/proximos/{usuarioId}")
    public ResponseEntity<?> buscarAlertasProximos(
            @PathVariable Long usuarioId
    ) {

        try {

            List<Alerta> alertas =
                    alertaService.buscarAlertasProximos(
                            usuarioId
                    );

            return ResponseEntity.ok(alertas);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // ============================================
    // REGISTRAR AJUDA
    // ============================================

    @PostMapping("/{id}/ajudar")
    public ResponseEntity<?> registrarAjuda(
            @PathVariable Long id,
            @RequestBody AjudaAlertaRequest request
    ) {

        // Verifica se o ID da usuária foi enviado.
        if (request.getUsuarioId() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "O ID da usuária é obrigatório."
                    );
        }

        try {

            Alerta alerta =
                    alertaService.registrarAjuda(
                            id,
                            request.getUsuarioId()
                    );

            return ResponseEntity.ok(alerta);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================
    // BUSCAR STATUS DO ALERTA DA USUÁRIA
    // ============================================

    @GetMapping("/usuario/{usuarioId}/status")
    public ResponseEntity<?> buscarStatusAlerta(
            @PathVariable Long usuarioId
    ) {

        Optional<AlertaStatusResponse> resposta =
                alertaService.buscarStatusAlerta(
                        usuarioId
                );

        if (resposta.isEmpty()) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(
                resposta.get()
        );
    }

    // ============================================
    // ENCERRAR ALERTA
    // ============================================

    @PutMapping("/{id}/encerrar")
    public ResponseEntity<?> encerrarAlerta(
            @PathVariable Long id,
            @RequestBody EncerrarAlertaRequest request
    ) {

        // Verifica se o ID da usuária foi enviado.
        if (request.getUsuarioId() == null) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "O ID da usuária é obrigatório."
                    );
        }

        try {

            alertaService.encerrarAlerta(
                    id,
                    request.getUsuarioId()
            );

            return ResponseEntity.ok(
                    "Alerta encerrado com sucesso."
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}