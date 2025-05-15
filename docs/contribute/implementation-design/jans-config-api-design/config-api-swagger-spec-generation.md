---
tags:
  - developer
  - config-api
---

# Config API Swagger Spec generation at build time

**What Is OpenAPI?** OpenAPI Specification (formerly Swagger Specification) 
is an API description format for REST APIs. An OpenAPI file allows you to 
describe your entire API, including:

* Available endpoints (/users) and operations on each endpoint (GET /users, POST /users)
* Operation parameters Input and output for each operation
* Authentication methods
* Contact information, license, terms of use, and other information. API specifications 
can be written in YAML or JSON. The format is easy to learn and readable to both humans 
and machines. The complete OpenAPI Specification can be found on GitHub: 
[OpenAPI 3.0 Specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md)


## OpenAPI specification can be generated at;

* Runtime
* Build time

This document is related to build time generation.

## Quick start

OpenAPI Specification is generated when pre-processing the API based on the 
meta-data added against the various resources, methods, and controllers. 
This is also called the code first approach.

### Project POM changes

* Swagger-core is an open-source Java implementation of Swagger/OpenAPI. Its 
java-related libraries can be used for creating, consuming, and working with 
OpenAPI definitions. It contains a set of modules that enables integration with 
a JAX-RS to produce OpenAPI definitions. We use its set of annotations to decorate 
code with meta-data information.

Add its dependency to the project POM.

```xml
  <dependencies>
  ...
  	<dependency>
  		<groupId>io.swagger.core.v3</groupId>
  		<artifactId>swagger-core-jakarta</artifactId>
  		<version>2.2.4</version>
  	</dependency>
  </dependencies>
```

* Swagger Maven Plugin This plugin is intended to use the Swagger Core library to generate 
OpenAPI documentation from a JAX-RS based REST service. If you want to learn more about 
the plugin and available configuration options, please visit its website: https://github.com/openapi-tools/swagger-maven-plugin 

To have Swagger generate the OpenAPI specifications as part of the build, add the plugin to the POM.

```xml
<build>
 <plugins>
   ...
   <plugin>
  	<groupId>io.swagger.core.v3</groupId>
  	<artifactId>swagger-maven-plugin-jakarta</artifactId>
  	<version>2.2.2</version>
  	<executions>
  		<execution>
  			<configuration>
  				<outputFormat>YAML</outputFormat>
  				<outputPath>${project.parent.basedir}/docs</outputPath>
  				<prettyPrint>true</prettyPrint>
  				<filterClass>io.jans.configapi.filters.SpecFilter</filterClass>
  			</configuration>
  			<phase>compile</phase>
  			<goals>
  				<goal>resolve</goal>
  			</goals>
  		</execution>
  	</executions>
  	<dependencies>
  		<dependency>
  			<groupId>io.swagger.core.v3</groupId>
  			<artifactId>swagger-models-jakarta</artifactId>
  			<version>2.2.2</version>
  		</dependency>
  	</dependencies>
  </plugin>
   ...
 </plugins>
</build>
```

Checkout a sample POM [here](https://github.com/JanssenProject/jans/blob/main/jans-config-api/pom.xml).

### Add meta-data in code

* `Defining general OpenAPI information` @OpenAPIDefinition annotation is used to 
populate OpenAPI object fields like - info, tags, servers, security, and externalDocs. 
If more than one class is annotated with OpenAPIDefinition, with the same fields 
defined, behaviour is inconsistent. Use this is your Application class that extends 
javax.ws.rs.core.Application.

Sample code snippet

```java
import ...
...
@OpenAPIDefinition(info = @Info(title = "Jans Config API", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "xxx@gluu.org"),

        license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

        tags = { @Tag(name = "Attribute"), @Tag(name = "Default Authentication Method"),@Tag(name = "Cache Configuration"),
                @Tag(name = "Cache Configuration – Memcached"), @Tag(name = "Cache Configuration – Redis"),
                @Tag(name = "Cache Configuration – in-Memory"), @Tag(name = "Cache Configuration – Native-Persistence"),
                @Tag(name = "Configuration – Properties"),
                @Tag(name = "Configuration – SMTP"), @Tag(name = "Configuration – Logging"),
                @Tag(name = "Configuration – JWK - JSON Web Key (JWK)"), @Tag(name = "Custom Scripts"),
                @Tag(name = "Database - LDAP configuration"),
                @Tag(name = "OAuth - OpenID Connect - Clients"), @Tag(name = "OAuth - UMA Resources"),
                @Tag(name = "OAuth - Scopes"), @Tag(name = "Configuration – Agama Flow"),
                @Tag(name = "Statistics - User"), @Tag(name = "Health - Check"), @Tag(name = "Server Stats"),
                @Tag(name = "Auth - Session Management"),
                @Tag(name = "Organization Configuration"),
                @Tag(name = "Auth Server Health - Check") },

        servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
        @OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/properties.readonly", description = "View Auth Server properties related information"),
        @OAuthScope(name = "https://jans.io/oauth/jans-auth-server/config/properties.write", description = "Manage Auth Server properties related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/attributes.readonly", description = "View attribute related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/attributes.write", description = "Manage attribute related information"),
        @OAuthScope(name = "https://jans.io/oauth/config/attributes.delete", description = "Delete attribute related information"),
        ...
       }

)))
public class ApiApplication extends Application {
   ...
 
}
```


Checkout sample code [here](https://github.com/JanssenProject/jans/blob/main/jans-config-api/server/src/main/java/io/jans/configapi/rest/ApiApplication.java#L31)


Sample code snippet

```java
import ...
...
package ...;
...

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

...

@Path(ApiConstants.ATTRIBUTES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttributesResource ... {

    ...

    @Operation(summary = "Gets a list of Gluu attributes.", description = "Gets a list of Gluu attributes.", operationId = "get-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response example" , value = "example/attribute/attribute-get-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS })
    public Response getAttributes(
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) 
			{

        ...
        return Response.ok(...).build();
    }

   ...

    @Operation(summary = "Adds a new attribute", description = "Adds a new attribute", operationId = "post-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class) , examples = @ExampleObject(name = "Request example" , value = "example/attribute/attribute.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class) , examples = @ExampleObject(name = "Response example" , value = "example/attribute/attribute.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS })
    public Response createAttribute(@Valid GluuAttribute attribute) {
        log.debug(" GluuAttribute details to add - attribute:{}", attribute);
        checkNotNull(attribute.getName(), AttributeNames.NAME);
        checkNotNull(attribute.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkResourceNotNull(attribute.getDataType(), AttributeNames.DATA_TYPE);
        String inum = attributeService.generateInumForNewAttribute();
        attribute.setInum(inum);
        attribute.setDn(attributeService.getDnForAttribute(inum));
        attributeService.addAttribute(attribute);
        GluuAttribute result = attributeService.getAttributeByInum(inum);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

   ...
    }
```

Checkout a sample code [here](https://github.com/JanssenProject/jans/blob/main/jans-config-api/server/src/main/java/io/jans/configapi/rest/resource/auth/AttributesResource.java).


### Generate code with Maven

The Maven compile command can be used to generate the OpenAPI Swagger Specification. 
Specification should be generated in `outputFormat` and `outputPath` 
as specified in the `swagger-maven-plugin-...`


```bash
mvn compile
```

### Sample OpenAPI Swagger specification.

Checkout a sample of an autogenerated spec [here](https://github.com/JanssenProject/jans/blob/main/jans-config-api/docs/jans-config-api-swagger-auto.yaml).


