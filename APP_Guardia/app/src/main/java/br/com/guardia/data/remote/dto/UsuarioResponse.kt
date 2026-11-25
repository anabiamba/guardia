package br.com.guardia.data.remote.dto

data class UsuarioResponse(
    val id: Long?,
    val nome: String,
    val email: String,
    val latitude: Double?,
    val longitude: Double?
)