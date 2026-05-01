package com.customs.customs_system.entity;

public enum DocumentType {
    BILL_OF_LADING,       // بوليصة شحن
    DELIVERY_ORDER,       // أمر تسليم
    CERTIFICATE_OF_ORIGIN,// شهادة منشأ
    INVOICE,              // فاتورة
    PACKING_LIST,         // قائمة تعبئة
    STATISTICAL_CODE,     // الرمز الإحصائي (بيانات)
    AUTHORIZATION_LETTER, // رسالة تخويل
    CUSTOMS_DECLARATION,  // الإقرار الجمركي
    BROKER_ID,            // تعريف مخلص ورقم وطني
    STATISTICAL_IMAGE,    // صورة الرمز الإحصائي
    OTHER                 // أخرى
}