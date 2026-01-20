package com.wwun.acme.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.wwun.acme")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wwun.acme.auth.feign")
public class AuthServiceApplication 
{
    public static void main( String[] args ){
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
