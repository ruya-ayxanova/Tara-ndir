package com.benimtarayici.app.browser

import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Ekran { ANA, TARAYICI, SEKMELER }

class Sekme(val id: Int, val webView: WebView) {
    var baslik by mutableStateOf("Yeni sekme")
    var adres by mutableStateOf("")
    var ilerleme by mutableIntStateOf(0)
    var geriGidebilir by mutableStateOf(false)
    var ileriGidebilir by mutableStateOf(false)
}

class SekmeYoneticisi(private val context: Context) {

    val sekmeler = mutableStateListOf<Sekme>()
    var aktifId by mutableStateOf<Int?>(null)
    var ekran by mutableStateOf(Ekran.ANA)

    private var sayac = 0

    val aktifSekme: Sekme?
        get() = sekmeler.firstOrNull { it.id == aktifId }

    fun yeniSekme(adres: String) {
        sayac += 1
        val webView = WebView(context)
        val sekme = Sekme(sayac, webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean
            ) {
                if (url != null) sekme.adres = url
                sekme.geriGidebilir = view?.canGoBack() ?: false
                sekme.ileriGidebilir = view?.canGoForward() ?: false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                sekme.ilerleme = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) sekme.baslik = title
            }
        }

        sekme.adres = adres
        webView.loadUrl(adres)

        sekmeler.add(sekme)
        aktifId = sekme.id
        ekran = Ekran.TARAYICI
    }

    fun sekmeyeGec(sekme: Sekme) {
        aktifId = sekme.id
        ekran = Ekran.TARAYICI
    }

    fun sekmeKapat(sekme: Sekme) {
        val sira = sekmeler.indexOf(sekme)
        if (sira < 0) return
        sekmeler.removeAt(sira)
        sekme.webView.destroy()
        if (aktifId == sekme.id) {
            aktifId = sekmeler.getOrNull(minOf(sira, sekmeler.size - 1))?.id
        }
    }

    fun hepsiniYokEt() {
        sekmeler.forEach { it.webView.destroy() }
        sekmeler.clear()
    }
}
