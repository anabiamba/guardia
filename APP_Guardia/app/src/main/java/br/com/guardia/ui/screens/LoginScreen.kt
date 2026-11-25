package br.com.guardia.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import br.com.guardia.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessoesAndroid.Saida
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    api: ApiAutenticada,
    sessaoEncerrada: Boolean,
    saida: Saida,
    onCadastroClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember {
        mutableStateOf(if (sessaoEncerrada && saida == Saida.NENHUMA) "Sua sessão terminou. Entre novamente." else "")
    }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(76.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Guardiã°", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Entrar na sua conta", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        CampoConta(
            value = email,
            onValueChange = {
                email = it
                mensagem = ""
            },
            rotulo = "E-mail",
            icone = R.drawable.ic_auth_email,
            enabled = !carregando && saida != Saida.BLOQUEADA,
            teclado = KeyboardType.Email
        )
        Spacer(Modifier.height(16.dp))
        CampoConta(
            value = senha,
            onValueChange = {
                senha = it
                mensagem = ""
            },
            rotulo = "Senha",
            icone = R.drawable.ic_auth_senha,
            enabled = !carregando && saida != Saida.BLOQUEADA,
            teclado = KeyboardType.Password
        )
        Spacer(Modifier.height(24.dp))
        when (saida) {
            Saida.CONCLUIDA -> Text("Você saiu da conta.")
            Saida.PENDENTE -> Text("Você saiu deste aparelho. A revogação será reenviada quando houver conexão.")
            Saida.FALHA_LOCAL -> Text("Você saiu deste aparelho. Não foi possível salvar a revogação.")
            Saida.BLOQUEADA -> Text("Falha ao proteger a saída neste aparelho. A autenticação está bloqueada nesta execução.")
            Saida.NENHUMA -> Unit
        }
        if (mensagem.isNotEmpty()) {
            Text(mensagem)
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                when {
                    email.isBlank() -> mensagem = "Informe seu e-mail."
                    senha.isBlank() -> mensagem = "Informe sua senha."
                    else -> {
                        carregando = true
                        scope.launch {
                            try {
                                mensagem = when (val status = api.login(email.trim(), senha)) {
                                    200 -> ""
                                    400 -> "Confira os dados informados."
                                    401 -> "E-mail ou senha inválidos."
                                    else -> "Erro ao realizar login. Código: $status"
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                mensagem = "Não foi possível iniciar a sessão."
                            } finally {
                                carregando = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            enabled = !carregando && saida != Saida.BLOQUEADA
        ) { Text(if (carregando) "Entrando…" else "Entrar") }

        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = {
                carregando = true
                mensagem = "Testando API..."
                scope.launch {
                    try {
                        val (status, corpo) = api.testarConexao()
                        mensagem = "Código: $status\n$corpo"
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        mensagem = "Não foi possível conectar à API."
                    } finally {
                        carregando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            enabled = !carregando && saida != Saida.BLOQUEADA
        ) { Text("TESTAR API") }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Não tem uma conta?")
            TextButton(onCadastroClick, enabled = !carregando && saida != Saida.BLOQUEADA) {
                Text("Cadastre-se")
            }
        }
    }
}
