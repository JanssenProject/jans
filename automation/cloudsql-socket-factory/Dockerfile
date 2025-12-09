FROM maven:3.9-eclipse-temurin-17 AS builder

ARG CLOUDSQL_SOCKET_FACTORY_VERSION=1.27.0

RUN apt-get update && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/* \
    && git clone --depth 1 --branch v${CLOUDSQL_SOCKET_FACTORY_VERSION} \
       https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory.git /build

WORKDIR /build

RUN mvn -P jar-with-dependencies clean package -DskipTests -q \
    && mkdir -p /cloudsql \
    && cp jdbc/mysql-j-8/target/mysql-socket-factory-connector-j-8-*-jar-with-dependencies.jar /cloudsql/ \
    && cp jdbc/postgres/target/postgres-socket-factory-*-jar-with-dependencies.jar /cloudsql/

FROM scratch

LABEL org.opencontainers.image.title="Cloud SQL JDBC Socket Factory" \
      org.opencontainers.image.description="Pre-built Cloud SQL JDBC Socket Factory fat JARs for MySQL and PostgreSQL" \
      org.opencontainers.image.source="https://github.com/JanssenProject/jans" \
      org.opencontainers.image.vendor="Janssen Project"

COPY --from=builder /cloudsql/ /cloudsql/
