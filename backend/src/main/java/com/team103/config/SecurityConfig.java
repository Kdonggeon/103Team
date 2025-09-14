package com.team103.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                // ✅ Actuator 헬스체크/정보는 누구나 접근 가능 (K8s/ALB/모니터링)
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // (선택) 초경량 핑을 추가했다면 이것도 공개
                .requestMatchers("/ping").permitAll()

                // ✅ 공개 API (로그인/회원가입/비번재설정 등)
                .requestMatchers("/api/login").permitAll()
                .requestMatchers("/api/signup/**").permitAll()
                .requestMatchers("/api/reset-password").permitAll()

                // ❗ 그 외는 인증 필요
                .anyRequest().authenticated()
            );

        // (JWT 사용 시 여기에 세션 전략/필터 추가)
        // http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🌐 CORS (브라우저 클라이언트가 있을 때만 의미 있음; 안드로이드 네이티브 Retrofit에는 보통 불필요)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 정확한 Origin(스킴+호스트+포트) 필요. 예: 로컬 프론트엔드가 있다면 추가
        // config.setAllowedOrigins(List.of("http://localhost:3000", "http://10.0.2.2:3000"));
        // 다양한 포트를 허용하려면 패턴 사용:
        config.setAllowedOriginPatterns(List.of("*")); // 개발용(운영에선 좁혀주세요)

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
