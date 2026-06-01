package com.customs.customs_system.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.customs.customs_system.entity.Shipment;
import com.customs.customs_system.entity.ShipmentStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @Query("SELECT s FROM Shipment s")
    Page<Shipment> findAllBasic(Pageable pageable);

    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.documents WHERE s.id = :id")
    Shipment findByIdWithDocuments(@Param("id") Long id);

    @Query("SELECT s FROM Shipment s WHERE s.status = :status")
    Page<Shipment> findByStatusBasic(@Param("status") ShipmentStatus status, Pageable pageable);

    @Query("SELECT s FROM Shipment s WHERE s.status = :status AND (" +
           "LOWER(s.containerNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.customsbroker) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.statisticalCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Shipment> searchByStatus(@Param("keyword") String keyword, 
                                  @Param("status") ShipmentStatus status, 
                                  Pageable pageable);

    // ✅ تم تصحيح المسار هنا ليتوافق مع السيرفر ريندر والـ Package الجديد
    @Query("SELECT s FROM Shipment s WHERE s.status IN " +
           "(com.customs.customs_system.entity.ShipmentStatus.APPROVED, " +
           "com.customs.customs_system.entity.ShipmentStatus.COMPLETED)")
    Page<Shipment> findArchivedBasic(Pageable pageable);

    @Modifying
    @Query("UPDATE Shipment s SET s.status = :status WHERE s.id = :id")
    void updateShipmentStatus(@Param("id") Long id, @Param("status") ShipmentStatus status);

    long countByStatus(ShipmentStatus status);

    // ✅ تم تصحيح المسار هنا أيضاً ليتوافق تماماً مع السيرفر ريندر والـ Package الجديد
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

    List<Shipment> findByStatus(ShipmentStatus status);

    // 1. البحث برقم الحاوية (دقيق)
    @Query("SELECT s FROM Shipment s WHERE s.containerNumber = :containerNumber")
    Optional<Shipment> findByContainerNumber(@Param("containerNumber") String containerNumber);

    // 2. البحث بالرمز الإحصائي (دقيق)
    @Query("SELECT s FROM Shipment s WHERE s.statisticalCode = :statisticalCode")
    Optional<Shipment> findByStatisticalCode(@Param("statisticalCode") String statisticalCode);

    // 3. البحث المرن (للقائمة) - يبحث في رقم الحاوية أو المخلص أو الرمز الإحصائي
    @Query("SELECT s FROM Shipment s WHERE " +
           "LOWER(s.containerNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.customsbroker) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.statisticalCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Shipment> searchByKeyword(@Param("keyword") String keyword);

    // 🌟 التعديل المعتمد: إضافة LEFT JOIN FETCH لضمان جلب الصور مع الشحنة أثناء البحث
    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.documents WHERE s.containerNumber = :q OR s.statisticalCode = :q")
    Optional<Shipment> findAllByAnyId(@Param("q") String q);

    // =========================================================================
    // 🔥 🛠️ الدوال الجديدة المضافة للتحقق الذكي والمنع من التكرار 🛠️ 🔥
    // =========================================================================

    // 🔍 أ: التأكد هل الرمز الإحصائي ونفس رقم الحاوية موجودين معاً (عند الإضافة الجديدة)
    boolean existsByStatisticalCodeAndContainerNumber(String statisticalCode, String containerNumber);

    // 🔍 ب: التأكد هل الرمز ونفس الحاوية موجودين لشحنة أخرى غير الحالية (عند التعديل)
    boolean existsByStatisticalCodeAndContainerNumberAndIdNot(String statisticalCode, String containerNumber, Long id);
}