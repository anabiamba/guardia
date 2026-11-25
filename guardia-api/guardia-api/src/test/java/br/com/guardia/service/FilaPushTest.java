package br.com.guardia.service;

import java.time.*;
import java.util.List;
import java.util.Optional;
import br.com.guardia.model.*;
import br.com.guardia.repository.*;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilaPushTest {
    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");
    private final Clock clock = mock(Clock.class);
    private final EntregaPushRepository entregas = mock(EntregaPushRepository.class);
    private final VinculoFcmRepository vinculos = mock(VinculoFcmRepository.class);
    private final AlertaService alertas = mock(AlertaService.class);
    private final NotificacaoService notificacoes = spy(new NotificacaoSimuladaService());
    private final EventoPush evento = new EventoPush(7L, EventoPush.Modo.SIMULADO, AGORA, AGORA.plusSeconds(3600));
    private final EntregaPush entrega = new EntregaPush(evento, 2L, "i".repeat(64), "s".repeat(64), AGORA);
    private final FilaPushTransacoes fila = new FilaPushTransacoes(entregas, vinculos, alertas, notificacoes, clock, 5);

    FilaPushTest() {
        when(clock.instant()).thenReturn(AGORA);
        ReflectionTestUtils.setField(entrega, "id", 1L);
        when(entregas.reservarPendentes(eq("SIMULADO"), any(), eq(5))).thenReturn(List.of(entrega));
        when(entregas.bloquear(1L)).thenReturn(Optional.of(entrega));
        var usuaria = new Usuario();
        usuaria.setId(2L);
        when(alertas.buscarUsuariosProximas(7L)).thenReturn(List.of(usuaria));
        var vinculo = mock(VinculoFcm.class);
        when(vinculo.getToken()).thenReturn("fcm-ficticio");
        when(vinculos.buscarDestinoVigente(eq("s".repeat(64)), eq("i".repeat(64)), eq(2L), any()))
                .thenReturn(Optional.of(vinculo));
    }

    @Test
    void publicaSimuladoComEstadoDistintoDoAceiteFcm() {
        new ProcessadorFilaPush(fila, notificacoes).processar();
        assertEquals(EntregaPush.Estado.SIMULADO, entrega.getEstado());
        assertEquals(1, entrega.getTentativas());
    }

    @Test
    void falhaTemporariaAgendaNovaTentativaComEsperaProgressiva() {
        var reserva = fila.reservar().get(0);
        fila.falhar(reserva, "fcm-ficticio", new FalhaPush(FalhaPush.Tipo.TRANSITORIA, Duration.ofSeconds(180)));
        assertEquals(EntregaPush.Estado.PENDENTE, entrega.getEstado());
        assertEquals(AGORA.plusSeconds(180), entrega.getProximaTentativa());
        assertFalse(entrega.reservar(AGORA.plusSeconds(179), Duration.ofMinutes(2), 5));
        when(clock.instant()).thenReturn(AGORA.plusSeconds(180));
        var nova = fila.reservar().get(0);
        assertNotEquals(reserva.identificador(), nova.identificador());
        assertEquals(2, entrega.getTentativas());
    }

    @Test
    void reservaVencidaPodeSerRecuperadaEWorkerAntigoNaoConclui() {
        var antiga = fila.reservar().get(0);
        assertFalse(entrega.reservar(AGORA.plusSeconds(119), Duration.ofMinutes(2), 5));
        when(clock.instant()).thenReturn(AGORA.plusSeconds(120));
        var atual = fila.reservar().get(0);
        fila.concluir(antiga, NotificacaoService.ResultadoPush.ACEITO_FCM);
        assertEquals(EntregaPush.Estado.PROCESSANDO, entrega.getEstado());
        fila.concluir(atual, NotificacaoService.ResultadoPush.ACEITO_FCM);
        assertEquals(EntregaPush.Estado.ACEITO_FCM, entrega.getEstado());
    }

    @Test
    void maximoDeTentativasEncerraTrabalho() {
        for (int i = 0; i < 5; i++) {
            var reserva = fila.reservar().get(0);
            fila.falhar(reserva, "ficticio", new FalhaPush(FalhaPush.Tipo.TRANSITORIA, Duration.ZERO));
            when(clock.instant()).thenReturn(entrega.getProximaTentativa());
        }
        assertEquals(EntregaPush.Estado.FALHA_FINAL, entrega.getEstado());
    }

    @Test
    void eventoExpiradoCancelaReserva() {
        when(clock.instant()).thenReturn(AGORA.plusSeconds(3600));
        assertTrue(fila.reservar().isEmpty());
        assertEquals(EntregaPush.Estado.CANCELADO, entrega.getEstado());
    }

    @Test
    void encerramentoDuranteReservaCancelaSemEnviar() {
        var reserva = fila.reservar().get(0);
        when(alertas.buscarUsuariosProximas(7L)).thenThrow(new IllegalArgumentException());
        assertNull(fila.preparar(reserva));
        assertEquals(EntregaPush.Estado.CANCELADO, entrega.getEstado());
        verify(notificacoes, never()).enviarAvisoSos(any(), any(), any());
    }

    @Test
    void distanciaForaDoRaioCancelaSemEnviar() {
        var reserva = fila.reservar().get(0);
        when(alertas.buscarUsuariosProximas(7L)).thenReturn(List.of());
        assertNull(fila.preparar(reserva));
        assertEquals(EntregaPush.Estado.CANCELADO, entrega.getEstado());
    }

    @Test
    void sessaoRevogadaExpiradaOuInstalacaoTransferidaCancelaDestino() {
        var reserva = fila.reservar().get(0);
        when(vinculos.buscarDestinoVigente(any(), any(), any(), any())).thenReturn(Optional.empty());
        assertNull(fila.preparar(reserva));
        assertEquals(EntregaPush.Estado.CANCELADO, entrega.getEstado());
    }

    @Test
    void tokenRotacionadoEConsultadoNaHoraDaTentativa() {
        var reserva = fila.reservar().get(0);
        var vinculo = mock(VinculoFcm.class);
        when(vinculo.getToken()).thenReturn("fcm-ficticio-novo");
        when(vinculos.buscarDestinoVigente(any(), any(), any(), any())).thenReturn(Optional.of(vinculo));
        assertTrue(fila.preparar(reserva).token().equals("fcm-ficticio-novo"));
    }

    @Test
    void tokenInvalidoLimpaSomenteValorTentado() {
        var reserva = fila.reservar().get(0);
        fila.falhar(reserva, "fcm-ficticio-antigo", new FalhaPush(FalhaPush.Tipo.TOKEN_INVALIDO, Duration.ZERO));
        verify(vinculos).limparTokenInvalido("s".repeat(64), "i".repeat(64), "fcm-ficticio-antigo");
        assertEquals(EntregaPush.Estado.CANCELADO, entrega.getEstado());
    }

    @Test
    void falhaDefinitivaNaoTentaNovamente() {
        var reserva = fila.reservar().get(0);
        fila.falhar(reserva, "ficticio", new FalhaPush(FalhaPush.Tipo.DEFINITIVA, Duration.ZERO));
        assertEquals(EntregaPush.Estado.FALHA_FINAL, entrega.getEstado());
        verify(vinculos, never()).limparTokenInvalido(any(), any(), any());
    }

    @Test
    void destinoIgnoradoNaoRecebeEstadoAceito() {
        var reserva = fila.reservar().get(0);
        fila.concluir(reserva, NotificacaoService.ResultadoPush.IGNORADO);
        assertEquals(EntregaPush.Estado.CANCELADO, entrega.getEstado());
    }

    @Test
    void filaSelecionaSomenteModoDaExecucao() {
        fila.reservar();
        verify(entregas).reservarPendentes("SIMULADO", AGORA, 5);
    }

    @Test
    void classificaErrosFcmSemIncluirMensagemDoProvedor() {
        var erro = mock(FirebaseMessagingException.class);
        when(erro.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        assertEquals(FalhaPush.Tipo.TOKEN_INVALIDO, FalhaPush.de(erro, AGORA).getTipo());
        when(erro.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        assertEquals(FalhaPush.Tipo.TRANSITORIA, FalhaPush.de(erro, AGORA).getTipo());
        when(erro.getMessagingErrorCode()).thenReturn(MessagingErrorCode.SENDER_ID_MISMATCH);
        assertEquals(FalhaPush.Tipo.DEFINITIVA, FalhaPush.de(erro, AGORA).getTipo());
        verify(erro, never()).getMessage();
    }

    @Test
    void respeitaRetryAfterEmSegundosOuDataHttp() {
        var erro = mock(FirebaseMessagingException.class);
        var resposta = mock(com.google.firebase.IncomingHttpResponse.class);
        when(erro.getMessagingErrorCode()).thenReturn(MessagingErrorCode.QUOTA_EXCEEDED);
        when(erro.getHttpResponse()).thenReturn(resposta);
        when(resposta.getHeaders()).thenReturn(java.util.Map.of("Retry-After", List.of("180")));
        assertEquals(Duration.ofSeconds(180), FalhaPush.de(erro, AGORA).getEsperaMinima());
        when(resposta.getHeaders()).thenReturn(java.util.Map.of("retry-after",
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(AGORA.plusSeconds(300).atZone(ZoneOffset.UTC))));
        assertEquals(Duration.ofSeconds(300), FalhaPush.de(erro, AGORA).getEsperaMinima());
    }

    @Test
    void erroDeTransporteSemCodigoFcmAindaPodeSerRepetido() {
        var erro = mock(FirebaseMessagingException.class);
        when(erro.getErrorCode()).thenReturn(com.google.firebase.ErrorCode.UNAVAILABLE);
        assertEquals(FalhaPush.Tipo.TRANSITORIA, FalhaPush.de(erro, AGORA).getTipo());
    }

    @Test
    void rotacaoDuranteErroDeTokenInvalidoPreservaNovoTokenEAgendaTentativa() {
        var reserva = fila.reservar().get(0);
        var novo = mock(VinculoFcm.class);
        when(novo.getToken()).thenReturn("fcm-ficticio-novo");
        when(vinculos.findBySessaoHash("s".repeat(64))).thenReturn(Optional.of(novo));
        fila.falhar(reserva, "fcm-ficticio-antigo", new FalhaPush(FalhaPush.Tipo.TOKEN_INVALIDO, Duration.ZERO));
        assertEquals(EntregaPush.Estado.PENDENTE, entrega.getEstado());
        assertEquals(AGORA.plusSeconds(60), entrega.getProximaTentativa());
    }

    @Test
    void envioSemConfirmacaoLocalERepetidoAposReservaVencer() {
        when(entregas.bloquear(1L)).thenReturn(Optional.of(entrega))
                .thenThrow(new IllegalStateException("Falha fictícia ao confirmar"))
                .thenReturn(Optional.of(entrega));
        var worker = new ProcessadorFilaPush(fila, notificacoes);
        worker.processar();
        assertEquals(EntregaPush.Estado.PROCESSANDO, entrega.getEstado());
        when(clock.instant()).thenReturn(AGORA.plusSeconds(120));
        worker.processar();
        verify(notificacoes, times(2)).enviarAvisoSos("fcm-ficticio", 7L, 2L);
        assertEquals(EntregaPush.Estado.SIMULADO, entrega.getEstado());
    }

    @Test
    void executorIndisponivelMantemTrabalhoParaProximaRodada() {
        var executor = mock(java.util.concurrent.Executor.class);
        doThrow(new java.util.concurrent.RejectedExecutionException()).doAnswer(chamada -> {
            ((Runnable) chamada.getArgument(0)).run();
            return null;
        }).when(executor).execute(any());
        var worker = new ProcessadorFilaPush(fila, notificacoes, executor);
        worker.agendar();
        assertEquals(EntregaPush.Estado.PENDENTE, entrega.getEstado());
        worker.agendar();
        assertEquals(EntregaPush.Estado.SIMULADO, entrega.getEstado());
    }
}
