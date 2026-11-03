package com.benimtarayici.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.benimtarayici.app.browser.Ekran
import com.benimtarayici.app.browser.SekmeYoneticisi
import com.benimtarayici.app.data.AyarDeposu
import com.benimtarayici.app.data.VeriDeposu
import com.benimtarayici.app.download.Indirme
import com.benimtarayici.app.download.IndirmeDurumu
import com.benimtarayici.app.download.IndirmeYoneticisi
import com.benimtarayici.app.engel.EngelYoneticisi
import com.benimtarayici.app.ui.browser.BrowserScreen
import com.benimtarayici.app.ui.downloads.IndirmelerScreen
import com.benimtarayici.app.ui.home.HomeScreen
import com.benimtarayici.app.ui.lists.KayitListesiScreen
import com.benimtarayici.app.ui.player.VideoScreen
import com.benimtarayici.app.ui.privacy.GizlilikScreen
import com.benimtarayici.app.ui.settings.AyarlarScreen
import com.benimtarayici.app.ui.tabs.SekmelerScreen
import com.benimtarayici.app.ui.theme.TarayiciTema

class MainActivity : ComponentActivity() {

    private lateinit var depo: VeriDeposu
    private lateinit var ayar: AyarDeposu
    private lateinit var indirmeler: IndirmeYoneticisi
    private lateinit var engel: EngelYoneticisi
    private lateinit var yonetici: SekmeYoneticisi

    private val bildirimIzni =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        depo = VeriDeposu(this)
        ayar = AyarDeposu(this)
        indirmeler = IndirmeYoneticisi.al(this)
        engel = EngelYoneticisi(this, ayar)
        yonetici = SekmeYoneticisi(this, depo, ayar, indirmeler, engel)

