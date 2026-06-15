package com.sorodeveloper.insaatcebimde.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E88E5), // İnşaat temasına uygun bir mavi
    onPrimary = Color.White,
    secondary = Color(0xFFF57C00), // İş güvenliği turuncusu
    onSecondary = Color.White,
    background = Color(0xFFF5F5F5)
)

@Composable
fun InsaatCebimdeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
