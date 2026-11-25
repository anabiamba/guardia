package br.com.guardia.data.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import br.com.guardia.MainActivity
import br.com.guardia.data.session.SessaoAndroid
import br.com.guardia.data.session.SessoesAndroid
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.guardia.R

object NotificationHelper {

    private const val CHANNEL_ID = "guardia_alertas"
    private const val CHANNEL_NAME = "Alertas do Guardiã"
    private const val CHANNEL_DESCRIPTION =
        "Notificações de alertas e pedidos de ajuda."

    /**
     * Cria o canal de notificações.
     *
     * NotificationChannel só existe a partir da API 26.
     */
    fun criarCanal(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            notificationManager.createNotificationChannel(canal)
        }
    }

    /**
     * Exibe uma notificação.
     */
    fun podeNotificar(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED)

    fun mostrarAvisoSos(context: Context, sessoes: SessoesAndroid, sessao: SessaoAndroid, alertaId: Long) {
        if (alertaId <= 0 || !podeNotificar(context) ||
            !sessoes.registrarNotificacao(sessao, "sos:$alertaId")
        ) return

        criarCanal(context)
        mostrarNotificacao(context, "Guardiã°", "Você tem um novo alerta no Guardiã°.")
    }

    fun mostrarNotificacao(
        context: Context,
        titulo: String,
        mensagem: String
    ) {

        // Android 13+ exige POST_NOTIFICATIONS.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificacao =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.drawable.ic_launcher_foreground
                )
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, 0,
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                System.currentTimeMillis().toInt(),
                notificacao
            )
    }
}