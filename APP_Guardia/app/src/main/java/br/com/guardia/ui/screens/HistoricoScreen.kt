package br.com.guardia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessaoAndroid
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Locale

private data class ItemHistorico(val id: Long, val criada: String, val encerrada: String?,
    val situacao: String, val ajuda: Boolean)

@Composable
fun HistoricoScreen(api: ApiAutenticada, sessao: SessaoAndroid, aoVoltar: () -> Unit) {
    var itens by remember(sessao) { mutableStateOf<List<ItemHistorico>>(emptyList()) }
    var carregando by remember(sessao) { mutableStateOf(true) }
    var erro by remember(sessao) { mutableStateOf(false) }
    var tentativa by remember { mutableIntStateOf(0) }
    BackHandler(onBack = aoVoltar)
    LaunchedEffect(sessao, tentativa) {
        carregando = true
        erro = false
        try {
            val (codigo, corpo) = api.protegida(sessao, "/alertas/me/historico")
            if (codigo != 200) error("Histórico indisponível")
            val json = JSONArray(corpo)
            val novos = (0 until json.length()).map { indice ->
                val item = json.getJSONObject(indice)
                ItemHistorico(item.getLong("id"), item.getString("criadaEm"),
                    if (item.isNull("encerradaEm")) null else item.getString("encerradaEm"),
                    item.getString("situacao"), item.getBoolean("ajudaRegistrada"))
            }
            if (api.vigente(sessao)) itens = novos
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (api.vigente(sessao)) erro = true
        } finally {
            if (api.vigente(sessao)) carregando = false
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        TextButton(onClick = aoVoltar) { Text("VOLTAR") }
        Text("Meu histórico", style = MaterialTheme.typography.headlineMedium)
        Text("Seus 50 SOS mais recentes.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        when {
            carregando -> CircularProgressIndicator()
            erro -> {
                Text("Não foi possível carregar o histórico. Verifique sua conexão.")
                Button(onClick = { tentativa++ }) { Text("TENTAR NOVAMENTE") }
            }
            itens.isEmpty() -> Text("Você ainda não criou nenhum SOS.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(itens, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Alerta SOS", style = MaterialTheme.typography.titleMedium)
                            Text("Criado em: " + dataHistorico(item.criada))
                            Text(when (item.situacao) {
                                "ATIVO" -> "Ativo"
                                "EXPIRADO" -> "Expirado"
                                else -> "Encerrado"
                            })
                            item.encerrada?.let { Text("Encerrado em: " + dataHistorico(it)) }
                            Text(if (item.ajuda) "Houve confirmação de ajuda." else "Sem confirmação de ajuda.")
                        }
                    }
                }
            }
        }
    }
}

internal fun dataHistorico(valor: String): String = runCatching {
    val entrada = valor.take(19)
    val leitor = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply { isLenient = false }
    val posicao = ParsePosition(0)
    val data = leitor.parse(entrada, posicao)
    require(data != null && posicao.index == entrada.length && entrada.length == 19)
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(data)
}.getOrDefault("Data indisponível")
