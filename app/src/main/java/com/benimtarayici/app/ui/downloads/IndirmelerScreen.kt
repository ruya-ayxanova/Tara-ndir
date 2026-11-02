package com.benimtarayici.app.ui.downloads

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    konumMetni: String,
    onAc: (Indirme) -> Unit,
    onDuraklat: (Indirme) -> Unit,
    onDevamEt: (Indirme) -> Unit,
    onKaldir: (Indirme) -> Unit,
    onGeri: () -> Unit
) {
    BackHandler(enabled = true) { onGeri() }

    var filtre by remember { mutableIntStateOf(0) }
    var silinecek by remember { mutableStateOf<Indirme?>(null) }

    val liste = when (filtre) {
        1 -> indirmeler.filter { it.videoMu }
        2 -> indirmeler.filter { !it.videoMu }
        else -> indirmeler
    }

    val silinecekSon = silinecek
    if (silinecekSon != null) {
        AlertDialog(
            onDismissRequest = { silinecek = null },
            title = { Text("Dosya silinsin mi?") },
            text = { Text("\"${silinecekSon.dosyaAdi}\" cihazdan da silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    onKaldir(silinecekSon)
                    silinecek = null
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { silinecek = null }) { Text("Vazgeç") }
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
                text = konumMetni,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Filtre("Hepsi", filtre == 0, Modifier.weight(1f)) { filtre = 0 }
                Filtre("🎬 Video", filtre == 1, Modifier.weight(1f)) { filtre = 1 }
                Filtre("📄 Diğer", filtre == 2, Modifier.weight(1f)) { filtre = 2 }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (liste.isEmpty()) {
                Text("Burada gösterilecek indirme yok. Bir dosya bağlantısına dokununca indirme burada görünür.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(liste, key = { it.id }) { indirme ->
                        IndirmeKarti(
                            indirme = indirme,
                            onAc = { onAc(indirme) },
                            onDuraklat = { onDuraklat(indirme) },
                            onDevamEt = { onDevamEt(indirme) },
                            onKaldir = {
                                if (indirme.durum == IndirmeDurumu.TAMAMLANDI) {
                                    silinecek = indirme
                                } else {
                                    onKaldir(indirme)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Filtre(
    metin: String,
    secili: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val arka = if (secili) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val yazi = if (secili) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(arka)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = metin,
            color = yazi,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun IndirmeKarti(
    indirme: Indirme,
    onAc: () -> Unit,
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
    val tamamlandi = indirme.durum == IndirmeDurumu.TAMAMLANDI

    val durumMetni = when (indirme.durum) {
        IndirmeDurumu.INIYOR -> when {
            indirme.kopyalaniyor -> "Klasöre kopyalanıyor…"
            indirme.duraklatiliyor -> "Durduruluyor…"
            else -> "İndiriliyor • $indirilenMetin / $toplamMetin"
        }

        IndirmeDurumu.DURAKLATILDI -> "Duraklatıldı • $indirilenMetin / $toplamMetin"
        IndirmeDurumu.TAMAMLANDI -> "Tamamlandı • $indirilenMetin • Açmak için dokun"
        IndirmeDurumu.HATA -> "Hata: ${indirme.hata}"
    }

    val butonAktif = !indirme.duraklatiliyor && !indirme.kopyalaniyor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = tamamlandi, onClick = onAc)
            .padding(12.dp)
    ) {
        Text(
            text = (if (indirme.videoMu) "🎬 " else "") + indirme.dosyaAdi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (indirme.durum == IndirmeDurumu.INIYOR || indirme.durum == IndirmeDurumu.DURAKLATILDI) {
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
                IndirmeDurumu.INIYOR -> Dugme("⏸", butonAktif, onDuraklat)
                IndirmeDurumu.DURAKLATILDI, IndirmeDurumu.HATA -> Dugme("▶", true, onDevamEt)
                IndirmeDurumu.TAMAMLANDI -> {}
            }
            Dugme("✕", butonAktif, onKaldir)
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
