package jp.go.mext.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码编码工具：SHA-256 + salt
 *
 * 原系统使用专有加密，这里用 SHA-256 + 配置文件 salt 模拟，
 * 主要演示"密码不可明文存储 + 不可逆"的工程实践。
 */
public final class PasswordEncoder {

    private PasswordEncoder() {}

    public static String encode(String rawPassword, String salt) {
        if (rawPassword == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String rawPassword, String salt, String encoded) {
        if (rawPassword == null || encoded == null) return false;
        return encode(rawPassword, salt).equalsIgnoreCase(encoded);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
