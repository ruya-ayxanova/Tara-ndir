package com.benimtarayici.app.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import com.benimtarayici.app.network.AgDurumu
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

enum class Ekran {
    ANA, TARAYICI, SEKMELER, YER_IMLERI, GECMIS, AYARLAR, INDIRMELER, VIDEO, GIZLILIK, VPN
}

// Sayfada bulunan, doğrudan dosya olarak sunulan bir video.
data class VideoAdayi(val url: String, val ad: String)

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

    // https'e çevrilen http adresinin orijinali (hata olursa sormak için).
    var yukseltilenHttp: String? = null
    var sslHatasi: Boolean = false

    // Hata sayfası gösteriliyor mu / en son hata veren adres.
    // Arka plan iş parçacığından da yazıldığı için @Volatile.
    @Volatile
    var hataVar: Boolean = false

    @Volatile
    var sonHataAdresi: String? = null

    // Sekme kapatıldı mı? (kapatılmış WebView'e dokunmamak için)
    @Volatile
    var kapali: Boolean = false

    // ---- Video algılama ----
    val videolar = mutableStateListOf<VideoAdayi>()

    // Sayfada parçalı akış (blob / m3u8 / mpd) olarak sunulan, indirilemeyen video var mı?
    var akisVideoVar by mutableStateOf(false)

    // Sadece ana iş parçacığından çağrılmalı.
    fun videoEkle(url: String) {
        val temiz = url.substringBefore('#')
        if (!temiz.startsWith("http")) return
        if (videolar.any { it.url == temiz }) return
        if (videolar.size >= 20) return
        val uri = Uri.parse(temiz)
        val ad = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: (uri.host ?: "video")
        videolar.add(VideoAdayi(temiz, ad))
    }
}

// Onay bekleyen riskli indirme.
class BekleyenIndirme(
    val adres: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
    val dosyaAdi: String,
    val nedenler: List<String>
)

