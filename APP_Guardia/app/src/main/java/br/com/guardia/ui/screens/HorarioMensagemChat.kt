package br.com.guardia.ui.screens

// Horário local fornecido pela API; não criar timestamp quando o campo estiver ausente.
internal fun horarioMensagemChat(valor: String): String? = runCatching {
    val trecho = valor.take(19)
    if (trecho.length != 19) return null
    val leitor = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT).apply { isLenient = false }
    val posicao = java.text.ParsePosition(0)
    val data = leitor.parse(trecho, posicao)
    if (data == null || posicao.index != trecho.length) null else trecho.substring(11, 16)
}.getOrNull()
