package br.com.guardia

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.guardia.ui.screens.MapaAlertaScreen
import br.com.guardia.ui.theme.GuardiaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class MapaAlertaInstrumentadoTest {
    @get:Rule val compose = createComposeRule()
    @Test fun mapaLocalRenderizaPermiteZoomERetornoSemAPI() {
        compose.setContent {
            GuardiaTheme {
                var mostrar by remember { mutableStateOf(true) }
                if (mostrar) MapaAlertaScreen(0.0, 0.0, aoVoltar = { mostrar = false }, abrirExterno = {},
                    estiloUri = "asset://mapa_teste.json")
                else Text("Alerta de teste")
            }
        }
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText("Carregando mapa...").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("Não foi possível carregar o mapa.", substring = true).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription("Aproximar mapa").performClick()
        compose.onNodeWithContentDescription("Afastar mapa").performClick()
        compose.onNodeWithText("SOS").performClick()
        compose.onNodeWithText("VOLTAR AO ALERTA").performClick()
        compose.onNodeWithText("Alerta de teste").assertIsDisplayed()
    }
    @Test fun pontoInvalidoMantemRetornoEAlternativaExterna() {
        val externo = AtomicBoolean()
        val voltou = AtomicBoolean()
        compose.setContent {
            GuardiaTheme {
                MapaAlertaScreen(Double.NaN, 0.0, aoVoltar = { voltou.set(true) },
                    abrirExterno = { externo.set(true) }, erroExterno = "Nenhum aplicativo de mapas disponível.")
            }
        }
        compose.onNodeWithText("Esta localização não pode ser exibida", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Nenhum aplicativo de mapas disponível.").assertIsDisplayed()
        compose.onNodeWithText("ABRIR MAPA EXTERNO").performClick()
        assertTrue(externo.get())
        compose.onNodeWithText("VOLTAR AO ALERTA").performClick()
        assertTrue(voltou.get())
    }
}
