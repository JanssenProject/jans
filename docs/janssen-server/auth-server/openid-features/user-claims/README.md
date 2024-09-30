---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - claims
---

# User Claims

Claim is a piece of information asserted about an Entity. User claims refer to pieces of information about the authenticated user, such as their name, email address, date of birth, and more. These claims provide the RP with specific attributes or characteristics associated with the user. The claims are issued by the IDP after successful authentication and are included in the ID Token (which is a JSON Web Token (JWT) that contains user-related information) and are also available through the
`/userinfo` endpoint. 

## Types of User Claims

### Standard Claims 

These are predefined and standardized claims that provide basic user information. Some examples of standard claims include:

- `sub`: Subject identifier, a unique ID for the user.
- `name`: User's full name.
- `email`: User's email address.
- `birthdate`: User's date of birth.
- `preferred_username`: User's preferred username or nickname.

### Custom Claims

In addition to standard claims, custom claims are also allowed to be defined by the IDP. These claims provide flexibility to include application-specific user attributes that are not covered by the standard claims. Custom claims can provide additional context or information needed by the RP.