package com.processsync.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  
  @Value("${agent.base-url:http//localhost:8000}")
  private String agentBaseUrl;

  // Python FastAPI 에이전트 호출용
  @Bean(name = "agentWebClient")
  public WebClient agentWebClient() {
    return WebClient.builder()
            .baseUrl(agentBaseUrl)
            .defaultHeader("Content-Type", "application/json")
            .codecs(config -> config
                    .defaultCodecs()
                    .maxInMemorySize(10 * 1024 * 1024) // 10MB
            )
            .build(); 
  }
}
