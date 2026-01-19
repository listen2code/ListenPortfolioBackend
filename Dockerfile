FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY app.war app.war
ENTRYPOINT ["java","-jar","/app.war"]