package br.com.guardia.data.model

data class Usuario(
    val id: Long? = null,
    val nome: String,
    val email: String,
    val senha: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)