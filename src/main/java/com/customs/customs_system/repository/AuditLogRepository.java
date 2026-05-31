package com.customs.customs_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.customs.customs_system.entity.AuditLog;


public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // جلب السجلات مرتبة من الأحدث للأقدم تلقائياً
    List<AuditLog> findAllByOrderByActionDateDesc();
}
