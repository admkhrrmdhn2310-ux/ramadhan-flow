# RAMADHAN FLOW — Android Studio project

Aplikasi pencatat keuangan pribadi Android dengan saldo Bank dan Cash, transaksi masuk/keluar, grafik ringkas, dan riwayat transaksi. Data transaksi dan saldo disimpan lokal menggunakan Android DataStore.

## Cara membuat APK
1. Ekstrak ZIP ini.
2. Buka Android Studio.
3. Pilih **Open** lalu pilih folder `RamadhanFlow`.
4. Tunggu Gradle Sync selesai. Pastikan Android SDK Platform 35 terpasang.
5. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. APK debug akan muncul di `app/build/outputs/apk/debug/app-debug.apk`.

## Catatan
- Ini adalah source project Android Studio, bukan APK yang sudah dikompilasi.
- Saldo awal Bank dan Cash adalah Rp0. Tambahkan saldo awal sebagai transaksi uang masuk.
- Data disimpan di perangkat dan tidak terhubung otomatis ke rekening bank.
- Jangan memasukkan PIN, password, atau nomor kartu bank.
