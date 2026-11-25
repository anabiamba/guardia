package br.com.guardia.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.guardia.R

private data class Apresentacao(val imagem: Int, val titulo: String, val descricao: String)

private val paginas = listOf(
    Apresentacao(R.drawable.apresentacao_apoio, "Mulheres que se apoiam",
        "Peça ajuda e conte com o apoio de mulheres próximas a você."),
    Apresentacao(R.drawable.apresentacao_local, "Ajuda perto de você",
        "Ao acionar o SOS, seu alerta aparece para usuárias elegíveis em um raio de até 1 km."),
    Apresentacao(R.drawable.apresentacao_chat, "Conecte-se e receba apoio",
        "Converse pelo chat com a pessoa que aceitou ajudar enquanto o alerta estiver ativo.")
)

@Composable
internal fun ApresentacaoScreen(aoConcluir: () -> Unit) {
    var indice by rememberSaveable { mutableIntStateOf(0) }
    val pagina = paginas[indice]
    BackHandler(enabled = indice > 0) { indice-- }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TextButton(
            onClick = aoConcluir,
            modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
        ) { Text("Pular") }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painterResource(pagina.imagem), null, Modifier.size(200.dp))
            Spacer(Modifier.height(18.dp))
            Text(pagina.titulo, style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(pagina.descricao, style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paginas.indices.forEach { item ->
                Box(Modifier.size(8.dp).clip(CircleShape).background(
                    if (item == indice) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer
                ))
            }
        }
        Button(
            onClick = { if (indice == paginas.lastIndex) aoConcluir() else indice++ },
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
                .heightIn(min = 52.dp)
        ) { Text(if (indice == paginas.lastIndex) "Começar" else "Próximo") }
    }
}
