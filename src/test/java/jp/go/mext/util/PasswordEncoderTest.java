package jp.go.mext.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    private static final String SALT = "mext2026";

    @Test
    void encodeAndMatch() {
        String encoded = PasswordEncoder.encode("password123", SALT);
        assertTrue(encoded != null && encoded.length() == 64, "SHA-256 应为 64 字符 hex");
        assertTrue(PasswordEncoder.matches("password123", SALT, encoded));
    }

    @Test
    void wrongPasswordFails() {
        String encoded = PasswordEncoder.encode("password123", SALT);
        assertFalse(PasswordEncoder.matches("password456", SALT, encoded));
    }

    @Test
    void differentSaltProducesDifferentHash() {
        String a = PasswordEncoder.encode("password123", "salt-a");
        String b = PasswordEncoder.encode("password123", "salt-b");
        assertFalse(a.equals(b));
    }

    @Test
    void nullsAreSafe() {
        assertFalse(PasswordEncoder.matches(null, SALT, "xxx"));
        assertFalse(PasswordEncoder.matches("xxx", SALT, null));
    }
}
