package org.example.crypto_app.model.enums;

import java.util.Arrays;
import java.util.List;

public enum EncryptionType {
    AES192,
    DES,
    BLOWFISH;

    public static List<EncryptionType> getOptions() {
        return Arrays.asList(values());
    }

}
