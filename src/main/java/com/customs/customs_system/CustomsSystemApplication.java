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
			// التحقق إذا كان مستخدم admin موجود مسبقاً لتجنب التكرار
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				
				// تشفير كلمة المرور "123" باستخدام الـ Encoder اللي عرفناه في SecurityConfig
				admin.setPassword(passwordEncoder.encode("123")); 
				admin.setEnabled(true);
				
				// إعطاؤه صلاحية مدير
				admin.setRoles(Set.of("ROLE_ADMIN"));
				
				userRepository.save(admin);
				System.out.println("-----------------------------------------");
				System.out.println("✅ تم إنشاء مستخدم الأدمن بنجاح (admin / 123)");
				System.out.println("-----------------------------------------");
			}
		};
	}
}
