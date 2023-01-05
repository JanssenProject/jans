---
tags:
  - administration
  - auth-server
  - endpoint
  - device authorization
  - RFC 8628

---


### Device Authorization endpoint
The URI to invoke the Device Authorization Endpoint in Janssen Auth Server can be found by checking the introspection_endpoint claim of the OpenID Connect configuration response, typically deployed at `https://<my.jans.server>/.well-known/openid-configuration`

`"device_authorization_endpoint" : "https://<my.jans.server>/jans-auth/restv1/device_authorization"`

### Invoking the endpoint in Device Authorization Flow
The Device Authorization Grant defined by [RFC 8628](https://tools.ietf.org/html/rfc8628) contains a call to the Device Authorization endpoint in Step 2 of the diagram below. The details of the entire flow can be found in this [article](../oauth-features/device-grant.md)

```mermaid
sequenceDiagram
autonumber 1
    title Oauth2.0 Device Authorization flow

    participant User
    participant Browser on Computer / Smartphone
    participant Device App
    participant Jans AS
    participant Third Party App

    User->>Device App:Opens an app on device
    Device App->>Jans AS:Sends authorization request \n"jans-server.com/jans-auth/restv1/device_authorization"
    Jans AS->>Device App:Response - \nuser_code, device_code, verification_url, interval, expiration
    Device App ->>User: Instructs the user to access Verification URL \nand enter user_code
    note over Device App:Device App will keep polling AS for Access Token \nuntil device authorization is completed
    loop till Device App recieves Access Token:
    Device App->>Jans AS:request Access Token
    Jans AS->>Device App:Response - \naccess_denied \nOR expired_token \nOR authorization_pending \nOR Access token
    end
    User->>Browser on Computer / Smartphone:Opens a browser \nand access verification URL
    Browser on Computer / Smartphone->>Jans AS:send user_code to verification URL
    Jans AS -->> Browser on Computer / Smartphone :Login and authorization prompt
    Browser on Computer / Smartphone->>Jans AS:Authentication and consent
    Jans AS->>Jans AS: Mark device as Authorized
    note over Jans AS:Subsequent polling by the Device App \nwill return an Access Token as indicated \nby the loop above
    Device App->>Third Party App:Invoke API with Access Token
    Third Party App->>Device App: return Response
```

**Request:**
```
POST /restv1/device_authorization HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Host: test.jans.org
Authorization: Basic MTIzLTEyMy0xMjM6WkE1aWxpTFFDYUR4

client_id=123-123-123&scope=openid+profile+address+email+phone
```
**Response:**
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
