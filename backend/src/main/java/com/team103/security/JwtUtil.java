package com.team103.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.Base64;

@Component
public class JwtUtil {

    private final Key key;
    private final long expMs;          // 만료(ms)
    private final long clockSkewSec;   // 허용 시계 오차(sec)

    public JwtUtil(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.exp-ms:10800000}") long expMs,                 // 기본 3h
            @Value("${jwt.clock-skew-sec:60}") long clockSkewSec         // 기본 60s
    ) {
        byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expMs = expMs;
        this.clockSkewSec = clockSkewSec;
    }

    /* ================== 토큰 생성 ================== */

    /** 단일 역할로 생성 (예: "TEACHER") */
    public String generateToken(String username, String role) {
        return generateToken(username, role != null ? List.of(role) : List.of());
    }

    /** 복수 역할로 생성 (예: ["TEACHER","DIRECTOR"]) */
    public String generateToken(String username, Collection<String> roles) {
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>();
        if (roles != null) {
            // 호환성: role / roles 둘 다 넣어줌
            String first = roles.stream().findFirst().orElse(null);
            claims.put("role", first);
            claims.put("roles", new ArrayList<>(roles));
        }
        return Jwts.builder()
                .setSubject(username)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ================== 파싱/검증 ================== */

    /** 접두사 "Bearer " 있으면 제거 */
    public String stripBearer(String token) {
        if (token == null) return null;
        token = token.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.substring(7).trim();
        }
        return token;
    }

    /** 유효성 검사 (정상/만료/서명오류 등 false 반환) */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 유효하면 Claims 반환(예외 throw) */
    public Claims parseClaims(String token) throws JwtException {
        String t = stripBearer(token);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(clockSkewSec)
                .build()
                .parseClaimsJws(t)
                .getBody();
    }

    /* ================== 클레임 접근 ================== */

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * roles 클레임(배열) 또는 role 클레임(문자열)을 읽어서 리스트로 반환.
     * ROLE_ 접두사는 붙이지 않고 대문자로 정규화.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims c = parseClaims(token);

        // 1) roles: ["TEACHER","DIRECTOR"]
        Object rolesObj = c.get("roles");
        List<String> out = new ArrayList<>();
        if (rolesObj instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null) out.add(norm(String.valueOf(o)));
            }
        }

        // 2) role: "TEACHER" (단일) – 중복되지 않게 추가
        Object roleObj = c.get("role");
        if (roleObj != null) {
            String r = norm(String.valueOf(roleObj));
            if (!out.contains(r)) out.add(r);
        }

        return out;
    }

    private String norm(String r) {
        if (r == null) return null;
        r = r.trim();
        // "ROLE_TEACHER" → "TEACHER"
        if (r.regionMatches(true, 0, "ROLE_", 0, 5)) r = r.substring(5);
        return r.toUpperCase(Locale.ROOT);
    }
    

    /** 기존 컨트롤러 호환용: 토큰을 검증하고 Claims를 반환 */
    public Claims validateToken(String token) throws JwtException {
        // Bearer 접두사 처리 포함
        return parseClaims(token);
    }

    /** 토큰에서 사용자명(subject)와 권한을 꺼내 Security/세션에서 쓰려면 그대로 이용 */
    // 이미 getUsername/getRoles가 있으니 필요시 사용
    
    /* ================== Generic Claim getters ================== */

    /** 임의 클레임을 타입-세이프하게 꺼내기 (없으면 null) */
    public <T> T getClaim(String token, String name, Class<T> type) {
        Object v = parseClaims(token).get(name);
        if (v == null) return null;

        // 타입이 이미 맞으면 그대로
        if (type.isInstance(v)) return type.cast(v);

        // 문자열로 강제 변환을 허용
        if (type == String.class) return type.cast(String.valueOf(v));

        // 숫자류 간단 변환
        if (Number.class.isAssignableFrom(type)) {
            try {
                String s = String.valueOf(v);
                if (type == Integer.class) return type.cast(Integer.valueOf(s));
                if (type == Long.class)    return type.cast(Long.valueOf(s));
                if (type == Double.class)  return type.cast(Double.valueOf(s));
            } catch (NumberFormatException ignore) { /* fallthrough */ }
        }

        // 기대 타입이랑 다르면 null
        return null;
    }

    /** 문자열 클레임 sugar */
    public String getClaimAsString(String token, String name) {
        return getClaim(token, name, String.class);
    }

    /** 문자열 리스트 클레임(예: roles)이면 대문자 정규화해서 반환 */
    @SuppressWarnings("unchecked")
    public List<String> getClaimAsStringList(String token, String name) {
        Object v = parseClaims(token).get(name);
        if (v instanceof Collection<?> col) {
            List<String> out = new ArrayList<>(col.size());
            for (Object o : col) if (o != null) out.add(String.valueOf(o).trim());
            return out;
        }
        if (v != null) return List.of(String.valueOf(v).trim());
        return List.of();
    }


}
