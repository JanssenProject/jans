---
tags:
  - administration
  - planning
---

The holy grail of security policies since the introduction of RSA OTP tokens,
stepped-up authentication is when the subject is presented with an additional
authentication requirement, usually in response to perceived risk. As Jans
How do you decide when to invoke stepped-up authentication? As Auth Server is not
a policy enforcement point, or a policy management system, these two aspects are
out of scope. But once you make a runtime decision that you need more evidence
of identity, the OpenID Connect [Authentication Request](https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest) has a few features that can help you out.

The first is `prompt=login`. This is a hint from the Relying Party to the
OpenID Provider that re-authentication is needed. Not all OpenID Connect
providers support this feature (for example, Google does not).  However
Auth Server does currently support this parameter, although there is a
[feature request](https://github.com/JanssenProject/jans/issues/3006) to make it
optional.

The second is `acr_values`. In Auth Server, this provides a hint to the
Auth Server to invoke a specific Person Authentication interception script.
Typically, this would specify your workflow for additional authentication.
If you are using Agama, you can also provide a hint for which Agama flow to
invoke by adding __.

Another more esoteric strategy is to use UMA claims gathering. In this flow,
the subject's browser is directed to the claims gathering endpoint (i.e.
front channel), at which point the claims gathering script could invoke an
OpenID authentication, using the `prompt` and `acr_values` parameters mentioned
above.

If you want to see an example of how a client implements stepped-up
authentication, you should checkout this [mod_auth_openidc wiki page](https://github.com/zmartzone/mod_auth_openidc/wiki/Step-up-Authentication).
