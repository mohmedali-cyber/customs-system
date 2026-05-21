package com.customs.customs_system;

import com.customs.customs_system.entity.User;
import com.customs.customs_system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@SpringBootApplication
public class CustomsSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomsSystemApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
	    return args -> {
	        if (userRepository.findByUsername("admin").isEmpty()) {
	            User admin = new User();
	            admin.setUsername("admin");
	            admin.setPassword(passwordEncoder.encode("123")); 
	            admin.setEnabled(true);
	            admin.setRole("ADMIN"); 
	            userRepository.save(admin);
	        }

	        if (userRepository.findByUsername("editor").isEmpty()) {
	            User editor = new User();
	            editor.setUsername("editor");
	            editor.setPassword(passwordEncoder.encode("123")); 
	            editor.setEnabled(true);
	            editor.setRole("EDITOR"); 
	            userRepository.save(editor);
	        }
	        
	        if (userRepository.findByUsername("user").isEmpty()) {
	            User user = new User();
	            user.setUsername("user");
	            user.setPassword(passwordEncoder.encode("123")); 
	            user.setEnabled(true);
	            user.setRole("USER"); 
	            userRepository.save(user);
	        }
	    };
	}}