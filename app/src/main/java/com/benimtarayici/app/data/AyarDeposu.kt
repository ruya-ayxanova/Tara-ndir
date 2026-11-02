package com.benimtarayici.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    init {
        UrlYardimci.aramaSablonu = aramaMotorlari[aramaMotoruSirasi].sablon
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
}
