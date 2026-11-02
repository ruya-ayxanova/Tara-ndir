package com.benimtarayici.app.util

import android.net.Uri

object UrlYardimci {

    // Ayarlardan seçilen arama motorunun adres şablonu.
    var aramaSablonu: String = "https://www.google.com/search?q="

    // Yazılan metin adres ise adrese, değilse arama sayfasına çevirir.
    fun adresYaDaAramaYap(girdi: String): String {
        val metin = girdi.trim()
        if (metin.startsWith("http://") || metin.startsWith("https://")) {
            return metin
        }
        if (!metin.contains(" ") && metin.contains(".")) {
            return "https://$metin"
        }
        return aramaSablonu + Uri.encode(metin)
    }
}
