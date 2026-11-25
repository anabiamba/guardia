package br.com.guardia.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.guardia.R

@Composable
fun AlertaRecebidoScreen(
    nomeUsuaria: String,
    latitude: Double,
    longitude: Double,
    onAjudarClick: () -> Unit,
    onVoltarClick: () -> Unit,
    ajudaRegistrada: Boolean = false,
    onChatClick: (() -> Unit)? = null,
    alertaId: Long? = null,
    confirmandoAjuda: Boolean = false,
    mensagem: String = "",
    consultaIndisponivel: Boolean = false,
    mapa: @Composable (Modifier) -> Unit = { MapaResumoSos(latitude, longitude, it) },
    endereco: @Composable () -> Unit = { EnderecoAproximado(alertaId, latitude, longitude) }
) {
    var mostrarMapa by remember(alertaId, latitude, longitude) { mutableStateOf(false) }
    val context = LocalContext.current
    var erroMapa by remember(alertaId, latitude, longitude) { mutableStateOf<String?>(null) }
    val coordenadasValidas = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
    val abrirExterno: () -> Unit = {
        erroMapa = null
        val ponto = "$latitude,$longitude"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$ponto?q=$ponto")))
        } catch (_: ActivityNotFoundException) {
            erroMapa = "Nenhum aplicativo de mapas disponível neste dispositivo."
        } catch (_: SecurityException) {
            erroMapa = "Não foi possível abrir o aplicativo de mapas."
        }
    }
    if (mostrarMapa) {
        MapaAlertaScreen(latitude, longitude, aoVoltar = { mostrarMapa = false },
            abrirExterno = abrirExterno, erroExterno = erroMapa)
        return
    }
    BackHandler(onBack = onVoltarClick)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onVoltarClick, modifier = Modifier.align(Alignment.Start)) { Text("VOLTAR") }
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(R.drawable.ic_home_sirene), null,
                Modifier.padding(18.dp).size(52.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(if (ajudaRegistrada) "Ajuda confirmada" else "Alerta próximo",
            style = MaterialTheme.typography.headlineSmall)
        Text(if (ajudaRegistrada) "Você confirmou ajuda a esta usuária."
            else "Uma usuária precisa de ajuda perto de você.", textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge)
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.ic_home_local), null, Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Localização do SOS", style = MaterialTheme.typography.titleSmall)
                        if (coordenadasValidas) endereco()
                        else Text("Localização indisponível.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (coordenadasValidas) {
                    key(alertaId, latitude, longitude) {
                        mapa(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)))
                    }
                    Text("OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors",
                        style = MaterialTheme.typography.labelSmall)
                    Text("O provedor do mapa recebe requisições da região visualizada.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.ic_auth_pessoa), null, Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(nomeUsuaria.ifBlank { "Solicitante do alerta" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (consultaIndisponivel) Text("Não foi possível atualizar este alerta. Verifique sua conexão.",
            color = MaterialTheme.colorScheme.error)
        if (mensagem.isNotBlank()) Text(mensagem, textAlign = TextAlign.Center)
        if (ajudaRegistrada) {
            if (onChatClick != null) Button(onClick = onChatClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("ABRIR CHAT")
            }
        } else {
            Button(onClick = onAjudarClick, enabled = !confirmandoAjuda,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (confirmandoAjuda) "CONFIRMANDO..." else "CONFIRMAR AJUDA")
            }
        }
        OutlinedButton(onClick = { mostrarMapa = true }, enabled = coordenadasValidas,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))) {
            Text("VER DETALHES")
        }
    }
}

internal fun rotuloSolicitante(valor: String?): String {
    val primeiro = valor?.trim { it.isWhitespace() }
        ?.split(Regex("[\\s\\p{Z}]+"))?.firstOrNull()
        ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
    return primeiro?.let { "$it (solicitante)" } ?: "Solicitante do alerta"
}
