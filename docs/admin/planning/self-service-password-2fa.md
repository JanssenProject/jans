---
tags:
  - administration
  - planning
  - 2FA
  - Account Recovery
---

If your domain offers two-factor authentication, it's a good idea to enable end
users to manage their various credentials. Google does a great job in this
regard. Check out [2-Step Verification](https://myaccount.google.com/security).
What's great about this page is that Google lets you manage all your different
credentials on one page.

You can build a page like Google on your own website. You need to be able to
list, add, and remove 2FA credentials for a given user's account. But another
good option is the Casa web application, which is an [open source project](https://github.com/GluuFederation/casa) from Gluu, licensed under Apache 2.0. Casa supports a number of
authentication mechanism that are already available in Jans Auth Server. See the
[Casa Receipe](https://docs.jans.io/head/admin/recipes/casa/) and the
[Casa Person Authn Script](https://docs.jans.io/head/script-catalog/person_authentication/casa/)
for more information.
