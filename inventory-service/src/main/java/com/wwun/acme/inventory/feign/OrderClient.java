package com.wwun.acme.inventory.feign;

import org.springframework.cloud.openfeign.FeignClient;

import com.wwun.acme.inventory.security.FeignSecurityConfig;

@FeignClient(name = "msvc-orders", configuration = FeignSecurityConfig.class)
public interface OrderClient {

    // not implemented yet

}
