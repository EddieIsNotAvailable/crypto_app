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
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidParameterSpecException;
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

//    @Transactional
//    public List<UserFile> getUserFiles() {
//        BaseUser user = getUser();
//        return user.getFiles();
//    }

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

        CryptoKey userKey = user.getKeys().stream()
                .filter(k -> k.getId().equals(keyId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Key not found"));

        try {
            file.setFileContent(encryptFileContent(file.getFileContent(), userKey));
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting file: " + e.getMessage());
        }
        file.setKey(userKey);
        file.generateHashes();
        user.getFiles().add(file);
        baseUserRepository.save(user);
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
    public byte[] getFileContent(Long fileId) {
        return getFile(fileId).getFileContent();
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
        user.getFiles().removeIf(file -> file.getId().equals(fileId));
        baseUserRepository.save(user);
    }

    public byte[] transformFileContent(byte[] fileContent, CryptoKey key, int mode) throws IOException {
        if(mode != Cipher.ENCRYPT_MODE && mode != Cipher.DECRYPT_MODE) {
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
//                    DESKeySpec desKeySpec = new DESKeySpec(key.getSecretKey().getEncoded());
//                    SecretKey desKey = SecretKeyFactory.getInstance("DES").generateSecret(desKeySpec);
//                    key.setSecretKey(desKey);
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

        if(mode == Cipher.ENCRYPT_MODE) {
            try {
                cipher.init(mode, key.getSecretKey());
                AlgorithmParameters params = cipher.getParameters();
                key.setIv(params.getParameterSpec(IvParameterSpec.class).getIV());

            } catch (InvalidKeyException | InvalidParameterSpecException e) {
                throw new RuntimeException("Error initializing cipher for encryption: " + e.getMessage());
            }
        } else { //mode == Cipher.DECRYPT_MODE
            try {
                cipher.init(Cipher.DECRYPT_MODE, key.getSecretKey(), new IvParameterSpec(key.getIv()));

            } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
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


//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
//        try {
//            cipherOutputStream.write(fileContent);
//            cipherOutputStream.close();
//            return outputStream.toByteArray();
//        } catch (Exception e) {
//            throw new RuntimeException("Error encrypting file: " + e.getMessage());
//        }
    }

//    @Transactional
//    public void encryptFile(Long fileId, Long keyId) {
//        BaseUser user = getUser();
//        UserFile file = getFile(fileId);
//        CryptoKey key = user.getKeys().stream()
//                .filter(k -> k.getId().equals(keyId))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Key not found"));
//
//        byte[] encrytedContent = encryptFileContent(file.getFileContent(), key, Cipher.ENCRYPT_MODE);
//        UserFile newFile = new UserFile(file.getFileName(), file.getFileType(), encrytedContent);
//        newFile.setKey(key);
//        newFile.generateHashes();
//
//        user.getFiles().add(newFile);
//        baseUserRepository.save(user);
//    }

    @Transactional
    public byte[] decryptAndReturnFile(Long fileId) {
        BaseUser user = getUser();
        UserFile file = getFile(fileId);
        CryptoKey key = file.getKey();
        assert key != null : "File not encrypted";

        byte[] out;
        try {
            out = decryptFileContent(file.getFileContent(), key);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting file: " + e.getMessage());
        }
        return out;
    }


//    @Transactional
//    public void updateFile(Long fileId, UserFile file) {
//        BaseUser user = getUser();
//        UserFile oldFile = getFile(fileId);
//        oldFile.setFileName(file.getFileName());
//        oldFile.setFileContent(file.getFileContent());
//        baseUserRepository.save(user);
//    }

    @Transactional
    public byte[] encryptFileContent(byte[] fileContent, CryptoKey key) throws Exception {
        byte[] encryptedContent = transformFileContent(fileContent, key, Cipher.ENCRYPT_MODE);
        byte[] iv = key.getIv();
        byte[] result = new byte[iv.length + encryptedContent.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedContent, 0, result, iv.length, encryptedContent.length);
        return result;
    }

    @Transactional
    public byte[] decryptFileContent(byte[] fileContent, CryptoKey key) throws Exception {
//        System.out.println("In decryption");
//        System.out.println("Saved IV: " + new String(key.getIv()));
        int ivSize = key.getIv().length;
        byte[] iv = new byte[ivSize];
        System.arraycopy(fileContent, 0, iv, 0, ivSize);
//        key.setIv(iv);
//        System.out.println("IV from file: " + new String(iv));

        byte[] encryptedContent = new byte[fileContent.length - ivSize];
        System.arraycopy(fileContent, ivSize, encryptedContent, 0, encryptedContent.length);
        return transformFileContent(encryptedContent, key, Cipher.DECRYPT_MODE);
    }



}
