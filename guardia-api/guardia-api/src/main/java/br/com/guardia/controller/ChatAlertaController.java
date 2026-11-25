package br.com.guardia.controller;

import java.util.List;
import br.com.guardia.config.AcessoApiConfig;
import br.com.guardia.service.ChatAlertaService;
import br.com.guardia.service.SessaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/alertas/{alertaId}/mensagens")
public class ChatAlertaController {
    private final ChatAlertaService chat;

    public ChatAlertaController(ChatAlertaService chat) {
        this.chat = chat;
    }

    @GetMapping
    public List<ChatAlertaService.Mensagem> listar(
            @PathVariable Long alertaId, @RequestParam(defaultValue = "0") long depois,
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        if (depois < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return chat.listar(alertaId, identidade.usuarioId(), depois);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatAlertaService.Mensagem enviar(
            @PathVariable Long alertaId, @Valid @RequestBody Envio envio,
            @RequestAttribute(AcessoApiConfig.IDENTIDADE) SessaoService.Identidade identidade) {
        return chat.enviar(alertaId, identidade.usuarioId(), envio.texto());
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void mensagemInvalida() {}

    public record Envio(@NotBlank @Size(max = 500) String texto) {
        @Override
        public String toString() { return "Envio"; }
    }
}
