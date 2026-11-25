package br.com.guardia.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
@Profile("!teste & !teste-push")
public class FirebaseNotificationService implements NotificacaoService {

    @Override
    public void enviarAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId) {
        despacharAvisoSos(tokenFcm, alertaId, destinatariaId);
    }

    @Override
    public ResultadoPush despacharAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId) {
        return despacharAvisoSos(tokenFcm, alertaId, destinatariaId, java.time.Duration.ofMinutes(1));
    }

    @Override
    public ResultadoPush despacharAvisoSos(String tokenFcm, Long alertaId, Long destinatariaId, java.time.Duration validade) {
        if (validade.isNegative() || validade.isZero()) return ResultadoPush.IGNORADO;
        Message aviso = Message.builder().setToken(tokenFcm)
                .putData("tipo", "sos")
                .putData("alertaId", alertaId.toString())
                .putData("destinatariaId", destinatariaId.toString())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setTtl(Math.min(60_000, validade.toMillis()))
                        .build())
                .build();
        try {
            FirebaseMessaging.getInstance().send(aviso);
        } catch (FirebaseMessagingException e) {
            throw FalhaPush.de(e, java.time.Instant.now());
        }
        return ResultadoPush.ACEITO_FCM;
    }

    @Override
    public String enviarNotificacao(
            String tokenFcm,
            String titulo,
            String mensagem
    ) {

        try {

            Notification notification =
                    Notification.builder()
                            .setTitle(titulo)
                            .setBody(mensagem)
                            .build();

            Message message =
                    Message.builder()
                            .setToken(tokenFcm)
                            .setNotification(notification)
                            .build();

            String resposta =
                    FirebaseMessaging
                            .getInstance()
                            .send(message);

            System.out.println(
                    "Notificação FCM enviada com sucesso: "
                            + resposta
            );

            return resposta;

        } catch (Exception e) {

            System.err.println(
                    "Erro ao enviar notificação FCM: "
                            + e.getMessage()
            );

            throw new IllegalArgumentException(
                    "Não foi possível enviar a notificação FCM."
            );
        }
    }
}
