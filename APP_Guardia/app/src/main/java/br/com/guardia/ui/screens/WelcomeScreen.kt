package br.com.guardia.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.guardia.R

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit, onCadastroClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF4B164C), Color(0xFF8D195D), Color(0xFFC63E78))))
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(164.dp))
        Text("Guardiã°", color = Color.White, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Apoio e segurança entre mulheres",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF76174D))
        ) { Text("Entrar") }
        TextButton(
            onClick = onCadastroClick,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) { Text("Criar conta") }
    }
}
