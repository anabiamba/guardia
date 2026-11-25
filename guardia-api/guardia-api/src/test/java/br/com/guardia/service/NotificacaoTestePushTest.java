package br.com.guardia.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import br.com.guardia.config.LimitesPushTeste;
import br.com.guardia.repository.VinculoFcmRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacaoTestePushTest {
    @Test
    void springCriaServicoSemExigirBeanClock() {
        try (var contexto = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            contexto.getEnvironment().setActiveProfiles("teste-push");
            contexto.registerBean(LimitesPushTeste.class,
                    () -> new LimitesPushTeste(2, "a".repeat(64), "guardia-ficticio"));
            contexto.registerBean(VinculoFcmRepository.class, () -> mock(VinculoFcmRepository.class));
            contexto.registerBean(FirebaseMessaging.class, () -> mock(FirebaseMessaging.class));
            contexto.register(NotificacaoTestePushService.class);
            contexto.refresh();
            assertNotNull(contexto.getBean(NotificacaoTestePushService.class));
            assertTrue(contexto.getBeansOfType(Clock.class).isEmpty());
        }
    }

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
    private final String instalacao = "a".repeat(64);
    private final VinculoFcmRepository vinculos = mock(VinculoFcmRepository.class);
    private final FirebaseMessaging firebase = mock(FirebaseMessaging.class);
    private final NotificacaoTestePushService servico = new NotificacaoTestePushService(
            new LimitesPushTeste(2, instalacao, "guardia-ficticio"), vinculos, firebase, clock);

    @Test
    void enviaSomenteParaVinculoAutorizadoEVigente() throws Exception {
        when(vinculos.contarDestinoTesteAtivo(2L, instalacao, "ficticio", clock.instant())).thenReturn(1L);
        servico.enviarAvisoSos("ficticio", 7L, 2L);
        verify(firebase).send(any(Message.class));
        verifyNoMoreInteractions(firebase);
    }

    @Test
    void bloqueiaOutraContaAntesDeConsultarVinculo() {
        servico.enviarAvisoSos("ficticio", 7L, 3L);
        verifyNoInteractions(vinculos, firebase);
    }

    @Test
    void bloqueiaOutraInstalacaoTokenAntigoOuSessaoInativa() {
        // A consulta exige conta, instalação, token, sessão não revogada e prazo vigente.
        servico.enviarAvisoSos("ficticio-antigo", 7L, 2L);
        verify(vinculos).contarDestinoTesteAtivo(2L, instalacao, "ficticio-antigo", clock.instant());
        verifyNoInteractions(firebase);
    }

    @Test
    void bloqueiaEnvioGenericoEIdentificadoresInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> servico.enviarNotificacao("ficticio", "Teste", "Teste"));
        servico.enviarAvisoSos("ficticio", -1L, 2L);
        servico.enviarAvisoSos("", 7L, 2L);
        verifyNoInteractions(vinculos, firebase);
    }

    @Test
    void limitesInvalidosFalhamFechados() {
        assertThrows(IllegalStateException.class, () -> new LimitesPushTeste(0, instalacao, "guardia-ficticio"));
        assertThrows(IllegalStateException.class, () -> new LimitesPushTeste(2, "", "guardia-ficticio"));
        assertThrows(IllegalStateException.class, () -> new LimitesPushTeste(2, instalacao, ""));
    }
}
