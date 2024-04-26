package org.example.crypto_app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;
import org.example.crypto_app.model.enums.HashType;

import java.security.MessageDigest;

@Entity @Getter @Setter
@NoArgsConstructor
public class FileHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private HashType hashType;

    @NotNull
    private String hash;

    public FileHash(byte[] fileData, HashType hashType) {

        Digest digest = switch (hashType) {
            case MD5 -> new MD5Digest();
            case SHA256 -> new SHA256Digest();
            default -> throw new RuntimeException("Invalid Hash Type");
        };

        byte[] hashBytes = new byte[digest.getDigestSize()];
        digest.update(fileData, 0, fileData.length); //Hash entire file data, starting from beginning
        digest.doFinal(hashBytes, 0); //Generate final hash

        String hash = Hex.toHexString(hashBytes);

        this.hashType = hashType;
        this.hash = hash;
    }

}
