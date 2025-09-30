
package com.team103.config;

import com.team103.security.JwtAuthenticationFilter;
import com.team103.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception { // ✅ JwtUtil 주입
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info", "/ping").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/students").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/parents").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teachers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/directors/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/*/find_id").permitAll()

                // ✅ 부모 권한 허용
                .requestMatchers(HttpMethod.GET,
                        "/api/parents/*/children",
                        "/api/parents/*/students",
                        "/api/parents/*/children/names",
                        "/api/parents/*/attendance"
                ).hasAnyRole("PARENT","TEACHER","DIRECTOR")
                .requestMatchers(HttpMethod.PUT,
                        "/api/parents/*/fcm-token"
                ).hasAnyRole("PARENT","TEACHER","DIRECTOR")

                .anyRequest().authenticated()
            )
            // ✅ 주입받은 JwtUtil을 전달한 필터 인스턴스 등록
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("http://10.0.2.2:*","http://localhost:*","http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
