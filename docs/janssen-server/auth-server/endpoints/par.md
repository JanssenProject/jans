---
tags:
- administration
- auth-server
- par
- pushed authorization requests
- endpoint
---

# Pushed Authorization Request (PAR) Endpoint

PAR endpoint is used by client to send authorization request directly to the Janssen Server without using the usual
redirection mechanism via user agent. When PAR endpoint receives a valid request, it responds with a request URI. 
The request URI is a reference created and stored by Janssen Server. It is a reference to  authorization request and 
the metadata sent with it by the client. Client can send this request uri to the Janssen Server in a authorization
request using user agent redirect mechanism. There are multiple benefits of using this flow which are described along 
with other details in [PAR specification](https://datatracker.ietf.org/doc/html/rfc9126). Janssen Server PAR implementation 
conforms to PAR specification.

URL to access PAR endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`pushed_authorization_request_endpoint` claim in the response specifies the URL for the PAR endpoint. By default, PAR endpoint looks
like below:

```
https://jans-dynamic-mysql/jans-auth/restv1/par
```

In response to a valid request, the PAR endpoint returns `request_uri` in response similar to below:

```
 HTTP/1.1 201 Created
 Content-Type: application/json
 Cache-Control: no-cache, no-store

 {
  "request_uri":
    "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c",
  "expires_in": 60
 }
```

Since PAR endpoint is a protected resource. The client has to authenticate itself to the endpoint. Authentication 
methods used are same as the once used for client authentication at [token endpoint](./token.md#client-authentication). 

More information about request and response of the PAR endpoint can be found in
the OpenAPI specification of 
[jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/Authorization/post_par).

## Disabling The Endpoint Using Feature Flag

`PAR` endpoint can be enabled or disabled using [PAR feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#par).
Use [Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) or [Janssen command-line interface](../../config-guide/config-tools/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `PAR` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Configuration Properties

### Global Janssen Server configuration properties

PAR endpoint can be further configured using Janssen Server configuration properties listed below. It can be configured via
[Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) (navigate to `Auth Server`->`Properties`), admin UI or directly in persistence layer.

- [mtlsParEndpoint](../../reference/json/properties/janssenauthserver-properties.md#mtlsparendpoint) - Mutual TLS (mTLS) Pushed Authorization Requests (PAR) endpoint URL
- [parEndpoint](../../reference/json/properties/janssenauthserver-properties.md#parendpoint) - Pushed Authorization Requests (PAR) Endpoint location
- [requirePar](../../reference/json/properties/janssenauthserver-properties.md#requirepar) - Boolean value to indicate whether Pushed Authorisation Request (PAR) endpoint is required
- [requestUriParameterSupported](../../reference/json/properties/janssenauthserver-properties.md#requesturiparametersupported) - Boolean value specifying whether the OP supports use of the request_uri parameter

### Client specific PAR related configuration properties

Some configuration properties are configured on client level to allow more granular configuration which depends on client.

- `par_lifetime` - PAR object lifetime in seconds. If value is not specified then defaults to `600` value.
- `require_par`  - specified whether all authorization requests made by this client to Authorization Endpoint must be PAR. If `true` and authorization request is not PAR then error is returned back by Authorization Server. 

## PAR Custom script

The ParType interception script has the following method(s):

|Method |Method description|
|:-----|:------|
| `def createPar(self, par, context)`| Used to modify PAR object before it is persisted. `par` is `io.jans.as.persistence.model.Par`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`|
| `def modifyParResponse(self, response, context)`| Used to modify response from `/par` endpoint. 
`response` is `org.json.JSONObject`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`|


## Script Type: Java 


```java

import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.par.ParType;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class Par implements ParType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean createPar(Object par, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        io.jans.as.persistence.model.Par parObject = (io.jans.as.persistence.model.Par) par;
        parObject.getAttributes().setScope("openid profile");

        if ("bad".equalsIgnoreCase(scriptContext.getExecutionContext().getClient().getClientId())) {
            scriptContext.setWebApplicationException(
                    new NoLogWebApplicationException(Response
                            .status(Response.Status.FORBIDDEN)
                            .entity("Forbidden by custom script.")
                            .build()));
        }

        return true;
    }

    @Override
    public boolean modifyParResponse(Object responseAsJsonObject, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        JSONObject json = (JSONObject) responseAsJsonObject;
        json.accumulate("custom_key", "custom_value");
        return true;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized PAR Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized PAR Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed PAR Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}


```



## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).