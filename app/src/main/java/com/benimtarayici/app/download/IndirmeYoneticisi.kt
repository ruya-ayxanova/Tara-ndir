package com.benimtarayici.app.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
    val userAgent: String?,
    val hedefKlasor: String?
) {
    val dosyaAdi: String
        get() = File(yol).name

    var durum by mutableStateOf(IndirmeDurumu.INIYOR)
    var indirilen by mutableLongStateOf(0L)
    var toplam by mutableLongStateOf(-1L)
    var hata by mutableStateOf("")
    var duraklatiliyor by mutableStateOf(false)
    var kopyalaniyor by mutableStateOf(false)
    var hedefUri by mutableStateOf<String?>(null)

    val mime: String
        get() {
            val uzanti = dosyaAdi.substringAfterLast('.', "").lowercase()
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(uzanti)
                ?: "application/octet-stream"
        }

    val videoMu: Boolean
        get() = mime.startsWith("video/")

    @Volatile
    var duraklatIstendi = false

    @Volatile
    var iptalIstendi = false
}

class IndirmeYoneticisi private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var ornek: IndirmeYoneticisi? = null

        // Uygulama boyunca tek bir yönetici kullanılır.
        fun al(context: Context): IndirmeYoneticisi {
            return ornek ?: synchronized(this) {
                ornek ?: IndirmeYoneticisi(context.applicationContext).also { ornek = it }
            }
        }

        fun mevcut(): IndirmeYoneticisi? = ornek
    }

    val indirmeler = mutableStateListOf<Indirme>()

    // Activity bildirim izni istemek için bunu doldurur.
    var izinIste: (() -> Unit)? = null

    private val ana = Handler(Looper.getMainLooper())
    private val tercihler =
        context.getSharedPreferences("tarayici_indirmeler", Context.MODE_PRIVATE)
    private val ayarTercihleri =
        context.getSharedPreferences("tarayici_ayarlar", Context.MODE_PRIVATE)
    private var sayac = 0

    private val klasor: File
        get() {
            val k = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "indirilenler")
            k.mkdirs()
            return k
        }

    init {
        IndirmeBildirim.kanalOlustur(context)
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
        izinIste?.invoke()

        val ad = URLUtil.guessFileName(adres, contentDisposition, mimeType)
            .replace("/", "_")
        val dosya = benzersizDosya(ad)
        val secilen = ayarTercihleri.getString("indirme_konumu", "") ?: ""
        sayac += 1
        val indirme = Indirme(
            sayac,
            adres,
            dosya.absolutePath,
            userAgent,
            if (secilen.isBlank()) null else secilen
        )
        indirmeler.add(0, indirme)
        kaydet()
        Toast.makeText(context, "İndirme başladı: ${dosya.name}", Toast.LENGTH_SHORT).show()
        calistir(indirme)
    }

    fun duraklat(indirme: Indirme) {
        if (indirme.durum == IndirmeDurumu.INIYOR && !indirme.kopyalaniyor) {
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
            if (indirme.kopyalaniyor) return
            indirme.iptalIstendi = true
            indirme.duraklatiliyor = true
        } else {
            File(indirme.yol).delete()
            val uri = indirme.hedefUri
            if (uri != null) {
                try {
                    DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(uri))
                } catch (e: Exception) {
                    // Dosya zaten silinmiş olabilir.
                }
            }
            indirmeler.remove(indirme)
            kaydet()
        }
    }

    // Dosyayı açmak için kullanılacak adres.
    fun acmaUri(indirme: Indirme): Uri {
        val uri = indirme.hedefUri
        return if (uri != null) {
            Uri.parse(uri)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(indirme.yol)
            )
        }
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

    private fun servisiBaslat() {
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, IndirmeServisi::class.java)
            )
        } catch (e: Exception) {
            // Servis başlatılamazsa indirme yine uygulama açıkken sürer.
        }
    }

    // Tamamlanan dosyayı kullanıcının seçtiği klasöre kopyalar.
    private fun klasoreKopyala(indirme: Indirme, secilenKlasor: String): String {
        val agac = Uri.parse(secilenKlasor)
        val ust = DocumentsContract.buildDocumentUriUsingTree(
            agac,
            DocumentsContract.getTreeDocumentId(agac)
        )
        val kaynak = File(indirme.yol)
        val hedef = DocumentsContract.createDocument(
            context.contentResolver,
            ust,
            indirme.mime,
            indirme.dosyaAdi
        ) ?: throw IOException("Hedef dosya oluşturulamadı")

        val cikti = context.contentResolver.openOutputStream(hedef)
            ?: throw IOException("Hedef dosya açılamadı")
        cikti.use { yazici ->
            kaynak.inputStream().use { girdi -> girdi.copyTo(yazici) }
        }
        kaynak.delete()
        return hedef.toString()
    }

    private fun calistir(indirme: Indirme) {
        indirme.duraklatIstendi = false
        indirme.iptalIstendi = false
        indirme.duraklatiliyor = false
        indirme.hata = ""
        indirme.durum = IndirmeDurumu.INIYOR
        servisiBaslat()

        Thread {
            var baglanti: HttpURLConnection? = null
            var yerel = 0L
            var tamam = false
            var hataMesaji: String? = null

            try {
                val dosya = File(indirme.yol)
                val mevcut = if (dosya.exists()) dosya.length() else 0L

                val proxy = com.benimtarayici.app.network.AgAyarlari.indirmeProxy
baglanti = (if (proxy != null) {
    URL(indirme.adres).openConnection(proxy)
} else {
    URL(indirme.adres).openConnection()
}) as HttpURLConnection
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

            // İndirme bittiyse ve klasör seçilmişse kopyala.
            val secilenKlasor = indirme.hedefKlasor
            if (tamam && hataMesaji == null && !indirme.iptalIstendi &&
                secilenKlasor != null && indirme.hedefUri == null
            ) {
                indirme.kopyalaniyor = true
                try {
                    indirme.hedefUri = klasoreKopyala(indirme, secilenKlasor)
                } catch (e: Exception) {
                    hataMesaji = "Klasöre kopyalanamadı: " +
                        (e.message ?: e.javaClass.simpleName)
                }
                indirme.kopyalaniyor = false
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
                        IndirmeBildirim.tamamlandi(context, indirme.dosyaAdi, indirme.id)
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
                    .put("klasor", i.hedefKlasor ?: "")
                    .put("uri", i.hedefUri ?: "")
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
                val hedefKlasor = o.optString("klasor")
                val hedefUri = o.optString("uri")
                val indirme = Indirme(
                    o.getInt("id"),
                    o.getString("adres"),
                    yol,
                    if (ua.isBlank()) null else ua,
                    if (hedefKlasor.isBlank()) null else hedefKlasor
                )
                if (hedefUri.isNotBlank()) indirme.hedefUri = hedefUri

                val bitti = o.getString("durum") == IndirmeDurumu.TAMAMLANDI.name
                indirme.durum = if (bitti) IndirmeDurumu.TAMAMLANDI else IndirmeDurumu.DURAKLATILDI
                indirme.toplam = o.optLong("toplam", -1L)
                val dosya = File(yol)
                indirme.indirilen = if (bitti && indirme.toplam > 0) {
                    indirme.toplam
                } else if (dosya.exists()) {
                    dosya.length()
                } else {
                    0L
                }
                indirmeler.add(indirme)
                if (indirme.id > sayac) sayac = indirme.id
            }
        } catch (e: Exception) {
            // Bozuk kayıt varsa liste boş başlar.
        }
    }
}
