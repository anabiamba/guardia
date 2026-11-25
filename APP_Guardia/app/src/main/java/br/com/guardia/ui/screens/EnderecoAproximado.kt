package br.com.guardia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import br.com.guardia.data.location.GeocoderEndereco
import br.com.guardia.data.location.coordenadasValidas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

@Composable
internal fun EnderecoAproximado(alertaId: Long?, latitude: Double, longitude: Double,
    buscar: (suspend (Double, Double) -> String?)? = null) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val geocoder = remember(context) { GeocoderEndereco(context) }
    val buscaAtual by rememberUpdatedState(buscar)
    var tentativa by remember(alertaId, latitude, longitude) { mutableIntStateOf(0) }
    var carregando by remember(alertaId, latitude, longitude, tentativa) { mutableStateOf(true) }
    var endereco by remember(alertaId, latitude, longitude, tentativa) { mutableStateOf<String?>(null) }
    LaunchedEffect(alertaId, latitude, longitude, owner, tentativa) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!carregando) return@repeatOnLifecycle
            val consulta = buscaAtual
            val resultado = try {
                if (!coordenadasValidas(latitude, longitude)) null
                else if (consulta != null) consulta(latitude, longitude)
                else geocoder.buscar(latitude, longitude)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
            coroutineContext.ensureActive()
            endereco = resultado
            carregando = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Endereço aproximado", style = MaterialTheme.typography.titleSmall)
        Text(when {
            carregando -> "Buscando endereço..."
            endereco != null -> endereco!!
            else -> "Endereço indisponível. Consulte o mapa."
        }, style = MaterialTheme.typography.bodySmall)
        if (!carregando && endereco == null) TextButton(onClick = { tentativa++ }) { Text("TENTAR ENDEREÇO NOVAMENTE") }
        Text("A consulta de endereço pode enviar as coordenadas ao serviço do aparelho.",
            style = MaterialTheme.typography.labelSmall)
    }
}


