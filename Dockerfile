FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25 AS base

ENV TZ="Europe/Oslo"
ENV JAVA_OPTS='-XX:MaxRAMPercentage=90'

WORKDIR /app

CMD ["-jar", "app.jar"]

ARG BYGD_PA_NY='2026-01-30T10:18:46'

FROM base AS spock

COPY spock/build/libs/*.jar /app/

FROM base AS opprydding-dev

COPY opprydding-dev/build/libs/*.jar /app/
