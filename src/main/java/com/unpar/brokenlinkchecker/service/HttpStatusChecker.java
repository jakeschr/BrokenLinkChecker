package com.unpar.brokenlinkchecker.service;

import com.unpar.brokenlinkchecker.model.HttpLinkStatus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Melakukan pengecekan status HTTP dari suatu tautan.
 * Mengembalikan enum HttpLinkStatus berdasarkan kode status HTTP.
 */
public class HttpStatusChecker {

    /**
     * Memeriksa status HTTP dari URL.
     * @param urlStr URL yang akan dicek
     * @return HttpLinkStatus sesuai hasil koneksi
     */
    public HttpLinkStatus check(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) return HttpLinkStatus.OK;
            if (code >= 400 && code < 500) return HttpLinkStatus.BROKEN;
            if (code >= 500) return HttpLinkStatus.SERVER_ERROR;
            return HttpLinkStatus.UNKNOWN;
        } catch (IOException e) {
            return HttpLinkStatus.TIMEOUT;
        }
    }
}
