package com.benimtarayici.app.browser

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.benimtarayici.app.data.AyarDeposu
import com.benimtarayici.app.data.VeriDeposu
import com.benimtarayici.app.download.Indirme
import com.benimtarayici.app.download.IndirmeYoneticisi
import com.benimtarayici.app.engel.EngelYoneticisi
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

enum class Ekran {
    ANA, TARAYICI, SEKMELER, YER_IMLERI, GECMIS, AYARLAR, INDIRMELER, VIDEO, GIZLILIK
}

class Sekme(val id: Int, val webView: WebView, val gizli: Boolean) {
    var baslik by mutableStateOf(if (gizli) "🕶 Yeni sekme" else "Yeni sekme")
    var adres by mutableStateOf("")
    var ilerleme by mutableIntStateOf(0)
    var geriGidebilir by mutableStateOf(false)
    var ileriGidebilir by mutableStateOf(false)

    // Bu sayfada engellenen istek sayısı.
    var engellenen by mutableIntStateOf(0)
    val engelSayaci = AtomicInteger(0)

    // Arka plan iş parçacığından okunduğu için düz değişken.
    @Volatile
    var sayfaHost: String = ""
}

class SekmeYoneticisi(
    private val context: Context,
    private val depo: VeriDeposu,
    private val ayar: AyarDeposu,
    private val indirmeler: IndirmeYoneticisi,
    private val engel: EngelYoneticisi
) {

    private val masaustuUA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"

    // Boş kalan Google reklam kutularını gizler.
    private val kozmetikJs = """
        (function() {
            var s = document.createElement('style');
            s.textContent = 'ins.adsbygoogle, iframe[src*="doubleclick"], ' +
                'iframe[src*="googlesyndication"], [id^="google_ads_iframe"] ' +
                '{ display: none !important; }';
            if (document.head) document.head.appendChild(s);
        })();
    """.trimIndent()

    val sekmeler = mutableStateListOf<Sekme>()
    var aktifId by mutableStateOf<Int?>(null)
    var ekran by mutableStateOf(Ekran.ANA)
    var gizliMod by mutableStateOf(false)
    var oynatilacak by mutableStateOf<Indirme?>(null)
    var gizlilikOncesi by mutableStateOf(Ekran.AYARLAR)

    private var sayac = 0

    val aktifSekme: Sekme?
        get() = sekmeler.firstOrNull { it.id == aktifId }

    init {
        CookieManager.getInstance().setAcceptCookie(ayar.cerezKabul)
    }

    fun gizlilikAc() {
        gizlilikOncesi = ekran
        ekran = Ekran.GIZLILIK
    }

    private fun hostAl(url: String?): String {
        if (url == null) return ""
        return Uri.parse(url).host ?: ""
    }

    private fun masaustuUygula(webView: WebView) {
        webView.settings.setUserAgentString(
            if (ayar.masaustuSitesi) masaustuUA else null
        )
    }

    // Ayarlarda masaüstü sitesi değişince tüm sekmelere uygulanır.
    fun masaustuModunuUygula() {
        sekmeler.forEach { sekme ->
            masaustuUygula(sekme.webView)
            if (sekme.adres.isNotBlank()) sekme.webView.reload()
        }
    }

    // Çerez ayarları değişince hem genel ayarı hem açık sekmeleri günceller.
    fun cerezAyarlariniUygula() {
        val cerezler = CookieManager.getInstance()
        cerezler.setAcceptCookie(ayar.cerezKabul)
        sekmeler.forEach {
            cerezler.setAcceptThirdPartyCookies(it.webView, ayar.ucuncuTarafCerez)
        }
    }

    // Seçilen tarama verilerini siler.
    fun verileriTemizle(
        gecmis: Boolean,
        cerez: Boolean,
        onbellek: Boolean,
        siteVerisi: Boolean
    ) {
        if (gecmis) {
            depo.gecmisiTemizle()
            sekmeler.forEach {
                it.webView.clearHistory()
                it.geriGidebilir = false
                it.ileriGidebilir = false
            }
        }
        if (cerez) {
            val cerezler = CookieManager.getInstance()
            cerezler.removeAllCookies(null)
            cerezler.flush()
        }
        if (onbellek) {
            // Önbellek tüm WebView'ler için ortak, hangi sekme olduğu fark etmez.
            val webView = sekmeler.firstOrNull()?.webView ?: WebView(context)
            webView.clearCache(true)
            if (sekmeler.isEmpty()) webView.destroy()
        }
        if (siteVerisi) {
            WebStorage.getInstance().deleteAllData()
        }
    }

    fun yeniSekme(adres: String, gizli: Boolean = gizliMod) {
        gizliMod = false
        sayac += 1
        val webView = WebView(context)
        val sekme = Sekme(sayac, webView, gizli)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        if (gizli) {
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        }
        masaustuUygula(webView)
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(webView, ayar.ucuncuTarafCerez)

        // Sayfa bir dosya indirmek isterse indirme yöneticisine devret.
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            indirmeler.baslat(url, userAgent, contentDisposition, mimeType)
        }

        webView.webViewClient = object : WebViewClient() {

            // Her istek önce buraya sorulur. Engellenecekse boş cevap döneriz.
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request == null || request.isForMainFrame) return null
                if (engel.engellenmeliMi(request.url?.host, sekme.sayfaHost)) {
                    sekme.engellenen = sekme.engelSayaci.incrementAndGet()
                    engel.sayacArttir()
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        204,
                        "No Content",
                        emptyMap(),
                        ByteArrayInputStream(ByteArray(0))
                    )
                }
                return null
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                sekme.engelSayaci.set(0)
                sekme.engellenen = 0
                sekme.sayfaHost = hostAl(url)
            }

            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean
            ) {
                if (url != null) {
                    sekme.adres = url
                    sekme.sayfaHost = hostAl(url)
                }
                sekme.geriGidebilir = view?.canGoBack() ?: false
                sekme.ileriGidebilir = view?.canGoForward() ?: false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (ayar.reklamEngelle && !ayar.istisnaVarMi(sekme.sayfaHost)) {
                    view?.evaluateJavascript(kozmetikJs, null)
                }
                if (!gizli && url != null && url.startsWith("http")) {
                    val baslik = view?.title
                    depo.gecmiseEkle(
                        if (baslik.isNullOrBlank()) url else baslik,
                        url
                    )
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                sekme.ilerleme = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    sekme.baslik = (if (gizli) "🕶 " else "") + title
                }
            }
        }

        sekme.adres = adres
        sekme.sayfaHost = hostAl(adres)
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
        // Son gizli sekme kapandıysa ve ayar açıksa verileri sil.
        if (sekme.gizli && ayar.gizliOturumTemizle && sekmeler.none { it.gizli }) {
            verileriTemizle(
                gecmis = false,
                cerez = true,
                onbellek = true,
                siteVerisi = true
            )
        }
    }

    fun hepsiniYokEt() {
        sekmeler.forEach { it.webView.destroy() }
        sekmeler.clear()
    }
}
