package com.springboot.framework.common.util;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeyGenerator {
  
  private final KeyGeneratorMapper keyGeneratorMapper;

  public String generateKey(String jobType) {
    return keyGeneratorMapper.generateKey(jobType);
  }
}
