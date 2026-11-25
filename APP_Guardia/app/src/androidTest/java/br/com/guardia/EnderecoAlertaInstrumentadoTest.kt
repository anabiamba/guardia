package br.com.guardia

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.guardia.ui.screens.AlertaRecebidoScreen
import br.com.guardia.ui.screens.EnderecoAproximado
import br.com.guardia.ui.theme.GuardiaTheme
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class EnderecoAlertaInstrumentadoTest {
    @get:Rule val compose = createComposeRule()
    @Test fun indisponibilidadeNaoBloqueiaAjudaETentativaPodeObterEndereco() {
        val chamadas = AtomicInteger()
        compose.setContent {
            GuardiaTheme {
                AlertaRecebidoScreen("Conta fictícia", 0.0, 0.0, {}, {}, alertaId = 1,
                    mapa = { Text("Mapa fictício", it) }, endereco = {
                        EnderecoAproximado(1, 0.0, 0.0, buscar = { _, _ ->
                            if (chamadas.incrementAndGet() == 1) null else "Rua Fictícia, 123"
                        })
                    })
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Endereço indisponível. Consulte o mapa.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("CONFIRMAR AJUDA").assertIsEnabled()
        compose.onNodeWithText("TENTAR ENDEREÇO NOVAMENTE").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Rua Fictícia, 123").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Endereço aproximado").assertExists()
        assertEquals(2, chamadas.get())
    }
    @Test fun respostaAntigaNaoTrocaEnderecoDoNovoAlerta() {
        val antigo = CompletableDeferred<String?>()
        var id by mutableLongStateOf(1)
        compose.setContent {
            GuardiaTheme {
                val atual = id
                EnderecoAproximado(atual, atual.toDouble(), 0.0, buscar = { _, _ ->
                    if (atual == 1L) withContext(NonCancellable) { antigo.await() } else "Rua Nova Fictícia"
                })
            }
        }
        compose.waitUntil { compose.onAllNodesWithText("Buscando endereço...").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { id = 2 }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Rua Nova Fictícia").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { antigo.complete("Rua Antiga Fictícia") }
        compose.waitForIdle()
        compose.onNodeWithText("Rua Nova Fictícia").assertExists()
        compose.onNodeWithText("Rua Antiga Fictícia").assertDoesNotExist()
    }
    @Test fun sairDaTelaCancelaConsultaPendente() {
        val cancelou = AtomicBoolean()
        var mostrar by mutableStateOf(true)
        compose.setContent {
            GuardiaTheme {
                if (mostrar) EnderecoAproximado(1, 0.0, 0.0, buscar = { _, _ ->
                    try { awaitCancellation() } finally { cancelou.set(true) }
                }) else Text("Tela encerrada")
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { mostrar = false }
        compose.waitUntil(10_000) { cancelou.get() }
        compose.onNodeWithText("Tela encerrada").assertIsDisplayed()
    }
}
