## Implementing Social logins 
You can use a `PersonAuthenticationType` script to allow users to sign using credentials from popular **Social Identity providers** or **Inbound Identity Providers** like Facebook, Google and Apple. After users authenticate, we provision their Social Identity Provider credentials into the Jans-auth server. No additional username, password, credentials are needed for this user.
1. Facebook
2. [Google](https://github.com/maduvena/jans-docs/wiki/Google-Authentication-Script)
3. Apple

Following is a high-level diagram depicting a typical flow - user authentication on a Social Identity Platform and subsequent user provisioning on Jans-Auth server. 

You can copy paste this sequence in [https://sequencediagram.org/](https://sequencediagram.org/)
```
title Social login sign-in 
Jans AS<-User agent: Invoke /authorize endpoint
Jans AS->User agent: Present login screen with button to enable to sing-in to Social login site(e.g Sign in to Google)
User agent->Jans AS: click on Sign-in button
Jans AS->Social login Identity Provider:Redirects login request to IDP

loop n times - (multistep authentication)
Social login Identity Provider->User agent:present login screen
User agent->Social login Identity Provider:present credentials
end

Social login Identity Provider->Jans AS: return ID token
Jans AS->Jans AS: validate the ID token, persist unique user identifier (primary key) in JANS
Jans AS->User agent:create user session
```
![Social Sign-In](https://github.com/maduvena/jans-docs/blob/main/images/SocialSignIn.png)

### How user provisioning works

After a user has logged in at an external provider a new record is added in local LDAP - or updated if the user is known.

To determine if a user was already added, a string is composed with the provider name and the user ID. For example, if user "MrBrown123" has logged in at Twitter, the string would look like `passport-twitter:mrbrown123`. An LDAP search is performed for a match in the people branch for an entry where attribute `jansExtUid` equals `passport-twitter:mrbrown123`.

If there are no matches, an entry is added using the values received from the external provider (after having applied the corresponding attribute mapping) attaching the computed value for `jansExtUid`. The user profile can contain single or multivalued attributes.

📝 The prefix `passport-<provider-name>` is used to keep the code compatible with the Passport.js implementation for Inbound Identity
