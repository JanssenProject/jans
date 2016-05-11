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
    "jwksRequestParam": { 
        "keyRequestParams": [{
            "alg": [string],
            "kid": [string],
            "use": [string],
            "kty": [string],
            "n": [string],
            "e": [string],
            "crv": [string],
            "x": [string],
            "y": [string]
        }],
    },
    "sharedSecret": [string],
    "signatureAlgorithm": [string]
}
```

#### Sample Code - none

```java
VerifySignatureRequest request = new VerifySignatureRequest();
request.getVerifySignatureRequestParam().setSigningInput(signingInput);
request.getVerifySignatureRequestParam().setSignature(noneSignature);
request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.NONE);

VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
client.setRequest(request);

VerifySignatureResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isVerified());
```

#### Sample Request - none

```
POST /oxeleven/rest/oxeleven/verifySignature HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "Signing Input",
    "signature": "",
    "signatureAlgorithm": "none"
}
```

#### Sample Response - none

```
{
    "verified": true
}
```

#### Sample Code - HS256

```java
VerifySignatureRequest request = new VerifySignatureRequest();
request.getVerifySignatureRequestParam().setSigningInput(signingInput);
request.getVerifySignatureRequestParam().setSignature(hs256Signature);
request.getVerifySignatureRequestParam().setSharedSecret(sharedSecret);
request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.HS256);

VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
client.setRequest(request);

VerifySignatureResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isVerified());
```

#### Sample Request - HS256

```
POST /oxeleven/rest/oxeleven/verifySignature HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "Signing Input",
    "signature": "CZag3MkkRmJXCnDbE43k6gRit_7ZIPzzpBMHXiNNHBg",
    "signatureAlgorithm": "HS256",
    "sharedSecret": "secret"
}
```

#### Sample Response - HS256

```
{
    "verified": true
}
```

#### Sample Code - RS256 JWKs

```java
String alias = "RS256SIG";
JwksRequestParam jwksRequestParam = new JwksRequestParam();
KeyRequestParam keyRequestParam = new KeyRequestParam("RSA", "sig", "RS256", alias);
keyRequestParam.setN("AJpGcIVu7fmQJLHXeAClhXaJD7SvuABjYiPcT9IbKFWGWj51GgD-CxtyrQGXT0ctGEEsXOzMZM40q-V7GR-5qkJ_OalVTTc_EeKAHao45bZPsPHLxvusNfrfpyhc6JjF2TQhoOqxbgMgQ9L6W9q9fSjgzx-tPlD0d3X0GZOEQ_NYGstZWRRBwHgsxA2IRYtwSH-v76yPpxF9poLIWdnBKtKfSr6UY7p1BrLmMm0DdMhjQLn6j4S_eB-p2WyBwObvsLqO6FdClpZFtGr82Km2uinpHvZ6KJ_MUEW1sijPPI3rIGbaUbLtQJwX5GVynAP5qU2qRVkcsrKt-GeNoz6QNLM");
keyRequestParam.setE("AQAB");
jwksRequestParam.setKeyRequestParams(Arrays.asList(keyRequestParam));
        
VerifySignatureRequest request = new VerifySignatureRequest();
request.getVerifySignatureRequestParam().setSigningInput(signingInput);
request.getVerifySignatureRequestParam().setSignature(signature);
request.getVerifySignatureRequestParam().setAlias(alias);
request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
client.setRequest(request);

VerifySignatureResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isVerified());
```

#### Sample Request - RS256 JWKs

```
POST /oxeleven/rest/oxeleven/verifySignature HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlJTMjU2U0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCEzN0JBLkExRjEiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCEzN0JBLkExRjEiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6Ijc0NWY0N2RmLTY3ZDQtNDBlOC05MzhlLTVlMmI5OWQ5ZTQ3YSIsImV4cCI6MTQ2MTAzMDE5MSwiaWF0IjoxNDYxMDI5ODkxfQ",
    "signature": "RB8KEbzMTovJLGBzxbaxzLvZxj0CjAun1LG1KMuw9t9LBNzA9kxt_QT9qm_vr_SpCFuFhIy6ZeDx4lVPGks6JbWOYxmsCUcxe8l_tkCxOb6fwm3GTttDhHsk1JKPwDVjzXWAyW8i5Wiv39JD57K1SOs3xIOWIp7Uu7lR7HFw52ybT35enxiaGj1H3ROX5dd26GE35McTrEBxPLgAj_yEzAADBqI1nOmDvpzSpo3pkSoxaW8UkncIIdcG8WkPru-exN1nWqnsqA5rX3XxwlWNElq6O9kLOZQKKHbCF0EyZwnave3EdWp56XaZ9V5Y20_NL-aaR7DedZ5xPAyzLFCW2A",
    "signatureAlgorithm": "RS256",
    "alias": "RS256SIG",
    "jwksRequestParam": {
        "keyRequestParams": [{
            "alg": "RS256",
            "kid": "RS256SIG",
            "use": "sig",
            "kty": "RSA",
            "n": "AJpGcIVu7fmQJLHXeAClhXaJD7SvuABjYiPcT9IbKFWGWj51GgD-CxtyrQGXT0ctGEEsXOzMZM40q-V7GR-5qkJ_OalVTTc_EeKAHao45bZPsPHLxvusNfrfpyhc6JjF2TQhoOqxbgMgQ9L6W9q9fSjgzx-tPlD0d3X0GZOEQ_NYGstZWRRBwHgsxA2IRYtwSH-v76yPpxF9poLIWdnBKtKfSr6UY7p1BrLmMm0DdMhjQLn6j4S_eB-p2WyBwObvsLqO6FdClpZFtGr82Km2uinpHvZ6KJ_MUEW1sijPPI3rIGbaUbLtQJwX5GVynAP5qU2qRVkcsrKt-GeNoz6QNLM",
            "e": "AQAB"
        }]
    }
}
```

#### Sample Response - RS256 JWKs

```
{
    "verified": true
}
```

#### Sample Code - RS256

```java
VerifySignatureRequest request = new VerifySignatureRequest();
request.getVerifySignatureRequestParam().setSigningInput(signingInput);
request.getVerifySignatureRequestParam().setSignature(rs256Signature);
request.getVerifySignatureRequestParam().setAlias(rs256Alias);
request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.RS256);

VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
client.setRequest(request);

VerifySignatureResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isVerified());
```

#### Sample Request - RS256

```
POST /oxeleven/rest/oxeleven/verifySignature HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "Signing Input",
    "signature": "TharYC_SVPb_PDWyLM2d1_XsAAiePEMom0Wja8R9aWZpP2mRrzMJKuLUcOG7QE7JxnVgQmGGnEV8QPKguGDca5S2EU9NiodFBzg6N4JEFC5FvrpDyZPRhtQP3OKshGWyLKa37KddUWGVRTwfluUhirMRgFmTMYjv6Wuhj_Dx7DoBvMY5KbEkIcBm1tqvqT2U02RNo8ts0PSW3z3hkdygCAcwqmzb0ICBxZ6aCePmVtSXaicEX0Z8FuZY0t4b-PjkuCIUIPLdb5043HFdGX1dwErEi3Y1j-osALnamS8LCqvogjMxbx_MJt6QaUkW952JT0Tk1Xvc_J81ZekzvMpptw",
    "signatureAlgorithm": "RS256",
    "alias": "57a6c4fd-f65e-4baa-8a5d-f34812265383"
}
```

#### Sample Response - RS256

```
{
    "verified": true
}
```

#### Sample Code - ES256 JWKs

```java
String alias = "ES256SIG";
JwksRequestParam jwksRequestParam = new JwksRequestParam();
KeyRequestParam keyRequestParam = new KeyRequestParam("EC", "sig", "ES256", alias);
keyRequestParam.setCrv("P-256");
keyRequestParam.setX("QDpwgxzGm0XdD-3Rgk62wiUnayJDS5iV7nLBwNEX4SI");
keyRequestParam.setY("AJ3IvktOcoICgdFPAvBM44glxcqoHzqyEmj60eATGf5e");
jwksRequestParam.setKeyRequestParams(Arrays.asList(keyRequestParam));

