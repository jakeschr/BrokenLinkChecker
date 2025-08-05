package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.CrawlResult;
import com.unpar.brokenlinkchecker.model.LinkResult;
import com.unpar.brokenlinkchecker.util.HttpStatus;

import java.util.*;
import java.util.function.Consumer;

import java.io.IOException;

import org.jsoup.HttpStatusException;


/**
 * Kelas Service bertanggung jawab untuk menjalankan proses web crawling
 * menggunakan algoritma Breadth-First Search (BFS) dan teknologi Jsoup.
 * <p>
 * Fitur utama:
 * - Melakukan crawling terhadap situs web untuk menemukan tautan.
 * - Memeriksa status setiap tautan (kode HTTP).
 * - Mengirim hasil crawling secara streaming ke UI menggunakan Consumer.
 * - Menghitung dan mengirimkan update jumlah halaman, total tautan, dan tautan rusak secara real-time.
 * - Dapat dihentikan dengan perintah `stop()`.
 */
public class Service {

    // Callback untuk mengirimkan hasil tautan rusak satu per satu (streaming)
    private Consumer<LinkResult> onLinkResult;

    // Callback saat crawling selesai dengan ringkasan hasil
    private Consumer<CrawlResult> onComplete;

    // Callback jika terjadi kesalahan
    private Consumer<String> onError;

    // Callback untuk update jumlah halaman (unique page dari host yang sama)
    private Consumer<Integer> onPageCountUpdate;

    // Callback untuk update jumlah total tautan yang ditemukan
    private Consumer<Integer> onTotalLinkUpdate;

    // Callback untuk update jumlah tautan rusak (4xx, 5xx, atau gagal)
    private Consumer<Integer> onBrokenLinkUpdate;

    // Flag yang menandakan apakah crawling sedang diminta untuk dihentikan
    private volatile boolean stopRequested = false;

    /**
     * Memulai proses crawling berdasarkan URL awal dan nama algoritma yang digunakan
     * Proses dilakukan di thread terpisah agar tidak memblok UI.
     *
     * @param seedUrl   URL awal (root)
     * @param algorithm Algoritma yang digunakan (BFS/DFS)
     */
    public void startCrawling(String seedUrl, String algorithm) {
        Thread thread = new Thread(() -> {
            try {
                if ("BFS".equalsIgnoreCase(algorithm)) {
                    crawlWithJsoup(seedUrl);
                } else {
                    if (onError != null)
                        onError.accept("Algoritma belum didukung: " + algorithm);
                }
            } catch (Exception e) {
                if (onError != null)
                    onError.accept("Error saat crawling: " + e.getMessage());
            }
        });
        thread.setDaemon(true); // Thread daemon agar mati jika aplikasi ditutup
        thread.start(); // Menjalankan crawling secara paralel
    }

    /**
     * Menghentikan proses crawling secara manual.
     */
    public void stop() {
        stopRequested = true;
    }

    // ===================== SETTER CALLBACK ======================

    public void setOnLinkResult(Consumer<LinkResult> consumer) {
        this.onLinkResult = consumer;
    }

    public void setOnComplete(Consumer<CrawlResult> consumer) {
        this.onComplete = consumer;
    }

    public void setOnError(Consumer<String> consumer) {
        this.onError = consumer;
    }

    public void setOnPageCountUpdate(Consumer<Integer> consumer) {
        this.onPageCountUpdate = consumer;
    }

    public void setOnTotalLinkUpdate(Consumer<Integer> consumer) {
        this.onTotalLinkUpdate = consumer;
    }

    public void setOnBrokenLinkUpdate(Consumer<Integer> consumer) {
        this.onBrokenLinkUpdate = consumer;
    }

