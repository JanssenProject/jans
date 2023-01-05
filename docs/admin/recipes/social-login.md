---
tags:
  - administration
  - Social login
  - Google
  - Apple
  - Facebook
---

## Implementing Social logins
You can use a `PersonAuthenticationType` script to allow users to sign using credentials from popular **Social Identity providers** or **Inbound Identity Providers** like Facebook, Google and Apple. After users authenticate, we provision their Social Identity Provider credentials into the Jans-auth server. No additional username, password, credentials are needed for this user.

1. Facebook
2. [Google](../../../script-catalog/person-authentication/google-external-authenticator/README.md)
3. [Apple](../../../script-catalog/person-authentication/apple-external-authenticator/README.md)

Following is a high-level diagram depicting a typical flow - user authentication on a Social Identity Platform and subsequent user provisioning on Jans-Auth server.

You can copy paste this sequence in [https://sequencediagram.org/](https://sequencediagram.org/)
```
title Social login
Jans AS<-User agent: 1. Invoke /authorize endpoint
Jans AS->User agent: 2. Discovery: Present list of remote IDPs (Google, Apple, FB...)
User agent->Jans AS: 3. Select IDP (e.g. click on button)
Jans AS->Social login Identity Provider: 4. Redirects login request to IDP

loop n times - (multistep authentication)
Social login Identity Provider->User agent: 5. present login screen
User agent->Social login Identity Provider: 6. present credentials
end

Social login Identity Provider->Jans AS: 7. return id_token, user claims
Jans AS->Jans AS: 8. validate id_token,\ncreate internal Jans session
opt if new user
  Jans AS->Jans AS: 9. Dynamic enrollment or registration
end

Jans AS->User agent: 10. write Jans session cookie
```
![Social Sign-In](https://github.com/JanssenProject/jans/raw/main/docs/assets/SocialSignIn.png)

### User provisioning

After a user has logged in at an external provider a new record is added in local LDAP - or updated if the user is known.

To determine if a user was already added, a string is composed with the provider name and the user ID. For example, if user "MrBrown123" has logged in at Twitter, the string would look like `passport-twitter:mrbrown123`. An LDAP search is performed for a match in the people branch for an entry where attribute `jansExtUid` equals `passport-twitter:mrbrown123`.

If there are no matches, an entry is added using the values received from the external provider (after having applied the corresponding attribute mapping) attaching the computed value for `jansExtUid`. The user profile can contain single or multivalued attributes.

📝 The prefix `passport-<provider-name>` is used to keep the code compatible with the Passport.js implementation for Inbound Identity
