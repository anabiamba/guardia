package br.com.guardia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.guardia.ui.screens.EstadoMinhaLocalizacao
import br.com.guardia.ui.theme.GuardiaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class MinhaLocalizacaoInstrumentadoTest {
    @get:Rule val compose = createComposeRule()

    @Test fun falhaPermiteRepetirAbrirConfiguracoesEVoltarSemCriarSos() {
        val repetiu = AtomicBoolean()
        val configuracoes = AtomicBoolean()
        val voltou = AtomicBoolean()
        compose.setContent {
            GuardiaTheme {
                EstadoMinhaLocalizacao(false, "Permita a localização para mostrar sua posição no mapa.",
                    { voltou.set(true) }, { repetiu.set(true) }, { configuracoes.set(true) })
            }
        }
        compose.onNodeWithText("TENTAR NOVAMENTE").performClick()
        compose.onNodeWithText("CONFIGURAÇÕES DE LOCALIZAÇÃO").performClick()
        compose.onNodeWithText("VOLTAR AO INÍCIO").performClick()
        assertTrue(repetiu.get())
        assertTrue(configuracoes.get())
        assertTrue(voltou.get())
        compose.onNodeWithText("SOS").assertDoesNotExist()
    }

    @Test fun buscaEmAndamentoMantemRetornoSemOferecerConsultasDuplicadas() {
        compose.setContent {
            GuardiaTheme { EstadoMinhaLocalizacao(true, "Buscando sua localização atual...", {}, {}, {}) }
        }
        compose.onNodeWithText("Buscando sua localização atual...").assertIsDisplayed()
        compose.onNodeWithText("VOLTAR AO INÍCIO").assertIsDisplayed()
        compose.onNodeWithText("TENTAR NOVAMENTE").assertDoesNotExist()
    }
}
