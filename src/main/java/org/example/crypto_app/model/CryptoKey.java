package org.example.crypto_app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private SecretKey secretKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EncryptionType type;

    public CryptoKey(String name, SecretKey secretKey, EncryptionType type) {
        this.name = name;
        this.secretKey = secretKey;
        this.type = type;
    }

//    public String getSecretKey() {
//        Base64.Encoder encoder = Base64.getEncoder();
//        return encoder.encodeToString(secretKey.getEncoded());
//    }


}
