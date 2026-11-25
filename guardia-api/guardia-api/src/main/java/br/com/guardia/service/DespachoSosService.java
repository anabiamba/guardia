package br.com.guardia.service;

import java.time.Clock;
import br.com.guardia.model.Alerta;
import br.com.guardia.model.EventoPush;
import br.com.guardia.model.EntregaPush;
import br.com.guardia.repository.EventoPushRepository;
import br.com.guardia.repository.EntregaPushRepository;
import br.com.guardia.repository.VinculoFcmRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespachoSosService {
    private final AlertaService alertas;
    private final EventoPushRepository eventos;
    private final EntregaPushRepository entregas;
    private final VinculoFcmRepository vinculos;
    private final NotificacaoService notificacoes;
    private final Clock clock;

    public DespachoSosService(AlertaService alertas, EventoPushRepository eventos, EntregaPushRepository entregas,
                              VinculoFcmRepository vinculos, NotificacaoService notificacoes,
                              @Qualifier("filaPushClock") Clock clock) {
        this.alertas = alertas;
        this.eventos = eventos;
        this.entregas = entregas;
        this.vinculos = vinculos;
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    @Transactional
    public Alerta criarAlerta(Long usuaria, Double latitude, Double longitude) {
        Alerta alerta = alertas.criarAlerta(usuaria, latitude, longitude);
        var agora = clock.instant();
        var prazo = alerta.getDataHoraCriacao().plusHours(1).atZone(clock.getZone()).toInstant();
        var evento = eventos.saveAndFlush(new EventoPush(alerta.getId(), notificacoes.modoFila(), agora, prazo));
        for (var proxima : alertas.buscarUsuariosProximas(alerta.getId())) {
            for (var vinculo : vinculos.buscarVinculosAtivos(proxima.getId(), agora)) {
                entregas.save(new EntregaPush(evento, proxima.getId(), vinculo.getInstalacaoHash(), vinculo.getSessaoHash(), agora));
            }
        }
        entregas.flush();
        return alerta;
    }
}
