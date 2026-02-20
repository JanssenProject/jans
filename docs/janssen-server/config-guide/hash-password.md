---
tags:
  - administration
  - developer
  - configuration
  - Hashed Passwords
  - Entry manager
---

## Hashed Passwords

ORM stores users password in DB in hashing form. It supports next hash methods:  SHA, SSHA, SHA-256, SSHA-256, SHA-384, SSHA-384, SHA-512, SSHA-512 MD5, SMD5, CRYPT, CRYPT-MD5, CRYPT-SHA-256, CRYPT-SHA-512, CRYPT-BCRYPT $2a$, CRYPT-BCRYPT $2b$, PKCS5S2, ARGON2

Support additional hashing methods can be added with [Persistence Extension](../../script-catalog/persistence_extension/persistence.md#persistence-script) script.

# Hash Password properties

New hash methods like Argon2 supports default hash properties override. This is supported list of properties from `/etc/jans/conf/jans-sql.properties`:

```
# Argon 2 parameters
# 0 - ARGON2_d, 1 - ARGON2_i, 2 - ARGON2_id
password.method.argon2.type=2
# 1.0 - 16, 1.3 - 19
password.method.argon2.version=19
password.method.argon2.salt-length=16
password.method.argon2.memory=7168
password.method.argon2.iterations=5
password.method.argon2.parallelism=1
password.method.argon2.hash-length=32
```

After thess properties update `jans-auth` requires restart.

# Hash Password format

User password value in DB has format {TYPE}{BASE64 encoded hash}

Argon2 hashed passwords in DB has similar format. Each password starts from`{ARGON2}` which follows Base64 encoded argon2 password **hash** with **properties**.

Argon2 encoded format contains next parts:
$**type**$v=**version**$m=**memory**,t=**iterations**,p=**parallelism**$Base64WithoutPadding(**salt**)$Base64WithoutPadding(**password_hash**)

Here are samples of `secret` encoded passwords:

1. With default values:
```
{ARGON2}JGFyZ29uMmkkdj0xOSRtPTcxNjgsdD01LHA9MSRuSGZnL2JBZTRybEtNWS90ck9WNGdnJGJvWmgvcG9tVDJyR1dPV0pNRVp4KzlGa0dJWTVVbjhwTVk0Syt6L28rME0=
->
$argon2i$v=19$m=7168,t=5,p=1$nHfg/bAe4rlKMY/trOV4gg$boZh/pomT2rGWOWJMEZx+9FkGIY5Un8pMY4K+z/o+0M
```

2.  Override default setting with file `/etc/jans/conf/jans-sql.properties`
```
# Argon 2 parameters
password.method.argon2.type=2
password.method.argon2.version=19
password.method.argon2.salt-length=16
password.method.argon2.memory=32768
password.method.argon2.iterations=10
password.method.argon2.parallelism=1
password.method.argon2.hash-length=32
```

```
{ARGON2}JGFyZ29uMmlkJHY9MTkkbT0zMjc2OCx0PTEwLHA9MSRXMnQyRjVEWVNRYWtUOFZaUEJlTHRRJGMrb0RTdThiWG4zemQ2Q3NyM2RnN2huY3RqemEyUXFVMnladlZyL2w3YlU=
$argon2id$v=19$m=32768,t=10,p=1$W2t2F5DYSQakT8VZPBeLtQ$c+oDSu8bXn3zd6Csr3dg7hnctjza2QqU2yZvVr/l7bU
```
