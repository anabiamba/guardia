package br.com.guardia.data.notification

import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessaoAndroid
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object RegistroFcm {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val registros = Mutex()

    suspend fun obterTokenAtual(api: ApiAutenticada, sessao: SessaoAndroid) = registros.withLock {
        if (!api.vigente(sessao)) return@withLock
        val token = suspendCancellableCoroutine { continuacao ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tarefa ->
                continuacao.resume(if (tarefa.isSuccessful) tarefa.result else null)
            }
        }
        if (token != null) registrar(api, sessao, token)
    }

    fun renovar(api: ApiAutenticada, sessao: SessaoAndroid) {
        scope.launch { obterTokenAtual(api, sessao) }
    }

    private suspend fun registrar(api: ApiAutenticada, sessao: SessaoAndroid, token: String) {
        try {
            api.registrarFcm(sessao, token)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // O login e a Home continuam disponíveis se o registro do FCM falhar.
        }
    }
}
