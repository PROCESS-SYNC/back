package com.springboot.framework.common.config;

import java.io.IOException;
import java.util.List;

import org.springframework.web.filter.OncePerRequestFilter;

import com.springboot.framework.common.util.JwtProvider;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
  
  private static final String AUTHORIZATION_HEADER  = "Authorization";
  private static final String BEARER_PREFIX         = "Bearer ";

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request, 
    HttpServletResponse response, 
    FilterChain filterChain) throws ServletException, IOException 
  {
    String token = resolveToken(request);

    if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
      String userId = jwtProvider.getUserId(token);
      String role   = jwtProvider.getRole(token);

      UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                      userId,
                      null,
                      List.of(new SimpleGrantedAuthority("ROLE_" + role))
              );

      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.debug("[JWT] 인증 성공 - userId={}, role={}", userId, role);
    }

    filterChain.doFilter(request, response);
  }

  // Authorizatin 헤더에서 Bearer 토큰 추출
  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }

    return null;
  }
}
