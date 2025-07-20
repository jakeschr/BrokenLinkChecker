package com.unpar.brokenlinkchecker.model;

/**
 * Status hasil pemeriksaan HTTP untuk sebuah tautan.
 */
public enum HttpLinkStatus {
    OK("200 OK"),
    BROKEN("4xx Client Error"),
    SERVER_ERROR("5xx Server Error"),
    TIMEOUT("Timeout"),
    UNKNOWN("Unknown");

    private final String code;

    HttpLinkStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
