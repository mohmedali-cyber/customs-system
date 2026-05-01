# ===============================
# Dockerfile لمنظومة جمارك بنغازي
# ===============================

# 1️⃣ استخدم نفس الصورة الناجحة من مشروعك السابق
FROM eclipse-temurin:17-jdk-jammy

# 2️⃣ مجلد العمل داخل الحاوية
WORKDIR /app

# 3️⃣ انسخ كل ملفات المشروع داخل الحاوية
COPY . .

# 4️⃣ أعطِ صلاحيات تنفيذ لـ Maven Wrapper (ضروري لنجاح البناء)
RUN chmod +x mvnw

# 5️⃣ بناء المشروع وتخطي الاختبارات لسرعة التنفيذ
RUN ./mvnw clean package -DskipTests

# 6️⃣ تشغيل التطبيق بناءً علىartifactId و version في ملف pom.xml
CMD ["java", "-jar", "target/customs-system-0.0.1-SNAPSHOT.jar"]