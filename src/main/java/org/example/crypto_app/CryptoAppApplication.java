package org.example.crypto_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication
public class CryptoAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoAppApplication.class, args);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
}
