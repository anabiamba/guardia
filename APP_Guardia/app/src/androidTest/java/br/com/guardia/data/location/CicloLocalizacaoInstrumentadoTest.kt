package br.com.guardia.data.location

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class CicloLocalizacaoInstrumentadoTest {
    private class Dono : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
    @Test fun pausaCancelaCapturaERetornoReiniciaSemAPI() = runBlocking {
        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        lateinit var dono: Dono
        instrumentacao.runOnMainSync { dono = Dono() }
        val inicios = Channel<Unit>(Channel.UNLIMITED)
        val cancelamentos = Channel<Unit>(Channel.UNLIMITED)
        val capturas = AtomicInteger()
        val job = launch(Dispatchers.Main) {
            executarCicloLocalizacao(dono.lifecycle) {
                capturas.incrementAndGet()
                inicios.trySend(Unit)
                try { awaitCancellation() } finally { cancelamentos.trySend(Unit) }
            }
        }
        try {
            instrumentacao.runOnMainSync { dono.registry.currentState = Lifecycle.State.RESUMED }
            withTimeout(5000) { inicios.receive() }
            instrumentacao.runOnMainSync { dono.registry.currentState = Lifecycle.State.CREATED }
            withTimeout(5000) { cancelamentos.receive() }
            delay(50)
            assertEquals(1, capturas.get())
            instrumentacao.runOnMainSync { dono.registry.currentState = Lifecycle.State.RESUMED }
            withTimeout(5000) { inicios.receive() }
            assertEquals(2, capturas.get())
        } finally {
            instrumentacao.runOnMainSync { dono.registry.currentState = Lifecycle.State.DESTROYED }
            job.cancelAndJoin()
            inicios.close(); cancelamentos.close()
        }
    }
    @Test fun cicloRepeteSomenteEnquantoResumido() = runBlocking {
        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        lateinit var dono: Dono
        instrumentacao.runOnMainSync { dono = Dono() }
        val sinais = Channel<Unit>(Channel.UNLIMITED)
        val job = launch(Dispatchers.Main) {
            executarCicloLocalizacao(dono.lifecycle, intervaloMillis = 10) { sinais.trySend(Unit) }
        }
        try {
            instrumentacao.runOnMainSync { dono.registry.currentState = Lifecycle.State.RESUMED }
            withTimeout(5000) { repeat(2) { sinais.receive() } }
            instrumentacao.runOnMainSync { dono.registry.currentState = Lifecycle.State.CREATED }
            // Aguarda a transição já processada no dispatcher principal.
            instrumentacao.runOnMainSync { }
            while (sinais.tryReceive().isSuccess) { }
            delay(50)
            assertTrue(sinais.tryReceive().isFailure)
        } finally {
            instrumentacao.runOnMainSync { dono.registry.currentState = Lifecycle.State.DESTROYED }
            job.cancelAndJoin(); sinais.close()
        }
    }
}
