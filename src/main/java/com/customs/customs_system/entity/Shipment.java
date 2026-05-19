package com.customs.customs_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import com.customs.customs_system.entity.ShipmentStatus;
import com.customs.customs_system.entity.Document;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String containerNumber;   // رقم الحاوية
    private String customsbroker;     // اسم المخلص
    private String brokerPhone;       // رقم هاتف المخلص
    private String statisticalCode;   // الرمز الإحصائي

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;   // حقل لتخزين سبب الرفض

    
    // حقل لتخزين سبب الرفض
    
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    // ✅ تاريخ الإدخال: يُمنع تحديثه بعد أول مرة (updatable = false)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ✅ تاريخ الاعتماد: يتم تسجيله فقط عند الضغط على "اعتماد"
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(
            mappedBy = "shipment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
        )
    @JsonManagedReference
    private List<Document> documents;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ShipmentStatus.PENDING;
        }
    }

    // ==========================================
    // Getters & Setters (تمت إضافة حقل الاعتماد)
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContainerNumber() { return containerNumber; }
    public void setContainerNumber(String containerNumber) { this.containerNumber = containerNumber; }

    public String getCustomsbroker() { return customsbroker; }
    public void setCustomsbroker(String customsbroker) { this.customsbroker = customsbroker; }

    public String getBrokerPhone() { return brokerPhone; }
    public void setBrokerPhone(String brokerPhone) { this.brokerPhone = brokerPhone; }

    public String getStatisticalCode() { return statisticalCode; }
    public void setStatisticalCode(String statisticalCode) { this.statisticalCode = statisticalCode; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }


 // مع إضافة الـ Getter و Setter له
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}