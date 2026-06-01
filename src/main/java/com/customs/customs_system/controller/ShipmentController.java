package com.customs.customs_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.customs.customs_system.entity.*;
import com.customs.customs_system.service.AuditLogService;
import com.customs.customs_system.service.ShipmentService;

import com.customs.customs_system.repository.ShipmentRepository;
import com.customs.customs_system.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/shipments")
public class ShipmentController {

	 private final ShipmentService shipmentService;
	    private final UserRepository userRepository; 
	    private final AuditLogService auditLogService;

	    @Autowired
	    private ShipmentRepository shipmentRepository;
	    
	    public ShipmentController(ShipmentService shipmentService, UserRepository userRepository, AuditLogService auditLogService) {
	        this.shipmentService = shipmentService;
	        this.userRepository = userRepository;
	        this.auditLogService = auditLogService;
	    }

	    // =======================================================
	    // 🚪 0. البوابة الجمركية لولوج المشغلين (التعديل أو الإضافة)
	    // =======================================================
	    
	    // عرض واجهة الدخول النظيفة (التي تطلب رقم التسجيل والرمز السري)
	    @GetMapping("/gate")
	    public String showGateLogin() {
	        return "login-gate"; 
	    }

	    // التحقق الآمن لطلب الـ Fetch من واجهة البوابة وتحويله تلقائياً
	    @PostMapping("/track-login")
	    @ResponseBody
	    public ResponseEntity<?> trackLoginAndFetch(@RequestParam("loginId") String loginId, 
	                                                @RequestParam("vCode") String vCode) {
	        try {
	            long id = Long.parseLong(loginId.trim());
	            Shipment shipment = shipmentRepository.findById(id).orElse(null);
	            
	            if (shipment != null && vCode.trim().equals(shipment.getVerificationCode())) {
	                if (shipment.getStatus() == ShipmentStatus.APPROVED) {
	                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                            .body("⚠️ عذراً، هذه الشحنة تم اعتمادها ونقلها للأرشيف النهائي ولا يمكن تعديل مستنداتها.");
	                }
	                
	                // 🟢 هنا السر: نرجع كائن الشحنة كاملاً فوراً في نفس الطلب الناجح!
	                return ResponseEntity.ok().body(shipment);
	            }
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                    .body("⚠️ عذراً، رقم التسجيل الجمركي أو رمز التحقق السري غير صحيح.");
	        } catch (NumberFormatException e) {
	            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("⚠️ صيغة رقم التسجيل غير صالحة.");
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ حدث خطأ في الخادم الجمركي.");
	        }
	    }

	    // ==========================================
	    // 📥 1. العرض الأساسي مع التقسيم (7 سجلات) للموظفين
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
	    // 📄 2. إضافة شحنة جديدة (صفحة التسجيل الجديد للمشغل)
	    // ==========================================
	    @GetMapping("/new")
	    public String showForm(Model model) {
	        model.addAttribute("shipment", new Shipment());
	        return "shipment-form"; // صفحة الإضافة الخاصة بك
	    }

	    // ==========================================
	    // 🚀 3. حفظ الشحنة الجديدة كلياً
	    // ==========================================
	    @PostMapping("/save")
	    @ResponseBody
	    public ResponseEntity<?> saveShipment(@ModelAttribute Shipment shipment,
	                                         @RequestParam(value="BOL_files", required=false) MultipartFile[] bolFiles,
	                                         @RequestParam(value="DO_files", required=false) MultipartFile[] doFiles,
	                                         @RequestParam(value="OC_files", required=false) MultipartFile[] ocFiles,
	                                         @RequestParam(value="INV_files", required=false) MultipartFile[] invFiles,
	                                         @RequestParam(value="PL_files", required=false) MultipartFile[] plFiles,
	                                         @RequestParam(value="AL_files", required=false) MultipartFile[] alFiles,
	                                         @RequestParam(value="CD_files", required=false) MultipartFile[] cdFiles,
	                                         @RequestParam(value="BI_files", required=false) MultipartFile[] biFiles,
	                                         @RequestParam(value="SI_files", required=false) MultipartFile[] siFiles) {

	        // 🛑 1. الفحص المركب الذكي لمنع التكرار البشري الخطأ:
	        if (shipmentService.isShipmentDuplicate(shipment.getStatisticalCode(), shipment.getContainerNumber())) {
	            return ResponseEntity.badRequest().body("❌ خطأ: هذه الحاوية مسجلة بالفعل مسبقاً تحت نفس الرمز الإحصائي لهذه الجهة!");
	        }

	        // 🟢 2. حفظ الشحنة وتوليد المفاتيح السرية داخل الـ Service:
	        Shipment saved = shipmentService.createShipment(shipment);
	        
	        // 🚀 3. انطلاق معالجة الملفات في الخلفية:
	        shipmentService.processDocumentsAsync(
	                saved.getId(), bolFiles, doFiles, ocFiles, invFiles, 
	                plFiles, alFiles, cdFiles, biFiles, siFiles
	        );

	        // 🎁 4. الرد السحري: نرجعوا خريطة (Map) فيها المفاتيح الجديدة للواجهة لتظهر البطاقة الخضراء
	        Map<String, Object> response = new HashMap<>();
	        response.put("success", true);
	        response.put("message", "🎉 تم حفظ الشحنة بنجاح وأرشفتها مبدئياً!");
	        response.put("shipmentId", saved.getId()); // رقم الدخول الجمركي
	        response.put("verificationCode", saved.getVerificationCode()); // الرمز السري

	        return ResponseEntity.ok(response);
	    }
	    
