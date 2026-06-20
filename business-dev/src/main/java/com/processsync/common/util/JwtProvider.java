package com.processsync.common.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProvider {
  
  private final SecretKey secretKey;
  private final long      accessTokenExpiration;
  private final long      refreshTokenExpiration;
  
  public JwtProvider(
    @Value("${jwt.secret}") String secretKeyString,
    @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
    @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
  ) {
    this.secretKey              = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyString));
    this.accessTokenExpiration  = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  // Access Token 생성
  public String generateAccessToken(String userId, String role) {
    return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .claim("type", "ACCESS")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(secretKey)
            .compact();
  }

  // Refresh Token 생성
  public String generateRefreshToken(String userId) {
    return Jwts.builder()
            .subject(userId)
            .claim("type", "REFRESH")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(secretKey)
            .compact();
  }

  // 토큰에서 userId 추출
  public String getUserId(String token) {
    return getClaims(token).getSubject();
  }

  // 토큰에서 role 추출
  public String getRole(String token) {
    return getClaims(token).get("role", String.class);
  }

  // Refresh Token 만료일시 추출
  public Date getExpiration(String token) {
    return getClaims(token).getExpiration();
  }

  // 토큰 유효성 검증
  public boolean validateToken(String token) {
    try {
      getClaims(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.warn("[JWT] 만료된 토큰: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.warn("[JWT] 지원하지 않는 토큰: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.warn("[JWT] 잘못된 형식의 토큰: {}", e.getMessage());
    } catch (SecurityException e) {
      log.warn("[JWT] 서명 검증 실패: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.warn("[JWT] 빈 토큰: {}", e.getMessage());
    }
    
    return false;
  }

  // Claims 추출
  private Claims getClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }
}
