package br.com.guardia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import br.com.guardia.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessaoAndroid
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

@Composable
fun PerfilScreen(api: ApiAutenticada, sessao: SessaoAndroid, aoVoltar: () -> Unit, aoSair: () -> Unit) {
    var trocarSenha by remember(sessao) { mutableStateOf(false) }
    if (trocarSenha) {
        TrocarSenhaScreen(api, sessao, aoVoltar = { trocarSenha = false })
        return
    }
    var perfil by remember(sessao) { mutableStateOf<Pair<String, String>?>(null) }
    var carregando by remember(sessao) { mutableStateOf(true) }
    var erro by remember(sessao) { mutableStateOf(false) }
    var tentativa by remember(sessao) { mutableIntStateOf(0) }
    BackHandler(onBack = aoVoltar)
    LaunchedEffect(sessao, tentativa) {
        carregando = true
        erro = false
        try {
            val (codigo, corpo) = api.protegida(sessao, "/usuarios/me")
            if (codigo != 200) error("Perfil indisponível")
            val json = JSONObject(corpo)
            val dados = json.getString("nome") to json.getString("email")
            if (api.vigente(sessao)) perfil = dados
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (api.vigente(sessao)) erro = true
        } finally {
            if (api.vigente(sessao)) carregando = false
        }
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = aoVoltar, modifier = Modifier.align(Alignment.Start)) { Text("VOLTAR") }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(R.drawable.ic_auth_pessoa), null,
                Modifier.padding(24.dp).size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text("Meu perfil", style = MaterialTheme.typography.headlineMedium)
        if (!carregando && !erro) perfil?.first?.let { nome ->
            Text(nome, style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        }
        when {
            carregando -> {
                CircularProgressIndicator(Modifier.size(32.dp))
                Text("Carregando seus dados...", style = MaterialTheme.typography.bodyMedium)
            }
            erro -> {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Não foi possível carregar seu perfil. Verifique sua conexão.",
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        OutlinedButton(onClick = { tentativa++ }) { Text("TENTAR NOVAMENTE") }
                    }
                }
            }
            else -> perfil?.let { (nome, email) ->
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        PerfilDado(R.drawable.ic_auth_pessoa, "Nome", nome)
                        HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer)
                        PerfilDado(R.drawable.ic_auth_email, "E-mail", email)
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_auth_senha), null, Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column(Modifier.weight(1f)) {
                    Text("Acesso à conta", style = MaterialTheme.typography.titleSmall)
                    Text("Você pode alterar sua senha abaixo.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Button(onClick = { trocarSenha = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("TROCAR SENHA")
        }
        OutlinedButton(onClick = aoSair, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)) {
            Text("SAIR DA CONTA")
        }
    }
}

@Composable
private fun PerfilDado(icone: Int, rotulo: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(icone), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
