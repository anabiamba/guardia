package br.com.guardia.data.remote

import br.com.guardia.data.remote.dto.UsuarioResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val senha: String
)

interface ApiService {

    @GET("api/usuarios/teste")
    suspend fun testarConexao(): Response<String>

    @POST("api/usuarios/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<UsuarioResponse>
}