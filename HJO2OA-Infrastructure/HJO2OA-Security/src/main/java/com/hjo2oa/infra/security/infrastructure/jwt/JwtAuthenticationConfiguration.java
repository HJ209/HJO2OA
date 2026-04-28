package com.hjo2oa.infra.security.infrastructure.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

@Configuration
public class JwtAuthenticationConfiguration {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${hjo2oa.security.jwt.secret}") String secret,
            @Value("${hjo2oa.security.jwt.expiration}") long expiration,
            ObjectMapper objectMapper
    ) {
        return new JwtTokenProvider(secret, expiration, objectMapper);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
