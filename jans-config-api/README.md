# jans-config-api project

This project uses Weld, the reference implementation of CDI: Contexts and Dependency Injection for the Java EE Platform - a JCP standard for dependency injection and contextual lifecycle management.

If you want to learn more about Weld, please visit its website: https://weld.cdi-spec.org/

## Objective

Jans Config Api endpoints can be used to configure jans-auth-server, which is an open-source OpenID Connect Provider (OP) and UMA Authorization Server (AS)

## Packaging and running the application

The application can be packaged using `./mvnw package`.
It produces the `jans-config-api.war` file in the `server/target` directory.
Be aware that all the dependencies are copied into the `server/target/jans-config-api/WEB-INF/lib` directory.

The application can be deployed on web server like jetty and can be now runnable using `java -jar start.jar -Djans.base=etc/jans`.

## Documentation
Learn more in the [jans-config-api documentation](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-config-api/docs/jans-config-api-swagger-auto.yaml).
