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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** JwtAuthFilter 빈 등록 */
    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil) {
        return new JwtAuthFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                /* ====== 항상 허용 ====== */
                .requestMatchers("/error", "/error/**").permitAll()
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()

                /* ====== 공개 엔드포인트 ====== */
                .requestMatchers("/actuator/health/**", "/actuator/info", "/ping").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                .requestMatchers("/api/signup/**", "/api/login/**").permitAll()

                // (기존 공개 POST 유지 필요 시)
                .requestMatchers(HttpMethod.POST, "/api/*/find_id").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/signup/director").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/students").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teachers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/parents").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teacher").permitAll()

                /* ====== 교사/원장 공통 보호 엔드포인트 ====== */
                .requestMatchers("/api/teachers/**").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers("/api/calendar/**").hasAnyRole("TEACHER","DIRECTOR")

                /* ====== 강의실 조회: 교사/원장 허용 ====== */
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms/*/vector-layout").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms.vector-lite").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms/vector-lite").hasAnyRole("TEACHER","DIRECTOR")

                /* ====== 강의실 수정/삭제: 원장 전용 ====== */
                .requestMatchers(HttpMethod.PUT, "/api/admin/rooms/**").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/rooms/**").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/rooms/**").hasRole("DIRECTOR")

                /* ====== 원장 전용 관리 패널(API 분리: /api/manage/**) ====== */
                // 학생/교사 목록 & 삭제
                .requestMatchers(HttpMethod.GET,    "/api/manage/students").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/manage/students/*").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.GET,    "/api/manage/teachers").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/manage/teachers/*").hasRole("DIRECTOR")

                // 학생의 수업/출결 조회(원장 화면에서 다른 학생 조회)
                .requestMatchers(HttpMethod.GET, "/api/manage/students/*/classes").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/manage/students/*/attendance").hasRole("DIRECTOR")

                // 교사의 수업/출결(오늘자/특정일)
                .requestMatchers(HttpMethod.GET, "/api/manage/teachers/*/classes").hasAnyRole("DIRECTOR","TEACHER")
                .requestMatchers(HttpMethod.GET, "/api/manage/teachers/classes/*/attendance").hasAnyRole("DIRECTOR","TEACHER")

                /* ====== 그 외 admin은 기본적으로 원장 전용 ====== */
                .requestMatchers("/api/admin/**").hasRole("DIRECTOR")

                /* ====== 나머지는 인증 필요 ====== */
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** CORS */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://192.168.*:*",
            "https://your-web-domain.com"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PATCH","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}