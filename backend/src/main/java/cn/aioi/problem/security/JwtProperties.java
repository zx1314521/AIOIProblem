package cn.aioi.problem.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aioi.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}

