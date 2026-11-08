package com.nody.nodyspring.config;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    //password hash
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 보안 필터 체인 설정s
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors() // ✅ 이 줄을 추가해야 WebMvcConfigurer의 CORS 설정이 활성화됩니다
                .and()
                .csrf().disable()  // CSRF 토큰 비활성화 (Postman 에서 테스트할 때 필요)
                .authorizeHttpRequests()
                .requestMatchers(HttpMethod.OPTIONS, "/post/img/**").permitAll()
                .requestMatchers("/chat/**").permitAll()
                .requestMatchers("/user/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/reports/**").permitAll()
                .requestMatchers("/bot/**").permitAll()
                .requestMatchers("/channel/**").permitAll()
                .requestMatchers("/post/**").permitAll()
                .requestMatchers("/post/img/**").permitAll()
                .requestMatchers("/ws-chat/**").permitAll()
                .requestMatchers("/sms/**").permitAll()
                .requestMatchers("/friend/**").permitAll()
                .anyRequest().authenticated();                // 그 외는 인증 필요
        return http.build();
    }
}