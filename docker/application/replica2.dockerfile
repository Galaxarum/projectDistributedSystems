# Alpine Linux with OpenJDK JRE
FROM openjdk:8-jre-alpine
# copy WAR into image
COPY application-1.0-SNAPSHOT.jar ./application.jar
COPY lib ./libs
# run application with this command line
CMD ["/usr/bin/java", "-jar", "/application.jar", "3", "leader"]