# Persistence Extension Guide

## Overview

Allows administrators to specify the hashing algorithm employed to encode users' passwords in the local database.

## Interface

### Methods

|Method|`def createHashedPassword(self, credential)`|
|:---|:---|
|Description| Given a password, it must return its hashed repesentation as a `string`
|Method Parameter| `credential` is `string`

For implementation, method `createStoragePassword(String, PasswordEncryptionMethod`) of utility class `PasswordEncryptionHelper` can be useful here. The enumeration `PasswordEncryptionMethod` already provides a numerous list of encryption alternatives.


|Method |`def compareHashedPasswords(self, credential, storedCredential)`|
|:---|:---|
|Description| Returns true if the given (plain) password matches a hashed password representation. When the persistence extension script is enabled this method is called upon user+password authentication to determine if the authentication is deemed successful|
|Method Parameter| `credential` is the password as a `String` (eg. as given in the login form)|
|Method Parameter| `storedCredential` is the hashed password (`String`) as stored in the local database for the user in question|

Relevant Links:

- [Utility classes](https://github.com/GluuFederation/oxCore/tree/version_4.2.0/persistence-core/src/main/java/org/gluu/persist/operation/auth)
- [Sample script](https://github.com/GluuFederation/community-edition-setup/blob/version_4.2.0/static/extension/persistence_extension/SampleScript.py)

|**Note**|
|:---|
|It is recommended to store this script in your database. If you use the filesystem option, you may have to replicate the file across nodes when in a container-based environment. This is because the script caller can be oxTrust, oxAuth or even SCIM service.|

### Objects

Definitions of all objects used in the script

## Common Use Cases

Descriptions of common use cases for this script, including a code snippet for each
