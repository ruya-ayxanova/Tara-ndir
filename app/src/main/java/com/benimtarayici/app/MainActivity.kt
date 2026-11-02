package com.benimtarayici.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.benimtarayici.app.browser.Ekran
import com.benimtarayici.app.browser.SekmeYoneticisi
import com.benimtarayici.app.data.AyarDeposu
import com.benimtarayici.app.data.VeriDeposu
import com.benimtarayici.app.ui.browser.BrowserScreen
import com.benimtarayici.app.ui.home.HomeScreen
import com.benimtarayici.app.ui.lists.KayitListesiScreen
import com.benimtarayici.app.ui.settings.AyarlarScreen
import com.benimtarayici.app.ui.tabs.SekmelerScreen
import com.benimtarayici.app.ui.theme.TarayiciTema

class MainActivity : ComponentActivity() {

    private lateinit var depo: VeriDeposu
    private lateinit var ayar: AyarDeposu
    private lateinit var yonetici: SekmeYoneticisi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        depo = VeriDeposu(this)
        ayar = AyarDeposu(this)
        yonetici = SekmeYoneticisi(this, depo, ayar)

        setContent {
            LaunchedEffect(ayar.tamEkran) {
                tamEkranUygula(ayar.tamEkran)
            }

            TarayiciTema(tema = ayar.tema) {
                when (yonetici.ekran) {
                    Ekran.ANA -> HomeScreen(
                        sekmeSayisi = yonetici.sekmeler.size,
                        gizliMod = yonetici.gizliMod,
                        onAdresAc = { yonetici.yeniSekme(it) },
                        onSekmeler = { yonetici.ekran = Ekran.SEKMELER },
                        onGizliDegistir = { yonetici.gizliMod = !yonetici.gizliMod },
                        onYerImleri = { yonetici.ekran = Ekran.YER_IMLERI },
                        onGecmis = { yonetici.ekran = Ekran.GECMIS },
                        onAyarlar = { yonetici.ekran = Ekran.AYARLAR }
                    )

                    Ekran.TARAYICI -> {
                        val sekme = yonetici.aktifSekme
                        if (sekme != null) {
                            BrowserScreen(
                                sekme = sekme,
                                sekmeSayisi = yonetici.sekmeler.size,
                                yerImiVar = depo.yerImiVarMi(sekme.adres),
                                onYerImi = {
                                    depo.yerImiDegistir(
                                        sekme.baslik.removePrefix("🕶 "),
                                        sekme.adres
                                    )
                                },
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
                        onGeri = { yonetici.ekran = Ekran.ANA }
                    )
                }
            }
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
        yonetici.hepsiniYokEt()
        super.onDestroy()
    }
}
