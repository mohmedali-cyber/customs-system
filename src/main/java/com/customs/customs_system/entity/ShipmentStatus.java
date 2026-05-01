package com.customs.customs_system.entity;

public enum ShipmentStatus {
    PENDING,      // قيد الانتظار (الحالة الافتراضية عند الإدخال)
    UNDER_REVIEW, // تحت التدقيق (عندما يبدأ الموظف في مراجعة الأوراق)
    APPROVED,     // تم الاعتماد (تمت الموافقة النهائية)
    REJECTED,     // مرفوض (وجود خطأ في المستندات)
    COMPLETED     // مكتمل (بعد خروج الشحنة من الميناء)
}
