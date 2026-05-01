package com.customs.customs_system.repository;

import com.customs.customs_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // دالة للبحث عن المستخدم بواسطة اسمه (Username)
    Optional<User> findByUsername(String username);
}