package com.benimtarayici.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.benimtarayici.app.ui.browser.BrowserScreen
import com.benimtarayici.app.ui.home.HomeScreen
import com.benimtarayici.app.ui.theme.TarayiciTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TarayiciTema {
                var aktifAdres by rememberSaveable { mutableStateOf<String?>(null) }
                val adres = aktifAdres

                if (adres == null) {
                    HomeScreen(onAdresAc = { aktifAdres = it })
                } else {
                    BrowserScreen(
                        baslangicAdresi = adres,
                        onAnaSayfa = { aktifAdres = null }
                    )
                }
            }
        }
    }
}
