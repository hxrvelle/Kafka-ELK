FROM openjdk:17-oracle

WORKDIR /app

COPY target/RxJava-0.0.1-SNAPSHOT.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]