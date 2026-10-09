package com.hostel.management;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HostelManagementApplication {

    private static final Logger log = LoggerFactory.getLogger(HostelManagementApplication.class);

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    public static void main(String[] args) {
        SpringApplication.run(HostelManagementApplication.class, args);
    }

    @PostConstruct
    public void validateSecurityConfig() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT Secret must be configured via 'app.jwt.secret' or 'JWT_SECRET' environment variable.");
        }
        if (jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT Secret must be at least 32 characters (256 bits) long to meet HS256 security standards.");
        }
        if (jwtSecret.contains("LocalDevSecretKeyMustBeOverriddenInProduction")) {
            log.warn("SECURITY NOTICE: A development JWT secret key is currently in use. Ensure JWT_SECRET environment variable is set for production deployments.");
        }
    }
}
