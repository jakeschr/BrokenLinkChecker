package com.unpar.brokenlinkchecker.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementasi Crawler menggunakan Jsoup untuk memproses HTML statis.
 * Melakukan crawling rekursif hanya ke link internal (host sama).
 */
public class JsoupCrawler implements Crawler {

    private final Set<String> visited = new HashSet<>();
    private String baseHost;

    @Override
    public CrawlerResult crawl(String initialUrl) {
        CrawlerResult result = new CrawlerResult();

        try {
            baseHost = getHost(initialUrl);
            crawlRecursive(initialUrl, result);
        } catch (Exception e) {
            result.addBrokenLink(initialUrl); // initial url gagal
        }

        return result;
    }

    private void crawlRecursive(String url, CrawlerResult result) {
        if (visited.contains(url)) return;
        visited.add(url);

        Document doc;
        try {
            doc = Jsoup.connect(url).timeout(8000).get();
        } catch (Exception e) {
            result.addBrokenLink(url);
            return;
        }

        result.addPageLink(url); // halaman berhasil dibuka

        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.absUrl("href").split("#")[0].trim(); // hilangkan fragmen (#...)

            if (href.isEmpty() || visited.contains(href)) continue;

            if (isSameHost(href)) {
                crawlRecursive(href, result); // lanjutkan crawling ke halaman internal
            } else {
                result.addGeneralLink(href); // simpan link eksternal / umum
            }
        }
    }

    private String getHost(String url) throws URISyntaxException {
        return new URI(url).getHost();
    }

    private boolean isSameHost(String url) {
        try {
            return baseHost.equalsIgnoreCase(getHost(url));
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
