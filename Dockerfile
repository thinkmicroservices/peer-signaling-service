# grab the base image
FROM openjdk:8-jdk-alpine

# install curl
RUN apk add --update \
    curl \
    && rm -rf /var/cache/apk/*

# create a working volume in tmp
VOLUME /tmp

#define the jar dependency files
ARG DEPENDENCY=target/dependency

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
EXPOSE 8443
ENTRYPOINT ["java","-cp","app:app/lib/*","com.thinkmicroservices.ri.spring.peer.signaling.PeerSignalingApplication"]
