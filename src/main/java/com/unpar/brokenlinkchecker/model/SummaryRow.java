package com.unpar.brokenlinkchecker.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SummaryRow {
    private final SimpleStringProperty status;
    private final SimpleStringProperty pages;
    private final SimpleStringProperty links;
    private final SimpleStringProperty brokenLinks;

    public SummaryRow(String status, String pages, String links, String brokenLinks) {
        this.status = new SimpleStringProperty(status);
        this.pages = new SimpleStringProperty(pages);
        this.links = new SimpleStringProperty(links);
        this.brokenLinks = new SimpleStringProperty(brokenLinks);
    }

    public StringProperty statusProperty() { return status; }
    public StringProperty pagesProperty() { return pages; }
    public StringProperty linksProperty() { return links; }
    public StringProperty brokenLinksProperty() { return brokenLinks; }

    public void setStatus(String s) { status.set(s); }
    public void setPages(String p) { pages.set(p); }
    public void setLinks(String l) { links.set(l); }
    public void setBrokenLinks(String b) { brokenLinks.set(b); }
}
