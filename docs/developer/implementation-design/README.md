# Technical Documentation

This documentation explains technical design, architecture and interactions of various Janssen modules.

## Contents:

- [API Reference](#api-reference)
- [Design Consideration and Guidelines](#design-consideration-and-guidelines)
  - [REST API Design](#rest-api-design)
  - [Caching](#caching)
  - [Testing](#testing)
  - [Deployment](#deployment)
  - [Security](#security)
  - [Scalability and Cloud Infrastructure](#scalability-and-cloud-infrastructure)
- [Technical Documentation Guidelines](#technical-documentation-guidelines)

## API Reference

| Service | REST API | Java API |  
| --- | --- | --- |  
| Jans Auth Server | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-auth-server/docs/swagger.yaml)| [Javadoc](https://jenkins.jans.io/javadocs/jans-auth/main/)|  
| Jans Config API | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-config-api/docs/jans-config-api-swagger.yaml)| |  
| Jans Core | | [Javadoc](https://jenkins.jans.io/javadocs/jans-core/main/)|  
| Jans FIDO 2 | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-fido2/docs/jansFido2Swagger.yaml) | |  
| Jans SCIM API | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-scim/server/src/main/resources/jans-scim-openapi.yaml) | [Javadoc](https://jenkins.jans.io/javadocs/jans-scim/main/) |     
  
## Design Consideration and Guidelines

This section outlines high-level design principles, styles and design choices that Janssen project follow. 
### REST API Design
### Caching
### Testing
### Deployment
### Security
### Scalability and Cloud Infrastructure 

## Technical Documentation Guidelines
  
- Detailed technical documentation for each module should be placed under directory with module's name. For example, technical documentation for `jans-mod` should be placed under directory `docs/technical/jans-mod`
- Each module directory should have a README file that follows [Technical Overview Template](./technical-overview-template.md).
- Create directories if required under module directory to further arrange documentation
- Place all the digital assets to support your documentation under [assets](../../assets) following these [guidelines](../../assets/README.md)


