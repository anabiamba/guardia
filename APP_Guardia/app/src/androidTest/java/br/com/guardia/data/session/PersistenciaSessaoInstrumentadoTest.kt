package br.com.guardia.data.session

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenciaSessaoInstrumentadoTest {
    @Test
    fun reaberturaRestauraCredencialCifradaERegistraSomenteMotivo() = comAmbiente {
        val sessao = novaSessao()
        val inicial = abrir()
        inicial.iniciar(sessao)
        assertEquals(DiagnosticoSessao.Motivo.GRAVADA, inicial.diagnostico.ler())
        val reaberta = abrir()
        assertEquals(sessao.usuarioId, reaberta.estado.value.sessao?.usuarioId)
        assertTrue(reaberta.estado.value.sessao?.token == sessao.token)
        assertEquals(DiagnosticoSessao.Motivo.RESTAURADA, reaberta.diagnostico.ler())
        assertEquals("RESTAURADA", File(raiz, "diagnostico_sessao_v1").readText())
    }

    @Test
    fun ausenciaAposGravacaoFicaDistintaDeInstalacaoNova() = comAmbiente {
        val inicial = abrir()
        assertEquals(DiagnosticoSessao.Motivo.SEM_SESSAO, inicial.diagnostico.ler())
        inicial.iniciar(novaSessao())
        check(preferencias.edit().remove("sessao_com_revogacao").commit())
        val reaberta = abrir()
        assertNull(reaberta.estado.value.sessao)
        assertEquals(DiagnosticoSessao.Motivo.AUSENTE_APOS_GRAVACAO, reaberta.diagnostico.ler())
        assertEquals(DiagnosticoSessao.Motivo.AUSENTE_APOS_GRAVACAO, abrir().diagnostico.ler())
    }

    @Test
    fun chaveIndisponivelExigeLoginEPreservaMotivoSanitizado() = comAmbiente {
        val sessao = novaSessao()
        abrir().iniciar(sessao)
        CifraSessao.destruir(aliasSessao(sessao.idLocal))
        val reaberta = abrir()
        assertNull(reaberta.estado.value.sessao)
        assertTrue(reaberta.estado.value.exigeLogin)
        assertEquals(DiagnosticoSessao.Motivo.CREDENCIAL_LOCAL_INDISPONIVEL, reaberta.diagnostico.ler())
        assertFalse(preferencias.contains("sessao_com_revogacao"))
    }

    @Test
    fun logoutPendenteNaoRestauraSessaoDescartadaENaoAfetaNovaConta() = comAmbiente {
        val antiga = novaSessao()
        val inicial = abrir()
        inicial.iniciar(antiga)
        assertEquals(ResultadoDescarte.AGENDAVEL, inicial.iniciarSaida(antiga))
        val reaberta = abrir()
        assertNull(reaberta.estado.value.sessao)
        assertEquals(DiagnosticoSessao.Motivo.REVOGACAO_PENDENTE, reaberta.diagnostico.ler())
        val nova = novaSessao(900002)
        reaberta.iniciar(nova)
        reaberta.fila.concluir(antiga.idLocal)
        reaberta.confirmarSaida(antiga.idLocal)
        assertTrue(reaberta.vigente(nova))
        assertEquals(nova.usuarioId, abrir().estado.value.sessao?.usuarioId)
    }

    @Test
    fun retomadaMantemIdentidadeEDeduplicacao() = comAmbiente {
        val sessoes = abrir()
        val sessao = novaSessao()
        sessoes.iniciar(sessao)
        assertTrue(sessoes.registrarNotificacao(sessao, "sos:900001"))
        sessoes.retomar()
        sessoes.retomar()
        assertEquals(2L, sessoes.retomada.value)
        assertSame(sessao, sessoes.estado.value.sessao)
        assertFalse(sessoes.registrarNotificacao(sessao, "sos:900001"))
    }

    @Test
    fun retomadaNaoRestauraSessaoInvalidada() = comAmbiente {
        val sessoes = abrir()
        val sessao = novaSessao()
        sessoes.iniciar(sessao)
        sessoes.invalidar(sessao)
        sessoes.retomar()
        assertNull(sessoes.estado.value.sessao)
        assertNull(abrir().estado.value.sessao)
        assertEquals(DiagnosticoSessao.Motivo.INVALIDADA_API, sessoes.diagnostico.ler())
    }

    private fun comAmbiente(teste: Ambiente.() -> Unit) {
        val ambiente = Ambiente()
        try {
            ambiente.teste()
        } finally {
            ambiente.fechar()
        }
    }

    private class Ambiente {
        private val base = InstrumentationRegistry.getInstrumentation().targetContext
        private val nome = "persistencia_teste_${UUID.randomUUID()}"
        val raiz = File(base.cacheDir, nome).apply { check(mkdir()) }
        val preferencias = base.getSharedPreferences(nome, Context.MODE_PRIVATE)
        private val ids = mutableListOf<String>()
        private val contexto = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = raiz
            override fun getSharedPreferences(nomeOriginal: String, modo: Int) = preferencias
        }

        fun abrir(): SessoesAndroid {
            val construtor = SessoesAndroid::class.java.getDeclaredConstructor(Context::class.java)
                .apply { isAccessible = true }
            return construtor.newInstance(contexto)
        }

        fun novaSessao(usuaria: Long = 900001): SessaoAndroid = SessaoAndroid(
            usuaria, "credencial-ficticia", System.currentTimeMillis() + 600000, "revogacao-ficticia"
        ).also { ids.add(it.idLocal) }

        fun fechar() {
            ids.forEach { CifraSessao.destruir(aliasSessao(it)) }
            check(preferencias.edit().clear().commit())
            check(raiz.canonicalFile.parentFile == base.cacheDir.canonicalFile)
            raiz.deleteRecursively()
        }
    }
}