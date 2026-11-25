package br.com.guardia.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.guardia.data.notification.RegistroFcm
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.repository.UsuarioRepository
import br.com.guardia.data.session.SessoesAndroid
import br.com.guardia.ui.screens.ApresentacaoScreen
import br.com.guardia.ui.screens.CadastroScreen
import br.com.guardia.ui.screens.HomeScreen
import br.com.guardia.ui.screens.LoginScreen
import br.com.guardia.ui.screens.WelcomeScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(
    usuarioRepository: UsuarioRepository,
    sessoes: SessoesAndroid,
    api: ApiAutenticada
) {
    val context = LocalContext.current
    val estado by sessoes.estado.collectAsState()
    val retomada by sessoes.retomada.collectAsState()
    val sessao = estado.sessao
    if (sessao != null) {
        key(sessao) {
            LaunchedEffect(sessao) {
                while (sessoes.vigente(sessao)) {
                    delay((sessao.expiraEm - System.currentTimeMillis()).coerceIn(1, 60_000))
                }
            }
            LaunchedEffect(sessao, retomada) {
                RegistroFcm.obterTokenAtual(api, sessao)
            }
            HomeScreen(api, sessoes, sessao, onSair = {
                api.sair(sessao) { NotificationManagerCompat.from(context).cancelAll() }
            })
        }
    } else {
        key(estado.exigeLogin) {
            val navController = rememberNavController()
            NavHost(navController, startDestination = if (estado.exigeLogin) "login" else "welcome") {
                composable("welcome") {
                    WelcomeScreen(
                        onLoginClick = { navController.navigate("login") },
                        onCadastroClick = { navController.navigate("apresentacao") }
                    )
                }
                composable("apresentacao") {
                    ApresentacaoScreen {
                        navController.navigate("cadastro") {
                            popUpTo("apresentacao") { inclusive = true }
                        }
                    }
                }
                composable("login") {
                    LoginScreen(
                        api = api,
                        sessaoEncerrada = estado.exigeLogin,
                        saida = estado.saida,
                        onCadastroClick = { navController.navigate("apresentacao") }
                    )
                }
                composable("cadastro") {
                    CadastroScreen(usuarioRepository = usuarioRepository, onVoltar = { navController.popBackStack() })
                }
            }
        }
    }
}
