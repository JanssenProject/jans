---
tags:
  - administration
  - configuration
  - tools
  - cli
  - tui
  - commandline
  - logging
---

# Using TUI Command Line Log

When you do a write operation (post/put/patch) with Jans TUI, it [logs](README.md#command-line-logs) 
activities to file `<log-dir>/cli_cmd.log`,default `log-dir` is home directory.

Consider you created a user with TUI and have the following line in `cli_cmd.log`:

```
/usr/bin/python3 /opt/jans/jans-cli/cli/config_cli.py --operation-id post-user --data '{"customObjectClasses": ["top", "jansPerson"], "customAttributes": [{"name": "middleName", "multiValued": false, "values": [""]}, {"name": "sn", "multiValued": false, "values": ["Watts"]}, {"name": "nickname", "multiValued": false, "values": [""]}], "mail": "ewatts@foo.org", "userId": "ewatts", "displayName": "Emelia Watts", "givenName": "Emelia", "userPassword": "TopSecret", "jansStatus": "active"}'
```

You can modify this line to create another user, let us change the followings:

__userId__ padilla
__userPassword__ NewSecret
__sn__ Padilla
__givenName__ Reggie
__displayName__ Padilla Reggie
__mail__ reggie.padilla@egg.org

New command will become:

```
/usr/bin/python3 /opt/jans/jans-cli/cli/config_cli.py --operation-id post-user --data '{"customObjectClasses": ["top", "jansPerson"], "customAttributes": [{"name": "middleName", "multiValued": false, "values": [""]}, {"name": "sn", "multiValued": false, "values": ["Padilla"]}, {"name": "nickname", "multiValued": false, "values": [""]}], "mail": "reggie.padilla@egg.org", "userId": "padilla", "displayName": "Padilla Reggie", "givenName": "Reggie", "userPassword": "NewSecret", "jansStatus": "active"}'
```

When you execute this command, user will be created:
```
Server Response:
{
  "dn": "inum=65a5a60c-d314-426d-a8f4-8c36325a1024,ou=people,o=jans",
  "userId": "padilla",
  "createdAt": "2023-05-30T09:48:58",
  "customAttributes": [
    {
      "name": "sn",
      "multiValued": false,
      "values": [
        "Padilla"
      ],
      "displayValue": "Padilla",
      "value": "Padilla"
    }
  ],
  "customObjectClasses": [
    "top",
    "jansCustomPerson"
  ],
  "inum": "65a5a60c-d314-426d-a8f4-8c36325a1024",
  "mail": "reggie.padilla@egg.org",
  "displayName": "Padilla Reggie",
  "jansStatus": "active",
  "givenName": "Reggie",
  "baseDn": "inum=65a5a60c-d314-426d-a8f4-8c36325a1024,ou=people,o=jans"
}
```
