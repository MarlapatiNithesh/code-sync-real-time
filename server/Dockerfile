
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

COPY --from=builder /app/target/code-sync-server-1.0.0.jar ./app.jar

ENV PORT=3000

EXPOSE 3000
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
