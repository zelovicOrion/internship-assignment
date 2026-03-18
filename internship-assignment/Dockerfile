FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", \
  "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
  "-jar", "app.jar"]