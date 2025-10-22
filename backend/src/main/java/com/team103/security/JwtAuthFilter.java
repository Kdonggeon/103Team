package com.team103.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authorization: Bearer <JWT> 를 읽어 SecurityContext에 인증을 세팅.
 * role/roles/authorities/scope 어디서 와도 흡수하여 ROLE_* 권한으로 정규화.
 * 디버깅 로그 강화.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** 헤더가 비어있으면 쿠키에서 대체 토큰을 찾아주는 보조 함수(선택) */
    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return auth.substring(BEARER.length()).trim();
        }
        // (옵션) 쿠키에 "Authorization" 또는 "token" 으로 담아오는 경우 지원
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("Authorization".equalsIgnoreCase(c.getName()) || "token".equalsIgnoreCase(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) return jwtUtil.stripBearer(v);
                }
            }
        }
        return null;
    }

    /** 토큰 앞 몇 글자만 로깅 (전체 토큰 노출 금지) */
    private String maskToken(String token) {
        if (token == null) return "null";
        int keep = Math.min(12, token.length());
        return token.substring(0, keep) + "...(" + token.length() + ")";
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청 ID를 MDC에 넣어 로그 추적성 강화
        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);

        final String method = request.getMethod();
        final String uri = request.getRequestURI();

        try {
            // OPTIONS (CORS preflight)은 보통 인증 불필요 – 로깅만 하고 통과
            if ("OPTIONS".equalsIgnoreCase(method)) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] {} {} → preflight pass", rid, method, uri);
                }
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] {} {} → already authenticated: {}",
                            rid, method, uri,
                            SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                }
                filterChain.doFilter(request, response);
                return;
            }

            String token = resolveToken(request);
            if (log.isDebugEnabled()) {
                log.debug("[{}] {} {} → Authorization present? {}",
                        rid, method, uri, (token != null ? "YES (" + maskToken(token) + ")" : "NO"));
            }

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 유효성 검사
            boolean valid = false;
            try {
                valid = jwtUtil.validate(token);
            } catch (Exception ex) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] token validate threw: {}", rid, ex.toString());
                }
            }

            if (!valid) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] token invalid → continue without auth", rid);
                }
                filterChain.doFilter(request, response);
                return;
            }

            // 사용자/권한 추출
            String username = jwtUtil.getUsername(token);

            // 다양한 위치의 역할 수집
            Set<String> rawRoles = new LinkedHashSet<>();
            rawRoles.addAll(jwtUtil.getClaimAsStringList(token, "roles"));       // ["TEACHER","DIRECTOR"]
            String singleRole = jwtUtil.getClaimAsString(token, "role");         // "TEACHER"
            if (singleRole != null && !singleRole.isBlank()) rawRoles.add(singleRole);
            rawRoles.addAll(jwtUtil.getClaimAsStringList(token, "authorities")); // ["ROLE_TEACHER", ...]
            String scope = jwtUtil.getClaimAsString(token, "scope");             // "teacher director"
            if (scope != null) {
                Arrays.stream(scope.split("[,\\s]+"))
                        .filter(s -> !s.isBlank())
                        .forEach(rawRoles::add);
            }

            // ROLE_ 접두사 정규화
            List<String> normalized = rawRoles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .map(s -> s.startsWith("ROLE_") ? s : "ROLE_" + s)
                    .toList();

            List<GrantedAuthority> authorities = normalized.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("[{}] token OK user='{}' rawRoles={} normalizedAuthorities={}",
                        rid, username, rawRoles, normalized);
            }

            // 인증 세팅
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

            if (log.isDebugEnabled()) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                log.debug("[{}] after chain auth={}", rid, (auth != null ? auth.getAuthorities() : "null"));
            }

        } finally {
            MDC.remove("rid");
        }
    }
}
