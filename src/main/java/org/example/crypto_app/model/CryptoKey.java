package org.example.crypto_app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.crypto_app.model.enums.EncryptionType;

import javax.crypto.SecretKey;

@Entity @Getter @Setter
@NoArgsConstructor
public class CryptoKey {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private SecretKey secretKey;

    private byte[] iv;

    @Enumerated(EnumType.STRING)
    private EncryptionType type;

    public CryptoKey(String name, SecretKey secretKey, EncryptionType type) {
        this.name = name;
        this.secretKey = secretKey;
        this.type = type;
    }

}
