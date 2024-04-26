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
import java.io.ByteArrayOutputStream;
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
        if (user.getFiles().size() > MAX_FILES) {
            throw new RuntimeException("File limit (" + MAX_FILES + ") reached");
        }
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
                .map(file -> new FileInfoDTO(file.getId(), file.getFileName(), file.getFileType(),file.getFileSize(), file.getFileHashes()))
                .toList();
    }

    @Transactional
    public void deleteFile(Long fileId) {
        BaseUser user = getUser();
        user.getFiles().removeIf(file -> file.getId().equals(fileId));
        baseUserRepository.save(user);
    }

    private byte[] encryptFileContent(byte[] fileContent, CryptoKey key) {

        Cipher cipher;
        try {
            switch (key.getType()) {
                case AES192:
                    cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, key.getSecretKey());
                    break;
                case DES:
                    cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                    DESKeySpec desKeySpec = new DESKeySpec(key.getSecretKey().getEncoded());
                    SecretKey desKey = SecretKeyFactory.getInstance("DES").generateSecret(desKeySpec);
                    cipher.init(Cipher.ENCRYPT_MODE, desKey);
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

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
        try {
            cipherOutputStream.write(fileContent);
            cipherOutputStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting file: " + e.getMessage());
        }
    }

    @Transactional
    public void encryptFile(Long fileId, Long keyId) {
        BaseUser user = getUser();
        UserFile file = getFile(fileId);
        CryptoKey key = user.getKeys().stream()
                .filter(k -> k.getId().equals(keyId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Key not found"));

        byte[] encrytedContent = encryptFileContent(file.getFileContent(), key);
        UserFile newFile = new UserFile(file.getFileName(), file.getFileType(), encrytedContent);
        newFile.setKey(key);
        newFile.generateHashes();

        user.getFiles().add(newFile);
        baseUserRepository.save(user);
    }



//    @Transactional
//    public void updateFile(Long fileId, UserFile file) {
//        BaseUser user = getUser();
//        UserFile oldFile = getFile(fileId);
//        oldFile.setFileName(file.getFileName());
//        oldFile.setFileContent(file.getFileContent());
//        baseUserRepository.save(user);
//    }




}
