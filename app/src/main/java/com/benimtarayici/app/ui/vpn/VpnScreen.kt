package com.benimtarayici.app.ui.vpn

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benimtarayici.app.network.AgDurumu
import com.benimtarayici.app.ui.common.AltButon

@Composable
fun VpnScreen(
    ag: AgDurumu,
    onVpnAyarlari: () -> Unit,
    onWireGuard: () -> Unit,
    onGeri: () -> Unit
) {
    BackHandler(enabled = true) { onGeri() }

    var onayGoster by remember { mutableStateOf(false) }

    if (onayGoster) {
        AlertDialog(
            onDismissRequest = { onayGoster = false },
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
                    onayGoster = false
                }) { Text("Aç") }
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
                    "Telefonun VPN'ine bağlandığında bu ekrandaki durum otomatik güncellenir.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
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

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Güvenlik",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
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
                            onayGoster = true
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
