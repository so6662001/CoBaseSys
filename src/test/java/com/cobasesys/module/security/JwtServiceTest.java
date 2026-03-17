package com.cobasesys.module.security;

import com.cobasesys.module.security.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "test-jwt-secret-key-must-be-at-least-32-chars");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 60000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 300000L);
    }

    @Test
    void generateAndParseToken() {
        List<String> perms = List.of("billing:order:view", "points:account:view");
        String token = jwtService.generateToken(1L, "admin", perms);

        assertTrue(jwtService.isValid(token));
        assertEquals("admin", jwtService.getUsername(token));
        assertEquals(1L, jwtService.getUserId(token));
        assertEquals(perms, jwtService.getPermissions(token));
    }

    @Test
    void generateRefreshToken_shouldContainTypeRefresh() {
        String token = jwtService.generateRefreshToken(1L, "admin");
        assertTrue(jwtService.isValid(token));
        Claims claims = jwtService.parseToken(token);
        assertEquals("refresh", claims.get("type"));
    }

    @Test
    void invalidToken_shouldReturnFalse() {
        assertFalse(jwtService.isValid("invalid.token.here"));
        assertFalse(jwtService.isValid(""));
        assertFalse(jwtService.isValid(null));
    }

    @Test
    void expiredToken_shouldBeInvalid() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);
        String token = jwtService.generateToken(1L, "admin", List.of());
        assertFalse(jwtService.isValid(token));
    }

    @Test
    void differentSecrets_shouldNotValidate() {
        String token = jwtService.generateToken(1L, "admin", List.of());

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "jwtSecret", "different-secret-key-also-32-chars-long!!");
        ReflectionTestUtils.setField(otherService, "jwtExpirationMs", 60000L);
        ReflectionTestUtils.setField(otherService, "refreshExpirationMs", 300000L);

        assertFalse(otherService.isValid(token));
    }
}
