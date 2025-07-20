package com.unpar.brokenlinkchecker.model;

/**
 * Enum ExecutionStatus merepresentasikan status saat ini dari proses eksekusi utama
 * dalam aplikasi BrokenLinkChecker.
 *
 * Status ini digunakan untuk menunjukkan tahapan atau kondisi proses pemeriksaan tautan
 * yang sedang berlangsung, dan akan memengaruhi tampilan antarmuka (UI) seperti label status,
 * tombol yang aktif/nonaktif, dan alur logika pemeriksaan.
 *
 * Status ini BUKAN status dari HTTP response, tetapi status dari JALANNYA aplikasi.
 */
public enum ExecutionStatus {

    /**
     * IDLE berarti proses belum dijalankan, atau sudah selesai dan belum dimulai ulang.
     * Ini adalah status default aplikasi saat pertama kali dijalankan.
     * - Tombol "Check" aktif
     * - Tombol "Stop" dan "Export" nonaktif
     */
    IDLE,

    /**
     * CHECKING berarti proses crawling dan pemeriksaan tautan sedang berjalan.
     * Aplikasi sedang aktif menjelajahi halaman dan memeriksa tautan satu per satu.
     * - Tombol "Check" nonaktif (disable)
     * - Tombol "Stop" aktif
     * - Tombol "Export" nonaktif
     */
    CHECKING,

    /**
     * COMPLETED berarti proses pemeriksaan selesai sepenuhnya tanpa dihentikan.
     * Semua halaman dan tautan sudah diperiksa dan hasilnya tersedia di tabel.
     * - Tombol "Check" aktif kembali
     * - Tombol "Stop" nonaktif
     * - Tombol "Export" aktif
     */
    COMPLETED,

    /**
     * STOPPED berarti proses pemeriksaan dihentikan oleh pengguna sebelum selesai.
     * Sebagian hasil mungkin tetap tersedia di tabel.
     * - Tombol "Check" aktif
     * - Tombol "Stop" nonaktif
     * - Tombol "Export" aktif
     */
    STOPPED,

    /**
     * ERROR berarti terjadi kesalahan (misalnya exception atau URL tidak valid)
     * saat menjalankan proses pemeriksaan.
     * - Tombol "Check" aktif
     * - Tombol "Stop" nonaktif
     * - Tombol "Export" nonaktif
     */
    ERROR
}
