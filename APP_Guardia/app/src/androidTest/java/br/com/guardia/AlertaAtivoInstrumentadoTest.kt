package br.com.guardia

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.guardia.ui.screens.AlertaAtivoScreen
import br.com.guardia.ui.screens.ModeloAlertaAtivo
import br.com.guardia.ui.theme.GuardiaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AlertaAtivoInstrumentadoTest {
    @get:Rule val compose = createComposeRule()
    @Test fun ajudaConfirmadaOfereceChatEEncerramentoSemParticipantesInventados() {
        val chat = AtomicBoolean()
        val encerrou = AtomicBoolean()
        compose.setContent {
            GuardiaTheme {
                AlertaAtivoScreen(ModeloAlertaAtivo(7, criadaEm = "2026-11-23T12:30:00"),
                    "Conta fictícia", true, false, false, "", {}, { encerrou.set(true) }, { chat.set(true) },
                    mapa = { Text("Mapa fictício", it) })
            }
        }
        compose.onNodeWithText("Iniciado às 12:30").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Conta fictícia aceitou ajudar você.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ABRIR CHAT").performScrollTo().performClick()
        compose.onNodeWithText("ENCERRAR ALERTA").performScrollTo().performClick()
        assertTrue(chat.get()); assertTrue(encerrou.get())
        compose.onNodeWithText("Participantes (3)").assertDoesNotExist()
    }
    @Test fun prazoVisualZeroNaoEncerraPorContaPropriaEEnvioBloqueiaDuplicacao() {
        val encerrou = AtomicBoolean()
        compose.setContent {
            GuardiaTheme {
                AlertaAtivoScreen(ModeloAlertaAtivo(8, segundosInformados = 0), "", false,
                    true, false, "", {}, { encerrou.set(true) }, {}, mapa = { Text("Mapa fictício", it) })
            }
        }
        compose.onNodeWithText("Prazo atingido. Aguardando confirmação do servidor.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ABRIR CHAT").assertDoesNotExist()
        compose.onNodeWithText("ENCERRANDO...").performScrollTo().assertIsNotEnabled()
        assertFalse(encerrou.get())
    }
    @Test fun mapaComRaioRenderizaComEstiloLocalSemServicosReais() {
        compose.setContent {
            GuardiaTheme {
                br.com.guardia.ui.screens.MapaResumoSos(0.0, 0.0,
                    androidx.compose.ui.Modifier.fillMaxSize(),
                    estiloUri = "asset://mapa_teste.json")
            }
        }
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Carregando mapa...").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("Não foi possível carregar o mapa.", substring = true).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription("Aproximar mapa").performClick()
        compose.onNodeWithText("SOS").performClick()
    }}

