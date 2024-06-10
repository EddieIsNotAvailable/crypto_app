package org.example.crypto_app.service;

import org.example.crypto_app.model.BaseUser;
import org.example.crypto_app.repository.BaseUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final BaseUserRepository baseUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(BaseUserRepository baseUserRepository, PasswordEncoder passwordEncoder) {
        this.baseUserRepository = baseUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void createUser(BaseUser user) {
        BaseUser existingUser = baseUserRepository.findByUsername(user.getUsername());
        if(existingUser != null) throw new IllegalArgumentException("Username already in use");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        baseUserRepository.save(user);
    }

    public void deleteUser(BaseUser user, String passwordConfirmation) {
        if(passwordEncoder.matches(passwordConfirmation, user.getPassword())) {
            baseUserRepository.delete(user);
        } else throw new IllegalArgumentException("Incorrect Password");
    }

    public void changePassword(BaseUser user, String currentPassword, String newPassword) {
        newPassword = passwordEncoder.encode(newPassword);
        if(passwordEncoder.matches(currentPassword, user.getPassword())) {
            System.out.println("Passwords matched!");
            user.setPassword(newPassword);
            baseUserRepository.save(user);
        } else throw new IllegalArgumentException("Incorrect Password");
    }
}
