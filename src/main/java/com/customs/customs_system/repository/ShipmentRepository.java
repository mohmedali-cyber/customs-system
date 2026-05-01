package com.customs.customs_system.repository;

import com.customs.customs_system.entity.Shipment;
import com.customs.customs_system.entity.ShipmentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    // ⚡ 1. تحميل خفيف (Basic) يدعم التقسيم
    @Query("SELECT s FROM Shipment s")
    Page<Shipment> findAllBasic(Pageable pageable);

    // 📦 2. تحميل شحنة واحدة مع المستندات (تستخدم عند عرض التفاصيل فقط)
    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.documents WHERE s.id = :id")
    Shipment findByIdWithDocuments(@Param("id") Long id);

    // 🔄 3. جلب الشحنات حسب الحالة مع التقسيم
    @Query("SELECT s FROM Shipment s WHERE s.status = :status")
    Page<Shipment> findByStatusBasic(@Param("status") ShipmentStatus status, Pageable pageable);

    // 🔍 4. البحث المفلتر داخل الحالة مع التقسيم
    @Query("SELECT s FROM Shipment s WHERE s.status = :status AND (" +
           "LOWER(s.containerNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.customsbroker) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.statisticalCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Shipment> searchByStatus(@Param("keyword") String keyword, 
                                  @Param("status") ShipmentStatus status, 
                                  Pageable pageable);

    // ✅ 5. جلب الأرشيف مع التقسيم
    @Query("SELECT s FROM Shipment s WHERE s.status IN " +
           "(com.customs.customs_system.entity.ShipmentStatus.APPROVED, " +
           "com.customs.customs_system.entity.ShipmentStatus.COMPLETED)")
    Page<Shipment> findArchivedBasic(Pageable pageable);

    // 📝 6. تحديث الحالة مباشرة (Performance Optimization)
    @Modifying
    @Query("UPDATE Shipment s SET s.status = :status WHERE s.id = :id")
    void updateShipmentStatus(@Param("id") Long id, @Param("status") ShipmentStatus status);

    // 📊 7. دوال الإحصائيات للـ Dashboard (الحل لمشكلة الخط الأحمر)
    
    // عد الشحنات حسب حالة معينة (مثلاً PENDING)
    long countByStatus(ShipmentStatus status);

    // عد شحنات الأرشيف (APPROVED + COMPLETED)
    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.status IN " +
           "(com.customs.customs_system.entity.ShipmentStatus.APPROVED, " +
           "com.customs.customs_system.entity.ShipmentStatus.COMPLETED)")
    long countArchived();

    @Query("SELECT COUNT(s) FROM Shipment s")
    long countAllShipments();

    @Query("SELECT s FROM Shipment s WHERE s.id = :id")
    Shipment findBasicById(@Param("id") Long id);








    @Modifying
    @Query("DELETE FROM Shipment s WHERE s.id = :id")
    void deleteShipmentById(@Param("id") Long id);

}