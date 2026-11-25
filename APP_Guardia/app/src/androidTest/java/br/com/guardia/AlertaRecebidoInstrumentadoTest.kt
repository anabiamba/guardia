package br.com.guardia

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.guardia.ui.screens.AlertaRecebidoScreen
import br.com.guardia.ui.theme.GuardiaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AlertaRecebidoInstrumentadoTest {
    @get:Rule val compose = createComposeRule()
    @Test fun confirmacaoPendenteBloqueiaToquesECoordenadaInvalidaBloqueiaMapa() {
        compose.setContent {
            GuardiaTheme {
                AlertaRecebidoScreen("Solicitante do alerta", Double.NaN, 0.0, {}, {},
                    alertaId = 1L, confirmandoAjuda = true, mapa = { Text("Mapa fictício", it) }, endereco = { Text("Endereço fictício") })
            }
        }
        compose.onNodeWithText("Alerta próximo").assertIsDisplayed()
        compose.onNodeWithText("CONFIRMANDO...").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("VER DETALHES").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("ABRIR CHAT").assertDoesNotExist()
        compose.onNodeWithText("Latitude:", substring = true).assertDoesNotExist()
    }
    @Test fun falhaPermiteNovaTentativaSemConfirmarAjuda() {
        val tentou = AtomicBoolean()
        val voltou = AtomicBoolean()
        compose.setContent {
            GuardiaTheme {
                AlertaRecebidoScreen("Solicitante do alerta", 0.0, 0.0,
                    { tentou.set(true) }, { voltou.set(true) }, alertaId = 2L,
                    mensagem = "Não foi possível registrar a ajuda.", consultaIndisponivel = true, mapa = { Text("Mapa fictício", it) }, endereco = { Text("Endereço fictício") })
            }
        }
        // Conferir retorno ainda no topo evita depender de scroll programático até a borda superior.
        compose.onNodeWithText("VOLTAR").performClick()
        assertTrue(voltou.get())
        compose.onNodeWithText("Não foi possível registrar a ajuda.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("CONFIRMAR AJUDA").performScrollTo().performClick()
        assertTrue(tentou.get())
        compose.onNodeWithText("ABRIR CHAT").assertDoesNotExist()
        compose.onNodeWithText("VER DETALHES").assertIsEnabled()
    }
    @Test fun ajudaConfirmadaMostraChatSemOferecerNovoAceite() {
        val chat = AtomicBoolean()
        compose.setContent {
            GuardiaTheme {
                AlertaRecebidoScreen("Solicitante do alerta", 0.0, 0.0, {}, {},
                    ajudaRegistrada = true, onChatClick = { chat.set(true) }, alertaId = 3L, mapa = { Text("Mapa fictício", it) }, endereco = { Text("Endereço fictício") })
            }
        }
        compose.onNodeWithText("Ajuda confirmada").assertIsDisplayed()
        compose.onNodeWithText("CONFIRMAR AJUDA").assertDoesNotExist()
        compose.onNodeWithText("ABRIR CHAT").performScrollTo().performClick()
        assertTrue(chat.get())
    }

    @Test fun primeiroNomeEMapaDoAlertaSaoExibidosNoCartao() {
        compose.setContent {
            GuardiaTheme {
                AlertaRecebidoScreen("Ana (solicitante)", 0.0, 0.0, {}, {}, alertaId = 4L,
                    mapa = { Text("Mapa fictício do SOS", it) }, endereco = { Text("Endereço fictício") })
            }
        }
        compose.onNodeWithText("Mapa fictício do SOS").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ana (solicitante)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("VER DETALHES").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Usuária 2").assertDoesNotExist()
    }
}

