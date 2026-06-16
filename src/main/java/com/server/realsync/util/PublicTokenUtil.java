package com.server.realsync.util;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Generates and decodes secure public tokens for invoice sharing.
 * Token encodes: invoiceId (8 bytes) XOR'd with a secret salt.
 * Result is Base64URL-safe (no +/= chars) for clean URLs.
 */
public class PublicTokenUtil {

    // Secret salt bytes – 8 bytes for XOR with Long
    private static final byte[] SALT = {
        (byte) 0xA3, (byte) 0x7F, (byte) 0x2C, (byte) 0xE1,
        (byte) 0x55, (byte) 0x9B, (byte) 0xD4, (byte) 0x08
    };

    /**
     * Encode an invoice ID into a URL-safe public token.
     */
    public static String encode(long invoiceId) {
        byte[] raw = ByteBuffer.allocate(8).putLong(invoiceId).array();
        byte[] xored = new byte[8];
        for (int i = 0; i < 8; i++) {
            xored[i] = (byte) (raw[i] ^ SALT[i]);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(xored);
    }

    /**
     * Decode a public token back to the invoice ID.
     * Returns -1 if the token is invalid.
     */
    public static long decode(String token) {
        try {
            byte[] xored = Base64.getUrlDecoder().decode(token);
            if (xored.length != 8) return -1L;
            byte[] raw = new byte[8];
            for (int i = 0; i < 8; i++) {
                raw[i] = (byte) (xored[i] ^ SALT[i]);
            }
            return ByteBuffer.wrap(raw).getLong();
        } catch (Exception e) {
            return -1L;
        }
    }
}
