---
tags:
  - administration
  - configuration
  - default authentication
---

# Default Authentication Method

The Janssen Server allows administrators to set and manage the default
authentication method for the authentication server.
The Janssen Server provides multiple configuration tools to perform these tasks.

!!! Note

    Only one of the available authentication methods can be set as the default.
    While setting the Default authentication method, the Janssen Server 
    checks if the same authentication is available and active.
    
    See 
    [script documentation](custom-scripts-config.md#update-an-existing-custom-script) 
    to know how to enable/disable authentication methods using custom scripts.


    If the script is not active then the following error notification is 
    returned by API.
    ```{
        "code": "400",
        "message": "INVALID_ACR",
        "description": "Authentication script {acr} is not active"
    }
    ```

    Also, to understand how Janssen Server picks the authentication method *in absence* of default authentication method, refer to [ACR documentation](../auth-server/openid-features/acrs.md#flowchart---how-the-jans-as-derives-an-acr-value-for-a-user-session-)

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to
    use Jans CLI [here](./config-tools/jans-cli/README.md) or jump straight to
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal.
    Learn how to use Jans Text-based UI (TUI)
    [here](./config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](./config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

##  Using Command Line

Operations to manage the default authentication method are grouped under the
`DefaultAuthenticationMethod` task. To get information about those operations
use the command below.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info DefaultAuthenticationMethod
```
```text title="Output"
Operation ID: get-acrs
  Description: Gets default authentication method.
Operation ID: put-acrs
  Description: Updates default authentication method.
  Schema: AuthenticationMethod

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/AuthenticationMethod
```

### Find Current Authentication Method

To get the current default authentication method use the command below.
```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-acrs
```
```json title="Sample Output"
{
  "defaultAcr": "simple_password_auth"
}
```

### Update Default Authentication Method

Let's update the _Default Authentication Method_ using the janssen CLI command line.
To perform the _put-acrs_ operation, we have to use its schema.
To get its schema:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema AuthenticationMethod \
> /tmp/patch-default-auth.json
```
The schema can now be found in the patch-default-auth.json file.

For your information, you can obtain the format of the `AuthenticationMethod`
schema by running the aforementioned command without a file.

```text title="Schema Format"
defaultAcr   string
```
you can also use the following command for `AuthenticationMethod` schema example.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema-sample AuthenticationMethod
```
```json title="Schema Example"
{
"defaultAcr": "string"
}
```

We need to modify the patch-default-auth.json file.
We have seen that our default authentication method is `simple_password_auth`.
We are going to update it with `passport_saml` authentication method.

```json title="input"
{
  "defaultAcr": "passport_saml"
}

```

Now let's trigger the operation using the above file.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id put-acrs \
--data /tmp/patch-default-auth.json
```

It will show the updated result.

```json title="Sample Output"
{
  "defaultAcr": "passport_saml"
}

```

##  Using Text-based UI

In Janssen, You can manage default authentication method using
the [Text-Based UI](./config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
sudo /opt/jans/jans-cli/jans_cli_tui.py
```

### Find Current Authentication Method

Navigate to `Auth Server` -> `Authn` to open the `Authn` screen as shown
in the image below. This screen lists the available authentication methods
where the default method is marked with `x` under the `Default` column.

![image](../../assets/tui-curr-authn-method.png)


### Update Default Authentication Method

Bring the tab focus to the authentication method that should be the new default
method. Hit `Enter` to open the dialog as shown above. Using the checkbox for
`Default Authen Method` the current method can be made the default 
authentication method.


## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the Default Authentication Method. Endpoint details are published
in the [Swagger document](./../reference/openapi.md).

