# Count Scroll — Fase 6 (Polish, Ketahanan Service & Rilis v1.0.0)

Implementasi **Fase 6 (Final)** sesuai PRD Count Scroll v1.0:
- **Ketahanan Latar Belakang & Reboot (`BootCompletedReceiver.kt` & `AccessibilityHelper.kt`):** Mendukung pemulihan state setelah perangkat restart (`BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`) dan pengecekan pengecualian optimasi baterai (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) di Beranda.
- **Penyempurnaan Tunda & Efisiensi Baterai (`ScrollAccessibilityService.kt`):** Pengurangan kuota *scroll* tunda per *scroll* ketika mode tunda menggunakan satuan **Jumlah Scroll**, pengecekan masa habis tunda menit hanya saat aplikasi target aktif di layar, serta pencegahan duplikasi *render* dialog batas.
- **Build Debug & Release (`app/build.gradle.kts` & `.github/workflows/build-apk.yml`):** Konfigurasi `versionName = "1.0.0"`, R8/ProGuard resource shrinking pada build rilis, serta artifact APK Debug dan Release di GitHub Actions.
