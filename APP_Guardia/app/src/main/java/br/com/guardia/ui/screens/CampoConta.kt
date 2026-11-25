package br.com.guardia.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import br.com.guardia.R

@Composable
internal fun CampoConta(
    value: String,
    onValueChange: (String) -> Unit,
    rotulo: String,
    icone: Int,
    enabled: Boolean,
    teclado: KeyboardType = KeyboardType.Text
) {
    val senha = teclado == KeyboardType.Password
    var visivel by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(rotulo) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        leadingIcon = {
            Icon(painterResource(icone), null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = if (senha) {
            {
                IconButton(onClick = { visivel = !visivel }, enabled = enabled) {
                    Icon(
                        painterResource(if (visivel) R.drawable.ic_auth_ocultar else R.drawable.ic_auth_ver),
                        if (visivel) "Ocultar senha" else "Mostrar senha"
                    )
                }
            }
        } else null,
        visualTransformation = if (senha && !visivel) PasswordVisualTransformation()
            else VisualTransformation.None
    )
}
