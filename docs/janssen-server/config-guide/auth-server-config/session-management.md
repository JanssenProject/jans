---
tags:
  - administration
  - configuration
  - session management
---

# Session Management

User sessions in database can be searched, viewed and deleted using TUI,
command line an rest-api


## Use Command-line

In the Janssen Server, you can search, delete, view sessions under 
`ConfigurationLogging` task using the
command below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info=AuthSessionManagement
```

```text title="Sample Output"
--info AuthSessionManagement
Operation ID: get-session-by-id
  Description: Get session by id.
  Parameters:
  sid: Session identifier. [string]
Operation ID: delete-session
  Description: Delete a session.
  Parameters:
  sid: Session identifier. [string]
Operation ID: revoke-user-session
  Description: Revoke all sessions by userDn
  Parameters:
  userDn: User domain name [string]
Operation ID: get-sessions
  Description: Return all session
Operation ID: search-session
  Description: Search session
  Parameters:
  limit: Search size - max size of the results to return [integer]
  pattern: Search pattern [string]
  startIndex: The 1-based index of the first query result [integer]
  sortBy: Attribute whose value will be used to order the returned response [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
  fieldValuePair: Field and value pair for seraching [string]

```

As an example search sessions of user **admin** expiration date greater than
October 12, 2024

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id=search-session --endpoint-args="fieldValuePair:auth_user=admin\,expirationDate>2024-10-12 00:00:00"
```

To delete a session with **sid=c190667e-a186-421d-8679-420e82615a38**

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id=delete-session --url-suffix="sid:c190667e-a186-421d-8679-420e82615a38"
```

## Use Text-based UI

    Start TUI using the command below:

```bash title="Command"
jans tui
```

To start searching and deleting sessions, navigate to
`Auth Server`->`Sessions`.  This brings up a screen as shown below
with searching and deleting sessions.

![Searching Sessions](../../../assets/tui-auth-server-sessions.png)

After entering search criterias, navigate to **< Search >** button end press **Enter**.
To delete a session, select it and press **delete** key.

Refer to complete documentation [here](../../config-guide/config-tools/jans-tui/README.md)


## Use REST API

To use rest-api you need an access token. You can get access token by executing the following
command if you previously authorized by TUI:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --output-access-token
```

```text title="Sample Output"
5abf61a6-b86f-49d7-8134-77168303d0f9
```
As an example let us search sessions of user **admin** expiration date greater than
October 12, 2024

```bash title="Command"
curl -k "https://example.jans.io/jans-config-api/api/v1/jans-auth-server/session/search?fieldValuePair=auth_user%3Dadmin%2CexpirationDate%3E2024-10-12%2000%3A00%3A00" -H "Authorization: Bearer 5abf61a6-b86f-49d7-8134-77168303d0f9"
```

Note! that we use percent encodings for special characters, [see](https://en.wikipedia.org/wiki/Percent-encoding) for details.

The percent decoded value of the filter we used in our search was `fieldValuePair=auth_user=admin,expirationDate>2024-10-12 00:00:00`



Another example is to delete session with **sid=c190667e-a186-421d-8679-420e82615a38**

```bash title="Command"
curl -k -X DELETE "https://example.jans.io/jans-config-api/api/v1/jans-auth-server/session/sid/c190667e-a186-421d-8679-420e82615a38" -H "Authorization: Bearer 5abf61a6-b86f-49d7-8134-77168303d0f9"
```

Navigate to **Auth - Session Management** for other endpoints of managing sessions in config-api [swagger file](../../../../jans-config-api/docs/jans-config-api-swagger.yaml).
