package com.benimtarayici.app.ui.privacy

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.data.AyarDeposu
import com.benimtarayici.app.engel.EngelYoneticisi
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun GizlilikScreen(
    ayar: AyarDeposu,
    engel: EngelYoneticisi,
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Gizlilik ve güvenlik",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            Baslik("Engelleme")
            AnahtarSatiri(
                metin = "Reklam engelleme",
                aciklama = "Bilinen reklam sunucularına giden istekleri keser",
                deger = ayar.reklamEngelle,
                onChange = { ayar.reklamEngelleDegistir(it) }
            )
            AnahtarSatiri(
                metin = "İzleyici engelleme",
                aciklama = "Analiz ve takip betiklerini keser",
                deger = ayar.izleyiciEngelle,
                onChange = { ayar.izleyiciEngelleDegistir(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Listede ${engel.listeBoyutu} alan adı var • Şimdiye kadar ${engel.toplamEngellenen} istek engellendi",
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(20.dp))
            Baslik("Engellemeyi kapattığın siteler")
            if (ayar.istisnalar.isEmpty()) {
                Text(
                    text = "Henüz yok. Bir sayfada \"Bu sitede kapat\" yazısına dokunarak ekleyebilirsin.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ayar.istisnalar.toList().forEach { site ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = site,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "✕",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { ayar.istisnaDegistir(site) }
                                .padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Not: Engelleme liste tabanlıdır, her reklamı yakalamaz. " +
                    "YouTube gibi sitelerin kendi sunucusundan gelen reklamlar engellenemez. " +
                    "Listeleri GitHub'da app/src/main/assets/ klasöründeki metin dosyalarından büyütebilirsin.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Baslik(metin: String) {
    Text(
        text = metin,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun AnahtarSatiri(
    metin: String,
    aciklama: String,
    deger: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = metin, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = aciklama,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = deger, onCheckedChange = onChange)
    }
}
