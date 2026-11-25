FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
ARG MODULE=gateway
RUN mvn -B -pl ${MODULE} -am package -DskipTests
RUN cp ${MODULE}/target/*-1.0.0.jar /app.jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar /app/app.jar
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
