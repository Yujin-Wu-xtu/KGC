package com.wyj.kgc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Token 供应器 (制造和检验机)
 *
 * 声明为 @Component，使其成为一个 Spring Bean，
 * 这样我们就可以在其他 Service (如 UserService) 中注入它。
 */
@Component
public class JwtTokenProvider {

    // 1. 从 application.properties 注入 "签名密钥" (Base64 编码)
    private final String jwtSecretKey;

    // 2. 从 application.properties 注入 "有效时间" (毫秒)
    private final long jwtExpirationMs;

    // 3. jjwt 库中用于签名的 "密钥" 对象
    private final SecretKey key;

    // 构造函数：当 Spring 创建这个 Bean 时，会自动注入配置并初始化 "key"
    public JwtTokenProvider(
            @Value("${jwt.secret-key}") String jwtSecretKey,
            @Value("${jwt.expiration-ms}") long jwtExpirationMs) {

        this.jwtSecretKey = jwtSecretKey;
        this.jwtExpirationMs = jwtExpirationMs;

        // 【关键】将 Base64 编码的密钥字符串解码为字节数组
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecretKey);
        // 【关键】使用解码后的字节数组生成一个 HMAC-SHA 密钥对象
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    // 📋 添加到 JwtTokenProvider 类中
    public String createToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 86400000); // 1天后过期

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key) // 0.12.3 新写法，自动匹配算法
                .compact();
    }

    /**
     * 【制造 Token】
     * 为指定用户生成一个新的 JWT Token
     *
     * @param username 用户的用户名 (e.g., "teacher_wu")
     * @return 一个代表该用户的 JWT 字符串
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + this.jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username) // 1. 将 "username" 作为 Token 的 "主题" (Subject)
                .setIssuedAt(now)     // 2. Token 的签发时间
                .setExpiration(expiryDate) // 3. Token 的过期时间 (e.g., 24小时后)
                .signWith(this.key)   // 4. 【关键】使用我们的 "密钥" 签名
                .compact(); // 5. 组装成一个紧凑的字符串
    }

    /**
     * 【检验 Token - 步骤 A】
     * 从 Token 中解析出 "用户名" (Subject)
     *
     * @param token 客户端传来的 JWT 字符串
     * @return Token 中存储的用户名
     */
    public String getUsernameFromToken(String token) {
        // "parserBuilder" 会使用同一个 "key" 来验证签名
        // 如果签名不匹配、或 Token 过期，这里会抛出异常
        // JJWT 0.12.3 新写法
        return Jwts.parser()
                .verifyWith(key) // 以前是 setSigningKey
                .build()
                .parseSignedClaims(token) // 以前是 parseClaimsJws
                .getPayload() // 以前是 getBody
                .getSubject();
    }

    /**
     * 【检验 Token - 步骤 B】
     * 验证 Token 是否有效 (签名是否正确 & 是否未过期)
     *
     * @param token 客户端传来的 JWT 字符串
     * @return true - 如果有效, false - 如果无效 (e.g., 签名错误, 已过期)
     */
    public boolean validateToken(String token) {
        try {
            // JJWT 0.12.3 新写法
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // 这里可以打印日志，比如 log.error("Token验证失败: {}", e.getMessage());
        }
        return false;
    }
}