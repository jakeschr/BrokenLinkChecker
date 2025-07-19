package com.unpar.brokenlinkchecker.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LinkResult {
    private final StringProperty brokenLink;
    private final StringProperty status;
    private final StringProperty sourcePage;
    private final StringProperty anchorText;

    public LinkResult(String brokenLink, String status, String sourcePage, String anchorText) {
        this.brokenLink = new SimpleStringProperty(brokenLink);
        this.status = new SimpleStringProperty(status);
        this.sourcePage = new SimpleStringProperty(sourcePage);
        this.anchorText = new SimpleStringProperty(anchorText);
    }

    public String getBrokenUrl() {
        return brokenLink.get();
    }

    public StringProperty brokenLinkProperty() {
        return brokenLink;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public String getSourcePage() {
        return sourcePage.get();
    }

    public StringProperty sourcePageProperty() {
        return sourcePage;
    }

    public String getAnchorText() {
        return anchorText.get();
    }

    public StringProperty anchorTextProperty() {
        return anchorText;
    }
}
