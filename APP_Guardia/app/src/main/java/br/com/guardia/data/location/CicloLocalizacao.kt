package br.com.guardia.data.location

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal suspend fun executarCicloLocalizacao(
    lifecycle: Lifecycle, intervaloMillis: Long = 60_000L, atualizar: suspend () -> Unit
) {
    require(intervaloMillis > 0)
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while (isActive) {
            val inicio = System.nanoTime()
            atualizar()
            val decorrido = (System.nanoTime() - inicio) / 1_000_000L
            delay((intervaloMillis - decorrido).coerceAtLeast(1L))
        }
    }
}
