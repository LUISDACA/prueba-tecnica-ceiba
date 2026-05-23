# ============================================
# Etapa 1: build con Maven + JDK 21
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia primero los archivos de Maven para aprovechar la caché de capas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Ahora copia el código fuente y compila
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ============================================
# Etapa 2: runtime con JRE 21 (imagen mínima)
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia solo el JAR resultante de la etapa de build
COPY --from=builder /app/target/*.jar app.jar

# Usuario no-root para mayor seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Puerto expuesto (informativo; Azure inyecta PORT en runtime)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
