# Changelog

## [1.0.21](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.20...jans-core-v1.0.21) (2023-12-14)


### Features

* add Jans lock ([#7074](https://github.com/JanssenProject/jans/issues/7074)) ([ff3e904](https://github.com/JanssenProject/jans/commit/ff3e9044aa29ca32219b40eccab5c27e47233e15))
* add jans-lock-event library to publish messages to event server from jans-auth ([#6893](https://github.com/JanssenProject/jans/issues/6893)) ([c49f8f1](https://github.com/JanssenProject/jans/commit/c49f8f16af502429bf7a58fc31f6d5ffc1f67f78))
* add message configuration api to config-api [#6982](https://github.com/JanssenProject/jans/issues/6982) ([#6983](https://github.com/JanssenProject/jans/issues/6983)) ([945ba76](https://github.com/JanssenProject/jans/commit/945ba767da90d2c6c376b5b6cca6313c0851bbca))
* **idp-plugin:** inbound SAML with Keycloak as SP and external SAML IDP ([#6793](https://github.com/JanssenProject/jans/issues/6793)) ([bc5eaad](https://github.com/JanssenProject/jans/commit/bc5eaade348d74d93da25c7494975b9aa35cded4))


### Bug Fixes

* feature flag default values ([#6857](https://github.com/JanssenProject/jans/issues/6857)) ([75b49be](https://github.com/JanssenProject/jans/commit/75b49be719d64c81a11805ee1c8d9562027c22e8))
* **jans-core:** fixing jans-core compilation dependencies upgrade [#6783](https://github.com/JanssenProject/jans/issues/6783) ([#6785](https://github.com/JanssenProject/jans/issues/6785)) ([7a0a1da](https://github.com/JanssenProject/jans/commit/7a0a1da38ad08a81594ff1ec1f30636c2e31ef38))
* **jans-core:** jans-core starts to fail after dependencies upgrade [#6783](https://github.com/JanssenProject/jans/issues/6783) ([#6784](https://github.com/JanssenProject/jans/issues/6784)) ([b67f1d3](https://github.com/JanssenProject/jans/commit/b67f1d301a101cf9c29e5404f15aa6acfa1d5877))
* prepare for 1.0.21 release ([#7008](https://github.com/JanssenProject/jans/issues/7008)) ([2132de6](https://github.com/JanssenProject/jans/commit/2132de6683f67bf22d5a863b149770d657073a83))

## [1.0.20](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.19...jans-core-v1.0.20) (2023-11-08)


### Bug Fixes

* prepare for 1.0.20 release ([c6e806e](https://github.com/JanssenProject/jans/commit/c6e806eb31fed998d52cbef7a7d94c231d913102))

## [1.0.19](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.18...jans-core-v1.0.19) (2023-10-11)


### Features

* **config-api, keycloak:** saml plugin to create trust client in DB and keycloak storage provider to jans store ([#6155](https://github.com/JanssenProject/jans/issues/6155)) ([c4f5034](https://github.com/JanssenProject/jans/commit/c4f50343ef5f991cc3c0184cef0fa83ff1d7f03c))


### Bug Fixes

* prepare for 1.0.19 release ([554fd43](https://github.com/JanssenProject/jans/commit/554fd434f624c4b4be3b2031c472177709da8966))

## [1.0.18](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.17...jans-core-v1.0.18) (2023-09-23)


### Bug Fixes

* prepare for 1.0.18 release ([87af7e4](https://github.com/JanssenProject/jans/commit/87af7e4d41728ce2966362883b47e5354f8c3803))

## [1.0.17](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.16...jans-core-v1.0.17) (2023-09-17)


### Features

* BCFIPS support (sub-part 03) ([#5852](https://github.com/JanssenProject/jans/issues/5852)) ([8b0d12b](https://github.com/JanssenProject/jans/commit/8b0d12b96f7ea9f82f322c536e0deec03f63edbd))
* fido2 needs to search cache for session instead of persistent ([#6011](https://github.com/JanssenProject/jans/issues/6011)) ([0cc0c19](https://github.com/JanssenProject/jans/commit/0cc0c192735c0537c28bb7cc96a9db509d9628e0))
* **jans-auth-server:** added "The Use of Attestation in OAuth 2.0 Dynamic Client Registration" spec support [#5562](https://github.com/JanssenProject/jans/issues/5562) ([#5868](https://github.com/JanssenProject/jans/issues/5868)) ([38653c9](https://github.com/JanssenProject/jans/commit/38653c9cb9eb992213c5f230a5f36ce1187d0197))
* **jans-auth-server:** added JIT compiler support under jvm 17 ([#5954](https://github.com/JanssenProject/jans/issues/5954)) ([3760380](https://github.com/JanssenProject/jans/commit/37603801aa7856db9f657119b2cf890d169f3df2))
* **jans-auth-server:** OAuth 2.0 for First-Party Native Applications ([#5654](https://github.com/JanssenProject/jans/issues/5654)) ([9d90e28](https://github.com/JanssenProject/jans/commit/9d90e28791c49bc86771623601c654f2c662b7a1))


### Bug Fixes

* prepare for 1.0.17 release ([4ba8c15](https://github.com/JanssenProject/jans/commit/4ba8c151734f02d762e902b46a35cae2d498fa8f))

## [1.0.16](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.15...jans-core-v1.0.16) (2023-08-02)


### Features

* add new methnod to fido2 extension to allow modify json ([#5683](https://github.com/JanssenProject/jans/issues/5683)) ([256675b](https://github.com/JanssenProject/jans/commit/256675b2ad9e195ea793eee00257ed400f815a56)), closes [#5680](https://github.com/JanssenProject/jans/issues/5680)


### Bug Fixes

* authentication Filter should not process OPTIONS request ([#5525](https://github.com/JanssenProject/jans/issues/5525)) ([aed5e4f](https://github.com/JanssenProject/jans/commit/aed5e4f52cc0ac6d0f278a6813e698068cd4ec9e)), closes [#5524](https://github.com/JanssenProject/jans/issues/5524)
* **jans-core:** db document Store fixes [#5619](https://github.com/JanssenProject/jans/issues/5619) ([#5620](https://github.com/JanssenProject/jans/issues/5620)) ([1bdbbd5](https://github.com/JanssenProject/jans/commit/1bdbbd5ab9910d6697c6bcdc6e230cebb9c369a9))
* prepare for 1.0.16 release ([042ce79](https://github.com/JanssenProject/jans/commit/042ce7941b9597fade8d5f10e40a89d9e7662315))
* prepare for 1.0.16 release ([b2649c3](https://github.com/JanssenProject/jans/commit/b2649c33a9857f356f91df2f38787ec56269e6dd))

## [1.0.15](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.14...jans-core-v1.0.15) (2023-07-12)


### Bug Fixes

* **jans-auth-server:** initializing of jsf navigation has been updated; ([#5253](https://github.com/JanssenProject/jans/issues/5253)) ([bed5d6f](https://github.com/JanssenProject/jans/commit/bed5d6fb7f9718c40a347108f8433c6552cacae9))
* prepare for 1.0.15 release ([0e3cc2f](https://github.com/JanssenProject/jans/commit/0e3cc2f5ea287c2c35f45def54f074daa473ec49))

## [1.0.14](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.13...jans-core-v1.0.14) (2023-06-12)


### Features

* **jans-auth-server:** Support of Select Account interception script [#3452](https://github.com/JanssenProject/jans/issues/3452) ([#5149](https://github.com/JanssenProject/jans/issues/5149)) ([b062148](https://github.com/JanssenProject/jans/commit/b062148b7395e2828432061363058d7e1a9dd6db))
* **jans-link-interception:** add cache refresh to Jans ([#5007](https://github.com/JanssenProject/jans/issues/5007)) ([878dd5e](https://github.com/JanssenProject/jans/commit/878dd5e414345aac8ba49490cd1aa8604e834533))


### Bug Fixes

* **config-api:** resolved dependency conflict for CustomScriptService ([3121833](https://github.com/JanssenProject/jans/commit/312183373f72f7916b3c4c6cc5176420bba3bae0))
* **config-api:** revert hide smtp and client model utility method ([#4976](https://github.com/JanssenProject/jans/issues/4976)) ([6519744](https://github.com/JanssenProject/jans/commit/651974408565441951b6a4ca80a4ab555c01352f))
* failed to initialize resteasy proxy from script at server ([#5118](https://github.com/JanssenProject/jans/issues/5118)) ([51f9ad9](https://github.com/JanssenProject/jans/commit/51f9ad9bfa02c285b99deab81e593817b6c61137)), closes [#5116](https://github.com/JanssenProject/jans/issues/5116)
* prepare for 1.0.14 release ([25ccadf](https://github.com/JanssenProject/jans/commit/25ccadf85327ea14685c6066dc6609919e4f2865))

## [1.0.13](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.12...jans-core-v1.0.13) (2023-05-10)


### Features

* **jans-fido2:** interception scripts issue 1485, swagger updates ([#4543](https://github.com/JanssenProject/jans/issues/4543)) ([80274ff](https://github.com/JanssenProject/jans/commit/80274ffd1a20318988d9cc99ee015c5c7d5984b7))


### Bug Fixes

* cors filter should not store in local variable allowed ([#4688](https://github.com/JanssenProject/jans/issues/4688)) ([0d99195](https://github.com/JanssenProject/jans/commit/0d99195972dfe2963d3d0b785cd25b7337b55296)), closes [#4687](https://github.com/JanssenProject/jans/issues/4687)
* prepare for 1.0.13 release ([493478e](https://github.com/JanssenProject/jans/commit/493478e71f6231553c998b48c0f163c7f5869da4))

## [1.0.12](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.11...jans-core-v1.0.12) (2023-04-18)


### Features

* **config-api:** search pattern, client auth response and security fix ([#4595](https://github.com/JanssenProject/jans/issues/4595)) ([4dbfcc2](https://github.com/JanssenProject/jans/commit/4dbfcc241353c4e03672d4103d10768cbc0c5bdd))


### Bug Fixes

* jsonvalue has been added; ([#4604](https://github.com/JanssenProject/jans/issues/4604)) ([f3b46f4](https://github.com/JanssenProject/jans/commit/f3b46f4a2b8da47063efe8e3e90d98db58eeec81))
* mailservice should send non signed emails, if keystore isn't defined (update); ([#4544](https://github.com/JanssenProject/jans/issues/4544)) ([57f4b75](https://github.com/JanssenProject/jans/commit/57f4b75ce68f0f12a775d397f97cf56df5299900))
* mismatch in notation used for SmtpConfiguration [#4581](https://github.com/JanssenProject/jans/issues/4581) ([#4600](https://github.com/JanssenProject/jans/issues/4600)) ([3a62f05](https://github.com/JanssenProject/jans/commit/3a62f0588b996df97f9f21f26edd2d980b4fadec))
* prepare for 1.0.12 release ([6f83197](https://github.com/JanssenProject/jans/commit/6f83197705511c39413456acdc64e9136a97ff39))
* yaml has been updated; ([f3b46f4](https://github.com/JanssenProject/jans/commit/f3b46f4a2b8da47063efe8e3e90d98db58eeec81))

## [1.0.11](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.10...jans-core-v1.0.11) (2023-04-05)


### Features

* loggerService should update root log level [#4251](https://github.com/JanssenProject/jans/issues/4251) ([#4252](https://github.com/JanssenProject/jans/issues/4252)) ([20264a2](https://github.com/JanssenProject/jans/commit/20264a2f61e7b49015bbf6f7b93e9d241e3176a1))
* userName -&gt; smtpAuthenticationAccountUsername; ([#4401](https://github.com/JanssenProject/jans/issues/4401)) ([2bbb95d](https://github.com/JanssenProject/jans/commit/2bbb95dc4558a3251d52f74ff88b41f1aafe8a5e))


### Bug Fixes

* mailservice should send non signed emails, if keystore isn't defied; ([#4455](https://github.com/JanssenProject/jans/issues/4455)) ([7b41c44](https://github.com/JanssenProject/jans/commit/7b41c44f2933b8fde79d0478cf8df69303b9b3ba))
* prepare for  release ([60775c0](https://github.com/JanssenProject/jans/commit/60775c09dc5ab9996bf80c03dcb457861d48dfb1))
* Unable to send emails issue 4121 ([#4333](https://github.com/JanssenProject/jans/issues/4333)) ([70a566b](https://github.com/JanssenProject/jans/commit/70a566b67f660750bf742f19ee127f79b2db8930))

## [1.0.10](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.9...jans-core-v1.0.10) (2023-03-16)


### Bug Fixes

* prepare release for 1.0.10 ([e996926](https://github.com/JanssenProject/jans/commit/e99692692ef04d881468d120f7c7d462568dce36))

## [1.0.9](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.7...jans-core-v1.0.9) (2023-03-09)


### Bug Fixes

* prepare 1.0.9 release ([e6ea522](https://github.com/JanssenProject/jans/commit/e6ea52220824bd6b5d2dca0539d9d515dbeda362))
* prepare 1.0.9 release ([55f7e0c](https://github.com/JanssenProject/jans/commit/55f7e0c308b869c2c4b5668aca751d022138a678))
* update next SNAPSHOT and dev ([0df0e7a](https://github.com/JanssenProject/jans/commit/0df0e7ae06af64ac477955119c2522f03e0603c3))

## 1.0.7 (2023-02-22)


### Features

* add Jupiter+Weld+Mockito+Extension to Fido2 ([#3875](https://github.com/JanssenProject/jans/issues/3875)) ([0152435](https://github.com/JanssenProject/jans/commit/01524358cbd720ad547c6b0d622c2cc32e76a125))
* **config-api:** health check response rectification and Agama ADS swagger spec ([#3293](https://github.com/JanssenProject/jans/issues/3293)) ([faf2888](https://github.com/JanssenProject/jans/commit/faf2888f3d58d14fc6361d5a9ff5f743984cea4f))
* **jans-core:** add AES utility class [#3215](https://github.com/JanssenProject/jans/issues/3215) ([#3242](https://github.com/JanssenProject/jans/issues/3242)) ([7e59795](https://github.com/JanssenProject/jans/commit/7e59795e21bc63b173802346b614e7ae6112de4e))


### Bug Fixes

* **config-api:** user service conflict with fido2 and script enhancement ([#3767](https://github.com/JanssenProject/jans/issues/3767)) ([5753d39](https://github.com/JanssenProject/jans/commit/5753d3989b96d76699f234cc87f58e355ba313b0))
* login page doesn't display the correct localized characters ([#3528](https://github.com/JanssenProject/jans/issues/3528)) ([395b376](https://github.com/JanssenProject/jans/commit/395b3769750d2d32f624060c0b6e6ceeee7df0be)), closes [#1660](https://github.com/JanssenProject/jans/issues/1660)
* prepare 1.0.7 release ([ce02fd9](https://github.com/JanssenProject/jans/commit/ce02fd9322ab49d5bea4f6e88f316f931e9d2169))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))
* store correct script revision after script reload from file ([#3704](https://github.com/JanssenProject/jans/issues/3704)) ([2ca6a83](https://github.com/JanssenProject/jans/commit/2ca6a833132b129c29924106ed65db430917cb8c)), closes [#3703](https://github.com/JanssenProject/jans/issues/3703)

## 1.0.6 (2023-01-09)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))
* **config-api:** health check response rectification and Agama ADS swagger spec ([#3293](https://github.com/JanssenProject/jans/issues/3293)) ([faf2888](https://github.com/JanssenProject/jans/commit/faf2888f3d58d14fc6361d5a9ff5f743984cea4f))
* for file based scripts check both script revision and file ([#2878](https://github.com/JanssenProject/jans/issues/2878)) ([97ab071](https://github.com/JanssenProject/jans/commit/97ab0712e39605520b3ac8fb8df1c00bf0437797))
* **jans-core:** add AES utility class [#3215](https://github.com/JanssenProject/jans/issues/3215) ([#3242](https://github.com/JanssenProject/jans/issues/3242)) ([7e59795](https://github.com/JanssenProject/jans/commit/7e59795e21bc63b173802346b614e7ae6112de4e))
* ssa revoke endpoint ([#2865](https://github.com/JanssenProject/jans/issues/2865)) ([9c68f91](https://github.com/JanssenProject/jans/commit/9c68f914e155de492e54121033c8f0ed45d66817))


### Bug Fixes

* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* **jans-fido2:** [#2971](https://github.com/JanssenProject/jans/issues/2971) ([#2972](https://github.com/JanssenProject/jans/issues/2972)) ([2f15cf8](https://github.com/JanssenProject/jans/commit/2f15cf8aba64410fe1dd0ef71e9860ae0ec919bd))
* login page doesn't display the correct localized characters ([#3528](https://github.com/JanssenProject/jans/issues/3528)) ([395b376](https://github.com/JanssenProject/jans/commit/395b3769750d2d32f624060c0b6e6ceeee7df0be)), closes [#1660](https://github.com/JanssenProject/jans/issues/1660)
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))


### Documentation

* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## [1.0.5](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.4...jans-core-v1.0.5) (2022-12-01)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))


### Bug Fixes

* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* **jans-fido2:** [#2971](https://github.com/JanssenProject/jans/issues/2971) ([#2972](https://github.com/JanssenProject/jans/issues/2972)) ([2f15cf8](https://github.com/JanssenProject/jans/commit/2f15cf8aba64410fe1dd0ef71e9860ae0ec919bd))

## [1.0.4](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.3...jans-core-v1.0.4) (2022-11-08)


### Features

* for file based scripts check both script revision and file ([#2878](https://github.com/JanssenProject/jans/issues/2878)) ([97ab071](https://github.com/JanssenProject/jans/commit/97ab0712e39605520b3ac8fb8df1c00bf0437797))
* ssa revoke endpoint ([#2865](https://github.com/JanssenProject/jans/issues/2865)) ([9c68f91](https://github.com/JanssenProject/jans/commit/9c68f914e155de492e54121033c8f0ed45d66817))


### Documentation

* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## 1.0.3 (2022-11-01)


### Features

* **jans-config-api:** pagination functionality for endpoints and swagger spec rectification ([#2397](https://github.com/JanssenProject/jans/issues/2397)) ([d893e13](https://github.com/JanssenProject/jans/commit/d893e13efd57871ed0a09688b9b02f4294a10d4f))
* **jans-core:** port Gluu ORM-based document store ([#2581](https://github.com/JanssenProject/jans/issues/2581)) ([b61df80](https://github.com/JanssenProject/jans/commit/b61df8094a390762395fae41bba394d4e6bbf796))
* ssa creation endpoint ([#2495](https://github.com/JanssenProject/jans/issues/2495)) ([61c83e3](https://github.com/JanssenProject/jans/commit/61c83e3305beeaf1a3dbde39d70324153281f218))


### Bug Fixes

* **jans-auth-server:** ssa get endpoint ([#2719](https://github.com/JanssenProject/jans/issues/2719)) ([35ffbf0](https://github.com/JanssenProject/jans/commit/35ffbf041e7da7376e07d8e7425a2925ce31f403))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* **jans-core:** removed redundant reference [#1927](https://github.com/JanssenProject/jans/issues/1927) ([#1928](https://github.com/JanssenProject/jans/issues/1928)) ([064cbb8](https://github.com/JanssenProject/jans/commit/064cbb8040f4d7b21ff13e5f48c7f923c38f67b1))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))


### Miscellaneous Chores

* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## 1.0.2 (2022-08-30)


### Features

* added config to disable attempt to update before insert in cache ([#1787](https://github.com/JanssenProject/jans/issues/1787)) ([d9a07ff](https://github.com/JanssenProject/jans/commit/d9a07ffb8dc1af290be6bc9b4978ad21c6797a3f))
* **jans-client-api:** migration to Weld/Resteasy and Jetty 11 - Issue 260 ([#1319](https://github.com/JanssenProject/jans/issues/1319)) ([420ffc3](https://github.com/JanssenProject/jans/commit/420ffc3329b91c52d5c9996d7c1e600d9b6fead2))
* **jans-core:** added StandaloneJavaCustomScriptManagerTest ([48ba08b](https://github.com/JanssenProject/jans/commit/48ba08b2f336c2cef1f244d1411c71859fe337a4))
* **jans-orm:** update Couchbase ORM to use SDK 3.x [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#1852](https://github.com/JanssenProject/jans/issues/1852)) ([d9d5157](https://github.com/JanssenProject/jans/commit/d9d5157c3421f4995ee4abd6918c106f9a78dd5f))


### Bug Fixes

* **jans-core:** removed redundant reference [#1927](https://github.com/JanssenProject/jans/issues/1927) ([#1928](https://github.com/JanssenProject/jans/issues/1928)) ([064cbb8](https://github.com/JanssenProject/jans/commit/064cbb8040f4d7b21ff13e5f48c7f923c38f67b1))
* **jans-core:** switch to 1.0.1-SNAPSHOT ([dbe9355](https://github.com/JanssenProject/jans/commit/dbe9355d97618a267df1ab7aa5c0780e125a3420))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.0...jans-core-v1.0.1) (2022-07-06)


### Features

* **jans-client-api:** migration to Weld/Resteasy and Jetty 11 - Issue 260 ([#1319](https://github.com/JanssenProject/jans/issues/1319)) ([420ffc3](https://github.com/JanssenProject/jans/commit/420ffc3329b91c52d5c9996d7c1e600d9b6fead2))
* **jans-core:** added Discovery.java script and sample external service ([440f2dd](https://github.com/JanssenProject/jans/commit/440f2dd41a0dafc915fd409b21da454f8cf1e046))
* **jans-core:** added StandaloneJavaCustomScriptManagerTest ([48ba08b](https://github.com/JanssenProject/jans/commit/48ba08b2f336c2cef1f244d1411c71859fe337a4))
* **jans-core:** added test dependencies to scripts ([53e5f67](https://github.com/JanssenProject/jans/commit/53e5f6725648521a983a86a533f62587b902f951))
* **jans-core:** wip for java compiler ([e038ff3](https://github.com/JanssenProject/jans/commit/e038ff3b1ea0d6ec670940e140304bde65e93926))


### Bug Fixes

* **jans-core:** switch to 1.0.1-SNAPSHOT ([dbe9355](https://github.com/JanssenProject/jans/commit/dbe9355d97618a267df1ab7aa5c0780e125a3420))


### Miscellaneous Chores

* release 1.0.0 ([3df6f77](https://github.com/JanssenProject/jans/commit/3df6f7721a8e9d57e28d065ee29153d023dfe9ea))
* release 1.0.0 ([9644d1b](https://github.com/JanssenProject/jans/commit/9644d1bd29c291e57c140b0c9ac67243c322ac35))
* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

## 1.0.0 (2022-05-19)


### Features

* **jans-auth-server:** changed prog lang name python->jython ([b9ba291](https://github.com/JanssenProject/jans/commit/b9ba291e576b8443f37c774088747bab09db2db9))
* **jans-core:** added more error logs if script is not loaded ([4084aeb](https://github.com/JanssenProject/jans/commit/4084aebc7076ac612f569f72478941a9f1284930))
* **jans-core:** compile java code on the fly for custom script ([5da6e27](https://github.com/JanssenProject/jans/commit/5da6e2743761cbdf8f06b3dca9a5cf7c8af1abe3))
* **jans-core:** corrected StandaloneCustomScriptManager ([0a52ec8](https://github.com/JanssenProject/jans/commit/0a52ec872d5ad7cbe065fbf3868c35df6e015393))
* **jans-core:** remove UPDATE_USER and USER_REGISTRATION scripts [#1289](https://github.com/JanssenProject/jans/issues/1289) ([c34e75d](https://github.com/JanssenProject/jans/commit/c34e75d49db89999633249376c7b42c41bb1ce24))
* **jans:** jetty 11 integration ([#1123](https://github.com/JanssenProject/jans/issues/1123)) ([6c1caa1](https://github.com/JanssenProject/jans/commit/6c1caa1c4c92d28571f8589cd701e6885d4d85ef))


### Bug Fixes

* **jans-auth-server:** fixed equals/hashcode by removing redundant dn field ([d27659d](https://github.com/JanssenProject/jans/commit/d27659d99200246de68387273c308bda012f39af))


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

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/jans-core-v1.0.0-beta.15...jans-core-v1.0.0-beta.16) (2022-03-14)


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

* **jans-auth-server:** add methods to dynamic client registration script to modify POST, PUT and GET responses ([#661](https://github.com/JanssenProject/jans/issues/661)) ([2aa2ba8](https://github.com/JanssenProject/jans/commit/2aa2ba86a2a639336079a1151ec38aca93ed9360))
* **jans-config-api:** config api interception script ([#840](https://github.com/JanssenProject/jans/issues/840)) ([8e4c688](https://github.com/JanssenProject/jans/commit/8e4c68889f9286e68ddd79d05ebd0d1bebd68097))
* **jans-core:** added methods for register response modification ([9f18613](https://github.com/JanssenProject/jans/commit/9f1861313982b1b702f854260b94637eac768a68))
* **jans-core:** added read response modification method ([74bbe38](https://github.com/JanssenProject/jans/commit/74bbe38ef5f4e3940e36a665c621daf95ca84bde))


### Bug Fixes

* use diamond operator ([#766](https://github.com/JanssenProject/jans/issues/766)) ([57664b0](https://github.com/JanssenProject/jans/commit/57664b0c0fd5926b2986f6b6d738d909e4865bca))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))
