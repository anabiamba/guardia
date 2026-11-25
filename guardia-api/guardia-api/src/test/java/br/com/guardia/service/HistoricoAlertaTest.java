package br.com.guardia.service;

import br.com.guardia.model.Alerta;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.UsuarioRepository;
import br.com.guardia.controller.RecursosAutenticadosController;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistoricoAlertaTest {
    final Clock clock = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    final AlertaRepository repo = mock(AlertaRepository.class);
    final AlertaService service = new AlertaService(repo, mock(UsuarioRepository.class), clock);

    @Test void consultaSomenteAutoraESemDadosSensiveis() {
        when(repo.findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(2L)).thenReturn(List.of());
        assertTrue(service.historico(2L).isEmpty());
        verify(repo).findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(2L);
        verifyNoMoreInteractions(repo);
        var campos = Arrays.stream(br.com.guardia.dto.HistoricoAlertaResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        assertEquals(List.of("id", "criadaEm", "encerradaEm", "situacao", "ajudaRegistrada"), campos);
    }

    @Test void limiteExatoNaoDependeDoAgendamentoENaoGravaBanco() {
        Alerta antes = alerta(LocalDateTime.now(clock).minusHours(1).plusNanos(1));
        Alerta limite = alerta(LocalDateTime.now(clock).minusHours(1));
        Alerta encerrado = alerta(LocalDateTime.now(clock).minusHours(2));
        encerrado.setAtivo(false);
        encerrado.setUsuarioAjudandoId(9L);
        when(repo.findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(2L))
                .thenReturn(List.of(antes, limite, encerrado));
        var resultado = service.historico(2L);
        assertEquals(List.of("ATIVO", "EXPIRADO", "ENCERRADO"),
                resultado.stream().map(br.com.guardia.dto.HistoricoAlertaResponse::situacao).toList());
        assertTrue(resultado.get(2).ajudaRegistrada());
        assertTrue(limite.getAtivo());
        verify(repo, never()).save(any());
    }

    @Test void controllerUsaIdentidadeAutenticada() {
        AlertaService alertas = mock(AlertaService.class);
        when(alertas.historico(2L)).thenReturn(List.of());
        var controller = new RecursosAutenticadosController(null, alertas, null, null);
        controller.historico(new SessaoService.Identidade(2L, UUID.randomUUID(), "ficticio"));
        verify(alertas).historico(2L);
    }

    private Alerta alerta(LocalDateTime criada) {
        Alerta a = new Alerta();
        a.setUsuarioId(2L);
        a.setDataHoraCriacao(criada);
        return a;
    }
}
