package com.cobasesys.module.security;

import com.cobasesys.module.security.service.HashChainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class HashChainServiceTest {

    private HashChainService hashChainService;

    @BeforeEach
    void setUp() {
        hashChainService = new HashChainService();
        ReflectionTestUtils.setField(hashChainService, "chainSecret", "test-chain-secret-32chars!!!!!!!");
        ReflectionTestUtils.setField(hashChainService, "balanceSecret", "test-balance-secret-32chars!!!!!");
    }

    @Test
    void computeDataHash_shouldBeConsistent() {
        String h1 = hashChainService.computeDataHash("field1", "field2", "field3");
        String h2 = hashChainService.computeDataHash("field1", "field2", "field3");
        assertEquals(h1, h2);
    }

    @Test
    void computeDataHash_differentData_shouldDiffer() {
        String h1 = hashChainService.computeDataHash("field1", "100");
        String h2 = hashChainService.computeDataHash("field1", "200");
        assertNotEquals(h1, h2);
    }

    @Test
    void computeChainHash_shouldBeConsistent() {
        String ch1 = hashChainService.computeChainHash("datahash1", "prevhash1");
        String ch2 = hashChainService.computeChainHash("datahash1", "prevhash1");
        assertEquals(ch1, ch2);
    }

    @Test
    void verifyChainHash_validChain_shouldPass() {
        String dataHash = hashChainService.computeDataHash("tx1", "100", "0", "100");
        String prevHash = "0000000000000000";
        String chainHash = hashChainService.computeChainHash(dataHash, prevHash);

        assertTrue(hashChainService.verifyChainHash(dataHash, prevHash, chainHash));
    }

    @Test
    void verifyChainHash_tamperedData_shouldFail() {
        String dataHash = hashChainService.computeDataHash("tx1", "100", "0", "100");
        String prevHash = "0000000000000000";
        String chainHash = hashChainService.computeChainHash(dataHash, prevHash);

        String tamperedData = hashChainService.computeDataHash("tx1", "999", "0", "999");
        assertFalse(hashChainService.verifyChainHash(tamperedData, prevHash, chainHash));
    }

    @Test
    void chainIntegrity_multipleRecords() {
        String prevHash = "0000000000000000";

        String data1 = hashChainService.computeDataHash("tx1", "100");
        String chain1 = hashChainService.computeChainHash(data1, prevHash);

        String data2 = hashChainService.computeDataHash("tx2", "200");
        String chain2 = hashChainService.computeChainHash(data2, chain1);

        String data3 = hashChainService.computeDataHash("tx3", "300");
        String chain3 = hashChainService.computeChainHash(data3, chain2);

        assertTrue(hashChainService.verifyChainHash(data1, prevHash, chain1));
        assertTrue(hashChainService.verifyChainHash(data2, chain1, chain2));
        assertTrue(hashChainService.verifyChainHash(data3, chain2, chain3));

        assertFalse(hashChainService.verifyChainHash(data2, prevHash, chain2));
    }

    @Test
    void balanceDigest_shouldVerify() {
        String digest = hashChainService.computeBalanceDigest("1", "1000", "0", "5000", "3000", "5");
        assertTrue(hashChainService.verifyBalanceDigest(digest, "1", "1000", "0", "5000", "3000", "5"));
    }

    @Test
    void balanceDigest_tamperedBalance_shouldFail() {
        String digest = hashChainService.computeBalanceDigest("1", "1000", "0", "5000", "3000", "5");
        assertFalse(hashChainService.verifyBalanceDigest(digest, "1", "9999", "0", "5000", "3000", "5"));
    }
}
