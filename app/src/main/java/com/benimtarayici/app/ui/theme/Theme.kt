package com.benimtarayici.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AcikRenkler = lightColorScheme(
    primary = Color(0xFFE8590C),
    primaryContainer = Color(0xFFFFE3D1),
    onPrimaryContainer = Color(0xFF5C2200),
    background = Color(0xFFFFFBF8),
    surface = Color(0xFFFFFBF8)
)

private val KoyuRenkler = darkColorScheme(
    primary = Color(0xFFFF9457),
    primaryContainer = Color(0xFF5C2A0E),
    onPrimaryContainer = Color(0xFFFFE3D1),
    background = Color(0xFF16120F),
    surface = Color(0xFF16120F)
)

@Composable
fun TarayiciTema(icerik: @Composable () -> Unit) {
    val renkler = if (isSystemInDarkTheme()) KoyuRenkler else AcikRenkler
    MaterialTheme(colorScheme = renkler, content = icerik)
}
