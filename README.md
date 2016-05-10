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

#### Sample Code - RS256

```java
GenerateKeyRequest request = new GenerateKeyRequest();
request.setSignatureAlgorithm(SignatureAlgorithm.RS256);
request.setExpirationTime(expirationTime);

GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
client.setRequest(request);

GenerateKeyResponse response = client.exec();

assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertNotNull(response.getKeyId());
String rs256Alias = response.getKeyId();
```

#### Sample Request - RS256

```
POST /oxeleven/rest/oxeleven/generateKey HTTP/1.1
Host: ce.gluu.info:8443
Cache-Control: no-cache
Content-Type: application/x-www-form-urlencoded

signatureAlgorithm=RS256&expirationTime=1462916947752
```

#### Sample Response - RS256

```
{
    "kty": "RSA",
    "kid": "57a6c4fd-f65e-4baa-8a5d-f34812265383",
    "use": "sig",
    "alg": "RS256",
    "exp": 1462916947752
}
```

#### Sample Code - ES256

```java
GenerateKeyRequest request = new GenerateKeyRequest();
request.setSignatureAlgorithm(SignatureAlgorithm.ES256);
request.setExpirationTime(expirationTime);

GenerateKeyClient client = new GenerateKeyClient(generateKeyEndpoint);
client.setRequest(request);

GenerateKeyResponse response = client.exec();

assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertNotNull(response.getKeyId());
String es256Alias = response.getKeyId();
```

#### Sample Request - ES256

```
POST /oxeleven/rest/oxeleven/generateKey HTTP/1.1
Host: ce.gluu.info:8443
Cache-Control: no-cache
Content-Type: application/x-www-form-urlencoded

signatureAlgorithm=ES256&expirationTime=1462916947752
```

#### Sample Response - ES256

```
{
    "kty": "EC",
    "kid": "f6ade591-4230-4114-8147-316dde969395",
    "use": "sig",
    "alg": "ES256",
    "crv": "P-256",
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

#### Sample Code - HS256

```java
SignRequest request = new SignRequest();
request.getSignRequestParam().setSigningInput(signingInput);
request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);
request.getSignRequestParam().setSharedSecret(sharedSecret);

SignClient client = new SignClient(signEndpoint);
client.setRequest(request);

SignResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertNotNull(response.getSignature());
String hs256Signature = response.getSignature();
```

#### Sample Request - HS256

```
POST /oxeleven/rest/oxeleven/sign HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "Signing Input",
    "signatureAlgorithm": "HS256",
    "sharedSecret": "secret"
}
```

#### Sample Response - HS256

```
{
    "sig": "CZag3MkkRmJXCnDbE43k6gRit_7ZIPzzpBMHXiNNHBg"
}
```

#### Sample Code - RS256

```java
SignRequest request = new SignRequest();
request.getSignRequestParam().setSigningInput(signingInput);
request.getSignRequestParam().setAlias(rs256Alias);
request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

SignClient client = new SignClient(signEndpoint);
client.setRequest(request);

SignResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertNotNull(response.getSignature());
String rs256Signature = response.getSignature();
```

#### Sample Request - RS256

```
POST /oxeleven/rest/oxeleven/sign HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "Signing Input",
    "signatureAlgorithm": "RS256",
    "alias": "57a6c4fd-f65e-4baa-8a5d-f34812265383"
}
```

#### Sample Response - RS256

```
{
    "sig": "TharYC_SVPb_PDWyLM2d1_XsAAiePEMom0Wja8R9aWZpP2mRrzMJKuLUcOG7QE7JxnVgQmGGnEV8QPKguGDca5S2EU9NiodFBzg6N4JEFC5FvrpDyZPRhtQP3OKshGWyLKa37KddUWGVRTwfluUhirMRgFmTMYjv6Wuhj_Dx7DoBvMY5KbEkIcBm1tqvqT2U02RNo8ts0PSW3z3hkdygCAcwqmzb0ICBxZ6aCePmVtSXaicEX0Z8FuZY0t4b-PjkuCIUIPLdb5043HFdGX1dwErEi3Y1j-osALnamS8LCqvogjMxbx_MJt6QaUkW952JT0Tk1Xvc_J81ZekzvMpptw"
}
```

#### Sample Code - ES256

```java
SignRequest request = new SignRequest();
request.getSignRequestParam().setSigningInput(signingInput);
request.getSignRequestParam().setAlias(es256Alias);
request.getSignRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

SignClient client = new SignClient(signEndpoint);
client.setRequest(request);

SignResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertNotNull(response.getSignature());
String es256Signature = response.getSignature();
```

#### Sample Request - ES256

```
POST /oxeleven/rest/oxeleven/sign HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "Signing Input",
    "signatureAlgorithm": "ES256",
    "alias": "f6ade591-4230-4114-8147-316dde969395"
}
```

#### Sample Response - ES256

```
{
    "sig": "MEUCIQCe-t-b4ba7OaIBuNKHCCW2GIKPzjTZKCdBAP4EEmVJAQIgXHIW3c9_Ax2DvUHu_tJJzV9LUeYH5uw40m-h2qy-jgM"
}
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
