package com.benimtarayici.app.ui.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.browser.Sekme
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun SekmelerScreen(
    sekmeler: List<Sekme>,
    aktifId: Int?,
    onSec: (Sekme) -> Unit,
    onKapat: (Sekme) -> Unit,
    onYeni: () -> Unit,
    onGeri: () -> Unit
) {
    BackHandler(enabled = true) { onGeri() }

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
                        etiket = "Yeni sekme",
                        simge = { Text("➕", fontSize = 20.sp) },
                        onClick = { onYeni() }
                    )
                    AltButon(
                        etiket = "Geri",
                        simge = { Text("◀", fontSize = 20.sp) },
                        onClick = { onGeri() }
                    )
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
                text = "Sekmeler (${sekmeler.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (sekmeler.isEmpty()) {
                Text("Açık sekme yok")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sekmeler, key = { it.id }) { sekme ->
                        SekmeKarti(
                            sekme = sekme,
                            aktif = sekme.id == aktifId,
                            onSec = { onSec(sekme) },
                            onKapat = { onKapat(sekme) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SekmeKarti(
    sekme: Sekme,
    aktif: Boolean,
    onSec: () -> Unit,
    onKapat: () -> Unit
) {
    val sekil = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(sekil)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (aktif) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, sekil)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onSec)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sekme.baslik,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "✕",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onKapat)
                    .padding(6.dp),
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sekme.adres,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
