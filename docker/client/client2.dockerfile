# Alpine Linux with OpenJDK JRE
FROM openjdk:8-jre-alpine
# copy WAR into image
COPY client-1.0-SNAPSHOT.jar ./client.jar
COPY lib ./libs
# run application with this command line
CMD ["/usr/bin/java", "-jar", "/client.jar", "replica2", "12350"]