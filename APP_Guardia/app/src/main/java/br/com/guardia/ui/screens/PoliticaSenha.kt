package br.com.guardia.ui.screens

internal const val DESCRICAO_SENHA = "Use no mínimo 8 caracteres, com letra, número e caractere especial (como !, @ ou #)."

// Mesma política da API; sem normalizar, remover espaços ou cortar a credencial.
internal fun erroNovaSenha(senha: String): String? {
    if (senha.codePointCount(0, senha.length) < 8) return DESCRICAO_SENHA
    if (senha.toByteArray(Charsets.UTF_8).size > 72)
        return "A senha ultrapassa o limite permitido. Reduza seu tamanho."
    var letra = false
    var numero = false
    var especial = false
    var indice = 0
    while (indice < senha.length) {
        val cp = senha.codePointAt(indice)
        indice += Character.charCount(cp)
        val tipo = Character.getType(cp)
        if (Character.isISOControl(cp) || tipo == Character.SURROGATE.toInt())
            return "A senha contém um caractere inválido."
        letra = letra || Character.isLetter(cp)
        numero = numero || Character.isDigit(cp)
        especial = especial || when (tipo) {
            Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt(), Character.MATH_SYMBOL.toInt(),
            Character.CURRENCY_SYMBOL.toInt(), Character.MODIFIER_SYMBOL.toInt(), Character.OTHER_SYMBOL.toInt() -> true
            else -> false
        }
    }
    return if (letra && numero && especial) null else DESCRICAO_SENHA
}
