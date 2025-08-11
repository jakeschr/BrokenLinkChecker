package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.CrawlResult;
import com.unpar.brokenlinkchecker.model.CrawledPage;
import com.unpar.brokenlinkchecker.model.LinkResult;
import com.unpar.brokenlinkchecker.util.HttpStatus;
import javafx.application.Platform;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Service {

    // ===================== State kontrol =====================
    private volatile boolean stopRequested = false;

    // ===================== Callback (event) =====================
    private Consumer<LinkResult> onLinkResult;
    private Consumer<CrawledPage> onCrawledPage;
    private Consumer<CrawlResult> onComplete;
    private Consumer<String> onError;
    private Consumer<Integer> onPageCountUpdate;
    private Consumer<Integer> onTotalLinkUpdate;
    private Consumer<Integer> onBrokenLinkUpdate;

    // ===================== HTTP Client =====================
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ===================== Setter callback =====================
    public void setOnLinkResult(Consumer<LinkResult> cb) {
        this.onLinkResult = cb;
    }

    public void setOnCrawledPage(Consumer<CrawledPage> cb) {
        this.onCrawledPage = cb;
    }

    public void setOnComplete(Consumer<CrawlResult> cb) {
        this.onComplete = cb;
    }

    public void setOnError(Consumer<String> cb) {
        this.onError = cb;
    }

    public void setOnPageCountUpdate(Consumer<Integer> cb) {
        this.onPageCountUpdate = cb;
    }

    public void setOnTotalLinkUpdate(Consumer<Integer> cb) {
        this.onTotalLinkUpdate = cb;
    }

    public void setOnBrokenLinkUpdate(Consumer<Integer> cb) {
        this.onBrokenLinkUpdate = cb;
    }

    // ===================== API kontrol =====================
    public void startCrawling(String seedUrl, String algorithm) {
        stopRequested = false;
        new Thread(() -> {
            try {
                if ("DFS".equalsIgnoreCase(algorithm)) {
                    crawlWithJsoupDfs(seedUrl);
                } else if ("BFS".equalsIgnoreCase(algorithm)) {
                    crawlWithJsoupBfs(seedUrl);
                } else {
                    emitError("Algoritma tidak dikenali: " + algorithm);
                }
            } catch (Exception ex) {
                emitError("Kesalahan tidak terduga: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
        }, "Crawler-Thread").start();
    }

    public void stop() {
        stopRequested = true;
    }

    // =========================================================
    // =============== Implementasi BFS (iteratif) =============
    // =========================================================
    private void crawlWithJsoupBfs(String seedUrl) {
        ResultAccumulator acc = new ResultAccumulator();
        String seedHost = safeHost(seedUrl);

        Set<String> visitedPages = new HashSet<>();
        ArrayDeque<String> frontier = new ArrayDeque<>();

        visitedPages.add(seedUrl);
        frontier.add(seedUrl);

        AtomicInteger pageCount = new AtomicInteger(0);
        AtomicInteger totalLinks = new AtomicInteger(0);
        AtomicInteger brokenCount = new AtomicInteger(0);

        while (!frontier.isEmpty() && !stopRequested) {
            String currentUrl = frontier.poll();
            LocalDateTime accessed = LocalDateTime.now();

            try {
                Document doc = Jsoup.connect(currentUrl)
                        .timeout(10_000) // 10 detik (10000 == 10_000)
                        .get();

                List<Element> anchors = doc.select("a[href]");
                int pageLinkCount = 0;

                for (Element a : anchors) {
                    if (stopRequested) break;

                    String href = a.absUrl("href").trim();
                    if (href.isEmpty() || href.startsWith("mailto:")) continue;

                    // Halaman dengan host yang sama akan dicrawl
                    String host = safeHost(href);
                    boolean sameHost = host.equalsIgnoreCase(seedHost);
                    if (sameHost && !visitedPages.contains(href)) {
                        visitedPages.add(href);
                        frontier.add(href);
                    }

                    // Cek status tautan (semua URL tetap dicek)
                    String status = checkStatus(href); // "200 OK", "404 Not Found", atau "FAILED (...)"
                    totalLinks.incrementAndGet();
                    pageLinkCount++;

                    LinkResult lr = new LinkResult(href, status, currentUrl, a.text());
                    acc.allLinks.add(lr);

                    if (isBrokenStatus(status)) {
                        brokenCount.incrementAndGet();
                        acc.brokenLinks.add(lr);
                        emitLink(lr); // stream ke UI (Broken Links)
                    }

                    emitTotal(totalLinks.get());
                    emitBroken(brokenCount.get());
                }

                // Emit data halaman yang berhasil dikunjungi
                emitPage(new CrawledPage(currentUrl, HttpStatus.getReasonPhrase(200), pageLinkCount, accessed));
                emitPageCount(pageCount.incrementAndGet());

            } catch (HttpStatusException hse) {
                String status = HttpStatus.getReasonPhrase(hse.getStatusCode());
                // Halaman ini sendiri dianggap tautan rusak (gagal dicrawl)
                LinkResult lr = new LinkResult(currentUrl, status, currentUrl, "(page error)");
                acc.allLinks.add(lr);
                acc.brokenLinks.add(lr);
                brokenCount.incrementAndGet();
                emitBroken(brokenCount.get());
                emitLink(lr);

                emitPage(new CrawledPage(currentUrl, status, 0, accessed));
                emitPageCount(pageCount.incrementAndGet());

            } catch (IOException ioe) {
                String status = "FAILED (" + ioe.getClass().getSimpleName() + ")";
                LinkResult lr = new LinkResult(currentUrl, status, currentUrl, "(exception)");
                acc.allLinks.add(lr);
                acc.brokenLinks.add(lr);
                brokenCount.incrementAndGet();
                emitBroken(brokenCount.get());
                emitLink(lr);

                emitPage(new CrawledPage(currentUrl, status, 0, accessed));
                emitPageCount(pageCount.incrementAndGet());
            }
        }

        if (!stopRequested) {
            emitComplete(new CrawlResult(acc.allLinks, pageCount.get(), totalLinks.get(), brokenCount.get()));
        }
    }

    // =========================================================
    // =============== Implementasi DFS (iteratif) =============
    // =========================================================
    private void crawlWithJsoupDfs(String seedUrl) {
        ResultAccumulator acc = new ResultAccumulator();
        String seedHost = safeHost(seedUrl);

        Set<String> visitedPages = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        visitedPages.add(seedUrl);
        stack.push(seedUrl);

        AtomicInteger pageCount = new AtomicInteger(0);
        AtomicInteger totalLinks = new AtomicInteger(0);
        AtomicInteger brokenCount = new AtomicInteger(0);

        while (!stack.isEmpty() && !stopRequested) {
            String currentUrl = stack.pop();
            LocalDateTime accessed = LocalDateTime.now();

            try {
                Document doc = Jsoup.connect(currentUrl)
                        .timeout(10_000)
                        .get();

                List<Element> anchors = doc.select("a[href]");
                int pageLinkCount = 0;

                // Untuk DFS yang lebih deterministik: iterasi dari belakang,
                // sehingga urutan di stack mengikuti urutan muncul di dokumen.
                ListIterator<Element> it = anchors.listIterator(anchors.size());
                while (it.hasPrevious() && !stopRequested) {
                    Element a = it.previous();

                    String href = a.absUrl("href").trim();
                    if (href.isEmpty() || href.startsWith("mailto:")) continue;

                    String host = safeHost(href);
                    boolean sameHost = host.equalsIgnoreCase(seedHost);
                    if (sameHost && !visitedPages.contains(href)) {
                        visitedPages.add(href);
                        stack.push(href);
                    }

                    String status = checkStatus(href);
                    totalLinks.incrementAndGet();
                    pageLinkCount++;

                    LinkResult lr = new LinkResult(href, status, currentUrl, a.text());
                    acc.allLinks.add(lr);

                    if (isBrokenStatus(status)) {
                        brokenCount.incrementAndGet();
                        acc.brokenLinks.add(lr);
                        emitLink(lr);
                    }

                    emitTotal(totalLinks.get());
                    emitBroken(brokenCount.get());
                }

                emitPage(new CrawledPage(currentUrl, HttpStatus.getReasonPhrase(200), pageLinkCount, accessed));
                emitPageCount(pageCount.incrementAndGet());

            } catch (HttpStatusException hse) {
                String status = HttpStatus.getReasonPhrase(hse.getStatusCode());
                LinkResult lr = new LinkResult(currentUrl, status, currentUrl, "(page error)");
                acc.allLinks.add(lr);
                acc.brokenLinks.add(lr);
                brokenCount.incrementAndGet();
                emitBroken(brokenCount.get());
                emitLink(lr);

                emitPage(new CrawledPage(currentUrl, status, 0, accessed));
                emitPageCount(pageCount.incrementAndGet());

            } catch (IOException ioe) {
                String status = "FAILED (" + ioe.getClass().getSimpleName() + ")";
                LinkResult lr = new LinkResult(currentUrl, status, currentUrl, "(exception)");
                acc.allLinks.add(lr);
                acc.brokenLinks.add(lr);
                brokenCount.incrementAndGet();
                emitBroken(brokenCount.get());
                emitLink(lr);

                emitPage(new CrawledPage(currentUrl, status, 0, accessed));
                emitPageCount(pageCount.incrementAndGet());
            }
        }

        if (!stopRequested) {
            emitComplete(new CrawlResult(acc.allLinks, pageCount.get(), totalLinks.get(), brokenCount.get()));
        }
    }

    // =========================================================
    // ========================= Helper ========================
    // =========================================================
    private static final class ResultAccumulator {
        final List<LinkResult> allLinks = new ArrayList<>();
        final List<LinkResult> brokenLinks = new ArrayList<>();
    }

    private String checkStatus(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();

            // Kembalikan format konsisten, mis. "200 OK", "404 Not Found"
            return HttpStatus.getReasonPhrase(code);

        } catch (Exception ex) {
            // Saat gagal (DNS/SSL/timeout, dll.), tandai FAILED.
            return "FAILED (" + ex.getClass().getSimpleName() + ")";
        } finally {
            // Rate limit kecil untuk sopan ke server & meredam lonjakan request
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private boolean isBrokenStatus(String status) {
        // String status selalu diawali kode jika sukses (4xx/5xx terdeteksi),
        // atau "FAILED (...)" jika exception.
        return status.startsWith("4") || status.startsWith("5") || status.startsWith("FAILED");
    }

    private String safeHost(String url) {
        try {
            URI u = new URI(url);
            String h = u.getHost();
            return h == null ? "" : h.toLowerCase();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    // ===================== Emitters (JavaFX thread) =====================
    private void emitLink(LinkResult lr) {
        if (onLinkResult != null) Platform.runLater(() -> onLinkResult.accept(lr));
    }

    private void emitPage(CrawledPage cp) {
        if (onCrawledPage != null) Platform.runLater(() -> onCrawledPage.accept(cp));
    }

    private void emitComplete(CrawlResult cr) {
        if (onComplete != null) Platform.runLater(() -> onComplete.accept(cr));
    }

    private void emitError(String msg) {
        if (onError != null) Platform.runLater(() -> onError.accept(msg));
    }

    private void emitPageCount(int n) {
        if (onPageCountUpdate != null) Platform.runLater(() -> onPageCountUpdate.accept(n));
    }

    private void emitTotal(int n) {
        if (onTotalLinkUpdate != null) Platform.runLater(() -> onTotalLinkUpdate.accept(n));
    }

    private void emitBroken(int n) {
        if (onBrokenLinkUpdate != null) Platform.runLater(() -> onBrokenLinkUpdate.accept(n));
    }
}
