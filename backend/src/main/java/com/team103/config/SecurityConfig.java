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

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil) { return new JwtAuthFilter(jwtUtil); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                /* 항상 허용 */
                .requestMatchers("/error", "/error/**").permitAll()
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()

                /* 공개 엔드포인트 */
                .requestMatchers("/actuator/health/**", "/actuator/info", "/ping").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                .requestMatchers("/api/signup/**", "/api/login/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/*/find_id").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/signup/director").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/students").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teachers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/parents").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teacher").permitAll()

                /* 교사/원장 공통 */
                .requestMatchers("/api/teachers/**").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers("/api/calendar/**").hasAnyRole("TEACHER","DIRECTOR")

                /* 강의실 조회(교사/원장 허용) */
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms/*/vector-layout").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms.vector-lite").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms/vector-lite").hasAnyRole("TEACHER","DIRECTOR")

                /* 좌석 벡터 저장/수정(교사/원장 허용) */
                .requestMatchers(HttpMethod.PUT,   "/api/admin/rooms/*/vector-layout").hasAnyRole("TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/rooms/*/vector-layout").hasAnyRole("TEACHER","DIRECTOR")

                /* 그 외 /api/admin/**는 원장 전용 */
                .requestMatchers(HttpMethod.PUT,   "/api/admin/rooms/**").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/rooms/**").hasRole("DIRECTOR")
                .requestMatchers(HttpMethod.DELETE,"/api/admin/rooms/**").hasRole("DIRECTOR")
                .requestMatchers("/api/admin/**").hasRole("DIRECTOR")

                /* 나머지 인증 필요 */
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
            "http://localhost:3000", "http://127.0.0.1:3000",
            "http://192.168.*:*", "https://your-web-domain.com"
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
