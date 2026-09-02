FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system spring && \
    useradd --system --gid spring --create-home spring && \
    mkdir -p /app/logs && \
    mkdir -p /var/log/identity-service && \
    chown -R spring:spring /app/logs /var/log/identity-service

COPY build/libs/app.jar app.jar

USER spring

EXPOSE 8080
EXPOSE 9091

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
