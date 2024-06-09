package org.example.crypto_app.service;

import jakarta.transaction.Transactional;
import org.example.crypto_app.model.BaseUser;
import org.example.crypto_app.model.CryptoKey;
import org.example.crypto_app.model.FileInfoDTO;
import org.example.crypto_app.model.UserFile;
import org.example.crypto_app.repository.BaseUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.util.List;

@Service
public class FileService {

    //Max file size of 1mb
    private static final int MAX_FILE_SIZE = 1024 * 1024;

    //Max number of files per user
    private static final int MAX_FILES = 8;

    private final BaseUserRepository baseUserRepository;

    public FileService(BaseUserRepository baseUserRepository) {
        this.baseUserRepository = baseUserRepository;
    }

    private BaseUser getUser() {
        BaseUser user = (BaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return baseUserRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void saveFile(UserFile file) {
        BaseUser user = getUser();
        if (user.getFiles().size() >= MAX_FILES) {
            throw new RuntimeException("File limit (" + MAX_FILES + ") reached");
        }
        file.generateHashes();
        user.getFiles().add(file);
        baseUserRepository.save(user);
    }

    @Transactional
    public void saveFileEncrypted(UserFile file, Long keyId) {
        BaseUser user = getUser();
        if (user.getFiles().size() >= MAX_FILES) {
            throw new RuntimeException("File limit (" + MAX_FILES + ") reached");
        }

        CryptoKey key = user.getKeys().stream()
                .filter(k -> k.getId().equals(keyId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Key not found"));

        try {
            byte[] iv = null;
            file.setFileContent(transformFileContent(file.getFileContent(), key, iv, Cipher.ENCRYPT_MODE));
            file.setIv(iv);
            file.setKey(key);
            file.generateHashes();
            user.getFiles().add(file);
            baseUserRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting file: " + e.getMessage());
        }
    }

    @Transactional
    public byte[] decryptAndReturnFile(Long fileId) {
        BaseUser user = getUser();

        UserFile file = baseUserRepository.getUserFile(user.getId(), fileId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found"));

        CryptoKey key = file.getKey();
        assert key != null : "File not encrypted";
        byte[] iv = file.getIv();
        assert iv != null : "Cannot decrypt unencrypted file";

        try {
            return transformFileContent(file.getFileContent(), key, iv, Cipher.DECRYPT_MODE);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting file: " + e.getMessage());
        }
    }

    @Transactional
    public UserFile getFile(Long fileId) {
        BaseUser user = getUser();
        return user.getFiles()
                .stream()
                .filter(file -> file.getId().equals(fileId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public List<FileInfoDTO> getUserFilesInfo() {
        BaseUser user = getUser();
        return user.getFiles().stream()
                .map(file -> new FileInfoDTO(file.getId(), file.getFileName(), file.getFileType(),file.getFileContent().length, file.getFileHashes(), file.getKey()))
                .toList();
    }

    @Transactional
    public void deleteFile(Long fileId) {
        BaseUser user = getUser();
        if(!baseUserRepository.deleteFileById(user.getId(), fileId)) {
            throw new RuntimeException("File not found");
        }
    }

    public byte[] transformFileContent(byte[] fileContent, CryptoKey key, byte[] iv, int mode) throws IOException {
        if (mode != Cipher.ENCRYPT_MODE && mode != Cipher.DECRYPT_MODE) {
            throw new RuntimeException("Invalid encryption mode");
        }
        Cipher cipher;
        try {
            switch (key.getType()) {
                case AES192:
                    cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    break;
                case DES:
                    cipher = Cipher.getInstance("TripleDES/CBC/PKCS5Padding");
                    break;
                case BLOWFISH:
                    cipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding");
                    break;
                default:
                    throw new RuntimeException("Invalid encryption type");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing cipher: " + e.getMessage());
        }

        if (mode == Cipher.ENCRYPT_MODE) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, key.getSecretKey());
                AlgorithmParameters params = cipher.getParameters();
                iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            } catch (Exception e) {
                throw new RuntimeException("Error initializing cipher for encryption: " + e.getMessage());
            }
        } else {
            try {
                cipher.init(Cipher.DECRYPT_MODE, key.getSecretKey(), new IvParameterSpec(iv));
            } catch (Exception e) {
                throw new RuntimeException("Error initializing cipher for decryption: " + e.getMessage());
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (mode == Cipher.ENCRYPT_MODE) {
            try {
                CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
                cipherOutputStream.write(fileContent);
                cipherOutputStream.close();
            } catch (Exception e) {
                throw new RuntimeException("Error encrypting file: " + e.getMessage());
            }
        } else {
            CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(fileContent), cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return outputStream.toByteArray();
    }
}
