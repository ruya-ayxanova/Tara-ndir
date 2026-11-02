package com.benimtarayici.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.benimtarayici.app.browser.Ekran
import com.benimtarayici.app.browser.SekmeYoneticisi
import com.benimtarayici.app.ui.browser.BrowserScreen
import com.benimtarayici.app.ui.home.HomeScreen
import com.benimtarayici.app.ui.tabs.SekmelerScreen
import com.benimtarayici.app.ui.theme.TarayiciTema

class MainActivity : ComponentActivity() {

    private lateinit var yonetici: SekmeYoneticisi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        yonetici = SekmeYoneticisi(this)

        setContent {
            TarayiciTema {
                when (yonetici.ekran) {
                    Ekran.ANA -> HomeScreen(
                        sekmeSayisi = yonetici.sekmeler.size,
                        onAdresAc = { yonetici.yeniSekme(it) },
                        onSekmeler = { yonetici.ekran = Ekran.SEKMELER }
                    )

                    Ekran.TARAYICI -> {
                        val sekme = yonetici.aktifSekme
                        if (sekme != null) {
                            BrowserScreen(
                                sekme = sekme,
                                sekmeSayisi = yonetici.sekmeler.size,
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
                }
            }
        }
    }

    override fun onDestroy() {
        yonetici.hepsiniYokEt()
        super.onDestroy()
    }
}
