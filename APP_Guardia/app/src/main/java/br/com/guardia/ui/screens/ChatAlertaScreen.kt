package br.com.guardia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.com.guardia.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessaoAndroid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class MensagemChat(val id: Long, val remetenteId: Long, val texto: String, val enviadaEm: String) {
    companion object {
        fun ler(json: JSONObject) = MensagemChat(
            json.getLong("id"), json.getLong("remetenteId"), json.getString("texto"), json.optString("enviadaEm", "")
        )
    }
}

@Composable
internal fun ChatAlertaScreen(
    api: ApiAutenticada,
    sessao: SessaoAndroid,
    alertaId: Long,
    nomeAjudante: String?,
    aoVoltar: () -> Unit
) {
    val rota = "/alertas/$alertaId/mensagens"
    var mensagens by remember { mutableStateOf(emptyList<MensagemChat>()) }
    var cursor by remember { mutableLongStateOf(0L) }
    var texto by remember { mutableStateOf("") }
    var erroLeitura by remember { mutableStateOf(false) }
    var erroEnvio by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var disponivel by remember { mutableStateOf(false) }
    var encerrado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = aoVoltar)
    LaunchedEffect(alertaId) {
        while (!encerrado) {
            val quantidade = try {
                val (codigo, corpo) = api.protegida(sessao, "$rota?depois=$cursor")
                when (codigo) {
                    200 -> {
                        val itens = JSONArray(corpo)
                        val novas = List(itens.length()) { MensagemChat.ler(itens.getJSONObject(it)) }
                        mensagens = (mensagens + novas).distinctBy { it.id }.sortedBy { it.id }
                        cursor = novas.lastOrNull()?.id ?: cursor
                        erroLeitura = false
                        disponivel = true
                        novas.size
                    }
                    404 -> {
                        encerrado = true
                        0
                    }
                    else -> {
                        erroLeitura = true
                        0
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                erroLeitura = true
                0
            }
            if (!encerrado && quantidade < 50) delay(3000)
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = aoVoltar) { Text("Voltar") }
            Text("Chat do alerta", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        if (encerrado) {
            Text("O chat deste alerta já não está disponível.")
            return@Column
        }
        if (erroLeitura) Text("Não foi possível atualizar a conversa. Tentando novamente.")
        if (erroEnvio) Text("O envio não foi confirmado. Confira a conversa antes de tentar novamente.")
        if (!disponivel && !erroLeitura) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (disponivel && mensagens.isEmpty()) Text("Converse com a outra participante do alerta.",
            modifier = Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            reverseLayout = true
        ) {
            items(mensagens.asReversed(), key = { it.id }) { mensagem ->
                val minha = mensagem.remetenteId == sessao.usuarioId
                val autora = if (minha) "Você"
                    else nomeAjudante?.takeIf { it.isNotBlank() } ?: "Participante"
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = if (minha) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom) {
                    if (!minha) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(painterResource(R.drawable.ic_auth_pessoa), null, Modifier.padding(7.dp).size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Surface(
                        modifier = Modifier.widthIn(max = 300.dp).weight(1f, fill = false),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp,
                            bottomEnd = if (minha) 4.dp else 18.dp, bottomStart = if (minha) 18.dp else 4.dp),
                        color = if (minha) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (minha) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(autora, style = MaterialTheme.typography.labelMedium)
                            Text(mensagem.texto, style = MaterialTheme.typography.bodyLarge)
                            val horario = horarioMensagemChat(mensagem.enviadaEm)
                            if (horario != null) Text(horario, modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { if (it.length <= 500) texto = it },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(26.dp),
            placeholder = { Text("Digite uma mensagem...") },
            label = { Text("Mensagem") },
            enabled = disponivel && !enviando,
            maxLines = 3
        )
        FilledIconButton(enabled = disponivel && !enviando && texto.isNotBlank(),
            modifier = Modifier.size(52.dp).semantics { contentDescription = "Enviar mensagem" }, onClick = {
            enviando = true
            erroEnvio = false
            scope.launch {
                try {
                    val (codigo, corpo) = api.protegida(
                        sessao, rota, "POST", JSONObject().put("texto", texto.trim())
                    )
                    when (codigo) {
                        201 -> {
                            val enviada = MensagemChat.ler(JSONObject(corpo))
                            mensagens = (mensagens + enviada).distinctBy { it.id }.sortedBy { it.id }
                            texto = ""
                        }
                        404 -> encerrado = true
                        else -> erroEnvio = true
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    erroEnvio = true
                } finally {
                    enviando = false
                }
            }
        }) {
            if (enviando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Icon(painterResource(R.drawable.ic_chat_enviar), null, Modifier.size(25.dp))
        }
        }
    }
}

