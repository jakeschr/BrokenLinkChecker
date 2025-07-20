package com.unpar.brokenlinkchecker.service;

import java.util.HashSet;
import java.util.Set;

/**
 * Menyimpan hasil proses crawling dari satu domain.
 * Berisi 3 jenis tautan:
 * - pageLinks: tautan internal (halaman)
 * - generalLinks: tautan eksternal atau resource
 * - brokenLinks: tautan internal yang gagal dicrawl (rusak)
 */
public class CrawlerResult {

    private final Set<String> pageLinks = new HashSet<>();
    private final Set<String> generalLinks = new HashSet<>();
    private final Set<String> brokenLinks = new HashSet<>();

    public void addPageLink(String url) {
        pageLinks.add(url);
    }

    public void addGeneralLink(String url) {
        generalLinks.add(url);
    }

    public void addBrokenLink(String url) {
        brokenLinks.add(url);
    }

    public Set<String> getPageLinks() {
        return pageLinks;
    }

    public Set<String> getGeneralLinks() {
        return generalLinks;
    }

    public Set<String> getBrokenLinks() {
        return brokenLinks;
    }
}
