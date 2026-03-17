package com.cobasesys.module.security.service;

import com.cobasesys.common.util.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HashChainService {

    @Value("${cobasesys.security.chain-secret:default-chain-secret-key-32chars!}")
    private String chainSecret;

    @Value("${cobasesys.security.balance-secret:default-balance-secret-key-32c!}")
    private String balanceSecret;

    public String computeDataHash(String... fields) {
        return SignatureUtil.hmacSha256("data-hash-key", String.join("|", fields));
    }

    public String computeChainHash(String dataHash, String prevHash) {
        return SignatureUtil.hmacSha256(chainSecret, dataHash + "|" + prevHash);
    }

    public boolean verifyChainHash(String dataHash, String prevHash, String chainHash) {
        String expected = computeChainHash(dataHash, prevHash);
        return expected.equals(chainHash);
    }

    public String computeBalanceDigest(String... fields) {
        return SignatureUtil.hmacSha256(balanceSecret, String.join("|", fields));
    }

    public boolean verifyBalanceDigest(String digest, String... fields) {
        return digest.equals(computeBalanceDigest(fields));
    }
}
