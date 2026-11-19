# ---- Build stage: compile, test and package the WAR ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package

# ---- Runtime stage: Tomcat 10.1 (Jakarta Servlet 6) ----
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
# Deploy as ROOT so the app is served at http://localhost:8080/
COPY --from=build /app/target/grocery-inventory.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
