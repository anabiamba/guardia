package br.com.guardia.service;

import java.time.Clock;
import br.com.guardia.config.LimitesPushTeste;
import br.com.guardia.repository.VinculoFcmRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Profile("teste-push & !teste")
public class NotificacaoTestePushService implements NotificacaoService {
    private final LimitesPushTeste limites;
    private final VinculoFcmRepository vinculos;
    private final FirebaseMessaging firebase;
    private final Clock clock;

    @Autowired
    public NotificacaoTestePushService(LimitesPushTeste limites, VinculoFcmRepository vinculos,
                                      FirebaseMessaging firebase) {
        this(limites, vinculos, firebase, Clock.systemUTC());
    }

    public NotificacaoTestePushService(LimitesPushTeste limites, VinculoFcmRepository vinculos,
                                      FirebaseMessaging firebase, Clock clock) {
        this.limites = limites;
        this.vinculos = vinculos;
        this.firebase = firebase;
        this.clock = clock;
    }

    @Override
    public void enviarAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId) {
        despacharAvisoSos(tokenFcm, alertaId, destinatariaId);
    }

    @Override
    public br.com.guardia.model.EventoPush.Modo modoFila() {
        return br.com.guardia.model.EventoPush.Modo.RESTRITO;
    }

    @Override
    public ResultadoPush despacharAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId) {
        return despacharAvisoSos(tokenFcm, alertaId, destinatariaId, java.time.Duration.ofMinutes(1));
    }

    @Override
    public ResultadoPush despacharAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId, java.time.Duration validade) {
        if (validade.isNegative() || validade.isZero()) return ResultadoPush.IGNORADO;
        if (destinatariaId == null || destinatariaId != limites.usuarioId() ||
                alertaId == null || alertaId <= 0 || tokenFcm == null || tokenFcm.isBlank()) return ResultadoPush.IGNORADO;
        if (vinculos.contarDestinoTesteAtivo(destinatariaId, limites.instalacaoHash(), tokenFcm,
                clock.instant()) != 1) return ResultadoPush.IGNORADO;
        var aviso = Message.builder().setToken(tokenFcm)
                .putData("tipo", "sos").putData("alertaId", alertaId.toString())
                .putData("destinatariaId", destinatariaId.toString())
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH)
                        .setTtl(Math.min(60_000, validade.toMillis())).build()).build();
        try {
            firebase.send(aviso);
        } catch (FirebaseMessagingException e) {
            throw FalhaPush.de(e, clock.instant());
        }
        return ResultadoPush.ACEITO_FCM;
    }

    @Override
    public String enviarNotificacao(String tokenFcm, String titulo, String mensagem) {
        throw new IllegalArgumentException("Envio genérico desabilitado no ensaio push.");
    }
}
