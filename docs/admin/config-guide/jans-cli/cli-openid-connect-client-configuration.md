---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

# OpenID Connect Configuration

> Prerequisite: Know how to use the Janssen CLI in [command-line mode](cli-index.md)

Let's get the information of OpenID Connect Client Configuration:

```text
/opt/jans/jans-cli/config-cli.py --info OAuthOpenIDConnectClients



Operation ID: get-oauth-openid-clients
  Description: Gets list of OpenID Connect clients
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
Operation ID: post-oauth-openid-clients
  Description: Create new OpenId connect client
  Schema: /components/schemas/Client
Operation ID: put-oauth-openid-clients
  Description: Update OpenId Connect client.
  Schema: /components/schemas/Client
Operation ID: get-oauth-openid-clients-by-inum
  Description: Get OpenId Connect Client by Inum.
  url-suffix: inum
Operation ID: delete-oauth-openid-clients-by-inum
  Description: Delete OpenId Connect client.
  url-suffix: inum
Operation ID: patch-oauth-openid-clients-by-inum
  Description: Update modified properties of OpenId Connect client by Inum.
  url-suffix: inum
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest

```

## Get List of OpenID Clients

To get the openid clients, run the following command:

```text
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-openid-clients




Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
[
  {
    "dn": "inum=1801.30bd0499-9dc0-48dc-9eb3-96b80a8da856,ou=clients,o=jans",
    "inum": "1801.30bd0499-9dc0-48dc-9eb3-96b80a8da856",
    "clientSecret": "zITPCsgIfmDTkKWkonuu+g==",
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": null,
    "claimRedirectUris": null,
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "refresh_token",
      "client_credentials"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "Jans Config Api Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "pairwise",
    "idTokenSignedResponseAlg": "RS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=CACA-B9D4,ou=scopes,o=jans",
      "inum=CACA-5AA4,ou=scopes,o=jans",
      "inum=CACA-F1E3,ou=scopes,o=jans",
      "inum=CACA-A1BD,ou=scopes,o=jans",
      "inum=CACA-113F,ou=scopes,o=jans",
      "inum=CACA-22E5,ou=scopes,o=jans",
      "inum=CACA-E6DE,ou=scopes,o=jans",
      "inum=CACA-B965,ou=scopes,o=jans",
      "inum=CACA-7FB9,ou=scopes,o=jans",
      "inum=CACA-3B0C,ou=scopes,o=jans",
      "inum=CACA-FD1D,ou=scopes,o=jans",
      "inum=CACA-7419,ou=scopes,o=jans",
      "inum=CACA-55A1,ou=scopes,o=jans",
      "inum=CACA-7B22,ou=scopes,o=jans",
      "inum=CACA-66AE,ou=scopes,o=jans",
      "inum=CACA-8283,ou=scopes,o=jans",
      "inum=CACA-1A74,ou=scopes,o=jans",
      "inum=CACA-CCFC,ou=scopes,o=jans",
      "inum=CACA-EABC,ou=scopes,o=jans",
      "inum=CACA-E7BB,ou=scopes,o=jans",
      "inum=CACA-EF5F,ou=scopes,o=jans",
      "inum=CACA-179E,ou=scopes,o=jans",
      "inum=CACA-174C,ou=scopes,o=jans",
      "inum=CACA-B36D,ou=scopes,o=jans",
      "inum=CACA-88E3,ou=scopes,o=jans",
      "inum=CACA-C1F5,ou=scopes,o=jans",
      "inum=CACA-82B8,ou=scopes,o=jans",
      "inum=CACA-016F,ou=scopes,o=jans",
      "inum=CACA-8F20,ou=scopes,o=jans",
      "inum=CACA-79A1,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": true,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": "RS256",
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  },
  {
    "dn": "inum=1001.3c40746d-63a6-478e-b06d-8f49bb984e4f,ou=clients,o=jans",
    "inum": "1001.3c40746d-63a6-478e-b06d-8f49bb984e4f",
    "clientSecret": "eVXRaEojULdohgOUbMeFPA==",
    "frontChannelLogoutUri": "https://testjans.imshakil.me/identity/ssologout.htm",
    "frontChannelLogoutSessionRequired": true,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.imshakil.me/identity/scim/auth",
      "https://testjans.imshakil.me/identity/authcode.htm",
      "https://testjans.imshakil.me/jans-auth/restv1/uma/gather_claims?authentication=true"
    ],
    "claimRedirectUris": [
      "https://testjans.imshakil.me/jans-auth/restv1/uma/gather_claims"
    ],
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "implicit",
      "refresh_token"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "oxTrust Admin GUI",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "public",
    "idTokenSignedResponseAlg": "HS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": [
      "https://testjans.imshakil.me/identity/finishlogout.htm"
    ],
    "requestUris": null,
    "scopes": [
      "inum=F0C4,ou=scopes,o=jans",
      "inum=10B2,ou=scopes,o=jans",
      "inum=764C,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": true,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": null,
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": null,
    "jansId": null
  },
  {
    "dn": "inum=1202.049eb91f-6339-4e83-ac83-55df359f6c9c,ou=clients,o=jans",
    "inum": "1202.049eb91f-6339-4e83-ac83-55df359f6c9c",
    "clientSecret": null,
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": null,
    "claimRedirectUris": null,
    "responseTypes": null,
    "grantTypes": [
      "client_credentials"
    ],
    "applicationType": "native",
    "contacts": null,
    "clientName": "SCIM Requesting Party Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": "{  \"keys\" : [ {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"0b60383f-13c9-4064-9de1-7946724c0bbc_sig_rs256\",    \"x5c\" : [ \"MIIDCTCCAfGgAwIBAgIgLJXeu/MFKl144/y6Xj55fqA+RWTWE0VgEhOSb1CmITcwDQYJKoZIhvcNAQELBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTNaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCK7v3/S3Qn1puC16XM1mChQa3ygnAMoDQivlDj0AxLmSEO4ulmubVTbsvBFkt45+kKLvDUDozaNFhhNtnX1vZt37Fnd7/lnsODVn7GOrc8pGyiR048MfmPONO77LLqyf/ByrxhMpBYTR22kniRdQMc1+dHjWHIGzvmsQgMuefT2U81fqRpL0dkL2xDs7OEHm6BjQUoJgSXnf5BmWvdf+WiYPe5DXe6g56LdyZwwgN0vcx1IoYSMvmHlZyNjzyOPhCNgLPexXFpniBcFc5b5nGISgpn37yjVm4UIIMMGajv7jNJZKXkKZ+F4KRnuIByTYHwTqiHEwQoleRhlKJpNbthAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQsFAAOCAQEAhYEvDDBZ1Sl8b4Ng0aSXN+zw4nwS7pXBAwj4yLid47D6FnomFw25PYDAghO7YehDW18zjgbON69L5PH9Tqnq/Jzu1qAWpjWpwBVMRogCiGip/Kk59HmQos5/ckm9kgKrWhUw7vEqramHw40uqjXWuDfykWDbSqRYX2rccubSGwRsocMoMEoeFXLtyeBgjqoFY1Uqt4VTMdjTv6ekD+BLVfXOTlhemHSRXBG7GJVpwebYIyN/lx7LFAHYqbBi0adyGTI0/HQBtxMQeu57qy9oP+Q9gKse5QAz5Zesld71bKmUOshSGg4ks1JHH70wtRNPdQOgfdaaHHVzoyZo6FcMqg==\" ],    \"exp\" : 1642286363453,    \"alg\" : \"RS256\",    \"n\" : \"iu79_0t0J9abgtelzNZgoUGt8oJwDKA0Ir5Q49AMS5khDuLpZrm1U27LwRZLeOfpCi7w1A6M2jRYYTbZ19b2bd-xZ3e_5Z7Dg1Z-xjq3PKRsokdOPDH5jzjTu-yy6sn_wcq8YTKQWE0dtpJ4kXUDHNfnR41hyBs75rEIDLnn09lPNX6kaS9HZC9sQ7OzhB5ugY0FKCYEl53-QZlr3X_lomD3uQ13uoOei3cmcMIDdL3MdSKGEjL5h5WcjY88jj4QjYCz3sVxaZ4gXBXOW-ZxiEoKZ9-8o1ZuFCCDDBmo7-4zSWSl5CmfheCkZ7iAck2B8E6ohxMEKJXkYZSiaTW7YQ\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"b3c52773-7377-45a2-97f8-0e8cc3895342_sig_rs384\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAPXe8+Rao043PUbs+WlpDB17Gyq8osq3tl/4d2qb38eTMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTE0WhcNMjIwMTE1MjIzOTIzWjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0DJHMKIC9rsPUurwE9aRXJ0YCi0hUrfZJ7CP3bnE5dbjSFgO9jSpFT+BS8SEaEWhTMUsrLAXqANTqPfEz3ITWhVEHdDvBDrSrpjQQWcEksxYP4/ZaScnFg09yt6Y6U3UMzwPijlzvq84xsJ1KWaz2klCSWvb/jQ4RJj6SG4eTApX0A2cHJmHwJ1oM9SwQe+eeKprd+uZj12iouWPjah4ztz2PzzAmYh8l3Wlycw7hs5OQnxU2ZnygSMYh/2V5cKVK22FAp3fE3QxLXYmn4hkmSoHcy0UjRxhSS5Q8m4AcJzdfUauMqpIJ0yL/W9jkAVdsgTMcacjJF9eesVyhcwwgwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAJHB8bKmnQpllotN3dDg1heS2zqdsqyPK7K/5TFBRpXJV2LrIIAHEw5NjIh3vxva/dUwiJD/9uGpi3Xmn9wVFXhIxzYtSAGQmYxFEtkKsZZ7HmndmFbqbeHYv2q266yBQxx1GoZELyU85rrF+hB+/ZSdeMdqjq+Tyr25NwSHwDkGlYui3WAqLH0l0LqIvtSO5bgv6fpqhXe4H8PJ41EUsChN0HrIXNMJLbdvw8tnznJSMbaqKXjKCh0qr9GvHhxvZkDyklWRFTelg8Ct2xiH/eGeu2jwwc/QndcxNq0lqcFzyzp66oTUIdwrQitP9lgipB4c42jefoZjhv6mQaDbuO8=\" ],    \"exp\" : 1642286363453,    \"alg\" : \"RS384\",    \"n\" : \"0DJHMKIC9rsPUurwE9aRXJ0YCi0hUrfZJ7CP3bnE5dbjSFgO9jSpFT-BS8SEaEWhTMUsrLAXqANTqPfEz3ITWhVEHdDvBDrSrpjQQWcEksxYP4_ZaScnFg09yt6Y6U3UMzwPijlzvq84xsJ1KWaz2klCSWvb_jQ4RJj6SG4eTApX0A2cHJmHwJ1oM9SwQe-eeKprd-uZj12iouWPjah4ztz2PzzAmYh8l3Wlycw7hs5OQnxU2ZnygSMYh_2V5cKVK22FAp3fE3QxLXYmn4hkmSoHcy0UjRxhSS5Q8m4AcJzdfUauMqpIJ0yL_W9jkAVdsgTMcacjJF9eesVyhcwwgw\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"bf5b0a2f-c977-4e0d-9a3c-bd8faef18e48_sig_rs512\",    \"x5c\" : [ \"MIIDCTCCAfGgAwIBAgIgDrTQ+5YMX6eyx/WzSSJqS9gEsHlewOmswtphb3jE4/4wDQYJKoZIhvcNAQENBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTVaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCWfXYlVgDnZpXP7XdEfSYbmJbKgVh0VzhRUzoXc1UTK+FZRM4NNuQBwkfF1X1vMg5x+1Dp1fhNw6anzf5oRjkgog6hEOucWDXq9+jjlkJPnUrYD9/yrinnBQPjsv2NFxWu7qI3KYUIWe96blPiqO1pJjUPk6dybCYoNoxk/0ut07/9uXcf3qVawqypGz4FHeiVz3SUJ1P17h59CS0+nCBT5OkR+rhT4XNc6qcqO3YDX/mj1vahuJijztoQQN82xp31bod9KsBezHIpuW8aM+steNz/aOn49bLYbNxneXV032wPmTZHr0mxxIlS95Vux0y/FVMnt/D1/L5SbWV/SxVDAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAaEMcSaALKCCLjQ9GFyGQT3pOdT5AplJSD4ql+dISq44atxuDGSyrKyGehW07djBlUZFW8aDDMsOnQMjC049RU1LuU77FB9cmFhWAFCGIPTFFDdQrCK+LYB9LwSRX7kqBHsHZhqH9STdRMamakLnNuSJS5YzQNFziCIEUofkg0xe5WsAB4GdJrOfvy7JF0UnmjXhwpvZY/65b/Vv0o28j46QS4w769ltZwxIABKom0jdbfbn41UeLTlwgRftXh2/k59W5ma3lZPO/zi2aOl9nuj+7lXIUQKLoBUgDBYJ+8SyF0HhqDvlWijb29eJlPKKHkFRiQTo5Cbs704GWK8bx1A==\" ],    \"exp\" : 1642286363453,    \"alg\" : \"RS512\",    \"n\" : \"ln12JVYA52aVz-13RH0mG5iWyoFYdFc4UVM6F3NVEyvhWUTODTbkAcJHxdV9bzIOcftQ6dX4TcOmp83-aEY5IKIOoRDrnFg16vfo45ZCT51K2A_f8q4p5wUD47L9jRcVru6iNymFCFnvem5T4qjtaSY1D5OncmwmKDaMZP9LrdO__bl3H96lWsKsqRs-BR3olc90lCdT9e4efQktPpwgU-TpEfq4U-FzXOqnKjt2A1_5o9b2obiYo87aEEDfNsad9W6HfSrAXsxyKblvGjPrLXjc_2jp-PWy2GzcZ3l1dN9sD5k2R69JscSJUveVbsdMvxVTJ7fw9fy-Um1lf0sVQw\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-256\",    \"kid\" : \"fb0ef9a4-3b7f-4880-b896-57449de9ece8_sig_es256\",    \"x5c\" : [ \"MIIBfTCCASOgAwIBAgIgQgvjjFY0ZMqTJ3pbRsXCrIcHCdP64r+VwPgHUCzhTwgwCgYIKoZIzj0EAwIwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTVaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAT7B1VR+pnR8J0Omavpaeyq5K2aiJZXXQvuHn6piFZd7Gfr0rzzA9hSTgGZ84yOA96ZkV8XS71cuzP24Q72SsCKoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwIDSAAwRQIgPMWe6opagvCW0nkMASqpy7aQmnOw2cHFk8gqc7ztZyoCIQCK0xN9Kc3my9qGPYM75lUx2AwAzgyhkdWzo80jd+BVkA==\" ],    \"x\" : \"APsHVVH6mdHwnQ6Zq-lp7KrkrZqIllddC-4efqmIVl3s\",    \"y\" : \"Z-vSvPMD2FJOAZnzjI4D3pmRXxdLvVy7M_bhDvZKwIo\",    \"exp\" : 1642286363453,    \"alg\" : \"ES256\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-384\",    \"kid\" : \"7cc680c6-c7d5-4a5c-885e-1e591dc1511d_sig_es384\",    \"x5c\" : [ \"MIIBuTCCAUCgAwIBAgIgDPX0NX82/puI5AxdpOoQxPrsODbEGF3usqHUizJFvd4wCgYIKoZIzj0EAwMwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTZaFw0yMjAxMTUyMjM5MjNaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAAR88zjor1uRqZg+UFFF7VrUyPXrGlkojxw2WiJsk3AKr6IbZNhGasSxjLV24Gjoo8BJUdcwX4DcOufpspU0KBUCaNY0rJjV6UM8kiyqDCYoKW0UpKxx1eXwm5m1AmCjkOSjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDAwNnADBkAjAjyJsnKck1+hkXjAoN5PpLLwua4i6+KfW6fBeOXbwGjN7WkfJ595KstuPMI7GzP/ACMAaHxFdnih0lkfWJ6lwr3IXn4eon/yAskkN24DrK0Q9e1mJkrDU2uc3ybh796+f3IQ==\" ],    \"x\" : \"fPM46K9bkamYPlBRRe1a1Mj16xpZKI8cNloibJNwCq-iG2TYRmrEsYy1duBo6KPA\",    \"y\" : \"SVHXMF-A3Drn6bKVNCgVAmjWNKyY1elDPJIsqgwmKCltFKSscdXl8JuZtQJgo5Dk\",    \"exp\" : 1642286363453,    \"alg\" : \"ES384\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-521\",    \"kid\" : \"98011bc0-8566-41ec-a64b-e0fca1fb22a2_sig_es512\",    \"x5c\" : [ \"MIICBjCCAWegAwIBAgIhAM1ik4Lr1/favN6xSF65r92aemqYgpCMfLO9vVAtmOO4MAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTE2WhcNMjIwMTE1MjIzOTIzWjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQAZG1VCPrKI5D9iLqkibhaKHH/j3SmDjhr9em63SIztO6gFXtEFNW4Jqc7oTHHcOv6VpagxX5XTzLinhpUQRuzEFUAw39iGsIJbwvGWarrw5/OCZaKPNRVA/kzAf9dl0I17EMyvGP0ctm6t4qqY8PjqygjA2nBoZWwLnhZu9q54IrdT+6jJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBjAAwgYgCQgHydUf16d/5yvFP5NGzBHOYY7sQ5jV0i2ICC+Vdh02jVTcmaAy6f2uraa6eL5X9SrfiwtR9HvkMqB/svMzAv999mQJCAL3LrtobouAY/i4Hxvfgt/H9Sf5G47zbO5QJBoqkOA9Q1OG4paRIVSQ3d1iZFvSPLfmbqwXee0aq8H9CU192+y52\" ],    \"x\" : \"ZG1VCPrKI5D9iLqkibhaKHH_j3SmDjhr9em63SIztO6gFXtEFNW4Jqc7oTHHcOv6VpagxX5XTzLinhpUQRuzEFU\",    \"y\" : \"AMN_YhrCCW8Lxlmq68OfzgmWijzUVQP5MwH_XZdCNexDMrxj9HLZureKqmPD46soIwNpwaGVsC54WbvaueCK3U_u\",    \"exp\" : 1642286363453,    \"alg\" : \"ES512\"  } ]}",
    "sectorIdentifierUri": null,
    "subjectType": "public",
    "idTokenSignedResponseAlg": "HS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "private_key_jwt",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": null,
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": null,
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": null,
    "jansId": null
  },
  {
    "dn": "inum=1201.95be8034-0b72-4add-959c-3edf98b91af6,ou=clients,o=jans",
    "inum": "1201.95be8034-0b72-4add-959c-3edf98b91af6",
    "clientSecret": null,
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": null,
    "claimRedirectUris": null,
    "responseTypes": null,
    "grantTypes": [
      "client_credentials"
    ],
    "applicationType": "native",
    "contacts": null,
    "clientName": "SCIM Resource Server Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": "{  \"keys\" : [ {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"8e6d654d-2133-45b6-84c4-4fce267d6bee_sig_rs256\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAO4ZTRoknOI/s7Mq9hIT424qwd9tY05Ht2uSgz/CuWmbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTA5WhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsircewYbwIlSPYt6H3yFoxn26wSpij6sYsFHe6HcWkxSmbgkYSfTF5n+14PorBhsWzKDY+YODqIH9sNfZAjx3yX/VQpqlXn52Uwt1ZGb8tnwaNbbMkFW2848fqZXxtZHrBUVMbN+jiMec1tnI6ONIsDNJlBB4jJgE/wIThMtl5cmys9/RHmfr6YAnEVEZksFtyaDS3W4f3JsrbgWs1IYcY9MGeAQJ+OpXifb5D0qhSUrDjLBbKCOukvRf6Ue3U/Q4NaxpokHYhqbr/YA6jiZ2XPcJl53HKdpU4eO6V4HP0nuiVi7q1nQhb9f4cnuPnIKYaai759bozXTjByoki6YCQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAIyswl5l42DSzAJmJTIwNwJUDGORPPoj3CQlBsaT5pQ2Ykiqv1gqBLjNZw/Em1TYeD2Udm8Gjn5sPUxcwdiiH1zATYPyxk/uv4POkwQBw2n3L8OA4pl0d6So5HVzA1CS23Sy1CSKK0OiRVMym0TxzutYIJB4Usqg2KhgwJBzonoAHY8IXd9QvlV7+4Cb2gE4jpF8kUKgF3dCqQVZybDrEFQ0xf8bDPWp4CBTTVoHsyr/bn7UFpHO1FxKAWJglNa6cNOv1a/1QzjPK2OZGnYciHf/BT6SWRkJukRNs/O3jrfJJsw+LSlQSjpcZPMUwJ3+JJ5Eax41bseue1mLa1js2h4=\" ],    \"exp\" : 1642286359278,    \"alg\" : \"RS256\",    \"n\" : \"sircewYbwIlSPYt6H3yFoxn26wSpij6sYsFHe6HcWkxSmbgkYSfTF5n-14PorBhsWzKDY-YODqIH9sNfZAjx3yX_VQpqlXn52Uwt1ZGb8tnwaNbbMkFW2848fqZXxtZHrBUVMbN-jiMec1tnI6ONIsDNJlBB4jJgE_wIThMtl5cmys9_RHmfr6YAnEVEZksFtyaDS3W4f3JsrbgWs1IYcY9MGeAQJ-OpXifb5D0qhSUrDjLBbKCOukvRf6Ue3U_Q4NaxpokHYhqbr_YA6jiZ2XPcJl53HKdpU4eO6V4HP0nuiVi7q1nQhb9f4cnuPnIKYaai759bozXTjByoki6YCQ\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"10a2a7f6-cabf-41c0-9d62-37644f214e1c_sig_rs384\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAPAwaemYVwozpAJBHURxIh6oD+BjzEvhLegWbdsM7/6zMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTEwWhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1s5Kx0ltPJswSizHDNTA786pyU3bqT5dym0O/9RUs1ishsp76QjytsYdxRZzzaYB4CcEaVRj5ZIZQwt0JNqbfx/MPiMEvpJhZHEruZRc1EsE3kUCx6yInBIuHk6yKsdYoAKiHa0dJOMirTcElZyCCzq83QBKpYdX7kV+i29kF/lOSvPeAsizN8HSmB29Hhy30nB3GmcYwghHAXCSpG5g467iGBi+gOMxRm7g/Uj/WSYIDc8CyGyd6iHPK9smCyna5cCfRtuGTAf+/fnfiK5IFrGXvDK5ggM+cbKBope7RceSawN28kjVtt+gY6oLvI3JrV0V33qKTC30JnPJCxTZ4wIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAGT0S+ke7qG9b7VS553n2JLTB13kSlXrBoa1XX4NlhnHnWO/w/YTFAi3jGVmgCesTr+2XBBFAIdOD9bC397Ufi8xd3PEDhNQKpWThhJpasMJvsiVqnXRsmoN+j5sqtzyUwl1Dsk5zdtDvoV7zeJHJ3niEpS5in6Gw8jMakn84VTekvtQvG6NBWwjVwWc4awSN18YVhMJYbs5J1tTSOcXjdwPD4Ee0WQMBrKqLeo7b9W5/F2Jb3DbfgMhkikVA9jRowyLMgFpyMI6VPneMDWyadnS9YrJHeX1Ml6i3m0uN3Jvp817jhgYFS1L74p3gA9oO0Tin6wnS5EZdWp8kCfrCx4=\" ],    \"exp\" : 1642286359278,    \"alg\" : \"RS384\",    \"n\" : \"1s5Kx0ltPJswSizHDNTA786pyU3bqT5dym0O_9RUs1ishsp76QjytsYdxRZzzaYB4CcEaVRj5ZIZQwt0JNqbfx_MPiMEvpJhZHEruZRc1EsE3kUCx6yInBIuHk6yKsdYoAKiHa0dJOMirTcElZyCCzq83QBKpYdX7kV-i29kF_lOSvPeAsizN8HSmB29Hhy30nB3GmcYwghHAXCSpG5g467iGBi-gOMxRm7g_Uj_WSYIDc8CyGyd6iHPK9smCyna5cCfRtuGTAf-_fnfiK5IFrGXvDK5ggM-cbKBope7RceSawN28kjVtt-gY6oLvI3JrV0V33qKTC30JnPJCxTZ4w\"  }, {    \"kty\" : \"RSA\",    \"e\" : \"AQAB\",    \"use\" : \"sig\",    \"crv\" : \"\",    \"kid\" : \"35a9fa3e-56ac-4408-91c3-e959735222b9_sig_rs512\",    \"x5c\" : [ \"MIIDCjCCAfKgAwIBAgIhAKLT4VuuTD1hb4Gsdd6djKvblI8eSGoksOMt+l2OG01XMA0GCSqGSIb3DQEBDQUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTEwWhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAppg3PsDya/MqabJX27lWx/xy/2zWaSF9+AQ1el34ECdf5e2PGjZIY5Nsx4T2K5uPTz9gH1i/3x8ViCNCX+VGjmyU96LQrLqTP0p9/dI1/E9Llr1igVn8ryyMCf0i8o+y9wEuxaRtiSCtUx65KLzwiGefgZGd7UwrAFce6Hy7VvYDdx1z6AcsJt08CXdDUAIU/M5zq3JCfmpyMFQQHPQ6H6UlK8pFeAGxLNp4IUVmZgUaswnZiaKgglMBqVVOh7bGBIQbmjzbwnIOWVoyuZt6vRfdQUoduya+PwxjwkF4WCRNNJr0NRbMp2aXJJvAHLNPcDXr2pntg4Gb2s40DMuimwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQENBQADggEBAIjIpRhEg9fQ9DQXbe7wEtaKpxzTl/FM+BAriQiIek2ZYDy2ux+uHmbTPYwIGNyTpkeAkkqk2xFUjP0QYDo9Q9vqlrWz0mTXDoRDuqyFtHy/4n2xVtAmJSg5us4wFWyBSiSOWyOL3H7TUMlSFjVGLUkweSOTymM29mPYb6xRay/6f1q6jB28LtHCpWm15ZmHa2XdyZTb5WSSD480S7jXcF3ON48AasXBfRxZpZpigF6YLZbxqSU4829RjnexfMJWHy81hVeFH3L+7WUkASpZNfIGlzKSFDqVuK3RncMswCHgMcygdzjem6DDH71qUDpwMVGLIcVQ0ZSjQzFSpd6fKag=\" ],    \"exp\" : 1642286359278,    \"alg\" : \"RS512\",    \"n\" : \"ppg3PsDya_MqabJX27lWx_xy_2zWaSF9-AQ1el34ECdf5e2PGjZIY5Nsx4T2K5uPTz9gH1i_3x8ViCNCX-VGjmyU96LQrLqTP0p9_dI1_E9Llr1igVn8ryyMCf0i8o-y9wEuxaRtiSCtUx65KLzwiGefgZGd7UwrAFce6Hy7VvYDdx1z6AcsJt08CXdDUAIU_M5zq3JCfmpyMFQQHPQ6H6UlK8pFeAGxLNp4IUVmZgUaswnZiaKgglMBqVVOh7bGBIQbmjzbwnIOWVoyuZt6vRfdQUoduya-PwxjwkF4WCRNNJr0NRbMp2aXJJvAHLNPcDXr2pntg4Gb2s40DMuimw\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-256\",    \"kid\" : \"b05a91e8-aa4e-4f6c-860a-3b63f13b16da_sig_es256\",    \"x5c\" : [ \"MIIBfDCCASOgAwIBAgIgFbx/JYXagj82QeW+8XBk/FcdCinm/kX04q4tBOKiQ+gwCgYIKoZIzj0EAwIwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTFaFw0yMjAxMTUyMjM5MTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARNynAgBRUdqdd5/Os3gpG/Y/CozNptimxnUXdGcDrMrLFBOtwVrB6wYk69Z9U2iY6KPTmgxHQ/MxcHiJOsTfuOoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwIDRwAwRAIgeohxfVOK3qgJSNZRzk50PvjeZZ6sIJi1uAOlsaYEMpcCIGPLcDJYIaFfNVQbO/UZjymtiDpoZdb8U39GSanA6HfP\" ],    \"x\" : \"TcpwIAUVHanXefzrN4KRv2PwqMzabYpsZ1F3RnA6zKw\",    \"y\" : \"ALFBOtwVrB6wYk69Z9U2iY6KPTmgxHQ_MxcHiJOsTfuO\",    \"exp\" : 1642286359278,    \"alg\" : \"ES256\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-384\",    \"kid\" : \"30fc8067-9cbd-4a39-8621-6555815e046f_sig_es384\",    \"x5c\" : [ \"MIIBuzCCAUCgAwIBAgIgAeP45q0dJdlruXGW4aKW/728ttfGj31IHROMLnFa5OQwCgYIKoZIzj0EAwMwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MTFaFw0yMjAxMTUyMjM5MTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAARHgy8tqS+sfqcL4f0LrTnisvAN6QEgylRR/upFj9+FOc7b5eImEzhO+PMhTmNvbutWN+0pVhZ5IcBY9dFSDyBSs9lkWgUDgcKXyg7HCIhnC7CXQfwPKOzH7ZzoD/2D6SWjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDAwNpADBmAjEAp1Gof1uj66oNQsIvSaKBhZkgRoAIweQKVbvcXUTKr3P00HOZMdCrkhYwqXEmDmmzAjEA2sk385nNl/uyUzaW3gfciCxXAeMXUQUjmp6ZDrpuPDleL6jo1u6hoURO30EXBNRR\" ],    \"x\" : \"R4MvLakvrH6nC-H9C6054rLwDekBIMpUUf7qRY_fhTnO2-XiJhM4TvjzIU5jb27r\",    \"y\" : \"VjftKVYWeSHAWPXRUg8gUrPZZFoFA4HCl8oOxwiIZwuwl0H8Dyjsx-2c6A_9g-kl\",    \"exp\" : 1642286359278,    \"alg\" : \"ES384\"  }, {    \"kty\" : \"EC\",    \"use\" : \"sig\",    \"crv\" : \"P-521\",    \"kid\" : \"4c1c8652-06a0-4203-bec7-f5e1408f9a71_sig_es512\",    \"x5c\" : [ \"MIICBTCCAWegAwIBAgIhAOSKu6QwZhmEMffavHu0TX9xI23MKmwdmhS3iFnklzJrMAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTExWhcNMjIwMTE1MjIzOTE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQA79ze+ti5XVPYSVjd5j/pFDMftJC/yKZ67UXMF4hGKYPOpyntzg2DpObgmYwnyituSmE+Nk04aYyMb9wDYPtAKywA4K+G+8M4i3oQ3u2fxxIEcd/k1hl63rAJwaRCHHYSeUuHkDs90aYNkwTotuOta2+IVzLHTFtut78Ifejy41yqG76jJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBiwAwgYcCQSyYB5WGd7N0eutyN4VZFnhivJczXshpkYuz5/XX1HmZwNIwBtc1nTSTclQyZNNNPsadKHW3zrpLmuh7ZJtkAQwjAkIBHOUe7iZNtzzz9o3JX6c3a0GtAkNdV3fDB523Xp+jq6coUzzbUo0qX3tD01iKrxtHh/RY3C7GYNRTOndw9G7efnE=\" ],    \"x\" : \"AO_c3vrYuV1T2ElY3eY_6RQzH7SQv8imeu1FzBeIRimDzqcp7c4Ng6Tm4JmMJ8orbkphPjZNOGmMjG_cA2D7QCss\",    \"y\" : \"AOCvhvvDOIt6EN7tn8cSBHHf5NYZet6wCcGkQhx2EnlLh5A7PdGmDZME6LbjrWtviFcyx0xbbre_CH3o8uNcqhu-\",    \"exp\" : 1642286359278,    \"alg\" : \"ES512\"  } ]}",
    "sectorIdentifierUri": null,
    "subjectType": "public",
    "idTokenSignedResponseAlg": "HS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "private_key_jwt",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=6D99,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": null,
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": null,
    "jansId": null
  }
]
```

