package org.example.crypto_app.controller;

import org.example.crypto_app.model.FileInfoDTO;
import org.example.crypto_app.model.UserFile;
import org.example.crypto_app.service.FileService;
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

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/files") // Load page with user files info
    public String files(Model model, RedirectAttributes redirectAttributes) {
        try {
            List<FileInfoDTO> filesInfo = fileService.getUserFilesInfo();
            if(!filesInfo.isEmpty()) model.addAttribute("files", filesInfo);

            // Add message and error attributes to the model
            if (redirectAttributes.getFlashAttributes().containsKey("message")) {
                model.addAttribute("message", redirectAttributes.getFlashAttributes().get("message"));
            }
            if (redirectAttributes.getFlashAttributes().containsKey("error")) {
                model.addAttribute("error", redirectAttributes.getFlashAttributes().get("error"));
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "files";
    }

    @PostMapping("/uploadFile")
    public RedirectView upload(MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            UserFile newFile = new UserFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
            newFile.generateHashes();
            fileService.saveFile(newFile);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return new RedirectView("/files", true);
    }

    @PostMapping("/deleteFile/{fileId}")
    public RedirectView delete(@PathVariable Long fileId, RedirectAttributes redirectAttributes) {
        try {
            fileService.deleteFile(fileId);
            redirectAttributes.addFlashAttribute("message", "File deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return new RedirectView("/files", true);
    }

    @GetMapping("/downloadFile/{fileId}")
    public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
        try {
            byte[] file = fileService.getFileContent(fileId);
            return ResponseEntity.ok().body(file);
        } catch (Exception e) {
            System.out.println("Error downloading file: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Encrypt file, given fileId and keyId
    @PostMapping("/encryptFile/{fileId}/{keyId}")
    public ResponseEntity<?> encrypt(@PathVariable Long fileId, @PathVariable Long keyId) {
        try {
            fileService.encryptFile(fileId, keyId);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return new RedirectView("/files", true);
    }

}
