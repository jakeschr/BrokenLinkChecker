package com.unpar.brokenlinkchecker.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitas untuk memetakan kode status HTTP ke reason phrase (misalnya "404 Not Found").
 * Berdasarkan standar dari RFC 9110 dan beberapa penambahan umum (seperti 429, 431, 451).
 */
public class HttpStatus {

    private static final Map<Integer, String> STATUS_REASON_MAP = new HashMap<>();

    static {
        // 4xx - Client Errors
        STATUS_REASON_MAP.put(400, "400 Bad Request");
        STATUS_REASON_MAP.put(401, "401 Unauthorized");
        STATUS_REASON_MAP.put(402, "402 Payment Required");
        STATUS_REASON_MAP.put(403, "403 Forbidden");
        STATUS_REASON_MAP.put(404, "404 Not Found");
        STATUS_REASON_MAP.put(405, "405 Method Not Allowed");
        STATUS_REASON_MAP.put(406, "406 Not Acceptable");
        STATUS_REASON_MAP.put(407, "407 Proxy Authentication Required");
        STATUS_REASON_MAP.put(408, "408 Request Timeout");
        STATUS_REASON_MAP.put(409, "409 Conflict");
        STATUS_REASON_MAP.put(410, "410 Gone");
        STATUS_REASON_MAP.put(411, "411 Length Required");
        STATUS_REASON_MAP.put(412, "412 Precondition Failed");
        STATUS_REASON_MAP.put(413, "413 Content Too Large");
        STATUS_REASON_MAP.put(414, "414 URI Too Long");
        STATUS_REASON_MAP.put(415, "415 Unsupported Media Type");
        STATUS_REASON_MAP.put(416, "416 Range Not Satisfiable");
        STATUS_REASON_MAP.put(417, "417 Expectation Failed");
        STATUS_REASON_MAP.put(418, "418 I'm a teapot");
        STATUS_REASON_MAP.put(421, "421 Misdirected Request");
        STATUS_REASON_MAP.put(422, "422 Unprocessable Content");
        STATUS_REASON_MAP.put(423, "423 Locked");
        STATUS_REASON_MAP.put(424, "424 Failed Dependency");
        STATUS_REASON_MAP.put(425, "425 Too Early");
        STATUS_REASON_MAP.put(426, "426 Upgrade Required");
        STATUS_REASON_MAP.put(428, "428 Precondition Required");
        STATUS_REASON_MAP.put(429, "429 Too Many Requests");
        STATUS_REASON_MAP.put(431, "431 Request Header Fields Too Large");
        STATUS_REASON_MAP.put(451, "451 Unavailable For Legal Reasons");

        // 5xx - Server Errors
        STATUS_REASON_MAP.put(500, "500 Internal Server Error");
        STATUS_REASON_MAP.put(501, "501 Not Implemented");
        STATUS_REASON_MAP.put(502, "502 Bad Gateway");
        STATUS_REASON_MAP.put(503, "503 Service Unavailable");
        STATUS_REASON_MAP.put(504, "504 Gateway Timeout");
        STATUS_REASON_MAP.put(505, "505 HTTP Version Not Supported");
        STATUS_REASON_MAP.put(506, "506 Variant Also Negotiates");
        STATUS_REASON_MAP.put(507, "507 Insufficient Storage");
        STATUS_REASON_MAP.put(508, "508 Loop Detected");
        STATUS_REASON_MAP.put(510, "510 Not Extended");
        STATUS_REASON_MAP.put(511, "511 Network Authentication Required");
    }

    /**
     * Mengembalikan reason phrase berdasarkan kode status HTTP.
     * Jika tidak ditemukan, akan mengembalikan hanya angka kodenya sebagai String.
     *
     * @param statusCode kode status HTTP
     * @return reason phrase lengkap, misalnya "404 Not Found"
     */
    public static String getReasonPhrase(int statusCode) {
        return STATUS_REASON_MAP.getOrDefault(statusCode, String.valueOf(statusCode));
    }
}
