FROM eclipse-temurin:21-jre
WORKDIR /app
# Копируем собранный JAR-файл
COPY target/scala-3.3.7/*-assembly-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]