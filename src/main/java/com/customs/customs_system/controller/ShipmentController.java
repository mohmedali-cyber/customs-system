package com.customs.customs_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.customs.customs_system.entity.*;
import com.customs.customs_system.service.ShipmentService;
import com.example.customs_systemm.entity.Shipment;
import com.example.customs_systemm.entity.User;
import com.customs.customs_system.repository.ShipmentRepository;
import com.customs.customs_system.repository.UserRepository;

import java.util.List;


@Controller
@RequestMapping("/shipments")
public class ShipmentController {

	 private final ShipmentService shipmentService;
	    private final UserRepository userRepository; 

	    @Autowired
	    private ShipmentRepository shipmentRepository;
	    
	    public ShipmentController(ShipmentService shipmentService, UserRepository userRepository) {
	        this.shipmentService = shipmentService;
	        this.userRepository = userRepository;
	    }
	    
    // ==========================================
    // 📥 1. العرض الأساسي مع التقسيم (9 سجلات)
    // ==========================================
    @GetMapping("")
    public String getShipments(
            @RequestParam(name = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        
        try {
            ShipmentStatus selectedStatus = ShipmentStatus.valueOf(status.toUpperCase());
            PageRequest pageable = PageRequest.of(page, 7, Sort.by("id").descending());
            
            Page<Shipment> shipmentPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                shipmentPage = shipmentRepository.searchByStatus(keyword, selectedStatus, pageable);
                model.addAttribute("keyword", keyword);
            } else {
                shipmentPage = shipmentRepository.findByStatusBasic(selectedStatus, pageable);
            }
            
            model.addAttribute("shipments", shipmentPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", shipmentPage.getTotalPages());
            model.addAttribute("currentStatus", status.toUpperCase());
            
            String title = switch (selectedStatus) {
                case APPROVED -> "الأرشيف المعتمد";
                case REJECTED -> "المرفوضات";
                default -> "المراجعة";
            };
            
            model.addAttribute("pageTitle", "مركز معالجة الشحنات | " + title);
            return "shipment-list";
            
        } catch (IllegalArgumentException e) {
            return "redirect:/shipments?status=PENDING"; 
        }
    }

    
    
    
    
    
    
    
    
    @GetMapping("/fragment")
    public String getShipmentsFragment(
            @RequestParam(name = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        try {
            ShipmentStatus selectedStatus = ShipmentStatus.valueOf(status.toUpperCase());
            PageRequest pageable = PageRequest.of(page, 7, Sort.by("id").descending());

            Page<Shipment> shipmentPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                shipmentPage = shipmentRepository.searchByStatus(keyword, selectedStatus, pageable);
                model.addAttribute("keyword", keyword);
            } else {
                shipmentPage = shipmentRepository.findByStatusBasic(selectedStatus, pageable);
            }

            model.addAttribute("shipments", shipmentPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", shipmentPage.getTotalPages());
            model.addAttribute("currentStatus", status.toUpperCase());

            // 🔥 أهم سطر
            return "shipment-list :: tableFragment";

        } catch (IllegalArgumentException e) {
            return "shipment-list :: tableFragment";
        }
    }
    
    
    
    
    
    
    
    
    // ==========================================
    // 🛡️ إعادة توجيه الروابط القديمة
    // ==========================================
    @GetMapping("/pending")
    public String redirectOldPending() {
        return "redirect:/shipments?status=PENDING";
    }

    @GetMapping("/archive")
    public String redirectOldArchive() {
        return "redirect:/shipments?status=APPROVED";
    }

    // ==========================================
    // 📄 2. إضافة شحنة جديدة
    // ==========================================
    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("shipment", new Shipment());
        return "shipment-form";
    }

    // ==========================================
    // 🚀 3. حفظ الشحنة
    // ==========================================
    @PostMapping("/save")
    @ResponseBody
    public Shipment saveShipment(@ModelAttribute Shipment shipment,
                                 @RequestParam(value="BOL_files", required=false) MultipartFile[] bolFiles,
                                 @RequestParam(value="DO_files", required=false) MultipartFile[] doFiles,
                                 @RequestParam(value="OC_files", required=false) MultipartFile[] ocFiles,
                                 @RequestParam(value="INV_files", required=false) MultipartFile[] invFiles,
                                 @RequestParam(value="PL_files", required=false) MultipartFile[] plFiles,
                                 @RequestParam(value="AL_files", required=false) MultipartFile[] alFiles,
                                 @RequestParam(value="CD_files", required=false) MultipartFile[] cdFiles,
                                 @RequestParam(value="BI_files", required=false) MultipartFile[] biFiles,
                                 @RequestParam(value="SI_files", required=false) MultipartFile[] siFiles) {

        Shipment saved = shipmentService.createShipment(shipment);
        shipmentService.processDocumentsAsync(
                saved.getId(), bolFiles, doFiles, ocFiles, invFiles, 
                plFiles, alFiles, cdFiles, biFiles, siFiles
        );
        return saved;
    }

    // ==========================================
    // ✅ 4. اعتماد الشحنة
    // ==========================================
    @GetMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {
        shipmentService.approveAndArchive(id);
        // التوجيه للأرشيف المعتمد لرؤية النتيجة
        return "redirect:/shipments?status=APPROVED";
    }

    // ==========================================
    // ❌ 5. رفض الشحنة (تعديل لاستقبال السبب)
    // ==========================================
    @PostMapping("/reject/{id}")
    public String reject(@PathVariable Long id, @RequestParam("reason") String reason) {
        shipmentService.rejectShipment(id, reason);
        // التوجيه لقائمة المرفوضات لرؤية النتيجة
        return "redirect:/shipments?status=REJECTED";
    }

    // ==========================================
    // 📁 6. عرض المستندات
    // ==========================================
//    @GetMapping("/view-docs/{id}")
//    public String viewDocuments(@PathVariable Long id, Model model) {
//        Shipment shipment = shipmentService.getShipmentById(id);
//        model.addAttribute("shipment", shipment);
//        return "view-documents";
//    }
//    @GetMapping("/view-docs/{id}")
//    public String viewDocuments(@PathVariable Long id, Model model) {
//        // 1. جلب بيانات الشحنة الحالية كالعادة
//        Shipment shipment = shipmentService.getShipmentById(id);
//        model.addAttribute("shipment", shipment);
//
//        try {
//            // 2. معرفة اسم المستخدم الحالي اللي مسجل دخول
//            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
//            String currentUsername = auth.getName();
//
//            // 3. جلب المستخدم من جدول users في نيون
//            User currentUser = userRepository.findByUsername(currentUsername).orElse(null);
//
//            if (currentUser != null && currentUser.getRoles() != null) {
//                // هنا السحر: التشييك المباشر على الكلمة اللي أنت كاتبها في نيون
//                if (currentUser.getRoles().contains("ROLE_ADMIN")) {
//                    model.addAttribute("userRole", "ADMIN");
//                } else if (currentUser.getRoles().contains("ROLE_EDITOR")) {
//                    model.addAttribute("userRole", "EDITOR");
//                } else {
//                    model.addAttribute("userRole", "USER"); // مستخدم عادي بدون صلاحيات اعتماد
//                }
//            } else {
//                model.addAttribute("userRole", "GUEST"); // ضيف
//            }
//        } catch (Exception e) {
//            model.addAttribute("userRole", "GUEST");
//        }
//
//        return "view-documents";
//    }
   
    
    @GetMapping("/view-docs/{id}")
    public String viewDocuments(@PathVariable Long id, Model model) {
        // 1. جلب بيانات الشحنة الحالية
        Shipment shipment = shipmentService.getShipmentById(id);
        model.addAttribute("shipment", shipment);

        try {
            // 2. معرفة اسم المستخدم الحالي المسجل
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth.getName();

            // 3. جلب المستخدم من جدول users
            User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

            if (currentUser != null && currentUser.getRole() != null) {
                // نبعث الكلمة المكتوبة في القاعدة مباشرة (ADMIN أو USER أو EDITOR)
                model.addAttribute("userRole", currentUser.getRole().toUpperCase());
            } else {
                model.addAttribute("userRole", "USER");
            }
        } catch (Exception e) {
            model.addAttribute("userRole", "USER");
        }

        return "view-documents";
    }
    // ==========================================
    // 🗑️ 7. حذف الشحنة
    // ==========================================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, @RequestParam(name = "status", defaultValue = "PENDING") String status) {
        shipmentService.deleteShipment(id);
        // يعيدك لنفس الحالة التي كنت تتصفحها قبل الحذف
        return "redirect:/shipments?status=" + status;
    }

