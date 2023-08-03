---
tags:
  - administration
  - configuration
  - cli
  - commandline
  - tui
---

Please follow [these instructions](../jans-tui/README.md#standalone-installation) for stand alone installation. Example execution:

```
$ ./jans-cli-tui.pyz --no-tui --host test.jans.io --client-id 2000.562981df-1623-4136-b1d0-aaa277edc48c --client-secret KU6ydImJZK6S --operation-id get-acrs
Please wait while retrieving data ...
Access token was not found.
Please visit verification url https://test.jans.io/device-code?user_code=LKHC-PBTR and authorize this device within 1800 secods
Please press «Enter» when ready
{
  "defaultAcr": "simple_password_auth"
}
```

Note that argument `--no-tui` is necessary, otherwise it will switch to TUI mode.
