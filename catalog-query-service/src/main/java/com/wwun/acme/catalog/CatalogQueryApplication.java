package com.wwun.acme.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"com.wwun.acme.catalog", "com.wwun.acme.security"})
@EnableMethodSecurity(prePostEnabled = true)
public class CatalogQueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogQueryApplication.class, args);
    }
}
