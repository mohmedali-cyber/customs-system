package com.customs.customs_system.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.customs.customs_system.entity.Document;


public interface DocumentRepository extends JpaRepository<Document, Long> {
}
