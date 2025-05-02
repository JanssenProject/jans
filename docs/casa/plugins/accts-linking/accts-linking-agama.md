---
tags:
- Casa
- Accounts Linking
- Agama
---

# Accounts linking project configuration

## Overview

The accounts linking Agama project must be configured in order to integrate third-party identity providers. The configuration of this project is supplied in a JSON file whose structure is like:

```
{
"io.jans.casa.authn.acctlinking": {
    
    "providerID_1": { ... },
    "providerID_2": { ... },
    ...
}
}
```

Each property part of the JSON object `io.jans.casa.auth.acctlinking` holds the configuration of a different identity provider. Here's a how a typical configuration of a provider looks like:

```
{
    "displayName": "Goooogle",
    "flowQname": "io.jans.inbound.GenericProvider",
    "mappingClassField": "io.jans.casa.acctlinking.Mappings.GOOGLE",    
    "oauthParams": {
        "authzEndpoint": "https://accounts.google.com/o/oauth2/v2/auth",
        "tokenEndpoint": "https://oauth2.googleapis.com/token",
        "userInfoEndpoint": "https://www.googleapis.com/oauth2/v3/userinfo",
        "clientId": "202403151302",
        "clientSecret": "m|a1l2d3i4t5a6S7h8a9k0i'r¿a",
        "scopes": ["email", "profile"]
    }
}
   
```         

In this case, we are populating the configuration of an OAuth-based provider called "Goooogle". 

The tables shown in [this](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/docs/agama-catalog/jans/inboundID/README.md#supply-configurations) page list all possible properties to configure a provider. Particularly, two properties deserve the most detail:

1. `flowQname`. Agama projects are made up of flows - think of small "web journeys". This property must contain the name of an existing flow capable of interfacing with the identity provider of interest. Often, there is no need to write such "interfacing" flow. The below are ready-to-use and cover most of real-world cases, specifically  OpenId/OAuth providers that support the **authorization code grant** (see section 1.3 of [rfc6749](https://www.ietf.org/rfc/rfc6749)):

    - `io.jans.inbound.GenericProvider`. It implements the authorization code flow where the user's browser is taken to the external site. When authentication completes, a `code` is received at a designated redirect (callback) URL. With such `code` an access token is obtained as well as user's profile data. This flow supports _dynamic client registration_

    - `io.jans.inbound.Apple`. It implements the authorization code flow with some nuances required in order to integrate "Apple Sign In"
    

2. `mappingClassField`. This is key for performing the attribute mapping process and the user provisioning. The remainder of this document is dedicated to these two aspects

!!! Note
    Recall `enabled` is a handy property that can be used to temporarily "deactive" a given identity provider.

## Configuring attribute mappings

An introduction to attribute mapping can be found [here](../../../agama-catalog/jans/inboundID/README.md#attribute-mappings). Unless an elaborated processing of attributes is required, a basic knowledge of Java language suffices to write a useful mapping.

To write a mapping, you can use the samples provided as a guideline (see folder `lib/io/jans/casa/acctlinking` in the Agama accounts linking project). You can add your mapping in the same file or create a new Java class for this purpose.  Then save your changes, re-package (zip) the project, re-deploy, and update (re-import) the configuration if necessary.

Specifically, for Casa accounts linking, the mapping **must** include an attribute named `ID`. While `ID` is not part of the Jans database, here it is used to supply what could be understood as the _identifier_ of the user at the external site. For instance, in a social site this may be the username or email. The example below shows how to set `ID` assuming the username was returned by the external site in an attribute named `userId`:

```
profile -> {
    Map<String, Object> map = new HashMap<>();
    
    map.put("ID", profile.get("userId"));
    ...
    return map;
}
```

In the above example, `profile` is a `Map<String, Object>` that holds the attribute-value pairs the third-party identity provider released for this user. For the interested, `profile` contents are dumped to the server logs (check `jans-auth_script.log`) so it is easy to peak into the values. Check for a message in `debug` level starting with "Profile data". 
    
Both the ID of identity provider and the ID of the user will end up stored in an auxiliary database attribute. This helps to identify if the incoming user is already known (has been onboarded previously).

When the attribute mapping is applied, the `uid` attribute is set as well. This is the username the incoming user will be assigned in the local Jans database. The `uid` is automatically generated based on `ID` unless the mapping already populates the `uid` directly.

The return value of the mapping is a `Map<String, Object>`. This caters for cases where resulting attributes hold booleans, dates, numbers, strings, etc. When the attribute has to hold multiple values, you can use an array or a Java `Collection` object, like a `List`.      

## User provisioning

After attribute mapping occurs, the local database is checked to determine if the incoming user is known (based on the `ID` in the mapping and the ID of the provider in question). If no match is found, the user is onboarded: a new entry is created in the database using the information found in the resulting mapping. Otherwise, the exact behavior varies depending on the provider configuration as follows:

- If `skipProfileUpdate` is set to `false`, the existing database entry is left untouched, otherwise:
- If `cumulativeUpdate` is set to `false`, the existing attributes in the entry which are part of the mapping are overwritten
- If `cumulativeUpdate` is set to `true`, the existing attribute values in the entry are preserved and new values are added if present in the mapping

The updates just referenced apply to the matching entry based on mapping and provider ID, however, when `emailLinkingSafe` is set to `true` and the mapping comes with a `mail` value equal to an existing e-mail in the database, the update is carried over the e-mail matching entry.
