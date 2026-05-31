package com.customs.customs_system.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime; // إضافة استيراد الوقت
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.customs.customs_system.entity.*;
import com.customs.customs_system.repository.*;

@Service
public class ShipmentService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ShipmentRepository shipmentRepository;
    private final DocumentRepository documentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           DocumentRepository documentRepository) {
        this.shipmentRepository = shipmentRepository;
        this.documentRepository = documentRepository;
    }

    // =========================
    // ➕ CREATE SHIPMENT
    // =========================
    public Shipment createShipment(Shipment shipment) {
        // الـ prePersist في الـ Entity ستتكفل بالـ createdAt والـ Status
        // ولكن للتأكيد الإضافي:
        if (shipment.getStatus() == null) {
            shipment.setStatus(ShipmentStatus.PENDING);
        }
        return shipmentRepository.save(shipment);
    }

    // =========================
    // 📊 DASHBOARD STATS
    // =========================
    public long getPendingCount() {
        return shipmentRepository.countByStatus(ShipmentStatus.PENDING);
    }

    public long getApprovedCount() {
        return shipmentRepository.countArchived();
    }

    // =========================
    // 📋 GET DATA
    // =========================
    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    public Shipment getShipmentById(Long id) {
        return shipmentRepository.findByIdWithDocuments(id);
    }

    // ======================================================
    // ✅ APPROVAL & REJECTION (المعدلة لتشمل تاريخ الاعتماد)
    // ======================================================
    @Transactional
    public void approveAndArchive(Long id) {
        // 1. نجلب الشحنة بالكامل من قاعدة البيانات
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + id));

        // 2. نحدث الحالة إلى معتمد
        shipment.setStatus(ShipmentStatus.APPROVED);

        // 3. نثبت تاريخ الاعتماد "الآن" (وهو مختلف عن تاريخ الإدخال)
        shipment.setApprovedAt(LocalDateTime.now());

        // 4. نحفظ التغييرات
        shipmentRepository.save(shipment);
        
        System.out.println("✅ Shipment ID " + id + " has been approved on: " + shipment.getApprovedAt());
    }

    
    
 // في ملف ShipmentService.java
    @Transactional
    public void rejectShipment(Long id, String reason) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));
        
        shipment.setStatus(ShipmentStatus.REJECTED);
        shipment.setRejectionReason(reason); // حفظ السبب هنا
        
        shipmentRepository.save(shipment);
        System.out.println("❌ تم رفض الشحنة رقم " + id + " بسبب: " + reason);
    }

    
    
    
    
    // ======================================================
    // 🚀 ASYNC DOCUMENT PROCESSING (IMAGEKIT)
    // ======================================================
    @Async
    public void processDocumentsAsync(Long shipmentId,
                                      MultipartFile[] bolFiles, MultipartFile[] doFiles,
                                      MultipartFile[] ocFiles, MultipartFile[] invFiles,
                                      MultipartFile[] plFiles, MultipartFile[] alFiles,
                                      MultipartFile[] cdFiles, MultipartFile[] biFiles,
                                      MultipartFile[] siFiles) {

        List<Document> docs = new ArrayList<>();

        collect(bolFiles, shipmentId, DocumentType.BILL_OF_LADING, docs);
        collect(doFiles, shipmentId, DocumentType.DELIVERY_ORDER, docs);
        collect(ocFiles, shipmentId, DocumentType.CERTIFICATE_OF_ORIGIN, docs);
        collect(invFiles, shipmentId, DocumentType.INVOICE, docs);
        collect(plFiles, shipmentId, DocumentType.PACKING_LIST, docs);
        collect(alFiles, shipmentId, DocumentType.AUTHORIZATION_LETTER, docs);
        collect(cdFiles, shipmentId, DocumentType.CUSTOMS_DECLARATION, docs);
        collect(biFiles, shipmentId, DocumentType.BROKER_ID, docs);
        collect(siFiles, shipmentId, DocumentType.STATISTICAL_IMAGE, docs);

        saveAllDocuments(docs);
    }

    
    
    private void collect(MultipartFile[] files, Long shipmentId, DocumentType type, List<Document> docs) {
        if (files == null) return;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    String url = uploadToImageKit(file);
                    docs.add(createDocumentObject(shipmentId, url, type));
                } catch (Exception e) {
                    System.err.println("❌ Upload failed for " + type + ": " + e.getMessage());
                }
            }
        }
    }

    
    
    public Document createDocumentObject(Long shipmentId, String fileUrl, DocumentType type) {
        Shipment shipmentProxy = shipmentRepository.getReferenceById(shipmentId);
        Document doc = new Document();
        doc.setFileUrl(fileUrl);
        doc.setType(type);
        doc.setShipment(shipmentProxy);
        return doc;
    }

    
    
    @Transactional
    public void saveAllDocuments(List<Document> documents) {
        if (documents != null && !documents.isEmpty()) {
            documentRepository.saveAll(documents);
        }
    }

    
    
    // =========================
    // 📤 IMAGEKIT INTEGRATION
    // =========================
    public String uploadToImageKit(MultipartFile file) throws IOException {
        Map<String, String> auth = getImageKitAuth();
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        });
        body.add("fileName", file.getOriginalFilename());
        body.add("publicKey", auth.get("publicKey"));
        body.add("signature", auth.get("signature"));
        body.add("token", auth.get("token"));
        body.add("expire", auth.get("expire"));
        body.add("folder", "/customs_docs");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        Map<?, ?> response = restTemplate.postForObject("https://upload.imagekit.io/api/v1/files/upload", request, Map.class);

        if (response != null && response.containsKey("url")) {
            return response.get("url").toString();
        }
        throw new RuntimeException("ImageKit upload failed");
    }

   
    
    public Map<String, String> getImageKitAuth() {
        String privateKey = "private_nquPEnCL4+zSvZi/auHh3NA9HWo=";
        String publicKey = "public_80IFZLJ87Rpxk1Gj8nCYPqTddJ0=";
        long expire = Instant.now().getEpochSecond() + 600;
        String token = UUID.randomUUID().toString();

        try {
            String toSign = token + expire;
            Mac sha1 = Mac.getInstance("HmacSHA1");
            SecretKeySpec key = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            sha1.init(key);
            byte[] hash = sha1.doFinal(toSign.getBytes(StandardCharsets.UTF_8));

            Map<String, String> auth = new HashMap<>();
            auth.put("token", token);
            auth.put("expire", String.valueOf(expire));
            auth.put("signature", bytesToHex(hash));
            auth.put("publicKey", publicKey);
            return auth;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    // =========================
    // 🗑️ DELETE SHIPMENT
    // =========================
    @Transactional
    public void deleteShipment(Long id) {
        shipmentRepository.deleteById(id);
        System.out.println("🗑️ تم حذف الشحنة رقم: " + id);
    }













//    @Transactional
//    public Shipment updateShipment(Long id, Shipment updatedData) {
//        Shipment existing = shipmentRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Shipment not found"));
//
//        // تحديث البيانات الأساسية فقط
//        existing.setCustomsbroker(updatedData.getCustomsbroker());
//        existing.setBrokerPhone(updatedData.getBrokerPhone());
//        existing.setContainerNumber(updatedData.getContainerNumber());
//        existing.setStatisticalCode(updatedData.getStatisticalCode());
//        
//        // ملاحظة: لا نغير الـ createdAt ولا الـ Status إلا لو أردت ذلك يدوياً
//        return shipmentRepository.save(existing);
//    }
    @Transactional
    public Shipment updateShipment(Long id, Shipment updatedData) {
        Shipment existing = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        // 1. تحديث البيانات الأساسية القادمة من شاشة التعديل
        existing.setCustomsbroker(updatedData.getCustomsbroker());
        existing.setBrokerPhone(updatedData.getBrokerPhone());
        existing.setContainerNumber(updatedData.getContainerNumber());
        existing.setStatisticalCode(updatedData.getStatisticalCode());
        
        // 2. الفحص الذكي: إذا كانت الشحنة مرفوضة سابقاً، نعيدها قيد المراجعة وننظف سبب الرفض
        if (existing.getStatus() == ShipmentStatus.REJECTED) {
            existing.setStatus(ShipmentStatus.PENDING); // تتحول تلقائياً إلى (قيد المراجعة) في الـ Tabs
            existing.setRejectionReason(null);          // مسح سبب الرفض القديم حتى لا يسبب ارتباكاً للمراجع
        }

        // ملاحظة: الـ createdAt لن يتأثر وسيبقى ثابتاً كما هو في قاعدة البيانات
        return shipmentRepository.save(existing);
    }
    
    
    
    
    public List<Shipment> findByStatus(ShipmentStatus status) {
        return shipmentRepository.findByStatus(status);
    }
    
    
    
    
    
    
//    public Shipment getShipmentByAnyId(String query) {
//        // محاولة البحث برقم الحاوية أولاً ثم الرمز الإحصائي
//        return shipmentRepository.findByContainerNumber(query)
//                .orElseGet(() -> shipmentRepository.findByStatisticalCode(query)
//                .orElse(null));
//    }
 // تعديل دالة البحث لتشمل الجميع
    public Shipment getShipmentByAnyId(String query) {
        // نستخدم findAllByAnyId لضمان جلب الشحنة مهما كانت حالتها
        // الـ .trim() تمسح المسافات الزائدة التي قد تسبب فشل البحث
        return shipmentRepository.findAllByAnyId(query.trim()).orElse(null);
    }

    
    
    
    
 // 🔍 1. فحص الرمز الإحصائي قبل إدخال أي شحنة جديدة
    public boolean isStatisticalCodeExists(String statisticalCode) {
        if (statisticalCode == null || statisticalCode.trim().isEmpty()) {
            return false;
        }
        return shipmentRepository.existsByStatisticalCode(statisticalCode.trim());
    }

    // 🔍 2. فحص الرمز الإحصائي عند التعديل لشحنة أخرى
    public boolean isStatisticalCodeExistsForOther(String statisticalCode, Long currentId) {
        if (statisticalCode == null || statisticalCode.trim().isEmpty()) {
            return false;
        }
        return shipmentRepository.existsByStatisticalCodeAndIdNot(statisticalCode.trim(), currentId);
    }
}