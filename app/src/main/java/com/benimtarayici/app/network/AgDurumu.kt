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

// Bu uygulamanın internet ve VPN durumunu izler.
class AgDurumu(context: Context) {

    private val yonetici = context.getSystemService(ConnectivityManager::class.java)
    private val ana = Handler(Looper.getMainLooper())
    private val tercihler =
        context.getSharedPreferences("tarayici_ag", Context.MODE_PRIVATE)

    var internetVar by mutableStateOf(
        yetenek()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    )
        private set

    // Bu uygulamanın trafiği VPN üzerinden mi geçiyor?
    var vpnBagli by mutableStateOf(
        yetenek()?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    )
        private set

    var vpnZorunlu by mutableStateOf(tercihler.getBoolean("vpn_zorunlu", false))
        private set

    // Bağlantı veya VPN yokken geri geldiğinde çağrılır.
    var geriGeldi: (() -> Unit)? = null

    private val yenileIs = Runnable { yenile() }

    private val geriCagri = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            planla()
        }

        override fun onLost(network: Network) {
            planla()
        }

        override fun onCapabilitiesChanged(network: Network, yetenek: NetworkCapabilities) {
            planla()
        }
    }

    // Ağ geçişlerinde art arda gelen olayları birleştirmek için kısa bekleme.
    private fun planla() {
        ana.removeCallbacks(yenileIs)
        ana.postDelayed(yenileIs, 300)
    }

    private fun yetenek(): NetworkCapabilities? {
        val ag = yonetici.activeNetwork ?: return null
        return yonetici.getNetworkCapabilities(ag)
    }

    private fun yenile() {
        val y = yetenek()
        val internet = y?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val vpn = y?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        val internetEskiden = internetVar
        val vpnEskiden = vpnBagli
        internetVar = internet
        vpnBagli = vpn

        if ((!internetEskiden && internet) || (!vpnEskiden && vpn)) {
            geriGeldi?.invoke()
        }
    }

    fun vpnZorunluDegistir(deger: Boolean) {
        vpnZorunlu = deger
        tercihler.edit().putBoolean("vpn_zorunlu", deger).apply()
    }

    fun baslat() {
        try {
            yonetici.registerDefaultNetworkCallback(geriCagri)
        } catch (e: Exception) {
            // İzleme başlatılamazsa durum güncellenmez, tarayıcı yine çalışır.
        }
        yenile()
    }

    fun durdur() {
        ana.removeCallbacks(yenileIs)
        try {
            yonetici.unregisterNetworkCallback(geriCagri)
        } catch (e: Exception) {
            // Zaten kaldırılmış olabilir.
        }
    }
}
