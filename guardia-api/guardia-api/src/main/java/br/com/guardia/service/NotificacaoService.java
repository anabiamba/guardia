package br.com.guardia.service;

public interface NotificacaoService {
    enum ResultadoPush { ACEITO_FCM, SIMULADO, IGNORADO }

    default br.com.guardia.model.EventoPush.Modo modoFila() {
        return br.com.guardia.model.EventoPush.Modo.REAL;
    }

    default ResultadoPush despacharAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId) {
        enviarAvisoSos(tokenFcm, alertaId, destinatariaId);
        return ResultadoPush.ACEITO_FCM;
    }

    default ResultadoPush despacharAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId, java.time.Duration validade) {
        return despacharAvisoSos(tokenFcm, alertaId, destinatariaId);
    }

    void enviarAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId);

    String enviarNotificacao(String tokenFcm, String titulo, String mensagem);
}
