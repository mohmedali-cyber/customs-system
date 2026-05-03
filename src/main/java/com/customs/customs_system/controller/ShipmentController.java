package com.customs.customs_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.customs.customs_system.entity.*;
import com.customs.customs_system.service.ShipmentService;
import com.example.customs_systemm.entity.Shipment;
import com.example.customs_systemm.entity.ShipmentStatus;
import com.customs.customs_system.repository.ShipmentRepository;
import java.util.List;

@Controller
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;
    
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
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
    @GetMapping("/view-docs/{id}")
    public String viewDocuments(@PathVariable Long id, Model model) {
        Shipment shipment = shipmentService.getShipmentById(id);
        model.addAttribute("shipment", shipment);
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
}