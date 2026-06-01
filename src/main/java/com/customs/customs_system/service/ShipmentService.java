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

    // ======================================================
    // ➕ CREATE SHIPMENT (المطورة لتوليد رمز التحقق السري تلقائياً)
    // ======================================================
    public Shipment createShipment(Shipment shipment) {
        // 1. توليد رمز تحقق سري عشوائي وفريد مكون من 6 خانات (حروف كبيرة وأرقام)
        String randomCode = UUID.randomUUID().toString()
                                .replace("-", "")
                                .substring(0, 6)
                                .toUpperCase();
        
        shipment.setVerificationCode(randomCode);
        shipment.setIsLocked(false); // الشحنة تفتح جديدة ومتاحة للتعديل لو رفضت

        // 2. التأكيد الإضافي على الحالة الابتدائية
        if (shipment.getStatus() == null) {
            shipment.setStatus(ShipmentStatus.PENDING);
        }
        
        // 3. الحفظ في نيون (الـ ID التلقائي الراجع هو حيكون "رقم الدخول الجمركي")
        return shipmentRepository.save(shipment);
    }

    // ======================================================
    // 🔐 LOGIN & VERIFY (الدالة الذكية لولوج المخلص والتعديل)
    // ======================================================
    public Shipment loginAndGetShipment(Long id, String verificationCode) {
        // البحث عن الشحنة برقم الدخول (ID)
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("❌ عذراً، رقم الدخول الجمركي هذا غير مسجل بالنظام!"));

        // التحقق من الرمز السري (مع تجاهل الحروف الكبيرة والصغيرة لسهولة الاستخدام)
        if (shipment.getVerificationCode() == null || 
            !shipment.getVerificationCode().equalsIgnoreCase(verificationCode.trim())) {
            throw new IllegalArgumentException("❌ عذراً، رمز التحقق السري الذي أدخلته غير صحيح!");
        }

        // قفل الدخول التام إذا كانت الشحنة معتمدة رسمياً (APPROVED)
        if (shipment.getStatus() == ShipmentStatus.APPROVED || Boolean.TRUE.equals(shipment.getIsLocked())) {            throw new IllegalStateException("🔒 عذراً، هذه الشحنة تم اعتمادها وقفلها نهائياً من قبل مصلحة الجمارك، ولا يمكن تعديلها!");
        }

        // لو كل شيء سليم، يرجع الشحنة عشان تفتح صفحة التعديل فوراً
        return shipment;
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
    // ✅ APPROVAL & REJECTION 
    // ======================================================
    @Transactional
    public void approveAndArchive(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + id));

        shipment.setStatus(ShipmentStatus.APPROVED);
        shipment.setApprovedAt(LocalDateTime.now());
        shipment.setIsLocked(true); // قفل الشحنة صراحة لمنع أي محاولة دخول مستقبلاً

        shipmentRepository.save(shipment);
        System.out.println("✅ Shipment ID " + id + " has been approved on: " + shipment.getApprovedAt());
    }

    @Transactional
    public void rejectShipment(Long id, String reason) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));
        
        shipment.setStatus(ShipmentStatus.REJECTED);
        shipment.setRejectionReason(reason); 
        shipment.setIsLocked(false); // التأكيد على فتحها لكي يستطيع المخلص تعديل النواقص برقم الدخول والرمز

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

    // ======================================================
    // 🛠️ UPDATE SHIPMENT
    // ======================================================
    @Transactional
    public Shipment updateShipment(Long id, Shipment updatedData) {
        Shipment existing = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        // 1. تحديث البيانات الأساسية 
        existing.setCustomsbroker(updatedData.getCustomsbroker());
        existing.setBrokerPhone(updatedData.getBrokerPhone());
        existing.setContainerNumber(updatedData.getContainerNumber());
        existing.setStatisticalCode(updatedData.getStatisticalCode());
        existing.setEntityName(updatedData.getEntityName());
        existing.setBrokerNationalId(updatedData.getBrokerNationalId());
        
        // 2. الفحص الذكي: إذا عدلها وهي مرفوضة ترجع PENDING
        if (existing.getStatus() == ShipmentStatus.REJECTED) {
            existing.setStatus(ShipmentStatus.PENDING); 
            existing.setRejectionReason(null);          
        }

        return shipmentRepository.save(existing);
    }
    
    public Shipment getShipmentByAnyId(String query) {
        return shipmentRepository.findAllByAnyId(query.trim()).orElse(null);
    }

    // ======================================================
    // 🔍 COMPOSITE DUPLICATION CHECKS
    // ======================================================
    public boolean isShipmentDuplicate(String statisticalCode, String containerNumber) {
        if (statisticalCode == null || statisticalCode.trim().isEmpty() || 
            containerNumber == null || containerNumber.trim().isEmpty()) {
            return false;
        }
        return shipmentRepository.existsByStatisticalCodeAndContainerNumber(statisticalCode.trim(), containerNumber.trim());
    }

    public boolean isShipmentDuplicateForOther(String statisticalCode, String containerNumber, Long currentId) {
        if (statisticalCode == null || statisticalCode.trim().isEmpty() || 
            containerNumber == null || containerNumber.trim().isEmpty()) {
            return false;
        }
        return shipmentRepository.existsByStatisticalCodeAndContainerNumberAndIdNot(statisticalCode.trim(), containerNumber.trim(), currentId);
    }



 // ========================================================
 // 🗑️ دالة حذف مستند فردي من الأرشيف الرقمي (Neon)
 // ========================================================
 @Transactional
 public void deleteDocumentById(Long docId) {
     if (documentRepository.existsById(docId)) {
         documentRepository.deleteById(docId);
         System.out.println("🗑️ تم حذف المستند الفردي رقم: " + docId + " من قاعدة البيانات بنجاح.");
     } else {
         throw new IllegalArgumentException("⚠️ عذراً، المستند غير موجود بالفعل.");
     }
 }

}