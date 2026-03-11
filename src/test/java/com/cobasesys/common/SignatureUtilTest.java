package com.cobasesys.common;

import com.cobasesys.common.util.IdGenerator;
import com.cobasesys.common.util.SignatureUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignatureUtilTest {

    @Test
    void hmacSha256_shouldProduceConsistentHash() {
        String hash1 = SignatureUtil.hmacSha256("secret", "data");
        String hash2 = SignatureUtil.hmacSha256("secret", "data");
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void hmacSha256_differentSecrets_shouldProduceDifferentHashes() {
        String hash1 = SignatureUtil.hmacSha256("secret1", "data");
        String hash2 = SignatureUtil.hmacSha256("secret2", "data");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void md5_shouldProduceConsistentHash() {
        String hash1 = SignatureUtil.md5("hello");
        String hash2 = SignatureUtil.md5("hello");
        assertEquals(hash1, hash2);
        assertEquals(32, hash1.length());
    }

    @Test
    void generateSign_andVerify_shouldMatch() {
        String secret = "test-secret";
        String method = "POST";
        String path = "/api/v1/points/earn";
        String timestamp = "1234567890";
        String nonce = "abc123";
        String bodyMd5 = SignatureUtil.md5("{\"userId\":\"U1\"}");

        String signature = SignatureUtil.generateSign(secret, method, path, timestamp, nonce, bodyMd5);
        assertTrue(SignatureUtil.verifySign(secret, method, path, timestamp, nonce, bodyMd5, signature));
    }

    @Test
    void verifySign_wrongSignature_shouldFail() {
        assertFalse(SignatureUtil.verifySign("secret", "POST", "/path", "123", "nonce", "md5", "wrong"));
    }

    @Test
    void generateAppSecret_shouldBeUnique() {
        String s1 = SignatureUtil.generateAppSecret();
        String s2 = SignatureUtil.generateAppSecret();
        assertNotEquals(s1, s2);
        assertTrue(s1.length() >= 40);
    }

    @Test
    void idGenerator_shouldProduceUniqueIds() {
        String id1 = IdGenerator.pointTransactionNo();
        String id2 = IdGenerator.pointTransactionNo();
        assertNotEquals(id1, id2);
        assertTrue(id1.startsWith("PT"));
    }

    @Test
    void idGenerator_walletTransactionNo() {
        String id = IdGenerator.walletTransactionNo();
        assertTrue(id.startsWith("WT"));
    }

    @Test
    void idGenerator_rechargeOrderNo() {
        String id = IdGenerator.rechargeOrderNo();
        assertTrue(id.startsWith("RC"));
    }
}