    /**
     * Metode utama untuk melakukan crawling menggunakan Jsoup dengan pendekatan BFS.
     * Mengunjungi semua halaman dari host yang sama dan memeriksa semua tautan di dalamnya.
     *
     * @param seedUrl URL awal untuk crawling
     */
    private void crawlWithJsoup(String seedUrl) {
        Set<String> visitedPages = new HashSet<>();      // Menyimpan halaman yang sudah dikunjungi
        Queue<String> frontier = new LinkedList<>();     // Antrian BFS untuk halaman yang akan dikunjungi
        List<LinkResult> allLinks = new ArrayList<>();   // Semua tautan (baik yang rusak maupun tidak)
        List<LinkResult> brokenLinks = new ArrayList<>();// Hanya tautan yang rusak

        frontier.add(seedUrl);
        visitedPages.add(seedUrl);
        updatePageCount(visitedPages.size());

        while (!frontier.isEmpty() && !stopRequested) {
            String currentUrl = frontier.poll(); // Mengambil halaman berikutnya

            try {
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(currentUrl)
                        .timeout(10_000) // timeout 10 detik
                        .get();

                org.jsoup.select.Elements links = doc.select("a[href]"); // Ambil semua elemen <a href=...>

                for (org.jsoup.nodes.Element link : links) {
                    String href = link.absUrl("href").trim(); // Mendapatkan absolute URL

                    // Lewati tautan yang kosong atau non-HTTP seperti mailto:
                    if (href.isEmpty() || href.startsWith("mailto:")) continue;

                    String anchorText = link.text(); // Teks anchor
                    boolean isSameHost = isSameDomain(seedUrl, href);

                    // Jika halaman dari host yang sama dan belum dikunjungi, tambahkan ke frontier
                    if (isSameHost && !visitedPages.contains(href)) {
                        visitedPages.add(href);
                        frontier.add(href);
                        updatePageCount(visitedPages.size());
                    }

                    // Periksa status tautan
                    String status = checkStatus(href);

                    LinkResult result = new LinkResult(href, status, currentUrl, anchorText);
                    allLinks.add(result);
                    updateTotalLinkCount(allLinks.size());

                    // Jika status rusak (kode 4xx/5xx atau gagal koneksi), kirim ke UI
                    if (status.startsWith("4") || status.startsWith("5") || status.startsWith("FAILED")) {
                        brokenLinks.add(result);
                        updateBrokenLinkCount(brokenLinks.size());

                        if (onLinkResult != null) {
                            onLinkResult.accept(result);
                        }
                    }
                }

            } catch (org.jsoup.HttpStatusException e) {
                // Error HTTP (misal 404, 500)
                int statusCode = e.getStatusCode();
                String reason = HttpStatus.getReasonPhrase(statusCode);
                String status = statusCode + " " + reason;

                LinkResult result = new LinkResult(currentUrl, status, currentUrl, "");
                allLinks.add(result);
                brokenLinks.add(result);
                updateTotalLinkCount(allLinks.size());
                updateBrokenLinkCount(brokenLinks.size());

                if (onLinkResult != null) {
                    onLinkResult.accept(result);
                }

            } catch (IOException e) {
                // Error koneksi lainnya (timeout, DNS error, SSL, dsb)
                String status = "FAILED (" + e.getClass().getSimpleName() + ")";

                LinkResult result = new LinkResult(currentUrl, status, currentUrl, "");
                allLinks.add(result);
                brokenLinks.add(result);
                updateTotalLinkCount(allLinks.size());
                updateBrokenLinkCount(brokenLinks.size());

                if (onLinkResult != null) {
                    onLinkResult.accept(result);
                }
            }
        }

        // Kirim ringkasan hasil jika tidak dihentikan
        if (onComplete != null && !stopRequested) {
            CrawlResult summary = new CrawlResult(
                    allLinks,
                    visitedPages.size(),
                    allLinks.size(),
                    brokenLinks.size()
            );
            onComplete.accept(summary);
        }
    }


    // ===================== HELPER UNTUK UPDATE UI ======================

    private void updatePageCount(int count) {
        if (onPageCountUpdate != null) onPageCountUpdate.accept(count);
    }

    private void updateTotalLinkCount(int count) {
        if (onTotalLinkUpdate != null) onTotalLinkUpdate.accept(count);
    }

    private void updateBrokenLinkCount(int count) {
        if (onBrokenLinkUpdate != null) onBrokenLinkUpdate.accept(count);
    }

    /**
     * Memeriksa status HTTP dari suatu URL menggunakan metode HEAD.
     * Menambahkan delay 300ms sebagai bentuk rate limiting sederhana.
     *
     * @param url URL yang akan diperiksa
     * @return Status kode HTTP (misalnya "200 OK", "404 Not Found", atau deskripsi error)
     */
    private String checkStatus(String url) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            java.net.http.HttpResponse<Void> response = client.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.discarding()
            );

            Thread.sleep(300); // Rate limiting

            return HttpStatus.getReasonPhrase(response.statusCode());

        } catch (IllegalArgumentException e) {
            return "Invalid URL: " + e.getMessage(); // Jika URL tidak valid
        } catch (java.net.UnknownHostException e) {
            return "Unknown Host: " + e.getMessage(); // Jika DNS gagal
        } catch (java.net.ConnectException e) {
            return "Connection Refused: " + e.getMessage(); // Server tidak merespons
        } catch (java.net.http.HttpTimeoutException e) {
            return "Timeout: " + e.getMessage(); // Timeout dari client
        } catch (Exception e) {
            return "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage(); // Catch-all fallback
        }
    }


    /**
     * Membandingkan dua URL apakah berasal dari domain (host) yang sama.
     *
     * @param base  URL awal
     * @param other URL yang dibandingkan
     * @return true jika host sama, false jika beda host atau error
     */
    private boolean isSameDomain(String base, String other) {
        try {
            java.net.URI baseUri = new java.net.URI(base);
            java.net.URI otherUri = new java.net.URI(other);
            return baseUri.getHost() != null &&
                    baseUri.getHost().equalsIgnoreCase(otherUri.getHost());
        } catch (Exception e) {
            return false;
        }
    }
}
