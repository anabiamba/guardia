package br.com.guardia.data.location

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal data class PartesEndereco(val rua: String?, val numero: String? = null,
    val bairro: String? = null, val cidade: String? = null, val estado: String? = null)

internal fun formatarEndereco(partes: PartesEndereco?): String? {
    fun limpar(valor: String?) = valor?.replace(Regex("[\\p{Cntrl}]+"), " ")
        ?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", true) }?.take(160)
    val rua = limpar(partes?.rua) ?: return null
    val numero = limpar(partes?.numero)
    val local = listOfNotNull(limpar(partes?.bairro), limpar(partes?.cidade), limpar(partes?.estado))
        .distinctBy { it.lowercase(java.util.Locale.ROOT) }.joinToString(" — ")
    return rua + (numero?.let { ", $it" } ?: "") + if (local.isEmpty()) "" else " — $local"
}

// O adaptador retorna um cancelamento local. O serviço Android pode continuar fisicamente em execução.
internal suspend fun consultarEndereco(latitude: Double, longitude: Double,
    iniciar: (Double, Double, (PartesEndereco?) -> Unit) -> (() -> Unit),
    timeoutMillis: Long = 8_000L): String? {
    if (!coordenadasValidas(latitude, longitude)) return null
    require(timeoutMillis > 0)
    return withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine { continuacao ->
            val finalizou = AtomicBoolean(false)
            val cancelar: () -> Unit = try {
                iniciar(latitude, longitude) { resultado ->
                    if (finalizou.compareAndSet(false, true)) continuacao.resume(formatarEndereco(resultado))
                }
            } catch (_: Exception) {
                if (finalizou.compareAndSet(false, true)) continuacao.resume(null)
                val vazio: () -> Unit = {}
                vazio
            }
            continuacao.invokeOnCancellation {
                finalizou.set(true)
                runCatching { cancelar() }
            }
        }
    }
}

