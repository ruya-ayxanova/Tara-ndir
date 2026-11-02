package com.benimtarayici.app.download

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class IndirmeDurumu { INIYOR, DURAKLATILDI, TAMAMLANDI, HATA }

class Indirme(
    val id: Int,
    val adres: String,
    val yol: String,
    val userAgent: String?
) {
    val dosyaAdi: String
        get() = File(yol).name

    var durum by mutableStateOf(IndirmeDurumu.INIYOR)
    var indirilen by mutableLongStateOf(0L)
    var toplam by mutableLongStateOf(-1L)
    var hata by mutableStateOf("")
    var duraklatiliyor by mutableStateOf(false)

    @Volatile
    var duraklatIstendi = false

    @Volatile
    var iptalIstendi = false
}

class IndirmeYoneticisi(private val context: Context) {

    val indirmeler = mutableStateListOf<Indirme>()

    private val ana = Handler(Looper.getMainLooper())
    private val tercihler =
        context.getSharedPreferences("tarayici_indirmeler", Context.MODE_PRIVATE)
    private var sayac = 0

    private val klasor: File
        get() {
            val k = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "indirilenler")
            k.mkdirs()
            return k
        }

    init {
        yukle()
    }

    // ---- Dışarıya açık işlemler ----

    fun baslat(
        adres: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        if (!adres.startsWith("http://") && !adres.startsWith("https://")) {
            Toast.makeText(
                context,
                "Bu tür bağlantı henüz desteklenmiyor",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val ad = URLUtil.guessFileName(adres, contentDisposition, mimeType)
            .replace("/", "_")
        val dosya = benzersizDosya(ad)
        sayac += 1
        val indirme = Indirme(sayac, adres, dosya.absolutePath, userAgent)
        indirmeler.add(0, indirme)
        kaydet()
        Toast.makeText(context, "İndirme başladı: ${dosya.name}", Toast.LENGTH_SHORT).show()
        calistir(indirme)
    }

    fun duraklat(indirme: Indirme) {
        if (indirme.durum == IndirmeDurumu.INIYOR) {
            indirme.duraklatIstendi = true
            indirme.duraklatiliyor = true
        }
    }

    fun devamEt(indirme: Indirme) {
        if (indirme.durum == IndirmeDurumu.DURAKLATILDI ||
            indirme.durum == IndirmeDurumu.HATA
        ) {
            calistir(indirme)
        }
    }

    // Kaydı ve dosyayı siler.
    fun kaldir(indirme: Indirme) {
        if (indirme.durum == IndirmeDurumu.INIYOR) {
            indirme.iptalIstendi = true
            indirme.duraklatiliyor = true
        } else {
            File(indirme.yol).delete()
            indirmeler.remove(indirme)
            kaydet()
        }
    }

    fun hepsiniDuraklat() {
        indirmeler.forEach { if (it.durum == IndirmeDurumu.INIYOR) it.duraklatIstendi = true }
    }

    // ---- İç işlemler ----

    private fun benzersizDosya(ad: String): File {
        var dosya = File(klasor, ad)
        if (!dosya.exists()) return dosya
        val nokta = ad.lastIndexOf('.')
        val govde = if (nokta > 0) ad.substring(0, nokta) else ad
        val uzanti = if (nokta > 0) ad.substring(nokta) else ""
        var n = 1
        while (dosya.exists()) {
            dosya = File(klasor, "$govde ($n)$uzanti")
            n += 1
        }
        return dosya
    }

    private fun calistir(indirme: Indirme) {
        indirme.duraklatIstendi = false
        indirme.iptalIstendi = false
        indirme.duraklatiliyor = false
        indirme.hata = ""
        indirme.durum = IndirmeDurumu.INIYOR

        Thread {
            var baglanti: HttpURLConnection? = null
            var yerel = 0L
            var tamam = false
            var hataMesaji: String? = null

            try {
                val dosya = File(indirme.yol)
                val mevcut = if (dosya.exists()) dosya.length() else 0L

                baglanti = URL(indirme.adres).openConnection() as HttpURLConnection
                baglanti.connectTimeout = 15000
                baglanti.readTimeout = 15000

                val ua = indirme.userAgent
                if (!ua.isNullOrBlank()) baglanti.setRequestProperty("User-Agent", ua)

                val cerez = CookieManager.getInstance().getCookie(indirme.adres)
                if (!cerez.isNullOrBlank()) baglanti.setRequestProperty("Cookie", cerez)

                if (mevcut > 0) baglanti.setRequestProperty("Range", "bytes=$mevcut-")

                val kod = baglanti.responseCode

                if (kod == 416 && mevcut > 0) {
                    // Dosya zaten tamamen inmiş.
                    yerel = mevcut
                    tamam = true
                } else if (kod == HttpURLConnection.HTTP_OK ||
                    kod == HttpURLConnection.HTTP_PARTIAL
                ) {
                    val ekle = kod == HttpURLConnection.HTTP_PARTIAL
                    val baslangic = if (ekle) mevcut else 0L
                    val uzunluk = baglanti.contentLengthLong
                    indirme.toplam = if (uzunluk > 0) uzunluk + baslangic else -1L
                    yerel = baslangic
                    indirme.indirilen = yerel
                    var son = 0L

                    baglanti.inputStream.use { girdi ->
                        FileOutputStream(dosya, ekle).use { cikti ->
                            val tampon = ByteArray(16 * 1024)
                            while (true) {
                                if (indirme.iptalIstendi || indirme.duraklatIstendi) break
                                val n = girdi.read(tampon)
                                if (n < 0) {
                                    tamam = true
                                    break
                                }
                                cikti.write(tampon, 0, n)
                                yerel += n
                                val simdi = System.currentTimeMillis()
                                if (simdi - son > 250) {
                                    indirme.indirilen = yerel
                                    son = simdi
                                }
                            }
                        }
                    }
                    indirme.indirilen = yerel
                } else {
                    throw IOException("Sunucu hatası: $kod")
                }
            } catch (e: Exception) {
                hataMesaji = e.message ?: e.javaClass.simpleName
            } finally {
                baglanti?.disconnect()
            }

            val hataSon = hataMesaji
            val tamamSon = tamam
            val yerelSon = yerel

            ana.post {
                indirme.duraklatiliyor = false
                when {
                    indirme.iptalIstendi -> {
                        File(indirme.yol).delete()
                        indirmeler.remove(indirme)
                    }

                    hataSon != null -> {
                        indirme.hata = hataSon
                        indirme.durum = IndirmeDurumu.HATA
                    }

                    tamamSon -> {
                        if (indirme.toplam <= 0) indirme.toplam = yerelSon
                        indirme.indirilen = yerelSon
                        indirme.durum = IndirmeDurumu.TAMAMLANDI
                    }

                    else -> {
                        indirme.durum = IndirmeDurumu.DURAKLATILDI
                    }
                }
                kaydet()
            }
        }.start()
    }

    private fun kaydet() {
        val dizi = JSONArray()
        indirmeler.forEach { i ->
            dizi.put(
                JSONObject()
                    .put("id", i.id)
                    .put("adres", i.adres)
                    .put("yol", i.yol)
                    .put("durum", i.durum.name)
                    .put("toplam", i.toplam)
                    .put("ua", i.userAgent ?: "")
            )
        }
        tercihler.edit().putString("liste", dizi.toString()).apply()
    }

    private fun yukle() {
        val metin = tercihler.getString("liste", null) ?: return
        try {
            val dizi = JSONArray(metin)
            for (s in 0 until dizi.length()) {
                val o = dizi.getJSONObject(s)
                val yol = o.getString("yol")
                val ua = o.optString("ua")
                val indirme = Indirme(
                    o.getInt("id"),
                    o.getString("adres"),
                    yol,
                    if (ua.isBlank()) null else ua
                )
                val bitti = o.getString("durum") == IndirmeDurumu.TAMAMLANDI.name
                indirme.durum = if (bitti) IndirmeDurumu.TAMAMLANDI else IndirmeDurumu.DURAKLATILDI
                indirme.toplam = o.optLong("toplam", -1L)
                val dosya = File(yol)
                indirme.indirilen = if (dosya.exists()) dosya.length() else 0L
                indirmeler.add(indirme)
                if (indirme.id > sayac) sayac = indirme.id
            }
        } catch (e: Exception) {
            // Bozuk kayıt varsa liste boş başlar.
        }
    }
}