It will show all the openid clients together. To search using parameters:

```text
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-openid-clients --endpoint-args limit:2
```

It will show two OpenID clients randomly.

```text

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
Calling with params limit=2
[
  {
    "dn": "inum=1801.d361f68d-8200-4ba2-a0bb-ca7fea79e805,ou=clients,o=jans",
    "inum": "1801.d361f68d-8200-4ba2-a0bb-ca7fea79e805",
    "clientSecret": "KfwZeAfq4jrL",
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.gluu.com/admin-ui",
      "http//:localhost:4100"
    ],
    "claimRedirectUris": null,
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "refresh_token",
      "client_credentials"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "Jans Config Api Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "pairwise",
    "idTokenSignedResponseAlg": "RS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=1800.F6E877,ou=scopes,o=jans",
      "inum=1800.D4F3E7,ou=scopes,o=jans",
      "inum=1800.2FD7EF,ou=scopes,o=jans",
      "inum=1800.97B23C,ou=scopes,o=jans",
      "inum=1800.8FC2C7,ou=scopes,o=jans",
      "inum=1800.1FFDF2,ou=scopes,o=jans",
      "inum=1800.5CF44C,ou=scopes,o=jans",
      "inum=1800.CCA518,ou=scopes,o=jans",
      "inum=1800.E62D6E,ou=scopes,o=jans",
      "inum=1800.11CB33,ou=scopes,o=jans",
      "inum=1800.781FA2,ou=scopes,o=jans",
      "inum=1800.ADAD8F,ou=scopes,o=jans",
      "inum=1800.40F22F,ou=scopes,o=jans",
      "inum=1800.7619BA,ou=scopes,o=jans",
      "inum=1800.E0DAF5,ou=scopes,o=jans",
      "inum=1800.7F45B0,ou=scopes,o=jans",
      "inum=1800.778C57,ou=scopes,o=jans",
      "inum=1800.E39293,ou=scopes,o=jans",
      "inum=1800.939483,ou=scopes,o=jans",
      "inum=1800.0ED2E8,ou=scopes,o=jans",
      "inum=1800.66CA59,ou=scopes,o=jans",
      "inum=1800.A4DBE5,ou=scopes,o=jans",
      "inum=1800.9AF358,ou=scopes,o=jans",
      "inum=1800.478CCF,ou=scopes,o=jans",
      "inum=1800.450A9A,ou=scopes,o=jans",
      "inum=1800.27A193,ou=scopes,o=jans",
      "inum=1800.3971D5,ou=scopes,o=jans",
      "inum=1800.891693,ou=scopes,o=jans",
      "inum=1800.A35DFD,ou=scopes,o=jans",
      "inum=1800.3516DE,ou=scopes,o=jans",
      "inum=F0C4,ou=scopes,o=jans",
      "inum=764C,ou=scopes,o=jans",
      "inum=10B2,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": true,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": "RS256",
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  },
  {
    "dn": "inum=1001.0e964ce7-7670-44a4-a2d1-d0a5f689a34f,ou=clients,o=jans",
    "inum": "1001.0e964ce7-7670-44a4-a2d1-d0a5f689a34f",
    "clientSecret": "4OJLToBXav0P",
    "frontChannelLogoutUri": "https://testjans.gluu.com/identity/ssologout.htm",
    "frontChannelLogoutSessionRequired": true,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.gluu.com/identity/scim/auth",
      "https://testjans.gluu.com/identity/authcode.htm",
      "https://testjans.gluu.com/jans-auth/restv1/uma/gather_claims?authentication=true"
    ],
    "claimRedirectUris": [
      "https://testjans.gluu.com/jans-auth/restv1/uma/gather_claims"
    ],
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "implicit",
      "refresh_token"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "oxTrust Admin GUI",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "public",
    "idTokenSignedResponseAlg": "HS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": [
      "https://testjans.gluu.com/identity/finishlogout.htm"
    ],
    "requestUris": null,
    "scopes": [
      "inum=F0C4,ou=scopes,o=jans",
      "inum=10B2,ou=scopes,o=jans",
      "inum=764C,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": true,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": null,
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  }
]
```


