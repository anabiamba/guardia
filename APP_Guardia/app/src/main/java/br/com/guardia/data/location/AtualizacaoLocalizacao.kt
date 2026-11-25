package br.com.guardia.data.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessaoAndroid
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

internal data class EstadoLocalizacao(val mensagem: String, val solicitarPermissao: () -> Unit)

@Composable
internal fun lembrarAtualizacaoLocalizacao(api: ApiAutenticada, sessao: SessaoAndroid, helper: LocationHelper): EstadoLocalizacao {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revisao by remember(sessao) { mutableIntStateOf(0) }
    var mensagem by remember(sessao) { mutableStateOf("Atualizando sua localização...") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Conceder permissão aqui nunca cria SOS.
        if (api.vigente(sessao)) revisao++
    }
    LaunchedEffect(sessao, lifecycle, revisao) {
        executarCicloLocalizacao(lifecycle) {
                if (!api.vigente(sessao)) return@executarCicloLocalizacao
                val permitida = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                if (!permitida) {
                    mensagem = "Permita a localização para encontrar alertas próximos."
                } else {
                    try {
                        val resultado = sincronizarLocalizacao(
                            obter = { helper.obterLocalizacao()?.let { Posicao(it.latitude, it.longitude) } },
                            vigente = { api.vigente(sessao) },
                            enviar = { p -> api.protegida(sessao, "/usuarios/me/localizacao", "PUT",
                                JSONObject().put("latitude", p.latitude).put("longitude", p.longitude)).first == 200 }
                        )
                        if (api.vigente(sessao)) mensagem = when (resultado) {
                            ResultadoLocalizacao.ATUALIZADA -> "Localização atualizada para buscar alertas próximos."
                            ResultadoLocalizacao.INDISPONIVEL -> "Não foi possível atualizar a localização. Verifique o GPS e a conexão."
                            ResultadoLocalizacao.DESCARTADA -> ""
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        if (api.vigente(sessao)) mensagem = "Não foi possível atualizar a localização. Verifique o GPS e a conexão."
                    }
                }
        }
    }
    return EstadoLocalizacao(mensagem) {
        if (api.vigente(sessao)) {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
