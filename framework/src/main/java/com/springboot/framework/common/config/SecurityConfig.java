package com.springboot.framework.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.springboot.framework.common.util.JwtProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  
  private final JwtProvider jwtProvider;

  // 인증 없이 접근 가능한 URL
  private static final String[] PUBLIC_URLS = {
    "/api/auth/**",     // 로그인, 회원가입, 토큰 재발급
    "/actuator/health",  //헬스체크
    "/actuator/info",
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      // CSRF 보호 비활성화 (JWT 사용시 불필요)
      .csrf(AbstractHttpConfigurer::disable)
      // 폼 로그인 비활성화
      .formLogin(AbstractHttpConfigurer::disable)
      // HTTP Basic 인증 비활성화
      .httpBasic(AbstractHttpConfigurer::disable)
      // 세션 미사용 (JWT Stateless)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      // URL 권한 설정
      .authorizeHttpRequests(auth -> auth
                              .requestMatchers(PUBLIC_URLS).permitAll()
                              .requestMatchers("/api/admin/**").hasRole("ADMIN")
                              .anyRequest().authenticated()
      )
      // JWT 필터 등록
      .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // 비밀번호 암호화 (BCrypt)
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
