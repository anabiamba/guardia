package br.com.guardia.data.notification

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.guardia.data.session.CifraSessao
import br.com.guardia.data.session.SessaoAndroid
import br.com.guardia.data.session.SessoesAndroid
import br.com.guardia.data.session.aliasSessao
import com.google.firebase.messaging.RemoteMessage
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvisoSosInstrumentadoTest {
    @Test
    fun recebimentoSimuladoIsolaContasEDeduplicaPushEPolling() {
        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        val base = instrumentacao.targetContext
        check(NotificationHelper.podeNotificar(base)) {
            "Ative as notificações do Guardiã antes deste teste."
        }
        val nome = "aviso_sos_teste_${UUID.randomUUID()}"
        val pasta = File(base.cacheDir, nome).apply { check(mkdir()) }
        val contexto = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = pasta
            override fun getSharedPreferences(nomeOriginal: String, modo: Int) =
                base.getSharedPreferences(nome, modo)
        }
        // Isola preferências e fila sem adicionar pontos de entrada ao aplicativo.
        val construtor = SessoesAndroid::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }
        val sessoes = construtor.newInstance(contexto)
        val instancia = SessoesAndroid::class.java.getDeclaredField("instancia")
            .apply { isAccessible = true }
        val anterior = instancia.get(null)
        val servico = GuardiaFirebaseMessagingService()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
            .apply { isAccessible = true }.invoke(servico, contexto)
        val gerenciador = base.getSystemService(NotificationManager::class.java)
        val existentes = gerenciador.activeNotifications.map { it.key }.toSet()
        val a = SessaoAndroid(900001, "ficticio-a", System.currentTimeMillis() + 600000, "revogacao-ficticia-a")
        val b = SessaoAndroid(900002, "ficticio-b", System.currentTimeMillis() + 600000, "revogacao-ficticia-b")

        fun quantidade() = gerenciador.activeNotifications.count { it.key !in existentes }
        fun aguardarQuantidade(esperada: Int) {
            val limite = android.os.SystemClock.elapsedRealtime() + 3000
            while (quantidade() != esperada && android.os.SystemClock.elapsedRealtime() < limite) {
                Thread.sleep(25)
            }
            assertEquals(esperada, quantidade())
        }
        fun receber(conta: Long, alerta: Long) {
            servico.onMessageReceived(RemoteMessage(Bundle().apply {
                putString("from", "simulacao-local")
                putString("tipo", "sos")
                putString("destinatariaId", conta.toString())
                putString("alertaId", alerta.toString())
            }))
            instrumentacao.waitForIdleSync()
        }
        try {
            instancia.set(null, sessoes)
            sessoes.iniciar(a)
            receber(a.usuarioId, 900001)
            aguardarQuantidade(1)
            assertEquals("Primeiro SOS deve publicar um aviso", 1, quantidade())
            receber(a.usuarioId, 900001)
            NotificationHelper.mostrarAvisoSos(contexto, sessoes, a, 900001)
            assertEquals("Push e polling devem compartilhar deduplicação", 1, quantidade())
            sessoes.iniciar(b)
            receber(a.usuarioId, 900002)
            assertEquals("Mensagem da conta anterior deve ser ignorada", 1, quantidade())
            // O ID também pode aparecer para outra destinatária autorizada.
            Thread.sleep(50)
            receber(b.usuarioId, 900001)
            aguardarQuantidade(2)
            assertEquals("Conta atual pode receber o mesmo SOS", 2, quantidade())
            sessoes.invalidar(b)
            receber(b.usuarioId, 900003)
            assertEquals("Sessão descartada não deve receber avisos", 2, quantidade())
        } finally {
            instrumentacao.waitForIdleSync()
            instancia.set(null, anterior)
            gerenciador.activeNotifications.filter { it.key !in existentes }.forEach {
                gerenciador.cancel(it.tag, it.id)
            }
            CifraSessao.destruir(aliasSessao(a.idLocal))
            CifraSessao.destruir(aliasSessao(b.idLocal))
            base.getSharedPreferences(nome, Context.MODE_PRIVATE).edit().clear().commit()
            pasta.deleteRecursively()
        }
    }
}
