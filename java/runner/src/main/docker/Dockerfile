FROM artifactory.epam.com:6190/thirdparty.docker/alpine-c-jdk:11.0.13_p8-r0-alpine3.15.4

COPY dc-runner /opt/deltix/timebase/dc-runner

ENV JAVA_OPTS=-verbose:gc
WORKDIR /opt/deltix/timebase/dc-runner

EXPOSE 8055
ENTRYPOINT ["/bin/sh", "-c", "java ${JAVA_OPTS} -cp ./lib/* -jar dc-runer*.jar"]