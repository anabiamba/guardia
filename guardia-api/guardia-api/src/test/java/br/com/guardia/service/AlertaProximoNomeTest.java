package br.com.guardia.service;

import br.com.guardia.model.Alerta;
import br.com.guardia.model.Usuario;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.UsuarioRepository;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertaProximoNomeTest {
    final Clock clock = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneOffset.UTC);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final AlertaRepository alertas = mock(AlertaRepository.class);
    final AlertaService service = new AlertaService(alertas, usuarios, clock);
    Usuario conta(long id, String nome, double latitude) {
        Usuario u = new Usuario(); u.setId(id); u.setNome(nome); u.setLatitude(latitude); u.setLongitude(0.0);
        u.setLocalizacaoAtualizadaEm(clock.instant()); return u;
    }
    Alerta alerta(long autora, double latitude) {
        Alerta a = new Alerta(); a.setUsuarioId(autora); a.setLatitude(latitude); a.setLongitude(0.0);
        a.setDataHoraCriacao(LocalDateTime.now(clock));
        org.springframework.test.util.ReflectionTestUtils.setField(a, "id", autora+10L); return a;
    }
    @Test void adicionaSoPrimeiroNomeAosAlertasPermitidosSemMudarCamposAnteriores() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta(2, "Gustavo Teste", 0.0)));
        when(usuarios.findById(1L)).thenReturn(Optional.of(conta(1, "  Ana Beatriz  ", 0.0)));
        Alerta perto = alerta(1, 0.0); Alerta longe = alerta(3, 1.0);
        when(alertas.findByAtivoTrueAndDataHoraCriacaoAfter(any())).thenReturn(List.of(perto, longe));
        var lista = service.buscarAlertasProximosComNome(2L);
        assertEquals(1, lista.size()); var resposta = lista.get(0);
        assertEquals("Ana", resposta.primeiroNome()); assertEquals(perto.getId(), resposta.id());
        assertEquals(1L, resposta.usuarioId()); assertEquals(0.0, resposta.latitude());
        assertEquals(0.0, resposta.longitude()); assertEquals(perto.getDataHoraCriacao(), resposta.dataHoraCriacao());
        verify(usuarios, never()).findById(3L);
    }
    @Test void posicaoVencidaNaoObtemNomeOuCoordenadasDeAlertaNaoParticipante() {
        Usuario u = conta(2, "Teste", 0.0); u.setLocalizacaoAtualizadaEm(clock.instant().minusSeconds(900));
        when(usuarios.findById(2L)).thenReturn(Optional.of(u));
        when(alertas.findByAtivoTrueAndDataHoraCriacaoAfter(any())).thenReturn(List.of(alerta(1, 0.0)));
        assertTrue(service.buscarAlertasProximosComNome(2L).isEmpty());
        verify(usuarios, never()).findById(1L);
    }
    @Test void ajudanteConfirmadaMantemAcessoEPrimeiroNomeTrataEspacosUnicode() {
        Usuario u = conta(2, "Teste", 0.0); u.setLocalizacaoAtualizadaEm(null);
        when(usuarios.findById(2L)).thenReturn(Optional.of(u));
        when(usuarios.findById(1L)).thenReturn(Optional.of(conta(1, "\u00a0Érica\u00a0Silva\u00a0", 0.0)));
        Alerta a = alerta(1, 0.0); a.setUsuarioAjudandoId(2L);
        when(alertas.findByAtivoTrueAndDataHoraCriacaoAfter(any())).thenReturn(List.of(a));
        assertEquals("Érica", service.buscarAlertasProximosComNome(2L).get(0).primeiroNome());
    }
    @Test void nomeAusenteNaoInventaIdentidade() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta(2, "Teste", 0.0)));
        when(usuarios.findById(1L)).thenReturn(Optional.empty());
        when(alertas.findByAtivoTrueAndDataHoraCriacaoAfter(any())).thenReturn(List.of(alerta(1, 0.0)));
        assertNull(service.buscarAlertasProximosComNome(2L).get(0).primeiroNome());
    }
}

