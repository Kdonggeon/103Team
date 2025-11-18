// src/main/java/com/team103/config/SecurityConfig.java
package com.team103.config;

import com.team103.security.JwtAuthFilter;
import com.team103.security.JwtUtil;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {

    /* ====== 비밀번호 인코더 ====== */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* ====== JWT 필터 Bean ====== */
    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil) {
        return new JwtAuthFilter(jwtUtil);
    }

    /* ====== 메인 Security FilterChain ====== */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {

        http
            // CORS + CSRF + 세션
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 인가 규칙
            .authorizeHttpRequests(auth -> auth
                /* ----- 항상 허용 ----- */
                .requestMatchers("/error", "/error/**").permitAll()
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()

                // 헬스체크, ping
                .requestMatchers("/actuator/health/**", "/actuator/info", "/ping").permitAll()

                // CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                /* ----- 로그인/아이디 찾기/비밀번호 재설정/회원가입 (공개) ----- */
                .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                .requestMatchers("/api/login/**").permitAll()
                .requestMatchers("/api/signup/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/find-id").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reset-password").permitAll()

                // 회원가입 (학생/학부모/교사/원장)
                .requestMatchers(HttpMethod.POST, "/api/directors/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/signup/director").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/students").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teachers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/parents").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teacher").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/account/delete").authenticated()

                // 업로드된 정적 파일
                .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                /* ----- 교사용 메인 패널 ----- */
                .requestMatchers("/api/teachermain/**").hasAnyRole("TEACHER", "DIRECTOR")

                /* ====== 공지: 읽기(로그인), 쓰기/수정/삭제(교사·원장) ====== */
                .requestMatchers(HttpMethod.GET,
                    "/api/notices", "/api/notices/**"
                ).authenticated()
                .requestMatchers(HttpMethod.POST,
                    "/api/notices", "/api/notices/**"
                ).hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers(HttpMethod.PUT,
                    "/api/notices/**"
                ).hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers(HttpMethod.PATCH,
                    "/api/notices/**"
                ).hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers(HttpMethod.DELETE,
                    "/api/notices/**"
                ).hasAnyRole("TEACHER", "DIRECTOR")

                /* ----- 수업 조회 ----- */
                .requestMatchers(HttpMethod.GET, "/api/lookup/classes/**").authenticated()

                /* ====== 교사/원장 공통 보호 엔드포인트 ====== */
                // 🔹 교사 소속 해제: 선생/원장 둘 다 허용
                .requestMatchers(HttpMethod.PATCH, "/api/teachers/*/academies/detach")
                    .hasAnyRole("TEACHER", "DIRECTOR")

                // 나머지 /api/teachers/** 도 선생/원장만 접근
                .requestMatchers("/api/teachers/**").hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers("/api/calendar/**").hasAnyRole("TEACHER", "DIRECTOR")

                /* 강의실 조회(교사/원장 허용) */
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms").hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms/*/vector-layout").hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms.vector-lite").hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms/vector-lite").hasAnyRole("TEACHER", "DIRECTOR")

                /* 좌석 벡터 저장/수정(교사/원장 허용) */
                .requestMatchers(HttpMethod.PUT,   "/api/admin/rooms/*/vector-layout").hasAnyRole("TEACHER", "DIRECTOR")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/rooms/*/vector-layout").hasAnyRole("TEACHER", "DIRECTOR")

                /* 그 외 /api/admin/** 는 원장 전용 */
                .requestMatchers(HttpMethod.PUT,   "/api/admin/rooms/**").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/rooms/**").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.DELETE,"/api/admin/rooms/**").hasRole("DIRECTOR")

                /* ====== 원장 전용 관리 패널(API 분리: /api/manage/**) ====== */
                .requestMatchers(HttpMethod.GET,    "/api/manage/students").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/manage/students/*").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.GET,    "/api/manage/teachers").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/manage/teachers/*").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.GET,    "/api/manage/students/*/classes").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.GET,    "/api/manage/students/*/attendance").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.GET,    "/api/manage/teachers/*/classes").hasAnyRole("DIRECTOR", "TEACHER")
                .requestMatchers(HttpMethod.GET,    "/api/manage/teachers/classes/*/attendance").hasAnyRole("DIRECTOR", "TEACHER")

                /* ====== 그 외 admin/overview ====== */
                .requestMatchers("/api/admin/**").hasRole("DIRECTOR")
                .requestMatchers("/api/director/overview/**").hasAnyRole("DIRECTOR", "TEACHER")

                /* ----- 나머지는 토큰 필요 ----- */
                .anyRequest().authenticated()
            )

            // JWT 필터 추가
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /* ====== CORS 설정 ====== */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // 허용 도메인
        cfg.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://192.168.*:*",
            "https://103team-web.vercel.app",
            "https://greenacademy.vercel.app"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
