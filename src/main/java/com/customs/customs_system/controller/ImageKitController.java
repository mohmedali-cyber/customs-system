package com.customs.customs_system.controller;

import org.springframework.web.bind.annotation.*;
import com.customs.customs_system.service.ShipmentService;

import java.util.Map;

@RestController
public class ImageKitController {

    private final ShipmentService shipmentService;

    public ImageKitController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/imagekit/auth")
    public Map<String, String> getAuth() {
        return shipmentService.getImageKitAuth();
    }
}
