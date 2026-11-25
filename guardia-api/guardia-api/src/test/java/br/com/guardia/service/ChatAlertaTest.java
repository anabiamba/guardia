package br.com.guardia.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import br.com.guardia.model.Alerta;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.MensagemAlertaRepository;
import br.com.guardia.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatAlertaTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneOffset.UTC);
    private final AlertaRepository alertas = mock(AlertaRepository.class);
    private final MensagemAlertaRepository mensagens = mock(MensagemAlertaRepository.class);
    private final ChatAlertaService chat = new ChatAlertaService(
            new AlertaService(alertas, mock(UsuarioRepository.class), clock), mensagens);

    @Test
    void autorizaAutoraEAjudanteComPaginacao() {
        prepararAlerta();
        when(mensagens.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(mensagens.findTop50ByAlertaIdAndIdGreaterThanOrderByIdAsc(3L, 7L)).thenReturn(List.of());
        assertEquals(1L, chat.enviar(3L, 1L, " Oi ").remetenteId());
        assertEquals("Tudo bem?", chat.enviar(3L, 2L, "Tudo bem?").texto());
        assertTrue(chat.listar(3L, 1L, 7L).isEmpty());
        assertTrue(chat.listar(3L, 2L, 7L).isEmpty());
        verify(mensagens, times(2)).findTop50ByAlertaIdAndIdGreaterThanOrderByIdAsc(3L, 7L);
    }

    @Test
    void bloqueiaTerceiraConta() {
        prepararAlerta();
        assertBloqueado(4L);
    }

    @Test
    void bloqueiaAntesDoAceite() {
        prepararAlerta().setUsuarioAjudandoId(null);
        assertBloqueado(1L);
    }

    @Test
    void bloqueiaEncerrado() {
        prepararAlerta().setAtivo(false);
        assertBloqueado(2L);
    }

    @Test
    void bloqueiaAoCompletarUmaHora() {
        prepararAlerta().setDataHoraCriacao(LocalDateTime.now(clock).minusHours(1));
        assertBloqueado(1L);
    }

    private void assertBloqueado(long usuaria) {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> chat.listar(3L, usuaria, 0)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> chat.enviar(3L, usuaria, "Oi")).getStatusCode().value());
        verifyNoInteractions(mensagens);
    }

    private Alerta prepararAlerta() {
        var alerta = new Alerta();
        alerta.setUsuarioId(1L);
        alerta.setUsuarioAjudandoId(2L);
        alerta.setAtivo(true);
        alerta.setDataHoraCriacao(LocalDateTime.now(clock).minusMinutes(59));
        when(alertas.bloquear(3L)).thenReturn(Optional.of(alerta));
        return alerta;
    }
}
