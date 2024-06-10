package org.example.crypto_app.controller;

import jakarta.validation.constraints.NotNull;
import org.example.crypto_app.model.CryptoKey;
import org.example.crypto_app.model.FileInfoDTO;
import org.example.crypto_app.model.UserFile;
import org.example.crypto_app.service.FileService;
import org.example.crypto_app.service.KeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;


@Controller
public class FileController {

    private final FileService fileService;
    private final KeyService keyService;

    public FileController(FileService fileService, KeyService keyService) {
        this.fileService = fileService;
        this.keyService = keyService;
    }

    @GetMapping("/files") // Load page with user files info
    public String files(Model model) {
        try {
            List<FileInfoDTO> filesInfo = fileService.getUserFilesInfo();
            if(!filesInfo.isEmpty()) model.addAttribute("files", filesInfo);

            List<CryptoKey> keys = keyService.getUserKeys();
            if(!keys.isEmpty()) model.addAttribute("keys", keys);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            System.out.println("Error loading files page: " + e.getMessage());
        }
        return "files";
    }

    //For uploading files, with support for encrypting it
    @PostMapping("/uploadFile")
    public RedirectView upload(@NotNull MultipartFile file, Long keyId, RedirectAttributes re) {
        long startTime = System.currentTimeMillis();
        try {
            UserFile newFile = new UserFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
            if(keyId == null) fileService.saveFile(newFile);
            else fileService.saveFileEncrypted(newFile, keyId);

            long endTime = System.currentTimeMillis();
            double responseTime = endTime - startTime;
            re.addFlashAttribute("responseTime", "Took " + String.format("%.2f", responseTime) + "ms");
            return new RedirectView("/files", true);
        } catch(Exception e) {
            re.addFlashAttribute("error", e.getMessage());
            return new RedirectView("/files", true);
        }
    }

    @PostMapping("/deleteFile/{fileId}")
    public RedirectView delete(@PathVariable Long fileId, RedirectAttributes redirectAttributes) {
        try {
            fileService.deleteFile(fileId);
            redirectAttributes.addFlashAttribute("message", "File deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete file");
        }
        return new RedirectView("/files", true);
    }

    @GetMapping("/downloadFile/{fileId}")
    public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
        try {
            byte[] file = fileService.getFile(fileId).getFileContent();
            return ResponseEntity.ok().body(file);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/decryptFile/{fileId}")
    public ResponseEntity<byte[]> decrypt(@PathVariable Long fileId) {
        try {
            byte[] file = fileService.decryptAndReturnFile(fileId);
            return ResponseEntity.ok().body(file);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}
