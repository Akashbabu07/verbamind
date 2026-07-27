package com.verbamind.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "verbamind.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationMs;
    private long refreshTokenExpirationMs;


}