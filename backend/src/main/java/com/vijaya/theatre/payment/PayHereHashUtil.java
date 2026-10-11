package com.vijaya.theatre.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * PayHere's own checkout-request and IPN-verification signing scheme (not a
 * scheme we chose): MD5 is mandated by their API, not used for anything of
 * ours. See PayHere's "Checkout Integration Guide" for the exact algorithm.
 */
final class PayHereHashUtil {

    private PayHereHashUtil() {
    }

    static String requestHash(String merchantId, String orderId, String amount, String currency, String merchantSecret) {
        String hashedSecret = md5Hex(merchantSecret).toUpperCase();
        return md5Hex(merchantId + orderId + amount + currency + hashedSecret).toUpperCase();
    }

    static String notifySignature(String merchantId, String orderId, String payhereAmount, String payhereCurrency,
                                   String statusCode, String merchantSecret) {
        String hashedSecret = md5Hex(merchantSecret).toUpperCase();
        return md5Hex(merchantId + orderId + payhereAmount + payhereCurrency + statusCode + hashedSecret).toUpperCase();
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }
}
