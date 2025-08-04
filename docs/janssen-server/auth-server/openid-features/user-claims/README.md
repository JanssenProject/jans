---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - claims
  - attributes
  - custom claims
  - user claims
---

# User Claims

Claim is a piece of information asserted about an Entity. User claims refer to pieces of information about the authenticated user, such as their name, email address, date of birth, and more. These claims provide the RP with specific attributes or characteristics associated with the user. The claims are issued by the IDP after successful authentication and are included in the ID Token (which is a JSON Web Token (JWT) that contains user-related information) and are also available through the
`/userinfo` endpoint. 



!!! Note
    `Attribute` and `User Claims` refer to the same user data fields in 
    the Janssen Server.

## Types of User Claims

### Standard Claims 

The Janssen Server includes all standard claims defined in [OpenID Connect specifications](https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims). Built-in claims defines a standard set of pre-defined claims available to use after installation for sharing of user attributes between identity providers and relying parties. 

The built-in user claims in Janssen are listed in below table

|Display Name|Claim Name|Description|
|---|---|---|
|sub|sub|	Subject - Identifier for the End-User at the Issuer.The authorization server will always send this claim.|
|Username|user_name|Username of user| 
|Password|user_password|Password of user|
|First Name|given_name|First name of user|
|Middle Name|middle_name|Middle name of user|
|Last Name|family_name|Last name of user|
|Display Name|name|Display name of user|
|Email|email|Email address of user|
|Nickname|nickname|Nickname used for user|
|CIBA Device Registration Token|jans_backchannel_device_registration_tkn|CIBA Device Registration Token|
|CIBA User code|jans_backchannel_usr_code|CIBA User code|
|Locale|locale|End-User's locale, represented as a BCP47 (RFC5646) language tag|      
|Website URL|website|URL of the End-User's Web page or blog| 
|IMAP Data|imap_data|IMAP data|   
|jansAdminUIRole|jansAdminUIRole|Gluu Flex Admin UI role|
|Enrollment code|jans_enrollment_code|Enrollment code|
|Preferred Language|preferred_language|Preferred language|
|Profile URL|profile|Profile URL|
|Secret Question|secret_question|Secret question used to verify user identity|
|Email Verified|email_verified|Is user's email verified?|
|Birthdate|birthdate|Baithdate of user|   
|Time zone info|zoneinfo|The End-User's time zone|
|Phone Number verified|phone_number_verified|Is user's phone number verified?|
|Home Telephone Number|phone_number|User's phone number|
|Preferred Username|preferred_username|A domain issued and managed identifier for the person|
|TransientId|transient_id|...| 
|PersistentId|persistent_id|...|
|Country|country|User's country|     
|Secret Answer|secret_answer|Secret answer used to verify user identity|
|OpenID Connect JSON formatted address|address|End-User's preferred postal address. The value of the address member is a JSON structure containing some or all of the members defined in OpenID Connect 1.0 Core Standard Section 5.1.1|
|User certificate|user_certificate|User certificate|
|Organization|o|Organization|
|Picture URL|picture|User's picture url| 
|gender, male or female|gender|	End-User's gender. Values defined by this specification are female and male. Other values MAY be used when neither of the defined values are applicable.|
|Last Updated|updated_at|Time the End-User's information was last updated. Its value is a JSON number representing the number of seconds from 1970-01-01T00:00:00Z as measured in UTC until the date/time.|


### Custom Claims

In addition to standard claims, custom claims are also allowed to be defined by the 
Janssen Server. These claims provide flexibility to include application-specific
 user attributes that are not covered by the standard claims. 
 Custom claims can provide additional context or information needed by the RP.

 Refer to [claims configuration document](../../../config-guide/auth-server-config/attribute-configuration.md) for more details on custom claims configuration.