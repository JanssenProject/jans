---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - device
  - grant
---

# Device Authorization Grant

The [OAuth 2.0 Device Authorization Grant (RFC 8628)](https://datatracker.ietf.org/doc/html/rfc8628) enables applications running on input-constrained or browserless devices, such as smart TVs, media consoles, and printers, to obtain user authorization.

In this flow, the device requests authorization from the Janssen Authentication Server and displays a verification URL and user code. The user opens the verification URL on another device, such as a smartphone or computer, to authenticate and approve the request. Meanwhile, the requesting device polls the Janssen token endpoint until authorization is completed or the request expires.

### Sequence Diagram

```mermaid

sequenceDiagram
    title OAuth 2.0 Device Authorization Flow
    autonumber

    participant User
    participant Browser as User's Browser
    participant Device as Device App
    participant Jans as Jans AS
    participant ThirdParty as Third Party App

    User->>Device: Opens an app on device
    Device->>Jans: Device authorization request
    Jans-->>Device: user_code, device_code, verification_uri, interval, expires_in
    Device-->>User: Display verification URI and user_code

    Note over Device,Jans: Device polls the token endpoint at the required interval

    Device->>Jans: Token request with device_code
    Jans-->>Device: authorization_pending

    User->>Browser: Opens verification URI
    Browser->>Jans: Submits user_code
    Jans-->>Browser: Login and authorization prompt
    Browser->>Jans: Authentication and consent
    Jans->>Jans: Mark device as authorized

    Device->>Jans: Poll token endpoint with device_code
    Jans-->>Device: Access token

    Device->>ThirdParty: Invoke API with access token
    ThirdParty-->>Device: API response

```

## User Experience

1. First, the user requests authorization from the device:

![DeviceFlow1](https://github.com/JanssenProject/jans/raw/main/docs/assets/device-flow-1.png)

2. At the URL displayed on the screen, the user can input the displayed code in the device.

![DeviceFlow2](https://github.com/JanssenProject/jans/raw/main/docs/assets/device-flow-2.png)

3. After that, user could need to authenticate, then decide whether permissions will be granted.

`acr` value can be specified in `deviceAuthzAcr` AS configuration property.

![DeviceFlow3](https://github.com/JanssenProject/jans/raw/main/docs/assets/device-flow-3.png)

4. Finally, the confirmation screen will be shown.

![DeviceFlow4](https://github.com/JanssenProject/jans/raw/main/docs/assets/device-flow-4.png)

#### ACR value during Device Authorization

User is required to authenticate if not authenticated yet. By default `acr_values` is not set in authorization request, which means it relies on default AS handling.
However it's possible explicitly set `acr` value during Device Authorization by setting `deviceAuthzAcr` AS configuration property.

## Request user and device codes

This first step, device sends an HTTP POST request to Jans authorization server, at `/jans-auth/restv1/device_authorization` which is also presented in the `Discovery url` published by Jans Server, this endpoint could process a common authentication method or it could also not do any authentication, depending on the configuration for the client in Jans Server.

#### Parameters

| Parameter | Description                                                                                                                                                                                                |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| client_id | **Required** The client ID for your application.                                                                                                                                                           |
| scope     | **Required** A space separated list of scopes that identify the resources that the device could access on the user's behalf. These values inform the consent screen that Jans server displays to the user. |

#### Example

```
POST /restv1/device_authorization HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Host: test.jans.org
Authorization: Basic MTIzLTEyMy0xMjM6WkE1aWxpTFFDYUR4

client_id=123-123-123&scope=openid+profile+address+email+phone
```

## Device Request response

In response, the Jans authorization server generates a unique device verification code and an end-user code that are valid for a limited time and includes them in the HTTP response body using the "application/json" format with a 200 (OK) status code.  The response contains the following parameters:

| Parameter                 | Description                                                                                                                                                                         |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| user_code                 | **Required** The end-user verification code                                                                                                                                         |
| device_code               | **Required** The device verification code                                                                                                                                           |
| verification_uri          | **Required** The end-user verification URI on the authorization server. This should be shown to the end-user because he should open this url in the rich user-agent.                |
| verification_uri_complete | **Optional** This is similar than `verification_uri`, however it also includes `user_code` as a query param in the url. It's issued if the device wants to use QR mode for example. |
| expires_in                | **Required** The lifetime in seconds of the `device_code` and `user_code`.                                                                                                          |
| interval                  | **Required** The minimum amount of time in seconds that the client should wait between polling requests to the token endpoint.                                                      |

#### Example:

```
HTTP/1.1 200
Content-Length: 307
Content-Type: application/json
Server: Jetty(9.4.19.v20190610)

{
    "user_code": "SJFP-DTPL",
    "device_code": "aeb28bdc90d806ac58d4b0f832f06c3ac9c4bd03292f0c09",
    "interval": 5,
    "verification_uri_complete": "https://test.jans.io:8443/device-code?user_code=SJFP-DTPL",
    "verification_uri": "https://test.jans.io:8443/device-code",
    "expires_in": 1800
}
```

## Device Display

Common flow, the device should display `verification_url` and `user_code` received from Jans server. The content that the device displays to the user should instruct the user to navigate to the `verification_url` on a separate device and enter the `user_code`.

Design device interface following these rules:

1. `user_code` has the following format: XXXX-XXXX where Xs represent to any ASCII character, for example: *RTXD-HTLK*. The length of the `user_code` will be always the same, therefore it's highly recommended to show it as clear and big that the user can read it easly.

2. `verification_url` should be displayed also in a way that the user can read it easily. The normal length should be around 40 characters, however it could depends also on the domain used for the server. Remember that the user will need to write the whole URL manually in the web browser, therefore it's recommended to use a short URL.

3. `verification_url_complete` will be used for those cases where device can show QR (Quick Response) codes or NFC (Near Field Communication) to save the user from typing the whole URI. Interaction between device and Jans server will be the same, however user can process the authorization faster. For example:



## User Login & Authorization

The user will need to put `user_code` value in the browser and after that, he will be redirected to the common authorization flow, it requires login whether there is no session between the user-agent and the Jans server and grant the permissions based on the device request.

## Poll Token

Since the user will be using a separate device to navigate to the `verification_url` and grant or deny access, the requesting device is not automatically notified when the user responds to the access request. For that reason, the requesting device needs to poll the Jans Server to determine when the user has responded to the request.

The requesting device continues polling the token endpoint while authorization is pending, until it receives a successful token response, a terminal error, or the `device_code` expires. The `interval` parameter returned by the device authorization endpoint specifies the minimum number of seconds between polling requests. If the server returns a `slow_down` error, the device must increase the polling interval by at least five seconds.

The URL of the endpoint to poll is `/jans-auth/restv1/token`. The polling request contains the following parameters:

#### Parameters

| Parameter   | Description                                                                           |
|-------------|---------------------------------------------------------------------------------------|
| client_id   | **Required** The client ID for your application.                                      |
| grant_type  | **Required** Must be `urn:ietf:params:oauth:grant-type:device_code`.                  |
| device_code | **Required** The device verification code which is returned by Jans server in Step 2. |

Authentication can be done using any of those authentication methods for this token endpoint.

#### Example:

```
POST /restv1/token HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Host: test.jans.io
Authorization: Basic MTIzLTEyMy0xMjM6WkE1aWxpTFFDYUR4

grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code&device_code=0bd7068e7fdab4bb91b313296a96462256a7370d12f073f0
```

### Token Response

The Jans authorization server responds to each polling request with one of the following responses:

**Access granted**

If the user grants access to the device, the Janssen Authentication Server returns an access token. A refresh token and an ID token may also be issued, depending on the requested scopes and server configuration.

The device can use the access token to access protected resources on the user's behalf.

Example:

```
HTTP/1.1 200
Content-Length: 858
Content-Type: application/json
Server: Jetty(9.4.19.v20190610)

{"access_token":"c31fc092-453b-4275-a36f-b2740c3eb1a6","id_token":"eyJraWQiOiJlY4.2Tc4NDgxYy05OTJkLTRmN2UtYTkzMS03NjM2NTYyMzgwZjVfc2lnX3JzMjU2IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJhdF9oYXNoIjoidWdMSnAzMkxXdnI4QUdlbmdNTlF3QSIsImF1ZCI6IjEyMy0xMjMtMTIzIiwic3ViIjoiM2M2M25HdWZnWFNkMWFwNU81NFZkVjlUUDdmdjJHc0YtLWl0eVBHeFJBTSIsImlzcyI6Imh0dHBzOi8vdGVzdC5nbHV1Lm9yZzo4NDQzIiwiZXhwIjoxNTk1NjQzOTg0LCJpYXQiOjE1OTU2NDAzODQsIm94T3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCJ9.fElZtuUslhSSuqTOuvGeafG4QuQoHKLpya25RHWkC5V9Xf9ODYa6tD_Tdav2D9Gff2Zz7pt8WKso-WYOqmJ3NrgMoVU7d1SMj6pYGilTL1JokjB18Yw1TI6oR6Z4wegy8_ajftLLhqosI5-ZE36TzPwoAKzjPl-iZEpV2U1OPHWZrdwc9N3YOyO0I_IJGQmFnXC_oacitMV2VZaTxfuCew5cPwNp5durooFNvv3DPzc9JYEctmaLsiRtfqN7pCaV30B3hnYTYZ4p2HNsUbOewBI8_Brm1v1CByitQPUFqETgmPGbf4HCTEoaH-7DfaXnAsePt73blNwJrlTlUBieew","token_type":"bearer","expires_in":299}
```

**Access denied**

If the user refuses to grant access to the device, then the server response has a `400` HTTP response status code. The response contains the following error:

```
HTTP/1.1 400
Cache-Control: no-store
Content-Length: 74
Content-Type: application/json
Pragma: no-cache
Server: Jetty(9.4.19.v20190610)

{
    "error_description": "access_denied",
    "error": "access_denied"
}
```

**Authorization pending**
It means that the request hasn't been processed by the end-user yet, therefore the request is still in process.

Example:

```
HTTP/1.1 400
Cache-Control: no-store
Content-Length: 90
Content-Type: application/json
Pragma: no-cache
Server: Jetty(9.4.19.v20190610)

{
    "error_description": "authorization_pending",
    "error": "authorization_pending"
}
```

**Polling too frequently**
If the device sends polling requests too frequently, then the server returns a `400` HTTP response status code. Then the device should increase the time interval between requests.

Example:

```
HTTP/1.1 400
Cache-Control: no-store
Content-Length: 66
Content-Type: application/json
Pragma: no-cache
Server: Jetty(9.4.19.v20190610)

{
    "error_description": "slow_down",
    "error": "slow_down"
}
```

**Other errors**
This token endpoint can return any error code already defined, for example whether client can't be authenticated or the grant type sent is invalid. Some of them could be: `invalid_client`, `invalid_grant`, `invalid_request` and others.

### Customizing Device Grant page

To customize the Device Authorization page with your organization's stylesheets, images, and content, refer to the [Device Authorization page source](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/device_authorization.xhtml) and the [Customize Web Pages guide](../../developer/customization/customize-web-pages.md).