    // ==========================================
    // 🔌 8. API
    // ==========================================
    @GetMapping("/api")
    @ResponseBody
    public List<Shipment> getAll() {
        return shipmentService.getAllShipments();
    }








    




 // 1. عرض صفحة التعديل المنفردة
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Shipment shipment = shipmentService.getShipmentById(id);
        if (shipment == null) return "redirect:/shipments";
        
        model.addAttribute("shipment", shipment);
        // نرسل المستندات الحالية لعرضها في الصفحة
        model.addAttribute("existingDocs", shipment.getDocuments());
        return "shipment-edit"; // اسم ملف الـ HTML الجديد
    }

//    // 2. استقبال التحديث (POST)
//    @PostMapping("/update/{id}")
//    @ResponseBody
//    public Shipment updateShipment(@PathVariable Long id,
//                                   @ModelAttribute Shipment shipment,
//                                   @RequestParam(value="BOL_files", required=false) MultipartFile[] bolFiles,
//                                   @RequestParam(value="DO_files", required=false) MultipartFile[] doFiles,
//                                   @RequestParam(value="OC_files", required=false) MultipartFile[] ocFiles,
//                                   @RequestParam(value="INV_files", required=false) MultipartFile[] invFiles,
//                                   @RequestParam(value="PL_files", required=false) MultipartFile[] plFiles,
//                                   @RequestParam(value="AL_files", required=false) MultipartFile[] alFiles,
//                                   @RequestParam(value="CD_files", required=false) MultipartFile[] cdFiles,
//                                   @RequestParam(value="BI_files", required=false) MultipartFile[] biFiles,
//                                   @RequestParam(value="SI_files", required=false) MultipartFile[] siFiles) {
//
//        // تنفيذ التحديث في الـ Service
//        Shipment updated = shipmentService.updateShipment(id, shipment);
//
//        // معالجة الملفات الجديدة فقط إذا تم رفع شيء
//        shipmentService.processDocumentsAsync(
//                updated.getId(), bolFiles, doFiles, ocFiles, invFiles, 
//                plFiles, alFiles, cdFiles, biFiles, siFiles
//        );
//
//        return updated;
//    }
 
    
    // تعديل دالة التحديث لتكون متوافقة مع الفورم العادي
