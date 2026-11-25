package br.com.guardia.ui.screens

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
internal fun AlertaAtivoScreen(modelo: ModeloAlertaAtivo, nomeAjudante: String,
    ajudaConfirmada: Boolean, encerrando: Boolean, consultaIndisponivel: Boolean,
    mensagem: String, aoVoltar: () -> Unit, aoEncerrar: () -> Unit, aoChat: () -> Unit,
    mapa: @Composable (Modifier) -> Unit = { modifier -> MapaResumoSos(modelo.latitude, modelo.longitude, modifier) }
) {
    val owner = LocalLifecycleOwner.current
    var agora by remember(modelo.id) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(owner, modelo.id) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { agora = SystemClock.elapsedRealtime(); delay(1000) }
        }
    }
    val segundos = segundosAlerta(modelo, agora)
    BackHandler(onBack = aoVoltar)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
        .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = aoVoltar) { Text("VOLTAR") }
            Text("Alerta ativo", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleLarge)
        }
        key(modelo.id, modelo.latitude, modelo.longitude) {
            mapa(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp)))
        }
        Text("OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall)
        Text("Ao abrir o mapa, o provedor recebe requisições da região visualizada.",
            style = MaterialTheme.typography.bodySmall)
        Text("Você solicitou ajuda", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(if (modelo.criadaEm.isBlank()) "Horário de início indisponível"
            else "Iniciado às "+horarioInicioAlerta(modelo.criadaEm))
        Text("Raio de 1 km")
        Text(when {
            consultaIndisponivel -> "Não foi possível atualizar o alerta. Exibindo o último estado confirmado."
            segundos == null -> "Não foi possível consultar o tempo restante."
            segundos == 0L -> "Prazo atingido. Aguardando confirmação do servidor."
            else -> "Tempo restante: "+((segundos + 59) / 60)+" min"
        })
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ajudaConfirmada) "Ajuda confirmada" else "Aguardando ajuda",
                    style = MaterialTheme.typography.titleMedium)
                Text(if (ajudaConfirmada) nomeAjudante.takeIf { it.isNotBlank() }?.let { "$it aceitou ajudar você." }
                    ?: "Uma pessoa confirmou ajuda."
                    else "Quando alguém confirmar ajuda, você poderá conversar pelo chat.")
                if (ajudaConfirmada) Button(onClick = aoChat) { Text("ABRIR CHAT") }
            }
        }
        if (mensagem.isNotBlank()) Text(mensagem, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = aoEncerrar, enabled = !encerrando, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
            Text(if (encerrando) "ENCERRANDO..." else "ENCERRAR ALERTA")
        }
    }
}

