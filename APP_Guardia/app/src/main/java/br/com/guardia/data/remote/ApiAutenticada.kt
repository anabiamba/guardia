package br.com.guardia.data.remote

import br.com.guardia.data.session.ExpiracaoSessao
import br.com.guardia.data.session.ResultadoDescarte
import br.com.guardia.data.session.RevogacaoWorker
import br.com.guardia.data.session.SessaoAndroid
import br.com.guardia.data.session.SessoesAndroid
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class ApiAutenticada(private val sessoes: SessoesAndroid) {
    suspend fun login(email: String, senha: String): Int {
        val resposta = enviar(
            "/api/v2/sessoes/login", "POST",
            JSONObject().put("email", email).put("senha", senha),
            instalacao = sessoes.instalacaoId(),
            provaInstalacao = sessoes.provaInstalacao()
        )
        currentCoroutineContext().ensureActive()
        if (resposta.first == 200) {
            val corpo = JSONObject(resposta.second)
            val token = corpo.getString("token")
            val tokenRevogacao = corpo.getString("tokenRevogacao")
            val formato = Regex("[A-Za-z0-9_-]{43}")
            require(token.matches(formato) && tokenRevogacao.matches(formato) && token != tokenRevogacao)
            sessoes.iniciar(SessaoAndroid(
                corpo.getJSONObject("usuario").getLong("id"),
                token,
                ExpiracaoSessao.emMillis(corpo.getString("expiraEm")),
                tokenRevogacao
            ))
        }
        return resposta.first
    }

    fun sair(sessao: SessaoAndroid, limparNotificacoes: () -> Unit) {
        val resultado = sessoes.iniciarSaida(sessao) ?: return
        limparNotificacoes()
        if (resultado == ResultadoDescarte.AGENDAVEL) {
            RevogacaoWorker.agendar(sessoes.contexto)
        }
    }

    internal suspend fun revogarPendente(credencial: String): Int =
        enviar("/api/v2/sessoes/revogacoes", "POST",
            JSONObject().put("tokenRevogacao", credencial)).first

    suspend fun testarConexao(): Pair<Int, String> = enviar("/api/usuarios/teste", "GET")

    fun vigente(sessao: SessaoAndroid): Boolean = sessoes.vigente(sessao)

    suspend fun protegida(
        sessao: SessaoAndroid,
        rota: String,
        metodo: String = "GET",
        corpo: JSONObject? = null
    ): Pair<Int, String> {
        sessoes.exigirVigente(sessao)
        val resposta = try {
            enviar("/api/v2$rota", metodo, corpo, token = sessao.token)
        } catch (e: IOException) {
            sessoes.exigirVigente(sessao)
            throw e
        }
        if (resposta.first == 401) sessoes.invalidar(sessao)
        sessoes.exigirVigente(sessao)
        return resposta
    }

    suspend fun alterarSenha(sessao: SessaoAndroid, atual: String, nova: String): Int {
        val codigo = protegida(sessao, "/usuarios/me/senha", "PUT",
            JSONObject().put("senhaAtual", atual).put("novaSenha", nova)).first
        if (codigo == 204) {
            sessoes.invalidar(sessao)
            runCatching { androidx.core.app.NotificationManagerCompat.from(sessoes.contexto).cancelAll() }
        }
        return codigo
    }

    suspend fun registrarFcm(sessao: SessaoAndroid, tokenFcm: String) =
        protegida(sessao, "/usuarios/me/token-fcm", "PUT", JSONObject().put("tokenFcm", tokenFcm))

    private suspend fun enviar(
        rota: String,
        metodo: String,
        corpo: JSONObject? = null,
        token: String? = null,
        instalacao: String? = null,
        provaInstalacao: String? = null
    ): Pair<Int, String> = suspendCancellableCoroutine { continuidade ->
        val body = when (metodo) {
            "POST", "PUT" -> (corpo?.toString() ?: "").toRequestBody(JSON)
            else -> null
        }
        val request = Request.Builder().url("$BASE$rota").method(metodo, body)
            .header("Accept", "application/json")
            .apply {
                if (token != null) header("Authorization", "Bearer $token")
                if (instalacao != null) header("X-Instalacao-Id", instalacao)
                if (provaInstalacao != null) header("X-Instalacao-Prova", provaInstalacao)
            }.build()
        val chamada = CLIENTE.newCall(request)
        continuidade.invokeOnCancellation { chamada.cancel() }
        chamada.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuidade.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val resultado = try {
                        it.code to it.body?.string().orEmpty()
                    } catch (e: IOException) {
                        continuidade.resumeWithException(e)
                        return
                    }
                    continuidade.resume(resultado)
                }
            }
        })
    }

    companion object {
        private const val BASE = "http://10.0.2.2:8080"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val CLIENTE = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
