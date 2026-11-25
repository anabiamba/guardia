package br.com.guardia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("teste")
public class NotificacaoSimuladaService implements NotificacaoService {
    @Override
    public br.com.guardia.model.EventoPush.Modo modoFila() {
        return br.com.guardia.model.EventoPush.Modo.SIMULADO;
    }

    @Override
    public ResultadoPush despacharAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId) {
        enviarAvisoSos(tokenFcm, alertaId, destinatariaId);
        return ResultadoPush.SIMULADO;
    }

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoSimuladaService.class);

    @Override
    public void enviarAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId) {
        LOG.info("Aviso SOS simulado no perfil teste.");
    }

    @Override
    public String enviarNotificacao(String tokenFcm, String titulo, String mensagem) {
        return "envio-simulado";
    }
}
