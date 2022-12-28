---
tags:
  - administration
  - auth-server
  - session
  - Account Chooser
---

## Select Account

A person may have several accounts in a single Jans Auth Server instance. For
example, you may have several diffent Gmail ids. An RP can enable a person
to choose their account by sending an [OpenID Authentication Request](https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest) with the
parameter `prompt=select_account`.

For example, following an initial authentication, the session cookie on a person's browser would contain the following data:

```
session_id: de510ab6-b06c-4393-86d8-12a7c501aafe
current_sessions: ["de510ab6-b06c-4393-86d8-12a7c501aafe"]
```

Continuing this example, if an RP sent an additional Authentication Request with `prompt=select_account`, Auth Server may return a page enabling the person
to select their account, or even to login as a different account. Once authenticated as a different user, the `current_sessions` cookie would reference
all the current sessions, like this:

```
session_id: c691e83d-eb1b-41f0-b453-fab905681b5b
current_sessions: ["de510ab6-b06c-4393-86d8-12a7c501aafe", "c691e83d-eb1b-41f0-b453-fab905681b5b"]
```