class SekmeYoneticisi(
    private val context: Context,
    private val depo: VeriDeposu,
    private val ayar: AyarDeposu,
    private val indirmeler: IndirmeYoneticisi,
    private val engel: EngelYoneticisi,
    private val ag: AgDurumu
) {

    private val ana = Handler(Looper.getMainLooper())

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

    // Sayfadaki <video> öğelerinin kaynaklarını toplar.
    private val videoTaramaJs = """
        (function() {
            var sonuc = { u: [], akis: false };
            function ekle(s) {
                if (!s) return;
                if (s.indexOf('blob:') === 0 || /\.(m3u8|mpd)(\?|#|${'$'})/i.test(s)) {
                    sonuc.akis = true;
                    return;
                }
                try { s = new URL(s, document.baseURI).href; } catch (e) { return; }
                if (sonuc.u.indexOf(s) < 0) sonuc.u.push(s);
            }
            var v = document.querySelectorAll('video');
            for (var i = 0; i < v.length; i++) {
                ekle(v[i].currentSrc || v[i].src);
                var k = v[i].querySelectorAll('source');
                for (var j = 0; j < k.length; j++) ekle(k[j].src);
            }
            return sonuc;
        })();
    """.trimIndent()

    private val videoUzantilari = setOf("mp4", "webm", "m4v", "mov", "mkv", "3gp")

    private val riskliUzantilar = setOf(
        "apk", "xapk", "apks", "exe", "msi", "bat", "cmd", "scr",
        "jar", "vbs", "ps1", "sh", "dll", "lnk", "hta", "dex"
    )

    // Kullanıcının bu oturumda http ile devam etmeyi onayladığı siteler.
    private val httpIzinli = HashSet<String>()

    val sekmeler = mutableStateListOf<Sekme>()
    var aktifId by mutableStateOf<Int?>(null)
    var ekran by mutableStateOf(Ekran.ANA)
    var gizliMod by mutableStateOf(false)
    var oynatilacak by mutableStateOf<Indirme?>(null)
    var gizlilikOncesi by mutableStateOf(Ekran.AYARLAR)
    var onayBekleyen by mutableStateOf<BekleyenIndirme?>(null)

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

    // VPN zorunlu modu açık ve VPN yoksa true döner: hiçbir istek çıkmamalı.
    fun vpnEngeli(): Boolean = ag.vpnZorunlu && !ag.vpnBagli

    private fun hostAl(url: String?): String {
        if (url == null) return ""
        return Uri.parse(url).host ?: ""
    }

    private fun bosCevap(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            204,
            "No Content",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )
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

    // ---- Bağlantı ----

    // İnternet veya VPN geri gelince, hata sayfasında kalan aktif sekmeyi yeniden dener.
    fun baglantiGeldi() {
        val sekme = aktifSekme ?: return
        val adres = sekme.sonHataAdresi ?: return
        if (sekme.webView.title == HataSayfasi.BASLIK) {
            sekme.hataVar = false
            sekme.sslHatasi = false
            sekme.webView.loadUrl(yuksel(sekme, adres))
        }
    }

    // Hata koduna göre kullanıcıya gösterilecek başlık ve açıklama.
    private fun hataMetni(kod: Int): Pair<String, String> {
        if (!ag.internetVar) {
            return Pair(
                "İnternet bağlantısı yok",
                "Wi-Fi veya mobil veriyi kontrol et. Bağlantı gelince sayfa otomatik yenilenir."
            )
        }
        val mesaj = when (kod) {
            WebViewClient.ERROR_HOST_LOOKUP ->
                "Site bulunamadı. Adresi kontrol et veya biraz sonra tekrar dene."

            WebViewClient.ERROR_CONNECT ->
                "Siteye bağlanılamadı. Site kapalı olabilir veya bağlantın engelleniyor olabilir."

            WebViewClient.ERROR_TIMEOUT ->
                "Bağlantı zaman aşımına uğradı. Site yavaş yanıt veriyor olabilir."

            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE ->
                "Güvenli bağlantı kurulamadı."

            WebViewClient.ERROR_REDIRECT_LOOP ->
                "Sayfa sürekli başka bir adrese yönlendiriyor."

            WebViewClient.ERROR_BAD_URL ->
                "Adres geçersiz görünüyor."

            WebViewClient.ERROR_UNSUPPORTED_SCHEME ->
                "Bu tür bağlantı desteklenmiyor."

            else ->
                "Bilinmeyen bir bağlantı hatası oluştu."
        }
        return Pair("Sayfa açılamadı", mesaj)
    }

    // ---- HTTPS ----

    // http adresini https'e çevirir (site için izin verilmediyse).
    // Çevrildiyse orijinali sekmeye not eder.
    private fun yuksel(sekme: Sekme, adres: String): String {
        val uri = Uri.parse(adres)
        if (uri.scheme == "http" && !httpIzinli.contains(uri.host ?: "")) {
            sekme.yukseltilenHttp = adres
            return uri.buildUpon().scheme("https").build().toString()
        }
        return adres
    }

    // Adres çubuğundan veya dışarıdan gelen adresi güvenli şekilde açar.
    fun adresAc(sekme: Sekme, adres: String) {
        sekme.hataVar = false
        sekme.sslHatasi = false
        sekme.webView.loadUrl(yuksel(sekme, adres))
    }

    // ---- Video algılama ----

    private fun videoTara(sekme: Sekme) {
        if (sekme.kapali) return
        sekme.webView.evaluateJavascript(videoTaramaJs) { ham ->
            videoSonucuIsle(sekme, ham)
        }
    }

    private fun videoSonucuIsle(sekme: Sekme, ham: String?) {
        if (ham == null || ham == "null") return
        try {
            val sonuc = JSONObject(ham)
            val dizi = sonuc.getJSONArray("u")
            for (i in 0 until dizi.length()) {
                sekme.videoEkle(dizi.getString(i))
            }
            if (sonuc.optBoolean("akis")) sekme.akisVideoVar = true
        } catch (e: Exception) {
            // Beklenmeyen biçim gelirse sessizce yok say.
        }
    }

    // Kullanıcının seçtiği videoyu normal indirme güvenlik kontrolüne gönderir.
    fun videoIndir(sekme: Sekme, video: VideoAdayi) {
        val ua = sekme.webView.settings.userAgentString
        indirmeIstegi(video.url, ua, null, null)
    }

    // ---- İndirme güvenliği ----

    private fun riskNedenleri(url: String, dosyaAdi: String): List<String> {
        val nedenler = mutableListOf<String>()
        val uzanti = dosyaAdi.substringAfterLast('.', "").lowercase()
        if (uzanti in riskliUzantilar) {
            nedenler.add("\".$uzanti\" dosyaları cihazına zarar verebilecek programlar olabilir.")
        }
        if (url.startsWith("http://")) {
            nedenler.add("Bağlantı şifrelenmemiş (http). Dosya yolda değiştirilmiş olabilir.")
        }
        return nedenler
    }

    private fun indirmeIstegi(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        if (vpnEngeli()) {
            Toast.makeText(
                context,
                "VPN zorunlu modu açık ve VPN bağlı değil. İndirme başlatılmadı.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val ad = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val nedenler = riskNedenleri(url, ad)
        if (nedenler.isEmpty()) {
            indirmeler.baslat(url, userAgent, contentDisposition, mimeType)
        } else {
            onayBekleyen = BekleyenIndirme(
                url, userAgent, contentDisposition, mimeType, ad, nedenler
            )
        }
    }

    fun indirmeyiOnayla() {
        val bekleyen = onayBekleyen ?: return
        onayBekleyen = null
        if (vpnEngeli()) {
            Toast.makeText(
                context,
                "VPN zorunlu modu açık ve VPN bağlı değil. İndirme başlatılmadı.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        indirmeler.baslat(
            bekleyen.adres,
            bekleyen.userAgent,
            bekleyen.contentDisposition,
            bekleyen.mimeType
        )
    }

    fun indirmeyiReddet() {
        onayBekleyen = null
    }

    // ---- Sekmeler ----

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
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        if (gizli) {
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        }
        masaustuUygula(webView)
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(webView, ayar.ucuncuTarafCerez)

        // Sayfa bir dosya indirmek isterse önce güvenlik kontrolünden geçir.
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            indirmeIstegi(url, userAgent, contentDisposition, mimeType)
        }

        webView.webViewClient = object : WebViewClient() {

            // Bağlantı tıklamaları ve yönlendirmeler burada kontrol edilir.
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request == null) return false
                val url = request.url
                val sema = url.scheme ?: return false

                // Hata sayfasındaki "Geri dön" düğmesi.
                if (sema == "tarayici-geri") {
                    if (view != null && view.canGoBack()) {
                        view.goBack()
                    } else {
                        view?.loadUrl("about:blank")
                    }
                    return true
                }

                // Hata sayfasındaki "Güvenli olmayan bağlantıyla devam et" düğmesi.
                if (sema == "tarayici-devam") {
                    val hedef = url.getQueryParameter("u")
                    if (hedef != null && view != null) {
                        sekme.hataVar = false
                        sekme.sslHatasi = false
                        httpIzinli.add(Uri.parse(hedef).host ?: "")
                        view.loadUrl(hedef)
                    }
                    return true
                }

                // Hata sayfasındaki "Tekrar dene" düğmesi.
                if (sema == "tarayici-tekrar") {
                    val hedef = url.getQueryParameter("u")
                    if (hedef != null && view != null) {
                        sekme.hataVar = false
                        sekme.sslHatasi = false
                        view.loadUrl(yuksel(sekme, hedef))
                    }
                    return true
                }

                // E-posta, telefon ve SMS bağlantıları ilgili uygulamada açılır.
                if (sema == "mailto" || sema == "tel" || sema == "sms") {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, url)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        // Açabilecek uygulama yoksa bir şey yapma.
                    }
                    return true
                }

                if (request.isForMainFrame) {
                    sekme.hataVar = false
                    sekme.sslHatasi = false
                    if (sema == "http") {
                        val yeni = yuksel(sekme, url.toString())
                        if (yeni != url.toString()) {
                            view?.loadUrl(yeni)
                            return true
                        }
                    }
                }
                return false
            }

            // Her istek önce buraya sorulur: VPN kontrolü, reklam/izleyici engelleme, video algılama.
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request == null) return null

                // VPN zorunlu modunda VPN yoksa hiçbir istek dışarı çıkmaz.
                if (vpnEngeli()) {
                    if (request.isForMainFrame) {
                        val adres = request.url.toString()
                        sekme.hataVar = true
                        sekme.sonHataAdresi = adres
                        val html = HataSayfasi.olustur(
                            baslik = "VPN bağlı değil",
                            mesaj = "VPN zorunlu modu açık, bu yüzden VPN bağlanana kadar " +
                                "sayfalar açılmaz. VPN'e bağlandığında sayfa otomatik yenilenir.",
                            tekrarAdresi = adres
                        )
                        return WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            ByteArrayInputStream(html.toByteArray(Charsets.UTF_8))
                        )
                    }
                    return bosCevap()
                }

                if (request.isForMainFrame) return null

                if (engel.engellenmeliMi(request.url?.host, sekme.sayfaHost)) {
                    sekme.engellenen = sekme.engelSayaci.incrementAndGet()
                    engel.sayacArttir()
                    return bosCevap()
                }

                // Engellenmeyen, doğrudan video dosyası isteklerini aday olarak not et.
                val yol = request.url.path?.lowercase() ?: ""
                if (yol.substringAfterLast('.', "") in videoUzantilari) {
                    val adres = request.url.toString()
                    ana.post { sekme.videoEkle(adres) }
                }
                return null
            }

            // Sayfa açılamazsa anlaşılır bir hata sayfası göster.
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (view == null || request == null || !request.isForMainFrame) return
                sekme.hataVar = true
                val adres = request.url.toString()
                sekme.sonHataAdresi = adres

                // Sertifika hatası sayfası zaten gösterildi.
                if (sekme.sslHatasi) return

                // https'e çevirdiğimiz adres açılmadıysa güvenli olmayan devam seçeneği sun.
                val orijinal = sekme.yukseltilenHttp
                if (orijinal != null && ag.internetVar &&
                    request.url.host == Uri.parse(orijinal).host
                ) {
                    sekme.yukseltilenHttp = null
                    val html = HataSayfasi.olustur(
                        baslik = "Güvenli bağlantı kurulamadı",
                        mesaj = "Bu site HTTPS ile açılamadı. Devam edersen gönderdiğin ve " +
                            "aldığın bilgiler şifrelenmeden iletilir. Şifre veya kart bilgisi girme.",
                        ikincilEtiket = "Güvenli olmayan bağlantıyla devam et",
                        ikincilAdres = HataSayfasi.devamAdresi(orijinal)
                    )
                    view.loadDataWithBaseURL(adres, html, "text/html", "UTF-8", adres)
                    return
                }

                val (baslik, mesaj) = hataMetni(error?.errorCode ?: WebViewClient.ERROR_UNKNOWN)
                val html = HataSayfasi.olustur(
                    baslik = baslik,
                    mesaj = mesaj,
                    tekrarAdresi = adres
                )
                view.loadDataWithBaseURL(adres, html, "text/html", "UTF-8", adres)
            }

            // Sertifika hatalarında sayfa açılmaz, devam seçeneği de sunulmaz.
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                if (view == null || error == null) return
                if (Uri.parse(error.url).host != sekme.sayfaHost) return

                sekme.hataVar = true
                sekme.sslHatasi = true
                val sebep = when (error.primaryError) {
                    SslError.SSL_EXPIRED -> "Sitenin sertifikasının süresi dolmuş."
                    SslError.SSL_IDMISMATCH -> "Sertifika bu alan adı için verilmemiş."
                    SslError.SSL_NOTYETVALID -> "Sertifika henüz geçerli değil."
                    SslError.SSL_UNTRUSTED -> "Sertifikayı veren kurum güvenilir değil."
                    else -> "Sertifika doğrulanamadı."
                }
                val html = HataSayfasi.olustur(
                    baslik = "Bağlantı güvenli değil",
                    mesaj = "$sebep Güvenliğin için sayfa açılmadı."
                )
                view.loadDataWithBaseURL(error.url, html, "text/html", "UTF-8", error.url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                sekme.engelSayaci.set(0)
                sekme.engellenen = 0
                sekme.sayfaHost = hostAl(url)
                // Yeni sayfa: eski sayfanın videoları geçersiz.
                sekme.videolar.clear()
                sekme.akisVideoVar = false
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
                // Hata sayfaları geçmişe yazılmaz.
                if (sekme.hataVar || view?.title == HataSayfasi.BASLIK) return

                // Sayfa sorunsuz açıldı, http yedeği artık gerekmez.
                sekme.yukseltilenHttp = null

                if (ayar.reklamEngelle && !ayar.istisnaVarMi(sekme.sayfaHost)) {
                    view?.evaluateJavascript(kozmetikJs, null)
                }

                // Videoları tara; oynatıcılar geç yüklenebildiği için birkaç kez dene.
                videoTara(sekme)
                ana.postDelayed({ videoTara(sekme) }, 1500)
                ana.postDelayed({ videoTara(sekme) }, 4000)

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

        val yuklenecek = yuksel(sekme, adres)
        sekme.adres = yuklenecek
        sekme.sayfaHost = hostAl(yuklenecek)
        webView.loadUrl(yuklenecek)

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
        sekme.kapali = true
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
        sekmeler.forEach {
            it.kapali = true
            it.webView.destroy()
        }
        sekmeler.clear()
    }
}
