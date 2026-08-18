# Build stage
FROM gradle:8-jdk21-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x gradlew
RUN ./gradlew buildFatJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
RUN apk add --no-cache gcompat
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/fintrack.jar
ENTRYPOINT ["java", "-jar", "/app/fintrack.jar"]