VerifySignatureRequest request = new VerifySignatureRequest();
request.getVerifySignatureRequestParam().setSigningInput(signingInput);
request.getVerifySignatureRequestParam().setSignature(signature);
request.getVerifySignatureRequestParam().setAlias(alias);
request.getVerifySignatureRequestParam().setJwksRequestParam(jwksRequestParam);
request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
client.setRequest(request);

VerifySignatureResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isVerified());
```

#### Sample Request - ES256 JWKs

```
POST /oxeleven/rest/oxeleven/verifySignature HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IkVTMjU2U0lHIn0.eyJpc3MiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCE3OUIzLjY3MzYiLCJzdWIiOiJAITkwQ0MuMkUzOC43NzRDLjYxMEIhMDAwMSFGRDNCLkIwQTAhMDAwOCE3OUIzLjY3MzYiLCJhdWQiOiJodHRwczovL2NlLmdsdXUuaW5mbzo4NDQzL3NlYW0vcmVzb3VyY2UvcmVzdHYxL294YXV0aC90b2tlbiIsImp0aSI6IjQ0ZjU0NmU0LWRmMmMtNDE5Ny1iNTNjLTIzNzhmY2YwYmRiZSIsImV4cCI6MTQ2MTAzMjgzMiwiaWF0IjoxNDYxMDMyNTMyfQ",
    "signature": "MEQCIGmPSoCExpDu2jPkxttRZ0hjKId9SQM1pP3PLd4CXmt9AiB57tUzvBILyBvHqf3bHVMi0Fsy8M-v-ERib2KVdWJLtg",
    "signatureAlgorithm": "ES256",
    "alias": "ES256SIG",
    "jwksRequestParam": {
        "keyRequestParams": [{
            "alg": "ES256",
            "kid": "ES256SIG",
            "use": "sig",
            "kty": "EC",
            "crv": "P-256",
            "x": "QDpwgxzGm0XdD-3Rgk62wiUnayJDS5iV7nLBwNEX4SI",
            "y": "AJ3IvktOcoICgdFPAvBM44glxcqoHzqyEmj60eATGf5e"
        }]
    }
}
```

#### Sample Response - ES256 JWKs

```
{
    "verified": true
}
```

#### Sample Code - ES256

```java
VerifySignatureRequest request = new VerifySignatureRequest();
request.getVerifySignatureRequestParam().setSigningInput(signingInput);
request.getVerifySignatureRequestParam().setSignature(es256Signature);
request.getVerifySignatureRequestParam().setAlias(es256Alias);
request.getVerifySignatureRequestParam().setSignatureAlgorithm(SignatureAlgorithm.ES256);

VerifySignatureClient client = new VerifySignatureClient(verifySignatureEndpoint);
client.setRequest(request);

VerifySignatureResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isVerified());
```

#### Sample Request - ES256

```
POST /oxeleven/rest/oxeleven/verifySignature HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "signingInput": "Signing Input",
    "signature": "MEUCIQCe-t-b4ba7OaIBuNKHCCW2GIKPzjTZKCdBAP4EEmVJAQIgXHIW3c9_Ax2DvUHu_tJJzV9LUeYH5uw40m-h2qy-jgM",
    "signatureAlgorithm": "ES256",
    "alias": "f6ade591-4230-4114-8147-316dde969395"
}
```

#### Sample Response - ES256

```
{
    "verified": true
}
```

### /deleteKey

- **URL:** https://ce.gluu.info:8443/oxeleven/rest/oxeleven/deleteKey
- **Method:** POST
- **Media Type:** application/x-www-form-urlencoded
- **Data Params**
    - kid [string]

#### Sample Code - RS256

```java
DeleteKeyRequest request = new DeleteKeyRequest();
request.setAlias(rs256Alias);

DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
client.setRequest(request);

DeleteKeyResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isDeleted());
```

#### Sample Request - RS256

```
POST /oxeleven/rest/oxeleven/deleteKey HTTP/1.1
Host: ce.gluu.info:8443
Cache-Control: no-cache
Content-Type: application/x-www-form-urlencoded

kid=57a6c4fd-f65e-4baa-8a5d-f34812265383
```

#### Sample Response - RS256

```
{
    "deleted": true
}
```

#### Sample Code - ES256

```java
DeleteKeyRequest request = new DeleteKeyRequest();
request.setAlias(es256Alias);

DeleteKeyClient client = new DeleteKeyClient(deleteKeyEndpoint);
client.setRequest(request);

DeleteKeyResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertTrue(response.isDeleted());
```

#### Sample Request - ES256

```
POST /oxeleven/rest/oxeleven/deleteKey HTTP/1.1
Host: ce.gluu.info:8443
Cache-Control: no-cache
Content-Type: application/x-www-form-urlencoded

kid=f6ade591-4230-4114-8147-316dde969395
```

#### Sample Response - ES256

```
{
    "deleted": true
}
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
KeyRequestParam k1 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS256, rs256Alias);
KeyRequestParam k2 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS384, rs384Alias);
KeyRequestParam k3 = new KeyRequestParam("RSA", "sig", SignatureAlgorithm.RS512, rs512Alias);
KeyRequestParam k4 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES256, "P-256", es256Alias);
KeyRequestParam k5 = new KeyRequestParam("EC", "sig", SignatureAlgorithm.ES384, "P-384", es384Alias);

JwksRequestParam jwksRequestParam = new JwksRequestParam();
jwksRequestParam.setKeyRequestParams(Arrays.asList(k1, k2, k3, k4, k5));

JwksRequest request = new JwksRequest();
request.setJwksRequestParam(jwksRequestParam);

JwksClient client = new JwksClient(jwksEndpoint);
client.setRequest(request);

JwksResponse response = client.exec();
assertEquals(response.getStatus(), HttpStatus.SC_OK);
assertNotNull(response.getJwksRequestParam());
assertNotNull(response.getJwksRequestParam().getKeyRequestParams());
```

#### Sample Request

```
POST /oxeleven/rest/oxeleven/jwks HTTP/1.1
Host: ce.gluu.info:8443
Content-Type: application/json
Cache-Control: no-cache

