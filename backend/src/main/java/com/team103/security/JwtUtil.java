// src/main/java/com/team103/security/JwtUtil.java
package com.team103.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;
    private final long clockSkewSec;

    public JwtUtil(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.exp-ms:10800000}") long expirationMs,
            @Value("${jwt.clock-skew-sec:60}") long clockSkewSec
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
        this.clockSkewSec = clockSkewSec;
    }

    public String generateToken(String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of("role", role))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(clockSkewSec)
                .build()
                .parseClaimsJws(token);
    }

    public Claims validateAndGetClaims(String token) {
        return parseToken(token).getBody();
    }

    public static String resolve(String header) {
        if (header == null) return null;
        return header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}
