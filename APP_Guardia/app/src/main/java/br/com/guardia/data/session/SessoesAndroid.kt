package br.com.guardia.data.session

import android.content.Context
import android.util.Base64
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SessaoAndroid internal constructor(
    val usuarioId: Long,
    internal val token: String,
    val expiraEm: Long,
    internal val tokenRevogacao: String,
    internal val idLocal: String = UUID.randomUUID().toString()
)

class SessoesAndroid private constructor(context: Context) {
    internal val contexto = context.applicationContext
    internal val fila = FilaRevogacoes(contexto)
    private val preferencias = contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
    internal val diagnostico = DiagnosticoSessao(contexto)
    private val retomadaMutavel = MutableStateFlow(0L)
    val retomada = retomadaMutavel.asStateFlow()
    private val estadoMutavel = MutableStateFlow(restaurar())
    val estado = estadoMutavel.asStateFlow()

    @Synchronized
    fun instalacaoId(): String = preferencias.getString("instalacao", null)
        ?: UUID.randomUUID().toString().also {
            check(preferencias.edit().putString("instalacao", it).commit())
        }

    @Synchronized
    internal fun provaInstalacao(): String {
        val alias = "guardia_prova_instalacao"
        val salvo = preferencias.getString("prova_instalacao", null)
        if (salvo != null) {
            return CifraSessao.decifrar(alias, Base64.decode(salvo, Base64.NO_WRAP))
                .toString(Charsets.US_ASCII)
        }
        val bytes = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val prova = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val cifrado = CifraSessao.cifrar(alias, prova.toByteArray(Charsets.US_ASCII))
        check(preferencias.edit().putString("prova_instalacao",
            Base64.encodeToString(cifrado, Base64.NO_WRAP)).commit())
        return prova
    }

    @Synchronized
    internal fun iniciar(sessao: SessaoAndroid) {
        check(estadoMutavel.value.saida != Saida.BLOQUEADA)
        require(sessao.expiraEm > System.currentTimeMillis())
        val dados = JSONObject()
            .put("usuarioId", sessao.usuarioId)
            .put("token", sessao.token)
            .put("expiraEm", sessao.expiraEm)
            .put("tokenRevogacao", sessao.tokenRevogacao)
        val alias = aliasSessao(sessao.idLocal)
        try {
            val cifrado = CifraSessao.cifrar(alias, dados.toString().toByteArray(Charsets.UTF_8))
            val salvo = sessao.idLocal + ":" + Base64.encodeToString(cifrado, Base64.NO_WRAP)
            check(preferencias.edit().putString(SESSAO, salvo).commit())
        } catch (e: Exception) {
            diagnostico.gravar(DiagnosticoSessao.Motivo.FALHA_GRAVACAO)
            runCatching { CifraSessao.destruir(alias) }
            throw e
        }
        diagnostico.gravar(DiagnosticoSessao.Motivo.GRAVADA)
        estadoMutavel.value = EstadoSessao(sessao, false)
    }

    @Synchronized
    fun vigente(sessao: SessaoAndroid): Boolean {
        if (estadoMutavel.value.sessao !== sessao) return false
        if (sessao.expiraEm > System.currentTimeMillis()) return true
        invalidar(sessao, DiagnosticoSessao.Motivo.EXPIRADA)
        return false
    }

    internal fun exigirVigente(sessao: SessaoAndroid) {
        if (!vigente(sessao)) throw CancellationException("Sessão indisponível.")
    }

    @Synchronized
    internal fun invalidar(
        sessao: SessaoAndroid,
        motivo: DiagnosticoSessao.Motivo = DiagnosticoSessao.Motivo.INVALIDADA_API
    ) {
        if (estadoMutavel.value.sessao !== sessao) return
        runCatching { CifraSessao.destruir(aliasSessao(sessao.idLocal)) }
        preferencias.edit().remove(SESSAO).apply()
        diagnostico.gravar(motivo)
        estadoMutavel.value = EstadoSessao(null, true)
    }

    @Synchronized
    fun retomar() {
        val sessao = estadoMutavel.value.sessao
        if (sessao != null && !vigente(sessao)) return
        retomadaMutavel.value += 1
    }

    @Synchronized
    fun registrarNotificacao(sessao: SessaoAndroid, evento: String): Boolean {
        if (!vigente(sessao)) return false
        val chave = "eventos_${sessao.usuarioId}"
        val anteriores = preferencias.getString(chave, "").orEmpty().split(',')
        if (evento in anteriores) return false
        return preferencias.edit().putString(chave, (anteriores + evento).takeLast(256).joinToString(","))
            .commit()
    }

