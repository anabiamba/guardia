package br.com.guardia.ui.screens

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.guardia.data.location.lembrarAtualizacaoLocalizacao
import kotlinx.coroutines.Job
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import br.com.guardia.R
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.guardia.data.location.LocationHelper
import br.com.guardia.data.notification.NotificationHelper
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.SessaoAndroid
import br.com.guardia.data.session.SessoesAndroid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun HomeScreen(
    api: ApiAutenticada,
    sessoes: SessoesAndroid,
    sessao: SessaoAndroid,
    onSair: () -> Unit
) {

    val context = LocalContext.current
    var chatAlertaId by remember { mutableStateOf<Long?>(null) }
    var mostrarHistorico by remember(sessao) { mutableStateOf(false) }
    var mostrarPerfil by remember(sessao) { mutableStateOf(false) }
    var mostrarMinhaLocalizacao by remember(sessao) { mutableStateOf(false) }

    var mensagem by remember {
        mutableStateOf("")
    }

    var carregando by remember {
        mutableStateOf(false)
    }

    var alertasProximos by remember {
        mutableStateOf<List<JSONObject>>(emptyList())
    }

    var mostrarAlertaRecebido by remember {
        mutableStateOf(false)
    }

    var estadoAjuda by remember { mutableStateOf(EstadoAlertaRecebido()) }
    var ajudaEmEnvioId by remember(sessao) { mutableStateOf<Long?>(null) }
    var consultaIndisponivel by remember { mutableStateOf(false) }
    var statusIndisponivel by remember { mutableStateOf(false) }

    // Nome da pessoa que aceitou ajudar.
    var nomeUsuarioAjudando by remember {
        mutableStateOf("")
    }

    // Controla se a confirmação de ajuda
    // deve aparecer para a criadora do SOS.
    var mostrarConfirmacaoAjuda by remember {
        mutableStateOf(false)
    }

    // ID do alerta criado pela própria usuária.
    var meuAlertaId by remember {
        mutableStateOf<Long?>(null)
    }

    var detalhesMeuAlerta by remember(sessao) { mutableStateOf<ModeloAlertaAtivo?>(null) }
    var mostrarMeuAlerta by remember(sessao) { mutableStateOf(false) }

    // Indica se a usuária possui um alerta ativo.
    var meuAlertaAtivo by remember {
        mutableStateOf(false)
    }

    // Evita múltiplos cliques durante o encerramento.
    var encerrandoAlerta by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()

    val retomada by sessoes.retomada.collectAsState()

    val locationHelper = remember {
        LocationHelper(context)
    }

    lembrarAtualizacaoLocalizacao(api, sessao, locationHelper)
    var operacaoSos by remember(sessao) { mutableStateOf<Job?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, locationHelper, sessao) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) {
                operacaoSos?.cancel()
                locationHelper.cancelarPendentes()
                if (api.vigente(sessao)) carregando = false
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            operacaoSos?.cancel()
            locationHelper.cancelarPendentes()
        }
    }

    // ============================================
    // CONSULTA AUTOMÁTICA DE ALERTAS PRÓXIMOS
    // ============================================

    LaunchedEffect(sessao, retomada) {

        if (api.vigente(sessao)) {

            while (true) {

                try {

                    val resultado =
                        api.protegida(sessao, "/alertas/proximos")

                    if (resultado.first == 200) {

                        val jsonArray =
                            JSONArray(resultado.second)

                        val lista =
                            mutableListOf<JSONObject>()

                        for (i in 0 until jsonArray.length()) {

                            lista.add(
                                jsonArray.getJSONObject(i)
                            )
                        }

                        consultaIndisponivel = false
                        alertasProximos = lista

                        if (lista.isNotEmpty()) {

                            mostrarAlertaRecebido = true

                            val alerta = lista.first()
                            val alertaId = alerta.optLong("id", 0)

                            NotificationHelper.mostrarAvisoSos(context, sessoes, sessao, alertaId)

                        }
                    } else {
                        consultaIndisponivel = true
                    }

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    if (!api.vigente(sessao)) throw CancellationException("Sessão indisponível.")
                    consultaIndisponivel = true
                }

                delay(10000)
            }
        }
    }

    // ============================================
    // CONSULTA AUTOMÁTICA DO STATUS DO SOS
    // ============================================

    LaunchedEffect(sessao, retomada) {

        if (api.vigente(sessao)) {

            while (true) {

                try {

                    val consultaIniciadaEm = android.os.SystemClock.elapsedRealtime()
                    val resultado =
                        api.protegida(sessao, "/alertas/me/status")

                    if (resultado.first == 200) {

                        val json =
                            JSONObject(resultado.second)

                        statusIndisponivel = false
                        // Guarda o ID do alerta ativo.
                        val alertaId =
                            json.optLong("id", 0)

                        if (alertaId != 0L) {
                            if (meuAlertaId != alertaId) mostrarMeuAlerta = true
                            meuAlertaId = alertaId
                            detalhesMeuAlerta = ModeloAlertaAtivo(
                                alertaId, json.optDouble("latitude", Double.NaN),
                                json.optDouble("longitude", Double.NaN),
                                json.optString("criadaEm", ""),
                                json.optLong("segundosRestantes", -1L).takeIf { it in 0L..3600L },
                                consultaIniciadaEm)
                        }

                        meuAlertaAtivo = true

                        val ajudanteId =
                            json.optLong(
                                "usuarioAjudandoId",
                                0
                            )

                        val nomeAjudante =
                            json.optString(
                                "nomeUsuarioAjudando",
                                ""
                            )

                        mostrarConfirmacaoAjuda = ajudanteId != 0L
                        if (
                            ajudanteId != 0L &&
                            nomeAjudante.isNotEmpty()
                        ) {

                            nomeUsuarioAjudando =
                                nomeAjudante

                            mostrarConfirmacaoAjuda =
                                true

                            mensagem =
                                "🤝 $nomeAjudante aceitou ajudar você."

                            // Notifica somente uma vez
                            // para cada pessoa que aceitar ajudar.
                            if (
                                alertaId != 0L &&
                                NotificationHelper.podeNotificar(context) && sessoes.registrarNotificacao(sessao, "ajuda:$alertaId:$ajudanteId")
                            ) {

                                NotificationHelper
                                    .mostrarNotificacao(
                                        context = context,
                                        titulo =
                                            "🤝 Ajuda confirmada",
                                        mensagem =
                                            "$nomeAjudante aceitou ajudar você."
                                    )
                            }

                        }

                    } else if (resultado.first == 404) {

                        statusIndisponivel = false
                        // Não há alerta ativo.
                        meuAlertaAtivo = false
                        meuAlertaId = null
                        detalhesMeuAlerta = null
                        mostrarMeuAlerta = false

                        mostrarConfirmacaoAjuda =
                            false

                        nomeUsuarioAjudando = ""

                    } else {
                        statusIndisponivel = true
                    }

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    if (!api.vigente(sessao)) throw CancellationException("Sessão indisponível.")
                    statusIndisponivel = true
                }

                delay(10000)
            }
        }
    }

    // ============================================
    // SE HOUVER ALERTA RECEBIDO, MOSTRA A TELA
    // ============================================

    if (mostrarMinhaLocalizacao) {
        MinhaLocalizacaoScreen(aoVoltar = { mostrarMinhaLocalizacao = false })
        return
    }

    if (mostrarPerfil) {
        PerfilScreen(api, sessao, aoVoltar = { mostrarPerfil = false }, aoSair = onSair)
        return
    }

    if (mostrarHistorico) {
        HistoricoScreen(api, sessao, aoVoltar = { mostrarHistorico = false })
        return
    }

    chatAlertaId?.let { id ->
        androidx.compose.runtime.key(id) {
            ChatAlertaScreen(
                api, sessao, id,
                nomeAjudante = nomeUsuarioAjudando.takeIf { id == meuAlertaId },
                aoVoltar = { chatAlertaId = null }
            )
        }
        return
    }

    if (
        mostrarAlertaRecebido &&
        alertasProximos.isNotEmpty()
    ) {

        val alerta =
            alertasProximos.first()

        val alertaId =
            alerta.optLong("id")

        val latitude =
            alerta.optDouble("latitude")

        val longitude =
            alerta.optDouble("longitude")

        AlertaRecebidoScreen(
            alertaId = alertaId,
            nomeUsuaria = rotuloSolicitante(alerta.optString("primeiroNome", "")),
            confirmandoAjuda = ajudaEmEnvioId == alertaId,
            consultaIndisponivel = consultaIndisponivel,
            mensagem = mensagem,
            latitude = latitude,
            longitude = longitude,
            ajudaRegistrada = estadoAjuda.confirmadoPara(alertaId) ||
                alerta.optLong("usuarioAjudandoId") == sessao.usuarioId,
            onChatClick = { chatAlertaId = alertaId },

            onAjudarClick = ajudar@ {
                if (ajudaEmEnvioId != null) return@ajudar
                ajudaEmEnvioId = alertaId

                coroutineScope.launch {

                    try {

                        estadoAjuda = estadoAjuda.limpar(alertaId)

                        mensagem =
                            "Registrando ajuda..."

                        val resultado =
                            api.protegida(sessao, "/alertas/$alertaId/ajudar", "POST")

                        if (resultado.first in 200..299) {

                            estadoAjuda = estadoAjuda.confirmar(alertaId)

                            mensagem =
                                "✅ Ajuda registrada com sucesso."

                        } else if (resultado.first == 404) {
                            alertasProximos = alertasProximos.filterNot { it.optLong("id") == alertaId }
                            mensagem = "Este alerta não está mais disponível."
                        } else {

                            mensagem =
                                "Não foi possível registrar a ajuda.\n" +
                                        "Código: ${resultado.first}\n" +
                                        "Tente novamente."
                        }

                    } catch (e: CancellationException) {

                        throw e

                    } catch (e: Exception) {

                        if (!api.vigente(sessao)) throw CancellationException("Sessão indisponível.")

                        mensagem =
                            "Erro ao registrar ajuda: " +
                                    "${e::class.simpleName}"
                    } finally {
                        if (api.vigente(sessao) && ajudaEmEnvioId == alertaId) ajudaEmEnvioId = null
                    }
                }
            },

            onVoltarClick = {

                mostrarAlertaRecebido = false
                estadoAjuda = estadoAjuda.limpar(alertaId)
            }
        )

        return
    }

    // ============================================
    // PERMISSÃO DE LOCALIZAÇÃO
    // ============================================

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (!api.vigente(sessao)) return@rememberLauncherForActivityResult

            val permissaoFine =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val permissaoCoarse =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true

            if (permissaoFine || permissaoCoarse) {

                operacaoSos = verificarLocalizacaoEExecutar(
                    context = context,
                    locationHelper = locationHelper,
                    api = api,
                    sessao = sessao,
                    coroutineScope = coroutineScope,
                    atualizarMensagem = {
                        mensagem = it
                    },
                    atualizarCarregando = {
                        carregando = it
                    }
                )

            } else {

                mensagem =
                    "Permissão de localização negada."
            }
        }

    val encerrarMeuAlerta: () -> Unit = encerrar@ {
        if (encerrandoAlerta) return@encerrar
        val alertaId = meuAlertaId ?: return@encerrar
        encerrandoAlerta = true
        mensagem = "Encerrando alerta..."
        coroutineScope.launch {
            try {
                val resultado = api.protegida(sessao, "/alertas/$alertaId/encerrar", "PUT")
                if (!api.vigente(sessao) || meuAlertaId != alertaId) return@launch
                if (resultado.first in 200..299) {
                    detalhesMeuAlerta = null
                    mostrarMeuAlerta = false
                    meuAlertaAtivo = false
                    meuAlertaId = null
                    mostrarConfirmacaoAjuda = false
                    nomeUsuarioAjudando = ""
                    mensagem = "✅ Alerta encerrado com sucesso."
                } else {
                    mensagem = "Não foi possível encerrar o alerta. Código: "+resultado.first
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (!api.vigente(sessao)) throw CancellationException("Sessão indisponível.")
                mensagem = "Não foi possível confirmar o encerramento. Verifique a conexão."
            } finally {
                if (api.vigente(sessao)) encerrandoAlerta = false
            }
        }
    }
    if (mostrarMeuAlerta && meuAlertaAtivo && meuAlertaId != null) {
        AlertaAtivoScreen(
            modelo = detalhesMeuAlerta ?: ModeloAlertaAtivo(meuAlertaId!!),
            nomeAjudante = nomeUsuarioAjudando,
            ajudaConfirmada = mostrarConfirmacaoAjuda,
            encerrando = encerrandoAlerta,
            consultaIndisponivel = statusIndisponivel,
            mensagem = mensagem,
            aoVoltar = { mostrarMeuAlerta = false },
            aoEncerrar = encerrarMeuAlerta,
            aoChat = { meuAlertaId?.let { chatAlertaId = it } }
        )
        return
    }

    // ============================================
    // HOME
    // ============================================

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(48.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Olá!", style = MaterialTheme.typography.headlineSmall)
                Text("Estamos aqui para apoiar você.", style = MaterialTheme.typography.bodySmall)
            }
            Text("Guardiã°", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(if (meuAlertaAtivo) "Seu alerta está ativo" else "Como você está?",
            style = MaterialTheme.typography.titleMedium)

        if (meuAlertaAtivo && meuAlertaId != null) {
            Button(onClick = { mostrarMeuAlerta = true }, modifier = Modifier.fillMaxWidth()) {
                Text("VER MEU ALERTA")
            }
        }

        // ============================================
        // BOTÃO SOS
        // ============================================

        Button(
            onClick = {

                val permissaoFine =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                val permissaoCoarse =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                if (permissaoFine || permissaoCoarse) {

                    operacaoSos = verificarLocalizacaoEExecutar(
                        context = context,
                        locationHelper = locationHelper,
                        api = api,
                        sessao = sessao,
                        coroutineScope = coroutineScope,
                        atualizarMensagem = {
                            mensagem = it
                        },
                        atualizarCarregando = {
                            carregando = it
                        }
                    )

                } else {

                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.size(200.dp),
            contentPadding = PaddingValues(0.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !carregando && !meuAlertaAtivo
        ) {

            Column(
                modifier = Modifier.fillMaxSize().background(
                    if (!carregando && !meuAlertaAtivo) Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.error))
                    else Brush.verticalGradient(listOf(
                        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(painterResource(R.drawable.ic_home_sirene), null, Modifier.size(46.dp))
                Text(if (carregando) "Enviando…" else "SOS", style = MaterialTheme.typography.headlineLarge)
                Text(if (meuAlertaAtivo) "Alerta em andamento" else "Pedir ajuda",
                    style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text =
                if (meuAlertaAtivo) {
                    "Finalize seu alerta antes de criar outro."
                } else {
                    "Toque para acionar um alerta"
                }
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ============================================
        // BOTÃO MANUAL
        // ============================================

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeAtalho(
            onClick = {

                carregando = true

                mensagem =
                    "Procurando alertas próximos..."

                coroutineScope.launch {

                    try {

                        val resultado =
                            api.protegida(sessao, "/alertas/proximos")

                        if (resultado.first == 200) {

                            val alertas =
                                JSONArray(resultado.second)

                            val quantidade =
                                alertas.length()

                            if (quantidade == 0) {

                                mensagem =
                                    "Nenhum alerta encontrado " +
                                            "em até 1 km."

                            } else {

                                val lista =
                                    mutableListOf<JSONObject>()

                                for (i in 0 until quantidade) {

                                    lista.add(
                                        alertas.getJSONObject(i)
                                    )
                                }

                                alertasProximos =
                                    lista

                                mostrarAlertaRecebido =
                                    true

                                mensagem =
                                    "Alerta encontrado."
                            }

                        } else {

                            mensagem =
                                "Não foi possível consultar os alertas.\n" +
                                        "Código: ${resultado.first}"
                        }

                    } catch (e: CancellationException) {

                        throw e

                    } catch (e: Exception) {

                        if (!api.vigente(sessao)) throw CancellationException("Sessão indisponível.")

                        mensagem =
                            "Erro: ${e::class.simpleName}\n" +
                                    e.message

                    } finally {

                        if (api.vigente(sessao)) carregando = false
                    }
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !carregando
        ) {

            HomeRotulo(R.drawable.ic_home_alerta, "Alertas próximos")
        }
        HomeAtalho(onClick = { mostrarHistorico = true }, modifier = Modifier.weight(1f)) {
            HomeRotulo(R.drawable.ic_home_relogio, "Histórico")
        }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeAtalho(onClick = { mostrarPerfil = true }, modifier = Modifier.weight(1f)) {
                HomeRotulo(R.drawable.ic_auth_pessoa, "Meu perfil")
            }
            HomeAtalho(onClick = { mostrarMinhaLocalizacao = true }, modifier = Modifier.weight(1f)) {
                HomeRotulo(R.drawable.ic_home_local, "Minha localização")
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // ============================================
        // MENSAGEM
        // ============================================

        TextButton(onClick = onSair, modifier = Modifier.fillMaxWidth()) {
            Text("SAIR DA CONTA")
        }

        if (statusIndisponivel || consultaIndisponivel) {
            Text("Não foi possível atualizar o estado dos alertas.")
        }
        if (mensagem.isNotEmpty()) {

            Text(
                text = mensagem
            )
        }
    }
}

// ============================================
// LOCALIZAÇÃO E ENVIO DO SOS
// ============================================

private fun verificarLocalizacaoEExecutar(
    context: Context,
    locationHelper: LocationHelper,
    api: ApiAutenticada,
    sessao: SessaoAndroid,
    coroutineScope: CoroutineScope,
    atualizarMensagem: (String) -> Unit,
    atualizarCarregando: (Boolean) -> Unit
): Job? {
    if (!api.vigente(sessao)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        atualizarMensagem("A localização do dispositivo está desativada. Ative-a para usar o alerta.")
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        return null
    }
    atualizarMensagem("Obtendo sua localização...")
    atualizarCarregando(true)
    return coroutineScope.launch {
        try {
            val location = locationHelper.obterLocalizacao()
            if (!api.vigente(sessao)) return@launch
            if (location == null) {
                atualizarMensagem("Não foi possível obter uma localização recente. Verifique o GPS e tente novamente.")
                return@launch
            }
            val coordenadas = JSONObject().put("latitude", location.latitude).put("longitude", location.longitude)
            val (localizacaoStatus) = api.protegida(sessao, "/usuarios/me/localizacao", "PUT", coordenadas)
            if (localizacaoStatus != 200) {
                atualizarMensagem("Não foi possível atualizar sua localização. Código: $localizacaoStatus")
                return@launch
            }
            atualizarMensagem("Localização atualizada. Criando alerta...")
            val (status, corpo) = api.protegida(sessao, "/alertas", "POST", coordenadas)
            when (status) {
                201 -> {
                    val id = JSONObject(corpo).getLong("id")
                    val (codigo, proximas) = api.protegida(sessao, "/alertas/$id/proximas")
                    atualizarMensagem(if (codigo == 200) {
                        val quantidade = JSONObject(proximas).getInt("quantidade")
                        if (quantidade == 0) "🚨 Alerta criado. Nenhuma usuária encontrada em até 1 km."
                        else "🚨 Alerta criado. Usuárias próximas: $quantidade."
                    } else "Alerta criado, mas não foi possível buscar as usuárias próximas.")
                }
                409 -> atualizarMensagem("Você já possui um alerta ativo.")
                else -> atualizarMensagem("Não foi possível criar o alerta. Código: $status")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (api.vigente(sessao)) atualizarMensagem("Não foi possível concluir a solicitação de SOS.")
        } finally {
            if (api.vigente(sessao)) atualizarCarregando(false)
        }
    }
}

@Composable
private fun HomeAtalho(onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, content: @Composable () -> Unit) {
    OutlinedCard(onClick = onClick, enabled = enabled,
        modifier = modifier, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun HomeRotulo(icone: Int, texto: String) {
    Icon(painterResource(icone), null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
    Text(texto, style = MaterialTheme.typography.labelLarge,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

