---
tags:
  - administration
  - auth-server
  - session
---

## Select Account

AS supports different accounts. To be able select account or login as another user within same user-agent authorization request must have `prompt=select_account`.

When user is logged in cookies looks as:
```
session_id: de510ab6-b06c-4393-86d8-12a7c501aafe
current_sessions: ["de510ab6-b06c-4393-86d8-12a7c501aafe"]
```

If RP sends Authorization Request with `prompt=select_account` and selects hits `Login as another user` button and authenticate then cookies looks as:
```
session_id: c691e83d-eb1b-41f0-b453-fab905681b5b
current_sessions: ["de510ab6-b06c-4393-86d8-12a7c501aafe", "c691e83d-eb1b-41f0-b453-fab905681b5b"]
```

AS represents accounts based on `current_sessions` cookies. It may contain outdated references but AS will filter it out and update on request. Thus page represents always actual/active sessions.

![selectAccount](../img/admin-guide/selectAccount.png)
