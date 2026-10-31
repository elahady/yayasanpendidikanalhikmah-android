# Yayasan Pendidikan Al-Hikmah - Android

App Android (Jetpack Compose) untuk TPQ & Madrasah Diniyah Yayasan Pendidikan Al-Hikmah: login, rekap harian, absensi ustadz/santri, daftar santri + progress, dan "Anak Saya" untuk wali. Klien dari API v1 milik web app (`C:\Project\Personal\yayasanpendidikanalhikmah-web-app`).

## Status Pengembangan

_Diperbarui 31 Oktober 2026 (Tahap 4: beranda per peran, profil, tema). Rencana besar, keputusan role, dan status backend/deploy: `CLAUDE.md` repo web (`C:\Project\Personal\yayasanpendidikanalhikmah-web-app`, bagian Status Pengembangan)._

> Remote git: `https://github.com/elahady/yayasanpendidikanalhikmah-android` (branch `main`).

### Sudah selesai
- Scope versi 1 terhadap kontrak API: login + token aman, cek sesi `GET /me`, 401 kembali ke login, rekap harian, absensi ustadz/santri (tulis + lihat), daftar santri + detail + ubah progress, Anak Saya.
- Tahap 4 (31 Oktober 2026): beranda per peran dari `GET /dashboard` (varian admin / tugas piket+ustadz / wali), navigasi bawah berbasis `MenuResolver`, layar Profil (identitas, pemilih tema Ikuti sistem/Terang/Gelap tersimpan, ubah password lewat `POST /me/password`, keluar). `./gradlew test assembleDebug assembleRelease` lulus (90 tes `:domain`, 44 tes `:app`).
- **Jadwal Senin–Jumat (31 Oktober 2026):** Sabtu/Minggu libur. `Jadwal` di `:domain`; beranda menampilkan kartu "Hari ini libur", layar Absensi dan Rekap terkunci di hari libur (banner + tombol "Ke hari belajar terakhir"), server menolak PUT absensi di hari libur (422). `./gradlew test assembleDebug assembleRelease` lulus (134 tes `:domain`, 63 tes `:app`). Dilihat di emulator terhadap backend lokal pada hari Sabtu: kartu libur, banner Absensi Santri, dan pindah ke Jumat bekerja, tanpa crash.
- **Sudah diuji di emulator terhadap backend lokal tahap 4** (data uji + akun per peran): login ADMIN, PENGAWAS, PIKET+USTADZ, dan WALI (2 anak) menampilkan beranda yang benar dan tidak crash; tema Gelap berlaku (status bar dan bilah navigasi ikut); dialog Ubah password terbuka. Crash `NetworkOnMainThreadException` setelah login sudah diperbaiki (body respons dibaca di `Dispatchers.IO`).

