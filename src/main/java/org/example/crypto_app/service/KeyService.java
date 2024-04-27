package org.example.crypto_app.service;

import jakarta.transaction.Transactional;
import org.example.crypto_app.model.BaseUser;
import org.example.crypto_app.model.CryptoKey;
import org.example.crypto_app.model.enums.EncryptionType;
import org.example.crypto_app.repository.BaseUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class KeyService {

    private static final int MAX_KEYS = 20;

    private final BaseUserRepository baseUserRepository;

    public KeyService(BaseUserRepository baseUserRepository) {
        this.baseUserRepository = baseUserRepository;
    }

    private BaseUser getUser() {
        BaseUser user = (BaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return baseUserRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public List<CryptoKey> getUserKeys() {
        BaseUser user = getUser();
        return user.getKeys();
    }

    @Transactional
    public void createKey(String keyName, String keyType) {
        BaseUser user = getUser();
        if(user.getKeys().size() < MAX_KEYS) {
            try {
                EncryptionType type = EncryptionType.valueOf(keyType);
                SecretKey key = generateKeyByType(type);
                user.getKeys().add(new CryptoKey(keyName, key, type));
                baseUserRepository.save(user);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid Key Type");
            }
        } else throw new RuntimeException("Key limit Reached");
    }

    private SecretKey generateKeyByType(EncryptionType type) {
        KeyGenerator keyGenerator;
        try {
            switch (type) {
                case AES192:
                    keyGenerator = KeyGenerator.getInstance("AES");
                    keyGenerator.init(192);
                    break;
                case DES:
                    keyGenerator = KeyGenerator.getInstance("TripleDES");
                    keyGenerator.init(112);
                    break;
                case BLOWFISH:
                    keyGenerator = KeyGenerator.getInstance("Blowfish");
                    keyGenerator.init(128);
                    break;
                default:
                    throw new RuntimeException("Invalid Key Type:" + type);
            }
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating key of type " + type + ": " + e.getMessage());
        }
    }

    @Transactional
    public void deleteKey(Long keyId) {
        BaseUser user = getUser();
        CryptoKey key = user.getKeys().stream().filter(k -> k.getId().equals(keyId)).findFirst().orElseThrow(() -> new RuntimeException("Key not found"));
        user.getKeys().remove(key);
        baseUserRepository.save(user);
    }

    @Transactional
    public byte[] downloadKey(Long keyId) {
        BaseUser user = getUser();
        CryptoKey key = user.getKeys().stream().filter(k -> k.getId().equals(keyId)).findFirst().orElse(null);
        assert key != null : "Key not found";
        return key.getSecretKey().getEncoded();
    }

    //Bad approach. Should get key through keys of user to implicitly require key id belongs to user

//    public byte[] downloadKey(Long keyId) {
//        System.out.println("In downloadKey service");
//        CryptoKey key = keyRepository.findById(keyId).orElseThrow(() -> new RuntimeException("Key not found"));
//        return key.getSecretKey().getEncoded();
//    }





}
