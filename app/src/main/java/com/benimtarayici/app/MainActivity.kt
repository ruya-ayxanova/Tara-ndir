package com.benimtarayici.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.benimtarayici.app.network.AgDurumu
import com.benimtarayici.app.network.ProxyYoneticisi
import com.benimtarayici.app.ui.browser.BrowserScreen
import com.benimtarayici.app.ui.downloads.IndirmelerScreen
import com.benimtarayici.app.ui.home.HomeScreen
import com.benimtarayici.app.ui.lists.KayitListesiScreen
import com.benimtarayici.app.ui.network.ProxyScreen
import com.benimtarayici.app.ui.player.VideoScreen
import com.benimtarayici.app.ui.privacy.GizlilikScreen
import com.benimtarayici.app.ui.settings.AyarlarScreen
import com.benimtarayici.app.ui.tabs.SekmelerScreen
import com.benimtarayici.app.ui.theme.TarayiciTema
import com.benimtarayici.app.ui.vpn.VpnScreen

class MainActivity : ComponentActivity() {

    private lateinit var depo: VeriDeposu
    private lateinit var ayar: AyarDeposu
    private lateinit var indirmeler: IndirmeYoneticisi
    private lateinit var engel: EngelYoneticisi
    private lateinit var ag: AgDurumu
    private lateinit var proxy: ProxyYoneticisi
    private lateinit var yonetici: SekmeYoneticisi

    private val bildirimIzni =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        depo = VeriDeposu(this)
        ayar = AyarDeposu(this)
        indirmeler = IndirmeYoneticisi.al(this)
        engel = EngelYoneticisi(this, ayar)
        ag = AgDurumu(this)
        proxy = ProxyYoneticisi.al(this)
        yonetici = SekmeYoneticisi(this, depo, ayar, indirmeler, engel, ag)

        // Kayıtlı proxy seçimi her açılışta yeniden uygulanır.
        proxy.uygula()

        // İnternet veya VPN geri gelince hata sayfasındaki sekme yeniden denenir.
        ag.geriGeldi = { yonetici.baglantiGeldi() }
        ag.baslat()

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
                // Ayarlar içindeki Proxy alt ekranı açık mı?
                var proxyAcik by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxSize()) {

                    // Üstteki uyarı bandı: önce internet, sonra VPN kontrolü.
                    if (!ag.internetVar) {
                        UyariBandi("📵 İnternet bağlantısı yok")
                    } else if (yonetici.vpnEngeli()) {
                        UyariBandi("🔐 VPN bağlı değil (zorunlu mod açık)")
                    }

                    Box(modifier = Modifier.weight(1f)) {

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
                                        onVideoIndir = { yonetici.videoIndir(sekme, it) },
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

                            Ekran.AYARLAR -> {
                                if (proxyAcik) {
                                    ProxyScreen(
                                        proxy = proxy,
                                        onDegisti = { sayfalariYenile() },
                                        onGeri = { proxyAcik = false }
                                    )
                                } else {
                                    AyarlarScreen(
                                        ayar = ayar,
                                        vpnOzeti = if (ag.vpnBagli) "Bağlı" else "Bağlı değil",
                                        proxyOzeti = proxy.aktifOzet,
                                        onMasaustuDegistir = {
                                            ayar.masaustuDegistir(it)
                                            yonetici.masaustuModunuUygula()
                                        },
                                        onGizlilik = { yonetici.gizlilikAc() },
                                        onVpn = { yonetici.ekran = Ekran.VPN },
                                        onProxy = { proxyAcik = true },
                                        onGeri = { yonetici.ekran = Ekran.ANA }
                                    )
                                }
                            }

                            Ekran.VPN -> VpnScreen(
                                ag = ag,
                                onVpnAyarlari = { vpnAyarlariniAc() },
                                onWireGuard = { wireGuardAc() },
                                onGeri = { yonetici.ekran = Ekran.AYARLAR }
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
                                onDevamEt = { devamEttir(it) },
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
        }
    }

    @androidx.compose.runtime.Composable
    private fun UyariBandi(metin: String) {
        Surface(
            color = Color(0xFFB3261E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = metin,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }

    // Proxy değişince açık sayfalar yeni ayarla yeniden yüklenir.
    private fun sayfalariYenile() {
        yonetici.sekmeler.forEach {
            if (it.adres.isNotBlank()) it.webView.reload()
        }
    }

    // VPN zorunlu modunda VPN yokken duraklatılmış indirme devam ettirilmez.
    private fun devamEttir(indirme: Indirme) {
        if (yonetici.vpnEngeli()) {
            Toast.makeText(
                this,
                "VPN zorunlu modu açık ve VPN bağlı değil.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        indirmeler.devamEt(indirme)
    }

    private fun vpnAyarlariniAc() {
        try {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "VPN ayarları açılamadı. Telefon Ayarları > Bağlantılar bölümüne bak.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun wireGuardAc() {
        val niyet = packageManager.getLaunchIntentForPackage("com.wireguard.android")
        try {
            if (niyet != null) {
                startActivity(niyet)
            } else {
                // Yüklü değilse Play Store sayfasını aç.
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.wireguard.android")
                    )
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "WireGuard açılamadı", Toast.LENGTH_LONG).show()
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
        ag.geriGeldi = null
        ag.durdur()
        engel.kaydet()
        yonetici.hepsiniYokEt()
        super.onDestroy()
    }
}
