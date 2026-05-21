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
        // البحث عن المستخدم في قاعدة البيانات (نيون أو لوكال)
        // 👈 التعديل هنا: نرجعوا الـ user المجلوب مباشرة لأنه هو بحد ذاته يمثل UserDetails
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("المستخدم غير موجود: " + username));
    }
}