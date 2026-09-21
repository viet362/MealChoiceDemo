# =============================================================================
# Stage 1: Build Application with Gradle & JDK 17
# =============================================================================
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Sao chép file cấu hình Gradle để tận dụng Docker layer cache
COPY gradle ./gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# Sao chép mã nguồn và đóng gói JAR (bỏ qua unit tests khi build image)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# =============================================================================
# Stage 2: Runtime Image (Siêu nhẹ với Eclipse Temurin JRE 17 Alpine)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tạo sẵn các thư mục upload file ảnh
RUN mkdir -p uploads/foods uploads/settlements uploads/payouts

# Sao chép file jar đã build từ Stage 1
COPY --from=build /app/build/libs/*.jar app.jar

# Biến môi trường cổng mặc định (tương thích Render, Railway, Fly.io)
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
