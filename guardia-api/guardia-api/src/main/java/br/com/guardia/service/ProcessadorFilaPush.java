package br.com.guardia.service;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
@ConditionalOnProperty(name = "guardia.push.fila.habilitada", havingValue = "true", matchIfMissing = true)
public class ProcessadorFilaPush {
    private static final Logger log = LoggerFactory.getLogger(ProcessadorFilaPush.class);
    private final FilaPushTransacoes fila;
    private final NotificacaoService notificacoes;
    private final Executor executor;
    private final AtomicBoolean executando = new AtomicBoolean();

    public ProcessadorFilaPush(FilaPushTransacoes fila, NotificacaoService notificacoes) {
        this(fila, notificacoes, Runnable::run);
    }

    @Autowired
    public ProcessadorFilaPush(FilaPushTransacoes fila, NotificacaoService notificacoes,
                              @Qualifier("applicationTaskExecutor") Executor executor) {
        this.fila = fila;
        this.notificacoes = notificacoes;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${guardia.push.intervalo-ms:5000}", initialDelay = 1000)
    public void agendar() {
        if (!executando.compareAndSet(false, true)) return;
        try {
            executor.execute(() -> {
                try {
                    processar();
                } finally {
                    executando.set(false);
                }
            });
        } catch (RuntimeException e) {
            executando.set(false);
            log.warn("Executor push indisponível; trabalhos permanecem na fila persistente.");
        }
    }

    public synchronized void processar() {
        try {
            for (var reserva : fila.reservar()) processar(reserva);
        } catch (RuntimeException e) {
            log.warn("Processamento da fila push indisponível; trabalhos persistidos serão recuperados.");
        }
    }

    private void processar(FilaPushTransacoes.Reserva reserva) {
        try {
            var preparado = fila.preparar(reserva);
            if (preparado == null) return;
            NotificacaoService.ResultadoPush resultado;
            try {
                resultado = notificacoes.despacharAvisoSos(preparado.token(), preparado.alertaId(), preparado.usuariaId(), preparado.validade());
            } catch (FalhaPush e) {
                fila.falhar(reserva, preparado.token(), e);
                return;
            } catch (RuntimeException e) {
                fila.falhar(reserva, preparado.token(), new FalhaPush(FalhaPush.Tipo.TRANSITORIA, Duration.ofSeconds(60)));
                return;
            }
            fila.concluir(reserva, resultado);
        } catch (RuntimeException e) {
            // Falha local antes/depois da chamada externa mantém a reserva recuperável.
            log.warn("Trabalho push aguardando recuperação da reserva.");
        }
    }
}
