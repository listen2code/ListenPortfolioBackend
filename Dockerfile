FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY target/portfolio-0.0.1-SNAPSHOT.war app.war
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.war"]