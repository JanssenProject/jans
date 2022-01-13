[![Codacy Badge](https://app.codacy.com/project/badge/Grade/441f0f8d556f4e7f98f88ff5accd26a1)](https://www.codacy.com/gh/JanssenProject/jans-config-api/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=JanssenProject/jans-config-api&amp;utm_campaign=Badge_Grade)
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
Learn more in the [jans-config-api documentation](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans-config-api/master/docs/jans-config-api-swagger.yaml).
