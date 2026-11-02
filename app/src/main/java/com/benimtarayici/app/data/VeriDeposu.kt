package com.benimtarayici.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

data class Kayit(val baslik: String, val adres: String, val zaman: Long)

class VeriDeposu(context: Context) {

    private val tercihler =
        context.getSharedPreferences("tarayici_veri", Context.MODE_PRIVATE)

    val yerImleri = mutableStateListOf<Kayit>()
    val gecmis = mutableStateListOf<Kayit>()

    init {
        yerImleri.addAll(oku("yer_imleri"))
        gecmis.addAll(oku("gecmis"))
    }

    private fun oku(anahtar: String): List<Kayit> {
        val metin = tercihler.getString(anahtar, null) ?: return emptyList()
        return try {
            val dizi = JSONArray(metin)
            (0 until dizi.length()).map { i ->
                val o = dizi.getJSONObject(i)
                Kayit(o.getString("baslik"), o.getString("adres"), o.getLong("zaman"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun yaz(anahtar: String, liste: List<Kayit>) {
        val dizi = JSONArray()
        liste.forEach { k ->
            dizi.put(
                JSONObject()
                    .put("baslik", k.baslik)
                    .put("adres", k.adres)
                    .put("zaman", k.zaman)
            )
        }
        tercihler.edit().putString(anahtar, dizi.toString()).apply()
    }

    // ---- Yer imleri ----

    fun yerImiVarMi(adres: String): Boolean = yerImleri.any { it.adres == adres }

    fun yerImiDegistir(baslik: String, adres: String) {
        val mevcut = yerImleri.firstOrNull { it.adres == adres }
        if (mevcut != null) {
            yerImleri.remove(mevcut)
        } else {
            yerImleri.add(0, Kayit(baslik, adres, System.currentTimeMillis()))
        }
        yaz("yer_imleri", yerImleri)
    }

    fun yerImiSil(kayit: Kayit) {
        yerImleri.remove(kayit)
        yaz("yer_imleri", yerImleri)
    }

    // ---- Geçmiş ----

    fun gecmiseEkle(baslik: String, adres: String) {
        if (gecmis.firstOrNull()?.adres == adres) return
        gecmis.add(0, Kayit(baslik, adres, System.currentTimeMillis()))
        while (gecmis.size > 500) {
            gecmis.removeAt(gecmis.lastIndex)
        }
        yaz("gecmis", gecmis)
    }

    fun gecmistenSil(kayit: Kayit) {
        gecmis.remove(kayit)
        yaz("gecmis", gecmis)
    }

    fun gecmisiTemizle() {
        gecmis.clear()
        yaz("gecmis", gecmis)
    }
}
