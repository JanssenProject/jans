SCIM-Client
===========

A Java client for consumption of Jans-SCIM endpoints.

# How to start

If you use maven, add the following to your pom.xml:

```
<properties>
	<scim.client.version>1.0.0-SNAPSHOT</scim.client.version>
</properties>
...
<repositories>
  <repository>
    <id>jans</id>
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

Alternatively you can grab the library jar from [here](https://maven.jans.io/maven/io/jans/jans-scim-client/) and manually add all other dependant jars. 

## Sample code

Paste the following in your IDE or favorite code editor: 

```
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.factory.ScimClientFactory;
import io.jans.scim2.client.rest.ClientSideService;

import javax.ws.rs.core.Response;
import java.util.List;

public class TestScimClient {

    private String domainURL = "https://<host-name>/scim/restv1";
    private String OIDCMetadataUrl = "https://<host-name>/.well-known/openid-configuration";

    private void simpleSearch() throws Exception {

        ClientSideService client = ScimClientFactory.getTestClient(domainURL, OIDCMetadataUrl);
        String filter = "userName eq \"admin\"";

        Response response = client.searchUsers(filter, 1, 1, null, null, null, null);
        List<BaseScimResource> resources = response.readEntity(ListResponse.class).getResources();

        System.out.println("Length of results list is: " + resources.size());
        UserResource admin = (UserResource) resources.get(0);
        System.out.println("First user in the list is: " + admin.getDisplayName());
        
        client.close();

    }

}
```

Important:

- In the code, set your hostname appropriately in the `<host-name>` placeholder.

- Ensure both `jans-auth-server` and `jans-scim` are running. Ensure test mode is on (more on this [below](#protection-modes)).

- Add the SSL certificate to the `cacerts` keystore of your local Java installation. You can quickly obtain the cert by visiting `https://<host-name>/.well-known/openid-configuration` in a browser (click on the lock icon) and then export it to a file. A tool like [KeyStore Explorer](http://keystore-explorer.org) is handy to import the certificate to the Java keystore.

What's happening in the code?

- The sample code is intended to perform a search using a filter expression (add a `main` method please). This query returns a user entry that corresponds to the administrator user. Admin's displayname is printed in the console.

- The `ClientSideService` interface is a "mashup" of several interfaces and gives access to a rich number of methods that allows to do all CRUD developers may need. Other methods in `ScimClientFactory` class allow to supply a more specific interface class and thus get an object adhering to that interface.

- The variant of `getTestClient` used above will attempt to register a client in `jans-auth-server`. For this purpose ensure your installation has dynamic registration of clients enabled (this is the default setting in a fresh installation though). To avoid creating new clients everytime this method is called, or when dynamic registration is disabled, you can use `getTestClient(String, String, String, String)` where you can pass the ID and secret of an existing client for the third and fourth parameters respectively.

- It is recommended to call `close` whenever you know there will not be any other request associated to the client referenced obtained.

## Protection modes

TODO