        // İlk indirmede bildirim izni iste (Android 13 ve üstü).
        indirmeler.izinIste = {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                bildirimIzni.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LaunchedEffect(ayar.tamEkran) {
                tamEkranUygula(ayar.tamEkran)
            }

            TarayiciTema(tema = ayar.tema) {

                // Riskli indirme uyarısı (her ekranda görünebilir).
                val bekleyen = yonetici.onayBekleyen
                if (bekleyen != null) {
                    AlertDialog(
                        onDismissRequest = { yonetici.indirmeyiReddet() },
                        title = { Text("İndirme uyarısı") },
                        text = {
                            Text(
                                bekleyen.dosyaAdi + "\n\n" +
                                    bekleyen.nedenler.joinToString("\n\n") { "• $it" }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { yonetici.indirmeyiOnayla() }) {
                                Text("Yine de indir")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { yonetici.indirmeyiReddet() }) {
                                Text("Vazgeç")
                            }
                        }
                    )
                }

                when (yonetici.ekran) {
                    Ekran.ANA -> HomeScreen(
                        sekmeSayisi = yonetici.sekmeler.size,
                        gizliMod = yonetici.gizliMod,
                        onAdresAc = { yonetici.yeniSekme(it) },
                        onSekmeler = { yonetici.ekran = Ekran.SEKMELER },
                        onGizliDegistir = { yonetici.gizliMod = !yonetici.gizliMod },
                        onYerImleri = { yonetici.ekran = Ekran.YER_IMLERI },
                        onGecmis = { yonetici.ekran = Ekran.GECMIS },
                        onIndirmeler = { yonetici.ekran = Ekran.INDIRMELER },
                        onAyarlar = { yonetici.ekran = Ekran.AYARLAR }
                    )

                    Ekran.TARAYICI -> {
                        val sekme = yonetici.aktifSekme
                        if (sekme != null) {
                            val host = Uri.parse(sekme.adres).host ?: ""
                            BrowserScreen(
                                sekme = sekme,
                                sekmeSayisi = yonetici.sekmeler.size,
                                yerImiVar = depo.yerImiVarMi(sekme.adres),
                                istisnaVar = ayar.istisnaVarMi(host),
                                onAdresGit = { yonetici.adresAc(sekme, it) },
                                onYerImi = {
                                    depo.yerImiDegistir(
                                        sekme.baslik.removePrefix("🕶 "),
                                        sekme.adres
                                    )
                                },
                                onIstisna = {
                                    ayar.istisnaDegistir(host)
                                    sekme.webView.reload()
                                },
                                onGizlilik = { yonetici.gizlilikAc() },
                                onAnaSayfa = { yonetici.ekran = Ekran.ANA },
                                onSekmeler = { yonetici.ekran = Ekran.SEKMELER }
                            )
                        }
                    }

                    Ekran.SEKMELER -> SekmelerScreen(
                        sekmeler = yonetici.sekmeler,
                        aktifId = yonetici.aktifId,
                        onSec = { yonetici.sekmeyeGec(it) },
                        onKapat = { yonetici.sekmeKapat(it) },
                        onYeni = { yonetici.ekran = Ekran.ANA },
                        onGeri = {
                            yonetici.ekran = if (yonetici.aktifSekme != null) {
                                Ekran.TARAYICI
                            } else {
                                Ekran.ANA
                            }
                        }
                    )

                    Ekran.YER_IMLERI -> KayitListesiScreen(
                        baslik = "Yer imleri",
                        kayitlar = depo.yerImleri,
                        bosMesaj = "Henüz yer imi yok. Bir sayfada adres çubuğundaki ☆ işaretine dokun.",
                        onAc = { yonetici.yeniSekme(it.adres, false) },
                        onSil = { depo.yerImiSil(it) },
                        onGeri = { yonetici.ekran = Ekran.ANA }
                    )

                    Ekran.GECMIS -> KayitListesiScreen(
                        baslik = "Geçmiş",
                        kayitlar = depo.gecmis,
                        bosMesaj = "Geçmiş boş.",
                        onAc = { yonetici.yeniSekme(it.adres, false) },
                        onSil = { depo.gecmistenSil(it) },
                        onGeri = { yonetici.ekran = Ekran.ANA },
                        onHepsiniTemizle = { depo.gecmisiTemizle() }
                    )

                    Ekran.AYARLAR -> AyarlarScreen(
                        ayar = ayar,
                        onMasaustuDegistir = {
                            ayar.masaustuDegistir(it)
                            yonetici.masaustuModunuUygula()
                        },
                        onGizlilik = { yonetici.gizlilikAc() },
                        onGeri = { yonetici.ekran = Ekran.ANA }
                    )

                    Ekran.GIZLILIK -> GizlilikScreen(
                        ayar = ayar,
                        engel = engel,
                        onCerezKabul = {
                            ayar.cerezKabulDegistir(it)
                            yonetici.cerezAyarlariniUygula()
                        },
                        onUcuncuTaraf = {
                            ayar.ucuncuTarafCerezDegistir(it)
                            yonetici.cerezAyarlariniUygula()
                        },
                        onTemizle = { gecmis, cerez, onbellek, siteVerisi ->
                            yonetici.verileriTemizle(gecmis, cerez, onbellek, siteVerisi)
                        },
                        onGeri = { yonetici.ekran = yonetici.gizlilikOncesi }
                    )

                    Ekran.INDIRMELER -> IndirmelerScreen(
                        indirmeler = indirmeler.indirmeler,
                        konumMetni = if (ayar.indirmeKonumu.isEmpty()) {
                            "Konum: uygulama klasörü (Android/data/com.benimtarayici.app/files/Download)"
                        } else {
                            "Konum: ${ayar.indirmeKonumuAdi}"
                        },
                        onAc = { indirmeyiAc(it) },
                        onDuraklat = { indirmeler.duraklat(it) },
                        onDevamEt = { indirmeler.devamEt(it) },
                        onKaldir = { indirmeler.kaldir(it) },
                        onGeri = {
                            yonetici.ekran = if (yonetici.aktifSekme != null) {
                                Ekran.TARAYICI
                            } else {
                                Ekran.ANA
                            }
                        }
                    )

                    Ekran.VIDEO -> {
                        val video = yonetici.oynatilacak
                        if (video != null) {
                            VideoScreen(
                                baslik = video.dosyaAdi,
                                uri = indirmeler.acmaUri(video),
                                onGeri = { yonetici.ekran = Ekran.INDIRMELER }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun indirmeyiAc(indirme: Indirme) {
        if (indirme.durum != IndirmeDurumu.TAMAMLANDI) return

        if (indirme.dosyaAdi.lowercase().endsWith(".apk")) {
            Toast.makeText(
                this,
                "Güvenlik için APK dosyaları uygulamadan açılmaz",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (indirme.videoMu) {
            yonetici.oynatilacak = indirme
            yonetici.ekran = Ekran.VIDEO
            return
        }

        val niyet = Intent(Intent.ACTION_VIEW)
            .setDataAndType(indirmeler.acmaUri(indirme), indirme.mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(niyet)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Bu dosyayı açabilecek bir uygulama bulunamadı",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun tamEkranUygula(acik: Boolean) {
        val kontrol = WindowCompat.getInsetsController(window, window.decorView)
        if (acik) {
            kontrol.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            kontrol.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            kontrol.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onDestroy() {
        // İndirmeler arka planda sürebilir, burada durdurmuyoruz.
        indirmeler.izinIste = null
        engel.kaydet()
        yonetici.hepsiniYokEt()
        super.onDestroy()
    }
}
