package com.benimtarayici.app.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.ui.common.AltButon
import com.benimtarayici.app.ui.common.SekmeSayaci
import com.benimtarayici.app.util.UrlYardimci

data class HizliSite(val ad: String, val adres: String)

private val hizliSiteler = listOf(
    HizliSite("Google", "https://www.google.com"),
    HizliSite("YouTube", "https://www.youtube.com"),
    HizliSite("Vikipedi", "https://tr.wikipedia.org"),
    HizliSite("BBC Türkçe", "https://www.bbc.com/turkce"),
    HizliSite("Harita", "https://www.openstreetmap.org"),
    HizliSite("GitHub", "https://github.com"),
    HizliSite("Çeviri", "https://translate.google.com"),
    HizliSite("Hava", "https://www.mgm.gov.tr")
)

@Composable
fun HomeScreen(
    sekmeSayisi: Int,
    gizliMod: Boolean,
    onAdresAc: (String) -> Unit,
    onSekmeler: () -> Unit,
    onGizliDegistir: () -> Unit,
    onYerImleri: () -> Unit,
    onGecmis: () -> Unit,
    onIndirmeler: () -> Unit,
    onAyarlar: () -> Unit
) {
    var arama by rememberSaveable { mutableStateOf("") }

    fun git(girdi: String) {
        if (girdi.isBlank()) return
        onAdresAc(UrlYardimci.adresYaDaAramaYap(girdi))
    }

    Scaffold(
        bottomBar = {
            AltAracCubugu(
                sekmeSayisi = sekmeSayisi,
                onSekmeler = onSekmeler,
                onIndirmeler = onIndirmeler,
                onAyarlar = onAyarlar
            )
        }
    ) { icBosluk ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(icBosluk)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "Yeni Tarayıcı",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = arama,
                onValueChange = { arama = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ara veya adres yaz") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { git(arama) })
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Etiket(
                    metin = "⭐ Yer imleri",
                    secili = false,
                    modifier = Modifier.weight(1f),
                    onClick = onYerImleri
                )
                Etiket(
                    metin = "🕘 Geçmiş",
                    secili = false,
                    modifier = Modifier.weight(1f),
                    onClick = onGecmis
                )
                Etiket(
                    metin = "🕶 Gizli",
                    secili = gizliMod,
                    modifier = Modifier.weight(1f),
                    onClick = onGizliDegistir
                )
            }

            if (gizliMod) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sıradaki sayfa gizli sekmede açılacak",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(hizliSiteler) { site ->
                    HizliSiteKutusu(site = site, onClick = { git(site.adres) })
                }
            }
        }
    }
}

@Composable
private fun Etiket(
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
private fun HizliSiteKutusu(site: HizliSite, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = site.ad.first().uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = site.ad,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun AltAracCubugu(
    sekmeSayisi: Int,
    onSekmeler: () -> Unit,
    onIndirmeler: () -> Unit,
    onAyarlar: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AltButon(
                etiket = "Ana sayfa",
                simge = { Text("🏠", fontSize = 20.sp) },
                onClick = { }
            )
            AltButon(
                etiket = "Sekmeler",
                simge = { SekmeSayaci(sekmeSayisi) },
                onClick = { onSekmeler() }
            )
            AltButon(
                etiket = "İndirmeler",
                simge = { Text("⬇️", fontSize = 20.sp) },
                onClick = { onIndirmeler() }
            )
            AltButon(
                etiket = "Ayarlar",
                simge = { Text("⚙️", fontSize = 20.sp) },
                onClick = { onAyarlar() }
            )
        }
    }
}