{
    "keyRequestParams": [
        {
            "alg": "RS256",
            "kid": "425b3e3e-8514-45b9-a45d-426170e7d1f7",
            "use": "sig",
            "kty": "RSA"
        },
        {
            "alg": "RS384",
            "kid": "95a93e98-e39e-4f5e-929b-4cfd6c6ea04b",
            "use": "sig",
            "kty": "RSA"
        },
        {
            "alg": "RS512",
            "kid": "0825283d-973a-44c0-adc5-01e62ea89d0c",
            "use": "sig",
            "kty": "RSA"
        },
        {
            "alg": "ES256",
            "kid": "ddf87471-9171-4465-85d8-e91530906a5d",
            "use": "sig",
            "kty": "EC",
            "crv": "P-256"
        },
        {
            "alg": "ES384",
            "kid": "1b4b1179-f04e-4750-a06d-13ea265d4b65",
            "use": "sig",
            "kty": "EC",
            "crv": "P-384"
        }
    ]
}
```

#### Sample Response

```
{
    "keyRequestParams": [
        {
            "alg": "RS256",
            "kid": "425b3e3e-8514-45b9-a45d-426170e7d1f7",
            "use": "sig",
            "kty": "RSA",
            "n": "yr7Nc2VhSCd55lEpBWy-DVER8fhshoa17qLf4CuQYvNzOv05yVuLKVN9tcOE7nrBaY9QEgzaNvtyTUJD8y_tOfPGLH5raU3L-kKMfySEPs2hDt2iIYV2JQAhrEuWvHVJDlLMhoL-9fj5t-ur0mrygo6VbLY8f89spaW1HC73nU8B8mFdDTM_aR8OtZgDZ7mEuNy10TTu74Vm4nOCa1f17VRDOVuexgEADrmuKWmegxSoVNlTFIj7v2jZgvhW4GxHYJ5KdS_O1-lNXOMXjJ--_domZfoNqpX6QcQdAow9lgnr9g5vdYuZxf8fYe6qukapchNysschXBVDpuHVnGZo_Q",
            "e": "AQAB",
            "crv": null,
            "x": null,
            "y": null
        },
        {
            "alg": "RS384",
            "kid": "95a93e98-e39e-4f5e-929b-4cfd6c6ea04b",
            "use": "sig",
            "kty": "RSA",
            "n": "8eE0A3ouRZa4nr7T1XXCrdKWkgK8U5SnS1r7TG6lwP7JJm0juNQp6FQqZ1cP6np2T3rj4jr4K_d42pRQWa5EE4EnH1LOQ5_PrOFGs05XdW_aZwRz8scefso7Is0R_TjlSf09flr6C7UoW_uUZgEPoLaYn8mJJLg2rjleAipt-LsgOh0SOZ2Qv6K6RvU0YiukZdpiKKp7oxLmxMNuLDYwGTbvFIiy2o_bDYlf1iIWGDLBl_0Osrqx7w8Z6PTx_zp8GBsqaz25O1tyCqTnKLLxeyS0lDzPxqKjReKv_MsZAzmMfqLYc6EgfcKKgOdPTBfDSbsd_I6E4qd_Bb6mAlwKVQ",
            "e": "AQAB",
            "crv": null,
            "x": null,
            "y": null
        },
        {
            "alg": "RS512",
            "kid": "0825283d-973a-44c0-adc5-01e62ea89d0c",
            "use": "sig",
            "kty": "RSA",
            "n": "qSvqlMAFCHbmHG1cPZsW3gIosKtk7U0zy_PJPdgfPEQFQ-hrIWIMsA7ECKUIUSPg_dbcEMlxOUD-WplG9jmafjU_eiWoObZmThQRYAODQXhXyVNAoE-o7sLPKxJIc4TXzemL4ygVA3NcCAYeXqbhwblcogfql9AGNYnKitA1U8qyla9SBESxqduwUsTy7nFU6X31FKkpcMMAIdkacH-Va3JiTl99K8A52ZwXYeUzzAPzsnlVVb9xKhhYcVIOFnu0jnmVAfMDlI8RdMeriuHKGVUZUxy3iwBfuHYZJURljLPgp4SwLSsr3jwsH6qHgWo5FPcR8AWHh8xrulBFZ8OKQQ",
            "e": "AQAB",
            "crv": null,
            "x": null,
            "y": null
        },
        {
            "alg": "ES256",
            "kid": "ddf87471-9171-4465-85d8-e91530906a5d",
            "use": "sig",
            "kty": "EC",
            "n": null,
            "e": null,
            "crv": "P-256",
            "x": "BWYJuJvjR_xXlh2P6EccCK9vOx-xqcCWaZhTGAFRW5Q",
            "y": "4X7p94uxJqOCi8GgQNuSAE91bWslTOsmMTJNHQsu5j4"
        },
        {
            "alg": "ES384",
            "kid": "1b4b1179-f04e-4750-a06d-13ea265d4b65",
            "use": "sig",
            "kty": "EC",
            "n": null,
            "e": null,
            "crv": "P-384",
            "x": "vaQ77h84F8Qj18xFt9p9Dzur2yL-3NsEgoyzredAm3A9jq3aP5xljYrzwd5Im3Ca",
            "y": "FTFRTXi0Am56ma9G1JAVjwHbCdtUvNesFGZ44c73T-HiKuz3Ww41LRg3--8Aoc2w"
        }
    ]
}
```

## Supported Algorithms

- none
- HS256
- HS384
- HS512
- RS256
- RS384
- RS512
- ES256
- ES384
- ES512 (Currently not supported by SoftHSMv2)

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
