package com.xiuwen.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MD5 工具类。
 * 第一版按需求使用 MD5。真实生产环境建议改为 BCrypt/Argon2，并增加盐值和登录失败限制。
 */
public final class Md5Utils {
    private Md5Utils() {
    }

    /**
     * 对字符串进行 MD5 加密。
     *
     * @param raw 明文
     * @return 32 位小写 MD5
     */
    public static String md5(String raw) {
        if (raw == null) {
            raw = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(32);
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 加密失败", e);
        }
    }

    /**
     * 校验明文密码和数据库中的 MD5 值是否一致。
     */
    public static boolean matches(String rawPassword, String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        return md5(rawPassword).equalsIgnoreCase(passwordHash);
    }
}
