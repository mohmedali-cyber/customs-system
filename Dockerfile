# استخدام نسخة Java المستقرة
FROM eclipse-temurin:17-jdk-alpine

# نسخ ملف المشروع النهائي لداخل السيرفر
COPY target/*.jar app.jar

# الأمر اللي بيشغل المنظومة
ENTRYPOINT ["java","-jar","/app.jar"]