---
tags:
  - administration
  - auth-server
  - session
  - Account Chooser
---

## Select Account

A person may have several accounts on a single Jans Auth Server instance. For
example, it is common to have several Gmail accounts. Jans Auth Server uses two
cookies to track which accounts are associated with a browser: `session_id` and
`current_sessions`.

Below is an example or a person with two authenticated sessions:

```
session_id: de510ab6-b06c-4393-86d8-12a7c501aafe
current_sessions: ["de510ab6-b06c-4393-86d8-12a7c501aafe", "c691e83d-eb1b-41f0-b453-fab905681b5b"]
```

An RP trigger Auth Server's built in Account Chooser feature by sending an
[OpenID Authentication Request](https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest) with the parameter `prompt=select_account`. In
this case, Auth Server renders the default page:
`/opt/jans/jetty/jans-auth/custom/pages/selectAccount.xhtml`

This page iterates `current_sessions` and enables the person
to login as a different account, for example:

![Sample Select Account login page screenshot](../../assets/auth_server_sessions_selectAccount.png)

You can override this page if you place a `selectAccount.xhtml` in
`custom/pages`.

Other ways to handle account selection are possible using the Person
Authentication interception script, if you detect the `prompt=login`
parameter.
