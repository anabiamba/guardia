package br.com.guardia.data.session

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal object ExpiracaoSessao {
    private val formato = Regex("""(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?Z""")

    fun emMillis(valor: String): Long {
        val partes = requireNotNull(formato.matchEntire(valor))
        val segundos = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(partes.groupValues[1]) ?: error("Data de expiração inválida.")
        val milissegundos = partes.groupValues[2].take(3).padEnd(3, '0').toLong()
        return segundos.time + milissegundos
    }
}
