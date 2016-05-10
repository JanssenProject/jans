# oxEleven

Java web application providing REST API's for a PKCS #11 interface.

## Operations

### /generateKey

- **URL:** https://ce.gluu.info:8443/oxeleven/rest/oxeleven/generateKey
- **Method:** POST
- **Media Type:** application/x-www-form-urlencoded
- **Data Params**
    - signatureAlgorithm [string]
    - expirationTime [long]

#### Sample Code

```java
GenerateKeyRequest request = new GenerateKeyRequest();
request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
request.setExpirationTime(expirationTime);

GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
client.setRequest(request);

GenerateKeyResponse response = client.exec();

assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertNotNull(response.getKeyId());
String alias = response.getKeyId();
```

#### Sample Request

```
POST /oxeleven/rest/oxeleven/generateKey HTTP/1.1
Host: ce.gluu.info:8443
Cache-Control: no-cache
Content-Type: application/x-www-form-urlencoded

signatureAlgorithm=RS256&expirationTime=1462916947752
```

#### Sample Response

```
{
    "kty": "RSA",
    "kid": "57a6c4fd-f65e-4baa-8a5d-f34812265383",
    "use": "sig",
    "alg": "RS256",
    "exp": 1462916947752
}
```

### /sign

- **URL:** https://ce.gluu.info:8443/oxeleven/rest/oxeleven/sign
- **Method:** POST
- **Media Type:** application/json
- **Data Params**
```javascript
{
    "signingInput": [string], 
    "signatureAlgorithm": [string],
    "alias": [string],
    "sharedSecret": [string]
}
```

#### Sample Code

```java
```

#### Sample Request

```
```

#### Sample Response

```
```

### /verifySignature

- **URL:** https://ce.gluu.info:8443/oxeleven/rest/oxeleven/verifySignature
- **Method:** POST
- **Media Type:** application/json
- **Data Params**
```javascript
{
    "signingInput": [string],
    "signature": [string],
    "alias": [string],
    "jwksRequestParam": [
        {
            "alg": [string],
            "kid": [string],
            "use": [string],
            "kty": [string],
            "n": [string],
            "e": [string],
            "crv": [string],
            "x": [string],
            "y": [string]
        }
    ],
    "sharedSecret": [string],
    "signatureAlgorithm": [string]
}
```

#### Sample Code

```java
```

#### Sample Request

```
```

#### Sample Response

```
```

### /deleteKey

- **URL:** https://ce.gluu.info:8443/oxeleven/rest/oxeleven/deleteKey
- **Method:** POST
- **Media Type:** application/x-www-form-urlencoded
- **Data Params**
    - kid [string]

#### Sample Code

```java
```

#### Sample Request

```
```

#### Sample Response

```
```

###/jwks

- **URL:** https://ce.gluu.info:8443/oxeleven/rest/oxeleven/jwks
- **Method:** POST
- **Media Type:** application/json
- **Data Params**
```javascript
{
    "keyRequestParams": [
        {
            "alg": [string],
            "kid": [string],
            "use": [string],
            "kty": [string],
            "n": [string],
            "e": [string],
            "crv": [string],
            "x": [string],
            "y": [string]
        }
    ]
}
```

#### Sample Code

```java
```

#### Sample Request

```
```

#### Sample Response

```
```

## Supported Algorithms

none
HS256
HS384
HS512
RS256
RS384
RS512
ES256
ES384
ES512

## Installation

  1. Install [SoftHSM version 2](https://github.com/opendnssec/SoftHSMv2)

  2. Copy the file Server/conf/oxeleven-config.json to $CATALINA_HOME/conf/oxeleven-config.json

  3. Edit the configuration file $CATALINA_HOME/conf/oxeleven-config.json

  ```javascript
  {
    "pkcs11Config": {
      "name": "SoftHSM",
      "library": "/usr/local/lib/softhsm/libsofthsm2.so",
      "slot": "0",
      "showInfo": "true"
    },
    "pkcs11Pin": "1234",
    "dnName": "CN=oxAuth CA Certificate"
  }
  ```

  4. Deploy oxEleven.war in Tomcat
  
## Test

  1. Ensure oxEleven is deployed an running
  
  2. Edit the file Client/src/test/Resources/testng.xml to point to your oxEleven deployment
  
  3. cd Client
  
  4. mvn test

To access Gluu support, please register and open a ticket on [Gluu Support](http://support.gluu.org)
