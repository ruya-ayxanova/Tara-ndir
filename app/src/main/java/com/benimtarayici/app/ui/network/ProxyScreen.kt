package com.benimtarayici.app.ui.network

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.network.ProxyYoneticisi
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun ProxyScreen(
    proxy: ProxyYoneticisi,
    onDegisti: () -> Unit,
    onGeri: () -> Unit
) {
    BackHandler(enabled = true) { onGeri() }

    val context = LocalContext.current

    var ad by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var tur by remember { mutableStateOf("http") }
    var formHata by remember { mutableStateOf("") }

    // Seçimi kaydeder, WebView'e uygular, bitince açık sayfaları yeniler.
    fun secVeUygula(sira: Int) {
        if (proxy.sec(sira)) {
            proxy.uygula { onDegisti() }
        }
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
                text = "Proxy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Proxy açıkken sayfalar ve indirmeler seçtiğin sunucu üzerinden gider. " +
                    "Proxy bir VPN değildir: sunucu sahibi trafiğini görebilir (şifresiz http " +
                    "sitelerde içeriği de). Sadece güvendiğin bir proxy kullan. " +
                    "Kullanıcı adı/şifre isteyen proxy'ler desteklenmiyor. " +
                    "Proxy çalışmazsa doğrudan bağlantıya düşülmez, sayfalar açılmaz.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!proxy.desteklenir) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bu telefondaki WebView sürümü proxy'yi desteklemiyor. " +
                        "Play Store'dan \"Android System WebView\" uygulamasını güncelle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (proxy.durumMesaji.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = proxy.durumMesaji,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // ---- Kayıtlı proxy'ler ----
            Spacer(modifier = Modifier.height(20.dp))
            Baslik("Kullanılacak proxy")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { secVeUygula(-1) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = proxy.seciliSira == -1, onClick = null)
                Text(
                    text = "Kapalı (doğrudan bağlantı)",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            proxy.liste.forEachIndexed { sira, p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { secVeUygula(sira) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = proxy.seciliSira == sira, onClick = null)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(text = p.ad, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${p.turEtiketi} • ${p.host}:${p.port}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "✕",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                proxy.sil(sira)
                                proxy.uygula { onDegisti() }
                            }
                            .padding(10.dp)
                    )
                }
            }

            // ---- Yeni proxy ekle ----
            Spacer(modifier = Modifier.height(24.dp))
            Baslik("Yeni proxy ekle")
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ad,
                onValueChange = { ad = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ad (isteğe bağlı)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sunucu adresi (örn. proxy.ornek.com)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { harf -> harf.isDigit() }.take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Port (örn. 8080)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = tur == "http", onClick = { tur = "http" })
                Text("HTTP", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                RadioButton(selected = tur == "socks5", onClick = { tur = "socks5" })
                Text("SOCKS5", style = MaterialTheme.typography.bodyLarge)
            }

            if (formHata.isNotEmpty()) {
                Text(
                    text = formHata,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val hata = proxy.ekle(ad, tur, host, port.toIntOrNull() ?: 0)
                if (hata == null) {
                    ad = ""
                    host = ""
                    port = ""
                    formHata = ""
                    Toast.makeText(context, "Proxy eklendi", Toast.LENGTH_SHORT).show()
                } else {
                    formHata = hata
                }
            }) {
                Text("Ekle")
            }
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
