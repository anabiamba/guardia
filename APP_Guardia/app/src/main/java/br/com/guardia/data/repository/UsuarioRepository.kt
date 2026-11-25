package br.com.guardia.data.repository

import br.com.guardia.data.model.Usuario
import br.com.guardia.data.util.LocationUtils

class UsuarioRepository {

    private val usuarios = mutableListOf<Usuario>()

    companion object {
        const val RAIO_PROXIMIDADE_METROS = 1000f
    }

    fun cadastrar(usuario: Usuario): Boolean {

        val emailJaExiste = usuarios.any {
            it.email.equals(usuario.email, ignoreCase = true)
        }

        if (emailJaExiste) {
            return false
        }

        usuarios.add(usuario)

        return true
    }

    fun buscarPorEmail(email: String): Usuario? {

        return usuarios.find {
            it.email.equals(email, ignoreCase = true)
        }
    }

    fun autenticar(email: String, senha: String): Usuario? {

        return usuarios.find {
            it.email.equals(email, ignoreCase = true) &&
                    it.senha == senha
        }
    }

    fun atualizarLocalizacao(
        email: String,
        latitude: Double,
        longitude: Double
    ): Boolean {

        val index = usuarios.indexOfFirst {
            it.email.equals(email, ignoreCase = true)
        }

        if (index == -1) {
            return false
        }

        val usuarioAtual = usuarios[index]

        usuarios[index] = usuarioAtual.copy(
            latitude = latitude,
            longitude = longitude
        )

        return true
    }

    fun buscarUsuariosProximas(
        latitude: Double,
        longitude: Double,
        emailUsuarioAtual: String
    ): List<Usuario> {

        return usuarios.filter { usuario ->

            if (usuario.email.equals(emailUsuarioAtual, ignoreCase = true)) {
                return@filter false
            }

            if (usuario.latitude == null || usuario.longitude == null) {
                return@filter false
            }

            val distancia = LocationUtils.calcularDistanciaEmMetros(
                latitudeInicial = latitude,
                longitudeInicial = longitude,
                latitudeFinal = usuario.latitude,
                longitudeFinal = usuario.longitude
            )

            distancia <= RAIO_PROXIMIDADE_METROS
        }
    }
}