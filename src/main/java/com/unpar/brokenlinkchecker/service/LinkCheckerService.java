package com.unpar.brokenlinkchecker.service;

import com.unpar.brokenlinkchecker.model.CheckStatus;
import com.unpar.brokenlinkchecker.model.HttpLinkStatus;
import com.unpar.brokenlinkchecker.model.LinkResult;

import java.util.Set;

/**
 * Service utama untuk menjalankan proses crawling dan pengecekan tautan.
 * Menggunakan Crawler (Jsoup atau Playwright) dan HttpStatusChecker.
 * Hasil dikirim satu per satu ke UI menggunakan LinkResultListener.
 */
public class LinkCheckerService {

    private final Crawler crawler;
    private final HttpStatusChecker checker;

    public LinkCheckerService(Crawler crawler, HttpStatusChecker checker) {
        this.crawler = crawler;
        this.checker = checker;
    }

    /**
     * Menjalankan proses crawling dan pengecekan status tautan.
     * @param initialUrl URL awal situs yang akan diperiksa
     * @param listener callback untuk mengirim LinkResult satu per satu
     */
    public void run(String initialUrl, LinkResultListener listener) {
        // Jalankan crawling rekursif
        CrawlerResult result = crawler.crawl(initialUrl);

        // Tautan internal yang gagal diakses saat crawling dianggap rusak
        for (String broken : result.getBrokenLinks()) {
            listener.onResult(new LinkResult(broken, "Crawl Error", "-", "-"));
        }

        // Periksa status semua halaman internal (yang berhasil dicrawl)
        for (String url : result.getPageLinks()) {
            HttpLinkStatus status = checker.check(url);
            listener.onResult(new LinkResult(url, status.getCode(), "-", "-"));
        }


        // Periksa status semua link eksternal atau resource
        for (String url : result.getGeneralLinks()) {
            HttpLinkStatus status = checker.check(url);
            listener.onResult(new LinkResult(url, status.getCode(), "-", "-"));
        }
    }
}
