package com.customs.customs_system.controller;

import com.customs.customs_system.entity.Document;
import com.customs.customs_system.entity.DocumentType;
import com.customs.customs_system.entity.Shipment;
import com.customs.customs_system.repository.DocumentRepository;
import com.customs.customs_system.service.ShipmentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final ShipmentService shipmentService;

    public DocumentController(DocumentRepository documentRepository,
                              ShipmentService shipmentService) {
        this.documentRepository = documentRepository;
        this.shipmentService = shipmentService;
    }

    @PostMapping("/add")
    public Document addDocument(
            @RequestParam Long shipmentId,
            @RequestParam String fileUrl,
            @RequestParam DocumentType type
    ) {

        Shipment shipment = shipmentService.getShipmentById(shipmentId);

        Document doc = new Document();
        doc.setShipment(shipment);
        doc.setFileUrl(fileUrl);
        doc.setType(type);

        return documentRepository.save(doc);
    }
}
