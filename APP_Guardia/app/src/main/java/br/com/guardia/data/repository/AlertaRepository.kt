package br.com.guardia.data.repository

import br.com.guardia.data.model.Alerta
import br.com.guardia.data.util.LocationUtils

class AlertaRepository {

    private val alertas = mutableListOf<Alerta>()

    companion object {
        const val RAIO_ALERTA_METROS = 1000f
    }

    fun criarAlerta(alerta: Alerta): Alerta {
        alertas.add(alerta)
        return alerta
    }

    fun listarAlertasAtivos(): List<Alerta> {
        return alertas.filter {
            it.ativo
        }
    }

    fun buscarAlertasProximos(
        latitude: Double,
        longitude: Double
    ): List<Alerta> {

        return alertas.filter { alerta ->

            if (!alerta.ativo) {
                return@filter false
            }

            val distancia = LocationUtils.calcularDistanciaEmMetros(
                latitudeInicial = latitude,
                longitudeInicial = longitude,
                latitudeFinal = alerta.latitude,
                longitudeFinal = alerta.longitude
            )

            distancia <= RAIO_ALERTA_METROS
        }
    }

    fun encerrarAlerta(alertaId: Long) {

        val index = alertas.indexOfFirst {
            it.id == alertaId
        }

        if (index != -1) {
            alertas[index] = alertas[index].copy(
                ativo = false
            )
        }
    }
}