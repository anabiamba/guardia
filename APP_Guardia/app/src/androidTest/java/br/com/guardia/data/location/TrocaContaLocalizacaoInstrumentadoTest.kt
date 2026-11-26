package br.com.guardia.data.location

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.session.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Integra componentes Android reais com captura/envio simulados. Não chama HTTP, GPS ou FCM. */
@RunWith(AndroidJUnit4::class)
class TrocaContaLocalizacaoInstrumentadoTest {
    @Test
    fun capturaDeANaoEnviaPosicaoDepoisDaTrocaParaB() {
        comAmbiente { ambiente ->
            runBlocking {
                val a = ambiente.novaSessao(910001)
                val b = ambiente.novaSessao(910002)
                val sessoes = ambiente.abrir()
                val api = ApiAutenticada(sessoes)
                sessoes.iniciar(a)
                val capturaIniciada = CompletableDeferred<Unit>()
                val liberarCaptura = CompletableDeferred<Posicao?>()
                var enviosAntigos = 0
                val antiga = async {
                    sincronizarLocalizacao(
                        obter = { capturaIniciada.complete(Unit); liberarCaptura.await() },
                        vigente = { api.vigente(a) },
                        enviar = { enviosAntigos++; true }
                    )
                }
                try {
                    withTimeout(10_000) { capturaIniciada.await() }
                    ambiente.trocar(sessoes, a, b)
                    liberarCaptura.complete(Posicao(1.0, 2.0))
                    assertEquals(ResultadoLocalizacao.DESCARTADA, withTimeout(10_000) { antiga.await() })
                    assertEquals(0, enviosAntigos)
                    assertFalse(api.vigente(a))
                    assertTrue(api.vigente(b))
                    // A barreira de sessão da API real deve recusar A antes de construir qualquer chamada HTTP.
                    var recusouAntiga = false
                    try { api.protegida(a, "/usuarios/me/localizacao", "PUT") }
                    catch (_: CancellationException) { recusouAntiga = true }
                    assertTrue("API deve recusar a sessão antiga localmente.", recusouAntiga)
                    confirmarNovaBuscaDeB(api, sessoes, b)
                } finally {
                    liberarCaptura.complete(null)
                    antiga.cancelAndJoin()
                }
            }
        }
    }

    @Test
    fun respostaDeEnvioDeANaoConfirmaAtualizacaoNemInvalidaB() {
        comAmbiente { ambiente ->
            runBlocking {
                val a = ambiente.novaSessao(920001)
                val b = ambiente.novaSessao(920002)
                val sessoes = ambiente.abrir()
                val api = ApiAutenticada(sessoes)
                sessoes.iniciar(a)
                val envioIniciado = CompletableDeferred<Unit>()
                val liberarResposta = CompletableDeferred<Boolean>()
                var enviosDeA = 0
                val antiga = async {
                    sincronizarLocalizacao(
                        obter = { Posicao(1.0, 2.0) },
                        vigente = { api.vigente(a) },
                        enviar = {
                            assertTrue(api.vigente(a))
                            enviosDeA++
                            envioIniciado.complete(Unit)
                            liberarResposta.await()
                        }
                    )
                }
                try {
                    withTimeout(10_000) { envioIniciado.await() }
                    ambiente.trocar(sessoes, a, b)
                    liberarResposta.complete(true)
                    assertEquals(ResultadoLocalizacao.DESCARTADA, withTimeout(10_000) { antiga.await() })
                    assertEquals(1, enviosDeA)
                    // Mesmo efeito local de resposta 401 antiga; nenhuma resposta HTTP real é fabricada.
                    sessoes.invalidar(a)
                    assertTrue(api.vigente(b))
                    assertSame(b, sessoes.estado.value.sessao)
                    confirmarNovaBuscaDeB(api, sessoes, b)
                } finally {
                    liberarResposta.complete(false)
                    antiga.cancelAndJoin()
                }
            }
        }
    }

    private suspend fun confirmarNovaBuscaDeB(api: ApiAutenticada, sessoes: SessoesAndroid, b: SessaoAndroid) {
        val novaPosicao = Posicao(3.0, 4.0)
        var capturas = 0
        val envios = mutableListOf<Posicao>()
        val resultado = sincronizarLocalizacao(
            obter = { capturas++; novaPosicao },
            vigente = { api.vigente(b) },
            enviar = { posicao ->
                assertSame(b, sessoes.estado.value.sessao)
                envios.add(posicao)
                true
            }
        )
        assertEquals(ResultadoLocalizacao.ATUALIZADA, resultado)
        assertEquals(1, capturas)
        assertEquals(listOf(novaPosicao), envios)
        assertTrue(api.vigente(b))
    }

    private fun comAmbiente(teste: (Ambiente) -> Unit) {
        val ambiente = Ambiente()
        try { teste(ambiente) } finally { ambiente.fechar() }
    }

    private class Ambiente {
        private val base = InstrumentationRegistry.getInstrumentation().targetContext
        private val nome = "troca_localizacao_teste_${UUID.randomUUID()}"
        private val raiz = File(base.cacheDir, nome).apply { check(mkdir()) }
        private val preferencias = base.getSharedPreferences(nome, Context.MODE_PRIVATE)
        private val ids = mutableListOf<String>()
        private val contexto = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = raiz
            override fun getSharedPreferences(nomeOriginal: String, modo: Int) = preferencias
        }

        fun abrir(): SessoesAndroid = SessoesAndroid::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }.newInstance(contexto)

        fun novaSessao(usuaria: Long): SessaoAndroid = SessaoAndroid(
            usuaria, "token-ficticio-local", System.currentTimeMillis() + 600_000, "revogacao-ficticia-local"
        ).also { ids.add(it.idLocal) }

        fun trocar(sessoes: SessoesAndroid, a: SessaoAndroid, b: SessaoAndroid) {
            assertEquals(ResultadoDescarte.AGENDAVEL, sessoes.iniciarSaida(a))
            sessoes.iniciar(b)
            assertTrue(sessoes.vigente(b))
        }

        fun fechar() {
            ids.forEach { CifraSessao.destruir(aliasSessao(it)) }
            check(preferencias.edit().clear().commit())
            // Somente a pasta temporária exclusiva deste teste; não tocar nas preferências/filas reais.
            check(raiz.canonicalFile.parentFile == base.cacheDir.canonicalFile)
            check(raiz.name.startsWith("troca_localizacao_teste_"))
            check(raiz.deleteRecursively())
            base.deleteSharedPreferences(nome)
        }
    }
}
