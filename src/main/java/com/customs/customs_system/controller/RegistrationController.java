package com.customs.customs_system.controller;

import com.customs.customs_system.entity.User;
import com.customs.customs_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Set;

@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register"; // سننشئ صفحة register.html
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return "redirect:/register?error";
        }
        
        User newUser = new User();
        newUser.setUsername(username);
        // تشفير الباسورد قبل الحفظ في Neon
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEnabled(true);
        newUser.setRoles(Set.of("ROLE_USER"));
        
        userRepository.save(newUser);
        return "redirect:/login?registered";
    }
}