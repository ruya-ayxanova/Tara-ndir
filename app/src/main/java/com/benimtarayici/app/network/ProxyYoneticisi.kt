package com.benimtarayici.app.network

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy

// İndirmelerin (HttpURLConnection) kullanacağı proxy.
object AgAyarlari {
    @Volatile
    var indirmeProxy: Proxy? = null
}

data class ProxyAyari(
    val ad: String,
    val tur: String,
    val host: String,
    val port: Int
) {
    // WebView'in beklediği biçim: "http://host:port" veya "socks5://host:port"
    val adres: String
        get() = "$tur://$host:$port"

    val turEtiketi: String
        get() = if (tur == "socks5") "SOCKS5" else "HTTP"
}

class ProxyYoneticisi private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var ornek: ProxyYoneticisi? = null

        fun al(context: Context): ProxyYoneticisi {
            return ornek ?: synchronized(this) {
                ornek ?: ProxyYoneticisi(context.applicationContext).also { ornek = it }
            }
        }
    }

    private val tercihler =
        context.getSharedPreferences("tarayici_proxy", Context.MODE_PRIVATE)

    val liste = mutableStateListOf<ProxyAyari>()

    // -1 = kapalı
    var seciliSira by mutableIntStateOf(-1)
        private set

    var durumMesaji by mutableStateOf("")
        private set

    // Telefondaki WebView bu özelliği destekliyor mu?
    val desteklenir: Boolean by lazy {
        try {
            WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
        } catch (e: Exception) {
            false
        }
    }

    val aktifMi: Boolean
        get() = seciliSira in liste.indices

    val aktifOzet: String
        get() = if (aktifMi) liste[seciliSira].ad else "Kapalı"

    init {
        yukle()
        indirmeProxyAyarla()
    }

    // ---- Liste işlemleri ----

    // Başarılıysa null, hata varsa kullanıcıya gösterilecek mesaj döner.
    fun ekle(ad: String, tur: String, host: String, port: Int): String? {
        val h = host.trim()
        if (h.isEmpty()) return "Sunucu adresi boş olamaz."
        if (!Regex("^[A-Za-z0-9.-]+$").matches(h)) {
            return "Sunucu adresi sadece harf, rakam, nokta ve tire içerebilir (başına http:// yazma)."
        }
        if (port !in 1..65535) return "Port 1 ile 65535 arasında olmalı."
        if (liste.size >= 20) return "En fazla 20 proxy kaydedebilirsin."

        val etiket = ad.trim().ifBlank { "$h:$port" }
        liste.add(ProxyAyari(etiket, if (tur == "socks5") "socks5" else "http", h, port))
        kaydet()
        return null
    }

    // sira = -1 proxy'yi kapatır. Başarılıysa true döner.
    fun sec(sira: Int): Boolean {
        if (sira >= 0 && !desteklenir) {
            durumMesaji = "Bu telefondaki WebView sürümü proxy'yi desteklemiyor. " +
                "Play Store'dan \"Android System WebView\" uygulamasını güncelle."
            return false
        }
        seciliSira = if (sira in liste.indices) sira else -1
        durumMesaji = ""
        kaydet()
        return true
    }

    fun sil(sira: Int) {
        if (sira !in liste.indices) return
        if (sira == seciliSira) {
            seciliSira = -1
        } else if (sira < seciliSira) {
            seciliSira -= 1
        }
        liste.removeAt(sira)
        kaydet()
    }

    // ---- Uygulama ----

    private fun indirmeProxyAyarla() {
        AgAyarlari.indirmeProxy = if (aktifMi) {
            val p = liste[seciliSira]
            val tip = if (p.tur == "socks5") Proxy.Type.SOCKS else Proxy.Type.HTTP
            Proxy(tip, InetSocketAddress.createUnresolved(p.host, p.port))
        } else {
            null
        }
    }

    // Seçili proxy'yi WebView'e ve indirmelere uygular. Bitince (ana iş parçacığında) çağrılır.
    fun uygula(bitince: (() -> Unit)? = null) {
        if (aktifMi && !desteklenir) {
            seciliSira = -1
            kaydet()
            durumMesaji = "WebView sürümün proxy'yi desteklemiyor, proxy kapatıldı."
        }
        indirmeProxyAyarla()
        if (!desteklenir) {
            bitince?.invoke()
            return
        }

        val yurutucu = ContextCompat.getMainExecutor(context)
        try {
            val denetleyici = ProxyController.getInstance()
            if (aktifMi) {
                val yapilandirma = ProxyConfig.Builder()
                    .addProxyRule(liste[seciliSira].adres)
                    .build()
                denetleyici.setProxyOverride(yapilandirma, yurutucu) { bitince?.invoke() }
            } else {
                denetleyici.clearProxyOverride(yurutucu) { bitince?.invoke() }
            }
        } catch (e: Exception) {
            // Uygulanamadıysa ekranda doğru durumu göstermek için kapalıya dön.
            seciliSira = -1
            kaydet()
            indirmeProxyAyarla()
            durumMesaji = "Proxy uygulanamadı, doğrudan bağlantı kullanılıyor: " +
                (e.message ?: e.javaClass.simpleName)
            bitince?.invoke()
        }
    }

    // ---- Kayıt ----

    private fun kaydet() {
        val dizi = JSONArray()
        liste.forEach { p ->
            dizi.put(
                JSONObject()
                    .put("ad", p.ad)
                    .put("tur", p.tur)
                    .put("host", p.host)
                    .put("port", p.port)
            )
        }
        tercihler.edit()
            .putString("liste", dizi.toString())
            .putInt("secili", seciliSira)
            .apply()
    }

    private fun yukle() {
        val metin = tercihler.getString("liste", null) ?: return
        try {
            val dizi = JSONArray(metin)
            for (i in 0 until dizi.length()) {
                val o = dizi.getJSONObject(i)
                liste.add(
                    ProxyAyari(
                        o.getString("ad"),
                        o.getString("tur"),
                        o.getString("host"),
                        o.getInt("port")
                    )
                )
            }
            val kayitliSira = tercihler.getInt("secili", -1)
            seciliSira = if (kayitliSira in liste.indices) kayitliSira else -1
        } catch (e: Exception) {
            liste.clear()
            seciliSira = -1
        }
    }
}
