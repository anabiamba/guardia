package br.com.guardia.data.session

import kotlinx.coroutines.CancellationException

internal suspend fun entregarRevogacoes(
    ids: List<String>,
    ler: (String) -> String?,
    enviar: suspend (String) -> Int,
    confirmar: (String) -> Unit
): Boolean {
    var concluido = true
    for (id in ids) {
        try {
            val credencial = ler(id) ?: continue
            if (enviar(credencial) == 204) confirmar(id) else concluido = false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            concluido = false
        }
    }
    return concluido
}

internal enum class ResultadoDescarte { AGENDAVEL, SEM_FILA, FALHA_LOCAL }

internal fun descartarSessao(
    gravarRevogacao: () -> Unit,
    destruirChave: () -> Unit,
    removerSessao: () -> Boolean
): ResultadoDescarte {
    val filaGravada = runCatching(gravarRevogacao).isSuccess
    val chaveDestruida = runCatching(destruirChave).isSuccess
    val sessaoRemovida = runCatching(removerSessao).getOrDefault(false)
    return when {
        filaGravada -> ResultadoDescarte.AGENDAVEL
        chaveDestruida || sessaoRemovida -> ResultadoDescarte.SEM_FILA
        else -> ResultadoDescarte.FALHA_LOCAL
    }
}
