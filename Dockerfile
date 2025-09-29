# Build stage
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests package

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /opt/app
COPY --from=build /workspace/target/quarkus-app/lib/ /opt/app/lib/
COPY --from=build /workspace/target/quarkus-app/*.jar /opt/app/
COPY --from=build /workspace/target/quarkus-app/app/ /opt/app/app/
COPY --from=build /workspace/target/quarkus-app/quarkus/ /opt/app/quarkus/
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java","-jar","/opt/app/quarkus-run.jar"]
