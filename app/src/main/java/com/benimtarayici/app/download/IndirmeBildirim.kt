package com.benimtarayici.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.benimtarayici.app.MainActivity

object IndirmeBildirim {

    const val KANAL = "indirmeler"
    const val SERVIS_ID = 4242

    fun kanalOlustur(context: Context) {
        val yonetici = context.getSystemService(NotificationManager::class.java)
        val kanal = NotificationChannel(
            KANAL,
            "İndirmeler",
            NotificationManager.IMPORTANCE_LOW
        )
        yonetici.createNotificationChannel(kanal)
    }

    private fun acIntent(context: Context): PendingIntent {
        val niyet = Intent(context, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        return PendingIntent.getActivity(
            context,
            0,
            niyet,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // yuzde: 0-100 arası, bilinmiyorsa -1
    fun servis(context: Context, aktifSayisi: Int, yuzde: Int): Notification {
        val yapici = Notification.Builder(context, KANAL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                if (aktifSayisi == 1) "1 dosya indiriliyor" else "$aktifSayisi dosya indiriliyor"
            )
            .setContentIntent(acIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (yuzde >= 0) {
            yapici.setProgress(100, yuzde, false)
            yapici.setContentText("%$yuzde")
        } else {
            yapici.setProgress(0, 0, true)
        }
        return yapici.build()
    }

    fun tamamlandi(context: Context, dosyaAdi: String, id: Int) {
        val yonetici = context.getSystemService(NotificationManager::class.java)
        val bildirim = Notification.Builder(context, KANAL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("İndirme tamamlandı")
            .setContentText(dosyaAdi)
            .setContentIntent(acIntent(context))
            .setAutoCancel(true)
            .build()
        yonetici.notify(10000 + id, bildirim)
    }
}
