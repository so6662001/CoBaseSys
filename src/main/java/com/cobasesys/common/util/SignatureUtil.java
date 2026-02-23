package com.cobasesys.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

public final class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SignatureUtil() {}

    public static String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 signing failed", e);
        }
    }

    public static String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("MD5 hashing failed", e);
        }
    }

    public static String generateSign(String appSecret, String method, String path,
                                       String timestamp, String nonce, String bodyMd5) {
        String signContent = method.toUpperCase() + "\n" + path + "\n"
                + timestamp + "\n" + nonce + "\n" + bodyMd5;
        return hmacSha256(appSecret, signContent);
    }

    public static boolean verifySign(String appSecret, String method, String path,
                                      String timestamp, String nonce, String bodyMd5,
                                      String signature) {
        String expected = generateSign(appSecret, method, path, timestamp, nonce, bodyMd5);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String generateAppSecret() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
