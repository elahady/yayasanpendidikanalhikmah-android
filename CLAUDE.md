# Yayasan Pendidikan Al-Hikmah - Android

App Android (Jetpack Compose) untuk TPQ & Madrasah Diniyah Yayasan Pendidikan Al-Hikmah: login, rekap harian, absensi ustadz/santri, daftar santri + progress, dan "Anak Saya" untuk wali. Klien dari API v1 milik web app (`C:\Project\Personal\yayasanpendidikanalhikmah-web-app`).

## Status Pengembangan

_Diperbarui 31 Oktober 2026 (Tahap 3, versi 1). Rencana besar, keputusan role, dan status backend/deploy: `CLAUDE.md` repo web (`C:\Project\Personal\yayasanpendidikanalhikmah-web-app`, bagian Status Pengembangan)._

> Remote git: `https://github.com/elahady/yayasanpendidikanalhikmah-android` (branch `main`, sudah di-push 31 Oktober 2026).

### Sudah selesai
- Semua scope versi 1 dikerjakan terhadap kontrak API (lihat bawah): login + token aman, cek sesi `GET /me`, 401 kembali ke login, beranda berbasis role (multi-role), rekap harian, absensi ustadz/santri (tulis + lihat), daftar santri + detail + ubah progress, Anak Saya (riwayat absensi 30 hari + progress).
- Kontrak sudah diselaraskan dengan "Catatan implementasi" backend tahap 2 (PIKET tanpa absensi santri, INTERNAL, Retry-After, login akun nonaktif 403, dicatatOleh null).
- `./gradlew test assembleDebug assembleRelease` lulus (51 tes `:domain`, 24 tes `:app`).

### Belum / berikutnya
- **Belum pernah dijalankan di emulator/perangkat dan belum dites ke backend sungguhan** (hanya unit test dengan server palsu MockWebServer + fake repository). Langkah pertama berikutnya: jalankan backend lokal (`http://10.0.2.2:3000`) dan uji tiap role di emulator.
- Preview Compose ditulis tetapi belum dilihat di Android Studio; tampilan dark/light belum diperiksa visual.
- Ikon launcher/splash memakai `logo.png` web (194x194) yang di-upscale; minta logo resolusi lebih tinggi/vektor bila ada.
- Belum ada: keystore & signing rilis, R8/minify, GitHub Actions/CI, notifikasi push dan mode offline (rencana versi 2).
- Tidak ada UI untuk memilih tema manual (mengikuti sistem).
- Backend tahap 2 sudah di-deploy ke VPS (31 Oktober 2026, `http://103.54.170.198:8083`; login ADMIN terverifikasi lewat API), tetapi subdomain `tpq-alhikmah.roziqrizal.com` belum aktif (menunggu DNS), jadi build release belum bisa login. Build debug dengan `debug.apiHost=103.54.170.198:8083` bisa login ke VPS (HTTP polos: jangan dipakai untuk data sungguhan). Alternatif: backend lokal (repo web: `npm run dev`, MySQL XAMPP, seed).
- Pertanyaan terbuka: jilid Nahdliyah berlaku untuk jenjang mana saja (UI sementara menampilkan editor jilid untuk semua santri).
- Rilis Play Store (akun pribadi): keystore, kebijakan privasi, data safety, closed testing; lihat daftar di CLAUDE.md repo web.

## Perintah

Butuh **JDK 17+** lewat `JAVA_HOME` (JDK 11 di PATH tidak cukup). Di PC ini: `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`. Android SDK di `local.properties` (`sdk.dir=C:/Android/Sdk`, tidak di-commit; sama dengan Rizqflow).

```
export JAVA_HOME="C:\\Program Files\\Microsoft\\jdk-17.0.20.101-hotspot"   # Git Bash
./gradlew test                 # :domain (JVM murni) + :app (ViewModel, ApiClient)
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # belum ditandatangani
```

## Arsitektur

Mengikuti pola Rizqflow (`C:\Project\Personal\Rizqflow`): Gradle 9.7.1, AGP 9.4.1 (Kotlin bawaan AGP), Kotlin 2.4.20, Compose BOM 2026.09.00, compileSdk/targetSdk 37, minSdk 26, DI manual, JUnit Jupiter + kotlin.test. Versi semua di `gradle/libs.versions.toml`.

- `:domain` - Kotlin/JVM murni (`allWarningsAsErrors`), package `com.alhikmah.tpq.domain`:
  - `model/` Role, User, Santri (Jenjang, Jilid), Ustadz, AbsensiEntry, Rekap.
  - `api/` `ApiError`/`ApiException` (parsing body `{error:{code,message}}`, Retry-After), `ApiJson` (JSON <-> model, org.json), antarmuka `AuthApi`, `TpqRepository`, `TokenStore`.
  - `menu/MenuResolver` - aturan role -> menu (gabungan multi-role, WRITE menang atas READ), hak ubah progress, hak lihat absensi santri.
  - `roster/Roster` - menggabungkan daftar santri/ustadz dengan entri absensi satu tanggal; menyembunyikan ustadz = akun sendiri.
  - `session/SessionManager` - status sesi (Checking/LoggedOut/LoggedIn/Unreachable), login, restore lewat `/me`, `onUnauthorized`.
  - `fake/` `FakeTpqRepository`, `FakeAuthApi`, `InMemoryTokenStore` + `FakeData` (untuk preview Compose dan tes; meniru aturan server).
  - `util/` `Tanggal` (zona Asia/Jakarta, label Indonesia), `Load`/`Outcome`/`attempt`.
  - `org.json` hanya `compileOnly` (sudah ada di Android); tes JVM memakai `org.json:json`.
