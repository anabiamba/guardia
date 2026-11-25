package br.com.guardia.data.session

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilaRevogacoesTest {
    @Test
    fun filaCifradaSobreviveAReaberturaEConclusaoAntigaPreservaASessaoNova() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val raiz = File(contexto.cacheDir, "revogacao-teste-${UUID.randomUUID()}")
        check(raiz.mkdir())
        val teste = object : ContextWrapper(contexto) {
            override fun getNoBackupFilesDir(): File = raiz
        }
        val antiga = UUID.randomUUID().toString()
        val nova = UUID.randomUUID().toString()
        val credencial = "ficticia-sem-envio"
        val novaChave = aliasSessao(nova)
        val novoCifrado = CifraSessao.cifrar(novaChave, byteArrayOf(1))
        try {
            FilaRevogacoes(teste).gravar(antiga, credencial)
            val disco = File(raiz, "revogacoes_v2/$antiga").readBytes()
            assertFalse(disco.toString(Charsets.ISO_8859_1).contains(credencial))
            val reaberta = FilaRevogacoes(teste)
            assertTrue(reaberta.contem(antiga))
            assertTrue(credencial == reaberta.ler(antiga))
            reaberta.concluir(antiga)
            assertFalse(reaberta.contem(antiga))
            assertEquals(1, CifraSessao.decifrar(novaChave, novoCifrado).single().toInt())
        } finally {
            CifraSessao.destruir(novaChave)
            raiz.deleteRecursively()
        }
    }
}