### Belum / berikutnya
- **Belum diuji di emulator/perangkat:** menyimpan password baru lewat dialog (API-nya sudah dites di backend), pull-to-refresh, tab Absensi dan Santri, layar Anak saya penuh, peran PIKET murni dan USTADZ murni, tampilan wali di mode terang/gelap. Preview Compose belum dilihat di Android Studio.
- Backend tahap 4 **sudah di-deploy ke VPS** (31 Oktober 2026; login, `/me`, `/dashboard`, `/me/password` terverifikasi), termasuk **aturan jadwal Senin–Jumat (di-deploy 1 November 2026)**; build debug dengan `debug.apiHost=103.54.170.198:8083` memakainya. Subdomain `tpq-alhikmah.roziqrizal.com` belum aktif (menunggu DNS), jadi build release belum bisa login. Database produksi masih kosong (belum ada santri/ustadz).
- Beranda admin menampilkan kartu Kehadiran 14 hari dan Aktivitas terbaru; beranda piket menampilkan daftar ustadz per baris (`ringkasan.ustadz.daftar`). Keduanya sudah dilihat di emulator terhadap backend lokal. Ikon notifikasi dari mockup tidak dibuat.
- Ikon launcher/splash memakai `logo.png` web (194x194) yang di-upscale; minta logo resolusi lebih tinggi/vektor bila ada. Latar jendela/splash mengikuti tema sistem (bisa berkedip bila pilihan tema berbeda).
- Belum ada: keystore & signing rilis, R8/minify, GitHub Actions/CI, notifikasi push dan mode offline (rencana versi 2).
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
  - `model/` Role, User (+ `username`, `ustadzNama`, `dibuatPada`, opsional), Santri (Jenjang, Jilid), Ustadz, AbsensiEntry, Rekap, `Dashboard` (Ringkasan, Perhatian, AnakDashboard, KehadiranAnak; bentuk `GET /dashboard`).
  - `api/` `ApiError`/`ApiException` (parsing body `{error:{code,message}}`, Retry-After), `ApiJson` (JSON <-> model, org.json), antarmuka `AuthApi`, `TpqRepository`, `TokenStore`.
  - `menu/MenuResolver` - aturan role -> menu (gabungan multi-role, WRITE menang atas READ), hak ubah progress, hak lihat absensi santri.
  - `menu/BottomTabs` - tab navigasi bawah diturunkan dari `MenuResolver` (tab yang menunya tak boleh diakses role tidak muncul).
  - `beranda/` `BerandaResolver` (pemilih varian Beranda per role), `JilidJourney` (8 tahap jilid, tahap berikutnya), `BerandaTampilan` (logika kartu Beranda yang teruji: `UstadzHariIniBeranda`, `TrenKehadiran`, `AktivitasBeranda`/`WaktuAktivitas`).
  - `tema/ThemeSettings` (+ `ThemeMode`, antarmuka `ThemeStore`) - pilihan tampilan Ikuti sistem/Terang/Gelap; `profil/PasswordForm` - validasi lokal ubah password.
  - `roster/Roster` - menggabungkan daftar santri/ustadz dengan entri absensi satu tanggal; menyembunyikan ustadz = akun sendiri.
  - `session/SessionManager` - status sesi (Checking/LoggedOut/LoggedIn/Unreachable), login, restore lewat `/me`, `onUnauthorized`.
  - `fake/` `FakeTpqRepository`, `FakeAuthApi`, `InMemoryTokenStore` + `FakeData` (untuk preview Compose dan tes; meniru aturan server).
  - `util/` `Tanggal` (zona Asia/Jakarta, label Indonesia, waktu ISO server), `Inisial` (avatar), `Load`/`Outcome`/`attempt`.
  - `org.json` hanya `compileOnly` (sudah ada di Android); tes JVM memakai `org.json:json`.
- `:app` - package `com.alhikmah.tpq`:
  - `AlHikmahApplication` + `AppContainer` (DI manual) dan `LocalAppContainer`.
  - `data/` `ApiClient` (OkHttp: Bearer, error -> ApiException, 401 -> `onUnauthorized`, tanpa redirect, tanpa log body), `HttpRepositories`, `KeystoreTokenStore` (AES-GCM, kunci Android Keystore, disimpan di SharedPreferences; pengganti EncryptedSharedPreferences yang sudah deprecated), `SharedPrefsThemeStore` (pilihan tema, SharedPreferences biasa).
  - `ui/` tema (`theme/`), komponen bersama (`common/`), `AppRoot` (pemilih layar berdasar `SessionState`) + `MainNav` (NavHost + navigasi bawah), layar per fitur (`login/`, `beranda/` per varian, `profil/`, `rekap/`, `absensi/`, `santri/`). Tiap layar: composable stateful (ambil ViewModel) + composable stateless `XContent` yang dipakai `@Preview` dengan `FakeData`.

## Konvensi

