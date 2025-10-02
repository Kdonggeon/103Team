package com.team103.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long expMs;
    private final long clockSkewSec;

    public JwtUtil(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.exp-ms:10800000}") long expMs,              // 3h default
            @Value("${jwt.clock-skew-sec:60}") long clockSkewSec      // 60s default
    ) {
        // base64 문자열을 키로
        byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expMs = expMs;
        this.clockSkewSec = clockSkewSec;
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)              // e.g. parent/teacher/...
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 유효하면 Claims 반환, 아니면 예외 */
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(clockSkewSec)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
