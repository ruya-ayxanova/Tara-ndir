package com.benimtarayici.app.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SekmeSayaci(sayi: Int) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(
                2.dp,
                MaterialTheme.colorScheme.onSurface,
                RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sayi.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AltButon(
    etiket: String,
    simge: @Composable () -> Unit,
    aktif: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .alpha(if (aktif) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = aktif, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
            simge()
        }
        Text(text = etiket, style = MaterialTheme.typography.labelSmall)
    }
}
