package com.benimtarayici.app.engel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.benimtarayici.app.data.AyarDeposu
import java.util.concurrent.atomic.AtomicLong

class EngelYoneticisi(context: Context, private val ayar: AyarDeposu) {

    private val tercihler =
        context.getSharedPreferences("tarayici_engel", Context.MODE_PRIVATE)

    private val reklamListesi: Set<String> = listeYukle(context, "reklam_listesi.txt")
    private val izleyiciListesi: Set<String> = listeYukle(context, "izleyici_listesi.txt")
    private val toplam = AtomicLong(tercihler.getLong("toplam", 0L))

    var toplamEngellenen by mutableLongStateOf(toplam.get())
        private set

    val listeBoyutu: Int
        get() = reklamListesi.size + izleyiciListesi.size

    // Dosyadaki her satırı okur. "0.0.0.0 alanadi.com" biçimini de destekler.
    private fun listeYukle(context: Context, dosya: String): Set<String> {
        val sonuc = HashSet<String>()
        try {
            context.assets.open(dosya).bufferedReader().useLines { satirlar ->
                satirlar.forEach { ham ->
                    val satir = ham.substringBefore('#').trim()
                    if (satir.isNotEmpty()) {
                        val alan = satir.split(Regex("\\s+")).last().lowercase()
                        if (alan.contains('.')) sonuc.add(alan)
                    }
                }
            }
        } catch (e: Exception) {
            // Dosya okunamazsa liste boş kalır, tarayıcı yine çalışır.
        }
        return sonuc
    }

    // "a.b.example.com" için sırayla "a.b.example.com", "b.example.com", "example.com" bakılır.
    private fun listedeVarMi(liste: Set<String>, host: String): Boolean {
        var h = host
        while (true) {
            if (liste.contains(h)) return true
            val nokta = h.indexOf('.')
            if (nokta < 0) return false
            h = h.substring(nokta + 1)
        }
    }

    fun engellenmeliMi(istekHost: String?, sayfaHost: String): Boolean {
        if (istekHost.isNullOrBlank()) return false
        if (ayar.istisnaVarMi(sayfaHost)) return false
        val h = istekHost.lowercase()
        if (ayar.reklamEngelle && listedeVarMi(reklamListesi, h)) return true
        if (ayar.izleyiciEngelle && listedeVarMi(izleyiciListesi, h)) return true
        return false
    }

    fun sayacArttir() {
        val yeni = toplam.incrementAndGet()
        toplamEngellenen = yeni
        if (yeni % 25L == 0L) {
            tercihler.edit().putLong("toplam", yeni).apply()
        }
    }

    fun kaydet() {
        tercihler.edit().putLong("toplam", toplam.get()).apply()
    }
}
