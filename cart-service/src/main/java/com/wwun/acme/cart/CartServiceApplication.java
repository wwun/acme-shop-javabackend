package com.wwun.acme.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = {"com.wwun.acme.cart", "com.wwun.acme.security"})
@EnableFeignClients
@EnableMethodSecurity(prePostEnabled = true)
public class CartServiceApplication {

    public static void main(String[] args){
        SpringApplication.run(CartServiceApplication.class, args);
    }

}