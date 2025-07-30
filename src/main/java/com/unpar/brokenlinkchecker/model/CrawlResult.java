package com.unpar.brokenlinkchecker.model;

import java.util.List;

// Kembali gunakan allLinks, bukan hanya brokenLinks
public class CrawlResult {
    private final List<LinkResult> allLinks;
    private final int pageCount;
    private final int totalLinks;
    private final int brokenLinksCount;

    public CrawlResult(List<LinkResult> allLinks, int pageCount, int totalLinks, int brokenLinksCount) {
        this.allLinks = allLinks;
        this.pageCount = pageCount;
        this.totalLinks = totalLinks;
        this.brokenLinksCount = brokenLinksCount;
    }

    public List<LinkResult> getAllLinks() {
        return allLinks;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getTotalLinks() {
        return totalLinks;
    }

    public int getBrokenLinksCount() {
        return brokenLinksCount;
    }
}
