# oxd-https-extension

## overview

oxd-https-extension is the bridge between the oxd-server and the client application. It is secure translator for https to socket and vice versa. The following documentation demonstrates how to use this application to use Gluu's commercial OAuth 2.0 client software,
[oxd](http://oxd.gluu.org), to send users from this app to oxd to OpenID Connect Provider (OP) for login. 
You can securely send users to any standard OP for login, including Google and the [free open source Gluu Server](http://gluu.org/gluu-server).

## Requirements

[The Gluu Server](https://www.gluu.org/) is a free open source access management platform. You can point custom developed, open source, off-the-shelf, and SaaS applications at your Gluu Server to leverage a single identity and authentication infrastructure. If you haven’t already setup your Gluu server you can follow the [installation guide](https://gluu.org/docs/ce/installation-guide/) to do so. Or you can simply use Google as your OP.

*Note: You could have a gluu server deployed on CentOS, RHEL, debian or even Ubuntu 16 etc.*

[The oxd server](https://gluu.org/docs/oxd/) is commercial OAuth 2.0 client software written by Gluu. It is a mediator that provides API’s that can be called by a web application that are easier than directly calling the API’s of an OpenID Connect Provider (OP), like the Gluu Server. oxd can be used with any standard OP including the Gluu Server and Google. [Get your oxd license](https://oxd.gluu.org/).

*Note:
 If you have apps that are coded in other languages or frameworks you can still use oxd. There are oxd libraries for Ruby, .Net, c#, Java and more.
Read the docs.*

## Prerequisite

```
 You have to install gluu server and oxd-server in your hosting server to use oxd-https-extension
 library with your application.
```

## Configuration

oxd-server must run on localhost and be bound to port: 8099 and oxd-https-extension configuration you can go ahead anc change settings in `oxd-https.yml` file

## Running Test Cases

```sh
mvn test
```

## Running Application

To package this application run the following from the root application directory : 
```sh
mvn package
```

To run the server : 
```sh
java -jar target/oxd-https-extension-3.1.4.Final.jar server oxd-https.yml
```

Open this URL in browser:
 ```sh
http://127.0.0.1:8080/health-check
```

## Endpoints

### Setup Client

*Non-normative example request*
```
POST /setup-client
{
    "op_host" : "https://ce-dev3.gluu.org",
    "authorization_redirect_uri": "https://client.example.org/",
    "scope" : ["openid","profile","email","uma_protection"],
    "grant_types":["authorization_code","client_credentials"]
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6",
        "op_host": "https://ce-dev3.gluu.org",
        "client_id": "@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387",
        "client_secret": "f436b936-03fc-433f-9772-53c2bc9e1c74",
        "client_registration_access_token": "d836df94-44b0-445a-848a-d43189839b17",
        "client_registration_client_uri": "https://ce-dev3.gluu.org/oxauth/restv1/register?client_id=@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387",
        "client_id_issued_at": 1501854943,
        "client_secret_expires_at": 1501941343
    }
}
```

### Get Client Token

*Non-normative example request*
```
POST /get-client-token
{
	"op_host" : "https://ce-dev3.gluu.org",
	"scope" : ["openid","profile","email","uma_protection"],
	"op_host": "https://ce-dev3.gluu.org",
	"client_id": "@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!A2BB.9AE6.5F14.B387",
	"client_secret": "f436b936-03fc-433f-9772-53c2bc9e1c74"
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "scope": "openid profile uma_protection email",
        "access_token": "b75434ff-f465-4b70-92e4-b7ba6b6c58f2",
        "expires_in": 299,
        "refresh_token": null
    }
}
```

### Register Site

*Non-normative example request*
```
POST /register-site
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"op_host" : "https://ce-dev3.gluu.org",
	"authorization_redirect_uri": "https://client.example.org/",
	"scope" : ["openid","profile","email","uma_protection"],
	"grant_types":["authorization_code","client_credentials"]
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6",
        "op_host": "https://ce-dev3.gluu.org"
    }
}
```

### Update Site

*Non-normative example request*
```
POST /update-site
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6",
	"scope" : ["openid","profile","email","uma_protection"]
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6"
    }
}
```

### Get Authorization Url

*Non-normative example request*
```
POST /get-authorization-url
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6",
	"scope" : ["openid","profile","email","uma_protection"]
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "authorization_url": "https://ce-dev3.gluu.org/oxauth/restv1/authorize?response_type=code&client_id=@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!8A36.24E1.97DE.F4EF&redirect_uri=https://192.168.200.95/&scope=openid+profile+email+uma_protection&state=473ot4nuqb4ubeokc139raur13&nonce=lbrdgorr974q66q6q9g454iccm"
    }
}
```

### Get Tokens By Code

*Non-normative example request*
Use the code and state obtained in the previous step to call this API to retrieve tokens.
```
POST /get-tokens-by-code
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6",
	"code" : "0b9f1518-15aa-47b2-9477-d4c607447e18",
	"state" :"6q1ec90hn6ui4ipigv91hrbodj"
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "access_token": "88bba7f5-961c-4b71-8053-9ab35f1ad395",
        "expires_in": 299,
        "id_token": "eyJraWQiOiI5MTUyNTU1Ni04YmIwLTQ2MzYtYTFhYy05ZGVlNjlhMDBmYWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2NlLWRldjMuZ2x1dS5vcmciLCJhdWQiOiJAITE3MzYuMTc5RS5BQTYwLjE2QjIhMDAwMSE4RjdDLkI5QUIhMDAwOCE5Njk5LkFFQzcuOTM3MS4yODA3IiwiZXhwIjoxNTAxODYwMzMwLCJpYXQiOjE1MDE4NTY3MzAsIm5vbmNlIjoiOGFkbzJyMGMzYzdyZG03OHU1OTUzbTc5MXAiLCJhdXRoX3RpbWUiOjE1MDE4NTY2NzIsImF0X2hhc2giOiItQ3gyZHo1V3Z3X2tCWEFjVHMzbUZBIiwib3hPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwic3ViIjoialNadE9rOUlGTmdLRTZUVVNGMHlUbHlzLVhCYkpic0dSckY5eG9JV2c4dyJ9.gi5tvt-duNygoDGjCqQqdKH6D6jJnpW5p6zYzxYiHtYecxkp8ks6AUJ4bmvkVHBd7a3vNbbFDY9Z3wsHGIMRXZRUXFVSQL1-JG0ye9zFH6Pp--Ky3Hexrl7V8PJ-AAFJwX3s854svIXugKNJMwPMmOvKcdzhhPgMBjh8GfVCpTW415iIBg2XcCmoq40zMIdya2WFeBy7IndcaoKcyUKQwqvtGfA53K3qe6RnKS_ps116n24RyBGypovLlThnoGdh20SZfaGVzoumRwW5-wBR6Iff97jgjx_SEOhhJK7Dr4dxliePd6H5ZtgUmFFoxm6Jyln9LKx-WrrUZRYNuFkh-w",
        "refresh_token": "33d7988e-6ffb-4fe5-8c2a-0e158691d446",
        "id_token_claims": {
            "at_hash": [
                "-Cx2dz5Wvw_kBXAcTs3mFA"
            ],
            "aud": [
                "@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!9699.AEC7.9371.2807"
            ],
            "sub": [
                "jSZtOk9IFNgKE6TUSF0yTlys-XBbJbsGRrF9xoIWg8w"
            ],
            "auth_time": [
                "1501856672"
            ],
            "iss": [
                "https://ce-dev3.gluu.org"
            ],
            "exp": [
                "1501860330"
            ],
            "iat": [
                "1501856730"
            ],
            "nonce": [
                "8ado2r0c3c7rdm78u5953m791p"
            ],
            "oxOpenIDConnectVersion": [
                "openidconnect-1.0"
            ]
        }
    }
}
```

### Get User Info

*Non-normative example request*
```
POST /get-user-info
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
    "oxd_id" : "bcad760f-91ba-46e1-a020-05e4281d91b6",
    "access_token" :"88bba7f5-961c-4b71-8053-9ab35f1ad395"
}
```

*Non-normative example response*
```
{
  "claims": {
    "sub": [
      "N4tKFw2-ZCY5V7AaBgi2sGEgCGKtNX6--53aPnfEbNs"
    ],
    "zoneinfo": [
      "America/Chicago"
    ],
    "website": [
      "http://www.gluu.org"
    ],
    "birthdate": [
      "1983-1-6"
    ],
    "gender": [
      "Male"
    ],
    "profile": [
      "http://www.mywebsite.com/profile"
    ],
    "preferred_username": [
      "user"
    ],
    "middle_name": [
      "User"
    ],
    "locale": [
      "en-US"
    ],
    "given_name": [
      "Test"
    ],
    "picture": [
      "http://www.gluu.org/wp-content/uploads/2012/04/mike3.png"
    ],
    "updated_at": [
      "20170224125915.538Z"
    ],
    "nickname": [
      "user"
    ],
    "name": [
      "oxAuth Test User"
    ],
    "family_name": [
      "User"
    ]
  }
}
```

### Logout URL

*Non-normative example request*
```
POST /get-logout-uri
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
    "oxd_id" : "bcad760f-91ba-46e1-a020-05e4281d91b6"   
}
```

*Non-normative example response*
```
{
  "uri": "https://ce-dev.gluu.org/oxauth/seam/resource/restv1/oxauth/end_session?id_token_hint=eyJraWQiOiI1YmM2ZGM3MS0xYjA1LTQ5YzMtYWU3MC0zYTg4Y2ZiMjQwN2QiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2NlLWRldi5nbHV1Lm9yZyIsImF1ZCI6IkAhNUE1OC5BRTBELkQzODMuMUU0NiEwMDAxIUUzOEIuN0RCRSEwMDA4IUE3MTkuOTU4QS41QjdGLkVBQkMiLCJleHAiOjE0OTAwMTk5MjEsImlhdCI6MTQ5MDAxNjMyMSwibm9uY2UiOiJkNGdsbmtndHAxYWlqZ3JnY3V2cGp1N2k3cCIsImF1dGhfdGltZSI6MTQ5MDAxNjI3MiwiYXRfaGFzaCI6Im1Xa2NXQzZ6NC1qN0ZNX0ctX0tYMWciLCJveFZhbGlkYXRpb25VUkkiOiJodHRwczovL2NlLWRldi5nbHV1Lm9yZy9veGF1dGgvb3BpZnJhbWUiLCJveE9wZW5JRENvbm5lY3RWZXJzaW9uIjoib3BlbmlkY29ubmVjdC0xLjAiLCJzdWIiOiJONHRLRncyLVpDWTVWN0FhQmdpMnNHRWdDR0t0Tlg2LS01M2FQbmZFYk5zIn0.PvCdzPnMwqPNUw1bzd8tvzpJqYu-P2iCTnELr85ZaJTG8_Fdj3EruLgUBa-emeum3j29cFgdjFPx6WplfCV1GnehOieXjDiAAE85fy-stxXwII3xrva5ZjG0FnTYnJLoRmy0BWMjFC2IdCoISJI9imcfvmQmlvNmU0EjLS02cJf3JAaqEaM-FJWdQv8end9-Sq2bcp6ME3voRjV30ps_7jcDdlM_hW3M_e3RdrXYCDifbl_1jaNip5tb6_bLpgTADDoLT3fTvACRN057e2GCkSYdxvVhIjfDsjnOhk5n3TDcWedriu99H8-sNXyI_aBr3HAXd37CsgmdfIJcgUNJJw"
}
```

### Get Access Token By Refresh Token

*Non-normative example request*
```
POST /get-access-token-by-refresh-token
{
    "oxd_id" : "bcad760f-91ba-46e1-a020-05e4281d91b6",
    "refresh_token":"33d7988e-6ffb-4fe5-8c2a-0e158691d446", //refresh_token from get_tokens_by_code command
    "scope" : ["openid","profile","email","uma_protection"]
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "scope": "openid profile uma_protection email",
        "access_token": "14f95caa-1f5a-46f8-ae8c-069873591f67",
        "expires_in": 299,
        "refresh_token": "c6cbb8ec-1d36-4d06-bc4f-58c40214133e"
    }
}
```

### UMA RS Protect Resources

*Non-normative example request*
```
POST /uma-rs-protect
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6",
	"resources": [{
		"path": "/scim",
		"conditions": [{
			"httpMethods": ["GET"],
			"scopes": ["https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1"],
			"ticketScopes": ["https://scim-test.gluu.org/identity/seam/resource/restv1/scim/vas1"]
		}]
	}]

}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6"
    }
}
```


### UMA RS Check Access

*Non-normative example request*
```
POST /uma-rs-check-access
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"oxd_id": "bcad760f-91ba-46e1-a020-05e4281d91b6",
	"rpt":"",
	"path":"/scim",
	"http_method" : "GET"
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "access": "denied",
        "ticket": "e986fd2b-de83-4947-a889-8f63c7c409c0",
        "www-authenticate_header": "UMA realm=\"rs\",as_uri=\"https://ce-dev3.gluu.org\",error=\"insufficient_scope\",ticket=\"e986fd2b-de83-4947-a889-8f63c7c409c0\""
    }
}
```


### UMA RP - Get RPT

*Non-normative example request*
```
POST /uma-rp-get-rpt
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"oxd_id":"bcad760f-91ba-46e1-a020-05e4281d91b6",
	"ticket": "e986fd2b-de83-4947-a889-8f63c7c409c0"
}
```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "pct": "4f44136f-797d-4b70-aa4a-a4d5f96dad7c_86BA.DB48.64EE.52E2.1E48.828A.C4E6.7C82",
        "updated": false,
        "access_token": "656b0f54-bf05-4ec8-aa95-b81b7c9bfb7a_1649.62A5.396A.3D67.B24F.74E9.2254.E4EF",
        "token_type": "Bearer"
    }
}
```


### UMA RP Get Claims Gathering Url

*Non-normative example request*
```
POST /uma-rp-get-claims-gathering-url
Authorization: Bearer b75434ff-f465-4b70-92e4-b7ba6b6c58f2
{
	"oxd_id":"bcad760f-91ba-46e1-a020-05e4281d91b6",
	"ticket": "fba00191-59ab-4ed6-ac99-a786a88a9f40",
	"claims_redirect_uri":"https://client.example.com/cb",
	"protection_access_token": "b75434ff-f465-4b70-92e4-b7ba6b6c58f2"
}

```

*Non-normative example response*
```
{
    "status": "ok",
    "data": {
        "url": "https://ce-dev3.gluu.org/oxauth/restv1/uma/gather_claims?client_id@!1736.179E.AA60.16B2!0001!8F7C.B9AB!0008!4508.BF20.9B81.E904&ticket=fba00191-59ab-4ed6-ac99-a786a88a9f40&claims_redirect_uri=https://client.example.com/cb&state=d871gpie16np0f5kfv936sc33k",
        "state": "d871gpie16np0f5kfv936sc33k"
    }
}
```

 