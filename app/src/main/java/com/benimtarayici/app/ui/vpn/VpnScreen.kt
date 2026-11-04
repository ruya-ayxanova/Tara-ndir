package com.benimtarayici.app.ui.vpn

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.network.AgDurumu
import com.benimtarayici.app.network.WgDurum
import com.benimtarayici.app.network.WireGuardYoneticisi
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun VpnScreen(
    ag: AgDurumu,
    onVpnAyarlari: () -> Unit,
    onWireGuard: () -> Unit,
    onGeri: () -> Unit
) {
    BackHandler(enabled = true) { onGeri() }

    val context = LocalContext.current
    val wg = remember { WireGuardYoneticisi.al(context) }
    val pano = LocalClipboardManager.current

    var zorunluOnay by remember { mutableStateOf(false) }
    var silOnay by remember { mutableStateOf(false) }

    // Android'in "VPN bağlantısı isteği" penceresi sonucu.
    val izinSecici = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { sonuc ->
        if (sonuc.resultCode == Activity.RESULT_OK) {
            wg.baglan()
        } else {
            Toast.makeText(context, "VPN izni verilmedi.", Toast.LENGTH_LONG).show()
        }
    }

    // .conf dosyasını seç ve içe aktar.
    val dosyaSecici = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val metin = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                }
            } catch (e: Exception) {
                null
            }
            val mesaj = if (metin == null) {
                "Dosya okunamadı."
            } else {
                wg.iceAktar(metin) ?: "Yapılandırma eklendi."
            }
            Toast.makeText(context, mesaj, Toast.LENGTH_LONG).show()
        }
    }

    fun baglanTikla() {
        val niyet = VpnService.prepare(context)
        if (niyet != null) {
            izinSecici.launch(niyet)
        } else {
            wg.baglan()
        }
    }

    if (zorunluOnay) {
        AlertDialog(
            onDismissRequest = { zorunluOnay = false },
            title = { Text("VPN şu an bağlı değil") },
            text = {
                Text(
                    "Zorunlu modu açarsan VPN bağlanana kadar hiçbir sayfa açılmaz " +
                        "ve yeni indirme başlamaz. Devam edilsin mi?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ag.vpnZorunluDegistir(true)
                    zorunluOnay = false
                }) { Text("Aç") }
            },
            dismissButton = {
                TextButton(onClick = { zorunluOnay = false }) { Text("Vazgeç") }
            }
        )
    }

    if (silOnay) {
        AlertDialog(
            onDismissRequest = { silOnay = false },
            title = { Text("Yapılandırma silinsin mi?") },
            text = { Text("Kayıtlı WireGuard yapılandırması ve anahtarları bu telefondan silinir.") },
            confirmButton = {
                TextButton(onClick = {
                    wg.sil()
                    silOnay = false
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { silOnay = false }) { Text("Vazgeç") }
            }
        )
    }

    val wgAcik = wg.durum == WgDurum.ACIK
    val wgBaglaniyor = wg.durum == WgDurum.BAGLANIYOR

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
                text = "VPN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Durum kartı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (ag.vpnBagli) {
                            Color(0xFF1B5E20)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = if (ag.vpnBagli) {
                        "🟢 Bu tarayıcının trafiği VPN üzerinden geçiyor"
                    } else {
                        "⚪ VPN bağlı değil"
                    },
                    color = if (ag.vpnBagli) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Bu tarayıcı kendi VPN sunucusunu işletmez. VPN için bir sunucuya " +
                    "ihtiyacın var: kendi sunucun, modeminin VPN'i veya güvendiğin bir sağlayıcı. " +
                    "Aşağıdan WireGuard yapılandırmanı ekleyip tarayıcının içinde bağlanabilir " +
                    "ya da telefonun VPN'ini kullanabilirsin. Durum otomatik güncellenir.",
                style = MaterialTheme.typography.bodyMedium
            )

            // ---- Uygulama içi WireGuard ----
            Spacer(modifier = Modifier.height(24.dp))
            Baslik("Uygulama içi VPN (WireGuard)")
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (wg.durum) {
                    WgDurum.KAPALI -> "Tünel: kapalı"
                    WgDurum.BAGLANIYOR -> "Tünel: bağlanıyor…"
                    WgDurum.ACIK -> "Tünel: açık"
                    WgDurum.HATA -> "Tünel hatası: ${wg.hata}"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (wg.durum == WgDurum.HATA) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!wg.yapilandirmaVar) {
                Text(
                    text = "Henüz yapılandırma yok. Sunucundan veya sağlayıcından aldığın " +
                        "WireGuard .conf dosyasını ekle.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { dosyaSecici.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📄 Dosyadan içe aktar")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val metin = pano.getText()?.text
                        if (metin.isNullOrBlank()) {
                            Toast.makeText(context, "Pano boş.", Toast.LENGTH_SHORT).show()
                        } else {
                            val mesaj = wg.iceAktar(metin) ?: "Yapılandırma eklendi."
                            Toast.makeText(context, mesaj, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📋 Panodan yapıştır")
                }
            } else {
                Text(
                    text = "Sunucu: ${wg.sunucuOzeti}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { if (wgAcik) wg.kes() else baglanTikla() },
                    enabled = !wgBaglaniyor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            wgAcik -> "Bağlantıyı kes"
                            wgBaglaniyor -> "Bağlanıyor…"
                            else -> "Bağlan"
                        }
                    )
                }
                TextButton(
                    onClick = { silOnay = true },
                    enabled = !wgBaglaniyor
                ) {
                    Text("Yapılandırmayı sil")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bu tünel yalnızca bu tarayıcının trafiğini (indirmeler dahil) kapsar, " +
                    "telefondaki diğer uygulamalar etkilenmez. Yapılandırma ve anahtarlar " +
                    "bu telefonda şifrelenerek saklanır. \"Açık\" yazması tünelin kurulduğunu " +
                    "gösterir, sunucuya ulaşıldığını garanti etmez: bağlandıktan sonra " +
                    "api.ipify.org adresini açıp IP'nin değiştiğini kontrol et.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---- Telefonun VPN'i ----
            Spacer(modifier = Modifier.height(24.dp))
            Baslik("Telefonun VPN'i")
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onVpnAyarlari,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Telefonun VPN ayarlarını aç")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onWireGuard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("WireGuard uygulamasını aç")
            }

            // ---- Güvenlik ----
            Spacer(modifier = Modifier.height(24.dp))
            Baslik("Güvenlik")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VPN zorunlu",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "VPN bağlı değilken sayfalar açılmaz ve yeni indirmeler başlamaz.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = ag.vpnZorunlu,
                    onCheckedChange = { yeni ->
                        if (yeni && !ag.vpnBagli) {
                            zorunluOnay = true
                        } else {
                            ag.vpnZorunluDegistir(yeni)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Notlar: Bu mod sadece bu tarayıcıyı kapsayan ek bir güvencedir. " +
                    "Daha güçlü koruma için telefonun VPN ayarlarında \"Her zaman açık VPN\" ve " +
                    "\"VPN olmadan bağlantıları engelle\" seçeneklerini aç. " +
                    "Bir indirme sürerken VPN düşerse o indirme devam edebilir. " +
                    "Tanımadığın ücretsiz VPN uygulamalarından uzak dur, trafiğini görebilirler.",
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
