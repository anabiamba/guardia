package br.com.guardia.data.session

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.guardia.data.remote.ApiAutenticada
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RevogacaoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessoes = SessoesAndroid.obter(applicationContext)
        val api = ApiAutenticada(sessoes)
        val concluido = entregarRevogacoes(
            sessoes.fila.ids(), sessoes.fila::ler, api::revogarPendente
        ) { id ->
            sessoes.fila.concluir(id)
            sessoes.confirmarSaida(id)
        }
        if (concluido) Result.success() else Result.retry()
    }

    companion object {
        private val rede = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        internal fun agendar(context: Context) {
            val tarefa = OneTimeWorkRequestBuilder<RevogacaoWorker>()
                .setConstraints(rede)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("guardia-revogacao", ExistingWorkPolicy.APPEND_OR_REPLACE, tarefa)
        }

        internal fun recuperar(context: Context) {
            val tarefa = PeriodicWorkRequestBuilder<RevogacaoWorker>(15, TimeUnit.MINUTES)
                .setConstraints(rede)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "guardia-revogacao-recuperacao", ExistingPeriodicWorkPolicy.KEEP, tarefa
            )
            if (SessoesAndroid.obter(context).fila.ids().isNotEmpty()) agendar(context)
        }
    }
}
