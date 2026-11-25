package br.com.guardia.service;

import java.time.*;
import java.util.*;
import br.com.guardia.model.*;
import br.com.guardia.repository.*;
import br.com.guardia.config.PasswordConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalizacaoValidadeTest {
    final Instant agora = Instant.parse("2026-11-22T15:00:00Z");
    final Clock clock = Clock.fixed(agora, ZoneOffset.UTC);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final AlertaRepository alertas = mock(AlertaRepository.class);
    final AlertaService service = new AlertaService(alertas, usuarios, clock);
    Usuario conta(long id, Instant instante) {
        var u = new Usuario(); u.setId(id); u.setLatitude(0.0); u.setLongitude(0.0);
        u.setLocalizacaoAtualizadaEm(instante); return u;
    }
    Alerta alerta() {
        var a = new Alerta(); a.setUsuarioId(1L); a.setLatitude(0.0); a.setLongitude(0.0);
        a.setDataHoraCriacao(LocalDateTime.now(clock));
        org.springframework.test.util.ReflectionTestUtils.setField(a, "id", 7L); return a;
    }
    @Test void limiteInclusivoQuinzeMinutosEFuturoNaoValem() {
        var regra = new ValidadeLocalizacao(clock, Duration.ofMinutes(15));
        assertFalse(regra.vigente(conta(2, null)));
        assertFalse(regra.vigente(conta(2, agora.plusNanos(1))));
        assertFalse(regra.vigente(conta(2, agora.minusSeconds(900))));
        assertTrue(regra.vigente(conta(2, agora.minusSeconds(900).plusNanos(1))));
        assertTrue(regra.vigente(conta(2, agora)));
        assertFalse(new ValidadeLocalizacao(clock, Duration.ofMinutes(1)).vigente(conta(2, agora.minusSeconds(60))));
        assertThrows(IllegalArgumentException.class, () -> new ValidadeLocalizacao(clock, Duration.ZERO));
    }
    @Test void filtraDestinatariasERejeitaNovoAceiteComPosicaoVencida() {
        var a = alerta(); var recente = conta(2, agora); var antiga = conta(3, agora.minusSeconds(900));
        var distante = conta(4, agora); distante.setLatitude(1.0);
        when(alertas.findById(7L)).thenReturn(Optional.of(a));
        when(usuarios.findAll()).thenReturn(List.of(conta(1, agora), recente, antiga, distante));
        assertEquals(List.of(2L), service.buscarUsuariosProximas(7L).stream().map(Usuario::getId).toList());
        when(alertas.bloquear(7L)).thenReturn(Optional.of(a));
        when(usuarios.findById(3L)).thenReturn(Optional.of(antiga));
        assertThrows(ResponseStatusException.class, () -> service.registrarAjudaComoProxima(7L, 3L));
        assertNull(a.getUsuarioAjudandoId());
    }
    @Test void posicaoVencidaNaoInterrompeParticipacaoConfirmada() {
        var a = alerta(); a.setUsuarioAjudandoId(2L);
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta(2, null)));
        when(alertas.findByAtivoTrueAndDataHoraCriacaoAfter(any())).thenReturn(List.of(a));
        when(alertas.bloquear(7L)).thenReturn(Optional.of(a));
        assertEquals(1, service.buscarAlertasProximos(2L).size());
        assertNotNull(service.exigirParticipanteDoChat(7L, 2L));
    }
    @Test void atualizacaoUsaRelogioServidorEBloqueiaContaSemAlterarSOS() {
        var u = conta(2, null); var a = alerta();
        when(usuarios.bloquearParaCriarAlerta(2L)).thenReturn(Optional.of(u));
        when(usuarios.save(any())).thenAnswer(c -> c.getArgument(0));
        var usuarioService = new UsuarioService(usuarios, new PasswordConfig().passwordEncoder(), clock);
        usuarioService.atualizarLocalizacao(2L, 1.0, 1.0);
        assertEquals(agora, u.getLocalizacaoAtualizadaEm());
        assertEquals(0.0, a.getLatitude());
        verify(usuarios).bloquearParaCriarAlerta(2L);
        verify(usuarios, never()).findById(any());
        assertThrows(IllegalArgumentException.class, () -> usuarioService.atualizarLocalizacao(2L, Double.NaN, 0.0));
    }
    @Test void filaCancelaEntregaQuandoPosicaoVenceAntesDoEnvio() {
        var a = alerta();
        when(alertas.findById(7L)).thenReturn(Optional.of(a));
        when(usuarios.findAll()).thenReturn(List.of(conta(2, agora.minusSeconds(900))));
        var evento = new EventoPush(7L, EventoPush.Modo.SIMULADO, agora, agora.plusSeconds(3600));
        var entrega = new EntregaPush(evento, 2L, "instalacao-ficticia", "sessao-ficticia", agora);
        org.springframework.test.util.ReflectionTestUtils.setField(entrega, "id", 1L);
        entrega.reservar(agora, Duration.ofMinutes(2), 5);
        var repo = mock(EntregaPushRepository.class);
        when(repo.bloquear(1L)).thenReturn(Optional.of(entrega));
        var vinculos = mock(VinculoFcmRepository.class);
        var fila = new FilaPushTransacoes(repo, vinculos, service, new NotificacaoSimuladaService(), clock, 5);
        assertNull(fila.preparar(new FilaPushTransacoes.Reserva(1L, entrega.getReserva())));
        assertEquals(EntregaPush.Estado.CANCELADO, entrega.getEstado());
    }
}
