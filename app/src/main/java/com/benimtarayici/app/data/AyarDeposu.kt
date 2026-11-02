package com.benimtarayici.app.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.benimtarayici.app.util.UrlYardimci

data class AramaMotoru(val ad: String, val sablon: String)

class AyarDeposu(context: Context) {

    companion object {
        val aramaMotorlari = listOf(
            AramaMotoru("Google", "https://www.google.com/search?q="),
            AramaMotoru("DuckDuckGo", "https://duckduckgo.com/?q="),
            AramaMotoru("Bing", "https://www.bing.com/search?q="),
            AramaMotoru("Yandex", "https://yandex.com.tr/search/?text=")
        )
    }

    private val tercihler =
        context.getSharedPreferences("tarayici_ayarlar", Context.MODE_PRIVATE)

    // 0 = sistem, 1 = açık, 2 = koyu
    var tema by mutableIntStateOf(tercihler.getInt("tema", 0))
        private set

    var aramaMotoruSirasi by mutableIntStateOf(
        tercihler.getInt("arama_motoru", 0).coerceIn(0, aramaMotorlari.lastIndex)
    )
        private set

    var masaustuSitesi by mutableStateOf(tercihler.getBoolean("masaustu", false))
        private set

    var tamEkran by mutableStateOf(tercihler.getBoolean("tam_ekran", false))
        private set

    // Boş ise uygulama klasörü, doluysa seçilen klasörün adresi.
    var indirmeKonumu by mutableStateOf(tercihler.getString("indirme_konumu", "") ?: "")
        private set

    var reklamEngelle by mutableStateOf(tercihler.getBoolean("reklam_engelle", true))
        private set

    var izleyiciEngelle by mutableStateOf(tercihler.getBoolean("izleyici_engelle", true))
        private set

    // Engellemenin kapatıldığı siteler.
    val istisnalar = mutableStateListOf<String>()

    val indirmeKonumuAdi: String
        get() {
            if (indirmeKonumu.isEmpty()) return "Uygulama klasörü (varsayılan)"
            val ad = Uri.parse(indirmeKonumu).lastPathSegment?.substringAfter(":") ?: ""
            return ad.ifBlank { "Seçilen klasör" }
        }

    init {
        UrlYardimci.aramaSablonu = aramaMotorlari[aramaMotoruSirasi].sablon
        val kayitli = tercihler.getStringSet("engel_istisnalari", emptySet()) ?: emptySet()
        istisnalar.addAll(kayitli.sorted())
    }

    fun temaSec(deger: Int) {
        tema = deger
        tercihler.edit().putInt("tema", deger).apply()
    }

    fun aramaMotoruSec(sira: Int) {
        aramaMotoruSirasi = sira
        UrlYardimci.aramaSablonu = aramaMotorlari[sira].sablon
        tercihler.edit().putInt("arama_motoru", sira).apply()
    }

    fun masaustuDegistir(deger: Boolean) {
        masaustuSitesi = deger
        tercihler.edit().putBoolean("masaustu", deger).apply()
    }

    fun tamEkranDegistir(deger: Boolean) {
        tamEkran = deger
        tercihler.edit().putBoolean("tam_ekran", deger).apply()
    }

    fun indirmeKonumuSec(adres: String) {
        indirmeKonumu = adres
        tercihler.edit().putString("indirme_konumu", adres).apply()
    }

    fun reklamEngelleDegistir(deger: Boolean) {
        reklamEngelle = deger
        tercihler.edit().putBoolean("reklam_engelle", deger).apply()
    }

    fun izleyiciEngelleDegistir(deger: Boolean) {
        izleyiciEngelle = deger
        tercihler.edit().putBoolean("izleyici_engelle", deger).apply()
    }

    // ---- Site istisnaları ----

    private fun anahtar(host: String): String = host.lowercase().removePrefix("www.")

    fun istisnaVarMi(host: String): Boolean {
        val h = anahtar(host)
        if (h.isEmpty()) return false
        return istisnalar.any { h == it || h.endsWith(".$it") }
    }

    // Varsa kaldırır, yoksa ekler.
    fun istisnaDegistir(host: String) {
        val h = anahtar(host)
        if (h.isEmpty()) return
        if (istisnaVarMi(h)) {
            istisnalar.removeAll { h == it || h.endsWith(".$it") }
        } else {
            istisnalar.add(h)
        }
        tercihler.edit().putStringSet("engel_istisnalari", istisnalar.toSet()).apply()
    }
}
