package com.unpar.brokenlinkchecker.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * Model untuk merepresentasikan satu halaman yang berhasil di-crawl.
 * Digunakan untuk tabel daftar halaman.
 */
public class CrawledPage {

    private final StringProperty url = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();  // Misal: 200 OK, 404 Not Found
    private final IntegerProperty totalLinks = new SimpleIntegerProperty(); // Jumlah link ditemukan
    private final ObjectProperty<LocalDateTime> accessedTime = new SimpleObjectProperty<>(); // Waktu crawling

    public CrawledPage(String url, String status, int totalLinks, LocalDateTime accessedTime) {
        this.url.set(url);
        this.status.set(status);
        this.totalLinks.set(totalLinks);
        this.accessedTime.set(accessedTime);
    }

    // ===================== Getter & Setter =====================

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String value) {
        url.set(value);
    }

    public StringProperty urlProperty() {
        return url;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String value) {
        status.set(value);
    }

    public StringProperty statusProperty() {
        return status;
    }

    public int getTotalLinks() {
        return totalLinks.get();
    }

    public void setTotalLinks(int value) {
        totalLinks.set(value);
    }

    public IntegerProperty linkCountProperty() {
        return totalLinks;
    }

    public LocalDateTime getAccessedTime() {
        return accessedTime.get();
    }

    public void setAccessedTime(LocalDateTime value) {
        accessedTime.set(value);
    }

    public ObjectProperty<LocalDateTime> accessTimeProperty() {
        return accessedTime;
    }
}
