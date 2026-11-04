package com.benimtarayici.app.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.benimtarayici.app.util.Sifreleme
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

enum class WgDurum { KAPALI, BAGLANIYOR, ACIK, HATA }

// Uygulama içi WireGuard tüneli. Tünel sadece bu uygulamanın trafiğini kapsar.
class WireGuardYoneticisi private constructor(private val context: Context) {

    companion object {
        private const val ANAHTAR = "yapilandirma"
        private const val OZET = "ozet"

        @Volatile
        private var ornek: WireGuardYoneticisi? = null

        fun al(context: Context): WireGuardYoneticisi {
            return ornek ?: synchronized(this) {
                ornek ?: WireGuardYoneticisi(context.applicationContext).also { ornek = it }
            }
        }
    }

    private val ana = Handler(Looper.getMainLooper())
    private val tercihler =
        context.getSharedPreferences("tarayici_wg", Context.MODE_PRIVATE)

    // Native kütüphane yalnızca gerçekten bağlanırken yüklenir.
    private val backend: GoBackend by lazy { GoBackend(context) }

    private val tunel = object : Tunnel {
        override fun getName(): String = "tarayici"

        override fun onStateChange(newState: Tunnel.State) {
            // Durumu kendimiz yönetiyoruz, burada bir şey yapmaya gerek yok.
        }
    }

    var durum by mutableStateOf(WgDurum.KAPALI)
        private set

    var hata by mutableStateOf("")
        private set

    var yapilandirmaVar by mutableStateOf(tercihler.contains(ANAHTAR))
        private set

    // Sadece "sunucu:port" bilgisi, anahtarlar asla gösterilmez.
    var sunucuOzeti by mutableStateOf(tercihler.getString(OZET, "") ?: "")
        private set

    // ---- Yapılandırma ----

    // Başarılıysa null, hata varsa kullanıcıya gösterilecek mesaj döner.
    fun iceAktar(metin: String): String? {
        if (durum == WgDurum.ACIK || durum == WgDurum.BAGLANIYOR) {
            return "Önce bağlantıyı kes, sonra yeni yapılandırma ekle."
        }
        if (metin.length > 65536) {
            return "Dosya çok büyük, geçerli bir WireGuard yapılandırması değil."
        }

        // 1) Geçerli bir WireGuard yapılandırması mı?
        try {
            Config.parse(ByteArrayInputStream(metin.toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) {
            return "Yapılandırma okunamadı. Dosyanın bir WireGuard .conf dosyası olduğundan emin ol."
        }

        // 2) Şifreleyip kaydet.
        val ozet = ozetCikar(metin)
        try {
            val sifreli = Sifreleme.sifrele(metin)
            tercihler.edit()
                .putString(ANAHTAR, sifreli)
                .putString(OZET, ozet)
                .apply()
        } catch (e: Exception) {
            return "Güvenli depolama kullanılamadı, yapılandırma kaydedilmedi."
        }

        yapilandirmaVar = true
        sunucuOzeti = ozet
        durum = WgDurum.KAPALI
        hata = ""
        return null
    }

    fun sil() {
        if (durum == WgDurum.ACIK || durum == WgDurum.BAGLANIYOR) {
            kes()
        }
        tercihler.edit().remove(ANAHTAR).remove(OZET).apply()
        yapilandirmaVar = false
        sunucuOzeti = ""
        hata = ""
    }

    // ---- Bağlantı ----

    // VPN izni verildikten sonra çağrılmalı.
    fun baglan() {
        if (durum == WgDurum.BAGLANIYOR || durum == WgDurum.ACIK) return
        val sifreli = tercihler.getString(ANAHTAR, null)
        if (sifreli == null) {
            hata = "Önce bir yapılandırma ekle."
            durum = WgDurum.HATA
            return
        }

        durum = WgDurum.BAGLANIYOR
        hata = ""

        Thread {
            try {
                val metin = Sifreleme.coz(sifreli)
                val ozel = uygulamayaOzelYap(metin, context.packageName)
                val yapilandirma = Config.parse(ByteArrayInputStream(ozel.toByteArray(Charsets.UTF_8)))
                backend.setState(tunel, Tunnel.State.UP, yapilandirma)
                ana.post { durum = WgDurum.ACIK }
            } catch (e: Throwable) {
                val mesaj = hataMetni(e)
                ana.post {
                    hata = mesaj
                    durum = WgDurum.HATA
                }
            }
        }.start()
    }

    fun kes() {
        Thread {
            try {
                backend.setState(tunel, Tunnel.State.DOWN, null)
                ana.post {
                    durum = WgDurum.KAPALI
                    hata = ""
                }
            } catch (e: Throwable) {
                val mesaj = hataMetni(e)
                ana.post {
                    hata = mesaj
                    durum = WgDurum.HATA
                }
            }
        }.start()
    }

    // ---- Yardımcılar ----

    // [Interface] bölümüne sadece bu uygulamayı kapsayan satırı ekler,
    // dosyadaki kendi uygulama filtresi satırlarını kaldırır.
    private fun uygulamayaOzelYap(metin: String, paket: String): String {
        val temiz = metin.lines().filterNot { satir ->
            val k = satir.trim().lowercase()
            k.startsWith("includedapplications") || k.startsWith("excludedapplications")
        }
        val sonuc = ArrayList<String>()
        var eklendi = false
        for (satir in temiz) {
            sonuc.add(satir)
            if (!eklendi && satir.trim().equals("[Interface]", ignoreCase = true)) {
                sonuc.add("IncludedApplications = $paket")
                eklendi = true
            }
        }
        if (!eklendi) throw IllegalArgumentException("[Interface] bölümü bulunamadı")
        return sonuc.joinToString("\n")
    }

    private fun ozetCikar(metin: String): String {
        val eslesme = Regex("(?im)^\\s*Endpoint\\s*=\\s*(.+?)\\s*$").find(metin)
        return eslesme?.groupValues?.get(1) ?: "Sunucu adresi bulunamadı"
    }

    private fun hataMetni(e: Throwable): String {
        if (e is UnsatisfiedLinkError) {
            return "Bu telefonun işlemci mimarisi desteklenmiyor."
        }
        if (e.javaClass.simpleName == "BadConfigException") {
            return "Yapılandırma geçersiz."
        }
        return (e.message ?: e.javaClass.simpleName).take(120)
    }
}
