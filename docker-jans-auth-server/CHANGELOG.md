# Changelog

## [1.0.22-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.21-1...docker-jans-auth-server-v1.0.22-1) (2024-01-22)


### Features

* **docker-jans:** add jans-lock as custom library in jans-auth ([#7381](https://github.com/JanssenProject/jans/issues/7381)) ([8a45b10](https://github.com/JanssenProject/jans/commit/8a45b103c9e69d48978139906313a6a4ed56b78d))
* **docker-jans:** add support for passing jetty.http.idleTimeout option ([#7298](https://github.com/JanssenProject/jans/issues/7298)) ([a1a2e10](https://github.com/JanssenProject/jans/commit/a1a2e1062c0759a40c6d45b48158ff8741473ada))
* **docker:** support for OAuth 2.0 Rich Authorization Requests ([#7196](https://github.com/JanssenProject/jans/issues/7196)) ([f02db0f](https://github.com/JanssenProject/jans/commit/f02db0f14f47e69bce654e5c86d8a3b621cdb984))


### Bug Fixes

* prepare for 1.0.22 release ([#7455](https://github.com/JanssenProject/jans/issues/7455)) ([4bf2562](https://github.com/JanssenProject/jans/commit/4bf2562050c86317658259c72bb641780a283579))
* resolve install failure in CN setup ([#7439](https://github.com/JanssenProject/jans/issues/7439)) ([f84e99a](https://github.com/JanssenProject/jans/commit/f84e99a1a6257854cd23e2cc16aa1a3a5d0790b6))

## [1.0.21-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.20-1...docker-jans-auth-server-v1.0.21-1) (2023-12-14)


### Features

* **docker-jans:** add archived jwks ([#6564](https://github.com/JanssenProject/jans/issues/6564)) ([119d9ad](https://github.com/JanssenProject/jans/commit/119d9ade7cb4ce60b08825e6cda2f43fd153eadf))
* **docker-jans:** use mixed strategy for Agama serialization ([#6889](https://github.com/JanssenProject/jans/issues/6889)) ([289cf26](https://github.com/JanssenProject/jans/commit/289cf26ec6e2e019187346fb6aea05b135edbf8b))


### Bug Fixes

* prepare for 1.0.21 release ([#7008](https://github.com/JanssenProject/jans/issues/7008)) ([2132de6](https://github.com/JanssenProject/jans/commit/2132de6683f67bf22d5a863b149770d657073a83))
* remove agama inbound jar dependency ([#7095](https://github.com/JanssenProject/jans/issues/7095)) ([c58c55c](https://github.com/JanssenProject/jans/commit/c58c55cca0a88872d791941c9f7abca45a09813c))

## [1.0.20-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.19-1...docker-jans-auth-server-v1.0.20-1) (2023-11-08)


### Features

* **docker-all-in-one:** create image requirements ([#6340](https://github.com/JanssenProject/jans/issues/6340)) ([67e8abc](https://github.com/JanssenProject/jans/commit/67e8abc6a3067f4949ea0c4da4d41b370eef53fb))
* **jans-pycloudlib:** add lock to prevent race condition ([#6329](https://github.com/JanssenProject/jans/issues/6329)) ([9dd82da](https://github.com/JanssenProject/jans/commit/9dd82da5c87ee829c73a1135ce8740b8353f8ab5))


### Bug Fixes

* **jans-pycloudlib:** incorrect persistence entry check for ldap and couchbase ([#6297](https://github.com/JanssenProject/jans/issues/6297)) ([87ac453](https://github.com/JanssenProject/jans/commit/87ac453c121b9bdd2dfd7cb4ebb7b8628f322474))
* prepare for 1.0.20 release ([c6e806e](https://github.com/JanssenProject/jans/commit/c6e806eb31fed998d52cbef7a7d94c231d913102))
* update default docker images build names ([#6388](https://github.com/JanssenProject/jans/issues/6388)) ([34e2700](https://github.com/JanssenProject/jans/commit/34e27001fc9ed378f218b8c7460209027a0d8812))

## [1.0.19-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.18-1...docker-jans-auth-server-v1.0.19-1) (2023-10-11)


### Features

* **docker-jans-casa:** migrate Casa image from Flex ([#6194](https://github.com/JanssenProject/jans/issues/6194)) ([c83fe1e](https://github.com/JanssenProject/jans/commit/c83fe1ebaf2bbbe246681312a2c7aa76ff34e1d0))
* **docker-jans:** upgrade base image to Java 17 ([#6231](https://github.com/JanssenProject/jans/issues/6231)) ([8ed40e9](https://github.com/JanssenProject/jans/commit/8ed40e91a56c256cb34262659b6e0657571f8c97))


### Bug Fixes

* prepare for 1.0.19 release ([554fd43](https://github.com/JanssenProject/jans/commit/554fd434f624c4b4be3b2031c472177709da8966))

## [1.0.18-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.17-1...docker-jans-auth-server-v1.0.18-1) (2023-09-23)


### Bug Fixes

* prepare for 1.0.18 release ([87af7e4](https://github.com/JanssenProject/jans/commit/87af7e4d41728ce2966362883b47e5354f8c3803))

## [1.0.17-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.16-1...docker-jans-auth-server-v1.0.17-1) (2023-09-17)


### Features

* **docker-jans:** sync assets of images ([#5907](https://github.com/JanssenProject/jans/issues/5907)) ([acbc763](https://github.com/JanssenProject/jans/commit/acbc763f31422c3d9f80f44ade82cc8519dc4fa4))


### Bug Fixes

* **docker-jans:** update jetty to v11.0.16 ([#6010](https://github.com/JanssenProject/jans/issues/6010)) ([d8104cd](https://github.com/JanssenProject/jans/commit/d8104cd985d1ca869135b97f1f2e1c02f3bfd5ff))
* prepare for 1.0.17 release ([4ba8c15](https://github.com/JanssenProject/jans/commit/4ba8c151734f02d762e902b46a35cae2d498fa8f))

## [1.0.16-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.15-1...docker-jans-auth-server-v1.0.16-1) (2023-08-02)


### Features

* **docker-jans:** add dpop nonce ([#5640](https://github.com/JanssenProject/jans/issues/5640)) ([15bb0c1](https://github.com/JanssenProject/jans/commit/15bb0c1e019522aa3a911c1784dae53afb90d408))


### Bug Fixes

* prepare for 1.0.16 release ([042ce79](https://github.com/JanssenProject/jans/commit/042ce7941b9597fade8d5f10e40a89d9e7662315))
* prepare for 1.0.16 release ([b2649c3](https://github.com/JanssenProject/jans/commit/b2649c33a9857f356f91df2f38787ec56269e6dd))

## [1.0.15-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.14-1...docker-jans-auth-server-v1.0.15-1) (2023-07-12)


### Bug Fixes

* prepare for 1.0.15 release ([0e3cc2f](https://github.com/JanssenProject/jans/commit/0e3cc2f5ea287c2c35f45def54f074daa473ec49))

## [1.0.14-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.13-1...docker-jans-auth-server-v1.0.14-1) (2023-06-12)


### Bug Fixes

* **docker-jans-auth-server:** compare contents before pushing otp_configuration and super_gluu_creds to secrets ([#4798](https://github.com/JanssenProject/jans/issues/4798)) ([23f2630](https://github.com/JanssenProject/jans/commit/23f2630f6caa71f253a23cc509e1b8c0bca5b891))
* **docker-jans-auth-server:** handle missing secret when comparing contents ([#5187](https://github.com/JanssenProject/jans/issues/5187)) ([f2a373e](https://github.com/JanssenProject/jans/commit/f2a373e95b73d38c8f867fd686152aa18248c392))
* prepare for 1.0.14 release ([25ccadf](https://github.com/JanssenProject/jans/commit/25ccadf85327ea14685c6066dc6609919e4f2865))

## [1.0.13-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.12-1...docker-jans-auth-server-v1.0.13-1) (2023-05-10)


### Bug Fixes

* ensure google libs support all possible credentials ([#4777](https://github.com/JanssenProject/jans/issues/4777)) ([d0759e5](https://github.com/JanssenProject/jans/commit/d0759e595517ca16b97a1ce4f7cd168b29ff17dd))
* prepare for 1.0.13 release ([493478e](https://github.com/JanssenProject/jans/commit/493478e71f6231553c998b48c0f163c7f5869da4))

## [1.0.12-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.11-1...docker-jans-auth-server-v1.0.12-1) (2023-04-18)


### Bug Fixes

* opencontainer labels ([f8b0b36](https://github.com/JanssenProject/jans/commit/f8b0b365600bd700128bb9d92df6de7ba14830b6))
* prepare for 1.0.12 release ([6f83197](https://github.com/JanssenProject/jans/commit/6f83197705511c39413456acdc64e9136a97ff39))

## [1.0.11-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.10-1...docker-jans-auth-server-v1.0.11-1) (2023-04-05)


### Features

* **docker-jans:** upstream changes for license credentials ([#4416](https://github.com/JanssenProject/jans/issues/4416)) ([1565095](https://github.com/JanssenProject/jans/commit/15650953e2f6ee449d389682dd624028c9001c47))


### Bug Fixes

* **docker-jans:** update smtp config and agama script entry ([#4384](https://github.com/JanssenProject/jans/issues/4384)) ([d24dffb](https://github.com/JanssenProject/jans/commit/d24dffb93ed445b00819d696d87e046bcf5eb269))
* prepare for  release ([60775c0](https://github.com/JanssenProject/jans/commit/60775c09dc5ab9996bf80c03dcb457861d48dfb1))

## [1.0.10-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.9-1...docker-jans-auth-server-v1.0.10-1) (2023-03-16)


### Bug Fixes

* add custom permissions ([34336ac](https://github.com/JanssenProject/jans/commit/34336ac3799872303b9e4891b2405267ea9b6fd8))
* prepare release for 1.0.10 ([e996926](https://github.com/JanssenProject/jans/commit/e99692692ef04d881468d120f7c7d462568dce36))

## [1.0.9-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.7-1...docker-jans-auth-server-v1.0.9-1) (2023-03-09)


### Bug Fixes

* prepare 1.0.9 release ([e6ea522](https://github.com/JanssenProject/jans/commit/e6ea52220824bd6b5d2dca0539d9d515dbeda362))
* prepare 1.0.9 release ([55f7e0c](https://github.com/JanssenProject/jans/commit/55f7e0c308b869c2c4b5668aca751d022138a678))
* update next SNAPSHOT and dev ([0df0e7a](https://github.com/JanssenProject/jans/commit/0df0e7ae06af64ac477955119c2522f03e0603c3))

## 1.0.7-1 (2023-02-22)


### Features

* **docker-jans:** add support for mounted hybrid properties file ([#3623](https://github.com/JanssenProject/jans/issues/3623)) ([8c58a5a](https://github.com/JanssenProject/jans/commit/8c58a5a9530cc9c44e7009ce3952064f8610cc69))
* **docker-jans:** enable prefix and group for stdout logs ([#3481](https://github.com/JanssenProject/jans/issues/3481)) ([e7684e7](https://github.com/JanssenProject/jans/commit/e7684e7f6da7c789d03311fe2df855c687aa7fa6))
* **docker-jans:** introduce key_ops when generating keys ([#3770](https://github.com/JanssenProject/jans/issues/3770)) ([2495842](https://github.com/JanssenProject/jans/commit/249584257c3e892f5106d0e3559d1c0caa4a8d77))


### Bug Fixes

* modify clnId column type ([#3459](https://github.com/JanssenProject/jans/issues/3459)) ([701394c](https://github.com/JanssenProject/jans/commit/701394c3d7463ffb2bd223daf9662921244ad34d))
* prepare 1.0.7 release ([ce02fd9](https://github.com/JanssenProject/jans/commit/ce02fd9322ab49d5bea4f6e88f316f931e9d2169))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))

## 1.0.6-1 (2023-01-09)


### Features

* **docker-jans:** enable prefix and group for stdout logs ([#3481](https://github.com/JanssenProject/jans/issues/3481)) ([e7684e7](https://github.com/JanssenProject/jans/commit/e7684e7f6da7c789d03311fe2df855c687aa7fa6))
* **image:** add custom libs for couchbase and spanner persistence ([#2784](https://github.com/JanssenProject/jans/issues/2784)) ([db559dd](https://github.com/JanssenProject/jans/commit/db559ddc5e74cc7387720af7f084766c054541b5))
* **jans-pycloudlib:** add AWS Secrets Manager support for configuration layers ([#3112](https://github.com/JanssenProject/jans/issues/3112)) ([0522e61](https://github.com/JanssenProject/jans/commit/0522e61809b9052adce4fdb0db77e2d71558144e))


### Bug Fixes

* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* modify clnId column type ([#3459](https://github.com/JanssenProject/jans/issues/3459)) ([701394c](https://github.com/JanssenProject/jans/commit/701394c3d7463ffb2bd223daf9662921244ad34d))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))
* **pycloudlib:** searching values from spanner returns empty set ([#2833](https://github.com/JanssenProject/jans/issues/2833)) ([861a065](https://github.com/JanssenProject/jans/commit/861a0657233f271ffa41c908ce68a2206ed970fd))

## [1.0.5-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.4-1...docker-jans-auth-server-v1.0.5-1) (2022-12-01)


### Features

* **jans-pycloudlib:** add AWS Secrets Manager support for configuration layers ([#3112](https://github.com/JanssenProject/jans/issues/3112)) ([0522e61](https://github.com/JanssenProject/jans/commit/0522e61809b9052adce4fdb0db77e2d71558144e))


### Bug Fixes

* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))

## [1.0.4-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.3-1...docker-jans-auth-server-v1.0.4-1) (2022-11-08)


### Bug Fixes

* **pycloudlib:** searching values from spanner returns empty set ([#2833](https://github.com/JanssenProject/jans/issues/2833)) ([861a065](https://github.com/JanssenProject/jans/commit/861a0657233f271ffa41c908ce68a2206ed970fd))

## 1.0.3-1 (2022-11-01)


### Features

* **image:** add custom libs for couchbase and spanner persistence ([#2784](https://github.com/JanssenProject/jans/issues/2784)) ([db559dd](https://github.com/JanssenProject/jans/commit/db559ddc5e74cc7387720af7f084766c054541b5))
* **image:** generate scopes from swagger/openapi files ([#2759](https://github.com/JanssenProject/jans/issues/2759)) ([63722ea](https://github.com/JanssenProject/jans/commit/63722ea7372f33bf2ad2c3ff01b068383e81e746))


### Bug Fixes

* **image:** handle vulnerabilities reported by artifacthub scanner ([#2436](https://github.com/JanssenProject/jans/issues/2436)) ([77d8d88](https://github.com/JanssenProject/jans/commit/77d8d888bf414e519345704d033e65fbf4bc4128))
* **images:** conform to new couchbase persistence configuration ([#2188](https://github.com/JanssenProject/jans/issues/2188)) ([c708542](https://github.com/JanssenProject/jans/commit/c7085427fd298f74e8809ef4d6c39f780fa83776))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))
* **pycloudlib:** handle type mismatch for iterable ([#2004](https://github.com/JanssenProject/jans/issues/2004)) ([46e0b2e](https://github.com/JanssenProject/jans/commit/46e0b2e4aff70a97cdcdcd0102dc83d294e45fdc))
* **pycloudlib:** set default values for JSONB column ([#2651](https://github.com/JanssenProject/jans/issues/2651)) ([9b536ab](https://github.com/JanssenProject/jans/commit/9b536ab2b5d398a41733790f2eeb70339f993fb7))


### Miscellaneous Chores

* release 1.0.2-1 ([d01b51a](https://github.com/JanssenProject/jans/commit/d01b51a847bb2f67b52da433ebd1c5e4a66b7c1a))

## 1.0.2-1 (2022-08-30)


### Features

* add support for requestUriBlockList config ([#1572](https://github.com/JanssenProject/jans/issues/1572)) ([63b3b74](https://github.com/JanssenProject/jans/commit/63b3b7418861e8f4234c54a8d81fe5d21f56387f))
* expose prometheus metrics via jmx exporter ([#1573](https://github.com/JanssenProject/jans/issues/1573)) ([205e320](https://github.com/JanssenProject/jans/commit/205e3206cf87bdb7cf0908bfdd7ee777d1ab955d))
* introduce new hybrid persistence mapping ([#1505](https://github.com/JanssenProject/jans/issues/1505)) ([a77ab60](https://github.com/JanssenProject/jans/commit/a77ab602d15cb6bdf4751aaa11c2be9485b04a34))


### Bug Fixes

* a workaround for fido2 dependency ([#1590](https://github.com/JanssenProject/jans/issues/1590)) ([527c928](https://github.com/JanssenProject/jans/commit/527c928d5769320a57d203d59175077e10c2d30a))
* **images:** conform to new couchbase persistence configuration ([#2188](https://github.com/JanssenProject/jans/issues/2188)) ([c708542](https://github.com/JanssenProject/jans/commit/c7085427fd298f74e8809ef4d6c39f780fa83776))
* **pycloudlib:** handle type mismatch for iterable ([#2004](https://github.com/JanssenProject/jans/issues/2004)) ([46e0b2e](https://github.com/JanssenProject/jans/commit/46e0b2e4aff70a97cdcdcd0102dc83d294e45fdc))
* update external modules for otp/fido2 ([#1589](https://github.com/JanssenProject/jans/issues/1589)) ([fc42181](https://github.com/JanssenProject/jans/commit/fc4218110e5130878836a663aba72e67dcefcd10))


### Miscellaneous Chores

* prepare docker images release 1.0.1-1 ([12660a8](https://github.com/JanssenProject/jans/commit/12660a800bacb210bd3fb4b35c9156e9c5445343))
* release 1.0.2-1 ([d01b51a](https://github.com/JanssenProject/jans/commit/d01b51a847bb2f67b52da433ebd1c5e4a66b7c1a))

## [1.0.1-1](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.0-1...docker-jans-auth-server-v1.0.1-1) (2022-07-06)


### Features

* add support for requestUriBlockList config ([#1572](https://github.com/JanssenProject/jans/issues/1572)) ([63b3b74](https://github.com/JanssenProject/jans/commit/63b3b7418861e8f4234c54a8d81fe5d21f56387f))
* expose prometheus metrics via jmx exporter ([#1573](https://github.com/JanssenProject/jans/issues/1573)) ([205e320](https://github.com/JanssenProject/jans/commit/205e3206cf87bdb7cf0908bfdd7ee777d1ab955d))
* introduce new hybrid persistence mapping ([#1505](https://github.com/JanssenProject/jans/issues/1505)) ([a77ab60](https://github.com/JanssenProject/jans/commit/a77ab602d15cb6bdf4751aaa11c2be9485b04a34))


### Bug Fixes

* a workaround for fido2 dependency ([#1590](https://github.com/JanssenProject/jans/issues/1590)) ([527c928](https://github.com/JanssenProject/jans/commit/527c928d5769320a57d203d59175077e10c2d30a))
* update external modules for otp/fido2 ([#1589](https://github.com/JanssenProject/jans/issues/1589)) ([fc42181](https://github.com/JanssenProject/jans/commit/fc4218110e5130878836a663aba72e67dcefcd10))


### Miscellaneous Chores

* prepare docker images release 1.0.1-1 ([12660a8](https://github.com/JanssenProject/jans/commit/12660a800bacb210bd3fb4b35c9156e9c5445343))

## 1.0.0-1 (2022-05-20)


### Features

* add helper to create persistence entry from LDIF file ([#1262](https://github.com/JanssenProject/jans/issues/1262)) ([f2e653e](https://github.com/JanssenProject/jans/commit/f2e653ef917efd017195f2330b64e64c333f4699))
* adjust ownership and permission to avoid bloated images ([#1312](https://github.com/JanssenProject/jans/issues/1312)) ([d016682](https://github.com/JanssenProject/jans/commit/d0166821baf52665934c0eaa38de8b2f51825456))
* **jans:** jetty 11 integration ([#1123](https://github.com/JanssenProject/jans/issues/1123)) ([6c1caa1](https://github.com/JanssenProject/jans/commit/6c1caa1c4c92d28571f8589cd701e6885d4d85ef))
* remove Jython's pip from images ([#1176](https://github.com/JanssenProject/jans/issues/1176)) ([e3f374f](https://github.com/JanssenProject/jans/commit/e3f374f5bc3c385374593455243c88e2f7dfc00d))


### Bug Fixes

* set permission for jans-auth.xml explicitly ([#1315](https://github.com/JanssenProject/jans/issues/1315)) ([80f33a2](https://github.com/JanssenProject/jans/commit/80f33a23902af7498fa85d2785abe1af77a1751e))
* typo and indexing error ([#1125](https://github.com/JanssenProject/jans/issues/1125)) ([dc87dc0](https://github.com/JanssenProject/jans/commit/dc87dc01c4c63d6fcc2b967ce97a52880083b95f))


### Miscellaneous Chores

* prepare release 1.0.0-1 ([8985928](https://github.com/JanssenProject/jans/commit/89859286d69e7de7885bd9da9f50720c8371e797))
* release 1.0.0 ([b2895f2](https://github.com/JanssenProject/jans/commit/b2895f224b5772c0724ea0afbdf67a417a5c537c))
* release 1.0.0-beta.16 ([a083ad6](https://github.com/JanssenProject/jans/commit/a083ad6b1d43201126e8d4f690a55ea1b109524c))
* release 1.0.0-beta.16 ([90e4bb2](https://github.com/JanssenProject/jans/commit/90e4bb29df040bd9fe5921a054bc4226d34ca1ef))
* release 1.0.0-beta.16 ([eec2073](https://github.com/JanssenProject/jans/commit/eec2073be9fd25544f31087e171934afb9a71e6d))
* release 1.0.0-beta.16 ([cd92ead](https://github.com/JanssenProject/jans/commit/cd92ead2ca654383091c4923d3de5619b70fc5b9))
* release 1.0.0-beta.16 ([7f0a91b](https://github.com/JanssenProject/jans/commit/7f0a91bd90efc1cd7a80047f9cd6b7c6a22417a2))
* release 1.0.0-beta.16 ([c2ad604](https://github.com/JanssenProject/jans/commit/c2ad604dc29e7401bc4cb0788feaa20e11de0440))
* release 1.0.0-beta.16 ([a641486](https://github.com/JanssenProject/jans/commit/a6414864712789d1fcf80b823338100aebda030e))
* release 1.0.0-beta.16 ([94d5791](https://github.com/JanssenProject/jans/commit/94d5791a23fce4ecb8913c16c940cfbbc85fed4c))
* release 1.0.0-beta.16 ([16de429](https://github.com/JanssenProject/jans/commit/16de4299bc5e9c4a842f279ae0d3ae8282a4ff2c))
* release 1.0.0-beta.16 ([72915c0](https://github.com/JanssenProject/jans/commit/72915c0e82b9684ac1c59934d5b9a36c2456058d))
* release 1.0.0-beta.16 ([3ea2b37](https://github.com/JanssenProject/jans/commit/3ea2b37deac3416564614fb6a4e84b056ddbed3f))
* release 1.0.0-beta.16 ([78a6d39](https://github.com/JanssenProject/jans/commit/78a6d39ffadf9abee18c7be0e14ad3eb6ec2ef1b))
* release 1.0.0-beta.16 ([11bfa93](https://github.com/JanssenProject/jans/commit/11bfa9368e6ee482cc44240de08c8133d91b3f4c))
* release 1.0.0-beta.16 ([22b180b](https://github.com/JanssenProject/jans/commit/22b180bba9a08045a6daa7ca8ee2b71abd42a973))
* release 1.0.0-beta.16 ([b9acd0b](https://github.com/JanssenProject/jans/commit/b9acd0bceeeb54e3c47f869f11d97a22e8dc161f))
* release 1.0.0-beta.16 ([328cd30](https://github.com/JanssenProject/jans/commit/328cd309ae1655a52709e13ca2f89441c6c965a2))
* release 1.0.0-beta.16 ([5a84602](https://github.com/JanssenProject/jans/commit/5a84602838fb5d2e667422220fcd44dc53543e23))
* release 1.0.0-beta.16 ([4923277](https://github.com/JanssenProject/jans/commit/4923277b100b5c814d94b27b88d1809794dfc413))
* release 1.0.0-beta.16 ([258ba96](https://github.com/JanssenProject/jans/commit/258ba962bd93eb5be4d51e7de3a80da89c2e222f))
* release 1.0.0-beta.16 ([77c4423](https://github.com/JanssenProject/jans/commit/77c4423d82b697fd91a0e61f40bad6bd9da0dba8))

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/docker-jans-auth-server-v1.0.0-beta.15...docker-jans-auth-server-v1.0.0-beta.16) (2022-03-14)


### Features

* add validity length (in days) for certs ([#981](https://github.com/JanssenProject/jans/issues/981)) ([abc89dc](https://github.com/JanssenProject/jans/commit/abc89dc6fadae5627a68a97ab4f4f5ceb56af809))


### Bug Fixes

* avoid jetty hot-deployment issue ([#1012](https://github.com/JanssenProject/jans/issues/1012)) ([a343215](https://github.com/JanssenProject/jans/commit/a34321594055305d52aa855b32d060b113313652))


### Miscellaneous Chores

* release 1.0.0-beta.16 ([a083ad6](https://github.com/JanssenProject/jans/commit/a083ad6b1d43201126e8d4f690a55ea1b109524c))
* release 1.0.0-beta.16 ([90e4bb2](https://github.com/JanssenProject/jans/commit/90e4bb29df040bd9fe5921a054bc4226d34ca1ef))
* release 1.0.0-beta.16 ([eec2073](https://github.com/JanssenProject/jans/commit/eec2073be9fd25544f31087e171934afb9a71e6d))
* release 1.0.0-beta.16 ([cd92ead](https://github.com/JanssenProject/jans/commit/cd92ead2ca654383091c4923d3de5619b70fc5b9))
* release 1.0.0-beta.16 ([7f0a91b](https://github.com/JanssenProject/jans/commit/7f0a91bd90efc1cd7a80047f9cd6b7c6a22417a2))
* release 1.0.0-beta.16 ([c2ad604](https://github.com/JanssenProject/jans/commit/c2ad604dc29e7401bc4cb0788feaa20e11de0440))
* release 1.0.0-beta.16 ([a641486](https://github.com/JanssenProject/jans/commit/a6414864712789d1fcf80b823338100aebda030e))
* release 1.0.0-beta.16 ([94d5791](https://github.com/JanssenProject/jans/commit/94d5791a23fce4ecb8913c16c940cfbbc85fed4c))
* release 1.0.0-beta.16 ([16de429](https://github.com/JanssenProject/jans/commit/16de4299bc5e9c4a842f279ae0d3ae8282a4ff2c))
* release 1.0.0-beta.16 ([72915c0](https://github.com/JanssenProject/jans/commit/72915c0e82b9684ac1c59934d5b9a36c2456058d))
* release 1.0.0-beta.16 ([3ea2b37](https://github.com/JanssenProject/jans/commit/3ea2b37deac3416564614fb6a4e84b056ddbed3f))
* release 1.0.0-beta.16 ([78a6d39](https://github.com/JanssenProject/jans/commit/78a6d39ffadf9abee18c7be0e14ad3eb6ec2ef1b))
* release 1.0.0-beta.16 ([11bfa93](https://github.com/JanssenProject/jans/commit/11bfa9368e6ee482cc44240de08c8133d91b3f4c))
* release 1.0.0-beta.16 ([22b180b](https://github.com/JanssenProject/jans/commit/22b180bba9a08045a6daa7ca8ee2b71abd42a973))
* release 1.0.0-beta.16 ([b9acd0b](https://github.com/JanssenProject/jans/commit/b9acd0bceeeb54e3c47f869f11d97a22e8dc161f))
* release 1.0.0-beta.16 ([328cd30](https://github.com/JanssenProject/jans/commit/328cd309ae1655a52709e13ca2f89441c6c965a2))
* release 1.0.0-beta.16 ([5a84602](https://github.com/JanssenProject/jans/commit/5a84602838fb5d2e667422220fcd44dc53543e23))
* release 1.0.0-beta.16 ([4923277](https://github.com/JanssenProject/jans/commit/4923277b100b5c814d94b27b88d1809794dfc413))
* release 1.0.0-beta.16 ([258ba96](https://github.com/JanssenProject/jans/commit/258ba962bd93eb5be4d51e7de3a80da89c2e222f))
* release 1.0.0-beta.16 ([77c4423](https://github.com/JanssenProject/jans/commit/77c4423d82b697fd91a0e61f40bad6bd9da0dba8))
* release 1.0.0-beta.16 ([688b324](https://github.com/JanssenProject/jans/commit/688b32407b396917695cca787c08e95fe98269a1))
* release 1.0.0-beta.16 ([4e86f15](https://github.com/JanssenProject/jans/commit/4e86f15fc39ec89d4790ebfaa7d30e7053fef606))
* release 1.0.0-beta.16 ([8d514ee](https://github.com/JanssenProject/jans/commit/8d514ee63d840627321de2d89e816577dd919914))
* release 1.0.0-beta.16 ([0899898](https://github.com/JanssenProject/jans/commit/0899898e80ba9b7e6a915574737bdf0756b59a14))

## 1.0.0-beta.15 (2022-03-02)


### Features

* add Gluu Casa support ([608a9b8](https://github.com/JanssenProject/jans/commit/608a9b857872d7ccc65931a4dd9307a064e55492))
* add Gluu Casa support ([608a9b8](https://github.com/JanssenProject/jans/commit/608a9b857872d7ccc65931a4dd9307a064e55492))
* add Gluu Casa support ([089a872](https://github.com/JanssenProject/jans/commit/089a87214a9349916b537ef6755a10ef468f6221))
* add Gluu Casa support ([089a872](https://github.com/JanssenProject/jans/commit/089a87214a9349916b537ef6755a10ef468f6221))
* **image:** push otp and super_gluu configuration to secrets ([#784](https://github.com/JanssenProject/jans/issues/784)) ([87bd7fe](https://github.com/JanssenProject/jans/commit/87bd7fe66dad6a652e965597e44a424dd6f92c62))
* update base images [#672](https://github.com/JanssenProject/jans/issues/672) ([#673](https://github.com/JanssenProject/jans/issues/673)) ([0a23d08](https://github.com/JanssenProject/jans/commit/0a23d085ea8fe16d0b4cd21cd3ec8cde59df9f9a))


### Bug Fixes

* gprcio bug in build error ([664a4fe](https://github.com/JanssenProject/jans/commit/664a4fe4f611496e937428a0517f22aed1a564f4))
* **pycloudlib:** missing tar option to not restore file timestamp [#613](https://github.com/JanssenProject/jans/issues/613) ([#627](https://github.com/JanssenProject/jans/issues/627)) ([d19fbfd](https://github.com/JanssenProject/jans/commit/d19fbfd6891d03fb0c76073dfa8ba2ffc44a3b9b))
* update scripts ([#765](https://github.com/JanssenProject/jans/issues/765)) ([8b9aaca](https://github.com/JanssenProject/jans/commit/8b9aaca64e83a17b81498ea6e99523ff7f0f311b))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))

## 1.0.0-beta.14 (2021-12-27)


### Features

* add support for plugins ([#67](https://www.github.com/JanssenProject/jans-cloud-native/issues/67)) ([7f2204c](https://www.github.com/JanssenProject/jans-cloud-native/commit/7f2204cb186902ebdc0d1f6ae1d321f3c5eeea5b))
* deprecate password files in favor of secrets ([#152](https://www.github.com/JanssenProject/jans-cloud-native/issues/152)) ([f415213](https://www.github.com/JanssenProject/jans-cloud-native/commit/f415213cfd992363f3fb85005df16e963a6ed8ff))


### Miscellaneous Chores

* change release to 1.0.0-beta.14 ([2d5d61b](https://www.github.com/JanssenProject/jans-cloud-native/commit/2d5d61bc5971da0a087323e544f12206154af43f))
* release 1.0.0-beta.13 ([789a9ed](https://www.github.com/JanssenProject/jans-cloud-native/commit/789a9edbe2d78e7424dc6ce4f153f719a5f09e35))

## 1.0.0-beta.13 (2021-12-03)


### Features

* add support for plugins ([#67](https://www.github.com/JanssenProject/jans-cloud-native/issues/67)) ([7f2204c](https://www.github.com/JanssenProject/jans-cloud-native/commit/7f2204cb186902ebdc0d1f6ae1d321f3c5eeea5b))


### Miscellaneous Chores

* release 1.0.0-beta.13 ([789a9ed](https://www.github.com/JanssenProject/jans-cloud-native/commit/789a9edbe2d78e7424dc6ce4f153f719a5f09e35))
