# Changelog

## [1.0.21](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.20...jans-fido2-v1.0.21) (2023-12-14)


### Bug Fixes

* API spec metadata ([#6473](https://github.com/JanssenProject/jans/issues/6473)) ([3922ddb](https://github.com/JanssenProject/jans/commit/3922ddb509db422d9a0f2c88df9f0d2e3fd05f46))
* **fido2:** tests are failing due to NoClassDefFoundError: javax/xml/bind/annotation/XmlElement [#6865](https://github.com/JanssenProject/jans/issues/6865) ([#6866](https://github.com/JanssenProject/jans/issues/6866)) ([571b1d6](https://github.com/JanssenProject/jans/commit/571b1d670656487fc8ce30ad5658cf147fc4a919))
* **jans-fido2:** put timeout when authenticator attachment is cross-platform ([#6415](https://github.com/JanssenProject/jans/issues/6415)) ([4fbdb44](https://github.com/JanssenProject/jans/commit/4fbdb446c6c3bb1e25a343c63af47a8f7a46723f))
* openapi spec version element ([#6780](https://github.com/JanssenProject/jans/issues/6780)) ([e4aca8c](https://github.com/JanssenProject/jans/commit/e4aca8ce1b39cd89764b3c852418a8ed879b3925))
* prepare for 1.0.21 release ([#7008](https://github.com/JanssenProject/jans/issues/7008)) ([2132de6](https://github.com/JanssenProject/jans/commit/2132de6683f67bf22d5a863b149770d657073a83))

## [1.0.20](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.19...jans-fido2-v1.0.20) (2023-11-08)


### Bug Fixes

* prepare for 1.0.20 release ([c6e806e](https://github.com/JanssenProject/jans/commit/c6e806eb31fed998d52cbef7a7d94c231d913102))

## [1.0.19](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.18...jans-fido2-v1.0.19) (2023-10-11)


### Bug Fixes

* prepare for 1.0.19 release ([554fd43](https://github.com/JanssenProject/jans/commit/554fd434f624c4b4be3b2031c472177709da8966))

## [1.0.18](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.17...jans-fido2-v1.0.18) (2023-09-23)


### Bug Fixes

* prepare for 1.0.18 release ([87af7e4](https://github.com/JanssenProject/jans/commit/87af7e4d41728ce2966362883b47e5354f8c3803))

## [1.0.17](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.16...jans-fido2-v1.0.17) (2023-09-17)


### Features

* BCFIPS support (sub-part 01) ([#5767](https://github.com/JanssenProject/jans/issues/5767)) ([d8cea00](https://github.com/JanssenProject/jans/commit/d8cea008a73ccecb1b734a010b9e9bdd363c8432))
* fido2 needs to search cache for session instead of persistent ([#6011](https://github.com/JanssenProject/jans/issues/6011)) ([0cc0c19](https://github.com/JanssenProject/jans/commit/0cc0c192735c0537c28bb7cc96a9db509d9628e0))
* **jans-fido2:** mds optional ([#5409](https://github.com/JanssenProject/jans/issues/5409)) ([fad9961](https://github.com/JanssenProject/jans/commit/fad9961fbeeffb315d6ca495c43f8a4f000eac86))


### Bug Fixes

* **fido2:** Exception handling for assertion ([#5689](https://github.com/JanssenProject/jans/issues/5689)) ([2c82c5d](https://github.com/JanssenProject/jans/commit/2c82c5d73594464ed8ecec199f57774737cfd4e3))
* prepare for 1.0.17 release ([4ba8c15](https://github.com/JanssenProject/jans/commit/4ba8c151734f02d762e902b46a35cae2d498fa8f))
* version reference ([432a904](https://github.com/JanssenProject/jans/commit/432a9048fd104e6d8ddeb50684bf5df23f0722cf))

## [1.0.16](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.15...jans-fido2-v1.0.16) (2023-08-02)


### Features

* add new methnod to fido2 extension to allow modify json ([#5683](https://github.com/JanssenProject/jans/issues/5683)) ([256675b](https://github.com/JanssenProject/jans/commit/256675b2ad9e195ea793eee00257ed400f815a56)), closes [#5680](https://github.com/JanssenProject/jans/issues/5680)
* add new methnod to fido2 extension to allow modify json ([#5686](https://github.com/JanssenProject/jans/issues/5686)) ([6f56e51](https://github.com/JanssenProject/jans/commit/6f56e51706c0e44cd3a9baffa8d2758898b994ba)), closes [#5680](https://github.com/JanssenProject/jans/issues/5680)


### Bug Fixes

* prepare for 1.0.16 release ([042ce79](https://github.com/JanssenProject/jans/commit/042ce7941b9597fade8d5f10e40a89d9e7662315))
* prepare for 1.0.16 release ([b2649c3](https://github.com/JanssenProject/jans/commit/b2649c33a9857f356f91df2f38787ec56269e6dd))
* use filter by RP if it's SG request ([#5575](https://github.com/JanssenProject/jans/issues/5575)) ([2739f96](https://github.com/JanssenProject/jans/commit/2739f969f2b628b9409ad77ce438227c7685475b))

## [1.0.15](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.14...jans-fido2-v1.0.15) (2023-07-12)


### Features

* **fido2:** loading mds using external urls ([#5162](https://github.com/JanssenProject/jans/issues/5162)) ([d3d2294](https://github.com/JanssenProject/jans/commit/d3d2294ffabe3de9baa067b39f9578947b6d475f))


### Bug Fixes

* **jans-fido2:** buildfix - removed stale notify-client reference after move to jans-core [#5257](https://github.com/JanssenProject/jans/issues/5257) ([#5258](https://github.com/JanssenProject/jans/issues/5258)) ([90c6360](https://github.com/JanssenProject/jans/commit/90c6360cb9ec4b44f312feda48960efdbde8b1a5))
* prepare for 1.0.15 release ([0e3cc2f](https://github.com/JanssenProject/jans/commit/0e3cc2f5ea287c2c35f45def54f074daa473ec49))

## [1.0.14](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.13...jans-fido2-v1.0.14) (2023-06-12)


### Features

* **fido2:** Apple_WebAuthn_Root_CA certificate is now downloaded to /authentication_cert folder and read from AttestationCertificateService ([#4756](https://github.com/JanssenProject/jans/issues/4756)) ([1600185](https://github.com/JanssenProject/jans/commit/16001859895d5aa5d840cfc9d425d0ee95149979))
* **fido2:** unit test for the io.jans.fido2.service.sg package ([#5006](https://github.com/JanssenProject/jans/issues/5006)) ([70a5b38](https://github.com/JanssenProject/jans/commit/70a5b3815c09d9ef6d39d2cf7a6e216ca29e17a4))
* **fido2:** validate acceptable status when loading MDS ([#5192](https://github.com/JanssenProject/jans/issues/5192)) ([dd27295](https://github.com/JanssenProject/jans/commit/dd27295ec11adba9c49a250c29d091fa53db2851))
* **jans-fido2:** unit test for CertificateVerifier, CommonVerifiers and userVerificationVerifier ([#4782](https://github.com/JanssenProject/jans/issues/4782)) ([676c6ac](https://github.com/JanssenProject/jans/commit/676c6ac3e5737a92858ad78be32f4d5e6050a4be))
* move notify-client2 library to fido2 project [#5030](https://github.com/JanssenProject/jans/issues/5030) ([#5031](https://github.com/JanssenProject/jans/issues/5031)) ([ed5e09e](https://github.com/JanssenProject/jans/commit/ed5e09eff23dbea45e026728886d1e95f3e5cd95))
* store AttestationType ([#4813](https://github.com/JanssenProject/jans/issues/4813)) ([819f78a](https://github.com/JanssenProject/jans/commit/819f78ac4ebb9030097166e0fdc0926896c11df5))
* update SG script and notify client to conform scan API [#5061](https://github.com/JanssenProject/jans/issues/5061) ([#5062](https://github.com/JanssenProject/jans/issues/5062)) ([7afc42b](https://github.com/JanssenProject/jans/commit/7afc42b2ec00d35cb980d35f286289de2bdadff2))


### Bug Fixes

* FullFlowAppleTest and FullFlowAndroidTest with error [#5032](https://github.com/JanssenProject/jans/issues/5032) ([#5150](https://github.com/JanssenProject/jans/issues/5150)) ([399dc55](https://github.com/JanssenProject/jans/commit/399dc555f409cd11e7d7f925a279a26795e6bbf6))
* **jans-fido2:** compilation is failing due to missed jans-doc dependency [#5051](https://github.com/JanssenProject/jans/issues/5051) ([#5052](https://github.com/JanssenProject/jans/issues/5052)) ([ad2332e](https://github.com/JanssenProject/jans/commit/ad2332ee0b3b94232c8c79321fbc3e99d43a88d0))
* **jans-fido2:** created TPMAssertionFormatProcessor class ([#4827](https://github.com/JanssenProject/jans/issues/4827)) ([f019dcf](https://github.com/JanssenProject/jans/commit/f019dcfb5b9de43877f21cffd9ab43d7a1d3df9d))
* prepare for 1.0.14 release ([25ccadf](https://github.com/JanssenProject/jans/commit/25ccadf85327ea14685c6066dc6609919e4f2865))

## [1.0.13](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.12...jans-fido2-v1.0.13) (2023-05-10)


### Features

* **jans-fido2:** enhancements to getHashedBuffer and tpm process method ([#4603](https://github.com/JanssenProject/jans/issues/4603)) ([a367d6e](https://github.com/JanssenProject/jans/commit/a367d6efce450ad15b812497235b570b307e9509))
* **jans-fido2:** interception scripts issue 1485, swagger updates ([#4543](https://github.com/JanssenProject/jans/issues/4543)) ([80274ff](https://github.com/JanssenProject/jans/commit/80274ffd1a20318988d9cc99ee015c5c7d5984b7))


### Bug Fixes

* prepare for 1.0.13 release ([493478e](https://github.com/JanssenProject/jans/commit/493478e71f6231553c998b48c0f163c7f5869da4))

## [1.0.12](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.11...jans-fido2-v1.0.12) (2023-04-18)


### Bug Fixes

* prepare for 1.0.12 release ([6f83197](https://github.com/JanssenProject/jans/commit/6f83197705511c39413456acdc64e9136a97ff39))

## [1.0.11](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.10...jans-fido2-v1.0.11) (2023-04-05)


### Features

* loggerService should update root log level [#4251](https://github.com/JanssenProject/jans/issues/4251) ([#4252](https://github.com/JanssenProject/jans/issues/4252)) ([20264a2](https://github.com/JanssenProject/jans/commit/20264a2f61e7b49015bbf6f7b93e9d241e3176a1))
* restore status/errorMessage and timeout attributes ([#4240](https://github.com/JanssenProject/jans/issues/4240)) ([1d49d68](https://github.com/JanssenProject/jans/commit/1d49d683c2b2c9da5961ef2aa312c47b59864a0f))
* rests for cancel ([#4212](https://github.com/JanssenProject/jans/issues/4212)) ([12c4dff](https://github.com/JanssenProject/jans/commit/12c4dfffcbdd3ccb6814885d4190159d5628fde3))
* update fido2-server to conform latest FIDO2 conformance ([#4346](https://github.com/JanssenProject/jans/issues/4346)) ([29147cb](https://github.com/JanssenProject/jans/commit/29147cb0c3f5dd8dbda039f8613e58fa46f23579)), closes [#4239](https://github.com/JanssenProject/jans/issues/4239)


### Bug Fixes

* add documentDomain to assertion response ([#4392](https://github.com/JanssenProject/jans/issues/4392)) ([b567726](https://github.com/JanssenProject/jans/commit/b56772605044e8eb7531ff9ec742db3b9855879a))
* authenticatorAttachment should be not mandatory ([#4232](https://github.com/JanssenProject/jans/issues/4232)) ([aec8482](https://github.com/JanssenProject/jans/commit/aec848215d9869eb684e2af5d8eed7ea022c748c))
* prepare for  release ([60775c0](https://github.com/JanssenProject/jans/commit/60775c09dc5ab9996bf80c03dcb457861d48dfb1))
* Unable to send emails issue 4121 ([#4333](https://github.com/JanssenProject/jans/issues/4333)) ([70a566b](https://github.com/JanssenProject/jans/commit/70a566b67f660750bf742f19ee127f79b2db8930))
* user real URL  in documentDomain ([#4394](https://github.com/JanssenProject/jans/issues/4394)) ([6f66cb8](https://github.com/JanssenProject/jans/commit/6f66cb860003ba6fccf942fb8f4969ac6dd54295))

## [1.0.10](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.9...jans-fido2-v1.0.10) (2023-03-16)


### Bug Fixes

* **jans-fido2:** handling exception fido2 get endpoints by invalid params ([#4139](https://github.com/JanssenProject/jans/issues/4139)) ([a50d2af](https://github.com/JanssenProject/jans/commit/a50d2af5eea4f88632870546eb9e4505cd5c7e2b))
* prepare release for 1.0.10 ([e996926](https://github.com/JanssenProject/jans/commit/e99692692ef04d881468d120f7c7d462568dce36))

## [1.0.9](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.8...jans-fido2-v1.0.9) (2023-03-09)


### Bug Fixes

* prepare 1.0.9 release ([e6ea522](https://github.com/JanssenProject/jans/commit/e6ea52220824bd6b5d2dca0539d9d515dbeda362))
* prepare 1.0.9 release ([55f7e0c](https://github.com/JanssenProject/jans/commit/55f7e0c308b869c2c4b5668aca751d022138a678))
* update next SNAPSHOT and dev ([0df0e7a](https://github.com/JanssenProject/jans/commit/0df0e7ae06af64ac477955119c2522f03e0603c3))

## [1.0.8](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.7...jans-fido2-v1.0.8) (2023-03-01)


### Bug Fixes

* fix user publicKey search ([#3982](https://github.com/JanssenProject/jans/issues/3982)) ([d0584dd](https://github.com/JanssenProject/jans/commit/d0584dd3b67039c3bff76649547401e50012cce5))

## 1.0.7 (2023-02-22)


### Features

* add authentication SG flow tests ([#3877](https://github.com/JanssenProject/jans/issues/3877)) ([d5c3fac](https://github.com/JanssenProject/jans/commit/d5c3fac2ee16d0d8276da880b78cc5e676a2f302))
* add debug SG logging ([#3730](https://github.com/JanssenProject/jans/issues/3730)) ([a0c9ca2](https://github.com/JanssenProject/jans/commit/a0c9ca28e3558a8c85502d134a81d9f5bdd78b12))
* add Jupiter+Weld+Mockito+Extension to Fido2 ([#3875](https://github.com/JanssenProject/jans/issues/3875)) ([0152435](https://github.com/JanssenProject/jans/commit/01524358cbd720ad547c6b0d622c2cc32e76a125))
* add more loggers ([#3742](https://github.com/JanssenProject/jans/issues/3742)) ([919bc86](https://github.com/JanssenProject/jans/commit/919bc869fd3f2e0be143c5bfddc7ba3629178e86))
* optmize cleander job ([#3737](https://github.com/JanssenProject/jans/issues/3737)) ([2a864d9](https://github.com/JanssenProject/jans/commit/2a864d98b0d3e1983aead62ef9f95e6191248de5))
* support cancel request ([#3733](https://github.com/JanssenProject/jans/issues/3733)) ([2741e51](https://github.com/JanssenProject/jans/commit/2741e511bc7244a764a43d4dd8d4bb13da87aabb))
* Support Super Gluu one step authentication to Fido2 server [#3593](https://github.com/JanssenProject/jans/issues/3593) ([#3599](https://github.com/JanssenProject/jans/issues/3599)) ([c013b16](https://github.com/JanssenProject/jans/commit/c013b161f2eb47f5952cbb80c8740f8d62d302c3))


### Bug Fixes

* cbor data stream lenght calculatro return wrong lengh [#3614](https://github.com/JanssenProject/jans/issues/3614) ([#3615](https://github.com/JanssenProject/jans/issues/3615)) ([22065ea](https://github.com/JanssenProject/jans/commit/22065ea14fcaa523531a738f53d6659a790155c5))
* **fido2-client:** conflict of log4j config ([#3636](https://github.com/JanssenProject/jans/issues/3636)) ([77412d5](https://github.com/JanssenProject/jans/commit/77412d5ca1be8dd99797489db22a4e22c7a5cc13))
* fix authenticatorData encoding ([#3815](https://github.com/JanssenProject/jans/issues/3815)) ([687cb2a](https://github.com/JanssenProject/jans/commit/687cb2a3b12980f360a113c88e7cc295c1a4a752))
* fix fmt name ([#3900](https://github.com/JanssenProject/jans/issues/3900)) ([4a6a0c1](https://github.com/JanssenProject/jans/commit/4a6a0c19126220ae783a343d4fe1a54c99ed4475))
* fixes for cancel support ([#3735](https://github.com/JanssenProject/jans/issues/3735)) ([3e64530](https://github.com/JanssenProject/jans/commit/3e64530f3196e4f396c7e8a95d6fd722efa9a816))
* **jans-auth-server:** jansApp attribute only relevant for SG ([#3782](https://github.com/JanssenProject/jans/issues/3782)) ([6153a13](https://github.com/JanssenProject/jans/commit/6153a139d584e69088f8d9202ce072ae10a2dc73))
* **jans-fido2:** RegistrationPersistenceService implemntation ([#3728](https://github.com/JanssenProject/jans/issues/3728)) ([d5b8b67](https://github.com/JanssenProject/jans/commit/d5b8b67e10bd6d9bc93d831bef8198f406873b0e))
* prepare 1.0.7 release ([ce02fd9](https://github.com/JanssenProject/jans/commit/ce02fd9322ab49d5bea4f6e88f316f931e9d2169))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))

## 1.0.6 (2023-01-09)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))


### Bug Fixes

* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* **jans-fido2:** [#1120](https://github.com/JanssenProject/jans/issues/1120) ([#2928](https://github.com/JanssenProject/jans/issues/2928)) ([0fea95a](https://github.com/JanssenProject/jans/commit/0fea95a181811de2f592debcec5af76f9adda5b2))
* **jans-fido2:** [#2971](https://github.com/JanssenProject/jans/issues/2971) ([#2972](https://github.com/JanssenProject/jans/issues/2972)) ([2f15cf8](https://github.com/JanssenProject/jans/commit/2f15cf8aba64410fe1dd0ef71e9860ae0ec919bd))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))


### Documentation

* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## [1.0.5](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.4...jans-fido2-v1.0.5) (2022-12-01)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))


### Bug Fixes

* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* **jans-fido2:** [#1120](https://github.com/JanssenProject/jans/issues/1120) ([#2928](https://github.com/JanssenProject/jans/issues/2928)) ([0fea95a](https://github.com/JanssenProject/jans/commit/0fea95a181811de2f592debcec5af76f9adda5b2))
* **jans-fido2:** [#2971](https://github.com/JanssenProject/jans/issues/2971) ([#2972](https://github.com/JanssenProject/jans/issues/2972)) ([2f15cf8](https://github.com/JanssenProject/jans/commit/2f15cf8aba64410fe1dd0ef71e9860ae0ec919bd))

## [1.0.4](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.3...jans-fido2-v1.0.4) (2022-11-08)


### Documentation

* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## 1.0.3 (2022-11-01)


### Bug Fixes

* [#776](https://github.com/JanssenProject/jans/issues/776) ([#2503](https://github.com/JanssenProject/jans/issues/2503)) ([a564431](https://github.com/JanssenProject/jans/commit/a564431c8b6e503a36dbaf7ccc8f79e6b8adb95f))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))


### Miscellaneous Chores

* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## 1.0.2 (2022-08-30)


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.0...jans-fido2-v1.0.1) (2022-07-06)


### Miscellaneous Chores

* release 1.0.0 ([3df6f77](https://github.com/JanssenProject/jans/commit/3df6f7721a8e9d57e28d065ee29153d023dfe9ea))
* release 1.0.0 ([9644d1b](https://github.com/JanssenProject/jans/commit/9644d1bd29c291e57c140b0c9ac67243c322ac35))
* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

## 1.0.0 (2022-05-19)


### Features

* **jans:** jetty 11 integration ([#1123](https://github.com/JanssenProject/jans/issues/1123)) ([6c1caa1](https://github.com/JanssenProject/jans/commit/6c1caa1c4c92d28571f8589cd701e6885d4d85ef))


### Bug Fixes

* adjust beans and schema [#1107](https://github.com/JanssenProject/jans/issues/1107) ([#1248](https://github.com/JanssenProject/jans/issues/1248)) ([369129d](https://github.com/JanssenProject/jans/commit/369129d0c2614afb536d0e1329ac106fd7da187d))


### Miscellaneous Chores

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
* release 1.0.0-beta.16 ([688b324](https://github.com/JanssenProject/jans/commit/688b32407b396917695cca787c08e95fe98269a1))
* release 1.0.0-beta.16 ([4e86f15](https://github.com/JanssenProject/jans/commit/4e86f15fc39ec89d4790ebfaa7d30e7053fef606))
* release 1.0.0-beta.16 ([8d514ee](https://github.com/JanssenProject/jans/commit/8d514ee63d840627321de2d89e816577dd919914))
* release 1.0.0-beta.16 ([0899898](https://github.com/JanssenProject/jans/commit/0899898e80ba9b7e6a915574737bdf0756b59a14))

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/jans-fido2-v1.0.0-beta.15...jans-fido2-v1.0.0-beta.16) (2022-03-14)


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


### Bug Fixes

* **jans-fido2:** use diamond operator ([#764](https://github.com/JanssenProject/jans/issues/764)) ([5950a26](https://github.com/JanssenProject/jans/commit/5950a26f5c5dcf1731224d4ec56f7a5191a13d3d))
* use diamond operator ([#766](https://github.com/JanssenProject/jans/issues/766)) ([57664b0](https://github.com/JanssenProject/jans/commit/57664b0c0fd5926b2986f6b6d738d909e4865bca))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))
