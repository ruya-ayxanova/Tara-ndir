package com.benimtarayici.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Telefonun internete bağlı olup olmadığını izler.
class AgDurumu(context: Context) {

    private val yonetici = context.getSystemService(ConnectivityManager::class.java)
    private val ana = Handler(Looper.getMainLooper())

    var internetVar by mutableStateOf(simdiVarMi())
        private set

    // Bağlantı yokken geri geldiğinde çağrılır.
    var geriGeldi: (() -> Unit)? = null

    private val geriCagri = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            ana.post { durumuGuncelle(true) }
        }

        override fun onLost(network: Network) {
            ana.post { durumuGuncelle(false) }
        }
    }

    private fun simdiVarMi(): Boolean {
        val ag = yonetici.activeNetwork ?: return false
        val yetenek = yonetici.getNetworkCapabilities(ag) ?: return false
        return yetenek.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun durumuGuncelle(yeni: Boolean) {
        val eskiden = internetVar
        internetVar = yeni
        if (!eskiden && yeni) {
            geriGeldi?.invoke()
        }
    }

    fun baslat() {
        try {
            yonetici.registerDefaultNetworkCallback(geriCagri)
        } catch (e: Exception) {
            // İzleme başlatılamazsa uyarı bandı çalışmaz, tarayıcı yine çalışır.
        }
    }

    fun durdur() {
        try {
            yonetici.unregisterNetworkCallback(geriCagri)
        } catch (e: Exception) {
            // Zaten kaldırılmış olabilir.
        }
    }
}
