package com.unpar.brokenlinkchecker.service;

import com.unpar.brokenlinkchecker.model.LinkResult;

/**
 * Interface listener untuk menerima hasil pemeriksaan link secara streaming.
 * Dipanggil setiap kali satu LinkResult tersedia.
 */
public interface LinkResultListener {
    void onResult(LinkResult result);
}
