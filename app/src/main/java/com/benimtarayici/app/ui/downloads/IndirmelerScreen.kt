package com.benimtarayici.app.ui.downloads

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.download.Indirme
import com.benimtarayici.app.download.IndirmeDurumu
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun IndirmelerScreen(
    indirmeler: List<Indirme>,
    onDuraklat: (Indirme) -> Unit,
    onDevamEt: (Indirme) -> Unit,
    onKaldir: (Indirme) -> Unit,
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
                text = "İndirmeler",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Konum: Android/data/com.benimtarayici.app/files/Download",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (indirmeler.isEmpty()) {
                Text("Henüz indirme yok. Bir dosya bağlantısına dokununca indirme burada görünür.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(indirmeler, key = { it.id }) { indirme ->
                        IndirmeKarti(
                            indirme = indirme,
                            onDuraklat = { onDuraklat(indirme) },
                            onDevamEt = { onDevamEt(indirme) },
                            onKaldir = { onKaldir(indirme) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndirmeKarti(
    indirme: Indirme,
    onDuraklat: () -> Unit,
    onDevamEt: () -> Unit,
    onKaldir: () -> Unit
) {
    val context = LocalContext.current
    val indirilenMetin = Formatter.formatShortFileSize(context, indirme.indirilen)
    val toplamMetin = if (indirme.toplam > 0) {
        Formatter.formatShortFileSize(context, indirme.toplam)
    } else {
        "?"
    }

    val durumMetni = when (indirme.durum) {
        IndirmeDurumu.INIYOR ->
            if (indirme.duraklatiliyor) "Durduruluyor…" else "İndiriliyor • $indirilenMetin / $toplamMetin"

        IndirmeDurumu.DURAKLATILDI -> "Duraklatıldı • $indirilenMetin / $toplamMetin"
        IndirmeDurumu.TAMAMLANDI -> "Tamamlandı • $indirilenMetin"
        IndirmeDurumu.HATA -> "Hata: ${indirme.hata}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = indirme.dosyaAdi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (indirme.durum != IndirmeDurumu.TAMAMLANDI && indirme.durum != IndirmeDurumu.HATA) {
            Spacer(modifier = Modifier.height(8.dp))
            if (indirme.toplam > 0) {
                LinearProgressIndicator(
                    progress = {
                        (indirme.indirilen.toFloat() / indirme.toplam.toFloat())
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (indirme.durum == IndirmeDurumu.INIYOR) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = durumMetni,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (indirme.durum) {
                IndirmeDurumu.INIYOR -> Dugme("⏸", !indirme.duraklatiliyor, onDuraklat)
                IndirmeDurumu.DURAKLATILDI, IndirmeDurumu.HATA -> Dugme("▶", true, onDevamEt)
                IndirmeDurumu.TAMAMLANDI -> {}
            }
            Dugme("✕", !indirme.duraklatiliyor, onKaldir)
        }
    }
}

@Composable
private fun Dugme(simge: String, aktif: Boolean, onClick: () -> Unit) {
    Text(
        text = simge,
        fontSize = 20.sp,
        modifier = Modifier
            .alpha(if (aktif) 1f else 0.35f)
            .clip(CircleShape)
            .clickable(enabled = aktif, onClick = onClick)
            .padding(10.dp)
    )
}