- `:app` - package `com.alhikmah.tpq`:
  - `AlHikmahApplication` + `AppContainer` (DI manual) dan `LocalAppContainer`.
  - `data/` `ApiClient` (OkHttp: Bearer, error -> ApiException, 401 -> `onUnauthorized`, tanpa redirect, tanpa log body), `HttpRepositories`, `KeystoreTokenStore` (AES-GCM, kunci Android Keystore, disimpan di SharedPreferences; pengganti EncryptedSharedPreferences yang sudah deprecated).
  - `ui/` tema (`theme/`), komponen bersama (`common/`), `AppRoot` (NavHost + pemilih layar berdasar `SessionState`), layar per fitur (`login/`, `beranda/`, `rekap/`, `absensi/`, `santri/`). Tiap layar: composable stateful (ambil ViewModel) + composable stateless `XContent` yang dipakai `@Preview` dengan `FakeData`.

## Konvensi

- Bahasa UI, komentar, dan pesan commit: Indonesia. Akhiri pesan commit dengan baris `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Stage file satu per satu (bukan `git add -A`). **Jangan push / buat remote** tanpa diminta.
- **Kontrak API adalah sumber kebenaran**: `docs/api-contract.md` di repo web (termasuk "Catatan implementasi"). Ubah kontrak dulu, baru kode. Jangan mengarang endpoint/field.
- Base URL hanya di satu tempat: `BuildConfig.API_BASE_URL` (`app/build.gradle.kts`; debug `http://<debug.apiHost>/api/v1`, bawaan `10.0.2.2:3000` untuk emulator; release `https://tpq-alhikmah.roziqrizal.com/api/v1`). Untuk HP fisik / server VPS isi `debug.apiHost=103.54.170.198:8083` di `local.properties` (tidak di-commit), lalu build ulang. Cleartext hanya di build debug (`app/src/debug/res/xml/network_security_config.xml`, seluruh debug karena format XML tak mendukung wildcard IP); release HTTPS-only.
- Pesan error selalu dari `ApiError.message` (body server), jangan dibuat ulang di UI.
- Warna jangan di-hardcode di layar; pakai `MaterialTheme.colorScheme`. Token brand dipetakan dari `app/globals.css` web ke `ui/theme/Color.kt` (ubah di web dulu, samakan di sini). Font Fraunces (judul) + Plus Jakarta Sans (isi), lisensi OFL di `assets/licenses`.
- Status tidak boleh hanya lewat warna: selalu ada teks/pill ("Hadir", "Belum dicatat").
- Logika yang bisa diuji taruh di `:domain` atau ViewModel, bukan di composable.

## Keputusan

- `applicationId` = `com.alhikmah.tpq` (Rizqflow memakai `com.roziqrizal.rizqflow` = domain pemilik; app yayasan tidak memakai domain pribadi itu). Tetap setelah terbit di Play Store.
- Dua modul (`:domain`, `:app`), bukan tiga seperti Rizqflow: tidak ada database lokal, jadi `:data` tidak diperlukan; kode HTTP ada di `:app/data`.
- OkHttp (bukan `HttpURLConnection` seperti Rizqflow) karena API memakai `PATCH`, yang tidak didukung `HttpURLConnection`.
- ViewModel (lifecycle-viewmodel-compose) dipakai untuk state layar; Rizqflow memakai controller + lifecycleScope, tetapi ViewModel lebih cocok untuk daftar/form per layar dan bertahan lewat rotasi.
- Absensi: menu/aksi hanya disembunyikan di UI; **server tetap penentu hak** (`bisaDiubah`, FORBIDDEN, SELF_ATTENDANCE_FORBIDDEN ditampilkan dari pesan server dan baris dikunci).
- Ustadz yang tertaut ke akun disembunyikan dari daftar absensi ustadz **kecuali akun ADMIN/TU** (kontrak mengecualikan ADMIN/TU dari larangan self-attendance).
- PIKET murni tidak mendapat menu Absensi Santri dan layar detail santrinya tidak memuat riwayat absensi (server: 403). Fungsi: `MenuResolver.canViewAbsensiSantri`.
- Baris absensi yang belum dicatat memakai bawaan "Hadir" tetapi baru tersimpan saat tombol Simpan ditekan (tampil "Belum dicatat" sampai itu).
- "Tidak hadir" di rekap = total - hadir - belumDicatat (kontrak hanya memberi tiga angka itu).
- `dicatatOleh` null (data lama) ditangani: hanya ADMIN/TU yang bisa mengubah (`bisaDiubah` dari server).
- Tanpa refresh token (kontrak): token 30 hari, 401 membuang token dan kembali ke login; jaringan putus saat cek sesi menampilkan layar "Coba lagi" tanpa membuang token.
- `allowBackup=false`: token tidak ikut cadangan otomatis.
