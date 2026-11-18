# HIZ — Proje Haritası

> İlk harita: 18 Kasım 2026. Depo kök dosyaları incelendi; README bulunamadı. Bu nedenle aşağıdaki açıklamalar dosya adları ve türlerine dayalı ilk haritadır, uygulamanın tüm davranışını kanıtlamaz.

## Kök dosyalar
- `MainActivity.kt`: Android uygulamasının ana Activity/UI giriş noktası.
- `SpeedService.kt`: hız ölçümüyle ilişkili Android servis bileşeni.
- `index.html`: web arayüzü veya eşlik eden sayfa; kullanım ilişkisi ayrıca doğrulanmalı.
- `FK-ikon-512.png`: uygulama/görsel ikon kaynağı.
- `.github/`: GitHub Actions otomasyonu.
- `hiz-keystore.b64`: imzalama anahtarı materyali olabilecek hassas dosya. Bu dosya depoda bulunuyorsa güvenlik açısından derhal denetlenmeli; özel anahtar/keystore kaynak depoda tutulmamalı, gerekiyorsa rotate edilmelidir.

## Mimari taslak
Android Activity ↔ hız ölçüm servisi. Web sayfası ile Android tarafının ortak veri veya UI kullanıp kullanmadığı henüz doğrulanmadı.

## Derleme ve dağıtım
GitHub Actions yapılandırması mevcut. Kesin Gradle/Android derleme komutu, proje kökünün kalan dosyaları incelenerek doğrulanmalı.

## Açık işler
1. `hiz-keystore.b64` dosyasının gerçekten özel imzalama anahtarı olup olmadığını kontrol et.
2. Android manifest, Gradle yapılandırması ve servis izinlerini haritaya ekle.
3. `index.html` ile Android uygulaması arasındaki ilişkiyi doğrula.

## Harita güncelleme kuralı
Yeni ekran, servis, izin, veri kaynağı veya web bileşeni eklendiğinde bu dosyayı güncelle.