- Bahasa UI, komentar, dan pesan commit: Indonesia. Akhiri pesan commit dengan baris `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Stage file satu per satu (bukan `git add -A`). **Jangan push / buat remote** tanpa diminta.
- **Kontrak API adalah sumber kebenaran**: `docs/api-contract.md` di repo web (termasuk "Catatan implementasi"). Ubah kontrak dulu, baru kode. Jangan mengarang endpoint/field.
- Base URL hanya di satu tempat: `BuildConfig.API_BASE_URL` (`app/build.gradle.kts`; debug `http://<debug.apiHost>/api/v1`, bawaan `10.0.2.2:3000` untuk emulator; release `https://tpq-alhikmah.roziqrizal.com/api/v1`). Untuk HP fisik / server VPS isi `debug.apiHost=103.54.170.198:8083` di `local.properties` (tidak di-commit), lalu build ulang. Cleartext hanya di build debug (`app/src/debug/res/xml/network_security_config.xml`, seluruh debug karena format XML tak mendukung wildcard IP); release HTTPS-only.
- Pesan error selalu dari `ApiError.message` (body server), jangan dibuat ulang di UI.
- Warna jangan di-hardcode di layar; pakai `MaterialTheme.colorScheme`. Token brand dipetakan dari `app/globals.css` web ke `ui/theme/Color.kt` (ubah di web dulu, samakan di sini). Font Fraunces (judul) + Plus Jakarta Sans (isi), lisensi OFL di `assets/licenses`.
- Status tidak boleh hanya lewat warna: selalu ada teks/pill ("Hadir", "Belum dicatat").
- Logika yang bisa diuji taruh di `:domain` atau ViewModel, bukan di composable.
- Warna status kehadiran/aksen grafik di luar skema Material lewat `MaterialTheme.statusColors` (`ui/theme/StatusColors.kt`, token mockup `--okbg/--no/--accent` dengan varian gelap), jangan hardcode. Kotak/batang "hadir" memakai `colorScheme.primary`.
- Target sentuh >= 48dp (tombol, chip, baris yang bisa diketuk), `contentDescription` bermakna; grafik/kisi dibaca sebagai satu elemen dengan deskripsi ringkasan (`clearAndSetSemantics`). Bentuk membedakan status selain warna (tidak hadir = tanda ×, belum dicatat = garis putus-putus).

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
- **Pemilih varian Beranda** (`BerandaResolver`, satu-satunya tempat aturan): ada ADMIN/TU/PENGAWAS -> varian admin; selain itu ada PIKET/USTADZ -> varian tugas; selain itu WALI murni -> varian wali; role tak dikenal semua -> "Belum ada menu". Akun WALI + role lain mendapat varian role lain **ditambah** bagian "Anak saya" di bawahnya bila server mengirim `anak` tidak kosong. Beranda memanggil `GET /dashboard` tanpa `tanggal` (hari ini menurut server, bukan jam HP) dan memuat ulang saat kembali ke layar (resume) serta lewat tarik-untuk-muat-ulang / tombol Muat ulang.
- Beranda admin: "Perlu perhatian" menaut ke layar `aksi` item **hanya bila menunya ada untuk role itu** (`Perhatian.tujuan(allowed)`); selain itu tampil sebagai teks tanpa panah. Kartu "Kehadiran 14 hari" (`tren`: batang = hadir/tercatat per hari belajar, rata-rata = rata-rata persen harian, hari terakhir beraksen, deskripsi teks untuk pembaca layar) dan "Aktivitas terbaru" (`aktivitas`, waktu relatif Asia/Jakarta dengan tanggal dashboard sebagai "hari ini") tampil di varian admin; varian tugas tidak menampilkan aktivitas.
- Beranda tugas (PIKET/USTADZ): kontrak tidak punya data "kelas saya", jadi diganti kartu "Santri hari ini" (angka dari `ringkasan.santri`) dengan tombol ke Absensi Santri / Perbarui progress sesuai hak (`MenuResolver`); "Ustadz hari ini" = daftar per baris dari `ringkasan.ustadz.daftar` (inisial, nama, mapel, pill status), "belum dicatat" di atas lalu "tidak hadir" lalu "hadir", maksimal 5 baris + "dan N ustadz lainnya" + tautan Lihat semua; ustadz tertaut ke akun sendiri disembunyikan kecuali ADMIN/TU (`Roster.hiddenUstadzId`) dengan catatan "Absensi Anda sendiri dicatat oleh piket lain.". Server lama tanpa `daftar` jatuh ke angka hadir/tidak hadir/belum.
- Beranda wali: tahap berikutnya hanya "Tahap berikutnya Jilid N." (materi per jilid tidak ada di kontrak); catatan = `catatanProgressMadin` dengan tanggal `diperbaruiPada` ("Diperbarui ..."), bukan nama ustadz; kotak 30 hari = `kehadiran.hari` apa adanya (hanya hari belajar).
- **Navigasi bawah** (`BottomTabs` di :domain, turunan `MenuResolver`): Beranda, Absensi (layar pilihan Rekap/Absensi Ustadz/Absensi Santri yang boleh), Santri, Anak saya, Profil; hanya tab yang menunya boleh. WALI murni: Beranda, Anak saya, Profil. Bilah hanya tampil di layar tab; layar dalam (rekap, absensi, detail) memakai tombol kembali. Tombol Keluar ada di Profil (dengan konfirmasi).
- **Tema**: pilihan Ikuti sistem/Terang/Gelap disimpan di SharedPreferences "tampilan" (bukan rahasia), dibaca `ThemeSettings` (:domain, teruji) dan diterapkan di `MainActivity` ke `AlHikmahTheme` + gaya status bar/navigation bar (`enableEdgeToEdge` dipanggil ulang saat berubah). Catatan: latar jendela/splash tetap mengikuti tema sistem (bisa berkedip sesaat bila pilihan berbeda dari sistem).
- **Ubah password**: validasi lokal hanya isi + 8-72 karakter + cocok (`PasswordForm`); sisanya pesan server apa adanya (422 "Password sekarang salah.", 429 + Retry-After). 422 tidak memicu logout; hanya 401 yang memicu `onUnauthorized`. Mockup menampilkan "Sesi berlaku sampai ..." tetapi kontrak tidak memuat masa berlaku token, jadi diganti "Akun dibuat" (`dibuatPada`) dan "Ustadz tertaut" (`ustadzNama`) bila ada. Ikon notifikasi di header mockup admin tidak dibuat (belum ada fiturnya).
- **Jadwal: Senin-Jumat belajar, Sabtu dan Minggu libur** (keputusan pemilik, 31 Okt 2026; kontrak: bagian "Jadwal (hari belajar)" di `docs/api-contract.md` repo web). Satu aturan di `Jadwal` (`:domain/util`, salinan `lib/jadwal.ts` server); server tetap penentu. `Dashboard.hariBelajar` dan `Rekap.hariBelajar` (default true bila server lama tidak mengirim). Hari libur: Beranda mengganti cincin kehadiran / kartu tugas piket / "Ustadz hari ini" / "Santri hari ini" dengan kartu "Hari ini libur" (wali: keterangan singkat), tren-tangga jilid-aktivitas-perhatian tetap; layar Absensi dan Rekap menampilkan spanduk "Libur — TPQ belajar Senin–Jumat", mengunci input dan Simpan (data lama tetap terlihat; `AbsensiUiState.libur`, `isEditable`), dan tombol "Ke hari belajar terakhir" (`keHariBelajarTerakhir()`). Rekap libur ditentukan `hariBelajar` dari server setelah dimuat (jadwal lokal hanya sebelum itu); pesan 422 server tetap ditampilkan apa adanya. `FakeTpqRepository` meniru server (libur: `belumDicatat` 0, tanpa item belum-dicatat, PUT absensi 422). Belum ada kalender libur khusus (libur nasional/cuti bersama); bila ditambahkan, di server.
