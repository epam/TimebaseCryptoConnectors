FROM amazoncorretto:11-alpine3.17

ENV LANG=C.UTF-8
COPY runner /runner

ENV JAVA_OPTS=-verbose:gc
ENV SPRING_CONFIG_LOCATION=/runner/config/application.yaml

WORKDIR runner

EXPOSE 8055
ENTRYPOINT ["/bin/sh", "-c", "./runner.sh"]
