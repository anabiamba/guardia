package br.com.guardia.data.session

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.UUID

internal class FilaRevogacoes(context: Context) {
    private val pasta = File(context.noBackupFilesDir, "revogacoes_v2")

    fun contem(id: String): Boolean = arquivo(id).baseFile.exists()

    fun ids(): List<String> = pasta.list()?.filter {
        runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false)
    }.orEmpty()

    @Synchronized
    fun gravar(id: String, credencial: String) {
        check(pasta.isDirectory || pasta.mkdirs())
        val arquivo = arquivo(id)
        val dados = CifraSessao.cifrar(ALIAS, credencial.toByteArray(Charsets.US_ASCII), id.toByteArray())
        val saida = arquivo.startWrite()
        try {
            saida.write(dados)
            arquivo.finishWrite(saida)
        } catch (e: Exception) {
            arquivo.failWrite(saida)
            throw e
        }
        check(arquivo.readFully().contentEquals(dados))
    }

    @Synchronized
    fun ler(id: String): String? {
        val arquivo = arquivo(id)
        if (!arquivo.baseFile.exists()) return null
        return CifraSessao.decifrar(ALIAS, arquivo.readFully(), id.toByteArray()).toString(Charsets.US_ASCII)
    }

    @Synchronized
    fun concluir(id: String) {
        CifraSessao.destruir(aliasSessao(id))
        val arquivo = arquivo(id)
        arquivo.delete()
        check(!arquivo.baseFile.exists())
    }

    private fun arquivo(id: String): AtomicFile {
        require(UUID.fromString(id).toString() == id)
        return AtomicFile(File(pasta, id))
    }

    private companion object {
        const val ALIAS = "guardia_fila_revogacoes_v2"
    }
}

internal fun aliasSessao(id: String): String = "guardia_sessao_v3_$id"
