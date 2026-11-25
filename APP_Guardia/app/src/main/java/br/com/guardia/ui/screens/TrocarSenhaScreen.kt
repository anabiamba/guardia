package br.com.guardia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.guardia.R
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessaoAndroid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun TrocarSenhaScreen(api: ApiAutenticada, sessao: SessaoAndroid, aoVoltar: () -> Unit) {
    var atual by remember(sessao) { mutableStateOf("") }
    var nova by remember(sessao) { mutableStateOf("") }
    var confirmacao by remember(sessao) { mutableStateOf("") }
    var enviando by remember(sessao) { mutableStateOf(false) }
    var mensagem by remember(sessao) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    BackHandler { if (!enviando) aoVoltar() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = aoVoltar, enabled = !enviando) { Text("VOLTAR") }
        Text("Trocar senha", style = MaterialTheme.typography.headlineMedium)
        Text("Após a troca, todas as sessões serão encerradas. Entre novamente com a nova senha.")
        CampoConta(atual, { atual = it }, "Senha atual", R.drawable.ic_auth_senha, !enviando, KeyboardType.Password)
        CampoConta(nova, { nova = it }, "Nova senha", R.drawable.ic_auth_senha, !enviando, KeyboardType.Password)
        CampoConta(confirmacao, { confirmacao = it }, "Confirmar nova senha", R.drawable.ic_auth_senha, !enviando, KeyboardType.Password)
        Text(DESCRICAO_SENHA)
        if (mensagem.isNotEmpty()) Text(mensagem)
        Button(enabled = !enviando, modifier = Modifier.fillMaxWidth(), onClick = {
            val erro = validarTrocaSenha(atual, nova, confirmacao)
            if (erro != null) { mensagem = erro; return@Button }
            enviando = true
            mensagem = ""
            scope.launch {
                try {
                    val codigo = api.alterarSenha(sessao, atual, nova)
                    atual = ""; nova = ""; confirmacao = ""
                    if (api.vigente(sessao)) mensagem = when (codigo) {
                        400 -> "Confira a senha atual e use uma nova senha diferente, dentro dos limites informados."
                        else -> "Não foi possível alterar a senha. Tente novamente."
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (api.vigente(sessao)) mensagem = "Não foi possível confirmar a troca. Se sua sessão terminar, tente entrar com a nova senha."
                } finally {
                    atual = ""; nova = ""; confirmacao = ""
                    if (api.vigente(sessao)) enviando = false
                }
            }
        }) { Text(if (enviando) "ALTERANDO..." else "CONFIRMAR TROCA") }
    }
}

internal fun validarTrocaSenha(atual: String, nova: String, confirmacao: String): String? = when {
    atual.isBlank() -> "Informe a senha atual."
    erroNovaSenha(nova) != null -> erroNovaSenha(nova)
    atual.toByteArray(Charsets.UTF_8).size > 72 -> "Senha atual inválida."
    nova != confirmacao -> "A confirmação não corresponde à nova senha."
    nova == atual -> "Escolha uma senha diferente da atual."
    else -> null
}
