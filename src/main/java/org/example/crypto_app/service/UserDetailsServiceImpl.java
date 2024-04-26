package org.example.crypto_app.service;

import org.example.crypto_app.model.BaseUser;
import org.example.crypto_app.repository.BaseUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final BaseUserRepository baseUserRepository;

    public UserDetailsServiceImpl(BaseUserRepository baseUserRepository) {
        this.baseUserRepository = baseUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Loading user by username: " + username);
        BaseUser user =  baseUserRepository.findByUsername(username);
        if(user == null) throw new UsernameNotFoundException("User not found");
        return user;
    }
}
