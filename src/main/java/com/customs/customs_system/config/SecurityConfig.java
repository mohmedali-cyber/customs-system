package com.customs.customs_system.config;

import com.customs.customs_system.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // تشفير كلمة المرور
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // تعطيل مؤقت للتطوير لتسهيل طلبات الـ Fetch
            .authorizeHttpRequests(auth -> auth
                // 1. المصادر العامة المسموحة للجميع + روابط بوابة المخلصين بالكامل (عرض وإرسال وحفظ)
                .requestMatchers(
                    "/login", 
                    "/register", 
                    "/css/**", 
                    "/images/**", 
                    "/js/**",
                    "/shipments/gate",           // واجهة الدخول برقم المعاملة والرمز السري (النسخة السوداء)
                    "/shipments/track-login",     // دالة التحقق الآمن من الرمز السري عبر الـ Fetch
                    "/shipments/new",             // فتح صفحة تسجيل شحنة جديدة
                    "/shipments/save",            // 🔥 إجباري: فتح دالة حفظ الشحنة الجديدة القادمة من استمارة المخلص
                    "/shipments/edit-list",       // واجهة السلايدر لتعديل بيانات ومستندات الشحنة
                    "/shipments/edit/**",         // 🔥 إجباري: فتح مسار التعديل والتحديث الخاص بالشحنة
                    "/shipments/api/search",      // API جلب بيانات الشحنة وتفاصيلها
                    "/shipments/api/documents/**"   // API حذف المستند الفردي
                ).permitAll()
                
                // 2. روابط الاعتماد والرفض (خاصة بالـ ADMIN والـ EDITOR فقط من الموظفين)
                .requestMatchers("/shipments/approve/**", "/shipments/reject/**").hasAnyRole("ADMIN", "EDITOR")
                
                // 3. تأمين لوحات التحكم الحساسة لو كانت موجودة مستقبلاً
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // 4. أي طلب آخر داخل المنظومة يتطلب دخول الموظف بيوزر وباسورد
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}