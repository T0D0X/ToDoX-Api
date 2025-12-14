FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY project/ ./project/
COPY build.sbt ./
COPY src/ ./src/               # Эта команда копирует ВСЕ исходники, включая TodoApp.scala
RUN sbt assembly               # Сборка JAR из скопированных исходников

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/scala-3.3.7/*-assembly-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]