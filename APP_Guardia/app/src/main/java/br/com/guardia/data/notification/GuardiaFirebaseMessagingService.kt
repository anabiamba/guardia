package br.com.guardia.data.notification

import android.os.Handler
import android.os.Looper
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessoesAndroid
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GuardiaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val sessoes = SessoesAndroid.obter(this)
        val sessao = sessoes.estado.value.sessao ?: return
        RegistroFcm.renovar(ApiAutenticada(sessoes), sessao)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val sessoes = SessoesAndroid.obter(this)
        val sessao = sessoes.estado.value.sessao ?: return
        Handler(Looper.getMainLooper()).post {
            if (!sessoes.vigente(sessao)) return@post
            if (remoteMessage.data["tipo"] == "sos") {
                val destinataria = remoteMessage.data["destinatariaId"]?.toLongOrNull()
                val alertaId = remoteMessage.data["alertaId"]?.toLongOrNull()
                if (destinataria == sessao.usuarioId && alertaId != null) {
                    NotificationHelper.mostrarAvisoSos(this, sessoes, sessao, alertaId)
                }
                return@post
            }
            NotificationHelper.mostrarNotificacao(
                context = this,
                titulo = remoteMessage.notification?.title ?: "Guardiã",
                mensagem = remoteMessage.notification?.body ?: "Você recebeu uma nova notificação."
            )
        }
    }
}
