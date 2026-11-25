package br.com.guardia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.guardia.data.notification.NotificationHelper
import br.com.guardia.data.remote.ApiAutenticada
import br.com.guardia.data.repository.UsuarioRepository
import br.com.guardia.data.session.SessoesAndroid
import br.com.guardia.data.session.RevogacaoWorker
import br.com.guardia.navigation.AppNavigation
import br.com.guardia.ui.theme.GuardiaTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val LOCAL_NETWORK_PERMISSION_REQUEST = 1001
        private const val NOTIFICATION_PERMISSION_REQUEST = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Cria o canal de notificações.
        NotificationHelper.criarCanal(this)

        // Solicita as permissões necessárias.
        solicitarPermissaoRedeLocal()
        solicitarPermissaoNotificacoes()

        val usuarioRepository = UsuarioRepository()
        val sessoes = SessoesAndroid.obter(this)
        val api = ApiAutenticada(sessoes)

        setContent {
            GuardiaTheme {
                AppNavigation(
                    usuarioRepository = usuarioRepository,
                    sessoes = sessoes,
                    api = api
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SessoesAndroid.obter(this).retomar()
        RevogacaoWorker.recuperar(applicationContext)
    }

    // ============================================
    // PERMISSÃO DE REDE LOCAL
    // ============================================

    private fun solicitarPermissaoRedeLocal() {

        if (Build.VERSION.SDK_INT >= 37) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_LOCAL_NETWORK
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_LOCAL_NETWORK
                    ),
                    LOCAL_NETWORK_PERMISSION_REQUEST
                )
            }
        }
    }

    // ============================================
    // PERMISSÃO DE NOTIFICAÇÕES
    // ============================================

    private fun solicitarPermissaoNotificacoes() {

        // A permissão em tempo de execução é necessária
        // a partir do Android 13 (API 33).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }
}