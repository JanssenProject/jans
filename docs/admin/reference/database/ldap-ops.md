---
tags:
  - administration
  - reference
  - database
---

## LDAP Operations

## Establish Connection to Jans LDAP Server

All generated data in Jans Server by default stored in local LDAP server. This includes OpenID Connect client data, 
session data, tokens, user data, and more.

Check the ldap connection with bellow command.
```
/opt/opendj/bin/ldapsearch -h localhost -p 1636 -Z -X -D "cn=directory manager" -w "<password>" -b "o=jans" "(objectClass=*)"
```
You will see the output of you ldap specifications if connection is ok.


Besides use an LDAP browser like [JXplorer](http://jxplorer.org/) or [Apache Directory Studio](https://directory.apache.org/studio/) 
and can find the configuration in `/etc/jans/conf/jans-ldap.properties`, e.g.:

For Jans OpenDJ, it will look like this:

```
bindDN: cn=directory manager
bindPassword: xCptpQZ8Tpw=
servers: localhost:1636
```
Establish a tunnel from your computer to the target Jans Server's LDAP. Tunneling is required because Jans Server's LDAP 
port, 1636, is not exposed to the internet.

In the below example we are showing how to connect and use Jans Server's internal LDAP server with any LDAP browser.

* Create tunnel:
* `ssh -fNL 5902:localhost:1636 [username]@[ip_of_Jans_server]`
* Open LDAP browser
* Create new connection


![ldap-connection](https://github.com/JanssenProject/jans/assets/43112579/901483e4-d903-4b5f-af45-0a0c9957c29b)


* Perform authentication: Provide the password for 'admin' user.
  

![ldap-creds](https://github.com/JanssenProject/jans/assets/43112579/c9751ddf-8a0f-4fad-9b49-12ebd425018d)


* Browse ldap and go to `ou=people`.

  ![ldap_people](https://github.com/JanssenProject/jans/assets/43112579/8da57305-0227-4bdb-82f8-0044f8b05afe)


## Attributes and Object Classes

In LDAP, attributes and object classes are essential components that define the structure and characteristics of directory entries. Attributes represent specific pieces of information, while object classes define the set of attributes that an entry can have. In this section, we'll explore attributes and object classes in more detail.

###  Attributes

Attributes are the building blocks of information stored in an LDAP directory. They represent specific properties or data associated with an LDAP entry. Each attribute has a name and can have one or more values. Attributes are used to store information such as names, addresses, phone numbers, and more.

LDAP attributes are defined in schemas, which provide a set of rules for naming, syntax, and semantics. Some common attributes include:

* `cn` (Common Name): Represents the common or full name of an entry.
* `uid` (User ID): Represents a unique identifier for an entry.
* `mail` (Email Address): Represents the email address of an entry.

Attributes can be single-valued (one value) or multi-valued (multiple values). The attribute's syntax determines the type of data it can hold, such as strings, integers, dates, and more.

### Object Classes

Object classes define the type of an LDAP entry and determine which attributes an entry can have. An object class is a collection of attributes and their associated rules. When you create a new entry, you specify its object class to indicate its characteristics.

Object classes are hierarchical, meaning that they can inherit attributes and rules from other object classes.

Some common object classes include:

* `top:` The top-level object class for all entries.
* `janPerson:` Represents a person and includes attributes like cn, sn, and givenName.
* `jansCustomPerson:` Represents an organizational person and inherits attributes from person.


##  Basic LDAP Operations

Basic LDAP operations are fundamental actions you can perform to interact with an LDAP directory. These operations include searching for entries, adding new entries, modifying existing entries, and deleting entries. 

### Searching for entries based on filters and attributes

Searching for entries is a core function of LDAP. You can search for specific entries based on criteria such as attributes, object classes, and more. The `ldapsearch` command allows you to perform searches. The basic syntax is as follows:

For example, to search for all user entries under the base DN "ou=people,o=jans" with a specific object class like for users `jansCustomPerson`, you might use:

```
/opt/opendj/bin/ldapsearch -h localhost -p 1636 -Z -X -D "cn=directory manager" -w "<password>" -b "ou=people,o=jans" "(objectClass=jansCustomPerson)"
```
The output will be the users list of your Jans Server.

### Adding new entries to the directory

To add new entries to the LDAP directory, you use the `ldapmodify` command. Entries are specified in LDIF (LDAP Data Interchange Format) files. The syntax is as follows:

Here's an example of an LDIF file for adding a new user entry:

```
dn: uid=..,ou=people,o=jans
objectClass: top
objectClass: jansPerson
objectClass: jansCustomPerson
uid: ...
displayName: ...
cn: ...
sn: ...
givenName: ...
mail: ...
userPassword: {SSHA}nS4g2X2JqgOc3NTTR8QH2qGf88o/Ad2Q
```
Save the above content to a file named `new_user.ldif`, and then use the `ldapmodify` command:

```
/opt/opendj/bin/ldapmodify -h localhost -p 1636 -Z -X -D "cn=directory manager" -w "<password>" -a -f new_user.ldif
```

You will get successfull message.

### Modifying attributes of existing entries

LDAP allows you to modify the attributes of existing entries. The `ldapmodify` command is used to apply modifications specified in an LDIF (LDAP Data Interchange Format) file to the directory.

Here's an example of an LDIF file for modifying an existing user entry to update the `email` address:
```
dn: uid=...,ou=people,o=jans
changetype: modify
replace: mail
mail: test@example.com
```
Save the above content to a file named `modify_user.ldif`, and then use the `ldapmodify` command:

```
/opt/opendj/bin/ldapmodify -h localhost -p 1636 -Z -X -D "cn=directory manager" -w "<password>" -a -f modify_user.ldif
```
You will get successfull message.

### Deleting entries from the directory

LDAP allows you to delete entries from the directory using the `ldapdelete` command. You can delete entries by specifying their Distinguished Name (DN).

```
/opt/opendj/bin/ldapdelete -h localhost -p 1636 -Z -X -D "cn=directory manager" -w "<password>" "uid=...,ou=people,o=jans"
```
In response you will ge `DELETE operation successful for DN uid=..,ou=people,o=jans`



### Add User in Jans Admin group

You can add user in Jans Admin group. Here's an example of an LDIF file for modifying an existing user entry to add the user in Jans Admin group.

```
dn: uid=rima,ou=people,o=jans
changetype: modify
add: memberOf
memberOf: inum=60B7,ou=groups,o=jans
```
Save the above content to a file named `add_attribute.ldif`, and then use the `ldapmodify` command:

```
/opt/opendj/bin/ldapmodify -h localhost -p 1636 -Z -X -D "cn=directory manager" -w "<password>" -a -f add_attribute.ldif
```
You will get successfull message.

## Change password

Create an LDIF file that contains the modification you want to make. For setting a new password, you'll use the replace operation on the `userPassword` attribute. Here's an example LDIF file named modify_password.ldif:

```
dn: uid=...,ou=people,dc=example,dc=com
changetype: modify
replace: userPassword
userPassword: {SSHA}newhashedpassword
```

Use the `ldapmodify` command to apply the modification from the LDIF file to the LDAP directory:

```
/opt/opendj/bin/ldapmodify -h localhost -p 1636 -Z -X -D "cn=directory manager" -w "<password>" -a -f modify_password.ldif
```





## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
