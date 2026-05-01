package com.customs.customs_system.controller;

import com.customs.customs_system.service.ShipmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private final ShipmentService shipmentService;

    public IndexController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/")
    public String index(Model model) {
        // نستخدم الدوال الجديدة اللي بتعطيك الرقم مباشرة
        // هكذا تختفي الخطوط الحمراء لأن الدوال دي موجودة في الـ Service اللي عدلناه سوياً
        model.addAttribute("pendingCount", shipmentService.getPendingCount());
        model.addAttribute("approvedCount", shipmentService.getApprovedCount());
        
        return "dashboard"; 
    }
}