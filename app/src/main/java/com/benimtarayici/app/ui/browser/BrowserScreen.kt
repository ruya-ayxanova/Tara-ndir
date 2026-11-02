package com.benimtarayici.app.ui.browser

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.benimtarayici.app.browser.Sekme
import com.benimtarayici.app.ui.common.AltButon
import com.benimtarayici.app.ui.common.SekmeSayaci
import com.benimtarayici.app.util.UrlYardimci

@Composable
fun BrowserScreen(
    sekme: Sekme,
    sekmeSayisi: Int,
    yerImiVar: Boolean,
    istisnaVar: Boolean,
    onYerImi: () -> Unit,
    onIstisna: () -> Unit,
    onGizlilik: () -> Unit,
    onAnaSayfa: () -> Unit,
    onSekmeler: () -> Unit
) {
    val odak = LocalFocusManager.current
    var adresMetni by remember(sekme.id) { mutableStateOf(sekme.adres) }

    LaunchedEffect(sekme.id, sekme.adres) {
        adresMetni = sekme.adres
    }

    // Telefonun geri tuşu: önce sayfada geri git, gidemiyorsa ana sayfaya dön.
    BackHandler(enabled = true) {
        if (sekme.webView.canGoBack()) sekme.webView.goBack() else onAnaSayfa()
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
                        aktif = sekme.geriGidebilir,
                        onClick = { sekme.webView.goBack() }
                    )
                    AltButon(
                        etiket = "İleri",
                        simge = { Text("▶", fontSize = 20.sp) },
                        aktif = sekme.ileriGidebilir,
                        onClick = { sekme.webView.goForward() }
                    )
                    AltButon(
                        etiket = "Yenile",
                        simge = { Text("🔄", fontSize = 20.sp) },
                        onClick = { sekme.webView.reload() }
                    )
                    AltButon(
                        etiket = "Ana sayfa",
                        simge = { Text("🏠", fontSize = 20.sp) },
                        onClick = { onAnaSayfa() }
                    )
                    AltButon(
                        etiket = "Sekmeler",
                        simge = { SekmeSayaci(sekmeSayisi) },
                        onClick = { onSekmeler() }
                    )
                }
            }
        }
    ) { icBosluk ->
        Column(modifier = Modifier.padding(icBosluk)) {
            OutlinedTextField(
                value = adresMetni,
                onValueChange = { adresMetni = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                leadingIcon = if (sekme.gizli) {
                    { Text("🕶", fontSize = 18.sp) }
                } else {
                    null
                },
                trailingIcon = {
                    Text(
                        text = if (yerImiVar) "★" else "☆",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onYerImi)
                            .padding(8.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (adresMetni.isNotBlank()) {
                        sekme.webView.loadUrl(UrlYardimci.adresYaDaAramaYap(adresMetni))
                        odak.clearFocus()
                    }
                })
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (istisnaVar) {
                        "🛡 Bu sitede engelleme kapalı"
                    } else {
                        "🛡 ${sekme.engellenen} engellendi"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable(onClick = onGizlilik)
                        .padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (istisnaVar) "Aç" else "Bu sitede kapat",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onIstisna)
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                )
            }

            if (sekme.ilerleme in 1..99) {
                LinearProgressIndicator(
                    progress = { sekme.ilerleme / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            key(sekme.id) {
                AndroidView(
                    factory = { _ ->
                        (sekme.webView.parent as? ViewGroup)?.removeView(sekme.webView)
                        sekme.webView
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}
