package com.elsworne.stubline.spike.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WebhookSignature {
    private WebhookSignature() { }

    public static String sign(byte[] secret, long timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) { throw new IllegalStateException("Cannot sign webhook", e); }
    }

    public static boolean verify(byte[] secret, long timestamp, byte[] body, String signature) {
        if (signature == null || signature.length() != 64) return false;
        try {
            return MessageDigest.isEqual(HexFormat.of().parseHex(sign(secret, timestamp, body)), HexFormat.of().parseHex(signature));
        } catch (IllegalArgumentException e) { return false; }
    }
}
