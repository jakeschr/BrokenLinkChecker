package com.unpar.brokenlinkchecker.service;

/**
 * Interface untuk crawler.
 * Implementasi bisa menggunakan Jsoup, Playwright, dll.
 * Hasil crawling berupa kumpulan tautan internal, eksternal, dan rusak.
 */
public interface Crawler {

    /**
     * Melakukan crawling rekursif mulai dari initial URL.
     * @param initialUrl URL awal situs yang akan dicrawl
     * @return hasil crawling berupa objek CrawlerResult
     */
    CrawlerResult crawl(String initialUrl);
}
