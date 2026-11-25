package br.com.guardia.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class LocationHelper(private val context: Context) {
    private val cancelamentos = mutableSetOf<() -> Unit>()

    fun cancelarPendentes() {
        val copia = synchronized(cancelamentos) { cancelamentos.toList() }
        copia.forEach { it() }
    }

    @SuppressLint("MissingPermission")
    suspend fun obterLocalizacao(): Location? = withTimeoutOrNull(20_000L) {
        callbackFlow<Location?> {
            val permitida = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
            if (!permitida) { trySend(null); close(); return@callbackFlow }
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (coordenadasValidas(location.latitude, location.longitude) &&
                        localizacaoRecente(location.elapsedRealtimeNanos, SystemClock.elapsedRealtimeNanos())) {
                        trySend(location)
                        close()
                    }
                }
                override fun onProviderEnabled(provider: String) = Unit
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                override fun onProviderDisabled(provider: String) { trySend(null); close() }
            }
            val cancelar: () -> Unit = { close(); Unit }
            synchronized(cancelamentos) { cancelamentos.add(cancelar) }
            var registrados = 0
            for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                try {
                    if (manager.isProviderEnabled(provider)) {
                        manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                        registrados++
                    }
                } catch (_: SecurityException) {
                    // Permissão pode ter sido revogada durante a tentativa.
                } catch (_: IllegalArgumentException) {
                    // Provedor ausente neste dispositivo.
                }
            }
            if (registrados == 0) { trySend(null); close() }
            awaitClose {
                runCatching { manager.removeUpdates(listener) }
                synchronized(cancelamentos) { cancelamentos.remove(cancelar) }
            }
        }.firstOrNull()
    }
}
