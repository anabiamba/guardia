package br.com.guardia.ui.screens

internal data class EstadoAlertaRecebido(val confirmadoId: Long? = null) {
    fun confirmadoPara(id: Long) = confirmadoId == id
    fun confirmar(id: Long) = EstadoAlertaRecebido(id)
    fun limpar(id: Long) = if (confirmadoPara(id)) EstadoAlertaRecebido() else this
}
