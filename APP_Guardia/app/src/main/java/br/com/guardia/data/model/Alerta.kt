package br.com.guardia.data.model

data class Alerta(
    val id: Long? = null,
    val usuarioId: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val ativo: Boolean = true
)