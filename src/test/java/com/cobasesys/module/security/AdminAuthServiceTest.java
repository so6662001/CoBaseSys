package com.cobasesys.module.security;

import com.cobasesys.common.exception.BizException;
import com.cobasesys.module.security.service.AdminAuthService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuthServiceTest {

    @Test
    void validatePasswordStrength_validPassword() {
        assertDoesNotThrow(() -> AdminAuthService.validatePasswordStrength("Admin123"));
        assertDoesNotThrow(() -> AdminAuthService.validatePasswordStrength("MyP@ss99"));
        assertDoesNotThrow(() -> AdminAuthService.validatePasswordStrength("abcdef12"));
    }

    @Test
    void validatePasswordStrength_tooShort() {
        assertThrows(BizException.class, () -> AdminAuthService.validatePasswordStrength("Ab1"));
        assertThrows(BizException.class, () -> AdminAuthService.validatePasswordStrength("short1"));
    }

    @Test
    void validatePasswordStrength_noDigit() {
        assertThrows(BizException.class, () -> AdminAuthService.validatePasswordStrength("abcdefgh"));
    }

    @Test
    void validatePasswordStrength_noLetter() {
        assertThrows(BizException.class, () -> AdminAuthService.validatePasswordStrength("12345678"));
    }

    @Test
    void validatePasswordStrength_null() {
        assertThrows(BizException.class, () -> AdminAuthService.validatePasswordStrength(null));
    }
}
