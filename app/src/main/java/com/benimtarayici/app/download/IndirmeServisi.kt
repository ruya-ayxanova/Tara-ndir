package com.benimtarayici.app.download

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

// Sadece indirme sürerken uygulamayı canlı tutan ve bildirimi güncelleyen servis.
class IndirmeServisi : Service() {

    private val ana = Handler(Looper.getMainLooper())

    private val guncelle = object : Runnable {
        override fun run() {
            val yonetici = IndirmeYoneticisi.mevcut()
            val aktifler = yonetici?.indirmeler
                ?.filter { it.durum == IndirmeDurumu.INIYOR }
                ?: emptyList()

            if (aktifler.isEmpty()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            var indirilen = 0L
            var toplam = 0L
            var bilinmiyor = false
            aktifler.forEach {
                if (it.toplam > 0) {
                    indirilen += it.indirilen
                    toplam += it.toplam
                } else {
                    bilinmiyor = true
                }
            }
            val yuzde = if (!bilinmiyor && toplam > 0) {
                ((indirilen * 100) / toplam).toInt().coerceIn(0, 100)
            } else {
                -1
            }

            val bildirimYonetici = getSystemService(NotificationManager::class.java)
            bildirimYonetici.notify(
                IndirmeBildirim.SERVIS_ID,
                IndirmeBildirim.servis(this@IndirmeServisi, aktifler.size, yuzde)
            )
            ana.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        IndirmeBildirim.kanalOlustur(this)
        val bildirim = IndirmeBildirim.servis(this, 1, -1)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                IndirmeBildirim.SERVIS_ID,
                bildirim,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(IndirmeBildirim.SERVIS_ID, bildirim)
        }
        ana.removeCallbacks(guncelle)
        ana.post(guncelle)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ana.removeCallbacks(guncelle)
        super.onDestroy()
    }
}
