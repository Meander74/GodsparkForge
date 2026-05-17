package com.godspark.network.payload;

public final class PayloadSanitizer {

    private PayloadSanitizer() {}

    public static String cap(String value, int max) {
        if (value == null) {
            return "";
        }
        String cleaned = value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replaceAll("\\p{Cntrl}", "")
            .trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}