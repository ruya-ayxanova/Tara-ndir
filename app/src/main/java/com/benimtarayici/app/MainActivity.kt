package com.benimtarayici.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.benimtarayici.app.ui.home.HomeScreen
import com.benimtarayici.app.ui.theme.TarayiciTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TarayiciTema {
                HomeScreen()
            }
        }
    }
}
