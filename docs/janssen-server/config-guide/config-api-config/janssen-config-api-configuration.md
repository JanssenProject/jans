---
tags:
  - administration
  - configuration
  - config-api
---

# Janssen Config-API Configuration

The Janssen Server provides multiple configuration tools to perform these 
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](../config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

##  Using Command Line


In the Janssen Server, you can deploy and customize the Config-API Configuration using the
command line. To get the details of Janssen command line operations relevant to
Aonfig-API Configuration, you can check the operations under `ConfigurationConfigApi` task using the
command below:


```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info ConfigurationConfigApi
```

It will show the details of the available operation-ids for Config-API.

```text title="Sample Output"
/opt/jans/jans-cli/config-cli.py --info ConfigurationConfigApi
Operation ID: get-config-api-properties
  Description: Gets config-api configuration properties.
Operation ID: patch-config-api-properties
  Description: Partially modifies config-api Configuration properties.
  Schema: Array of JsonPatch

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema-sample <schema>, for example /opt/jans/jans-cli/config-cli.py --schema-sample JsonPatch
```

### Get The Current Config-API Configuration

To get the properties of Janssen Config-API Configuration, run the command below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id=get-config-api-properties
```

It will return the result as below:

```json title="Sample Output" linenums="1"
{
  "serviceName": "jans-config-api",
  "configOauthEnabled": true,
  "disableLoggerTimer": false,
  "disableAuditLogger": false,
  "customAttributeValidationEnabled": true,
  "acrValidationEnabled": true,
  "apiApprovedIssuer": [
    "https://example.jans.io"
  ],
  "apiProtectionType": "oauth2",
  "apiClientId": "1800.b62cb614-a09d-4163-a6bc-32e38a51c4d2",
  "apiClientPassword": "CN8ggRUhMkw9K0ocQ+LXbA==",
  "endpointInjectionEnabled": false,
  "authIssuerUrl": "https://example.jans.io",
  "authOpenidConfigurationUrl": "https://example.jans.io/.well-known/openid-configuration",
  "authOpenidIntrospectionUrl": "https://example.jans.io/jans-auth/restv1/introspection",
  "authOpenidTokenUrl": "https://example.jans.io/jans-auth/restv1/token",
  "authOpenidRevokeUrl": "https://example.jans.io/jans-auth/restv1/revoke",
  "exclusiveAuthScopes": [
    "jans_stat",
    "https://jans.io/scim/users.read",
    "https://jans.io/scim/users.write"
  ],
  "corsConfigurationFilters": [
    {
      "filterName": "CorsFilter",
      "corsEnabled": true,
      "corsAllowedOrigins": "*",
      "corsAllowedMethods": "GET,PUT,POST,DELETE,PATCH,HEAD,OPTIONS",
      "corsSupportCredentials": true,
      "corsLoggingEnabled": false,
      "corsPreflightMaxAge": 1800,
      "corsRequestDecorate": true
    }
  ],
  "loggingLevel": "INFO",
  "loggingLayout": "text",
  "disableJdkLogger": true,
  "maxCount": 200,
  "acrExclusionList": [
    "simple_password_auth"
  ],
  "userExclusionAttributes": [
    "userPassword"
  ],
  "userMandatoryAttributes": [
    "mail",
    "displayName",
    "status",
    "userPassword",
    "givenName"
  ],
  "agamaConfiguration": {
    "mandatoryAttributes": [
      "qname",
      "source"
    ],
    "optionalAttributes": [
      "serialVersionUID",
      "enabled"
    ]
  },
  "auditLogConf": {
    "enabled": true,
    "headerAttributes": [
      "User-inum"
    ]
  },
  "dataFormatConversionConf": {
    "enabled": true,
    "ignoreHttpMethod": [
      "@jakarta.ws.rs.GET()"
    ]
  },
  "plugins": [
    {
      "name": "admin",
      "description": "admin-ui plugin",
      "className": "io.jans.ca.plugin.adminui.rest.ApiApplication"
    },
    {
      "name": "fido2",
      "description": "fido2 plugin",
      "className": "io.jans.configapi.plugin.fido2.rest.ApiApplication"
    },
    {
      "name": "scim",
      "description": "scim plugin",
      "className": "io.jans.configapi.plugin.scim.rest.ApiApplication"
    },
    {
      "name": "user-management",
      "description": "user-management plugin",
      "className": "io.jans.configapi.plugin.mgt.rest.ApiApplication"
    },
    {
      "name": "jans-link",
      "description": "jans-link plugin",
      "className": "io.jans.configapi.plugin.link.rest.ApiApplication"
    },
    {
      "name": "saml",
      "description": "saml plugin",
      "className": "io.jans.configapi.plugin.saml.rest.ApiApplication"
    },
    {
      "name": "kc-link",
      "description": "kc-link plugin",
      "className": "io.jans.configapi.plugin.kc.link.rest.ApiApplication"
    },
    {
      "name": "lock",
      "description": "lock plugin",
      "className": "io.jans.configapi.plugin.lock.rest.ApiApplication"
    }
  ],
  "assetMgtConfiguration": {
    "assetMgtEnabled": true,
    "assetServerUploadEnabled": true,
    "fileExtensionValidationEnabled": true,
    "moduleNameValidationEnabled": true,
    "assetBaseDirectory": "/opt/jans/jetty/%s/custom",
    "jansServiceModule": [
      "jans-auth",
      "jans-config-api",
      "jans-fido2",
      "jans-scim"
    ],
    "assetDirMapping": [
      {
        "directory": "i18n",
        "type": [
          "properties"
        ],
        "description": "Resource bundle file."
      },
      {
        "directory": "libs",
        "type": [
          "jar"
        ],
        "description": "java archive library."
      },
      {
        "directory": "pages",
        "type": [
          "xhtml"
        ],
        "description": "Web pages."
      },
      {
        "directory": "static",
        "type": [
          "js",
          "css",
          "png",
          "gif",
          "jpg",
          "jpeg"
        ],
        "description": "Static resources like Java-script, style-sheet and images."
      }
    ]
  }
}
```

### Update Config-API Configuration Properties

To update the configuration follow the steps below.

1. [Get the current configuration](#get-the-current-config-api-configuration) and store it into a file for getting property to configure
2. Copy the property you want to modify, for example, let say we want to add attribute **** to **userMandatoryAttributes**,
 so prepare a json patch file in such a way that get current values, add the desired attribute as follows in file `config-api-patch.json`:
 ```json title="Sample Output" linenums="1"
 [
  {
    "op": "replace",
    "path": "/userMandatoryAttributes",
    "value": [
      "mail",
      "displayName",
      "status",
      "userPassword",
      "givenName",
      "gender"
    ]
  }
 ]
 ```
 See  [RFC 6902](https://datatracker.ietf.org/doc/html/rfc6902) for details.
3. Execute the following command to apply this patch:
 ```bash title="Command"
  /opt/jans/jans-cli/config-cli.py --operation-id=patch-config-api-properties --data ./config-api-patch.json
 ```
 Upon successful execution of the update, the Janssen Server responds with updated configuration.

As an another example, let us disable file extension validation in asset management, 
so our `config-api--assetmgt-patch.json` will be as follows:

```json title="Sample Output" linenums="1"
[
  {
    "op": "replace",
    "path": "/assetMgtConfiguration/fileExtensionValidationEnabled",
    "value": false
  }
]
```

##  Using Text-based UI

In the Janssen Server, You can manage Config-API Configuration using 
the [Text-Based UI](../config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
sudo /opt/jans/jans-cli/jans_cli_tui.py
```

Navigate to `Config API` section where administrators can update Config-API configurations
in six sub-tabs, namely **Main**, **Agama**, **Plugins**, **Asset Management**, **Audit Log Conf**,
**Data Format Conversion**, and **Auidit Logs**


![image](../../../assets/tui-config-api-main.png)

## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for 
managing and configuring the Config-API Configuration. Endpoint details 
are published in the [Swagger document](./../../reference/openapi.md), see endpoint `/api/v1/api-config`.
