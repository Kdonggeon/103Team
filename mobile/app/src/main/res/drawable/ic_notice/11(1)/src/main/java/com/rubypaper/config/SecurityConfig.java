package com.rubypaper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF 보호 기능 비활성화
        http.csrf().disable()
            // 회원가입 경로 모두 허용
            .authorizeHttpRequests()
                .requestMatchers("/signup").permitAll()
                // 기타 모든 요청 허용
                .anyRequest().permitAll();
        return http.build();
    }
}