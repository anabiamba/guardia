package br.com.guardia.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import br.com.guardia.data.location.LocationHelper
import kotlinx.coroutines.CancellationException

/** Consulta local, sem criar alerta ou enviar coordenadas pela API. */
@Composable
internal fun MinhaLocalizacaoScreen(aoVoltar: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    // Instância própria: cancelar esta consulta não cancela SOS ou sincronização da Home.
    val helper = remember(context) { LocationHelper(context) }
    var ponto by remember { mutableStateOf<PontoMapa?>(null) }
    var mensagem by remember { mutableStateOf("Buscando sua localização atual...") }
    var buscando by remember { mutableStateOf(true) }
    var revisao by remember { mutableIntStateOf(0) }
    var erroExterno by remember { mutableStateOf<String?>(null) }
    fun permitida() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { revisao++ }
    val solicitar: () -> Unit = {
        if (!permitida()) launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        else revisao++
    }
    BackHandler(onBack = aoVoltar)
    DisposableEffect(helper) { onDispose { helper.cancelarPendentes() } }
    LaunchedEffect(Unit) {
        if (!permitida()) launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(owner, revisao) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            ponto = null
            erroExterno = null
            buscando = true
            mensagem = "Buscando sua localização atual..."
            try {
                val manager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                if (!permitida()) {
                    mensagem = "Permita a localização para mostrar sua posição no mapa."
                } else if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    mensagem = "A localização do aparelho está desligada. Ative-a e tente novamente."
                } else {
                    val posicao = helper.obterLocalizacao()
                    ponto = posicao?.let { PontoMapa.criar(it.latitude, it.longitude) }
                    if (ponto == null) mensagem = "Não foi possível obter uma posição recente compatível com o mapa. Verifique o GPS e tente novamente."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mensagem = "Não foi possível consultar sua localização. Verifique a permissão e o GPS."
            } finally {
                buscando = false
            }
        }
    }
    val atual = ponto
    if (atual != null) {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = solicitar, modifier = Modifier.safeDrawingPadding()) { Text("ATUALIZAR LOCALIZAÇÃO") }
            Box(Modifier.weight(1f)) {
                MapaAlertaScreen(atual.latitude, atual.longitude, aoVoltar = aoVoltar,
                    abrirExterno = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("geo:${atual.latitude},${atual.longitude}?q=${atual.latitude},${atual.longitude}")))
                            erroExterno = null
                        } catch (_: Exception) { erroExterno = "Nenhum aplicativo de mapas disponível." }
                    }, erroExterno = erroExterno,
                    titulo = "Minha localização",
                    descricao = "Localização obtida.",
                    textoVoltar = "VOLTAR AO INÍCIO", textoCentralizar = "MINHA POSIÇÃO")
            }
        }
    } else {
        EstadoMinhaLocalizacao(buscando, mensagem, aoVoltar, solicitar) {
            try { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            catch (_: Exception) { mensagem = "Abra as configurações do aparelho para ativar a localização." }
        }
    }
}

@Composable
internal fun EstadoMinhaLocalizacao(buscando: Boolean, mensagem: String,
    aoVoltar: () -> Unit, tentar: () -> Unit, abrirConfiguracoes: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = aoVoltar) { Text("VOLTAR AO INÍCIO") }
        Text("Minha localização", style = MaterialTheme.typography.headlineSmall)
        if (buscando) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(mensagem)
        if (!buscando) {
            Button(onClick = tentar) { Text("TENTAR NOVAMENTE") }
            OutlinedButton(onClick = abrirConfiguracoes) { Text("CONFIGURAÇÕES DE LOCALIZAÇÃO") }
        }
    }
}



