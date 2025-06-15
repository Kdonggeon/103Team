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

    // 🔐 비밀번호 암호화용 PasswordEncoder 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 🔐 Security filter chain 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .csrf().disable() // ✅ CSRF 보호 비활성화 (Postman 테스트용)
        .authorizeHttpRequests()
        .requestMatchers("/**").permitAll(); // 모든 요청 허용
//            .cors(Customizer.withDefaults())
//            .csrf(csrf -> csrf.disable())
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/api/login").permitAll()
//                .requestMatchers("/api/signup/**").permitAll()
//                .requestMatchers("/api/students").permitAll()
//                .requestMatchers("/api/students/**").permitAll()
//                .requestMatchers("/api/reset-password").permitAll() // ✅ 이 줄 추가!
//                .anyRequest().authenticated()
//            );
        return http.build();
    }


    // 🌐 CORS 설정 - Android 에뮬레이터 접근 허용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://10.0.2.2"));  // Android 에뮬레이터 접근 허용
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // 인증 헤더 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // 모든 경로에 대해 CORS 설정 적용
        return source;
    }
    
    
}
