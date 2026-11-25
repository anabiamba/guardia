package br.com.guardia.controller;

import java.util.Map;

import br.com.guardia.config.AcessoApiConfig;
import br.com.guardia.dto.FcmTokenRequest;
import br.com.guardia.dto.LocalizacaoRequest;
import br.com.guardia.dto.UsuarioResponse;
import br.com.guardia.service.AlertaService;
import br.com.guardia.service.DespachoSosService;
import br.com.guardia.service.SessaoService;
import br.com.guardia.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2")
public class RecursosAutenticadosController {

    private final UsuarioService usuarios;
    private final AlertaService alertas;
    private final SessaoService sessoes;
    private final DespachoSosService despacho;

    public RecursosAutenticadosController(UsuarioService usuarios, AlertaService alertas,
                                          SessaoService sessoes, DespachoSosService despacho) {
        this.usuarios = usuarios;
        this.alertas = alertas;
        this.sessoes = sessoes;
        this.despacho = despacho;
    }

    @GetMapping("/usuarios/me")
    public ResponseEntity<br.com.guardia.dto.PerfilResponse> perfil(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        return ResponseEntity.of(usuarios.perfil(identidade.usuarioId()));
    }

    @PutMapping("/usuarios/me/localizacao")
    public UsuarioResponse atualizarLocalizacao(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade,
            @RequestBody LocalizacaoRequest request) {
        validarLocalizacao(request);
        return UsuarioResponse.fromUsuario(usuarios.atualizarLocalizacao(
                identidade.usuarioId(), request.getLatitude(), request.getLongitude()));
    }

    @PutMapping("/usuarios/me/token-fcm")
    public ResponseEntity<Void> atualizarTokenFcm(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade,
            @RequestBody FcmTokenRequest request) {
        String token = request.getTokenFcm();
        if (token == null || token.isBlank() || token.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token FCM inválido.");
        }
        sessoes.atualizarTokenFcm(identidade, token);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/usuarios/me/token-fcm")
    public ResponseEntity<Void> desvincularTokenFcm(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        sessoes.atualizarTokenFcm(identidade, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alertas")
    public ResponseEntity<?> criarAlerta(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade,
            @RequestBody LocalizacaoRequest request) {
        validarLocalizacao(request);
        try {
            var alerta = despacho.criarAlerta(
                    identidade.usuarioId(), request.getLatitude(), request.getLongitude());
            return ResponseEntity.status(HttpStatus.CREATED).body(alerta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/alertas/proximos")
    public ResponseEntity<?> alertasProximos(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        return ResponseEntity.ok(alertas.buscarAlertasProximosComNome(identidade.usuarioId()));
    }

    @GetMapping("/alertas/me/historico")
    public java.util.List<br.com.guardia.dto.HistoricoAlertaResponse> historico(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        return alertas.historico(identidade.usuarioId());
    }

    @GetMapping("/alertas/me/status")
    public ResponseEntity<?> status(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        return ResponseEntity.of(alertas.buscarStatusAlerta(identidade.usuarioId()));
    }

    @GetMapping("/alertas/{id}/proximas")
    public Map<String, Integer> contarProximas(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade,
            @PathVariable Long id) {
        return Map.of("quantidade", alertas.contarProximasComoAutora(id, identidade.usuarioId()));
    }

    @PostMapping("/alertas/{id}/ajudar")
    public ResponseEntity<?> ajudar(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(alertas.registrarAjudaComoProxima(id, identidade.usuarioId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/alertas/{id}/encerrar")
    public ResponseEntity<?> encerrar(
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade,
            @PathVariable Long id) {
        try {
            alertas.encerrarComoAutora(id, identidade.usuarioId());
            return ResponseEntity.ok("Alerta encerrado com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static void validarLocalizacao(LocalizacaoRequest request) {
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();
        if (latitude == null || longitude == null || !Double.isFinite(latitude)
                || !Double.isFinite(longitude) || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas.");
        }
    }
}
