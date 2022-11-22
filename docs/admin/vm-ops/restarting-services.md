---
tags:
  - administration
  - vm
  - operations
---

## Getting list of Jans services

```bash
sudo systemctl list-units --all "jans*"
```

Output should be like below: 
```
UNIT                    LOAD   ACTIVE SUB     DESCRIPTION               
jans-auth.service       loaded active running Janssen OAauth service    
jans-config-api.service loaded active running Janssen Config API service
jans-fido2.service      loaded active running Janssen Fido2 Service     
jans-scim.service       loaded active running Janssen Scim service      

LOAD   = Reflects whether the unit definition was properly loaded.
ACTIVE = The high-level unit activation state, i.e. generalization of SUB.
SUB    = The low-level unit activation state, values depend on unit type.

5 loaded units listed.
```

## Other Services

There are more services other than Jans services like LDAP or Apache. To get the status of those services make sure you use command like

```bash
sudo systemctl list-units --all "apache2*"
```


## Commands (Ubuntu 20.04, RHEL 8, SUSE 15)

### Start

``` 
systemctl start [service name]
```

### Stop

```
systemctl stop [service name]
```

### Status

```
systemctl status [service name]
```

### Restart

```
systemctl restart [service name]
```

### Reload
This command is used for the `apache2` and `httpd` services.

```
systemctl reload [service name]
```