## Creating a New OpenID Clients

We can create openid clients as well, Let's see the description. It has a schema file where is defined the properties needs to be filled to create a new openid clients.

```
Operation ID: post-oauth-openid-clients
  Description: Create new OpenId connect client
  Schema: /components/schemas/Client
```

To get the schema file:

```
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/Client
```
It contains a lot of properties. But, It's not important to fill each of these properties. We are going to fill these properties:

- frontChannelLogoutSessionRequired[false, true]
- applicationType[web, native]
- clientName
- subjectType[pairwise, public]
- includeClaimsInIdToken[false, true]

```
{
  "dn": null,
  "inum": null,
  "clientSecret": null,
  "frontChannelLogoutUri": null,
  "frontChannelLogoutSessionRequired": false,
  "registrationAccessToken": null,
  "clientIdIssuedAt": null,
  "clientSecretExpiresAt": null,
  "redirectUris": [
    "https://client.example.org/cb"
  ],
  "claimRedirectUris": [],
  "responseTypes": [],
  "grantTypes": [],
  "applicationType": "web",
  "contacts": [],
  "clientName": "testOIDC",
  "idTokenTokenBindingCnf": null,
  "logoUri": null,
  "clientUri": null,
  "policyUri": null,
  "tosUri": null,
  "jwksUri": null,
  "jwks": "{ \"keys\" : [ { \"e\" : \"AQAB\", \"n\" : \"gmlDX_mgMcHX..\" ] }",
  "sectorIdentifierUri": null,
  "subjectType": "public",
  "idTokenSignedResponseAlg": "HS512",
  "idTokenEncryptedResponseAlg": "RSA1_5",
  "idTokenEncryptedResponseEnc": "A256GCM",
  "userInfoSignedResponseAlg": "PS256",
  "userInfoEncryptedResponseAlg": "RSA1_5",
  "userInfoEncryptedResponseEnc": "A128GCM",
  "requestObjectSigningAlg": "PS512",
  "requestObjectEncryptionAlg": "A128KW",
  "requestObjectEncryptionEnc": "A256CBC+HS512",
  "tokenEndpointAuthMethod": "tls_client_auth",
  "tokenEndpointAuthSigningAlg": "PS384",
  "defaultMaxAge": 1000000,
  "requireAuthTime": true,
  "defaultAcrValues": [],
  "initiateLoginUri": null,
  "postLogoutRedirectUris": [
    "https://client.example.org/logout/page1",
    "https://client.example.org/logout/page2",
    "https://client.example.org/logout/page3"
  ],
  "requestUris": [],
  "scopes": [
    "read write dolphin"
  ],
  "claims": [],
  "trustedClient": false,
  "lastAccessTime": null,
  "lastLogonTime": null,
  "persistClientAuthorizations": false,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": 100000000,
  "accessTokenLifetime": 100000000,
  "customAttributes": {
    "name": "name, displayName, birthdate, email",
    "multiValued": false,
    "values": []
  },
  "customObjectClasses": [],
  "rptAsJwt": false,
  "accessTokenAsJwt": false,
  "accessTokenSigningAlg": "ES384",
  "disabled": false,
  "authorizedOrigins": [],
  "softwareId": "4NRB1-0XZABZI9E6-5SM3R",
  "softwareVersion": "2.1",
  "softwareStatement": null,
  "attributes": {
    "tlsClientAuthSubjectDn": null,
    "runIntrospectionScriptBeforeJwtCreation": true,
    "keepClientAuthorizationAfterExpiration": true,
    "allowSpontaneousScopes": false,
    "spontaneousScopes": [],
    "spontaneousScopeScriptDns": [],
    "backchannelLogoutUri": [],
    "backchannelLogoutSessionRequired": true,
    "additionalAudience": [],
    "postAuthnScripts": [],
    "consentGatheringScripts": [],
    "introspectionScripts": [],
    "rptClaimsScripts": []
  },
  "backchannelTokenDeliveryMode": "ping",
  "backchannelClientNotificationEndpoint": null,
  "backchannelAuthenticationRequestSigningAlg": "PS384",
  "backchannelUserCodeParameter": false,
  "expirationDate": null,
  "deletable": false,
  "jansId": null
}
```
I have changed few things only here to show how to create an OpenID Connect Client. Please make sure that you filled each of the required properties to work the client.


