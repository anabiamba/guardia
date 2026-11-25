package br.com.guardia.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.util.Locale
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Sem Activity, tokens, persistência ou logs de coordenadas/endereço. */
internal class GeocoderEndereco(context: Context) {
    private val app = context.applicationContext
    suspend fun buscar(latitude: Double, longitude: Double): String? =
        consultarEndereco(latitude, longitude, ::iniciar)

    private fun iniciar(latitude: Double, longitude: Double, retorno: (PartesEndereco?) -> Unit): () -> Unit {
        val ativo = AtomicBoolean(true)
        fun concluir(endereco: Address?) {
            if (ativo.compareAndSet(true, false)) {
                val partes = endereco?.let { PartesEndereco(it.thoroughfare, it.subThoroughfare,
                    it.subLocality, it.locality, it.adminArea) }
                retorno(partes)
            }
        }
        val futuro: Future<*> = try {
            executor.submit {
                try {
                    if (!ativo.get()) return@submit
                    if (!Geocoder.isPresent()) { concluir(null); return@submit }
                    val geocoder = Geocoder(app, Locale.forLanguageTag("pt-BR"))
                    if (Build.VERSION.SDK_INT >= 33) {
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) { concluir(addresses.firstOrNull()) }
                            override fun onError(errorMessage: String?) { concluir(null) }
                        })
                    } else {
                        concluir(buscarLegado(geocoder, latitude, longitude))
                    }
                } catch (_: Exception) { concluir(null) }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            concluir(null)
            return { ativo.set(false) }
        }
        return { ativo.set(false); futuro.cancel(true); Unit }
    }

    @Suppress("DEPRECATION") // Somente API 24–32, fora da thread da interface.
    private fun buscarLegado(geocoder: Geocoder, latitude: Double, longitude: Double): Address? =
        geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()

    companion object {
        // Sem fila ilimitada: uma chamada legada bloqueada não acumula novas tarefas.
        private val executor = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, SynchronousQueue<Runnable>(),
            { tarefa -> Thread(tarefa, "guardia-endereco").apply { isDaemon = true } })
    }
}
