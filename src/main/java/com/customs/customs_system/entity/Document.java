package com.customs.customs_system.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

@Entity
public class Document {
	
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "doc_generator")
    @SequenceGenerator(
        name = "doc_generator", 
        sequenceName = "doc_seq", 
        allocationSize = 50 
    )
    private Long id;

    private String fileUrl; 

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    // 2. إضافة هذا السطر هنا لمنع السيرفر من الدخول في حلقة مفرغة عند تحويل البيانات لـ JSON
    @JsonBackReference 
    private Shipment shipment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
    }

