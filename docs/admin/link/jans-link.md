---
tags:
  - administration
  - link
  - jans-link
---

## Jans Link

The Jans Link synchronization service updates the Janssen User Store when authoritative data is managed in an external database. Changes to the external data are detected by comparing periodic snapshots. Currently, Jans Link is unidirectional--Link treats the external data as authoritative. Custom data transformations, like adding claims or claims enhancement, is possible through the use of the Link Interception Script, which is invoked for each user update.

## Installation

The API is available as a component of Jans Server. Upon [installation](https://docs.jans.io/head/admin/install/vm-install/ubuntu/) you can select if you want Service-Link included in your environment. To add Jans-Link post-install do the following:

1. Run `python3 /opt/jans/jans-setup/setup.py --install-jans-link`

## Jans Link Service Start/Stop

* To Satrt Jans-Link `systemctl start jans-link`

* To check status `systemctl status jans-link`

* To Stop Jans-Link `systemctl stop jans-link`

## Jans Link on Jans CLI

To see Jans-Link info using Jans CLI

```
/opt/jans/jans-cli/config-cli.py --info JansLinkConfiguration
```

To see basic schema of Jans-Link use bellow command
```
/opt/jans/jans-cli/config-cli.py --schema "Jans Link Plugin:AppConfiguration"
```

To see Jans Link properties using bellow command.
```
/opt/jans/jans-cli/config-cli.py --operation-id get-jans-link-properties
```

output may like this:
```
{
  "inumConfig": {
    "configId": "local_inum",
    "bindDN": "cn=directory manager",
    "bindPassword": "+iChsQofo6Y=",
    "servers": [
      "localhost:1636"
    ],
    "maxConnections": 10,
    "useSSL": true,
    "baseDNs": [
      "ou=link,o=site"
    ],
    "useAnonymousBind": false,
    "enabled": true,
    "version": 0,
    "level": 0
  },
  "targetConfig": {
    "maxConnections": 2,
    "useSSL": false,
    "useAnonymousBind": false,
    "enabled": false,
    "version": 0,
    "level": 0
  },
  "ldapSearchSizeLimit": 1000,
  "keyAttributes": [
    "uid"
  ],
  "keyObjectClasses": [
    "gluuPerson"
  ],
  "sourceAttributes": [
    "mail",
    "cn",
    "sn"
  ],
  "updateMethod": "copy",
  "defaultInumServer": true,
  "keepExternalPerson": true,
  "useSearchLimit": false,
  "attributeMapping": [
    {
      "source": "uid",
      "destination": "uid"
    },
    {
      "source": "cn",
      "destination": "cn"
    },
    {
      "source": "sn",
      "destination": "sn"
    }
  ],
  "snapshotFolder": "/var/jans/link-snapshots",
  "snapshotMaxCount": 10,
  "baseDN": "o=jans",
  "personObjectClassTypes": [
    "jansCustomPerson",
    "jansPerson",
    "eduPerson"
  ],
  "personCustomObjectClass": "jansCustomPerson",
  "allowPersonModification": false,
  "supportedUserStatus": [
    "active",
    "inactive"
  ],
  "metricReporterInterval": 0,
  "metricReporterKeepDataDays": 0,
  "disableJdkLogger": true,
  "cleanServiceInterval": 0,
  "linkEnabled": false,
  "serverIpAddress": "255.255.255.255",
  "lastUpdate": "2023-07-05T18:27:28",
  "lastUpdateCount": "0",
  "problemCount": "3",
  "useLocalCache": false
}

```

## Jans Link using Jans TUI

The deployer needs to know various values of the backend AD to configure `Jans-Link`. For example, `host` & `port`, `bindDN` user information, `bindDN` `password`, `Objectclasses`, `attributes` which will be pulled etc.

In addition, the deployer needs to know generic information about the Jans Server's LDAP. By default, the deployer can use `localhost:1636`, `cn=directory manager`, `password` chosen during installation, `ou=people,o=site` as server information, `bindDN`, `bindDN` `password` and `baseDN` respectively.

Let's move forward with the Jans-Link setup.

Open Jans TUI using `/opt/jans/jans-cli/jans_cli_tui.py`, select tab **Jans link**

**Configuration**

![link-config](https://github.com/JanssenProject/jans/assets/43112579/c446d933-143c-485f-adfb-4c8d3f631c05)


* `Enabled` : Enable your Jans-link configuration.

* `Refresh Method`: There have two refresh mode `copy` and `VDS`. If the organization has any kind of Active Directory/LDAP server, they are strongly recommended to use the Copy Method from the drop-down menu. Any organization with a database like mysql can use the VDS method. This option can be enabled via the drop-down menu in Refresh Method option.

* `Mapping` : In the source attribute to destination attribute mapping field, you can enter the source attribute value on the left, and the destination attribute on the right. In other words, you can specify what the attribute is on the backend in the left field, and what it should be rendered as when it comes through the Jans Server in the right field.


* `Server IP address`: Proviede your server IP address.

* `Polling Interval (minutes)`: This is the interval value for running the link mechanism in the Jans Server. It is recommended to be kept higher than 15 minutes.

* `Keep External Persons` : Enable it during Jans-Link setup. This will allow your default user 'admin' to log into Jans Server after initial linking iteration. If you do not enable 'Keep External Person', your 'admin' user including all other test users will be gone after first linking iteration.

* `Snapshot Directory` : Every cycle of Jans Server link cycle saves an overall snapshot and problem-list record on a specified location. This is where the Jans Server Administrator can specify the location. You can easily decide whether link synchronizes all users or not. Generally the rejected users are enclosed in the problem-list file.

* `Snapshot Count` : This defines the total number of snapshots that are allowed to be saved in the hard drive of the VM. It is recommended to be kept to 10 snapshots.

* `Load Source Data withLimited Search`


**Customer Backend Key Attribute**

![customerBackend](https://github.com/JanssenProject/jans/assets/43112579/322cd6b7-43d6-4287-9821-99bca5c11674)

* `Key Attributes` : This is the unique key attribute of backend Active Directory/LDAP Server such as MikeAccountname for any Active Directory.

* `Key Object Classes` : This contains the `Object Classes` of the backend Active Directory/LDAP which has permission to talk to Jans Server link such as person, organizationalPerson, user etc.

* `Source Attributes` : This contains the list of attributes which will be pulled and read by the Jans Server.

* `Custom LDAP Filter` :  If there is any custom search required, this filtering mechanism can be used such as "sn=*" whereas the value of this field ensures that every user must contain an attribute named `SN`.


**Source Backend LDAP Server**

![sbls](https://github.com/JanssenProject/jans/assets/43112579/285e96ce-5b9e-4c2f-8eb4-629ce999f9f6)


This section allows the Jans Server to connect to the backend Active Directory/LDAP server of the organization.

Add new Source LDAP Config


* `Name` : Input `source` as the value

* `Bind DN` : This contains the username to connect to the backend server. You need to use full DN here. As for example, `cn=jans`,`dc=company`,`dc=org`.

* `Bind Password` : This is your server password.

* `Max Connections` : This value defines the maximum number of connections that are allowed to read the backend Active Directory/LDAP server. It is recommended to keep the value of 2 or 3.

* `Server:Port` : provide your server name and port number.

* `Base DNs` : This contains the location of the Active Directory/LDAP tree from where the Jans Server shall read the user information.

* `Use SSL` : Use this feature if the backend server allows SSL connectivity.

* `Enable` : This check-box is used to save and push the changes. Do not use this unless the server administrator has entered all the required values.


**Inum DB Server**

![inumsbs](https://github.com/JanssenProject/jans/assets/43112579/235c2358-2ded-45ff-92f6-2ea84c2b9f6f)


This section of the application allows the server administrator to connect to the internal LDAP of the Jans Server. As Jans Server administrator, you do not need to insert anything here in this section as new Gluu Server versions automatically populates this for you (unlessyou try to manually configure it anyway).



## Things To Remember

The Jans Server supports two LDAP modes:

* Authentication
* Identity mapping

To synchronize user accounts from an external LDAP directory server, you can use the built-in Jans CLI or Jans TUI features for Service Link, which supports mapping identities from one or more source directory servers.

After configuring Jans Link, you should give it some time to run and populate the LDAP server. Here are some tips before you get started:

* Make sure you are using LDAP authentication

* Check the snapshots folder to see if files are being created.

* Use the Jans TUI to browse users.

* Use the command ldapsearch to check to see if results are starting to come in. The following command will search for the total number of users in the Jans LDAP:


```
/opt/opendj/bin/ldapsearch -h localhost -p 1636 -Z -X -D "cn=directory manager" -w <pass> -b "ou=people,o=DA....,o=jans" dn | grep "dn\:" | wc -l
```

Try to log in with one of these users. We assume that you have also set up your Jans Server to use the correct LDAP server for authentication.




## This content is in progress

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
