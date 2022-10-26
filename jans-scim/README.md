# Jans-SCIM

A component of the Janssen Project that implements a standards-compliant SCIM service.

SCIM is a specification designed to reduce the complexity of user management 
operations by providing a common user schema and the patterns for exchanging such 
schema using HTTP in a platform-neutral fashion. The aim of SCIM is achieving 
interoperability, security, and scalability in the context of identity management.

For your reference, the current version of the standard is governed by the following 
documents: [RFC 7642](https://tools.ietf.org/html/rfc7642), [RFC 7643](https://tools.ietf.org/html/rfc7643), and [RFC 7644](https://tools.ietf.org/html/rfc7644).

**Jans-SCIM** can be seen merely as a **REST API** with endpoints exposing 
**CRUD** functionality (create, read, update and delete) for managing identity resources such as users, groups, and fido devices.

## About endpoints protection

In Jans-SCIM endpoints are protected with bearer OAuth 2.0 tokens; this is a safe, standardized approach for controlling access to resources.

Depending on the scopes associated to a token, you will be granted (or denied) access to perform certain operations. The following lists the available scopes:

|Scope|Actions allowed|
|-|-|
|`https://jans.io/scim/users.read`|Query user resources|
|`https://jans.io/scim/users.write`|Modify user resources|
|`https://jans.io/scim/groups.read`|Query group resources|
|`https://jans.io/scim/groups.write`|Modify group resources|
|`https://jans.io/scim/fido.read`|Query fido resources|
|`https://jans.io/scim/fido.write`|Modify fido resources|
|`https://jans.io/scim/fido2.read`|Query fido 2 resources|
|`https://jans.io/scim/fido2.write`|Modify fido 2 resources|
|`https://jans.io/scim/all-resources.search`|Access the root .search endpoint| 
|`https://jans.io/scim/bulk`|Send requests to the bulk endpoint|

In order to facilitate the process of getting an access token, your Janssen installation already bundles an OAuth client named "SCIM client" with support for all the scopes above. This client uses the `client_credentials` grant type and `client_secret_basic` mechanism to authenticate to the token endpoint.

To exercise a finer grained control over access, you may register multiple clients with limited scopes and deliver the client credentials as needed to your developers. 

### Example of usage

In the following example we leverage the OAuth SCIM Client to get a token and issue a call to an endpoint. 

### Get client credentials

Log into your Janssen machine and run the commands provided:

- Get the client id: `cat /root/.config/jans-cli.ini | grep 'scim_client_id'`
- Get the encrypted client secret: `cat /root/.config/jans-cli.ini | grep 'scim_client_secret_enc'`
- Decrypt the secret: `/opt/jans/bin/encode.py -D ENCRYPTED-SECRET-HERE`

### Get a token

Request a token with the scopes necessary to perform the intended operations. Use a white space to separate scopes. Here is how (line breaks added for readability): 

```
curl -u 'CLIENT_ID:DECRYPTED_CLIENT_SECRET' -k -d grant_type=client_credentials -d 
scope='https://jans.io/scim/users.read https://jans.io/scim/users.write' 
https://your-jans-server/jans-auth/restv1/token
```

Grab the "access_token" from the response. Ideally this and the commands that follow should be issued from a machine other than your Jans server. 

### Issue a request to the service

Using the token, call your SCIM operations (line breaks added for readability):

```
curl -k -G -H 'Authorization: Bearer ACCESS_TOKEN' --data-urlencode 'filter=displayName co "Admin"' 
https://your-jans-server/jans-scim/restv1/v2/Users
```

The output should show valid SCIM (JSON) output. Account the access token is short-lived: once it expires you will get a status response of 401 and need to re-request the token as in the previous step.

## API documentation and clients

The Jans-SCIM API is documented using [Open API](https://www.openapis.org) version 3.0 as well as the swagger 2.0 specification. Find the yaml documents [here](https://github.com/JanssenProject/jans-scim/tree/master/server/src/main/resources). You can quickly generate client stubs in a variety of languages and frameworks with [Swagger code generator](https://swagger.io/tools/swagger-codegen).

### Java client

There is a Java-based [client](https://github.com/JanssenProject/jans-scim/tree/master/client) ready to use. This client facilitates service consumption and abstracts out the complexities of access. It supports OAuth clients that use `client_secret_basic`, `client_secret_post` and `private_key_jwt` methods to authenticate to the token endpoint. To register a client, you can use the [jans-cli](https://github.com/JanssenProject/jans-cli) tool.

Usage examples follow:

#### Prerequisites

- Import the SSL certificate of your Jans server to the `cacerts` keystore of your local Java installation. A utility called [KeyStore Explorer](https://keystore-explorer.org/) makes this task super easy. By default, Janssen uses a self-signed certificate that can be found at `/etc/certs/httpd.crt`.

- Maven build tool installed

#### Add dependency

Add the artifact `jans-scim-client` to your project pom, eg:

```
<properties>
	<scim.client.version>1.0.0-SNAPSHOT</scim.client.version>
</properties>
...
<repositories>
  <repository>
    <id>Jans</id>
    <name>Janssen repository</name>
    <url>https://maven.jans.io/maven</url>
  </repository>
</repositories>
...
<dependency>
  <groupId>io.jans</groupId>
  <artifactId>jans-scim-client</artifactId>
  <version>${scim.client.version}</version>
</dependency>
```

#### Get an instance of SCIM service client:

```
import io.jans.scim2.client.factory.ScimClientFactory;
import io.jans.scim2.client.rest.ClientSideService;

...

ClientSideService client = ScimClientFactory.getClient(
      "https://your-jans-server/jans-scim/restv1"   // Base path of the service 
    , "https://your-jans-server/.well-known/openid-configuration"   // Metadata url of the authorization server 
    , "6a931fba-a55a-42ac-9154-5e44a7dfda77"    // OAuth client ID
    , "FBI_CIA_KGB_MI6"    // Client secret
    );
```

In the code above, it is assumed the client referenced uses `client_secret_basic`. Here you can use the already bundled client, otherwise see ["Where to go next"](#where-to-go-next).

#### Perform an operation

```
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.model.scim2.ListResponse;
import org.gluu.oxtrust.model.scim2.user.UserResource;

import javax.ws.rs.core.Response;
import java.util.List;

...

Logger logger = LogManager.getLogger(getClass());
String filter = "userName eq \"admin\"";

Response response = client.searchUsers(filter, 1, 1, null, null, null, null);
List<BaseScimResource> resources = response.readEntity(ListResponse.class).getResources();

logger.info("Length of results list is: {}", resources.size());
UserResource admin = (UserResource) resources.get(0);
logger.info("First user in the list is: {}", admin.getDisplayName());

client.close();
```

This code performs a search using a filter based on username. It is recommended to call `close` once you know there will not be any other request associated to the client object obtained.

#### Where to go next? 

The `client` instance resembles quite close the SCIM specification, so it is generally easy to map the operations described in the standard versus the Java methods available. It can be useful to have some javadocs at hand though, specifically those from `model` and `client` folders of this repository. You may clone this repo and run `mvn javadoc:javadoc` inside the two directories mentioned.

Note that `ScimClientFactory` provides several methods that allow you to use OAuth clients which employ mechanisms other than the default (`client_secret_basic`) to request tokens. Also, you can make `client` belong to more restrictive interfaces limiting the operations available in your code.
