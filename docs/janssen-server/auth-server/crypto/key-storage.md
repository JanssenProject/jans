---
tags:
  - administration
  - auth-server
  - cryptography
  - key-storage
---

# Janssen Key Storage

A Java KeyStore is a file that contains set of aliases. Every alias can contain private key and public key and certificate (additional property info about owner of they key) or only public key and certificate.

KeyStore stores the following type of data:
  - Private Keys;
  - Public Keys and Certificates;
  - Secret Keys.

KeyStore are used by follow crypto primitives:
  - asymetric encryption;
  - digital signature;
  - symmetric-key algorithm.



Janssen KeyStore files contain:
  - Private Keys;
  - Public Keys and Certificates.

## Supported Formats of Key Storages

There is some set of standardized KeyStore formats. Janssen applications use follow KeyStore formats:
  - **JKS**: Java KeyStore format (proprietary keystore implementation provided by the SUN provider). KeyStore file extensions: **.jks**, **.keystore**, **.ks**;
  - **PKCS#12**: Personal Information Exchange Syntax, developed by RSA Security. PKCS #12 defines an archive file format for storing many cryptography objects as a single file. KeyStore file extensions: **.pkcs12**, **.p12**, **.pfx**;
  - **BCFKS**: Bouncy Castle FIPS Key Store (BCFKS) format supports storage of certificates and private keys using AES-CCM and PBKDF2 algorithms, providing greater security than the standard JKS and PKCS12 implementations. Support for BCFKS format is implemented, using BCFIPS Crypto Provider (https://www.bouncycastle.org/fips_java_roadmap.html). KeyStore file extensions: **.bcfks**, **bcf**, **bcfips**. 

## Installed KeyStore files

Janssen installs KeyStore files in the directory: **/etc/certs**.

Follow Keystore files are used by Janssen:

  - jans-auth-keys.pkcs12
  - smtp-keys.pkcs12
.

### jans-auth-keys.pkcs12

Here is the example of the file: **/etc/certs/jans-auth-keys.pkcs12** (list of entries/aliases).

```bash
keytool -list -v -storetype PKCS12 -keystore /etc/certs/jans-auth-keys.pkcs12 -storepass gNzpzYj5h8i1
```

```text
Keystore type: PKCS12
Keystore provider: SUN

Your keystore contains 31 entries

Alias name: connect_3c83ba3a-7a62-49e7-8478-b6d3e242e549_sig_es256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: a81e8386d6774110b12a292a7185c454359b9b6d67a3f5aef587e9395db04166
Valid from: Thu Aug 17 05:21:05 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 4F:FD:3A:E6:61:D8:8E:0F:61:A4:AA:DE:D7:E4:F4:28:5E:EE:39:20
         SHA256: 52:15:67:CE:0B:56:1C:CD:CE:C9:39:4C:11:25:B8:73:13:7F:7F:91:BB:E4:1A:3F:48:8D:B0:DC:01:D0:55:3D
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_45c2ce16-6d5b-47d4-be40-c4da48ba49d3_enc_ecdh-es+a128kw
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: c6fdff1d75c83e32519a94133f9954de22a2aea4fb20ef669bce2221c08cf21f
Valid from: Thu Aug 17 05:21:17 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 02:35:E7:FC:50:47:4A:3A:EA:54:EE:92:CA:75:14:D0:A4:E0:0C:45
         SHA256: 19:18:8F:68:68:A2:FE:E2:03:BC:E6:2E:87:24:D4:E9:B0:64:D8:44:7D:32:A1:DE:1C:1B:8E:9A:96:3F:32:5A
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_5365f93a-368d-4643-8708-1f4b31b62d47_enc_rsa-oaep
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 4d561c04da6bbe50ea15720e6c6898d35793e711eabc9eaa02ad7ed9dafa148b
Valid from: Thu Aug 17 05:21:14 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: C7:B4:1A:9A:15:59:E6:45:AC:77:C7:1C:E4:7D:F2:01:EC:4B:59:AD
         SHA256: 75:11:06:F5:74:D7:B4:C4:0A:57:A5:88:AA:91:F9:57:4C:78:BE:D2:68:1F:0E:AF:0B:CA:16:2F:0F:FE:17:EA
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_5b5da374-2ccb-40ca-8544-9338fe7427ff_sig_rs384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 235fc012863b7291c3370978761a2df6a14796cc601b0fec801b84aa7281b116
Valid from: Thu Aug 17 05:21:03 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 17:33:24:FC:C8:F7:0C:10:7B:0A:30:5B:28:6E:30:AC:13:A5:FD:36
         SHA256: E5:05:5A:7E:00:93:2E:8B:3E:AE:91:D5:B5:B5:1A:A9:51:37:FE:72:29:02:83:20:F7:6F:BC:BE:D9:FF:23:7E
Signature algorithm name: SHA384withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_6218efa5-197e-49f5-919f-769e6d0aaaec_sig_es512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 304513446352446c48e39017e3044545e0e2a4a71c36899eacfac775606e958d
Valid from: Thu Aug 17 05:21:08 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: BE:27:21:18:F4:D4:F6:71:D9:4A:DF:A0:3F:F2:20:76:E1:3E:DD:05
         SHA256: 39:FB:09:EC:BD:62:CF:B2:8B:6A:7F:D9:AF:34:64:7C:50:53:E9:E1:14:01:FE:35:B3:B6:03:8D:C6:E3:A3:68
Signature algorithm name: SHA512withECDSA
Subject Public Key Algorithm: 521-bit EC (secp521r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_6dbf485d-9d78-49d0-b977-72608ca6b727_sig_rs512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 6310f2b2c5fe5e9354d6352140bedf9efe986ed1c44e68d6dda2ccb46038a14c
Valid from: Thu Aug 17 05:21:04 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: E1:78:BA:84:C1:9A:9F:6D:FF:32:1B:F4:D9:4E:AE:AE:23:A9:7F:52
         SHA256: EF:C9:B8:40:82:E1:16:B2:7B:2C:E9:E4:47:17:5D:C2:AB:D7:AA:16:EE:11:95:19:F7:52:7C:20:E9:3C:82:91
Signature algorithm name: SHA512withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_7325add6-aaa6-4e4c-bfee-968fa7eba793_sig_ps384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: ea8c7dfe1d28f4ff9f686f0eccd5b3d06c50d80b4751713cce0052997027c90f
Valid from: Thu Aug 17 05:21:10 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 2D:71:05:C2:5C:5A:50:CD:C7:64:42:69:03:8B:BC:66:6F:F7:E3:2A
         SHA256: C8:DD:E9:2E:49:91:04:25:29:48:6F:CA:01:0B:6F:17:CA:29:01:C5:4D:2B:AF:3F:A3:25:03:16:12:34:5C:48
Signature algorithm name: RSASSA-PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_829abf33-03f1-4f35-a42e-bca49f992df4_enc_ecdh-es+a256kw
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 24c62ce19733ddce99573c073bfa890e29d48fbc4a1b4365748fdc0c420b793a
Valid from: Thu Aug 17 05:21:18 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 66:FC:23:8E:08:80:2A:C8:74:08:A5:37:C2:6D:8F:68:AB:23:7E:D3
         SHA256: 27:7B:A3:5D:E2:F3:6D:15:52:F8:D0:03:0A:7D:D5:B5:32:B9:4E:93:F1:C3:14:98:CA:98:E9:53:84:45:24:4C
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_87933d28-0523-4bd2-a078-a94e32887b04_sig_es256k
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 276ab34dca0e41236756ae0863ebd5e16ea0b209645128f54da0c0dc1c12b64c
Valid from: Thu Aug 17 05:21:06 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 53:23:C9:45:4D:D2:29:70:BA:EB:27:EB:83:66:AB:4C:B9:56:9E:83
         SHA256: 42:38:33:97:94:ED:58:51:D4:F1:C8:E5:2E:AB:56:05:B0:1D:79:05:33:51:DA:0F:41:E9:E8:59:11:B8:0F:57
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256k1) key (disabled)
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_8d71b86c-5b81-444c-afdf-bb3da4de11e4_sig_rs256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: d8344594dfcce15695d56f30178bc2a38bf3d432756bfd15a69242b3348876dd
Valid from: Thu Aug 17 05:21:02 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 5D:75:82:47:5F:80:F9:2F:41:48:CF:01:9F:EE:66:0E:71:37:FA:83
         SHA256: D1:92:1C:B1:D0:29:B8:23:73:FA:2E:89:11:2D:F1:8F:5E:2E:FE:B0:80:D1:CC:60:1B:B3:46:82:BA:04:9E:4B
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_9574dff8-1f98-421d-91bf-a760a18be2d2_sig_ps512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: a92efd444e5ea4ac9cd573aa66746b3b90adf465d20ad1abb25d903619f30d6c
Valid from: Thu Aug 17 05:21:11 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 29:CF:94:A6:DF:1E:85:59:A4:C1:62:1F:95:AB:67:31:1B:2D:07:6C
         SHA256: E5:72:DE:3F:C5:96:63:21:AB:82:B4:64:00:E6:19:9B:2C:B0:6D:7C:C6:8C:3D:5B:21:58:6A:3D:D1:CC:54:0D
Signature algorithm name: RSASSA-PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_9f70c848-95b6-48fe-a9e9-79b374848e28_enc_rsa1_5
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 46d231ec4e06d52ce1d5a4950f2f91ffc5dd107f5ac7a422225d55fb0004ae26
Valid from: Thu Aug 17 05:21:13 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: A5:82:D0:70:8D:19:A5:5D:DE:4D:CC:52:AE:FB:F4:FF:EF:1A:56:AE
         SHA256: BB:FA:F0:82:DC:71:84:90:74:C1:33:D9:BC:AE:35:EA:DD:6B:35:95:CE:45:DB:94:E9:9B:E9:39:A4:47:CD:B2
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_a28e886e-090b-42c3-b437-92c91106c0c1_enc_ecdh-es
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 4f03840af117cdff5a44ae7f12050ead8934d460134a35088ba5cb3782046b9e
Valid from: Thu Aug 17 05:21:16 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 24:00:50:A1:B6:FD:F3:EC:19:95:CE:0F:BA:4E:AE:C5:64:57:F7:0C
         SHA256: BC:02:64:A2:1E:B6:B3:BD:BE:15:8E:E6:8B:CA:1E:00:A0:13:D9:40:72:7E:93:0F:40:DB:58:B5:56:1B:08:54
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_b7920896-d087-4668-9ee5-66fd3cf56694_enc_rsa-oaep-256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 16d9f709786c58f4343b2fce290cafa4130cb706cae25bcc98970742879d6f1d
Valid from: Thu Aug 17 05:21:15 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 28:B3:2B:59:0B:BF:DB:F1:05:63:27:C3:9B:CD:35:14:54:A9:A3:16
         SHA256: D9:55:37:5E:2E:AE:48:3E:72:DF:39:E2:0B:D8:79:4F:A3:21:33:EF:DA:DF:24:22:8B:42:08:61:E5:7F:F8:9F
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_c50ce977-00c0-4f4a-8594-1a0bc3935f88_sig_ps256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: c1992dccc3c0635dea6f34ac0f03414cc5f2422883dde72e8c07ea3fba66e865
Valid from: Thu Aug 17 05:21:09 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 65:CC:C8:CF:EA:54:C8:2C:27:31:59:34:5E:69:41:CE:09:37:EB:6B
         SHA256: C0:C8:68:75:BE:C1:CC:9F:4B:19:5E:23:9B:F4:3B:E8:E3:CE:B7:84:35:29:C0:8C:12:63:1E:B4:81:6B:23:99
Signature algorithm name: RSASSA-PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_e99b9f12-cbf5-4d5e-9156-12f112d7b4a3_sig_eddsa
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 523b5a9b1edbb4ac08435f1f86c7d810be34535e0bc02b073baeab6d9f35ca75
Valid from: Thu Aug 17 05:21:12 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: EB:D3:5F:54:76:44:D1:01:5F:39:EA:0B:2A:08:FB:30:39:50:E8:CC
         SHA256: 5F:99:18:AE:47:58:FD:7E:E3:B7:B4:F8:57:4A:5B:68:94:F8:DD:2A:02:DE:F7:58:8B:6A:06:D6:E2:CE:3E:ED
Signature algorithm name: 1.3.101.112
Subject Public Key Algorithm: 1.3.101.112 key of unknown size
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_eb5d1996-197c-4913-9b10-201b4f371ff7_sig_es384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 613554766f456cae0e968ecf8eaedd5fbb0f04c6ff64202c6b51df34cd7b2a20
Valid from: Thu Aug 17 05:21:07 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 50:FF:D2:51:CA:F6:35:31:C8:12:14:13:36:0B:A5:05:F4:08:3F:97
         SHA256: 13:13:32:47:2B:5B:E7:20:EC:4B:77:E8:80:78:E0:84:DB:D9:5E:6D:6C:C6:86:5F:B5:6D:18:99:38:02:B0:32
Signature algorithm name: SHA384withECDSA
Subject Public Key Algorithm: 384-bit EC (secp384r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: connect_eddc097c-d9bc-4672-a6ec-2740f8ed99c4_enc_ecdh-es+a192kw
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 9755e3a9d7404f86161bdfd47dc0f57021aa598b6f4bde9b3929a1303d66baef
Valid from: Thu Aug 17 05:21:18 CDT 2023 until: Sat Aug 19 06:21:10 CDT 2023
Certificate fingerprints:
         SHA1: 20:1F:EE:29:8F:57:B4:FA:29:E8:3F:D6:DD:60:0C:E7:6C:A6:82:54
         SHA256: 6A:69:0F:0F:D0:51:5F:83:29:91:BB:E5:A7:63:48:14:F7:D1:C4:18:EE:4D:F2:28:50:63:18:2D:00:94:C7:F4
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_02674321-5bdc-451e-a8e0-12fbc494ab00_sig_ps512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: b544920f9433a67816ac7e022649057deeecd012b68344683ca92a680496b3dc
Valid from: Tue Aug 08 06:57:41 CDT 2023 until: Tue Aug 08 06:57:51 CDT 2073
Certificate fingerprints:
         SHA1: BC:D6:33:CB:95:28:C1:61:D3:EC:D8:10:79:04:C2:22:B8:AF:93:C3
         SHA256: 38:0E:40:94:0F:57:1A:B5:9E:AD:A2:7E:B1:AE:ED:42:4C:60:9F:BE:C3:95:51:2A:8B:39:E8:92:90:1C:57:FD
Signature algorithm name: RSASSA-PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_18c82db5-7b1c-4bd4-bbe9-7a4e65045fb4_sig_es384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 874585304266014b022a0a321c5d4451bb433305b9137a1ddbdff36504e39d45
Valid from: Tue Aug 08 06:57:38 CDT 2023 until: Tue Aug 08 06:57:48 CDT 2073
Certificate fingerprints:
         SHA1: 56:79:CC:8A:CB:CF:74:40:31:D6:C3:1D:D8:DC:9B:03:EA:2A:80:89
         SHA256: 86:12:AF:78:E6:63:68:B7:9E:B7:70:A3:CF:EF:35:35:FF:46:15:18:97:97:A8:1A:17:79:85:F5:99:9E:61:0A
Signature algorithm name: SHA384withECDSA
Subject Public Key Algorithm: 384-bit EC (secp384r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_45aa4647-b976-44b8-a94d-025ecced0fe9_enc_rsa-oaep
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 4c0fc7cf58866f4beee13c4bf8d12bd084b264f6bedfa484b54034a385d563db
Valid from: Tue Aug 08 06:57:45 CDT 2023 until: Tue Aug 08 06:57:55 CDT 2073
Certificate fingerprints:
         SHA1: 91:48:30:F4:F6:E8:5D:AF:09:79:C2:EC:F9:C6:20:B0:85:3C:02:7E
         SHA256: 9A:D9:B9:3A:74:AF:99:A9:B2:44:40:1F:8D:E2:C5:72:6E:E3:AA:43:52:A0:8A:1E:61:5A:48:29:57:7E:92:60
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_46f71619-57a9-4c3f-bc15-b91872c828cf_sig_es256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 1187ed8ad9aa4a985c68ba2e0665665a50c970a06e25697360b0c680bda347a3
Valid from: Tue Aug 08 06:57:37 CDT 2023 until: Tue Aug 08 06:57:47 CDT 2073
Certificate fingerprints:
         SHA1: 9F:F8:EC:FF:83:8A:35:73:B1:AB:89:FF:39:6C:61:C2:76:24:78:37
         SHA256: B5:3C:C6:0B:13:D2:C0:E5:3D:E4:7D:66:B4:DA:AA:B1:6E:23:82:07:57:D9:ED:3B:47:28:91:CE:76:EE:62:64
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_51b7ce59-5b1e-4f24-92d0-57edff831f37_sig_es512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: e2dd7decf9dd456bddfa2b9800ac47a4e38ac420cf416c0255820851a8041daf
Valid from: Tue Aug 08 06:57:39 CDT 2023 until: Tue Aug 08 06:57:49 CDT 2073
Certificate fingerprints:
         SHA1: A0:F0:F1:30:66:6B:35:DB:31:AB:8D:D3:E5:58:F4:31:F9:0B:60:E8
         SHA256: 5F:CF:BB:E8:6E:FE:A9:F3:D0:09:8E:A5:2A:C7:A8:3B:42:CF:2C:A9:59:D9:A2:86:9B:B2:E3:A1:81:D9:BF:2F
Signature algorithm name: SHA512withECDSA
Subject Public Key Algorithm: 521-bit EC (secp521r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_5b5666f0-8c3c-46db-8e10-190263885d01_sig_rs512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 88d0bbd883c815c46882674a9e0cb82d4c27bc18231177a262aab71b28e1878
Valid from: Tue Aug 08 06:57:37 CDT 2023 until: Tue Aug 08 06:57:47 CDT 2073
Certificate fingerprints:
         SHA1: E7:6A:0E:37:85:BD:F9:E2:FD:B2:4B:96:A9:3A:97:91:EE:30:91:53
         SHA256: 41:AB:78:C1:91:19:BA:03:13:B1:D6:62:27:1D:6E:0B:53:FC:91:AF:DF:E0:6A:17:61:E5:28:D7:AF:26:BA:E1
Signature algorithm name: SHA512withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_64a3d3e3-503c-4574-a544-ea63bab7f637_enc_ecdh-es
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 58380f109ee41ec443e0e1655e4009f7c782b4e48203464651b92508e37e8cb1
Valid from: Tue Aug 08 06:57:46 CDT 2023 until: Tue Aug 08 06:57:56 CDT 2073
Certificate fingerprints:
         SHA1: DF:F5:B0:63:1B:B4:A1:81:28:FF:FB:0E:97:78:49:B1:0F:A7:9F:CA
         SHA256: 4E:72:58:BB:D6:8E:D1:D1:C8:33:32:2F:58:FA:66:8A:26:1E:10:4F:C3:7F:DD:33:6E:39:38:1F:11:A6:EF:7F
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_9b146bc3-5987-414c-9ed6-7ba735b0ab10_sig_rs384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: d62267e39615f334538d64353a48539673b6c8331f3dbb5bdae6b6897541a631
Valid from: Tue Aug 08 06:57:36 CDT 2023 until: Tue Aug 08 06:57:46 CDT 2073
Certificate fingerprints:
         SHA1: 15:16:97:87:F1:EF:56:12:9E:FB:24:A5:8E:EA:CF:B5:14:9F:81:24
         SHA256: 61:C4:F6:D9:8A:54:A9:31:D6:4D:09:B0:D9:0B:36:01:4F:97:53:B8:0D:09:83:01:E3:1B:67:F6:66:F9:05:0B
Signature algorithm name: SHA384withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_b037a3dc-b4a9-46a6-8b45-66df01da4f0a_sig_ps256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 8decb6c87779103a3915f71963b3c6d6208c7530a6935215b3f7ef6a2832effb
Valid from: Tue Aug 08 06:57:39 CDT 2023 until: Tue Aug 08 06:57:49 CDT 2073
Certificate fingerprints:
         SHA1: 74:9C:1C:D1:D0:15:92:A4:35:38:C7:12:FC:89:D8:19:29:B7:77:EE
         SHA256: AF:39:10:89:1E:0B:08:2F:AF:C1:D4:F9:13:73:1D:72:C9:6B:27:EF:8A:F0:66:D6:89:BA:27:CB:1A:90:16:EA
Signature algorithm name: RSASSA-PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_b0b8feac-e048-407c-afc8-44008b4e88cb_sig_rs256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 9a20c156e837e5f7d974e6ec9c3da25f30170e775f789c4836d3a00d919fe5e5
Valid from: Tue Aug 08 06:57:36 CDT 2023 until: Tue Aug 08 06:57:46 CDT 2073
Certificate fingerprints:
         SHA1: 3F:8D:06:04:A6:9F:ED:C1:39:FC:86:40:C4:8C:19:6D:C2:E3:A9:5E
         SHA256: 3A:1D:39:FC:3C:BD:17:A9:19:A8:45:BB:FD:FE:2A:3A:24:BD:CF:A6:0A:07:17:21:BF:D8:75:84:5E:10:50:16
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_b68c6303-e35e-44f0-b300-e9347e3e3305_sig_ps384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 9bd61db1532aef6c9efa870ccf8cd39caf227037d82c9c0404ba3c95bff46322
Valid from: Tue Aug 08 06:57:40 CDT 2023 until: Tue Aug 08 06:57:50 CDT 2073
Certificate fingerprints:
         SHA1: 34:29:CF:02:EF:D0:C9:CF:A3:6C:79:A8:B9:36:D8:6F:CB:6F:4D:02
         SHA256: 8B:F9:35:9E:86:1E:0A:1C:BC:2E:7C:BB:C9:50:FD:F3:84:51:F6:E8:92:C6:03:2A:0C:EB:74:0C:7D:93:8C:28
Signature algorithm name: RSASSA-PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_cef00de1-5a40-444a-94b4-35d7a290efad_enc_rsa1_5
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 845d78edfaf1912519bddcffffa6aaae9253dd1a087c781132686472763314e2
Valid from: Tue Aug 08 06:57:44 CDT 2023 until: Tue Aug 08 06:57:54 CDT 2073
Certificate fingerprints:
         SHA1: A4:EC:F5:F6:80:A1:51:FE:DD:65:AC:7E:2E:78:C9:0B:FB:82:33:DC
         SHA256: C9:62:C8:12:CB:C6:6B:96:C6:D4:E4:9B:1D:0A:8A:E8:47:0E:C6:EA:70:E2:CE:DE:06:C8:77:4D:84:3E:32:33
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************


Alias name: ssa_cef05e47-6622-43e7-b61d-1209f0dcdf99_sig_es256k
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: ee31a06bda23b8f5a042295d851018c3502a7a8bcd22e2367226c9b5014699a8
Valid from: Tue Aug 08 06:57:38 CDT 2023 until: Tue Aug 08 06:57:48 CDT 2073
Certificate fingerprints:
         SHA1: 22:EC:A2:90:AA:1C:73:0C:23:B2:AA:F8:B2:CD:35:C2:A4:10:0E:0E
         SHA256: F2:B4:3B:93:BA:BF:08:E0:58:30:78:03:8F:C0:60:6A:68:E7:68:7D:00:ED:50:B2:9A:8B:21:C3:9F:71:4A:8A
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256k1) key (disabled)
Version: 3

Extensions:

#1: ObjectId: 2.5.29.37 Criticality=false
ExtendedKeyUsages [
  serverAuth
  clientAuth
  anyExtendedKeyUsage
]



*******************************************
*******************************************
```

All keys are saved in aliases. Every alias has follow format:  
**KeyOpsType + GUID + Use + Algorithm**.

Where:
  - KeyOpsType: values **Connect** | **SSA**
  - Use: **sig** (signature) | **enc** (encryption) 
  - Algorithm:   
      if **Use == sig** follow algorithm values are used: **RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512 EdDSA**  
      if **Use == enc** follow algorithm values are used: **RSA1_5 RSA-OAEP RSA-OAEP-256 ECDH-ES ECDH-ES+A128KW ECDH-ES+A192KW ECDH-ES+A256KW**  
.

This Key Store is used for keys/certificates keeping, which are used by the OpenID provider (**jans-auth-server**).

### smtp-keys.pkcs12

Here is the example of the file: **/etc/certs/smtp-keys.pkcs12** (list of entries/aliases).

```bash
keytool -list -v -storetype PKCS12 -keystore /etc/certs/smtp-keys.pkcs12 -storepass 6VkIGu0DhnrD
```

```text
Keystore type: PKCS12
Keystore provider: SUN

Your keystore contains 1 entry

Alias name: smtp_sig_ec256
Creation date: Aug 8, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=SMTP CA Certificate
Issuer: CN=SMTP CA Certificate
Serial number: 690675c1
Valid from: Tue Aug 08 06:57:12 CDT 2023 until: Fri Aug 05 06:57:12 CDT 2033
Certificate fingerprints:
         SHA1: 69:77:F6:3E:44:0C:3A:BE:0D:58:02:71:21:72:39:12:54:DB:D7:88
         SHA256: E5:E6:CB:BE:76:62:06:2F:A0:C9:49:0F:DD:0D:B1:D1:5B:D6:A2:2C:11:E1:0B:FE:84:67:B0:9D:EB:9B:3A:ED
Signature algorithm name: SHA256withECDSA
Subject Public Key Algorithm: 256-bit EC (secp256r1) key
Version: 3

Extensions:

#1: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 94 D2 A8 BC 8C D7 6F E0   EC BF 2D 92 3A 8D E1 CE  ......o...-.:...
0010: 3D 71 CB B0                                        =q..
]
]



*******************************************
*******************************************
```

This Key Store stores one alias (**smtp_sig_ec256**) and it is used for keeping of key/certificate, which is used by email sending functionality - signing of emails.
Alias **smtp_sig_ec256** contains self-signed certificate. User/Admin can update this KeyStore, adding key/certificate signed by trusted certificate authority (CA) (X-509 PKI).


### BCFKS KeyStore

Janssen also supports **BCFKS** format of KeyStore. As a rule this format is used on **RHEL/DISA-STIG** OS. Access to this type of KeyStore is provided by **BCFIPS** crypto provider. Modules of cryptoprovider can be found (after installing on **RHEL/DISA-STIG** OS) here:

  - /var/gluu/dist/app/bc-fips-1.0.2.3.jar
  - /var/gluu/dist/app/bcpkix-fips-1.0.6.jar
.

Here is example of the BCFKS KeyStore **/etc/certs/jans-auth-keys.bcfks** (list of entries/aliases):

```bash
keytool -list -v -keystore /etc/certs/jans-auth-keys.bcfks -storetype BCFKS -providerpath /var/gluu/dist/app/bc-fips-1.0.2.3.jar:/var/gluu/dist/app/bcpkix-fips-1.0.6.jar -provider org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider -storepass CqE4ApXQxz5y
```

```text
Keystore type: BCFKS
Keystore provider: BCFIPS

Your keystore contains 30 entries

Alias name: connect_01906c9d-fed6-48f1-94b8-85446a692f23_enc_rsa1_5
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 3b795840da219a3a7736d0ac0ebc2d632e925dc4b472a02e4b930eb1d5ca278b
Valid from: Thu Aug 17 04:58:51 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 40:B5:54:85:6B:3B:B1:A9:93:22:6D:91:AA:37:AD:92:A9:B0:5A:A9
	 SHA256: 06:23:28:01:89:6A:96:EA:A6:3E:EA:27:1D:7C:7A:8A:C7:C9:6D:3F:D0:BF:00:69:35:27:CA:CB:54:75:C6:F1
Signature algorithm name: SHA256WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_03a3687a-9c3c-4026-abfc-cafef6a76f92_enc_rsa-oaep
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: df266f1918ae93954aaad44cdceab2ac89be1c20ad16245f1567d751db5d6d1a
Valid from: Thu Aug 17 04:58:52 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 12:12:0B:15:47:A4:C9:AB:44:4A:00:5E:B3:3A:8E:EE:56:70:6F:A7
	 SHA256: 11:F8:12:47:17:E6:D1:D4:80:1F:66:72:22:2C:49:93:72:DE:EF:5E:58:40:74:5B:AE:66:C4:96:F5:9C:86:13
Signature algorithm name: SHA256WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_1b960e55-7b8c-4eeb-9379-b933b8a28fb6_enc_ecdh-es+a128kw
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: ad0cb98a5844cfe6a56c944d269aa3ce6379dc8fea2b0752514753935ae3d821
Valid from: Thu Aug 17 04:58:52 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 28:8F:83:99:24:8E:97:54:5A:B7:41:BB:28:C5:5E:BE:67:38:AF:F4
	 SHA256: E5:DE:D0:4A:17:5B:F8:5D:32:E4:8B:E2:E6:24:D6:22:50:D6:2E:E1:D4:ED:AB:9A:AB:74:B8:24:8C:16:97:14
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: connect_4260d824-790d-440e-b2e0-a1a4ab813169_sig_es512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 28f4866a894a33bb70dcb33138631be67a3b3b2c2b02c31a24913fde2d6d905d
Valid from: Thu Aug 17 04:58:51 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: CD:2C:FC:D2:81:A7:26:EA:C1:63:66:C4:AF:43:20:F3:E0:10:B7:41
	 SHA256: F5:3F:81:C5:B4:CD:8B:7D:B1:E4:83:54:DE:B9:88:F5:1B:3E:DA:CD:A5:93:86:D5:21:E5:3B:3D:42:5C:56:E3
Signature algorithm name: SHA512WITHECDSA
Subject Public Key Algorithm: 521-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: connect_464cc164-24ec-43c2-9f13-bca1c7faab11_sig_es256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: d4786313f02f3444beabbac0ed6c307db4af29e3f044edd047e976efe48c2337
Valid from: Thu Aug 17 04:58:50 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 8A:37:4D:12:F9:24:93:DF:1B:59:90:26:3B:1A:95:5A:F8:46:1E:CB
	 SHA256: B4:28:C6:BC:3D:AA:7C:52:5C:E4:22:FC:E7:E0:94:BF:22:66:91:0F:D6:CF:37:6E:C5:54:ED:0C:30:A8:24:CF
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: connect_53cfa5f1-c227-4843-94d0-e5369cd822b0_sig_rs384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 321e23c27b9ec8a65e7e8fe0e64f68aa7390d713bebfa78b17f8962bb28c1d4f
Valid from: Thu Aug 17 04:58:50 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 6F:D2:4A:FC:48:1F:F3:B7:CC:13:4B:41:3A:BC:19:C4:85:A9:E9:7C
	 SHA256: 43:80:83:D8:84:75:87:5A:24:D6:6B:AC:EC:0D:45:2E:8F:4A:FC:B4:47:D4:D7:49:E8:06:12:2F:98:7C:91:8F
Signature algorithm name: SHA384WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_63e8e482-18ef-410e-bf27-0543a37ac166_sig_ps512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 5fe30fe9ce2b55b6738ec74e0b711c5aa3052ae200fdb9c39bb1392420087d7c
Valid from: Thu Aug 17 04:58:51 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 1C:27:A5:78:64:EA:01:13:C8:62:D4:4C:93:7A:95:F5:CC:C0:55:BA
	 SHA256: F6:CB:47:52:9D:10:B6:31:FB:C2:1F:CC:30:DD:91:DD:2D:85:C0:44:BB:AB:CA:05:9D:47:05:00:7E:35:73:8B
Signature algorithm name: PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_64e35c92-c4f2-4104-81ef-7a366f2dbc38_sig_ps384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 6742d9b42e180b7f4a1f58121dddb02896ff1638e8254bd74415a90e47718e91
Valid from: Thu Aug 17 04:58:51 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 75:AB:EA:2C:D7:C8:87:86:7C:FE:0D:23:33:24:CE:FD:75:3E:E5:E0
	 SHA256: EA:7D:7F:F1:51:00:5F:D8:80:A5:AF:67:F4:E1:84:A2:E5:D0:0A:8F:A3:98:81:29:36:9B:14:DA:59:02:76:AA
Signature algorithm name: PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_71a098e5-0fe0-4946-85f8-adf0d6759413_enc_ecdh-es+a256kw
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 285d3bcd2569a9a0db6091c184e47bf855edf758251749b739267b7f6f9821d3
Valid from: Thu Aug 17 04:58:52 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: D6:D7:0C:33:6E:A8:A4:54:2A:1F:2D:59:7D:2C:26:0A:EB:B8:AE:41
	 SHA256: EC:82:5E:A3:66:99:FB:3E:64:B7:47:27:AA:0D:13:C6:64:E1:42:41:B4:8E:F7:B9:23:20:AA:15:1E:F3:22:F7
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: connect_82a7b334-4756-4201-a432-1b181badf695_sig_ps256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: cce1b501329cff10503a96587042b4f37cc994ac3f98c615bab4c9680a53d769
Valid from: Thu Aug 17 04:58:51 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 37:1C:A3:F4:A3:04:E4:DC:AA:F3:21:AD:BD:B0:5F:D8:0A:95:7B:29
	 SHA256: D7:CA:AE:57:A3:43:1E:94:93:ED:C8:34:72:29:DD:CE:02:BD:E5:69:6D:34:D3:7D:26:AE:78:19:4C:E9:BF:CE
Signature algorithm name: PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_a3d528cf-fd43-4015-92be-1e644310bbf0_enc_ecdh-es
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: de13b1e646dd2456c3512de303b2fb272108bcdb40c180717077820c07e32f65
Valid from: Thu Aug 17 04:58:52 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: EF:5E:62:AA:89:31:70:83:35:40:CB:9E:30:23:32:21:12:77:DC:83
	 SHA256: 88:1A:A2:85:DB:27:46:04:EE:E5:DC:8A:5E:A7:77:CC:36:C5:A6:1A:E7:A1:85:44:2E:E4:A0:91:8A:FD:A4:6F
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: connect_ab2f67de-14fb-4312-89fa-56b2cd70f616_sig_es256k
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: d36755d2a472837ab9fe1dad688b5a1881bff6073d5a494af0275226f236146c
Valid from: Thu Aug 17 04:58:51 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 8B:FD:A9:75:7C:33:1E:9D:03:47:58:2E:31:D6:49:30:E6:65:6B:A7
	 SHA256: 5C:E2:28:7B:B0:66:17:A8:14:82:49:14:16:E8:40:B3:B6:C1:5F:90:77:25:A3:C4:A5:25:E6:77:CB:B9:92:91
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: connect_ae7adf42-8f9b-4fe0-bbf1-e9e1016904f5_enc_rsa-oaep-256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 324a9b70a8410e9a02eb596e536cda516f91b250befb22699a909d0b2505e03
Valid from: Thu Aug 17 04:58:52 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 66:CF:B6:EB:27:10:CC:45:E4:CF:8A:66:71:05:97:22:BA:47:6B:33
	 SHA256: EF:16:46:1F:C5:0B:41:0B:B4:06:A7:9D:7F:EB:81:76:5F:35:B3:E4:09:67:95:D2:2A:10:D3:2F:59:DA:5D:73
Signature algorithm name: SHA256WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_b52de13d-33fe-446f-b4fa-6b4ce816c18e_sig_rs256
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 4d4b22cacdd15cd7bbf339c18ef14fba4ba7b0f13ad37f9452e00404488a880c
Valid from: Thu Aug 17 04:58:50 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 47:03:76:16:4B:D9:5C:83:AC:C7:40:10:F3:46:72:86:15:21:6C:CD
	 SHA256: A3:3A:22:75:E2:E5:5B:69:31:FB:E8:59:E0:90:20:60:BC:47:30:9C:5C:5A:B6:97:92:37:71:CD:91:BF:93:78
Signature algorithm name: SHA256WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_c82cc876-6e92-408c-b519-76d153aad42d_enc_ecdh-es+a192kw
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 586b7dd9cecd3ef8c58b1bac766caec5d4c2ac8097c1cc4a22a20905ce5f9229
Valid from: Thu Aug 17 04:58:52 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: E6:3E:62:82:3C:6F:FE:CF:85:FD:6D:82:A9:7D:68:03:38:F0:AB:63
	 SHA256: 0D:D0:1B:50:02:0B:41:C1:35:CD:A9:7A:E5:10:AA:88:F2:AD:F7:8E:88:C2:3F:68:A9:33:E6:1B:EB:28:71:30
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: connect_d1001a39-29a1-45e4-ae0d-2eee86a83ada_sig_rs512
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: ba44ac71c5a340aec0d8e0078b059c36a2819915774e8403e1b589a330570833
Valid from: Thu Aug 17 04:58:50 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 49:59:6A:9D:D6:E6:98:05:18:BD:25:1A:C5:C0:2B:A6:C3:3E:36:52
	 SHA256: DA:57:65:45:A2:16:76:E0:B7:20:DA:82:BF:77:AF:B9:58:D5:E7:D3:2F:1B:D3:BC:48:51:E2:D1:C2:41:A9:FA
Signature algorithm name: SHA512WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: connect_f32a5790-4e43-4629-95bd-37b35d7c2e39_sig_es384
Creation date: Aug 17, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 9f0340f5c5cafc1816d2a31c20846fb24d400b3b5194a629ac5baf6db6d15e60
Valid from: Thu Aug 17 04:58:51 CDT 2023 until: Sat Aug 19 05:58:59 CDT 2023
Certificate fingerprints:
	 SHA1: 5A:37:CA:6A:27:AB:9F:FC:6B:C3:D3:C6:B4:47:F3:10:F4:C9:F3:B3
	 SHA256: 11:A4:9C:ED:C0:05:B9:27:89:2B:17:52:11:AA:34:B4:E7:81:08:13:44:7D:93:3A:DE:CA:23:A4:22:F7:05:9F
Signature algorithm name: SHA384WITHECDSA
Subject Public Key Algorithm: 384-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: ssa_0639ba94-902c-4ab6-923a-58137554f667_enc_ecdh-es
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 6fac3869f3fc7297470eee65fc4315328edaf475798d5bbc469de652f3088ca3
Valid from: Thu Aug 10 18:20:51 CDT 2023 until: Thu Aug 10 18:21:01 CDT 2073
Certificate fingerprints:
	 SHA1: 76:B5:F0:B9:1C:88:54:95:5B:11:2A:6A:AE:2E:C3:02:59:1B:07:2C
	 SHA256: 7B:26:2E:36:4B:27:AA:C0:5B:FE:2C:2E:73:B1:75:64:2D:3E:0B:D2:C4:8D:F6:63:12:E8:2E:4C:46:EB:35:4C
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: ssa_0d94f183-878c-445d-9917-28357b15c4ee_sig_rs256
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 6d76a82ed166e13ecc2fb0742ebc72f619af0b738baf69438d2aba4262cfdbc4
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: 14:8E:19:D8:8D:FD:68:14:40:72:35:1F:93:2A:7D:83:81:7E:6C:D8
	 SHA256: CB:69:E0:7C:55:2C:39:56:F8:17:ED:72:96:47:DB:8C:90:1A:1E:2A:E8:33:CA:79:96:DD:44:9F:F7:63:A2:D4
Signature algorithm name: SHA256WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_1a5ea0d5-0c70-43d2-ab23-e324620b413b_sig_ps512
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 42bd6fbc27749cd6598c54aa0928e93057dc6c6c083c9f73d4e2046c4bac02d0
Valid from: Thu Aug 10 18:20:51 CDT 2023 until: Thu Aug 10 18:21:01 CDT 2073
Certificate fingerprints:
	 SHA1: D3:D1:31:59:33:D1:A6:86:8F:A5:F8:7F:D6:15:44:53:15:99:6D:DC
	 SHA256: 88:95:8C:89:F3:E7:28:E8:3D:9F:47:39:E2:F8:F5:FC:FA:63:71:B3:81:F0:9C:98:7E:A4:0A:FD:7E:7E:E9:AD
Signature algorithm name: PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_20d8cce9-0994-4fca-9cfa-f5fbe22eb940_sig_es256k
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: a192c3ca749b78a153cb3134c50fddcdd5dd401dd399b831199ba383eddf355b
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: 4E:94:55:21:15:53:1E:3E:3B:6E:85:B9:BF:A7:4B:46:2A:FB:09:4F
	 SHA256: BA:AB:58:DF:29:7C:5A:D5:31:2F:B9:8B:CE:E9:3F:0B:5B:E5:01:94:CC:A8:9B:41:8B:45:9B:30:D2:63:D6:24
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: ssa_33b1591d-1b79-490f-a980-573486ec620b_sig_es384
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: ba8a0e96843f1a5181a96d2fc1bdae8b6954bb70b2b5c1fad4895438f4079dd0
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: B8:97:F4:EA:85:14:41:24:8C:DC:E0:8F:66:18:FD:31:7E:0B:5D:D5
	 SHA256: F3:9E:1F:25:E2:03:3C:7B:69:15:12:01:E2:94:EF:1E:95:43:43:A2:CB:8E:88:88:8A:3D:53:5E:82:E9:0E:9C
Signature algorithm name: SHA384WITHECDSA
Subject Public Key Algorithm: 384-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: ssa_34943497-d9fb-43ca-8ee1-1437e1dcf78e_sig_ps384
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: b45de18f13366f309b100be313770edc6e80942771b08d656e699f7e081e5a69
Valid from: Thu Aug 10 18:20:51 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: C4:2D:DB:4D:06:7F:27:89:9A:C0:84:DB:08:BF:0C:75:3B:5B:74:22
	 SHA256: 77:07:D2:90:7C:AD:7A:F1:D8:EE:C1:84:54:B6:E5:41:7B:BB:C7:8F:64:96:D4:D3:ED:FD:32:F3:6C:10:11:62
Signature algorithm name: PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_475830af-d019-4e04-9fb9-e68f0491284a_sig_es512
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 69e6093118ce51ec5f21477e2946dd023f3c5b70bbde2922b26139b653eddd62
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: 30:9D:95:9C:AE:60:F9:3F:D8:7A:AD:89:9D:AC:5D:7F:05:52:F9:50
	 SHA256: CA:90:7B:75:3C:99:CE:F2:42:DE:55:38:83:CB:C2:EA:F8:E7:0D:2B:00:5F:57:CF:75:D5:33:C4:5E:32:7E:A7
Signature algorithm name: SHA512WITHECDSA
Subject Public Key Algorithm: 521-bit EC key
Version: 3


*******************************************
*******************************************


Alias name: ssa_59bb3fe7-bf2a-4244-85a7-dd18785bcac6_enc_rsa-oaep
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 7f3e4adf1e08994ba83a1a59584c6f402e584baaecf1735ff9e0add33d65a423
Valid from: Thu Aug 10 18:20:51 CDT 2023 until: Thu Aug 10 18:21:01 CDT 2073
Certificate fingerprints:
	 SHA1: FC:D7:45:92:F4:60:73:C6:7C:19:DA:AB:EC:54:FF:B4:62:C6:AC:CB
	 SHA256: 66:05:C9:4C:3D:CA:EC:9F:29:06:57:00:A8:90:09:24:17:41:71:DC:1F:8E:7F:E7:5B:39:D8:A1:8B:A3:5A:F2
Signature algorithm name: SHA256WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_5e0f07d5-1e91-4bd3-bf56-6caa8a137ea6_enc_rsa1_5
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 3449c41d6f7c1b5b7f33c7411a81f41883f6f1fccb64d4e2bc22c130d771a26a
Valid from: Thu Aug 10 18:20:51 CDT 2023 until: Thu Aug 10 18:21:01 CDT 2073
Certificate fingerprints:
	 SHA1: 1F:55:2E:62:3D:7E:FE:92:20:13:0D:FC:33:F8:82:B1:B7:BF:9E:7A
	 SHA256: C9:6E:48:AF:F7:8B:04:30:FB:4B:30:47:A2:83:1E:4C:AF:9E:04:E6:07:8A:B1:AE:3B:CD:92:AE:CB:97:D2:EB
Signature algorithm name: SHA256WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_7d9cec29-697e-4e3e-9991-eba03394b69c_sig_ps256
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 15e7eed8194a80919e9bd699212efca7e50ec9aa3834c6a8e32a7eeb9d9e1da9
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: A5:BE:24:97:EB:F5:0F:18:05:56:02:BF:66:24:98:24:59:82:5A:E6
	 SHA256: 16:EF:DD:45:17:3B:D7:66:36:14:7E:96:03:1F:51:02:6C:86:1E:19:E7:43:C7:DF:E9:A9:D0:86:9C:A6:7C:B8
Signature algorithm name: PSS
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_96ea824f-f915-4da8-8f76-3b4bf8bdf552_sig_rs384
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 4c79afa5d03e2aaab17c90519d59dec517de004f4c9b62960ebf10276b699105
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: E8:F3:5D:16:D0:2C:47:98:D4:F6:98:A1:5F:46:A0:DB:BD:F8:72:30
	 SHA256: 15:42:A4:30:FE:DB:D8:C4:61:11:A6:8D:10:51:02:64:70:C7:EA:BF:F3:C8:F3:42:03:D9:F8:00:19:F9:7C:28
Signature algorithm name: SHA384WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_b7ee7e46-0a6b-4040-b6b3-a78e9dedc116_sig_rs512
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 82f95026558b89f3ca8c1129d3b79c74ce2ff70bb849991a8305afea974ebe1c
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: D8:45:78:27:94:39:93:57:DF:67:56:3B:06:5C:0C:2A:20:93:F6:0F
	 SHA256: 6D:55:8E:F3:5F:6A:EA:6D:37:71:A9:57:1D:32:98:92:04:5E:8B:4A:C4:FC:E1:ED:B1:7A:D4:11:BF:38:02:80
Signature algorithm name: SHA512WITHRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3


*******************************************
*******************************************


Alias name: ssa_b9d65217-ff51-4eab-914f-aeb40eb30bc2_sig_es256
Creation date: Aug 10, 2023
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Jans Auth CA Certificates
Issuer: CN=Jans Auth CA Certificates
Serial number: 632a5f6e2bf5c5dbabf7376718c35b3101fe8a8065a6bf54db3dde5a1825dbf4
Valid from: Thu Aug 10 18:20:50 CDT 2023 until: Thu Aug 10 18:21:00 CDT 2073
Certificate fingerprints:
	 SHA1: 7D:51:C1:44:45:EF:6B:71:3D:EF:BC:20:E0:AE:59:4C:3D:16:10:23
	 SHA256: CC:1C:60:5E:E4:6A:B4:C4:7F:3F:2D:1B:2F:12:B4:79:27:94:3D:1D:D5:FA:C4:11:0E:53:76:DA:52:34:B0:62
Signature algorithm name: SHA256WITHECDSA
Subject Public Key Algorithm: 256-bit EC key
Version: 3


*******************************************
*******************************************
```
.