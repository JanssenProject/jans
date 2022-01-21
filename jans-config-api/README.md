[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=bugs)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=code_smells)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=coverage)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=ncloc)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=alert_status)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=security_rating)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=sqale_index)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=JanssenProject_jans-config-api&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=JanssenProject_jans-config-api)
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
