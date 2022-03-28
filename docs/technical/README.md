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
  - [Storing Supporting Images and Diagrams](#storing-supporting-images-and-diagrams)
  - [File Naming Convensions](#file-naming-convensions )


## API Reference
  
| Service | REST API | Javadoc |  
| --- | --- | --- |  
| Jans Auth Server | | [server](https://jenkins.jans.io/javadocs/jans-auth/main/server/) [model](https://jenkins.jans.io/javadocs/jans-auth/main/model/) [client](https://jenkins.jans.io/javadocs/jans-auth/main/client/)|  
| Jans Client API | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-client-api/server/src/main/resources/swagger.yaml)| |  
| Jans Config API | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-config-api/docs/jans-config-api-swagger.yaml)| |  
| Jans Core | | [server](https://jenkins.jans.io/javadocs/jans-core/main/io/jans/server/filters/package-summary.html)|  
| Jans FIDO 2 | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-fido2/docs/jansFido2Swagger.yaml) | |  
| Jans SCIM API | [Swagger](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/main/jans-scim/server/src/main/resources/jans-scim-openapi.yaml) | [server](https://jenkins.jans.io/javadocs/jans-scim/main/server/) [model](https://jenkins.jans.io/javadocs/jans-scim/main/model/) [client](https://jenkins.jans.io/javadocs/jans-scim/main/client/) |  
  
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


### Storing Supporting Images and Diagrams
Use [assets](../assets) directory to store digital assets like images, diagrams that are used to support documentation for all modules. 
If directory with your module name doesn't already exist under `docs/assets`, please create it under [assets](../assets) directory. 
Under `<module-name>` directory, create directories as below to hold digital assets:
 - `images` : to hold screenshots and other images (Max size 1MB, preferrably `.png`)
 - `diagrams` : to hold interaction diagrams, class diagrams, sequence diagram. (Preferrably as PDF)
For instance, images that support documetation for `jans-core` module should be located at path `jans/docs/assets/jans-core/images`

### File Naming Convensions 
Image/diagram file names should follow the pattern: `image/diagram`-`3 tag words`-`mmddyyyy`.`type`.

For example,
- A screenshot of IDE settings for developer workspace setup can be named `image-eclipse-ide-setting-04052022.png`
- A sequence diagram for user registration flow can be named `diagram-user-registration-sequence-03062022.pdf`
 
