package com.benimtarayici.app.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.data.Kayit
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun KayitListesiScreen(
    baslik: String,
    kayitlar: List<Kayit>,
    bosMesaj: String,
    onAc: (Kayit) -> Unit,
    onSil: (Kayit) -> Unit,
    onGeri: () -> Unit,
    onHepsiniTemizle: (() -> Unit)? = null
) {
    BackHandler(enabled = true) { onGeri() }

    var onayGoster by remember { mutableStateOf(false) }

    if (onayGoster && onHepsiniTemizle != null) {
        AlertDialog(
            onDismissRequest = { onayGoster = false },
            title = { Text("Hepsi silinsin mi?") },
            text = { Text("Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    onHepsiniTemizle()
                    onayGoster = false
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { onayGoster = false }) { Text("Vazgeç") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AltButon(
                        etiket = "Geri",
                        simge = { Text("◀", fontSize = 20.sp) },
                        onClick = { onGeri() }
                    )
                    if (onHepsiniTemizle != null) {
                        AltButon(
                            etiket = "Hepsini sil",
                            simge = { Text("🗑", fontSize = 20.sp) },
                            aktif = kayitlar.isNotEmpty(),
                            onClick = { onayGoster = true }
                        )
                    }
                }
            }
        }
    ) { icBosluk ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(icBosluk)
                .padding(12.dp)
        ) {
            Text(
                text = baslik,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (kayitlar.isEmpty()) {
                Text(bosMesaj)
            } else {
                LazyColumn {
                    items(kayitlar) { kayit ->
                        KayitSatiri(
                            kayit = kayit,
                            onAc = { onAc(kayit) },
                            onSil = { onSil(kayit) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KayitSatiri(kayit: Kayit, onAc: () -> Unit, onSil: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAc)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kayit.baslik,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = kayit.adres,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "✕",
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onSil)
                .padding(10.dp),
            fontSize = 16.sp
        )
    }
}