    @Synchronized
    internal fun iniciarSaida(sessao: SessaoAndroid): ResultadoDescarte? {
        if (!vigente(sessao)) return null
        val resultado = descartarSessao(
            gravarRevogacao = { fila.gravar(sessao.idLocal, sessao.tokenRevogacao) },
            destruirChave = { CifraSessao.destruir(aliasSessao(sessao.idLocal)) },
            removerSessao = { preferencias.edit().remove(SESSAO).commit() }
        )
        val saida = when (resultado) {
            ResultadoDescarte.AGENDAVEL -> Saida.PENDENTE
            ResultadoDescarte.SEM_FILA -> Saida.FALHA_LOCAL
            ResultadoDescarte.FALHA_LOCAL -> Saida.BLOQUEADA
        }
        val motivo = when (resultado) {
            ResultadoDescarte.AGENDAVEL -> DiagnosticoSessao.Motivo.REVOGACAO_PENDENTE
            ResultadoDescarte.SEM_FILA -> DiagnosticoSessao.Motivo.SAIDA_SEM_FILA
            ResultadoDescarte.FALHA_LOCAL -> DiagnosticoSessao.Motivo.SAIDA_BLOQUEADA
        }
        diagnostico.gravar(motivo)
        estadoMutavel.value = EstadoSessao(null, true, saida, sessao.idLocal)
        return resultado
    }

    @Synchronized
    internal fun confirmarSaida(id: String) {
        val atual = estadoMutavel.value
        if (atual.sessao == null && atual.idSaida == id) {
            diagnostico.gravar(DiagnosticoSessao.Motivo.SAIDA_CONCLUIDA)
            estadoMutavel.value = EstadoSessao(null, true, Saida.CONCLUIDA)
        }
    }

    private fun restaurar(): EstadoSessao {
        val salvo = preferencias.getString(SESSAO, null)
        if (salvo == null) {
            diagnostico.registrarAusencia()
            val pendente = fila.ids().firstOrNull()
            if (pendente != null) {
                diagnostico.gravar(DiagnosticoSessao.Motivo.REVOGACAO_PENDENTE)
                return EstadoSessao(null, true, Saida.PENDENTE, pendente)
            }
            val legado = preferencias.contains("sessao") || preferencias.contains("logout_pendente")
            if (legado) {
                diagnostico.gravar(DiagnosticoSessao.Motivo.LEGADA_REMOVIDA)
                runCatching { CifraSessao.destruir("guardia_sessao_v2_aes") }
                preferencias.edit().remove("sessao").remove("logout_pendente").apply()
            }
            return EstadoSessao(null, legado)
        }
        val id = salvo.substringBefore(':')
        val sessao = try {
            require(UUID.fromString(id).toString() == id)
            if (fila.contem(id)) {
                diagnostico.gravar(DiagnosticoSessao.Motivo.REVOGACAO_PENDENTE)
                return EstadoSessao(null, true, Saida.PENDENTE, id)
            }
            val bytes = Base64.decode(salvo.substringAfter(':'), Base64.NO_WRAP)
            val dados = JSONObject(CifraSessao.decifrar(aliasSessao(id), bytes).toString(Charsets.UTF_8))
            SessaoAndroid(dados.getLong("usuarioId"), dados.getString("token"), dados.getLong("expiraEm"),
                dados.getString("tokenRevogacao"), id).takeIf {
                val vigente = it.expiraEm > System.currentTimeMillis()
                if (!vigente) diagnostico.gravar(DiagnosticoSessao.Motivo.EXPIRADA)
                vigente
            }
        } catch (_: Exception) {
            diagnostico.gravar(DiagnosticoSessao.Motivo.CREDENCIAL_LOCAL_INDISPONIVEL)
            null
        }
        if (sessao == null) {
            if (runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) {
                runCatching { CifraSessao.destruir(aliasSessao(id)) }
            }
            preferencias.edit().remove(SESSAO).apply()
        }
        if (sessao != null) diagnostico.gravar(DiagnosticoSessao.Motivo.RESTAURADA)
        return EstadoSessao(sessao, sessao == null)
    }

    enum class Saida { NENHUMA, CONCLUIDA, PENDENTE, FALHA_LOCAL, BLOQUEADA }

    class EstadoSessao(
        val sessao: SessaoAndroid?,
        val exigeLogin: Boolean,
        val saida: Saida = Saida.NENHUMA,
        internal val idSaida: String? = null
    )

    companion object {
        private const val PREFERENCIAS = "guardia_sessao_v2"
        private const val SESSAO = "sessao_com_revogacao"
        private var instancia: SessoesAndroid? = null

        @Synchronized
        fun obter(context: Context): SessoesAndroid = instancia
            ?: SessoesAndroid(context.applicationContext).also { instancia = it }
    }
}
