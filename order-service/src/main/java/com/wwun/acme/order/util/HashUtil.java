package com.wwun.acme.order.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HashUtil {

    private final ObjectMapper MAPPER = new ObjectMapper();

    public String sha256(Object input){
        try{
            String inputString = MAPPER.writeValueAsString(input);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(inputString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }catch(Exception ex){
            throw new RuntimeException("Error generating hash ",ex);
        }
    }

}
