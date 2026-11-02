package com.benimtarayici.app.ui.browser

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.benimtarayici.app.util.UrlYardimci

@Composable
fun BrowserScreen(baslangicAdresi: String, onAnaSayfa: () -> Unit) {
    val context = LocalContext.current
    val odak = LocalFocusManager.current

    var adresMetni by remember { mutableStateOf(baslangicAdresi) }
    var ilerleme by remember { mutableIntStateOf(0) }
    var geriGidebilir by remember { mutableStateOf(false) }
    var ileriGidebilir by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    if (url != null) adresMetni = url
                    geriGidebilir = view?.canGoBack() ?: false
                    ileriGidebilir = view?.canGoForward() ?: false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    ilerleme = newProgress
                }
            }

            loadUrl(baslangicAdresi)
        }
    }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    // Telefonun geri tuşu: önce sayfada geri git, gidemiyorsa ana sayfaya dön.
    BackHandler(enabled = true) {
        if (webView.canGoBack()) webView.goBack() else onAnaSayfa()
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
                    AltButon("◀", "Geri", geriGidebilir) { webView.goBack() }
                    AltButon("▶", "İleri", ileriGidebilir) { webView.goForward() }
                    AltButon("🔄", "Yenile", true) { webView.reload() }
                    AltButon("🏠", "Ana sayfa", true) { onAnaSayfa() }
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
                        webView.loadUrl(UrlYardimci.adresYaDaAramaYap(adresMetni))
                        odak.clearFocus()
                    }
                })
            )

            if (ilerleme in 1..99) {
                LinearProgressIndicator(
                    progress = { ilerleme / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AltButon(
    simge: String,
    etiket: String,
    aktif: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .alpha(if (aktif) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = aktif, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = simge, fontSize = 20.sp)
        Text(text = etiket, style = MaterialTheme.typography.labelSmall)
    }
}
