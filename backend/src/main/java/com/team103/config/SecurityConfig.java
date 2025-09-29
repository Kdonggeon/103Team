// src/main/java/com/team103/config/SecurityConfig.java
package com.team103.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers("/actuator/health/**", "/actuator/info", "/ping").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()       // CORS preflight

                // 로그인
                .requestMatchers(HttpMethod.POST, "/api/login").permitAll()

                // 회원가입 (현재 컨트롤러 형태에 맞춤)
                .requestMatchers(HttpMethod.POST, "/api/students").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/parents").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teachers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors/signup").permitAll()

                // 아이디 찾기 (역할별 분리, 언더스코어)
                .requestMatchers(HttpMethod.POST, "/api/*/find_id").permitAll()

                // 비밀번호 재설정(쓰는 경우)
                .requestMatchers(HttpMethod.POST, "/api/reset-password").permitAll()

                // 그 외는 인증 필요
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // 개발용: 프론트(Next.js) 오리진 지정
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        ));

        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // 필요시 노출 헤더 추가
        // config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
