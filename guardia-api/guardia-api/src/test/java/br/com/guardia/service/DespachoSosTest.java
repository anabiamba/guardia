package br.com.guardia.service;

import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import br.com.guardia.model.Usuario;
import br.com.guardia.model.Alerta;
import br.com.guardia.model.EventoPush;
import br.com.guardia.model.EntregaPush;
import br.com.guardia.model.VinculoFcm;
import br.com.guardia.repository.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

class DespachoSosTest {
    private final AlertaService alertas = mock(AlertaService.class);
    private final EventoPushRepository eventos = mock(EventoPushRepository.class);
    private final EntregaPushRepository entregas = mock(EntregaPushRepository.class);
    private final VinculoFcmRepository vinculos = mock(VinculoFcmRepository.class);
    private final NotificacaoService envio = spy(new NotificacaoSimuladaService());
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
    private final DespachoSosService despacho = new DespachoSosService(alertas, eventos, entregas, vinculos, envio, clock);

    @Test
    void gravaEventoEDestinosSemEnviarNaCriacao() {
        Alerta alerta = new Alerta();
        org.springframework.test.util.ReflectionTestUtils.setField(alerta, "id", 7L);
        alerta.setDataHoraCriacao(LocalDateTime.now(clock));
        when(alertas.criarAlerta(1L, 0.0, 0.0)).thenReturn(alerta);
        when(eventos.saveAndFlush(any())).thenAnswer(c -> c.getArgument(0));
        Usuario destinataria = new Usuario();
        destinataria.setId(2L);
        Usuario semVinculo = new Usuario();
        semVinculo.setId(3L);
        when(alertas.buscarUsuariosProximas(7L)).thenReturn(List.of(destinataria, semVinculo));
        when(vinculos.buscarVinculosAtivos(2L, clock.instant())).thenReturn(List.of(mock(VinculoFcm.class)));
        assertSame(alerta, despacho.criarAlerta(1L, 0.0, 0.0));
        verify(eventos).saveAndFlush(any(EventoPush.class));
        verify(entregas).save(any(EntregaPush.class));
        verify(entregas).flush();
        verify(envio, never()).enviarAvisoSos(any(), any(), any());
    }

    @Test
    void falhaDeGravacaoPropagaParaRollbackDaTransacao() {
        Alerta alerta = new Alerta();
        alerta.setDataHoraCriacao(LocalDateTime.now(clock));
        when(alertas.criarAlerta(1L, 0.0, 0.0)).thenReturn(alerta);
        when(eventos.saveAndFlush(any())).thenThrow(new IllegalStateException("Falha fictícia de persistência"));
        assertThrows(IllegalStateException.class, () -> despacho.criarAlerta(1L, 0.0, 0.0));
        verifyNoInteractions(entregas, vinculos);
    }
}
