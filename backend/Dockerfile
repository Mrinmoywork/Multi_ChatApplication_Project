# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom and download dependencies
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy source and build fat JAR
COPY src ./src
RUN mvn -q -DskipTests package

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the shaded JAR
COPY --from=build /app/target/*.jar app.jar

# Render provides PORT env var
ENV PORT=8080
EXPOSE 8080

# Run the fat JAR
CMD ["java", "-jar", "app.jar"]
