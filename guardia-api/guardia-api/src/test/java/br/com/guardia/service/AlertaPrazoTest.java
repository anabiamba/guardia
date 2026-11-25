package br.com.guardia.service;

import java.time.*;
import java.util.List;
import java.util.Optional;

import br.com.guardia.model.Alerta;
import br.com.guardia.model.Usuario;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertaPrazoTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    private final LocalDateTime agora = LocalDateTime.now(clock);
    private final AlertaRepository alertas = mock(AlertaRepository.class);
    private final UsuarioRepository usuarias = mock(UsuarioRepository.class);
    private final AlertaService service = new AlertaService(alertas, usuarias, clock);

    @Test
    void limiteExatoEProximidade() {
        Alerta alerta = alerta();
        Usuario ajudante = new Usuario("Teste", "b@example.test", null, 0.0, 0.0);
        ajudante.setId(2L);
        ajudante.setLocalizacaoAtualizadaEm(clock.instant());
        when(alertas.bloquear(1L)).thenReturn(Optional.of(alerta));
        when(usuarias.findById(2L)).thenReturn(Optional.of(ajudante));
        alerta.setDataHoraCriacao(agora.minusMinutes(59).minusSeconds(59));
        assertEquals(2L, service.registrarAjudaComoProxima(1L, 2L).getUsuarioAjudandoId());
        alerta.setUsuarioAjudandoId(null);
        alerta.setDataHoraCriacao(agora.minusHours(1));
        assertThrows(ResponseStatusException.class, () -> service.registrarAjudaComoProxima(1L, 2L));
        assertNull(alerta.getUsuarioAjudandoId());
        when(alertas.findByAtivoTrueAndDataHoraCriacaoAfter(agora.minusHours(1))).thenReturn(List.of());
        assertTrue(service.buscarAlertasProximos(2L).isEmpty());
    }

    @Test
    void autorizacaoEEncerramentoRepetido() {
        Alerta alerta = alerta();
        when(alertas.bloquear(1L)).thenReturn(Optional.of(alerta));
        assertThrows(ResponseStatusException.class, () -> service.encerrarComoAutora(1L, 2L));
        assertTrue(alerta.getAtivo());
        service.encerrarComoAutora(1L, 1L);
        LocalDateTime encerramento = alerta.getDataHoraEncerramento();
        new AlertaService(alertas, usuarias, Clock.offset(clock, Duration.ofMinutes(1)))
                .encerrarComoAutora(1L, 1L);
        assertFalse(alerta.getAtivo());
        assertEquals(encerramento, alerta.getDataHoraEncerramento());
    }

    @Test
    void recuperacaoUsaCorteInclusivoEAtualizacaoCondicional() {
        service.encerrarAlertasExpirados();
        verify(alertas).encerrarExpirados(agora.minusHours(1), agora);
    }

    @Test
    void criacaoBloqueiaAutoraAntesDeConsultarSOSVigente() {
        when(usuarias.bloquearParaCriarAlerta(1L)).thenReturn(Optional.of(new Usuario()));
        when(alertas.findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(1L, agora.minusHours(1)))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(alerta()));
        when(alertas.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        assertEquals(agora, service.criarAlerta(1L, 0.0, 0.0).getDataHoraCriacao());
        assertThrows(IllegalArgumentException.class, () -> service.criarAlerta(1L, 0.0, 0.0));
        var ordem = inOrder(usuarias, alertas);
        ordem.verify(usuarias).bloquearParaCriarAlerta(1L);
        ordem.verify(alertas).findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(1L, agora.minusHours(1));
        ordem.verify(alertas).save(any());
    }

    private Alerta alerta() {
        var alerta = new Alerta();
        alerta.setUsuarioId(1L);
        alerta.setLatitude(0.0);
        alerta.setLongitude(0.0);
        alerta.setDataHoraCriacao(agora);
        return alerta;
    }
}
