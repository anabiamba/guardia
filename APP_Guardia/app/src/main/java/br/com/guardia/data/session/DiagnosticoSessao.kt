package br.com.guardia.data.session

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** Somente o ultimo motivo, sem credenciais, IDs ou historico de atividade. */
internal class DiagnosticoSessao(context: Context) {
    enum class Motivo {
        SEM_SESSAO, GRAVADA, RESTAURADA, AUSENTE_APOS_GRAVACAO,
        EXPIRADA, INVALIDADA_API, CREDENCIAL_LOCAL_INDISPONIVEL,
        REVOGACAO_PENDENTE, SAIDA_SEM_FILA, SAIDA_BLOQUEADA,
        SAIDA_CONCLUIDA, FALHA_GRAVACAO, LEGADA_REMOVIDA
    }

    private val arquivo = AtomicFile(File(context.noBackupFilesDir, "diagnostico_sessao_v1"))

    @Synchronized
    fun ler(): Motivo? = runCatching {
        Motivo.valueOf(arquivo.readFully().toString(Charsets.US_ASCII))
    }.getOrNull()

    @Synchronized
    fun gravar(motivo: Motivo) {
        // Diagnostico indisponivel nunca altera o resultado da autenticacao.
        runCatching {
            val saida = arquivo.startWrite()
            try {
                saida.write(motivo.name.toByteArray(Charsets.US_ASCII))
                arquivo.finishWrite(saida)
            } catch (e: Exception) {
                arquivo.failWrite(saida)
                throw e
            }
        }
    }

    @Synchronized
    fun registrarAusencia() {
        when (ler()) {
            Motivo.GRAVADA, Motivo.RESTAURADA -> gravar(Motivo.AUSENTE_APOS_GRAVACAO)
            null -> gravar(Motivo.SEM_SESSAO)
            else -> Unit
        }
    }
}