	    // ==========================================
	    // ✅ 4. اعتماد الشحنة وتسجيل الإجراء للموظف
	    // ==========================================
	    @GetMapping("/approve/{id}")
	    public String approve(@PathVariable Long id) {
	        Shipment shipment = shipmentService.getShipmentById(id);
	        shipmentService.approveAndArchive(id);

	        try {
	            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
	            String username = (auth != null) ? auth.getName() : "مسؤول نظام";
	            
	            auditLogService.saveLog(
	                shipment.getContainerNumber(), 
	                shipment.getStatisticalCode(), 
	                username, 
	                "اعتماد وقبول", 
	                "تمت الموافقة على مستندات الشحنة ونقلها إلى الأرشيف المعتمد بنجاح."
	            );
	        } catch (Exception e) {
	            e.printStackTrace(); 
	        }

	        return "redirect:/shipments?status=APPROVED";
	    }

	    // ==========================================
	    // ❌ 5. رفض الشحنة وتسجيل الإجراء مع السبب للموظف
	    // ==========================================
	    @PostMapping("/reject/{id}")
	    public String reject(@PathVariable Long id, @RequestParam("reason") String reason) {
	        Shipment shipment = shipmentService.getShipmentById(id);
	        shipmentService.rejectShipment(id, reason);

	        try {
	            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
	            String username = (auth != null) ? auth.getName() : "مسؤول نظام";
	            
	            auditLogService.saveLog(
	                shipment.getContainerNumber(), 
	                shipment.getStatisticalCode(), 
	                username, 
	                "رفض شحنة", 
	                "تم رفض الشحنة بسبب: " + reason
	            );
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        return "redirect:/shipments?status=REJECTED";
	    }
	    
	    // ==========================================
	    // 📁 6. عرض المستندات والتحقق من صلاحية الموظف الحالي
	    // ==========================================
	    @GetMapping("/view-docs/{id}")
	    public String viewDocuments(@PathVariable Long id, Model model) {
	        Shipment shipment = shipmentService.getShipmentById(id);
	        model.addAttribute("shipment", shipment);

	        try {
	            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
	            String currentUsername = auth.getName();

	            User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

	            if (currentUser != null && currentUser.getRole() != null) {
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
	    // 🗑️ 7. حذف الشحنة من المنظومة (الموظف المسؤول)
	    // ==========================================
	    @GetMapping("/delete/{id}")
	    public String delete(@PathVariable Long id, @RequestParam(name = "status", defaultValue = "PENDING") String status) {
	        shipmentService.deleteShipment(id);
	        return "redirect:/shipments?status=" + status;
	    }

	    // ==========================================
	    // 🔌 8. API جلب الكل
	    // ==========================================
	    @GetMapping("/api")
	    @ResponseBody
	    public List<Shipment> getAll() {
	        return shipmentService.getAllShipments();
	    }

	    // ==========================================
	    // 🛠️ 9. واجهات تعديل وأرشيف المشغلين/المخلصين
	    // ==========================================
	    
	    // عرض صفحة التعديل التلقائية التي تستقبل السلايدر والمستندات الحالية
	    @GetMapping("/edit-list")
	    public String showEditList(Model model) {
	        return "shipment-edit-list"; // عدلها لتطابق اسم الملف الظاهر بالصورة الرابعة تماماً
	    }
	    
	    @GetMapping("/edit/{id}")
	    public String showEditForm(@PathVariable Long id, Model model) {
	        Shipment shipment = shipmentService.getShipmentById(id);
	        if (shipment == null) return "redirect:/shipments/edit-list";
	        
	        model.addAttribute("shipment", shipment);
	        model.addAttribute("existingDocs", shipment.getDocuments());
	        return "shipment-edit"; 
	    }

	    // دالة التحديث ومزامنة الأرشيف الرقمي عند تعديل البيانات أو المرفقات
	    @PostMapping("/update/{id}")
	    @ResponseBody 
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

	        if (shipmentService.isShipmentDuplicateForOther(shipment.getStatisticalCode(), shipment.getContainerNumber(), id)) {
	            return ResponseEntity.badRequest().body("❌ خطأ: هذه الحاوية مسجلة بالفعل مسبقاً تحت نفس الرمز الإحصائي لهذه الجهة!");
	        }

	        shipment.setId(id);
	        Shipment updated = shipmentService.updateShipment(id, shipment);

	        shipmentService.processDocumentsAsync(
	                updated.getId(), bolFiles, doFiles, ocFiles, invFiles, 
	                plFiles, alFiles, cdFiles, biFiles, siFiles
	        );

	        return ResponseEntity.ok(updated);
	    }

	    // دالة البحث والملء عبر المفاتيح المستلمة من الـ Fetch والـ onload الجديد
	    @GetMapping("/api/search")
	    @ResponseBody
	    public ResponseEntity<?> searchShipment(@RequestParam String q) {
	        Shipment shipment = shipmentService.getShipmentByAnyId(q.trim());
	        
	        if (shipment != null) {
	            Map<String, Object> response = new HashMap<>();
	            response.put("id", shipment.getId());
	            response.put("customsbroker", shipment.getCustomsbroker());
	            response.put("brokerPhone", shipment.getBrokerPhone());
	            response.put("containerNumber", shipment.getContainerNumber());
	            response.put("statisticalCode", shipment.getStatisticalCode());
	            response.put("entityName", shipment.getEntityName() != null ? shipment.getEntityName() : "");
	            response.put("brokerNationalId", shipment.getBrokerNationalId() != null ? shipment.getBrokerNationalId() : "");
	            
	            List<Map<String, Object>> docsList = new java.util.ArrayList<>();
	            
	            if (shipment.getDocuments() != null) {
	                for (Document doc : shipment.getDocuments()) {
	                    if (doc.getType() != null) {
	                        Map<String, Object> dMap = new HashMap<>();
	                        dMap.put("id", doc.getId()); 
	                        dMap.put("fileUrl", doc.getFileUrl() != null ? doc.getFileUrl() : "");
	                        dMap.put("type", doc.getType().toString());
	                        docsList.add(dMap);
	                    }
	                }
	            }
	            response.put("documents", docsList); 
	            return ResponseEntity.ok(response);
	        }
	        return ResponseEntity.notFound().build();
	    }
	    
	    // ========================================================
	    // 🗑️ 10. دالة حذف مستند فردي المصححة والمكتملة بالكامل
	    // ========================================================
	    @DeleteMapping("/api/documents/{docId}")
	    @ResponseBody
	    public ResponseEntity<?> deleteDocumentFromArchive(@PathVariable Long docId) {
	        try {
	            // 1. جلب الشحنة المرتبطة بالمستند لتحديث قائمة الهيبيرنيت بداخل الـ Transaction
	            java.util.Optional<Shipment> shipmentOpt = shipmentRepository.findAll().stream()
	                    .filter(s -> s.getDocuments() != null && s.getDocuments().stream().anyMatch(d -> d.getId().equals(docId)))
	                    .findFirst();

	            if (shipmentOpt.isPresent()) {
	                Shipment shipment = shipmentOpt.get();
	                
	                // 2. فك ارتباط المستند من قائمة مستندات الشحنة لكي يختفي من الـ Cache
	                shipment.getDocuments().removeIf(d -> d.getId().equals(docId));
	                shipmentRepository.save(shipment);
	                
	                // 3. استدعاء السيرفس لحذفه نهائياً من قاعدة البيانات والـ Cloud (إذا كنت مبرمج الـ Cloud Delete بداخلها)
	                shipmentService.deleteDocumentById(docId); 
	                
	                return ResponseEntity.ok().body(Map.of("success", true, "message", "🗑️ تم إقصاء المستند من الأرشيف السحابي بنجاح."));
	            }
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("⚠️ عذراً، لم يتم العثور على المستند المطلوب في قاعدة البيانات.");
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ حدث خطأ برمي أثناء محاولة مسح المستند.");
	        }
	    }
	}