package br.com.guardia.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import br.com.guardia.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.guardia.data.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun CadastroScreen(
    usuarioRepository: UsuarioRepository,
    onVoltar: () -> Unit
) {

    var nome by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var senha by remember {
        mutableStateOf("")
    }

    var confirmarSenha by remember {
        mutableStateOf("")
    }

    var mensagem by remember {
        mutableStateOf("")
    }

    var carregando by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        TextButton(onClick = onVoltar, modifier = Modifier.align(Alignment.Start)) {
            Text("← Voltar")
        }
        Text("Criar conta", style = MaterialTheme.typography.headlineSmall)

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        CampoConta(
            value = nome,
            onValueChange = {
                nome = it
                mensagem = ""
            },
            rotulo = "Nome completo",
            icone = R.drawable.ic_auth_pessoa,
            enabled = !carregando,
            teclado = KeyboardType.Text
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        CampoConta(
            value = email,
            onValueChange = {
                email = it
                mensagem = ""
            },
            rotulo = "E-mail",
            icone = R.drawable.ic_auth_email,
            enabled = !carregando,
            teclado = KeyboardType.Email
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        CampoConta(
            value = senha,
            onValueChange = {
                senha = it
                mensagem = ""
            },
            rotulo = "Senha",
            icone = R.drawable.ic_auth_senha,
            enabled = !carregando,
            teclado = KeyboardType.Password
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        CampoConta(
            value = confirmarSenha,
            onValueChange = {
                confirmarSenha = it
                mensagem = ""
            },
            rotulo = "Confirmar senha",
            icone = R.drawable.ic_auth_senha,
            enabled = !carregando,
            teclado = KeyboardType.Password
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(DESCRICAO_SENHA, style = MaterialTheme.typography.bodySmall)

        if (mensagem.isNotEmpty()) {

            Text(
                text = mensagem
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        Button(
            onClick = {

                when {

                    nome.isBlank() -> {
                        mensagem = "Informe seu nome."
                    }

                    email.isBlank() -> {
                        mensagem = "Informe seu e-mail."
                    }

                    senha.isBlank() -> {
                        mensagem = "Informe sua senha."
                    }

                    erroNovaSenha(senha) != null -> {
                        mensagem = erroNovaSenha(senha) ?: DESCRICAO_SENHA
                    }

                    confirmarSenha.isBlank() -> {
                        mensagem = "Confirme sua senha."
                    }

                    senha != confirmarSenha -> {
                        mensagem = "As senhas não são iguais."
                    }

                    else -> {

                        carregando = true
                        mensagem = "Criando conta..."

                        coroutineScope.launch {

                            try {

                                Log.d(
                                    "GUARDIA_CADASTRO",
                                    "Iniciando cadastro para: ${email.trim()}"
                                )

                                val resultado =
                                    withContext(Dispatchers.IO) {

                                        val url = URL(
                                            "http://10.0.2.2:8080/api/usuarios"
                                        )

                                        val connection =
                                            url.openConnection()
                                                    as HttpURLConnection

                                        try {

                                            connection.requestMethod = "POST"
                                            connection.connectTimeout = 10000
                                            connection.readTimeout = 10000
                                            connection.doOutput = true

                                            connection.setRequestProperty(
                                                "Content-Type",
                                                "application/json"
                                            )

                                            connection.setRequestProperty(
                                                "Accept",
                                                "application/json"
                                            )

                                            val json = JSONObject().apply {
                                                put(
                                                    "nome",
                                                    nome.trim()
                                                )
                                                put(
                                                    "email",
                                                    email.trim()
                                                )
                                                put(
                                                    "senha",
                                                    senha
                                                )
                                            }

                                            connection.outputStream
                                                .bufferedWriter()
                                                .use { writer ->
                                                    writer.write(
                                                        json.toString()
                                                    )
                                                }

                                            val codigo =
                                                connection.responseCode

                                            val corpo =
                                                if (codigo in 200..299) {

                                                    connection.inputStream
                                                        .bufferedReader()
                                                        .use {
                                                            it.readText()
                                                        }

                                                } else {

                                                    connection.errorStream
                                                        ?.bufferedReader()
                                                        ?.use {
                                                            it.readText()
                                                        }
                                                        ?: ""
                                                }

                                            Pair(
                                                codigo,
                                                corpo
                                            )

                                        } finally {

                                            connection.disconnect()
                                        }
                                    }

                                Log.d(
                                    "GUARDIA_CADASTRO",
                                    "HTTP status: ${resultado.first}"
                                )
                                when (resultado.first) {

                                    201 -> {

                                        mensagem =
                                            "Cadastro realizado com sucesso!"

                                        nome = ""
                                        email = ""
                                        senha = ""
                                        confirmarSenha = ""
                                    }

                                    400 -> {

                                        mensagem =
                                            if (
                                                resultado.second.contains(
                                                    "E-mail já cadastrado"
                                                )
                                            ) {
                                                "Este e-mail já está cadastrado."
                                            } else {
                                                "Confira os dados e as regras de senha informadas."
                                            }
                                    }

                                    else -> {

                                        mensagem =
                                            "Erro ao realizar cadastro. Código: ${resultado.first}"
                                    }
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    "GUARDIA_CADASTRO",
                                    "Erro no cadastro",
                                    e
                                )

                                mensagem =
                                    "Erro: ${e::class.simpleName}\n${e.message}"

                            } finally {

                                carregando = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            enabled = !carregando
        ) {

            Text(
                text = if (carregando) {
                    "CRIANDO..."
                } else {
                    "CRIAR CONTA"
                }
            )
        }
    }
}