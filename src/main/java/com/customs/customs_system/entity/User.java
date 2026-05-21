package com.customs.customs_system.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users") 
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private boolean enabled = true;
    
    // حقل نصي عادي داخل نفس الجدول لمنع المرطزة
    private String role; 

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // إذا كان الدور فارغاً نضع USER افتراضياً، ونضيف ROLE_ تلقائياً في الذاكرة
        String r = (role == null || role.isEmpty()) ? "USER" : role.toUpperCase();
        return List.of(new SimpleGrantedAuthority("ROLE_" + r));
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return this.enabled; }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @Override public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // 🔥 التعديل المضاف هنا: الـ Getter والـ Setter للـ Enabled عشان يختفي الخط الأحمر
    public boolean isEnabledField() { return enabled; } // ميثود عادية للحقل تختلف عن ميثود السكيورتي فوق
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}