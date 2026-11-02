package com.benimtarayici.app.ui.browser

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (adresMetni.isNotBlank()) {
                        sekme.webView.loadUrl(UrlYardimci.adresYaDaAramaYap(adresMetni))
                        odak.clearFocus()
                    }
                })
            )

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
