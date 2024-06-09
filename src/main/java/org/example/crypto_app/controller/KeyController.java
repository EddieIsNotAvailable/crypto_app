package org.example.crypto_app.controller;

import org.example.crypto_app.model.BaseUser;
import org.example.crypto_app.model.CryptoKey;
import org.example.crypto_app.model.enums.EncryptionType;
import org.example.crypto_app.service.KeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class KeyController {

    private final KeyService keyService;

    public KeyController(KeyService keyService) {
        this.keyService = keyService;
    }

    @GetMapping("keys")
    public String keys(Model model) {
        try {
            model.addAttribute("keyOptions", EncryptionType.getOptions());

            List<CryptoKey> keys = keyService.getUserKeys();
            if(!keys.isEmpty()) {
                model.addAttribute("keys", keys);
            }
        } catch (RuntimeException e) {
//            System.out.println("Error loading keys page: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
        }

        return "keys";
    }

    @PostMapping("/createKey")
    public String generateKey(@RequestParam String keyName, @RequestParam String keySelect, Model model) {
        BaseUser user = (BaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            keyService.createKey(keyName, keySelect);
            model.addAttribute("message", keySelect + " key created successfully");
//            System.out.println("Key created successfully for user: " + user.getUsername());
        } catch (RuntimeException e) {
//            System.out.println("Error creating key: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
        }
        return "redirect:/keys";
    }

    @PostMapping("/deleteKey/{keyId}")
    public ResponseEntity<?> deleteKey(@PathVariable Long keyId) {
        try {
            keyService.deleteKey(keyId);
        } catch (RuntimeException e) {
//            System.out.println("Error deleting key: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/downloadKey/{keyId}")
    public ResponseEntity<byte[]> downloadKey(@PathVariable Long keyId, Model model) {
        try {
            byte[] keyBytes = keyService.downloadKey(keyId);
            return ResponseEntity.ok().body(keyBytes);
        } catch (RuntimeException e) {
//            System.out.println("Error downloading key: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }



}
