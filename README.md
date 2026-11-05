# Yeni Tarayıcı

Android için özgün, Kotlin ve Jetpack Compose ile yazılmış, WebView tabanlı internet tarayıcısı.

## Özellikler
- Çoklu sekme, gizli sekme, yer imleri, geçmiş
- Arama motoru seçimi, masaüstü sitesi, tam ekran, koyu/açık tema
- İndirme yöneticisi (duraklat/devam, konum seçimi, video oynatıcı, arka plan indirme)
- Reklam ve izleyici engelleme, çerez yönetimi, veri temizleme
- HTTPS öncelikli bağlantı, riskli indirme uyarısı
- Bağlantı hatası sayfaları, çevrimdışı uyarısı
- Proxy desteği (HTTP/SOCKS5)
- VPN: durum algılama, "VPN zorunlu" modu, uygulama içi WireGuard tüneli

## Derleme
GitHub Actions kullanılır:
- `build.yml`: her `main` push'unda debug APK üretir.
- `anahtar-olustur.yml`: imza anahtarını bir kez üretir.
- `release.yml`: imzalı APK ve AAB üretir.

Minimum Android sürümü: 8 (API 26).

## Kullanılan açık kaynak bileşenler
- AndroidX, Jetpack Compose (Apache-2.0)
- WireGuard Android tünel kütüphanesi `com.wireguard.android:tunnel` (Apache-2.0)
