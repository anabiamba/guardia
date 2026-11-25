package br.com.guardia.service;

import br.com.guardia.model.Alerta;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.UsuarioRepository;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertaStatusVisualTest {
    final Clock clock = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    final AlertaRepository repository = mock(AlertaRepository.class);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final AlertaService service = new AlertaService(repository, usuarios, clock);
    final LocalDateTime agora = LocalDateTime.now(clock);
    Alerta alerta(LocalDateTime criada) {
        Alerta a = new Alerta(); org.springframework.test.util.ReflectionTestUtils.setField(a, "id", 7L); a.setUsuarioId(1L); a.setAtivo(true);
        a.setLatitude(0.0); a.setLongitude(0.0); a.setDataHoraCriacao(criada);
        when(repository.findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(1L, agora.minusHours(1)))
            .thenReturn(Optional.of(a));
        return a;
    }
    @Test void statusPreservaHorarioLocalExistenteERetornaDuracaoDoServidor() {
        alerta(agora.minusMinutes(20));
        var dto = service.buscarStatusAlerta(1L).orElseThrow();
        assertEquals(7L, dto.getId()); assertEquals(1L, dto.getUsuarioId());
        assertEquals(0.0, dto.getLatitude()); assertEquals(0.0, dto.getLongitude());
        assertEquals(agora.minusMinutes(20), dto.getCriadaEm());
        assertEquals(agora.plusMinutes(40), dto.getExpiraEm());
        assertEquals(2400L, dto.getSegundosRestantes());
        verifyNoInteractions(usuarios);
    }
    @Test void naoRetornaAlertaQueAtingiuUmaHoraMesmoSeConsultaTrouxerObjetoAntigo() {
        alerta(agora.minusHours(1));
        assertTrue(service.buscarStatusAlerta(1L).isEmpty());
    }
    @Test void naoRetornaEncerradoENaoConsultaAlertaDeOutraConta() {
        alerta(agora).setAtivo(false);
        assertTrue(service.buscarStatusAlerta(1L).isEmpty());
        verify(repository, never()).findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(eq(2L), any());
    }
    @Test void ultimoSegundoContinuaVigenteSemProrrogarPrazo() {
        alerta(agora.minusMinutes(59).minusSeconds(59));
        assertEquals(1L, service.buscarStatusAlerta(1L).orElseThrow().getSegundosRestantes());
    }
}

