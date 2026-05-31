package com.customs.customs_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "container_number", nullable = false)
    private String containerNumber;

    // 🟢 الحقل الجديد المضاف
    @Column(name = "statistical_code")
    private String statisticalCode;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "action_type", nullable = false)
    private String actionType; 

    @Column(name = "action_date", nullable = false)
    private LocalDateTime actionDate;

    @Column(name = "details", length = 1000)
    private String details;

    // --- الكونستركتورز ---
    public AuditLog() {}

    // 🟢 تحديث الكونستركتور ليستقبل الرمز الإحصائي في الترتيب الثاني
    public AuditLog(String containerNumber, String statisticalCode, String performedBy, String actionType, String details) {
        this.containerNumber = containerNumber;
        this.statisticalCode = statisticalCode;
        this.performedBy = performedBy;
        this.actionType = actionType;
        this.actionDate = LocalDateTime.now();
        this.details = details;
    }

    // --- الـ Getters والـ Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContainerNumber() { return containerNumber; }
    public void setContainerNumber(String containerNumber) { this.containerNumber = containerNumber; }

    // 🟢 الـ Getter والـ Setter الجدد للرمز الإحصائي
    public String getStatisticalCode() { return statisticalCode; }
    public void setStatisticalCode(String statisticalCode) { this.statisticalCode = statisticalCode; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public LocalDateTime getActionDate() { return actionDate; }
    public void setActionDate(LocalDateTime actionDate) { this.actionDate = actionDate; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
