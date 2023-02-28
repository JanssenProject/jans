---
tags:
  - administration
  - vm
  - operations
---

In order to debug issues, checking the Jans services may be necessary. The process to do this differs slightly between operating systems. The following examples are shown on Ubuntu 20.04; however, they should work on any operating system using systemd.

## Getting list of Jans services

```bash
$ sudo systemctl list-units --all "jans*"
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

Note: depending on your OS and the components of Jans installed, the output may be different.

## Checking status of a service
```bash
$ sudo systemctl status jans-auth.service
● jans-auth.service - Janssen OAauth service
     Loaded: loaded (/etc/systemd/system/jans-auth.service; enabled; vendor preset: enabled)
     Active: active (running) since Tue 2022-11-01 15:03:23 UTC; 1h 38min ago
    Process: 44700 ExecStart=/opt/dist/scripts/jans-auth start (code=exited, status=0/SUCCESS)
   Main PID: 44727 (java)
      Tasks: 60 (limit: 4677)
     Memory: 889.2M
     CGroup: /system.slice/jans-auth.service
             └─44727 /opt/jre/bin/java -server -Xms256m -Xmx928m -XX:+DisableExplicitGC -Djans.base=/etc/jans -Dserver.base=/opt/jans/jetty/jan>
```

In case of an error or a non-functional component, this is where you would find information about the component.