//    @PostMapping("/update/{id}")
//    public String updateShipment(@PathVariable Long id,
//                                   @ModelAttribute Shipment shipment,
//                                   @RequestParam(value="BOL_files", required=false) MultipartFile[] bolFiles,
//                                   @RequestParam(value="DO_files", required=false) MultipartFile[] doFiles,
//                                   @RequestParam(value="OC_files", required=false) MultipartFile[] ocFiles,
//                                   @RequestParam(value="INV_files", required=false) MultipartFile[] invFiles,
//                                   @RequestParam(value="PL_files", required=false) MultipartFile[] plFiles,
//                                   @RequestParam(value="AL_files", required=false) MultipartFile[] alFiles,
//                                   @RequestParam(value="CD_files", required=false) MultipartFile[] cdFiles,
//                                   @RequestParam(value="BI_files", required=false) MultipartFile[] biFiles,
//                                   @RequestParam(value="SI_files", required=false) MultipartFile[] siFiles) {
//
//        // 1. ربط الـ ID القادم من الرابط بالكائن لضمان التحديث وليس الإضافة
//        shipment.setId(id);
//
//        // 2. تنفيذ التحديث في الـ Service
//        Shipment updated = shipmentService.updateShipment(id, shipment);
//
//        // 3. معالجة الملفات (إذا وجدت)
//        shipmentService.processDocumentsAsync(
//                updated.getId(), bolFiles, doFiles, ocFiles, invFiles, 
//                plFiles, alFiles, cdFiles, biFiles, siFiles
//        );
//
//        // 4. التوجيه لصفحة القائمة بعد النجاح بدلاً من إرجاع كائن JSON
//        return "redirect:/shipments?status=PENDING";
//    }

    @PostMapping("/update/{id}")
    @ResponseBody // أضف هذه ليتعامل معها الـ Fetch في الصفحة
    public ResponseEntity<?> updateShipment(@PathVariable Long id,
                                   @ModelAttribute Shipment shipment,
                                   @RequestParam(value="BOL_files", required=false) MultipartFile[] bolFiles,
                                   @RequestParam(value="DO_files", required=false) MultipartFile[] doFiles,
                                   @RequestParam(value="OC_files", required=false) MultipartFile[] ocFiles,
                                   @RequestParam(value="INV_files", required=false) MultipartFile[] invFiles,
                                   @RequestParam(value="PL_files", required=false) MultipartFile[] plFiles,
                                   @RequestParam(value="AL_files", required=false) MultipartFile[] alFiles,
                                   @RequestParam(value="CD_files", required=false) MultipartFile[] cdFiles,
                                   @RequestParam(value="BI_files", required=false) MultipartFile[] biFiles,
                                   @RequestParam(value="SI_files", required=false) MultipartFile[] siFiles) {

        shipment.setId(id);
        Shipment updated = shipmentService.updateShipment(id, shipment);

        // معالجة الملفات الجديدة بنفس منطق الإضافة
        shipmentService.processDocumentsAsync(
                updated.getId(), bolFiles, doFiles, ocFiles, invFiles, 
                plFiles, alFiles, cdFiles, biFiles, siFiles
        );

        return ResponseEntity.ok(updated);
    }
    






    @GetMapping("/edit-list")
    public String showEditList(Model model) {
        // استدعاء الدالة لجلب الشحنات "قيد الانتظار" فقط
        List<Shipment> pendingShipments = shipmentService.findByStatus(ShipmentStatus.PENDING);
        model.addAttribute("shipments", pendingShipments);
        return "shipment-edit-list"; 
    }

    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<?> searchShipment(@RequestParam String q) {
        Shipment shipment = shipmentService.getShipmentByAnyId(q.trim());
        
        if (shipment != null) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", shipment.getId());
            response.put("customsbroker", shipment.getCustomsbroker());
            response.put("brokerPhone", shipment.getBrokerPhone());
            response.put("containerNumber", shipment.getContainerNumber());
            response.put("statisticalCode", shipment.getStatisticalCode());
            
            // 🌟 بناء مصفوفة مستندات مخصصة تضمن وصول الـ ID والـ fileUrl بوضوح تام للـ JS
            java.util.List<java.util.Map<String, Object>> docsList = new java.util.ArrayList<>();
            
            if (shipment.getDocuments() != null) {
                for (Document doc : shipment.getDocuments()) {
                    if (doc.getType() != null) {
                        java.util.Map<String, Object> dMap = new java.util.HashMap<>();
                        
                        // تمرير الـ ID الخاص بالمستند كـ Long بشكل صريح
                        dMap.put("id", doc.getId()); 
                        
                        // تمرير الرابط الخاص بالمستند
                        dMap.put("fileUrl", doc.getFileUrl() != null ? doc.getFileUrl() : "");
                        
                        // تمرير نوع المستند (Enum اسم كامل) ليقوم الجافا سكريبت الحالي بـ ترجمته
                        dMap.put("type", doc.getType().toString());
                        
                        docsList.add(dMap);
                    }
                }
            }
            
            // استبدال قائمة المستندات الأصلية بالقائمة المخصصة الصريحة
            response.put("documents", docsList); 
            
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }
    

    
    
    
    
    
 // ==========================================
 // 🗑️ دالة حذف مستند فردي من الأرشيف أثناء التعديل
 // ==========================================
    @DeleteMapping("/api/documents/{docId}")
    @ResponseBody
    public ResponseEntity<?> deleteDocumentFromArchive(@PathVariable Long docId) {
        try {
            // 1. جلب الشحنة المرتبطة بهذا المستند أولاً لضمان تحديث الـ Cache في الـ Hibernate
            // نقوم بالبحث في كل الشحنات للتأكد من وجود المستند
            java.util.Optional<Shipment> shipmentOpt = shipmentRepository.findAll().stream()
                    .filter(s -> s.getDocuments() != null && s.getDocuments().stream().anyMatch(d -> d.getId().equals(docId)))
                    .findFirst();

            if (shipmentOpt.isPresent()) {
                Shipment shipment = shipmentOpt.get();
                
                // 2. إزالة المستند من قائمة المستندات الخاصة بالشحنة في الجافا (لتفعيل الـ orphanRemoval = true)
                shipment.getDocuments().removeIf(doc -> doc.getId().equals(docId));
                
                // 3. حفظ الشحنة بعد التعديل، وبسبب وجود cascade و orphanRemoval سيقوم الـ JPA بحذف المستند تلقائياً من الـ DB
                shipmentRepository.save(shipment);

                return ResponseEntity.ok(java.util.Map.of("success", true, "message", "تم حذف المستند بنجاح"));
            } else {
                return ResponseEntity.status(404).body(java.util.Map.of("success", false, "message", "المستند غير موجود أو تم حذفه مسبقاً"));
            }
        } catch (Exception e) {
            e.printStackTrace(); // لطباعة الخطأ في الـ Console لمعرفته إن حدث
            return ResponseEntity.status(500).body(java.util.Map.of("success", false, "message", "حدث خطأ أثناء الحذف: " + e.getMessage()));
        }
    }
    
    //
//    @GetMapping("/api/search")
//    @ResponseBody
//    public ResponseEntity<?> searchShipment(@RequestParam String q) {
//        Shipment shipment = shipmentService.getShipmentByAnyId(q.trim());
//        
//        if (shipment != null) {
//            java.util.Map<String, Object> response = new java.util.HashMap<>();
//            response.put("id", shipment.getId());
//            response.put("status", shipment.getStatus().toString()); 
//            response.put("customsbroker", shipment.getCustomsbroker());
//            response.put("brokerPhone", shipment.getBrokerPhone());
//            response.put("containerNumber", shipment.getContainerNumber());
//            response.put("statisticalCode", shipment.getStatisticalCode());
//            
//            // 🔥 إضافة المستندات عشان الـ JS يقدر يعرض الصور القديمة
//            response.put("documents", shipment.getDocuments()); 
//            
//            return ResponseEntity.ok(response);
//        }
//        return ResponseEntity.notFound().build();
//    }
//    

}