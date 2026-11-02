package com.benimtarayici.app.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.data.AyarDeposu
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun AyarlarScreen(
    ayar: AyarDeposu,
    onMasaustuDegistir: (Boolean) -> Unit,
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
                text = "Ayarlar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            Baslik("Tema")
            val temalar = listOf("Sistem", "Açık", "Koyu")
            temalar.forEachIndexed { sira, ad ->
                SecenekSatiri(
                    metin = ad,
                    secili = ayar.tema == sira,
                    onClick = { ayar.temaSec(sira) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Baslik("Arama motoru")
            AyarDeposu.aramaMotorlari.forEachIndexed { sira, motor ->
                SecenekSatiri(
                    metin = motor.ad,
                    secili = ayar.aramaMotoruSirasi == sira,
                    onClick = { ayar.aramaMotoruSec(sira) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Baslik("Görüntüleme")
            AnahtarSatiri(
                metin = "Masaüstü sitesi",
                aciklama = "Siteleri bilgisayar görünümüyle açar",
                deger = ayar.masaustuSitesi,
                onChange = { onMasaustuDegistir(it) }
            )
            AnahtarSatiri(
                metin = "Tam ekran",
                aciklama = "Durum ve gezinme çubuklarını gizler. Geri getirmek için ekranın kenarından kaydır.",
                deger = ayar.tamEkran,
                onChange = { ayar.tamEkranDegistir(it) }
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
private fun SecenekSatiri(metin: String, secili: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = secili, onClick = null)
        Text(
            text = metin,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
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
