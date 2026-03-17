package com.cobasesys.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private IdGenerator() {}

    public static String generate(String prefix) {
        long seq = SEQUENCE.incrementAndGet() % 100000;
        int rand = ThreadLocalRandom.current().nextInt(1000);
        return prefix + LocalDateTime.now().format(FMT)
                + String.format("%05d", seq)
                + String.format("%03d", rand);
    }

    public static String pointTransactionNo() {
        return generate("PT");
    }

    public static String walletTransactionNo() {
        return generate("WT");
    }

    public static String rechargeOrderNo() {
        return generate("RC");
    }

    public static String appKey() {
        return generate("AK");
    }
}
