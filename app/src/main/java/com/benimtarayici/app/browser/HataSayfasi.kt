package com.benimtarayici.app.browser

import android.net.Uri

// WebView içinde gösterilen uyarı/hata sayfalarını üretir.
object HataSayfasi {

    const val BASLIK = "Bağlantı sorunu"
    const val GERI = "tarayici-geri://geri"

    // "Güvenli olmayan bağlantıyla devam et" düğmesinin adresi.
    fun devamAdresi(httpAdres: String): String {
        return "tarayici-devam://git?u=" + Uri.encode(httpAdres)
    }

    private fun kacir(metin: String): String {
        return metin
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    fun olustur(
        baslik: String,
        mesaj: String,
        ikincilEtiket: String? = null,
        ikincilAdres: String? = null
    ): String {
        val stil = """
            body{font-family:sans-serif;margin:0;padding:24px;background:#fffbf8;color:#222}
            @media (prefers-color-scheme:dark){body{background:#16120f;color:#eee}}
            .kutu{max-width:420px;margin:48px auto 0}
            .simge{font-size:48px}
            h1{font-size:22px}
            a{display:block;text-align:center;padding:14px;border-radius:24px;margin-top:12px;text-decoration:none;font-weight:bold}
            .birincil{background:#e8590c;color:#fff}
            .ikincil{border:2px solid #e8590c;color:#e8590c}
        """.trimIndent()

        val ikincil = if (ikincilEtiket != null && ikincilAdres != null) {
            "<a class=\"ikincil\" href=\"${kacir(ikincilAdres)}\">${kacir(ikincilEtiket)}</a>"
        } else {
            ""
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>${BASLIK}</title>
            <style>${stil}</style>
            </head>
            <body>
            <div class="kutu">
            <div class="simge">⚠️</div>
            <h1>${kacir(baslik)}</h1>
            <p>${kacir(mesaj)}</p>
            <a class="birincil" href="${GERI}">Geri dön</a>
            ${ikincil}
            </div>
            </body>
            </html>
        """.trimIndent()
    }
}
