package com.benimtarayici.app.ui.privacy

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
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
    onCerezKabul: (Boolean) -> Unit,
    onUcuncuTaraf: (Boolean) -> Unit,
    onTemizle: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onGeri: () -> Unit
) {
    BackHandler(enabled = true) { onGeri() }

    val context = LocalContext.current

    var gecmisSec by remember { mutableStateOf(true) }
    var cerezSec by remember { mutableStateOf(true) }
    var onbellekSec by remember { mutableStateOf(true) }
    var siteVerisiSec by remember { mutableStateOf(false) }
    var onayGoster by remember { mutableStateOf(false) }

    val hicbiriSecili = !gecmisSec && !cerezSec && !onbellekSec && !siteVerisiSec

    if (onayGoster) {
        AlertDialog(
            onDismissRequest = { onayGoster = false },
            title = { Text("Seçilen veriler silinsin mi?") },
            text = {
                Text(
                    "Bu işlem geri alınamaz." +
                        if (cerezSec) " Çerezleri silersen sitelerdeki oturumların kapanır." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onTemizle(gecmisSec, cerezSec, onbellekSec, siteVerisiSec)
                    onayGoster = false
                    Toast.makeText(context, "Seçilen veriler temizlendi", Toast.LENGTH_SHORT).show()
                }) { Text("Temizle") }
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

            // ---- Engelleme ----
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
            Text(
                text = "Listede ${engel.listeBoyutu} alan adı var • Şimdiye kadar ${engel.toplamEngellenen} istek engellendi",
                style = MaterialTheme.typography.labelMedium
            )

            // ---- Çerezler ----
            Spacer(modifier = Modifier.height(20.dp))
            Baslik("Çerezler")
            AnahtarSatiri(
                metin = "Çerezleri kabul et",
                aciklama = "Kapatırsan çoğu sitede giriş yapamazsın",
                deger = ayar.cerezKabul,
                onChange = { onCerezKabul(it) }
            )
            AnahtarSatiri(
                metin = "Üçüncü taraf çerezlere izin ver",
                aciklama = "Kapalıyken reklam ve takip çerezlerinin çoğu engellenir. Bazı sitelerde \"başka hesapla giriş\" bozulabilir.",
                deger = ayar.ucuncuTarafCerez,
                onChange = { onUcuncuTaraf(it) }
            )
            AnahtarSatiri(
                metin = "Gizli sekmeler kapanınca verileri sil",
                aciklama = "Son gizli sekme kapanınca çerezler, site verileri ve önbellek silinir. Android'in kısıtı yüzünden normal sekmelerdeki oturumlar da kapanır.",
                deger = ayar.gizliOturumTemizle,
                onChange = { ayar.gizliOturumTemizleDegistir(it) }
            )

            // ---- Temizleme ----
            Spacer(modifier = Modifier.height(20.dp))
            Baslik("Tarama verilerini temizle")
            KutuSatiri("Geçmiş", gecmisSec) { gecmisSec = it }
            KutuSatiri("Çerezler", cerezSec) { cerezSec = it }
            KutuSatiri("Önbellek", onbellekSec) { onbellekSec = it }
            KutuSatiri("Site verileri (yerel depolama)", siteVerisiSec) { siteVerisiSec = it }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onayGoster = true },
                enabled = !hicbiriSecili
            ) {
                Text("Seçilenleri temizle")
            }

            // ---- Engelleme istisnaları ----
            Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun KutuSatiri(metin: String, secili: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!secili) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = secili, onCheckedChange = null)
        Text(
            text = metin,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
