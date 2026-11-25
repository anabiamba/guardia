package br.com.guardia.service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import br.com.guardia.model.EntregaPush;
import br.com.guardia.model.EntregaPush.Estado;
import br.com.guardia.repository.EntregaPushRepository;
import br.com.guardia.repository.VinculoFcmRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilaPushTransacoes {
    private final EntregaPushRepository entregas;
    private final VinculoFcmRepository vinculos;
    private final AlertaService alertas;
    private final NotificacaoService notificacoes;
    private final Clock clock;
    private final int maximo;

    public FilaPushTransacoes(EntregaPushRepository entregas, VinculoFcmRepository vinculos,
                             AlertaService alertas, NotificacaoService notificacoes,
                             @Qualifier("filaPushClock") Clock clock,
                             @Value("${guardia.push.max-tentativas:5}") int maximo) {
        if (maximo < 1 || maximo > 10) throw new IllegalArgumentException("Limite de tentativas push inválido.");
        this.entregas = entregas;
        this.vinculos = vinculos;
        this.alertas = alertas;
        this.notificacoes = notificacoes;
        this.clock = clock;
        this.maximo = maximo;
    }

    @Transactional
    public List<Reserva> reservar() {
        var agora = clock.instant();
        var resultado = new ArrayList<Reserva>();
        for (var entrega : entregas.reservarPendentes(notificacoes.modoFila().name(), agora, 5)) {
            if (entrega.reservar(agora, Duration.ofMinutes(2), maximo)) {
                resultado.add(new Reserva(entrega.getId(), entrega.getReserva()));
            }
        }
        return resultado;
    }

    @Transactional
    public Preparado preparar(Reserva reserva) {
        var entrega = entregas.bloquear(reserva.id()).orElse(null);
        var agora = clock.instant();
        if (entrega == null || !entrega.pertenceA(reserva.identificador(), agora)) return null;
        if (!agora.isBefore(entrega.getEvento().getExpiraEm())) {
            entrega.concluir(Estado.CANCELADO);
            return null;
        }
        boolean proxima;
        try {
            proxima = alertas.buscarUsuariosProximas(entrega.getEvento().getAlertaId()).stream()
                    .anyMatch(u -> entrega.getUsuarioId().equals(u.getId()));
        } catch (IllegalArgumentException e) {
            proxima = false;
        }
        var vinculo = vinculos.buscarDestinoVigente(entrega.getSessaoHash(), entrega.getInstalacaoHash(),
                entrega.getUsuarioId(), agora);
        if (!proxima || vinculo.isEmpty()) {
            entrega.concluir(Estado.CANCELADO);
            return null;
        }
        return new Preparado(vinculo.get().getToken(), entrega.getEvento().getAlertaId(), entrega.getUsuarioId(),
                Duration.between(agora, entrega.getEvento().getExpiraEm()));
    }

    @Transactional
    public void concluir(Reserva reserva, NotificacaoService.ResultadoPush resultado) {
        var entrega = entregas.bloquear(reserva.id()).orElse(null);
        if (entrega == null || !entrega.pertenceA(reserva.identificador(), clock.instant())) return;
        Estado estado = switch (resultado) {
            case ACEITO_FCM -> Estado.ACEITO_FCM;
            case SIMULADO -> Estado.SIMULADO;
            case IGNORADO -> Estado.CANCELADO;
        };
        entrega.concluir(estado);
    }

    @Transactional
    public void falhar(Reserva reserva, String tokenTentado, FalhaPush falha) {
        var entrega = entregas.bloquear(reserva.id()).orElse(null);
        var agora = clock.instant();
        if (entrega == null || !entrega.pertenceA(reserva.identificador(), agora)) return;
        if (falha.getTipo() == FalhaPush.Tipo.TOKEN_INVALIDO) {
            vinculos.limparTokenInvalido(entrega.getSessaoHash(), entrega.getInstalacaoHash(), tokenTentado);
            var atual = vinculos.findBySessaoHash(entrega.getSessaoHash());
            if (atual.isPresent() && atual.get().getToken() != null && !tokenTentado.equals(atual.get().getToken())) {
                entrega.repetir(agora.plusSeconds(60), maximo);
            } else {
                entrega.concluir(Estado.CANCELADO);
            }
        } else if (falha.getTipo() == FalhaPush.Tipo.DEFINITIVA) {
            entrega.concluir(Estado.FALHA_FINAL);
        } else {
            long segundos = Math.min(600, 60L << Math.min(entrega.getTentativas() - 1, 4));
            Duration espera = Duration.ofSeconds(segundos + ThreadLocalRandom.current().nextLong(16));
            if (falha.getEsperaMinima().compareTo(espera) > 0) espera = falha.getEsperaMinima();
            if (espera.compareTo(Duration.ofHours(1)) > 0) entrega.concluir(Estado.FALHA_FINAL);
            else entrega.repetir(agora.plus(espera), maximo);
        }
    }

    public record Reserva(Long id, String identificador) {}

    public record Preparado(String token, Long alertaId, Long usuariaId, Duration validade) {
        @Override public String toString() { return "Preparado[credencial omitida]"; }
    }
}
