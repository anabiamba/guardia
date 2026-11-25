package br.com.guardia.data.util

import android.location.Location

object LocationUtils {

    fun calcularDistanciaEmMetros(
        latitudeInicial: Double,
        longitudeInicial: Double,
        latitudeFinal: Double,
        longitudeFinal: Double
    ): Float {

        val localInicial = Location("origem").apply {
            latitude = latitudeInicial
            longitude = longitudeInicial
        }

        val localFinal = Location("destino").apply {
            latitude = latitudeFinal
            longitude = longitudeFinal
        }

        return localInicial.distanceTo(localFinal)
    }
}