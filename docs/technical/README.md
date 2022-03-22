# Technical Documentation

This documentation explains technical design, architecture and interactions of various Janssen modules.

Contents:

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
  
| Service | REST API | Javadoc |  
| --- | --- | --- |  
| Jans Auth Server | | [client](https://jenkins.jans.io/javadocs/jans-scim/main/client/) [model](https://jenkins.jans.io/javadocs/jans-scim/main/model/) [server](https://jenkins.jans.io/javadocs/jans-auth/main/server/) |  
| Jans Client API | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans-client-api/master/server/src/main/resources/swagger.yaml)| |  
| Jans Config API | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans-config-api/master/docs/jans-config-api-swagger.yaml)| |  
| Jans Core | | [server](https://jenkins.jans.io/javadocs/jans-core/main/io/jans/server/filters/package-summary.html)|  
| Jans FIDO 2 | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans-fido2/master/docs/jansFido2Swagger.yaml) | |  
| Jans SCIM API | [Swagger](https://gluu.org/swagger-ui/?url=https:/raw.githubusercontent.com/JanssenProject/jans-scim/master/server/src/main/resources/jans-scim-openapi.yaml) | [client](https://jenkins.jans.io/javadocs/jans-scim/main/client/) [model](https://jenkins.jans.io/javadocs/jans-scim/main/model/) [server](https://jenkins.jans.io/javadocs/jans-scim/main/server/) |  
  
## Design Consideration and Guidelines
This section outlines high-level design principles, styles and design choices that Janssen project follow. 
### REST API Design
### Caching
### Testing
### Deployment
### Security
### Scalability and Cloud Infrastructure 

## Technical Documentation Guidelines
  
Each module directory will hold detailed technical documentation for that module. It should have README file that follows [Technical Overview Template](./technical-overview-template.md). Also create other directories as required and as mentioned in `Directory Structure` section below. 

### Directory Structure
Create directories within your module's directory as below to support your documentation. 
 - `images` : to hold screenshots and other images (Max size 1MB, preferrably `.png`)
 - `diagrams` : to hold interaction diagrams, class diagrams, sequence diagram. (Preferrably as PDF)
