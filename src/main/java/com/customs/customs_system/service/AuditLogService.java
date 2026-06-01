package com.customs.customs_system.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.customs.customs_system.entity.AuditLog;
import com.customs.customs_system.repository.AuditLogRepository;


@Service
public class AuditLogService {

	 private final AuditLogRepository auditLogRepository;

	    public AuditLogService(AuditLogRepository auditLogRepository) {
	        this.auditLogRepository = auditLogRepository;
	    }

	    public List<AuditLog> getAllLogsInOrder() {
	        return auditLogRepository.findAllByOrderByActionDateDesc();
	    }

	    // 🟢 تحديث الدالة لتستقبل الرمز الإحصائي وتخزنه
	    public void saveLog(String container, String statisticalCode, String user, String action, String details) {
	        AuditLog log = new AuditLog(container, statisticalCode, user, action, details);
	        auditLogRepository.save(log);
	    }
	}