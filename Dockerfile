FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY project/ ./project/
COPY build.sbt ./
COPY src/ ./src/
RUN sbt assembly

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/scala-3.3.7/*-assembly-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]