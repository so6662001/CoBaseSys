package com.cobasesys.module.security;

import com.cobasesys.module.security.service.AnomalyDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectorTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private AnomalyDetector detector;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        detector = new AnomalyDetector(redisTemplate);
    }

    @Test
    void checkPointChange_normalAmount_shouldNotAlert() {
        when(valueOps.increment(anyString(), anyLong())).thenReturn(500L);
        detector.checkPointChange("U1", 500);
        verify(valueOps).increment(anyString(), eq(500L));
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void checkWalletChange_normalAmount() {
        when(valueOps.increment(anyString(), anyLong())).thenReturn(100000L);
        detector.checkWalletChange("U1", 100000);
        verify(valueOps).increment(anyString(), eq(100000L));
    }

    @Test
    void checkApiSignatureFailure() {
        when(valueOps.increment(anyString())).thenReturn(5L);
        detector.checkApiSignatureFailure("AK123", "192.168.1.1");
        verify(valueOps).increment(anyString());
    }

    @Test
    void checkAdminLoginFailure() {
        when(valueOps.increment(anyString())).thenReturn(3L);
        detector.checkAdminLoginFailure("admin", "192.168.1.1");
        verify(valueOps).increment(anyString());
    }
}
