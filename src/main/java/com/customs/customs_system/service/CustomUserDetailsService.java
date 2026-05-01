package com.customs.customs_system.service;

import com.customs.customs_system.entity.User;
import com.customs.customs_system.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // البحث عن المستخدم في قاعدة بيانات نيون
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("المستخدم غير موجود: " + username));

        // تحويل بيانات المستخدم لصيغة يفهمها Spring Security
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(user.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .build();
    }
}