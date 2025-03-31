# Changelog

## [1.0.22](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.21...jans-linux-setup-v1.0.22) (2024-01-22)


### Features

* **jans-auth-server:** support for OAuth 2.0 Rich Authorization Requests ([#7145](https://github.com/JanssenProject/jans/issues/7145)) ([c7d99c8](https://github.com/JanssenProject/jans/commit/c7d99c81efbaffd31b1b7d2963cd4f77768fd40e))
* **jans-casa:** use an attribute to designate administrative role ([#7421](https://github.com/JanssenProject/jans/issues/7421)) ([7190e6e](https://github.com/JanssenProject/jans/commit/7190e6ed62c58ee547ba548ea7eae56f4f10b74a))
* **jans-fido2:** update the size of the jansPublicKeyId field to 256 ([#7121](https://github.com/JanssenProject/jans/issues/7121)) ([33728da](https://github.com/JanssenProject/jans/commit/33728da0e3add85ae674d2c22a3c19d0bb0fe1a3))
* **jans-linux-setup:** auto-enable PubSub messages for Postgres and Lock ([#7277](https://github.com/JanssenProject/jans/issues/7277)) ([f3c9fa5](https://github.com/JanssenProject/jans/commit/f3c9fa5d614a01d4a1305f19824cbb8626f0cc3d))
* **jans-linux-setup:** install OPA ([#7182](https://github.com/JanssenProject/jans/issues/7182)) ([2549287](https://github.com/JanssenProject/jans/commit/2549287bc2d7d378ac55bfcb8a4d8cca50e6373b))
* **jans-linux-setup:** jans-lock installer ([#7170](https://github.com/JanssenProject/jans/issues/7170)) ([c3035b7](https://github.com/JanssenProject/jans/commit/c3035b7e00faeda773f8961b635aa8cf7dd7b1b0))
* **jans-linux-setup:** load test data to external Jans setup ([#5661](https://github.com/JanssenProject/jans/issues/5661)) ([c47dce0](https://github.com/JanssenProject/jans/commit/c47dce0282097214d034ad043d086b3080a949d4))
* **jans-linux-setup:** postgresql is default backend ([#7420](https://github.com/JanssenProject/jans/issues/7420)) ([46d1087](https://github.com/JanssenProject/jans/commit/46d10876174f3ee2db94071d1c1c95d2f0c073d6))
* **jans-linux-setup:** remove MariaDB support ([#7218](https://github.com/JanssenProject/jans/issues/7218)) ([2e938f4](https://github.com/JanssenProject/jans/commit/2e938f4ca64251d1215ab95caa507c8733783575))
* **jans-linux-setup:** resource provisioning on both jans-auth and keycloak ([#7447](https://github.com/JanssenProject/jans/issues/7447)) ([e8fa4cf](https://github.com/JanssenProject/jans/commit/e8fa4cf120d6fdcf64a3b26a032cf303d50b3443))
* **jans-linux-setup:** schema for admin-ui webhook ([#7373](https://github.com/JanssenProject/jans/issues/7373)) ([0479535](https://github.com/JanssenProject/jans/commit/0479535f2f33890ffcb0bd6589eb8ebbd950ce96))
* jans-lock service should have own log4j configuration [#7309](https://github.com/JanssenProject/jans/issues/7309) ([#7310](https://github.com/JanssenProject/jans/issues/7310)) ([800811d](https://github.com/JanssenProject/jans/commit/800811da331c632d990972a28dc178512a88eb33))
* lock script, policy downloader, data publisher ([#7229](https://github.com/JanssenProject/jans/issues/7229)) ([e1c1c41](https://github.com/JanssenProject/jans/commit/e1c1c41cbbfe81180169a8c81202c3fd4a31c75c))
* lock should check periodically policies updates in specified list of URIs and update them in OPA [#6541](https://github.com/JanssenProject/jans/issues/6541) ([#7416](https://github.com/JanssenProject/jans/issues/7416)) ([39b6096](https://github.com/JanssenProject/jans/commit/39b609640aa290f7607089950ac3525ab09e6d1f))
* lock should subscribe to messages from event system to get notifications about token IDs [#6539](https://github.com/JanssenProject/jans/issues/6539) ([#7143](https://github.com/JanssenProject/jans/issues/7143)) ([fd6ece5](https://github.com/JanssenProject/jans/commit/fd6ece561314e675a31cf8db2d0ca15f9edd0bd0))
* publish Lock message on id_token issue/revoke ([#7271](https://github.com/JanssenProject/jans/issues/7271)) ([7963dce](https://github.com/JanssenProject/jans/commit/7963dce0d3812aab0f649ea09b13cc3667732c60))
* rename JSON message configuration properties to conform karate test framework ([#7186](https://github.com/JanssenProject/jans/issues/7186)) ([1dc880d](https://github.com/JanssenProject/jans/commit/1dc880de92025ab2d92eeaa13465779d32bbef31))


### Bug Fixes

* **config-api:** IDP mysql creation issue and added version endpoint ([#7394](https://github.com/JanssenProject/jans/issues/7394)) ([7ed7b9d](https://github.com/JanssenProject/jans/commit/7ed7b9daae7b7b272bcacba0f9c8ad495a938c3e))
* **jans-linux-setup:** check couchbase readiness before creating buckets ([#7404](https://github.com/JanssenProject/jans/issues/7404)) ([e07acca](https://github.com/JanssenProject/jans/commit/e07acca05708026cc5c3e037c2e06f97f2c0602f))
* **jans-linux-setup:** KC client ([#7177](https://github.com/JanssenProject/jans/issues/7177)) ([1ef9d9f](https://github.com/JanssenProject/jans/commit/1ef9d9f68499314e98866d708017854120caf989))
* **jans-linux-setup:** KC version 23.0.3 ([#7140](https://github.com/JanssenProject/jans/issues/7140)) ([5b83bc8](https://github.com/JanssenProject/jans/commit/5b83bc8b233a0055dcb85d768e8e877ef98f194f))
* **jans-linux-setup:** modification date of MANIFEST.MF in war file is build date ([#7453](https://github.com/JanssenProject/jans/issues/7453)) ([3639863](https://github.com/JanssenProject/jans/commit/363986305f4e856071a287c51f7242d39b050bf3))
* **jans-linux-setup:** opa and jans-lock unit files ([#7328](https://github.com/JanssenProject/jans/issues/7328)) ([c1e4f61](https://github.com/JanssenProject/jans/commit/c1e4f61a5916f60cc740f107603ec1d22490d531))
* **jans-linux-setup:** print version ([#7329](https://github.com/JanssenProject/jans/issues/7329)) ([43793a6](https://github.com/JanssenProject/jans/commit/43793a6122c66b30035d933f597331114f843adc))
* **jans-linux-setup:** systemctl fido2 start order ([#7103](https://github.com/JanssenProject/jans/issues/7103)) ([07e078b](https://github.com/JanssenProject/jans/commit/07e078bb4457800296ba74f3cadf70a639f39c79))
* **jans-linux-setup:** typo and primission of printVersion.py ([#7355](https://github.com/JanssenProject/jans/issues/7355)) ([8a025f2](https://github.com/JanssenProject/jans/commit/8a025f202432d17387f2f60b4e91a800a0eac9b4))
* **jans-linux-setup:** uninstall opa ([#7184](https://github.com/JanssenProject/jans/issues/7184)) ([e9e27f9](https://github.com/JanssenProject/jans/commit/e9e27f932b9b36f019801adba7fdd69926644c98))
* **jans-linux-setup:** write default lock config even not installed ([#7371](https://github.com/JanssenProject/jans/issues/7371)) ([abf69d1](https://github.com/JanssenProject/jans/commit/abf69d12691caffa553b0e05eb9819ac2d053b19))
* **jans:** setting default value set and width issue for metedata file ([#7438](https://github.com/JanssenProject/jans/issues/7438)) ([fd4207f](https://github.com/JanssenProject/jans/commit/fd4207fa65eba86111295dd70db57f83d2e09413))
* **kc-saml-plugin:** exception handling and error handling ([#7351](https://github.com/JanssenProject/jans/issues/7351)) ([afe15ff](https://github.com/JanssenProject/jans/commit/afe15ff0c5e9030613db91bc0a1c8a0797a2d543))
* **kc-saml-plugin:** fixed IDP creation issue, enum values and removed kc lib dependency ([#7238](https://github.com/JanssenProject/jans/issues/7238)) ([d3401e3](https://github.com/JanssenProject/jans/commit/d3401e3b695f21a61c5ddc5675d242a30839ef06))
* prepare for 1.0.22 release ([#7455](https://github.com/JanssenProject/jans/issues/7455)) ([4bf2562](https://github.com/JanssenProject/jans/commit/4bf2562050c86317658259c72bb641780a283579))
* re-engineer how calls work in the engine ([#7152](https://github.com/JanssenProject/jans/issues/7152)) ([a940e7d](https://github.com/JanssenProject/jans/commit/a940e7dbc7da77c9f896dbe7bd73d7f9056231af))
* update ldif entry related to failing test [#7193](https://github.com/JanssenProject/jans/issues/7193) ([#7194](https://github.com/JanssenProject/jans/issues/7194)) ([ef2943e](https://github.com/JanssenProject/jans/commit/ef2943edbc728d55041175c6467a22395545ec58))

## [1.0.21](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.20...jans-linux-setup-v1.0.21) (2023-12-14)


### Features

* add message configuration api to config-api [#6982](https://github.com/JanssenProject/jans/issues/6982) ([#6983](https://github.com/JanssenProject/jans/issues/6983)) ([945ba76](https://github.com/JanssenProject/jans/commit/945ba767da90d2c6c376b5b6cca6313c0851bbca))
* **agama:** use a mixed strategy for serialization ([#6883](https://github.com/JanssenProject/jans/issues/6883)) ([00aee0c](https://github.com/JanssenProject/jans/commit/00aee0c26565e8b0b574370610a75139c2155568))
* **idp-plugin:** inbound SAML with Keycloak as SP and external SAML IDP ([#6793](https://github.com/JanssenProject/jans/issues/6793)) ([bc5eaad](https://github.com/JanssenProject/jans/commit/bc5eaade348d74d93da25c7494975b9aa35cded4))
* **jans-auth-server:** archived jwks ([#6503](https://github.com/JanssenProject/jans/issues/6503)) ([c86ae0a](https://github.com/JanssenProject/jans/commit/c86ae0a5a703ff96fd1e69fddcc110b5b754ad71))
* **jans-auth-server:** set feature flags state according to list discussed in [#6611](https://github.com/JanssenProject/jans/issues/6611) ([#6769](https://github.com/JanssenProject/jans/issues/6769)) ([fa98c32](https://github.com/JanssenProject/jans/commit/fa98c326cb8d8a51c36053e44363fdf6ddcef4b9))
* **jans-config:** changes to merge config-idp-plugin merged with config-saml-plugin ([#6921](https://github.com/JanssenProject/jans/issues/6921)) ([86e71c9](https://github.com/JanssenProject/jans/commit/86e71c944ee002f3ddda96280123a6aef36f2554))
* **jans-linux-setup:** config-api idp-plugin ([#6613](https://github.com/JanssenProject/jans/issues/6613)) ([291fe84](https://github.com/JanssenProject/jans/commit/291fe846fc654aeeb1e8c25882a3f97b21bed338))
* **jans-linux-setup:** config-idp-plugin related changes ([#6895](https://github.com/JanssenProject/jans/issues/6895)) ([caea056](https://github.com/JanssenProject/jans/commit/caea05603676f82c7f08ee81076664de3327f323))
* **jans-linux-setup:** put KC behind apache ([#7092](https://github.com/JanssenProject/jans/issues/7092)) ([2c545c3](https://github.com/JanssenProject/jans/commit/2c545c3ce7bdef03f548b0248f9505c1c34566a6))
* **jans-linux-setup:** table based col size for rdbm ([#6920](https://github.com/JanssenProject/jans/issues/6920)) ([29a58cd](https://github.com/JanssenProject/jans/commit/29a58cd2893ed646940102c2546124984a4264e7))
* **jans-linux-setup:** use reference token for TUI ([#6585](https://github.com/JanssenProject/jans/issues/6585)) ([2918c11](https://github.com/JanssenProject/jans/commit/2918c11a25b50a395c71ad5dc252cf49d319a407))
* **kc-saml-plugin:** saml enhancement for validation ([#6949](https://github.com/JanssenProject/jans/issues/6949)) ([ba07f32](https://github.com/JanssenProject/jans/commit/ba07f32edc6210fc3ad64d35338e5b7a642cb16a))


### Bug Fixes

* add missing attribute name [#6624](https://github.com/JanssenProject/jans/issues/6624) ([#6631](https://github.com/JanssenProject/jans/issues/6631)) ([86bbfa9](https://github.com/JanssenProject/jans/commit/86bbfa98a58fb187ed45ba52b34dc702aa07ed38))
* avoid crash when variables at the top of util.js are serialized ([#6614](https://github.com/JanssenProject/jans/issues/6614)) ([d1a10da](https://github.com/JanssenProject/jans/commit/d1a10dadc97bc887f9563995cc3168a436d48419))
* **jans-linux-setup:** jans saml installation ([#7002](https://github.com/JanssenProject/jans/issues/7002)) ([70e4c54](https://github.com/JanssenProject/jans/commit/70e4c54fcd1e64e6143f69f62887571b0f97c7a9))
* **jans-linux-setup:** postgresql permission ([#6890](https://github.com/JanssenProject/jans/issues/6890)) ([45ce059](https://github.com/JanssenProject/jans/commit/45ce0590e3eee3a729a560e1945c56513832831f))
* **jans-linux-setup:** python requests-toolbelt library for tui ([#7052](https://github.com/JanssenProject/jans/issues/7052)) ([f0ecba7](https://github.com/JanssenProject/jans/commit/f0ecba7f75ee3f697ee5e0436a32b208f1a7bc0c))
* **jans-linux-setup:** remove keycloak-storage-api.properties and render keycloak.conf ([#6529](https://github.com/JanssenProject/jans/issues/6529)) ([cc9d64f](https://github.com/JanssenProject/jans/commit/cc9d64f830ac3a07c7dbcbaafe920386e6fdcb7f))
* **jans-linux-setup:** sql key regeneration ([#7004](https://github.com/JanssenProject/jans/issues/7004)) ([a926d75](https://github.com/JanssenProject/jans/commit/a926d751904ac99b9aaffd880d07f7106366d622))
* **kc-saml-plugin:** error while updating SAML TR  ([#6974](https://github.com/JanssenProject/jans/issues/6974)) ([ad3041d](https://github.com/JanssenProject/jans/commit/ad3041d35134c7eca49379267c7fa1835e11a989))
* prepare for 1.0.21 release ([#7008](https://github.com/JanssenProject/jans/issues/7008)) ([2132de6](https://github.com/JanssenProject/jans/commit/2132de6683f67bf22d5a863b149770d657073a83))
* update transpiled code of test flows [#6624](https://github.com/JanssenProject/jans/issues/6624) ([#6625](https://github.com/JanssenProject/jans/issues/6625)) ([9481c53](https://github.com/JanssenProject/jans/commit/9481c53f18e0e9625bd5d4f77ca60c7714774158))

## [1.0.20](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.19...jans-linux-setup-v1.0.20) (2023-11-08)


### Features

* **jans-auth-server:** allow revoke any token - explicitly allow by config and scope [#6381](https://github.com/JanssenProject/jans/issues/6381) ([#6412](https://github.com/JanssenProject/jans/issues/6412)) ([47cbee9](https://github.com/JanssenProject/jans/commit/47cbee9cf917f0f79c53e9e0cfe1e2beab3108bc))
* **jans-auth-server:** enabled authorization_challenge scope for dynamic registration [#6277](https://github.com/JanssenProject/jans/issues/6277) ([#6278](https://github.com/JanssenProject/jans/issues/6278)) ([f46fa69](https://github.com/JanssenProject/jans/commit/f46fa695cea73a4d98bd8076c85bf21f819e1fe6))
* **jans-linux-setup:** display casa brwose url after setup ([#6315](https://github.com/JanssenProject/jans/issues/6315)) ([ef67b12](https://github.com/JanssenProject/jans/commit/ef67b12f1dccee6d49e7b9fa6c5a76814bb17273))
* **jans-linux-setup:** setup Jans-keycloak-link files ([#6281](https://github.com/JanssenProject/jans/issues/6281)) ([36c8c21](https://github.com/JanssenProject/jans/commit/36c8c21191b99446676bbf21b61eb78c01adda3a))


### Bug Fixes

* **jans-linux-setup:** change oxtrust to Janssen ([#6384](https://github.com/JanssenProject/jans/issues/6384)) ([7126c1c](https://github.com/JanssenProject/jans/commit/7126c1c004a40eef415ed0c9d293a3e396caa95f))
* **jans-linux-setup:** enable agama by default ([#6451](https://github.com/JanssenProject/jans/issues/6451)) ([3c14dd9](https://github.com/JanssenProject/jans/commit/3c14dd988d1977131e5019978b434d6fa5a569cc))
* **jans-linux-setup:** jans-saml dependencies ([#6379](https://github.com/JanssenProject/jans/issues/6379)) ([7da86e0](https://github.com/JanssenProject/jans/commit/7da86e0f0933d626776ffad39e5077b552438a18))
* **jans-linux-setup:** KC install fixes ([#6311](https://github.com/JanssenProject/jans/issues/6311)) ([32ad25d](https://github.com/JanssenProject/jans/commit/32ad25d144ac4010a9dbacfd8e34a28d0bd80a3c))
* **jans-linux-setup:** kc maven urls ([#6386](https://github.com/JanssenProject/jans/issues/6386)) ([e82c3a2](https://github.com/JanssenProject/jans/commit/e82c3a2aca88d1bef047301b1402256c1772b5a2))
* **jans-linux-setup:** saml install issues ([#6407](https://github.com/JanssenProject/jans/issues/6407)) ([2c1b94c](https://github.com/JanssenProject/jans/commit/2c1b94cf1602a0b212525c09ae570e49c2e7d7fb))
* **jans-linux-setup:** selected idp ([#6424](https://github.com/JanssenProject/jans/issues/6424)) ([7b60262](https://github.com/JanssenProject/jans/commit/7b602628ccf96e8b7fd080a6e08ade15daa33a05))
* **jans-linux-setup:** super gluu credentials url ([#6330](https://github.com/JanssenProject/jans/issues/6330)) ([a24cd67](https://github.com/JanssenProject/jans/commit/a24cd6712973df2aef2b9e1c46f1dfe5d9029e4c))
* **jans-linux-setup:** super gluu credentials url for test data ([#6333](https://github.com/JanssenProject/jans/issues/6333)) ([04582fa](https://github.com/JanssenProject/jans/commit/04582fa2a7edbb8eed0805cb1fabdcd95482b83f))
* **jans-linux-setup:** typo in filename of casa plugin twilio ([#6471](https://github.com/JanssenProject/jans/issues/6471)) ([c0549f6](https://github.com/JanssenProject/jans/commit/c0549f6ad1e6963358557047098c1f17435fb67a))
* prepare for 1.0.20 release ([c6e806e](https://github.com/JanssenProject/jans/commit/c6e806eb31fed998d52cbef7a7d94c231d913102))
* registration_uri for super gluu script defaults to an /identity ([#6369](https://github.com/JanssenProject/jans/issues/6369)) ([28c4714](https://github.com/JanssenProject/jans/commit/28c47141a22a1139762655e3ccab6cc34cf7798f)), closes [#6183](https://github.com/JanssenProject/jans/issues/6183)

## [1.0.19](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.18...jans-linux-setup-v1.0.19) (2023-10-11)


### Features

* **config-api, keycloak:** saml plugin to create trust client in DB and keycloak storage provider to jans store ([#6155](https://github.com/JanssenProject/jans/issues/6155)) ([c4f5034](https://github.com/JanssenProject/jans/commit/c4f50343ef5f991cc3c0184cef0fa83ff1d7f03c))
* **jans-auth-server:** added "authorization_challenge" scope enforcement [#5856](https://github.com/JanssenProject/jans/issues/5856) ([#6216](https://github.com/JanssenProject/jans/issues/6216)) ([b3db5c8](https://github.com/JanssenProject/jans/commit/b3db5c8cba829fc3e6aec350af7c0b4e5cf068c6))
* **jans-auth:** new lifetime attribute in ssa ([#6214](https://github.com/JanssenProject/jans/issues/6214)) ([b049e33](https://github.com/JanssenProject/jans/commit/b049e334bbe9d0c3b0214694e9fd6501019b8530))
* **jans-linux-setup:** hide saml setup for developers ([#6225](https://github.com/JanssenProject/jans/issues/6225)) ([7b8bae6](https://github.com/JanssenProject/jans/commit/7b8bae6c746492f9bdfc18eb0c43467ac831da56))


### Bug Fixes

* **jans-casa:** jans-casa installation issues ([#6198](https://github.com/JanssenProject/jans/issues/6198)) ([2775d6f](https://github.com/JanssenProject/jans/commit/2775d6f2b25e0bc62291b98d4db15856ff3ee48e))
* **jans-linux-setup:** casa url ([#6187](https://github.com/JanssenProject/jans/issues/6187)) ([0df0c86](https://github.com/JanssenProject/jans/commit/0df0c86a684827a2fb932c1fd80d6cf472d3b355))
* **jans-linux-setup:** post setup fix for config-api ([#6217](https://github.com/JanssenProject/jans/issues/6217)) ([ccfd8b1](https://github.com/JanssenProject/jans/commit/ccfd8b14001ae53fdd8d1e06125c4dba9a4bbc22))
* **jans-linux-setup:** post-setup install issues ([#6212](https://github.com/JanssenProject/jans/issues/6212)) ([9379e66](https://github.com/JanssenProject/jans/commit/9379e66d3e095cbd06180aa36016c15e398db8c0))
* **jans-linux-setup:** script hide attribute ([#6181](https://github.com/JanssenProject/jans/issues/6181)) ([b4711cd](https://github.com/JanssenProject/jans/commit/b4711cd1074dccc0b8aad81fc4e4bd238c8a516f))
* prepare for 1.0.19 release ([554fd43](https://github.com/JanssenProject/jans/commit/554fd434f624c4b4be3b2031c472177709da8966))

## [1.0.18](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.17...jans-linux-setup-v1.0.18) (2023-09-23)


### Bug Fixes

* prepare for 1.0.18 release ([87af7e4](https://github.com/JanssenProject/jans/commit/87af7e4d41728ce2966362883b47e5354f8c3803))

## [1.0.17](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.16...jans-linux-setup-v1.0.17) (2023-09-17)


### Features

* fido2 needs to search cache for session instead of persistent ([#6011](https://github.com/JanssenProject/jans/issues/6011)) ([0cc0c19](https://github.com/JanssenProject/jans/commit/0cc0c192735c0537c28bb7cc96a9db509d9628e0))
* **jans-auth-server:** added "The Use of Attestation in OAuth 2.0 Dynamic Client Registration" spec support [#5562](https://github.com/JanssenProject/jans/issues/5562) ([#5868](https://github.com/JanssenProject/jans/issues/5868)) ([38653c9](https://github.com/JanssenProject/jans/commit/38653c9cb9eb992213c5f230a5f36ce1187d0197))
* **jans-auth-server:** OAuth 2.0 for First-Party Native Applications ([#5654](https://github.com/JanssenProject/jans/issues/5654)) ([9d90e28](https://github.com/JanssenProject/jans/commit/9d90e28791c49bc86771623601c654f2c662b7a1))
* **jans-fido2:** mds optional ([#5409](https://github.com/JanssenProject/jans/issues/5409)) ([fad9961](https://github.com/JanssenProject/jans/commit/fad9961fbeeffb315d6ca495c43f8a4f000eac86))
* **jans-linux-setup:** add selinux policies to post setup messages ([#5797](https://github.com/JanssenProject/jans/issues/5797)) ([33db186](https://github.com/JanssenProject/jans/commit/33db186fa2fe69adeb79f8e9e58e525fabfa311e))
* **jans-linux-setup:** make Casa .administrable by default ([#5829](https://github.com/JanssenProject/jans/issues/5829)) ([cd9dbfb](https://github.com/JanssenProject/jans/commit/cd9dbfbdd331ca2a59b8605db9359d614f2a560b))
* **jans-linux-setup:** option for java version ([#5830](https://github.com/JanssenProject/jans/issues/5830)) ([5d9290c](https://github.com/JanssenProject/jans/commit/5d9290c750ae9108ec4fa5415208d41f9c1c3372))
* **jans-linux-setup:** salt with argument ([#5786](https://github.com/JanssenProject/jans/issues/5786)) ([d433827](https://github.com/JanssenProject/jans/commit/d4338271d910ec66e2788e77049c111046193a95))
* **jans-linux-setup:** support for CentOS Stream 9 ([#5803](https://github.com/JanssenProject/jans/issues/5803)) ([1fce49a](https://github.com/JanssenProject/jans/commit/1fce49a9f5301a1ff505e2b911c44b9800c1741f))
* **jans-linux-setup:** use builtin libs for tar, zip, wget ([#5899](https://github.com/JanssenProject/jans/issues/5899)) ([280c26d](https://github.com/JanssenProject/jans/commit/280c26d6a29b5800172cbde813f9854fdc5bf9fc))


### Bug Fixes

* add missing CB index for native SSO ([#6033](https://github.com/JanssenProject/jans/issues/6033)) ([ea8963c](https://github.com/JanssenProject/jans/commit/ea8963c0a91a5d467a22de0b05c51bcb10fc8041))
* data type too small for jansDeviceData [#5940](https://github.com/JanssenProject/jans/issues/5940) ([#5943](https://github.com/JanssenProject/jans/issues/5943)) ([f342154](https://github.com/JanssenProject/jans/commit/f3421547911c9a34ddb6ca7613f06f17f75b31cf))
* data type too small for jansDeviceNotificationConf [#5940](https://github.com/JanssenProject/jans/issues/5940) ([#5946](https://github.com/JanssenProject/jans/issues/5946)) ([39ffb3a](https://github.com/JanssenProject/jans/commit/39ffb3a35b397d8cac1a7472908a6aa6d17e1a33))
* failed to search session by deviceSecret if DB is PostgreSQL [#6012](https://github.com/JanssenProject/jans/issues/6012) ([#6013](https://github.com/JanssenProject/jans/issues/6013)) ([00b4c8d](https://github.com/JanssenProject/jans/commit/00b4c8da3c1b66e1d85d3dc13ba3ed4fff517431))
* **fido2:** Exception handling for assertion ([#5689](https://github.com/JanssenProject/jans/issues/5689)) ([2c82c5d](https://github.com/JanssenProject/jans/commit/2c82c5d73594464ed8ecec199f57774737cfd4e3))
* fix schema and update ldap samples ([#6024](https://github.com/JanssenProject/jans/issues/6024)) ([beabaef](https://github.com/JanssenProject/jans/commit/beabaef7835fa4331f2b889b69d8bb6fab436fcc))
* **jans-cli-tui:** move agama archiever to jans-cli directory ([#5721](https://github.com/JanssenProject/jans/issues/5721)) ([61053f0](https://github.com/JanssenProject/jans/commit/61053f0a9cee48415b57289b8236733dcd725199))
* **jans-linux-setup:** cb installation with args ([#5860](https://github.com/JanssenProject/jans/issues/5860)) ([2582fd5](https://github.com/JanssenProject/jans/commit/2582fd5018c3a5a40817267b0eaa9c5dc9bc2f50))
* **jans-linux-setup:** Gluu/Flex Casa ([#5916](https://github.com/JanssenProject/jans/issues/5916)) ([276c082](https://github.com/JanssenProject/jans/commit/276c0828837f0283adda9874ffe6d7fd3f02674f))
* **jans-linux-setup:** missing attribute mappings ([#5886](https://github.com/JanssenProject/jans/issues/5886)) ([d4bd70b](https://github.com/JanssenProject/jans/commit/d4bd70bfa070dada8165d3f4158038c63d9ea8c0))
* **jans-linux-setup:** node installer ([#5933](https://github.com/JanssenProject/jans/issues/5933)) ([ddbb2cf](https://github.com/JanssenProject/jans/commit/ddbb2cf1bed6aec78f17e57c1acce237533a1412))
* **jans-linux-setup:** prompt for Gluu Casa ([#5898](https://github.com/JanssenProject/jans/issues/5898)) ([e388a26](https://github.com/JanssenProject/jans/commit/e388a26d0ada0ca0197b6992f3828f357ae90c5d))
* **jans-linux-setup:** suse apache version ([#5938](https://github.com/JanssenProject/jans/issues/5938)) ([66c4ef7](https://github.com/JanssenProject/jans/commit/66c4ef766a62788437cce88974357a9a2b20de21))
* prepare for 1.0.17 release ([4ba8c15](https://github.com/JanssenProject/jans/commit/4ba8c151734f02d762e902b46a35cae2d498fa8f))

## [1.0.16](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.15...jans-linux-setup-v1.0.16) (2023-08-02)


### Features

* **jans-auth-server:** added DPoP-Nonce and client level dpop control "dpop_bound_access_tokens" ([#5607](https://github.com/JanssenProject/jans/issues/5607)) ([cc5a47a](https://github.com/JanssenProject/jans/commit/cc5a47a082b68f6655335e29559fd69f3c80e434))
* **jans-cli-tui:** agama-cli ([#5715](https://github.com/JanssenProject/jans/issues/5715)) ([5fec965](https://github.com/JanssenProject/jans/commit/5fec965f18429882ec0dc3686a5f83b6f9fb6086))
* **jans-linux-setup:** gluu/flex casa installer ([#5590](https://github.com/JanssenProject/jans/issues/5590)) ([2ce1152](https://github.com/JanssenProject/jans/commit/2ce11527485cece5dea4c714ae8f03b1b19510b1))


### Bug Fixes

* **config-api:** jans link fix for DN change ([#5543](https://github.com/JanssenProject/jans/issues/5543)) ([40e9d4e](https://github.com/JanssenProject/jans/commit/40e9d4e429ad579b80aec375ab6374ba9a0df9b7))
* enlarge column `adsPrjDeplDetails` ([#5644](https://github.com/JanssenProject/jans/issues/5644)) ([ae059fe](https://github.com/JanssenProject/jans/commit/ae059fe0018c3a7a059c2431e63ab0bd90d1f314))
* **jans-linux-setup:** casa install option --with-casa ([#5598](https://github.com/JanssenProject/jans/issues/5598)) ([4758bf5](https://github.com/JanssenProject/jans/commit/4758bf577bd61746c80d63ee8624fa19cdb3aeed))
* **jans-linux-setup:** cryptography, opensuse tumbleweed support, openssl requirement ([#5554](https://github.com/JanssenProject/jans/issues/5554)) ([0d461e4](https://github.com/JanssenProject/jans/commit/0d461e42e8ed8559633e8acd1a1a4ab66e3c9ef2))
* **jans-linux-setup:** jans-link download url ([#5706](https://github.com/JanssenProject/jans/issues/5706)) ([5b750f4](https://github.com/JanssenProject/jans/commit/5b750f42527d2d9b77d681ea893e0971866182db))
* **jans-linux-setup:** link configuration DN ([#5539](https://github.com/JanssenProject/jans/issues/5539)) ([929b790](https://github.com/JanssenProject/jans/commit/929b790020bd918b1764ddb5bc7359ad82da28c0))
* **jans-linux-setup:** load test data with setup.properties ([#5723](https://github.com/JanssenProject/jans/issues/5723)) ([b2fa5de](https://github.com/JanssenProject/jans/commit/b2fa5de84c6a9917e0ec0a57a529924b4409ba66))
* **jans-linux-setup:** new location of jans_test_client_keys.zip ([#5641](https://github.com/JanssenProject/jans/issues/5641)) ([4b00a3a](https://github.com/JanssenProject/jans/commit/4b00a3a17cae115cea31af8e099953666d14bc8d))
* **jans-linux-setup:** non-document entry ([#5677](https://github.com/JanssenProject/jans/issues/5677)) ([9c4edb3](https://github.com/JanssenProject/jans/commit/9c4edb313c21d6e48c0e81ef966999986d0ec2b8))
* **jans-linux-setup:** OS dependent location of jwks.json ([#5700](https://github.com/JanssenProject/jans/issues/5700)) ([df27569](https://github.com/JanssenProject/jans/commit/df2756995d9769ae2b5f9585f5a811b75d293a9f))
* **jans-linux-setup:** remove disabling selinux and apply policies ([#5453](https://github.com/JanssenProject/jans/issues/5453)) ([5b619a6](https://github.com/JanssenProject/jans/commit/5b619a641a3453ec757a7dce86814204451e0865))
* **jans-linux-setup:** remove fido-u2f-configuration proxypass from apache ([#5624](https://github.com/JanssenProject/jans/issues/5624)) ([46ced1f](https://github.com/JanssenProject/jans/commit/46ced1f5d073377915f1058465d53913f2beb862))
* **jans-linux-setup:** remove modification of dirAllowed ([#5701](https://github.com/JanssenProject/jans/issues/5701)) ([71107c1](https://github.com/JanssenProject/jans/commit/71107c1a154f58f5916ec6d8ef7c7ce9dd7eea9f))
* **jans-linux-setup:** remove proxypass fido-configuration ([#5629](https://github.com/JanssenProject/jans/issues/5629)) ([bb92297](https://github.com/JanssenProject/jans/commit/bb92297a970062eab9563675eba1d14a16abe449))
* **jans-linux-setup:** remove service files after stop while uninstalling ([#5602](https://github.com/JanssenProject/jans/issues/5602)) ([c82dbd2](https://github.com/JanssenProject/jans/commit/c82dbd2ce72a533cbc357f340043ab8bc1643430))
* **jans-linux-setup:** typed value when setting value ([#5691](https://github.com/JanssenProject/jans/issues/5691)) ([0795df2](https://github.com/JanssenProject/jans/commit/0795df2cc84ad48db2c35db3fcbd7576baa46b1f))
* **jans-linux-setup:** update build dependencies ([#5702](https://github.com/JanssenProject/jans/issues/5702)) ([0c65fbc](https://github.com/JanssenProject/jans/commit/0c65fbc3af7d85e07c2421e5c4e28fcb50c603e6))
* prepare for 1.0.16 release ([042ce79](https://github.com/JanssenProject/jans/commit/042ce7941b9597fade8d5f10e40a89d9e7662315))
* prepare for 1.0.16 release ([b2649c3](https://github.com/JanssenProject/jans/commit/b2649c33a9857f356f91df2f38787ec56269e6dd))

## [1.0.15](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.14...jans-linux-setup-v1.0.15) (2023-07-12)


### Features

* **fido2:** loading mds using external urls ([#5162](https://github.com/JanssenProject/jans/issues/5162)) ([d3d2294](https://github.com/JanssenProject/jans/commit/d3d2294ffabe3de9baa067b39f9578947b6d475f))
* **jans-link:** change schema name cache refresh to link ([#5484](https://github.com/JanssenProject/jans/issues/5484)) ([412f753](https://github.com/JanssenProject/jans/commit/412f753f1f11337b51ab1e212b9bd013021102be))
* **jans-link:** set jans-link by default disabled ([#5472](https://github.com/JanssenProject/jans/issues/5472)) ([67a6329](https://github.com/JanssenProject/jans/commit/67a63290addccb0c3244722c19d0a8591e92b458))
* **jans-linux-setup:** add option --keep-setup ([#5343](https://github.com/JanssenProject/jans/issues/5343)) ([cdec0d2](https://github.com/JanssenProject/jans/commit/cdec0d211d8bd7dda7d668c492c249541740a715))
* **jans-linux-setup:** argument -disable-selinux ([#5401](https://github.com/JanssenProject/jans/issues/5401)) ([11f5ab1](https://github.com/JanssenProject/jans/commit/11f5ab12471ca0424bacbd5e01099a6dd42401db))
* **jans-linux-setup:** cache refresh installation ([#4745](https://github.com/JanssenProject/jans/issues/4745)) ([94983ea](https://github.com/JanssenProject/jans/commit/94983ea08b12eb24603223f282eb64ea8981b6fe))
* **jans-linux-setup:** disable selinux ([#5386](https://github.com/JanssenProject/jans/issues/5386)) ([e684c5f](https://github.com/JanssenProject/jans/commit/e684c5f6e5216a5b5544c269770821f1ef662adc))
* **jans-linux-setup:** make jans_stat a default oauth scope [#5393](https://github.com/JanssenProject/jans/issues/5393) ([#5394](https://github.com/JanssenProject/jans/issues/5394)) ([1f62b47](https://github.com/JanssenProject/jans/commit/1f62b47f09d347501a5ae8bc302b29cb50140c63))


### Bug Fixes

* **jans-auth-server:** ClassCastException during select account [#5285](https://github.com/JanssenProject/jans/issues/5285) ([#5286](https://github.com/JanssenProject/jans/issues/5286)) ([4d17cbc](https://github.com/JanssenProject/jans/commit/4d17cbcdab3272653f2cf547bcef1d8181353ffd))
* **jans-link:** corrected import class in py script ([#5440](https://github.com/JanssenProject/jans/issues/5440)) ([9b02417](https://github.com/JanssenProject/jans/commit/9b024171c55f17dc3a3d588b250eb8b23d53e165))
* **jans-linux-setup:** cache refresh port ([#5279](https://github.com/JanssenProject/jans/issues/5279)) ([28fed19](https://github.com/JanssenProject/jans/commit/28fed198e972b4550427d96cd436d392268e1d78))
* **jans-linux-setup:** CR variable names and snapshot dir ([#5302](https://github.com/JanssenProject/jans/issues/5302)) ([17222c9](https://github.com/JanssenProject/jans/commit/17222c99beb7bc08bb012f96aa6bd454b546d117))
* **jans-linux-setup:** mapping changes for admin-ui ([#5362](https://github.com/JanssenProject/jans/issues/5362)) ([d7b0f54](https://github.com/JanssenProject/jans/commit/d7b0f54490a1ab48daed0fa4080036ce46374567))
* **jans-linux-setup:** opendj stop for nonldap backends ([#5318](https://github.com/JanssenProject/jans/issues/5318)) ([b084517](https://github.com/JanssenProject/jans/commit/b084517863b3b0b4739f4a97aaf96753b529f54d))
* **jans-linux-setup:** remove unit files upon uninstall ([#5344](https://github.com/JanssenProject/jans/issues/5344)) ([b42fbb8](https://github.com/JanssenProject/jans/commit/b42fbb86c913973f2a85388635b0bd61b75207ef))
* **jans-linux-setup:** scope offline_access is non-default ([#5359](https://github.com/JanssenProject/jans/issues/5359)) ([cc8cfc1](https://github.com/JanssenProject/jans/commit/cc8cfc1482a913720ba424128a7892798f947e4a))
* **jans-linux-setup:** spanner import ldif with modify add ([#5430](https://github.com/JanssenProject/jans/issues/5430)) ([6a2b408](https://github.com/JanssenProject/jans/commit/6a2b40889f25d8dd5c92e95f421f290f375b7edd))
* prepare for 1.0.15 release ([0e3cc2f](https://github.com/JanssenProject/jans/commit/0e3cc2f5ea287c2c35f45def54f074daa473ec49))

## [1.0.14](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.13...jans-linux-setup-v1.0.14) (2023-06-12)


### Features

* Add DCR flow ([#5096](https://github.com/JanssenProject/jans/issues/5096)) ([4bdea42](https://github.com/JanssenProject/jans/commit/4bdea425bb3d2b174049d03f3664db559e449eb9)), closes [#5092](https://github.com/JanssenProject/jans/issues/5092)
* **agama:** allow flows to supply the identity of the user to authenticate with a parameterizable attribute ([#5010](https://github.com/JanssenProject/jans/issues/5010)) ([ca941ce](https://github.com/JanssenProject/jans/commit/ca941ce0c2b54a84cd0327f8ac21fe926b533660))
* **config-api:** link-interception plugin code ([#5000](https://github.com/JanssenProject/jans/issues/5000)) ([eed9526](https://github.com/JanssenProject/jans/commit/eed9526415996278b52806cfe37a4dfb5076aa00))
* **fido2:** Apple_WebAuthn_Root_CA certificate is now downloaded to /authentication_cert folder and read from AttestationCertificateService ([#4756](https://github.com/JanssenProject/jans/issues/4756)) ([1600185](https://github.com/JanssenProject/jans/commit/16001859895d5aa5d840cfc9d425d0ee95149979))
* **jans-auth-server:** added ability to set client expiration via DCR [#5057](https://github.com/JanssenProject/jans/issues/5057) ([#5185](https://github.com/JanssenProject/jans/issues/5185)) ([a15054b](https://github.com/JanssenProject/jans/commit/a15054b1c3350d6ee0bb9c92d39f6b2d992abfa1))
* update SG script to conform prod server ([#5103](https://github.com/JanssenProject/jans/issues/5103)) ([0ec3ca8](https://github.com/JanssenProject/jans/commit/0ec3ca8b0e4e8e7287c8041dc75be9b29632da81))


### Bug Fixes

* **jans-linux-setup:** change location of persistence script ([#5095](https://github.com/JanssenProject/jans/issues/5095)) ([49054ae](https://github.com/JanssenProject/jans/commit/49054ae66d17d2e7d34febd31ac7ab25be3acdc4))
* **jans-linux-setup:** maridb json columns ([#5079](https://github.com/JanssenProject/jans/issues/5079)) ([c2be202](https://github.com/JanssenProject/jans/commit/c2be20230a58d1d0ca4111454051fcce83611cce))
* **jans-linux-setup:** remove dependency distutils ([#5011](https://github.com/JanssenProject/jans/issues/5011)) ([ca42086](https://github.com/JanssenProject/jans/commit/ca42086ef05a63a11006458416ef266a42c958f6))
* **jans-linux-setup:** type of jans_stat is oauth ([#5059](https://github.com/JanssenProject/jans/issues/5059)) ([c6cc459](https://github.com/JanssenProject/jans/commit/c6cc4595c4f266a26fbcb0ab919352bfc7b9d454))
* prepare for 1.0.14 release ([25ccadf](https://github.com/JanssenProject/jans/commit/25ccadf85327ea14685c6066dc6609919e4f2865))

## [1.0.13](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.12...jans-linux-setup-v1.0.13) (2023-05-10)


### Features

* **jans-auth-server:** add "introspection" scope check on introspection endpoint access [#4557](https://github.com/JanssenProject/jans/issues/4557) ([#4716](https://github.com/JanssenProject/jans/issues/4716)) ([ce2d75c](https://github.com/JanssenProject/jans/commit/ce2d75c32df382eb2a28f89793778a3e72659700))
* **jans-linux-setup:** detect arch before starting setup ([#4667](https://github.com/JanssenProject/jans/issues/4667)) ([302aa9b](https://github.com/JanssenProject/jans/commit/302aa9b8243c6aa90ffb0a722eff3e47f2aaa3e5))
* **jans-linux-setup:** move openbanking profile to gluu ([#4682](https://github.com/JanssenProject/jans/issues/4682)) ([05fc275](https://github.com/JanssenProject/jans/commit/05fc275948b4eceb6c9264c7107e0c2ae7ec6ebc))


### Bug Fixes

* **jans-linux-setup:** create opendj sysv script for k8s ([#4804](https://github.com/JanssenProject/jans/issues/4804)) ([64ba632](https://github.com/JanssenProject/jans/commit/64ba632ba8cf77414a46518ebcd83da154a42b06))
* **jans-linux-setup:** default keystore type pkcs12 ([#4788](https://github.com/JanssenProject/jans/issues/4788)) ([b57739c](https://github.com/JanssenProject/jans/commit/b57739c2736bdae75771262e9ebfaef17a93f458))
* **jans-linux-setup:** key_regeneration should look up db for keystore file ([#4780](https://github.com/JanssenProject/jans/issues/4780)) ([6fd85a8](https://github.com/JanssenProject/jans/commit/6fd85a83d715558835fe594a0c8ac76f6399bae6))
* **jans-linux-setup:** role_based introspection script ([#4738](https://github.com/JanssenProject/jans/issues/4738)) ([86c4fee](https://github.com/JanssenProject/jans/commit/86c4feedd696db0271022e4de0a7ad8092d31738))
* **jans-linux-setup:** use /opt/dist/scripts in case of k8s ([#4807](https://github.com/JanssenProject/jans/issues/4807)) ([461d96b](https://github.com/JanssenProject/jans/commit/461d96b9fda237c924d074f8ef2bbc98f19c429e))
* prepare for 1.0.13 release ([493478e](https://github.com/JanssenProject/jans/commit/493478e71f6231553c998b48c0f163c7f5869da4))

## [1.0.12](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.11...jans-linux-setup-v1.0.12) (2023-04-18)


### Features

* **jans-auth-server:** redirect back to RP when session is expired or if not possible show error page [#4449](https://github.com/JanssenProject/jans/issues/4449) ([#4505](https://github.com/JanssenProject/jans/issues/4505)) ([0983e73](https://github.com/JanssenProject/jans/commit/0983e7397ea2aa99423e5e928690666cd67ca8b2))


### Bug Fixes

* avoid setting agama configuration root dir based on java system variable ([#4524](https://github.com/JanssenProject/jans/issues/4524)) ([1d93fd7](https://github.com/JanssenProject/jans/commit/1d93fd7cc3dfd0592781602c5b5bb00f6d5adf4c))
* **jans-linux-setup:** remove scan_update_token ([#4621](https://github.com/JanssenProject/jans/issues/4621)) ([b20f115](https://github.com/JanssenProject/jans/commit/b20f115e7bf4fa75c7df9c120596e67273f1b10e))
* **jans-linux-setup:** version 1.0.12 ([#4509](https://github.com/JanssenProject/jans/issues/4509)) ([5d80442](https://github.com/JanssenProject/jans/commit/5d8044229af9f5e44e05cef76b179e7359cff2a8))
* prepare for 1.0.12 release ([6f83197](https://github.com/JanssenProject/jans/commit/6f83197705511c39413456acdc64e9136a97ff39))

## [1.0.11](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.10...jans-linux-setup-v1.0.11) (2023-04-05)


### Features

* **agama:** add a "default agama flow" to bridge ([#4309](https://github.com/JanssenProject/jans/issues/4309)) ([3b2248f](https://github.com/JanssenProject/jans/commit/3b2248fdb2a8e842cde1baca81132fa47613c356))
* **jans-auth-server:** increase sessionIdUnauthenticatedUnusedLifetime value in setup [#4445](https://github.com/JanssenProject/jans/issues/4445) ([#4446](https://github.com/JanssenProject/jans/issues/4446)) ([ecf9395](https://github.com/JanssenProject/jans/commit/ecf93955f391bcda17ad6a2f6ead00d79afee165))


### Bug Fixes

* fix push config name ([#4342](https://github.com/JanssenProject/jans/issues/4342)) ([3c8f9f4](https://github.com/JanssenProject/jans/commit/3c8f9f4887981e9f0d391acf22f66de847793b92))
* **jans-auth-server:** white/blank screen after device flow authn [#4237](https://github.com/JanssenProject/jans/issues/4237) ([#4243](https://github.com/JanssenProject/jans/issues/4243)) ([89f744d](https://github.com/JanssenProject/jans/commit/89f744dcaccb8f0813cee6663b4a8923898b8cc5))
* **jans-linux-setup:** disable agama script by default (avoid blank page) [#4374](https://github.com/JanssenProject/jans/issues/4374) ([#4375](https://github.com/JanssenProject/jans/issues/4375)) ([cd62ff9](https://github.com/JanssenProject/jans/commit/cd62ff9fe8783f2e87dd5e47c2362d35ba9713ef))
* **jans-linux-setup:** post setup ([#4325](https://github.com/JanssenProject/jans/issues/4325)) ([b3ae222](https://github.com/JanssenProject/jans/commit/b3ae2225134f4d41c61706bb936e35affc93a72d))
* **jans-linux-setup:** re-orginize creating smtp configuration ([#4457](https://github.com/JanssenProject/jans/issues/4457)) ([5b543cd](https://github.com/JanssenProject/jans/commit/5b543cdab320de7918b9078735cfafff744ede23))
* **jans-linux-setup:** remove password strength check for ldap ([#4376](https://github.com/JanssenProject/jans/issues/4376)) ([2c73b1d](https://github.com/JanssenProject/jans/commit/2c73b1d85b4b116797de85d5f8d2d196fe9cde9b))
* **jans-linux-setup:** TUI string ([#4288](https://github.com/JanssenProject/jans/issues/4288)) ([5db3693](https://github.com/JanssenProject/jans/commit/5db3693a70db474c6886070d3d2dd64baec43846))
* **jans-linux-setup:** typo - remove attribute county (ref: [#4058](https://github.com/JanssenProject/jans/issues/4058)) ([#4247](https://github.com/JanssenProject/jans/issues/4247)) ([b3756af](https://github.com/JanssenProject/jans/commit/b3756af0077aa38e1d8d2b11521c0e6897d3f739))
* mailservice should send non signed emails, if keystore isn't defied; ([#4455](https://github.com/JanssenProject/jans/issues/4455)) ([7b41c44](https://github.com/JanssenProject/jans/commit/7b41c44f2933b8fde79d0478cf8df69303b9b3ba))
* prepare for  release ([60775c0](https://github.com/JanssenProject/jans/commit/60775c09dc5ab9996bf80c03dcb457861d48dfb1))
* Unable to send emails issue 4121 ([#4333](https://github.com/JanssenProject/jans/issues/4333)) ([70a566b](https://github.com/JanssenProject/jans/commit/70a566b67f660750bf742f19ee127f79b2db8930))

## [1.0.10](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.9...jans-linux-setup-v1.0.10) (2023-03-16)


### Features

* **jans-auth-server:** added online_access scope to issue session bound refresh token [#3012](https://github.com/JanssenProject/jans/issues/3012) ([#4106](https://github.com/JanssenProject/jans/issues/4106)) ([635f611](https://github.com/JanssenProject/jans/commit/635f6119fdf4cdf3b3aed33515854ef68257c98f))
* **jans-linux-setup:** enable agama engine by default  ([#4131](https://github.com/JanssenProject/jans/issues/4131)) ([7e432dc](https://github.com/JanssenProject/jans/commit/7e432dcde57657d1cfa1cd45bde2206156dc6905))


### Bug Fixes

* **jans-linux-setup:** set jansAuthMode - default acr mode ([#4162](https://github.com/JanssenProject/jans/issues/4162)) ([f7d0489](https://github.com/JanssenProject/jans/commit/f7d0489e47a86ce146846dda2064d378dd4a0897))
* prepare release for 1.0.10 ([e996926](https://github.com/JanssenProject/jans/commit/e99692692ef04d881468d120f7c7d462568dce36))

## [1.0.9](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.8...jans-linux-setup-v1.0.9) (2023-03-09)


### Bug Fixes

* prepare 1.0.9 release ([e6ea522](https://github.com/JanssenProject/jans/commit/e6ea52220824bd6b5d2dca0539d9d515dbeda362))
* prepare 1.0.9 release ([55f7e0c](https://github.com/JanssenProject/jans/commit/55f7e0c308b869c2c4b5668aca751d022138a678))
* update next SNAPSHOT and dev ([0df0e7a](https://github.com/JanssenProject/jans/commit/0df0e7ae06af64ac477955119c2522f03e0603c3))

## [1.0.8](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.7...jans-linux-setup-v1.0.8) (2023-03-01)


### Bug Fixes

* **jans-linux-setup:** install cb before jans installation ([#3981](https://github.com/JanssenProject/jans/issues/3981)) ([dfed3b5](https://github.com/JanssenProject/jans/commit/dfed3b5457cae0bc378f5eaf845a4a5475bdf7e7))
* **jans-linux-setup:** install ncurses-compat-libs cb backend for el8 ([#3969](https://github.com/JanssenProject/jans/issues/3969)) ([412e07f](https://github.com/JanssenProject/jans/commit/412e07f4fcce17c1a801ab5161f1470dd949bab7))
* **jans-linux-setup:** start jans-auth after backend ([#3975](https://github.com/JanssenProject/jans/issues/3975)) ([4afbcee](https://github.com/JanssenProject/jans/commit/4afbcee6176aa2efc85c554da07058311f4e3233))

## [1.0.7](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.6...jans-linux-setup-v1.0.7) (2023-02-22)


### Features

* add -key_ops ALL to setup (ref: [#3747](https://github.com/JanssenProject/jans/issues/3747)) ([#3755](https://github.com/JanssenProject/jans/issues/3755)) ([3ce4bb2](https://github.com/JanssenProject/jans/commit/3ce4bb2847e7a776fc344750bb28292e83658cc0))
* add custom Github External Authenticator script for ADS [#3625](https://github.com/JanssenProject/jans/issues/3625) ([#3626](https://github.com/JanssenProject/jans/issues/3626)) ([f922a7a](https://github.com/JanssenProject/jans/commit/f922a7a7b075a43750dd792a91a11399517dbb9b))
* **config-api:** data conversion, audit log and swagger enhancement ([#3588](https://github.com/JanssenProject/jans/issues/3588)) ([a87b75b](https://github.com/JanssenProject/jans/commit/a87b75bb257b00f71ba643bc81ed110e0c914b79))
* **config-api:** plugin endpoint and audit interceptor ([#3613](https://github.com/JanssenProject/jans/issues/3613)) ([95fadc6](https://github.com/JanssenProject/jans/commit/95fadc6c89c4e91c6d143f0ab9efce0b9395fb14))
* **jans-auth-server:** added flexible date formatter handler to AS (required by certification tools) [#3600](https://github.com/JanssenProject/jans/issues/3600) ([#3601](https://github.com/JanssenProject/jans/issues/3601)) ([f646d73](https://github.com/JanssenProject/jans/commit/f646d734d79f9da83cfe51103811efd1f8677d7f))
* **jans-auth-server:** renamed "key_ops" -&gt; "key_ops_type" [#3790](https://github.com/JanssenProject/jans/issues/3790) ([#3792](https://github.com/JanssenProject/jans/issues/3792)) ([7a6bcba](https://github.com/JanssenProject/jans/commit/7a6bcba5ca3597f7556d406e4a572c76a229bbdf))
* **jans-auth-server:** use key_ops=ssa to generate jwt from ssa ([#3806](https://github.com/JanssenProject/jans/issues/3806)) ([2603bbb](https://github.com/JanssenProject/jans/commit/2603bbb1080345cab4fe814dca39024d8d0b5434))
* jans-linux-setup create test client with all available scopes ([#3696](https://github.com/JanssenProject/jans/issues/3696)) ([c2da52e](https://github.com/JanssenProject/jans/commit/c2da52e539cc2edf7e2792d507d02bd7886a901f))
* jans-linux-setup spanner rest client ([#3436](https://github.com/JanssenProject/jans/issues/3436)) ([e4d1d0c](https://github.com/JanssenProject/jans/commit/e4d1d0cc69dad7176d35af4608fd46c4c73ad4ba))
* jans-linux-setup ssa admin scope ([#3759](https://github.com/JanssenProject/jans/issues/3759)) ([485f7b4](https://github.com/JanssenProject/jans/commit/485f7b4c1718f9f088be9931d3b0312b78727bca))
* Support Super Gluu one step authentication to Fido2 server [#3593](https://github.com/JanssenProject/jans/issues/3593) ([#3599](https://github.com/JanssenProject/jans/issues/3599)) ([c013b16](https://github.com/JanssenProject/jans/commit/c013b161f2eb47f5952cbb80c8740f8d62d302c3))


### Bug Fixes

* auto installing of the GithubAuthenticatorForADS.py has been removed; ([#3889](https://github.com/JanssenProject/jans/issues/3889)) ([bd6b7ad](https://github.com/JanssenProject/jans/commit/bd6b7ad89d16ae8f80c25c6b375860132de7c97e))
* **config-api:** fixed start-up issue due to scope objectclass case ([#3697](https://github.com/JanssenProject/jans/issues/3697)) ([eac6440](https://github.com/JanssenProject/jans/commit/eac644071d1ca711564ae07361e66dd6aad84366))
* **config-api:** user service conflict with fido2 and script enhancement ([#3767](https://github.com/JanssenProject/jans/issues/3767)) ([5753d39](https://github.com/JanssenProject/jans/commit/5753d3989b96d76699f234cc87f58e355ba313b0))
* jans-linux-setup add twilio and jans-fido2 client libraries to jans-auth ([#3716](https://github.com/JanssenProject/jans/issues/3716)) ([4f43328](https://github.com/JanssenProject/jans/commit/4f433288fd46dcc0357e0fd7c4e40cc64842ce51))
* jans-linux-setup conversion fails for null integer field ([#3610](https://github.com/JanssenProject/jans/issues/3610)) ([207946c](https://github.com/JanssenProject/jans/commit/207946cad8f1b50b9bcb9a0e2f8539d335127492))
* jans-linux-setup downgrade cryptography ([#3635](https://github.com/JanssenProject/jans/issues/3635)) ([c7b5e3b](https://github.com/JanssenProject/jans/commit/c7b5e3b01de61807143877a166213554a98d42ea))
* jans-linux-setup downgrade jwt for py3.6 ([#3621](https://github.com/JanssenProject/jans/issues/3621)) ([322f752](https://github.com/JanssenProject/jans/commit/322f752ee15934d15b0398dae03dfd3d341129e0))
* jans-linux-setup external libs in jans-fido2.xml ([#3627](https://github.com/JanssenProject/jans/issues/3627)) ([8d4783b](https://github.com/JanssenProject/jans/commit/8d4783b7495c0793aad140f78bd1ad06a4aac932))
* jans-linux-setup installation without test client ([#3706](https://github.com/JanssenProject/jans/issues/3706)) ([e45f19e](https://github.com/JanssenProject/jans/commit/e45f19e6b4d750578cf14fb28eeb8e6c7e67174a))
* jans-linux-setup key_ops_type for key regeneration tool (ref: [#3881](https://github.com/JanssenProject/jans/issues/3881)) ([#3882](https://github.com/JanssenProject/jans/issues/3882)) ([51c0750](https://github.com/JanssenProject/jans/commit/51c07503eaf97455875ba7436299b04aaecb61c3))
* jans-linux-setup ldif property objectClass should be case sensitive ([#3702](https://github.com/JanssenProject/jans/issues/3702)) ([0dc14a0](https://github.com/JanssenProject/jans/commit/0dc14a0680f159e72ab8093c8a26865aea1cd33c))
* jans-linux-setup rename config-api swagger file ([#3678](https://github.com/JanssenProject/jans/issues/3678)) ([4615973](https://github.com/JanssenProject/jans/commit/46159736811dca62ba05f6b80478734d6262c047))
* jans-linux-setup rename role_based_client as tui_client ([#3630](https://github.com/JanssenProject/jans/issues/3630)) ([b331ef3](https://github.com/JanssenProject/jans/commit/b331ef32b49c870e0a972fc0463e954939317f88))
* jans-linux-setup save test_client_id to setup.properties ([#3844](https://github.com/JanssenProject/jans/issues/3844)) ([d1d898c](https://github.com/JanssenProject/jans/commit/d1d898c949e91b457c347f68a56b938238247b07))
* jans-linux-setup script locatipn is db ([#3788](https://github.com/JanssenProject/jans/issues/3788)) ([4381928](https://github.com/JanssenProject/jans/commit/438192893dc3e091de60fc711dd219054dad374a))
* jans-linux-setup script locatipn is db openbanking ([#3789](https://github.com/JanssenProject/jans/issues/3789)) ([697efc5](https://github.com/JanssenProject/jans/commit/697efc505d6d5f8be04f47d9e46f65ac28ca6ddc))
* jans-linux-setup sync test client variable names ([#3862](https://github.com/JanssenProject/jans/issues/3862)) ([fe7e24c](https://github.com/JanssenProject/jans/commit/fe7e24cfe18e0263e3e38dc8dd6eacad6c1bf21f))
* jans-linux-setup test client fixes ([#3699](https://github.com/JanssenProject/jans/issues/3699)) ([72e2f3f](https://github.com/JanssenProject/jans/commit/72e2f3f27b73c009ce68de68e3df484596f3595e))
* missing comma delimiter for Postgres index fields ([#3741](https://github.com/JanssenProject/jans/issues/3741)) ([1a2d298](https://github.com/JanssenProject/jans/commit/1a2d298c5b4911bfaccdf80203df1a919ff7a6b8))
* prepare 1.0.7 release ([ce02fd9](https://github.com/JanssenProject/jans/commit/ce02fd9322ab49d5bea4f6e88f316f931e9d2169))

## [1.0.6](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.5...jans-linux-setup-v1.0.6) (2023-01-09)


### Features

* add attributes and branch for ADS deployments [#3095](https://github.com/JanssenProject/jans/issues/3095) ([#3228](https://github.com/JanssenProject/jans/issues/3228)) ([2dc9267](https://github.com/JanssenProject/jans/commit/2dc9267423f464414b19718e5c2daa9f58283863))
* add benchmark demo ([#3325](https://github.com/JanssenProject/jans/issues/3325)) ([26bbb0c](https://github.com/JanssenProject/jans/commit/26bbb0ca2ef9ec5ac72f80ee3641d222036d55b2))
* **config-api:** audit log, agama ADS spec, fix for 0 index search ([#3369](https://github.com/JanssenProject/jans/issues/3369)) ([ea04e2c](https://github.com/JanssenProject/jans/commit/ea04e2ce5d83d4840638cd2e137fcbc67ee69c81))
* **config-api:** client claim enhancement, manual spec removed ([#3413](https://github.com/JanssenProject/jans/issues/3413)) ([bd2cdf8](https://github.com/JanssenProject/jans/commit/bd2cdf8501d60959498078bbb31650965c321c73))
* **jans-auth-server:** block authentication flow originating from a webview ([#3204](https://github.com/JanssenProject/jans/issues/3204)) ([e48380e](https://github.com/JanssenProject/jans/commit/e48380e68653cd4bd25ec2265225e4900e20bec1))
* **jans-auth-server:** new configuration for userinfo has been added ([#3349](https://github.com/JanssenProject/jans/issues/3349)) ([3ccc4a9](https://github.com/JanssenProject/jans/commit/3ccc4a9ad8486a0795d733bf8961999bad319438))
* **jans-auth-server:** remove ox properties name ([#3285](https://github.com/JanssenProject/jans/issues/3285)) ([f70b207](https://github.com/JanssenProject/jans/commit/f70b207ecff565ff53e3efb13d897937d9aeaee0))
* jans-linux-setup script for adding sequenced users to rdbm backend ([#3311](https://github.com/JanssenProject/jans/issues/3311)) ([63c74ec](https://github.com/JanssenProject/jans/commit/63c74ecd05f4be9bac2caaa281e10157b3e6ea37))


### Bug Fixes

* app_info.json value of JANS_BUILD ([#3199](https://github.com/JanssenProject/jans/issues/3199)) ([fe35e85](https://github.com/JanssenProject/jans/commit/fe35e855b91eac0a63903199f96dd4c6996cdce0))
* fix token indexes and clnId type ([#3434](https://github.com/JanssenProject/jans/issues/3434)) ([4a18904](https://github.com/JanssenProject/jans/commit/4a18904ebfc3c3562a3e2308ae3a7bf200c0d1bc))
* jans-linux setup enable couchbase for packages ([#3249](https://github.com/JanssenProject/jans/issues/3249)) ([8f72ea6](https://github.com/JanssenProject/jans/commit/8f72ea6b111925b5aa262c6a96c665cb6ffa0709))
* jans-linux-setup agama test data file locations ([#3313](https://github.com/JanssenProject/jans/issues/3313)) ([a39fc69](https://github.com/JanssenProject/jans/commit/a39fc6979117cdb173e60af57a3e78c467670c18))
* jans-linux-setup centos/rhel pgsql installation ([#3404](https://github.com/JanssenProject/jans/issues/3404)) ([f168fbc](https://github.com/JanssenProject/jans/commit/f168fbcbf98fb70ddbd4a0e4cb9854484579ac3c))
* jans-linux-setup copy libs directory of agama test data ([#3376](https://github.com/JanssenProject/jans/issues/3376)) ([6a5322d](https://github.com/JanssenProject/jans/commit/6a5322d2fb8362d6ceaab589707b61ffcf89140b))
* jans-linux-setup enable mysqld on boot for el8 ([#3456](https://github.com/JanssenProject/jans/issues/3456)) ([30d082a](https://github.com/JanssenProject/jans/commit/30d082a60817e7b17bdbb2a05afa3abe7ea39880))
* jans-linux-setup load test data with jans-auth only ([#3432](https://github.com/JanssenProject/jans/issues/3432)) ([f696fee](https://github.com/JanssenProject/jans/commit/f696fee7b82c417a6cfce03c31e50fcf969b37b5))
* jans-linux-setup longtext for pgsql ([#3266](https://github.com/JanssenProject/jans/issues/3266)) ([547cee8](https://github.com/JanssenProject/jans/commit/547cee89a40763c847244d770ea07366efa051a1))
* jans-linux-setup opPolicyUri and opTosUri ([#3411](https://github.com/JanssenProject/jans/issues/3411)) ([67e1d22](https://github.com/JanssenProject/jans/commit/67e1d22c1a2de2576d7891ff89bb0a964b149c6a))
* jans-linux-setup remove dependency to removed dependency yaml ([#3422](https://github.com/JanssenProject/jans/issues/3422)) ([8385c96](https://github.com/JanssenProject/jans/commit/8385c962895e668ae5b6b499ee9d3091574d4209))
* jans-linux-setup set db component based on dn ([#3290](https://github.com/JanssenProject/jans/issues/3290)) ([8d743f2](https://github.com/JanssenProject/jans/commit/8d743f21c0f723c2891155711a13ec8f5300b08a))
* jans-linux-setup start mysql server before jans-auth on SUSE ([#3500](https://github.com/JanssenProject/jans/issues/3500)) ([3822975](https://github.com/JanssenProject/jans/commit/382297560c344ebd1559c4c3f89595450a98fffa))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))
* TUI client pre-authorized ([#3399](https://github.com/JanssenProject/jans/issues/3399)) ([ab30953](https://github.com/JanssenProject/jans/commit/ab3095340d82c0b5b5b5342b0e21273b03017524))

## [1.0.5](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.4...jans-linux-setup-v1.0.5) (2022-12-01)


### Features

* add feature to include custom-claims in user-info endpoint of admin-ui plugin [#2969](https://github.com/JanssenProject/jans/issues/2969) ([#2970](https://github.com/JanssenProject/jans/issues/2970)) ([0549879](https://github.com/JanssenProject/jans/commit/054987944d91fe2021092c30b337e880421533ff))
* jans cli to jans-cli-tui ([#3063](https://github.com/JanssenProject/jans/issues/3063)) ([fc20e28](https://github.com/JanssenProject/jans/commit/fc20e287feb4cc1b7bb983c44e25a8ae936580f0))


### Bug Fixes

* [#2487](https://github.com/JanssenProject/jans/issues/2487) - fido script, doc already moved to script-catalog ([#2982](https://github.com/JanssenProject/jans/issues/2982)) ([10d8df5](https://github.com/JanssenProject/jans/commit/10d8df5480853a0545dbe6350f494d6b4abf3661))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - removing inwebo ([#2975](https://github.com/JanssenProject/jans/issues/2975)) ([052f91f](https://github.com/JanssenProject/jans/commit/052f91fd45c888efb7480fc7cd403dc005ceca23))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - SIWA and SIWG (Sign in with Apple-Google), moved to script-catalog ([#2983](https://github.com/JanssenProject/jans/issues/2983)) ([402e7ae](https://github.com/JanssenProject/jans/commit/402e7aebd20322ef465a3805d3834c7174bc9bbc))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) removing duplicated files ([#3007](https://github.com/JanssenProject/jans/issues/3007)) ([9f3d051](https://github.com/JanssenProject/jans/commit/9f3d051308e7b29e5f112e74601aa05c42ed559c))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) scripts-catalog folder restructuring ([#2999](https://github.com/JanssenProject/jans/issues/2999)) ([7b66f2b](https://github.com/JanssenProject/jans/commit/7b66f2b27517ba560555f64d0ab4e49f10ddb374))
* disable github authentication and interception scripts by default and other changes. [#3022](https://github.com/JanssenProject/jans/issues/3022) ([#3023](https://github.com/JanssenProject/jans/issues/3023)) ([13f5998](https://github.com/JanssenProject/jans/commit/13f599830c0d6b48bd1cd6f71f3d200ec6bddfe7))
* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* jans-linux-setup fido2 script placeholder in scripts template ([#2986](https://github.com/JanssenProject/jans/issues/2986)) ([70a4fe0](https://github.com/JanssenProject/jans/commit/70a4fe0d4b6ee958cfaa0b1598092da0fde38620))
* jans-linux-setup remove opendj sysv script ([#2998](https://github.com/JanssenProject/jans/issues/2998)) ([13eebe4](https://github.com/JanssenProject/jans/commit/13eebe4bdcbc059eb40b3e33b9bfb4a2830e8a0b))
* jans-linux-setup service description for jans-auth ([#2989](https://github.com/JanssenProject/jans/issues/2989)) ([6566d27](https://github.com/JanssenProject/jans/commit/6566d272de1e2fdcb5040df6d1616bd3164ebdab))

## [1.0.4](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.3...jans-linux-setup-v1.0.4) (2022-11-08)


### Features

* **jans-auth-server:** ssa validation endpoint ([#2842](https://github.com/JanssenProject/jans/issues/2842)) ([de8a86e](https://github.com/JanssenProject/jans/commit/de8a86ed1eb29bd02546e9e22fc6f668ac3217c4))
* ssa revoke endpoint ([#2865](https://github.com/JanssenProject/jans/issues/2865)) ([9c68f91](https://github.com/JanssenProject/jans/commit/9c68f914e155de492e54121033c8f0ed45d66817))


### Bug Fixes

* **jans-auth-server:** fix language metadata format ([#2883](https://github.com/JanssenProject/jans/issues/2883)) ([e21e206](https://github.com/JanssenProject/jans/commit/e21e206df16b048b1743c3ee441d9fbdb1f8c67e))
* jans-linux-setup render webapps.xml ([#2839](https://github.com/JanssenProject/jans/issues/2839)) ([ed8fa84](https://github.com/JanssenProject/jans/commit/ed8fa8462b69b37f44d0e5b5bb65345ea96ecc45))


### Documentation

* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## 1.0.3 (2022-11-01)


### Features

* add inum claim in profile scope [#2095](https://github.com/JanssenProject/jans/issues/2095) ([#2096](https://github.com/JanssenProject/jans/issues/2096)) ([f67c32e](https://github.com/JanssenProject/jans/commit/f67c32e7891f95c7a00ad0fa263444214dcaecd5))
* **agama:** add utility classes for inbound identity ([#2280](https://github.com/JanssenProject/jans/issues/2280)) ([ca6fdc9](https://github.com/JanssenProject/jans/commit/ca6fdc90256e4ef103bf50dc27cb694c940ba70b))
* disable TLS in CB client by default ([#2167](https://github.com/JanssenProject/jans/issues/2167)) ([8ec5dd3](https://github.com/JanssenProject/jans/commit/8ec5dd3dc9818a53949468389a1918ed385c28a9))
* **jans-auth-server:** add access_token_singing_alg_values_supported to discovery [#2372](https://github.com/JanssenProject/jans/issues/2372) ([#2403](https://github.com/JanssenProject/jans/issues/2403)) ([3784c83](https://github.com/JanssenProject/jans/commit/3784c837073c7a45871efc11dac1b721ae710cf1))
* **jans-auth-server:** added allowSpontaneousScopes AS json config [#2074](https://github.com/JanssenProject/jans/issues/2074) ([#2111](https://github.com/JanssenProject/jans/issues/2111)) ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* **jans-auth-server:** added creator info to scope (time/id/type) [#1934](https://github.com/JanssenProject/jans/issues/1934) ([#2023](https://github.com/JanssenProject/jans/issues/2023)) ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* **jans-auth-server:** Draft support of OpenID Connect Native SSO  ([#2711](https://github.com/JanssenProject/jans/issues/2711)) ([595d1aa](https://github.com/JanssenProject/jans/commit/595d1aa8ce93c00aa13fb726499ca26d8f2a41b6))
* **jans-auth-server:** extended client schema - added jansClientGroup [#1824](https://github.com/JanssenProject/jans/issues/1824) ([#2299](https://github.com/JanssenProject/jans/issues/2299)) ([29cfd4e](https://github.com/JanssenProject/jans/commit/29cfd4edaff1248c65d43d956b7b1db0f684d294))
* **jans-auth-server:** renamed "enabledComponents" conf property -&gt; "featureFlags" [#2290](https://github.com/JanssenProject/jans/issues/2290) ([#2319](https://github.com/JanssenProject/jans/issues/2319)) ([56a33c4](https://github.com/JanssenProject/jans/commit/56a33c40a2bf58ebeb87c6f1724f60a836dc29d2))
* **jans-config-api:** agama flow endpoint ([#1898](https://github.com/JanssenProject/jans/issues/1898)) ([0e73306](https://github.com/JanssenProject/jans/commit/0e73306f7642a74a3ed2cf8a8687a1ea447aa7bd))
* **jans-core:** port Gluu ORM-based document store ([#2581](https://github.com/JanssenProject/jans/issues/2581)) ([b61df80](https://github.com/JanssenProject/jans/commit/b61df8094a390762395fae41bba394d4e6bbf796))
* jans-linux-setup delete_dn() ([#2450](https://github.com/JanssenProject/jans/issues/2450)) ([b80a270](https://github.com/JanssenProject/jans/commit/b80a270d1f4c08e88a57a833f44ce3515a64a905))
* jans-linux-setup external CB/Spanner libs ([#2730](https://github.com/JanssenProject/jans/issues/2730)) ([d97bffe](https://github.com/JanssenProject/jans/commit/d97bffe96a1de6f3dbfcd600bb88dd8e7d086cd1))
* jans-linux-setup load agama test data ([#2749](https://github.com/JanssenProject/jans/issues/2749)) ([c368a02](https://github.com/JanssenProject/jans/commit/c368a027e0bcb087a94aca264695c78520de9442))
* jans-linux-setup option reset-rdbm-db ([#2413](https://github.com/JanssenProject/jans/issues/2413)) ([1029619](https://github.com/JanssenProject/jans/commit/1029619291e53f0b5783c13685c23f9c2535aa06))
* jans-linux-setup postgresql support ([#2409](https://github.com/JanssenProject/jans/issues/2409)) ([08ecaf9](https://github.com/JanssenProject/jans/commit/08ecaf96d94c9741fe52b99cc55d3459109034a3))
* jans-linux-setup set_class_path() ([#2442](https://github.com/JanssenProject/jans/issues/2442)) ([8128244](https://github.com/JanssenProject/jans/commit/8128244d16f212b9c687540ded6cb349abe4aafc))
* **jans-linux-setup:** added device_sso scope ([#2766](https://github.com/JanssenProject/jans/issues/2766)) ([7c7af09](https://github.com/JanssenProject/jans/commit/7c7af09244cf590a4b2a9570c3df81c0993a1569))
* **jans-linux-setup:** added token exchange grant type ([#2768](https://github.com/JanssenProject/jans/issues/2768)) ([b3abcfe](https://github.com/JanssenProject/jans/commit/b3abcfeb8fbaddd6d39eeacba018b6baaf6a2d75))
* **jans-scim:** make max no. of operations and payload size of bulks operations parameterizable ([#1872](https://github.com/JanssenProject/jans/issues/1872)) ([c27a45b](https://github.com/JanssenProject/jans/commit/c27a45bb0a19257c824c4e195f203e9b9b45ec88))
* update Coucbase ORM to conform SDK 3.x (config updates) [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#2118](https://github.com/JanssenProject/jans/issues/2118)) ([fceec83](https://github.com/JanssenProject/jans/commit/fceec8332fb36826e5dccb797ee79b769859e126))


### Bug Fixes

* **forgot_password:** update imports to jans locations ([#1637](https://github.com/JanssenProject/jans/issues/1637)) ([6c6eeb3](https://github.com/JanssenProject/jans/commit/6c6eeb334ffd4e09c91a7e5b7e5c3de7d5fdf037)), closes [#1601](https://github.com/JanssenProject/jans/issues/1601)
* **jans-auth-server:** added schema for ssa, corrected persistence, added ttl [#2543](https://github.com/JanssenProject/jans/issues/2543) ([#2544](https://github.com/JanssenProject/jans/issues/2544)) ([ce2bc3f](https://github.com/JanssenProject/jans/commit/ce2bc3f34d78dd9e11414d0db2c5870c77265177))
* **jans-auth-server:** NPE during OB discovery [#2793](https://github.com/JanssenProject/jans/issues/2793) ([#2794](https://github.com/JanssenProject/jans/issues/2794)) ([fb3ee86](https://github.com/JanssenProject/jans/commit/fb3ee86704e3255c51a121baff3ebf89eceb7f2a))
* **jans-auth-server:** npe in discovery if SSA endpoint is absent [#2497](https://github.com/JanssenProject/jans/issues/2497) ([#2498](https://github.com/JanssenProject/jans/issues/2498)) ([c3b00b4](https://github.com/JanssenProject/jans/commit/c3b00b4dac70f164216642cfa5b7f4e8e6a6d9dc))
* **jans-auth-server:** ssa get endpoint ([#2719](https://github.com/JanssenProject/jans/issues/2719)) ([35ffbf0](https://github.com/JanssenProject/jans/commit/35ffbf041e7da7376e07d8e7425a2925ce31f403))
* **jans-auth-server:** structure, instance customAttributes, initial data for ssa ([#2577](https://github.com/JanssenProject/jans/issues/2577)) ([f11f789](https://github.com/JanssenProject/jans/commit/f11f789e595762af0c38f1b93de4541ac456d282))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))
* jans-linus-setup typo ([#2427](https://github.com/JanssenProject/jans/issues/2427)) ([8b5f287](https://github.com/JanssenProject/jans/commit/8b5f287f5827814099136a41fac683bed0ff3c8c))
* jans-linux-setup add mod_rewrite to httpd_2.4.conf ([#1987](https://github.com/JanssenProject/jans/issues/1987)) ([b33b78e](https://github.com/JanssenProject/jans/commit/b33b78ed2e4e1c628db1ce130dfc6735ec93f83c))
* jans-linux-setup Config API installation status ([#2276](https://github.com/JanssenProject/jans/issues/2276)) ([6cf25ae](https://github.com/JanssenProject/jans/commit/6cf25aeaeaa6445022d6d69ea36675d8a863edb1))
* jans-linux-setup config api prompt ([#2293](https://github.com/JanssenProject/jans/issues/2293)) ([abfa315](https://github.com/JanssenProject/jans/commit/abfa315daa05846ba0b967e60f1496e87ed697f1))
* jans-linux-setup config-api scope type oauth ([#2318](https://github.com/JanssenProject/jans/issues/2318)) ([8e48d71](https://github.com/JanssenProject/jans/commit/8e48d7136fcb2b5718b2011b2ac3c45caae407ee))
* jans-linux-setup debian11 installation ([#2160](https://github.com/JanssenProject/jans/issues/2160)) ([8b99498](https://github.com/JanssenProject/jans/commit/8b99498ad0d4b7c45b962ad459e81553a0f65d75))
* jans-linux-setup don't call package installation unless missing packages ([#2641](https://github.com/JanssenProject/jans/issues/2641)) ([d340c3c](https://github.com/JanssenProject/jans/commit/d340c3cb9cbd57279dc76c5fbde936524124f60a))
* jans-linux-setup downloads dependencies without interaction in case -n ([#2546](https://github.com/JanssenProject/jans/issues/2546)) ([d53f9a2](https://github.com/JanssenProject/jans/commit/d53f9a2af03e20fcaf5bdac0e71363797ce2f9d3))
* jans-linux-setup extract files ([#2464](https://github.com/JanssenProject/jans/issues/2464)) ([35ced3e](https://github.com/JanssenProject/jans/commit/35ced3e0168525dc05f6ec0758e29990963fb515))
* jans-linux-setup humanize os name ([#2066](https://github.com/JanssenProject/jans/issues/2066)) ([8c89638](https://github.com/JanssenProject/jans/commit/8c89638f27e7d440bf82c91df27cdf9d263ae63d))
* jans-linux-setup install cb via apt for dependencies ([#2330](https://github.com/JanssenProject/jans/issues/2330)) ([732ce6a](https://github.com/JanssenProject/jans/commit/732ce6afb18fb1f352dfbf4ce971039b8824bc36))
* jans-linux-setup lowercase admin user status ([#2274](https://github.com/JanssenProject/jans/issues/2274)) ([28e5f06](https://github.com/JanssenProject/jans/commit/28e5f0692bd3c760e8c52f4c96bdcb819a329cdf))
* jans-linux-setup MySQL schema name is db name ([#2592](https://github.com/JanssenProject/jans/issues/2592)) ([2fc3d6e](https://github.com/JanssenProject/jans/commit/2fc3d6e23a93f1108fa5c47ddbe5b820b6da7a51))
* jans-linux-setup python3-psycopg2 ([#2423](https://github.com/JanssenProject/jans/issues/2423)) ([0d4aad2](https://github.com/JanssenProject/jans/commit/0d4aad2da1c2802a3f098c3590ffcde1c8d094d5))
* jans-linux-setup remove fido2 metadata-root-ca.cer ([#2594](https://github.com/JanssenProject/jans/issues/2594)) ([139a6a4](https://github.com/JanssenProject/jans/commit/139a6a457bbb45e0f363914f8512462c5a4cfaa7))
* jans-linux-setup scan docs/script-catalog for custom scripts ([#2488](https://github.com/JanssenProject/jans/issues/2488)) ([de585c9](https://github.com/JanssenProject/jans/commit/de585c9912c55b249eea160c041d0ca58434c6b0))
* jans-linux-setup test data load ([#2685](https://github.com/JanssenProject/jans/issues/2685)) ([4d4d848](https://github.com/JanssenProject/jans/commit/4d4d84860fbaa6c6d0cfe1572bb922ea283ed9f2))
* jans-linux-setup typo ([#2526](https://github.com/JanssenProject/jans/issues/2526)) ([4fa83fc](https://github.com/JanssenProject/jans/commit/4fa83fcc3f298d91be3cb459bca52417aacd368e))
* jans-linux-setup upgrade to MDS3 in fido2 ([#2507](https://github.com/JanssenProject/jans/issues/2507)) ([fcbcd2f](https://github.com/JanssenProject/jans/commit/fcbcd2f7e661db62d4a01148b8dbafab7573d0e4))
* jans-linux-setup-typo ([#2523](https://github.com/JanssenProject/jans/issues/2523)) ([579ccac](https://github.com/JanssenProject/jans/commit/579ccac9c8d0e698db029222126f8f6925f54851))
* **jans-linux-setup:** review columns size for Agama tables ([#2324](https://github.com/JanssenProject/jans/issues/2324)) ([55d7a7e](https://github.com/JanssenProject/jans/commit/55d7a7e855d1a2ba2cf550e0ddb3a8e9f948456a))
* moved to script-catalog ([#2485](https://github.com/JanssenProject/jans/issues/2485)) ([960b87f](https://github.com/JanssenProject/jans/commit/960b87ff5ace40c63aada576816cae648e82d65c))
* random password for keystores ([#2102](https://github.com/JanssenProject/jans/issues/2102)) ([b7d9af1](https://github.com/JanssenProject/jans/commit/b7d9af12ecf946498d279a5f577db0528e5522bc))
* update error pages ([#1957](https://github.com/JanssenProject/jans/issues/1957)) ([3d63f4d](https://github.com/JanssenProject/jans/commit/3d63f4d3d58e86d499271ebafdbc0dd56f5c4e98))


### Miscellaneous Chores

* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))


### Documentation

* no docs ([ce2bc3f](https://github.com/JanssenProject/jans/commit/ce2bc3f34d78dd9e11414d0db2c5870c77265177))
* no docs ([c3b00b4](https://github.com/JanssenProject/jans/commit/c3b00b4dac70f164216642cfa5b7f4e8e6a6d9dc))
* no docs ([3784c83](https://github.com/JanssenProject/jans/commit/3784c837073c7a45871efc11dac1b721ae710cf1))
* no docs ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* no docs ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* no docs (config-api swagger updated) ([56a33c4](https://github.com/JanssenProject/jans/commit/56a33c40a2bf58ebeb87c6f1724f60a836dc29d2))
* no docs (swagger updated) ([29cfd4e](https://github.com/JanssenProject/jans/commit/29cfd4edaff1248c65d43d956b7b1db0f684d294))
* updated ([739b939](https://github.com/JanssenProject/jans/commit/739b9393fe4d5fe2a99868d15dc514b69ed44419))

## 1.0.2 (2022-08-30)


### Features

* add inum claim in profile scope [#2095](https://github.com/JanssenProject/jans/issues/2095) ([#2096](https://github.com/JanssenProject/jans/issues/2096)) ([f67c32e](https://github.com/JanssenProject/jans/commit/f67c32e7891f95c7a00ad0fa263444214dcaecd5))
* **agama:** reject usage of repeated input names ([#1484](https://github.com/JanssenProject/jans/issues/1484)) ([aed8cf3](https://github.com/JanssenProject/jans/commit/aed8cf33d89b98f0ac6aae52e145a84a0937d60e))
* disable TLS in CB client by default ([#2167](https://github.com/JanssenProject/jans/issues/2167)) ([8ec5dd3](https://github.com/JanssenProject/jans/commit/8ec5dd3dc9818a53949468389a1918ed385c28a9))
* jans linux setup enable/disable script via arg ([#1634](https://github.com/JanssenProject/jans/issues/1634)) ([0b3cf16](https://github.com/JanssenProject/jans/commit/0b3cf16f524add8f27ca321ce2d82f6d61660456))
* jans linux setup openbanking CLI and certificate automation ([#1472](https://github.com/JanssenProject/jans/issues/1472)) ([62b5868](https://github.com/JanssenProject/jans/commit/62b5868e1e864a000be210d250602d43a2719b51))
* **jans-auth-server:** added allowSpontaneousScopes AS json config [#2074](https://github.com/JanssenProject/jans/issues/2074) ([#2111](https://github.com/JanssenProject/jans/issues/2111)) ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* **jans-auth-server:** added creator info to scope (time/id/type) [#1934](https://github.com/JanssenProject/jans/issues/1934) ([#2023](https://github.com/JanssenProject/jans/issues/2023)) ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* **jans-auth-server:** added restriction for request_uri parameter (blocklist and allowed client.request_uri) [#1503](https://github.com/JanssenProject/jans/issues/1503) ([0696d92](https://github.com/JanssenProject/jans/commit/0696d92094eeb2ed36f6b0075680634acbf8992f))
* **jans-auth-server:** persist org_id from software statement into client's "o" attribute ([021d3bd](https://github.com/JanssenProject/jans/commit/021d3bd17f8a9814e5a0d59b4f28b0c19da88ced))
* **jans-auth-server:** removed dcrSkipSignatureValidation configuration property [#1623](https://github.com/JanssenProject/jans/issues/1623) ([6550247](https://github.com/JanssenProject/jans/commit/6550247ca727d9437ffceec3fa12b9fef93b81e4))
* **jans-client-api:** migration to Weld/Resteasy and Jetty 11 - Issue 260 ([#1319](https://github.com/JanssenProject/jans/issues/1319)) ([420ffc3](https://github.com/JanssenProject/jans/commit/420ffc3329b91c52d5c9996d7c1e600d9b6fead2))
* **jans-config-api:** agama flow endpoint ([#1898](https://github.com/JanssenProject/jans/issues/1898)) ([0e73306](https://github.com/JanssenProject/jans/commit/0e73306f7642a74a3ed2cf8a8687a1ea447aa7bd))
* jans-linux-setup add forgot password script ([#1587](https://github.com/JanssenProject/jans/issues/1587)) ([b2e3eb3](https://github.com/JanssenProject/jans/commit/b2e3eb3f07bfc877ee6aee9a3fdd187d7abbf52b))
* jans-linux-setup agama ([#1486](https://github.com/JanssenProject/jans/issues/1486)) ([6b23bfe](https://github.com/JanssenProject/jans/commit/6b23bfe19ef960039f76df4de167c159312dd930))
* jans-linux-setup debian 11 packages ([#1769](https://github.com/JanssenProject/jans/issues/1769)) ([6fbef91](https://github.com/JanssenProject/jans/commit/6fbef91dcb4e14aaa78a898d945ef2c2e38ca722))
* jans-linux-setup Script for Keystroke Authentication ([#1853](https://github.com/JanssenProject/jans/issues/1853)) ([11a9e04](https://github.com/JanssenProject/jans/commit/11a9e040923925d2a3009bfc208321c9ea7ad33c))
* **jans-linux-setup:** [#1731](https://github.com/JanssenProject/jans/issues/1731) ([#1732](https://github.com/JanssenProject/jans/issues/1732)) ([6fad15b](https://github.com/JanssenProject/jans/commit/6fad15b339c5e6b29055e3acf350f455c47ddc93))
* **jans-linux-setup:** added discoveryDenyKeys [#1827](https://github.com/JanssenProject/jans/issues/1827) ([f77a6da](https://github.com/JanssenProject/jans/commit/f77a6da20a4a699998cac7c5dc098d09519c2fe4))
* **jans-scim:** make max no. of operations and payload size of bulks operations parameterizable ([#1872](https://github.com/JanssenProject/jans/issues/1872)) ([c27a45b](https://github.com/JanssenProject/jans/commit/c27a45bb0a19257c824c4e195f203e9b9b45ec88))
* update Coucbase ORM to conform SDK 3.x (config updates) [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#2118](https://github.com/JanssenProject/jans/issues/2118)) ([fceec83](https://github.com/JanssenProject/jans/commit/fceec8332fb36826e5dccb797ee79b769859e126))


### Bug Fixes

* **agama:** template overriding not working with more than one level of nesting ([#1841](https://github.com/JanssenProject/jans/issues/1841)) ([723922a](https://github.com/JanssenProject/jans/commit/723922a17b1babc49a1135030c06db367726ab63))
* build from source ([#1793](https://github.com/JanssenProject/jans/issues/1793)) ([e389363](https://github.com/JanssenProject/jans/commit/e389363e3fdad7149cdd73ea6fcbc4058f38819a))
* indentation ([#1821](https://github.com/JanssenProject/jans/issues/1821)) ([8353092](https://github.com/JanssenProject/jans/commit/83530920d920a6fd71bfb65545816af2e7f8511d))
* jans app and java version ([#1492](https://github.com/JanssenProject/jans/issues/1492)) ([1257e49](https://github.com/JanssenProject/jans/commit/1257e4923eee28e20018720c8815cd518c28bd2f))
* Jans cli user userpassword ([#1542](https://github.com/JanssenProject/jans/issues/1542)) ([d2e13a2](https://github.com/JanssenProject/jans/commit/d2e13a2bcb16a14f63f3bd99c4b3dc83d2a96d9a))
* jans-linux-setup add dummy jansRedirectURI to scim client ([5023c02](https://github.com/JanssenProject/jans/commit/5023c0277dbf6aea969f554978e61bb833210df9))
* jans-linux-setup add gcs module path for downloading apps ([#1538](https://github.com/JanssenProject/jans/issues/1538)) ([e540738](https://github.com/JanssenProject/jans/commit/e540738e2d0e6816562b4c927c0ce4bfbaafea56))
* jans-linux-setup add gcs path after packages check (ref: [#1514](https://github.com/JanssenProject/jans/issues/1514)) ([#1516](https://github.com/JanssenProject/jans/issues/1516)) ([31dd609](https://github.com/JanssenProject/jans/commit/31dd609ebe3fb36213cbbafa1db74c6fc50e01a2))
* jans-linux-setup add mod_rewrite to httpd_2.4.conf ([#1987](https://github.com/JanssenProject/jans/issues/1987)) ([b33b78e](https://github.com/JanssenProject/jans/commit/b33b78ed2e4e1c628db1ce130dfc6735ec93f83c))
* jans-linux-setup debian11 installation ([#2160](https://github.com/JanssenProject/jans/issues/2160)) ([8b99498](https://github.com/JanssenProject/jans/commit/8b99498ad0d4b7c45b962ad459e81553a0f65d75))
* jans-linux-setup disable script Forgot_Password_2FA_Token ([#1662](https://github.com/JanssenProject/jans/issues/1662)) ([377affc](https://github.com/JanssenProject/jans/commit/377affc238bca236324dd8eeb9d9e6750879560f))
* jans-linux-setup displayName of forgot-password script ([#1595](https://github.com/JanssenProject/jans/issues/1595)) ([07a5ea0](https://github.com/JanssenProject/jans/commit/07a5ea017c8d120b28e2bc578045160e4d3ff0ba))
* jans-linux-setup download jans-auth for --download-exit ([#1659](https://github.com/JanssenProject/jans/issues/1659)) ([879ed87](https://github.com/JanssenProject/jans/commit/879ed87035265f6bb714ba6283fb274fcdb2fca4))
* jans-linux-setup enable forgot-password script ([#1597](https://github.com/JanssenProject/jans/issues/1597)) ([149d19c](https://github.com/JanssenProject/jans/commit/149d19cc358b30c4cdd9d1383ace04d911402886))
* jans-linux-setup humanize os name ([#2066](https://github.com/JanssenProject/jans/issues/2066)) ([8c89638](https://github.com/JanssenProject/jans/commit/8c89638f27e7d440bf82c91df27cdf9d263ae63d))
* jans-linux-setup jans and jetty version (ref: [#1792](https://github.com/JanssenProject/jans/issues/1792)) ([#1795](https://github.com/JanssenProject/jans/issues/1795)) ([58cbe20](https://github.com/JanssenProject/jans/commit/58cbe20832c2c5efae69321a2a64c06327ae4bf5))
* jans-linux-setup multiple argument --import-ldif ([#1476](https://github.com/JanssenProject/jans/issues/1476)) ([5556f36](https://github.com/JanssenProject/jans/commit/5556f36073fab29f8d379fe763326c736f5186da))
* jans-linux-setup no prompt for eleven installation ([#1748](https://github.com/JanssenProject/jans/issues/1748)) ([7228391](https://github.com/JanssenProject/jans/commit/7228391a3e4c8012612289cc17173996fd9670c0))
* jans-linux-setup python executable when launching setup ([#1683](https://github.com/JanssenProject/jans/issues/1683)) ([87ac58c](https://github.com/JanssenProject/jans/commit/87ac58ca72fdeaafc230183ebe0375537d1c24be))
* jans-linux-setup remove 101-jans.ldif and 77-customAttributes.ldif ([#1831](https://github.com/JanssenProject/jans/issues/1831)) ([bea6302](https://github.com/JanssenProject/jans/commit/bea6302dfbaf8ecb7fe4eeb53d4f129aa4494aae))
* jans-linux-setup remove apache config when uninstall ([#1844](https://github.com/JanssenProject/jans/issues/1844)) ([4a5bc3e](https://github.com/JanssenProject/jans/commit/4a5bc3e53e711fa55288e0f118043d34244abf1f))
* jans-linux-setup remove temporary link file ([#1495](https://github.com/JanssenProject/jans/issues/1495)) ([673859a](https://github.com/JanssenProject/jans/commit/673859a864023e7f2a0ba4a7c36d6fa4a164faaa))
* jans-linux-setup securing files and dirs under /etc/jans ([#1782](https://github.com/JanssenProject/jans/issues/1782)) ([d64a7ae](https://github.com/JanssenProject/jans/commit/d64a7ae6c39805e61a5fa70da73f8337a8eecfe1))
* random password for keystores ([#2102](https://github.com/JanssenProject/jans/issues/2102)) ([b7d9af1](https://github.com/JanssenProject/jans/commit/b7d9af12ecf946498d279a5f577db0528e5522bc))
* update error pages ([#1957](https://github.com/JanssenProject/jans/issues/1957)) ([3d63f4d](https://github.com/JanssenProject/jans/commit/3d63f4d3d58e86d499271ebafdbc0dd56f5c4e98))


### Documentation

* no docs ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* no docs ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* no docs required ([f77a6da](https://github.com/JanssenProject/jans/commit/f77a6da20a4a699998cac7c5dc098d09519c2fe4))
* update script link [#1570](https://github.com/JanssenProject/jans/issues/1570) ([#1571](https://github.com/JanssenProject/jans/issues/1571)) ([eded5ed](https://github.com/JanssenProject/jans/commit/eded5edd851f6205dc1e889edbb334d6daefaa9f))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.0...jans-linux-setup-v1.0.1) (2022-07-06)


### Features

* **agama:** improve flows timeout ([#1447](https://github.com/JanssenProject/jans/issues/1447)) ([ccfb62e](https://github.com/JanssenProject/jans/commit/ccfb62ec13d371c96a0d597d5a0229864f044373))
* **agama:** reject usage of repeated input names ([#1484](https://github.com/JanssenProject/jans/issues/1484)) ([aed8cf3](https://github.com/JanssenProject/jans/commit/aed8cf33d89b98f0ac6aae52e145a84a0937d60e))
* jans linux setup enable/disable script via arg ([#1634](https://github.com/JanssenProject/jans/issues/1634)) ([0b3cf16](https://github.com/JanssenProject/jans/commit/0b3cf16f524add8f27ca321ce2d82f6d61660456))
* jans linux setup openbanking CLI and certificate automation ([#1472](https://github.com/JanssenProject/jans/issues/1472)) ([62b5868](https://github.com/JanssenProject/jans/commit/62b5868e1e864a000be210d250602d43a2719b51))
* **jans-auth-server:** added restriction for request_uri parameter (blocklist and allowed client.request_uri) [#1503](https://github.com/JanssenProject/jans/issues/1503) ([0696d92](https://github.com/JanssenProject/jans/commit/0696d92094eeb2ed36f6b0075680634acbf8992f))
* **jans-auth-server:** persist org_id from software statement into client's "o" attribute ([021d3bd](https://github.com/JanssenProject/jans/commit/021d3bd17f8a9814e5a0d59b4f28b0c19da88ced))
* **jans-auth-server:** removed dcrSkipSignatureValidation configuration property [#1623](https://github.com/JanssenProject/jans/issues/1623) ([6550247](https://github.com/JanssenProject/jans/commit/6550247ca727d9437ffceec3fa12b9fef93b81e4))
* **jans-client-api:** migration to Weld/Resteasy and Jetty 11 - Issue 260 ([#1319](https://github.com/JanssenProject/jans/issues/1319)) ([420ffc3](https://github.com/JanssenProject/jans/commit/420ffc3329b91c52d5c9996d7c1e600d9b6fead2))
* jans-linux-setup add forgot password script ([#1587](https://github.com/JanssenProject/jans/issues/1587)) ([b2e3eb3](https://github.com/JanssenProject/jans/commit/b2e3eb3f07bfc877ee6aee9a3fdd187d7abbf52b))
* jans-linux-setup agama ([#1486](https://github.com/JanssenProject/jans/issues/1486)) ([6b23bfe](https://github.com/JanssenProject/jans/commit/6b23bfe19ef960039f76df4de167c159312dd930))


### Bug Fixes

* jans app and java version ([#1492](https://github.com/JanssenProject/jans/issues/1492)) ([1257e49](https://github.com/JanssenProject/jans/commit/1257e4923eee28e20018720c8815cd518c28bd2f))
* Jans cli user userpassword ([#1542](https://github.com/JanssenProject/jans/issues/1542)) ([d2e13a2](https://github.com/JanssenProject/jans/commit/d2e13a2bcb16a14f63f3bd99c4b3dc83d2a96d9a))
* jans-linux-setup add gcs module path for downloading apps ([#1538](https://github.com/JanssenProject/jans/issues/1538)) ([e540738](https://github.com/JanssenProject/jans/commit/e540738e2d0e6816562b4c927c0ce4bfbaafea56))
* jans-linux-setup add gcs path after packages check (ref: [#1514](https://github.com/JanssenProject/jans/issues/1514)) ([#1516](https://github.com/JanssenProject/jans/issues/1516)) ([31dd609](https://github.com/JanssenProject/jans/commit/31dd609ebe3fb36213cbbafa1db74c6fc50e01a2))
* jans-linux-setup disable script Forgot_Password_2FA_Token ([#1662](https://github.com/JanssenProject/jans/issues/1662)) ([377affc](https://github.com/JanssenProject/jans/commit/377affc238bca236324dd8eeb9d9e6750879560f))
* jans-linux-setup displayName of forgot-password script ([#1595](https://github.com/JanssenProject/jans/issues/1595)) ([07a5ea0](https://github.com/JanssenProject/jans/commit/07a5ea017c8d120b28e2bc578045160e4d3ff0ba))
* jans-linux-setup download jans-auth for --download-exit ([#1659](https://github.com/JanssenProject/jans/issues/1659)) ([879ed87](https://github.com/JanssenProject/jans/commit/879ed87035265f6bb714ba6283fb274fcdb2fca4))
* jans-linux-setup enable forgot-password script ([#1597](https://github.com/JanssenProject/jans/issues/1597)) ([149d19c](https://github.com/JanssenProject/jans/commit/149d19cc358b30c4cdd9d1383ace04d911402886))
* jans-linux-setup multiple argument --import-ldif ([#1476](https://github.com/JanssenProject/jans/issues/1476)) ([5556f36](https://github.com/JanssenProject/jans/commit/5556f36073fab29f8d379fe763326c736f5186da))
* jans-linux-setup python executable when launching setup ([#1683](https://github.com/JanssenProject/jans/issues/1683)) ([87ac58c](https://github.com/JanssenProject/jans/commit/87ac58ca72fdeaafc230183ebe0375537d1c24be))
* jans-linux-setup remove temporary link file ([#1495](https://github.com/JanssenProject/jans/issues/1495)) ([673859a](https://github.com/JanssenProject/jans/commit/673859a864023e7f2a0ba4a7c36d6fa4a164faaa))


### Documentation

* update script link [#1570](https://github.com/JanssenProject/jans/issues/1570) ([#1571](https://github.com/JanssenProject/jans/issues/1571)) ([eded5ed](https://github.com/JanssenProject/jans/commit/eded5edd851f6205dc1e889edbb334d6daefaa9f))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

## 1.0.0 (2022-05-20)


### Features

* add schema updates [#1390](https://github.com/JanssenProject/jans/issues/1390) ([c9023b3](https://github.com/JanssenProject/jans/commit/c9023b3435fbc8079aabe5c70de3177ec9112308))
* add script for Google login ([#1141](https://github.com/JanssenProject/jans/issues/1141)) ([bac9144](https://github.com/JanssenProject/jans/commit/bac9144ad8a5f8f2b378aa67663caab9f19f052b))
* Jans linux setup refactor ([#1328](https://github.com/JanssenProject/jans/issues/1328)) ([79d3a75](https://github.com/JanssenProject/jans/commit/79d3a756bf0477907e4364c9887a316d4730c07a))
* Jans linux setup ubuntu22 Installation ([#1325](https://github.com/JanssenProject/jans/issues/1325)) ([8597750](https://github.com/JanssenProject/jans/commit/85977502e307884423b4b248694cf74b9b66b96a))
* **jans-auth-server:** [#808](https://github.com/JanssenProject/jans/issues/808) sign-in with apple interception script ([c21183a](https://github.com/JanssenProject/jans/commit/c21183ab6331f95531d76c6d279646cc3c0b600e))
* **jans-auth-server:** enable person authn script to have multiple acr names ([#1074](https://github.com/JanssenProject/jans/issues/1074)) ([1dc9250](https://github.com/JanssenProject/jans/commit/1dc9250b9140cfe2a7ea3daff6c9e0d6383c4bce))
* **jans-auth-server:** force signed request object ([#1052](https://github.com/JanssenProject/jans/issues/1052)) ([28ebbc1](https://github.com/JanssenProject/jans/commit/28ebbc10d545ad69ceb4e9a625fbbf13e6360b75))
* jans-cli use test client (ref: [#1283](https://github.com/JanssenProject/jans/issues/1283)) ([#1285](https://github.com/JanssenProject/jans/issues/1285)) ([6320af7](https://github.com/JanssenProject/jans/commit/6320af7ed82ea6fac5672c1c348aeecb7a4b5d7a))
* **jans-core:** added pure java discovery sample custom script ([1d01ba7](https://github.com/JanssenProject/jans/commit/1d01ba7b67ca5096c987c87c7315e163d632d39a))
* **jans-core:** remove UPDATE_USER and USER_REGISTRATION scripts [#1289](https://github.com/JanssenProject/jans/issues/1289) ([c34e75d](https://github.com/JanssenProject/jans/commit/c34e75d49db89999633249376c7b42c41bb1ce24))
* jans-linux-setup config-api fido2-plugin (ref: [#1303](https://github.com/JanssenProject/jans/issues/1303)) ([#1308](https://github.com/JanssenProject/jans/issues/1308)) ([ea929c0](https://github.com/JanssenProject/jans/commit/ea929c0637c40ee75f3adbd5377c5e08aebbe087))
* jans-linux-setup copy site packages in case of pyz ([8a8a05e](https://github.com/JanssenProject/jans/commit/8a8a05e4990088c65692b6032d3dd8d084913b49))
* jans-linux-setup fido metadata folder ([8e95b7a](https://github.com/JanssenProject/jans/commit/8e95b7a3891d7168b08015b9606bad651f33d94e))
* jans-linux-setup load pure java sample custom script ([#1335](https://github.com/JanssenProject/jans/issues/1335)) ([60cb36c](https://github.com/JanssenProject/jans/commit/60cb36c1f46ac7ef383072a5ad753dc4ef6320b3))
* jans-linux-setup refactor key reneration for all backends (ref: [#1147](https://github.com/JanssenProject/jans/issues/1147)) ([#1228](https://github.com/JanssenProject/jans/issues/1228)) ([cbe29c4](https://github.com/JanssenProject/jans/commit/cbe29c4990405d9e00f665e51791f5dda94526a2))
* jans-linux-setup set DefaultTimeoutStartSec=300s ([#1279](https://github.com/JanssenProject/jans/issues/1279)) ([6b511c4](https://github.com/JanssenProject/jans/commit/6b511c4b504303d05e930e01a999c919ec9c7bbc))
* jans-linux-setup show version ([b16b77d](https://github.com/JanssenProject/jans/commit/b16b77dd0db16f07e2e0de81603be74ec4eab546))
* **jans-linux-setup:** config-api user management plugin (ref: #[#1213](https://github.com/JanssenProject/jans/issues/1213)) ([#1223](https://github.com/JanssenProject/jans/issues/1223)) ([450c78c](https://github.com/JanssenProject/jans/commit/450c78cc2097a08ea485c26e8df6eb36146ca3be))
* **jans-linux-setup:** multivalued json enhancement ([#1102](https://github.com/JanssenProject/jans/issues/1102)) ([b8fb658](https://github.com/JanssenProject/jans/commit/b8fb658e36eb13e0d6199eeec093733bf167ca1a))
* **jans:** jetty 11 integration ([#1123](https://github.com/JanssenProject/jans/issues/1123)) ([6c1caa1](https://github.com/JanssenProject/jans/commit/6c1caa1c4c92d28571f8589cd701e6885d4d85ef))
* move file downloads to setup ([2680bd0](https://github.com/JanssenProject/jans/commit/2680bd01aa9227aa517de5c7de97c51b9b123a28))
* user management enhancement to chk mandatory feilds ([3ac4b19](https://github.com/JanssenProject/jans/commit/3ac4b19ada28b11a27707c56ad266ce282f13b60))


### Bug Fixes

* [#1107](https://github.com/JanssenProject/jans/issues/1107) - not required ([cf46672](https://github.com/JanssenProject/jans/commit/cf466722c5ddd70b491d79a82e080557a32ce161))
* [#1107](https://github.com/JanssenProject/jans/issues/1107) jansCodeChallengeHash missing ([65ac184](https://github.com/JanssenProject/jans/commit/65ac1846f19e3d8e7d4833e009cb5cdd58ff2c09))
* adjust beans and schema [#1107](https://github.com/JanssenProject/jans/issues/1107) ([#1248](https://github.com/JanssenProject/jans/issues/1248)) ([369129d](https://github.com/JanssenProject/jans/commit/369129d0c2614afb536d0e1329ac106fd7da187d))
* code smells ([e5aaad7](https://github.com/JanssenProject/jans/commit/e5aaad7da310b26c4d6a6b8cfb6ed00e442b1629))
* Data too long for column [#1107](https://github.com/JanssenProject/jans/issues/1107) ([8eb2c70](https://github.com/JanssenProject/jans/commit/8eb2c70c95c2e60486ff1dd5a9e00acd9d70dc3b))
* extract directory ([fe7a3c5](https://github.com/JanssenProject/jans/commit/fe7a3c564fb867fda4b28181c3158e8d282da238))
* **jans-auth-server:** removed ThumbSignInExternalAuthenticator ([a13ca51](https://github.com/JanssenProject/jans/commit/a13ca51a753bc7f779899e0c86865c1a6bdb0374))
* **jans-auth-server:** validate redirect_uri blank and client redirect uris single item to return by default ([#1046](https://github.com/JanssenProject/jans/issues/1046)) ([aa139e4](https://github.com/JanssenProject/jans/commit/aa139e46e6d25c6135eb05e22dbc36fe84eb3e86))
* jans-linux-setup --add-module ([4f6b8a9](https://github.com/JanssenProject/jans/commit/4f6b8a9d6482f89426a82596fa1cfbc1cf12159a))
* jans-linux-setup code smell ([09bb36e](https://github.com/JanssenProject/jans/commit/09bb36ed70620238261e39689c8d843d6d0212b7))
* jans-linux-setup code smell ([b790c01](https://github.com/JanssenProject/jans/commit/b790c0181e50b1de22114e922b7c0788e50338d0))
* jans-linux-setup code smell ([3c57d5e](https://github.com/JanssenProject/jans/commit/3c57d5efb5e81b12b9a52b087e35fba42eb664a3))
* jans-linux-setup code smells ([4f362e5](https://github.com/JanssenProject/jans/commit/4f362e59e50f6598195e71f03dbe863fcb82526e))
* jans-linux-setup code smells ([824cf1f](https://github.com/JanssenProject/jans/commit/824cf1f2821f2e365e8905da21fb353de34048d4))
* jans-linux-setup code smells ([b2a48db](https://github.com/JanssenProject/jans/commit/b2a48db9df08d5566d8b98f2debfd3ba96b48435))
* jans-linux-setup code smells ([e930f16](https://github.com/JanssenProject/jans/commit/e930f16f34570645357dcc29d0cf9df3cb16cf4d))
* jans-linux-setup code smells ([45953c6](https://github.com/JanssenProject/jans/commit/45953c6daa0a2abece7fc08913bffcd76a11dce0))
* jans-linux-setup code smells ([b01da85](https://github.com/JanssenProject/jans/commit/b01da856158d2e59cc741ce6ff9283d1a549388f))
* jans-linux-setup config-api plugin dependencies ([#1310](https://github.com/JanssenProject/jans/issues/1310)) ([b5577dd](https://github.com/JanssenProject/jans/commit/b5577ddcec55ef9a47fca30ebd897867d5601f40))
* jans-linux-setup copy_tree ([2c2ad3a](https://github.com/JanssenProject/jans/commit/2c2ad3a100119d3c6cfb39b1454a6c9ba4e55a9a))
* jans-linux-setup create json index for multivalued attributes ([#1131](https://github.com/JanssenProject/jans/issues/1131)) ([be9e63c](https://github.com/JanssenProject/jans/commit/be9e63c6a96b6d91672d4e6b9700625da1196be3))
* jans-linux-setup dependency prompt-toolkit ([865647e](https://github.com/JanssenProject/jans/commit/865647eacdf3e4c9b0a26a0d3b4980fe2c3464b7))
* jans-linux-setup maven url ([244135d](https://github.com/JanssenProject/jans/commit/244135d0af7253896ccacbdcd59034666c7ace59))
* jans-linux-setup move mysql-timezone to config ([31df7db](https://github.com/JanssenProject/jans/commit/31df7db5d130194b3bc65fc014c92b926f828291))
* jans-linux-setup multivalued json mapping (ref: [#1088](https://github.com/JanssenProject/jans/issues/1088)) ([#1090](https://github.com/JanssenProject/jans/issues/1090)) ([e3d9dbf](https://github.com/JanssenProject/jans/commit/e3d9dbffdab29d58d31dab004f5d392f5ada0591))
* jans-linux-setup openbanking setup issues ([3837dd2](https://github.com/JanssenProject/jans/commit/3837dd2fd0a48de16625df368be3a3f23f5d3625))
* jans-linux-setup set log level to TRACE for test data ([#1345](https://github.com/JanssenProject/jans/issues/1345)) ([21a2120](https://github.com/JanssenProject/jans/commit/21a21201dcbf3b3e3d8aecf878122996b09349d1))
* jans-linux-setup typo ([#1311](https://github.com/JanssenProject/jans/issues/1311)) ([97723d5](https://github.com/JanssenProject/jans/commit/97723d588ba736bcf6e71f9a02028708c27b4fff))
* jans-linux-setup url of config api scim plugin ([da007f0](https://github.com/JanssenProject/jans/commit/da007f074ef35d1b613dccdb4dd29e22afe98f7a))
* jans-linux-setup-key key-regeneration fix spanner host ([#1229](https://github.com/JanssenProject/jans/issues/1229)) ([5a472ad](https://github.com/JanssenProject/jans/commit/5a472ad4f9c1e27ca361275353944043a12a5c1e))
* **jans-linux-setup:** copy user-mgt-plugin ([#1225](https://github.com/JanssenProject/jans/issues/1225)) ([8def41a](https://github.com/JanssenProject/jans/commit/8def41a608ad611ee2981ca790d4cc64d74d23c6))
* **jans-linux-setup:** defaults loggingLevel to INFO ([#1346](https://github.com/JanssenProject/jans/issues/1346)) ([26b1163](https://github.com/JanssenProject/jans/commit/26b116366bc640377d22ec5ac911be6b7cc009a3))
* **jans-linux-setup:** enable mod_auth_openidc ([#1048](https://github.com/JanssenProject/jans/issues/1048)) ([40e24ea](https://github.com/JanssenProject/jans/commit/40e24eac2f8e4b1903047afe077b2508067da7b3))
* **jans-linux-setup:** minor typo ([#1109](https://github.com/JanssenProject/jans/issues/1109)) ([32b5af5](https://github.com/JanssenProject/jans/commit/32b5af5beff24366de0e190126ee4720ae003cd9))
* **jans-linux-setup:** rdbm index ([#1135](https://github.com/JanssenProject/jans/issues/1135)) ([ec3bd1b](https://github.com/JanssenProject/jans/commit/ec3bd1bd2eb1c97aedd8824bad4bf90282e5fe06))
* **jans-linux-setup:** remove attributes of size 64 from sql_data_types.json ([#1112](https://github.com/JanssenProject/jans/issues/1112)) ([1726d09](https://github.com/JanssenProject/jans/commit/1726d09299bd11a3cb6c963a89770de26e043403))
* linux-setup don't use personCustomObjectClassList for RDBMS (ref: [#1214](https://github.com/JanssenProject/jans/issues/1214)) ([#1216](https://github.com/JanssenProject/jans/issues/1216)) ([4d8dff7](https://github.com/JanssenProject/jans/commit/4d8dff7dc3957f5aa57dee1e2a3060612196c8a4))
* Make column wider [#1044](https://github.com/JanssenProject/jans/issues/1044) ([f3e393f](https://github.com/JanssenProject/jans/commit/f3e393fe523d1edbe1ff110eaeb4caf6fdcfc61c))
* Security Hotspot ([4e091c4](https://github.com/JanssenProject/jans/commit/4e091c4d447d298b1d3320b8f1fd45c984ef402d))
* Security Hotspot ([1899a39](https://github.com/JanssenProject/jans/commit/1899a39ef68aaf7f535e3caf1945ef94412a8d30))
* submit button is missing from the Properties page [#175](https://github.com/JanssenProject/jans/issues/175) ([2424965](https://github.com/JanssenProject/jans/commit/242496594af8fd5d82960747c53078149a7e1e57))
* the admin-ui backend issues related to jetty 11 migration [#1258](https://github.com/JanssenProject/jans/issues/1258) ([cf94d5f](https://github.com/JanssenProject/jans/commit/cf94d5f56f43b523f3bfd06429992f4705a0a4ae))
* Typo httpLoggingExludePaths jans-auth-server jans-cli jans-config-api jans-linux-setup docker-jans-persistence-loader ([47a20ee](https://github.com/JanssenProject/jans/commit/47a20eefa781d1ca07a9aa30a5adcde3793076d1))
* update api-admin permissions from config api yaml ([#1183](https://github.com/JanssenProject/jans/issues/1183)) ([438c896](https://github.com/JanssenProject/jans/commit/438c8967bbd925779e8ec7b84b9021de32ec409c))
* update templates [#1053](https://github.com/JanssenProject/jans/issues/1053) ([2e33a43](https://github.com/JanssenProject/jans/commit/2e33a43f1d1cc029bcb96992b7bd468956d738fc))
* Use highest level script in case ACR script is not found. Added FF to keep existing behavior. ([#1070](https://github.com/JanssenProject/jans/issues/1070)) ([07473d9](https://github.com/JanssenProject/jans/commit/07473d9a8c3e31f6a75670a874e17341518bf0be))
* use shutil instead of zipfile ([c0a0cde](https://github.com/JanssenProject/jans/commit/c0a0cde87874a73a61bcf87efdfedada1e4f4f10))


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

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/jans-linux-setup-v1.0.0-beta.15...jans-linux-setup-v1.0.0-beta.16) (2022-03-14)


### Features

* **jans-linux-setup:** check availibility of ports for OpenDJ backend ([#949](https://github.com/JanssenProject/jans/issues/949)) ([a2944c1](https://github.com/JanssenProject/jans/commit/a2944c1ee432985c2bb8e8d52c22710ec73f7039))
* **jans-linux-setup:** install mod_auth_openidc (ref: [#909](https://github.com/JanssenProject/jans/issues/909)) ([#952](https://github.com/JanssenProject/jans/issues/952)) ([270a7b6](https://github.com/JanssenProject/jans/commit/270a7b6e1f83f08a2a3caadb2ef1ee36e4233957))
* **jans-linux-setup:** refactored argsp ([#969](https://github.com/JanssenProject/jans/issues/969)) ([409d364](https://github.com/JanssenProject/jans/commit/409d364383a1777ce0c5ef85fc19b432bce6c6d1))
* support regex client attribute to validate redirect uris ([#1005](https://github.com/JanssenProject/jans/issues/1005)) ([a78ee1a](https://github.com/JanssenProject/jans/commit/a78ee1a3cfc4e7a6d08a500750edb5db0f7709a4))


### Bug Fixes

* ** jans-linux-setup:** added to extraClasspath ([#968](https://github.com/JanssenProject/jans/issues/968)) ([bfb0bfe](https://github.com/JanssenProject/jans/commit/bfb0bfe63abdc86a1384badfe15e3d985213001e))
* jans-linux-setup add dependency python3-prompt-toolkit ([#975](https://github.com/JanssenProject/jans/issues/975)) ([2d4a101](https://github.com/JanssenProject/jans/commit/2d4a101defbcddb79d6417f128a553be4c16430c))
* jans-linux-setup flex-setup argsp ([7ee41a7](https://github.com/JanssenProject/jans/commit/7ee41a7a5745d28c9314ce0c84e894c230b9b7ae))
* jans-linux-setup flex-setup argsp ([7ee41a7](https://github.com/JanssenProject/jans/commit/7ee41a7a5745d28c9314ce0c84e894c230b9b7ae))
* jans-linux-setup flex-setup argsp ([9a00e93](https://github.com/JanssenProject/jans/commit/9a00e935f56da1448603e60f55e22dce6740a389))
* jans-linux-setup getting argparser ([#974](https://github.com/JanssenProject/jans/issues/974)) ([5fc60d4](https://github.com/JanssenProject/jans/commit/5fc60d4035d42fc85566c505bfdc9e693da542b4))
* jans-linux-setup remove fido authentication scripts from template ([#991](https://github.com/JanssenProject/jans/issues/991)) ([753ab0c](https://github.com/JanssenProject/jans/commit/753ab0c2b26fae8fc4fee14e268401588ab07a59))
* **jans-linux-setup:** backup cli direcory if any ([#976](https://github.com/JanssenProject/jans/issues/976)) ([dc42d0f](https://github.com/JanssenProject/jans/commit/dc42d0f014514bde69666abaeffcbe79a67888ca))
* **jans-linux-setup:** not copy duo_web.py ([#971](https://github.com/JanssenProject/jans/issues/971)) ([b5691b5](https://github.com/JanssenProject/jans/commit/b5691b51a200c2b9ba778f5a92c0d714d07e3b80))
* **jans-linux-setup:** openbanking argparser issue ([#985](https://github.com/JanssenProject/jans/issues/985)) ([ab40173](https://github.com/JanssenProject/jans/commit/ab40173a8c0cb6ec7e2fa1398561dab5ad2a5abc))
* **jans-linux-setup:** require python3-distutils for deb clones ([#967](https://github.com/JanssenProject/jans/issues/967)) ([9a76f23](https://github.com/JanssenProject/jans/commit/9a76f23e259e2e1b7290285f5ee9a70a66be9b0c))
* **jans-linux-setup:** update suse15 dependency ([#980](https://github.com/JanssenProject/jans/issues/980)) ([3be0ffa](https://github.com/JanssenProject/jans/commit/3be0ffaf09001b97b1e582b6e04bf51cc4bcdbed))


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

* **jans-auth-server:** allow return custom authz params to rp in response ([#756](https://github.com/JanssenProject/jans/issues/756)) ([0e865fb](https://github.com/JanssenProject/jans/commit/0e865fbace7d12634ad57b510a1ad81a9067f01f))
* **jans-auth-server:** set public subject identifier per client ([#800](https://github.com/JanssenProject/jans/issues/800)) ([c303bbc](https://github.com/JanssenProject/jans/commit/c303bbc4c928b32144a657b9c119846ed29cd522))
* **jans-config-api:** config api interception script ([#840](https://github.com/JanssenProject/jans/issues/840)) ([8e4c688](https://github.com/JanssenProject/jans/commit/8e4c68889f9286e68ddd79d05ebd0d1bebd68097))
* **jans-linux-setup:** added config-api interception script (ref: [#831](https://github.com/JanssenProject/jans/issues/831)) ([#882](https://github.com/JanssenProject/jans/issues/882)) ([48a3195](https://github.com/JanssenProject/jans/commit/48a3195addf27ee1fa92b1e901b358ec5304e0ea))
* **jans-linux-setup:** import custom ldif ([#873](https://github.com/JanssenProject/jans/issues/873)) ([363cf0e](https://github.com/JanssenProject/jans/commit/363cf0e63e8d43e360da05a70de2caf1540b1eae))
* **jans-linux-setup:** Jans linux setup pkg mysql and uninstall ([#827](https://github.com/JanssenProject/jans/issues/827)) ([0fb53e1](https://github.com/JanssenProject/jans/commit/0fb53e1efddb65441c25da69e95d60dc70780f8c))
* linux-setup node installer ([662a27f](https://github.com/JanssenProject/jans/commit/662a27f5810bb1cd95105fc9d6c84fd29c178ff3))


### Bug Fixes

* **jans-cli:** jans cli pkg fixes ([#854](https://github.com/JanssenProject/jans/issues/854)) ([9e96e4c](https://github.com/JanssenProject/jans/commit/9e96e4c6b13bc44f4bb2d74222da1669d5b5ed22))
* **jans-cli:** retain scim client in config.ini ([#872](https://github.com/JanssenProject/jans/issues/872)) ([8346517](https://github.com/JanssenProject/jans/commit/83465172bf11ea0a787ee3de34c8dd8968bcdcf0))
* jans-linux-setup config-api default file ([#910](https://github.com/JanssenProject/jans/issues/910)) ([86ff007](https://github.com/JanssenProject/jans/commit/86ff007e68048708f90ace0568363b104fd12420))
* jans-linux-setup openbanking ([1a5f708](https://github.com/JanssenProject/jans/commit/1a5f708864eae78668a2e5069c8918d6488e1dec))
* **jans-linux-setup :** tweak install.py for new directory structure ([#825](https://github.com/JanssenProject/jans/issues/825)) ([493337f](https://github.com/JanssenProject/jans/commit/493337f94045be705386b8f2b59dda29f85762e6))
* jans-linux-setup update config-api scim plugin maven url ([#866](https://github.com/JanssenProject/jans/issues/866)) ([885a06d](https://github.com/JanssenProject/jans/commit/885a06d0be03c72778afaf1a80000e917885a9b9))
* **jans-linux-setup:** added missing values for openbanking ([#913](https://github.com/JanssenProject/jans/issues/913)) ([1977eec](https://github.com/JanssenProject/jans/commit/1977eec5f5231b72dd576f2b85218552026d17ce))
* **jans-linux-setup:** fixed link in readme.md ([edf735e](https://github.com/JanssenProject/jans/commit/edf735ef9de1bcd487932f6aacb854015902eaab))
* **jans-linux-setup:** missing code for platform authenticator (TouchID) ([#792](https://github.com/JanssenProject/jans/issues/792)) ([263b76a](https://github.com/JanssenProject/jans/commit/263b76ad07cf5f6f1c5227a98b46b0ff83f1c9a3))
* **jans-linux-setup:** remove non-utf character from description of attribute 98FC ([#877](https://github.com/JanssenProject/jans/issues/877)) ([321a8e9](https://github.com/JanssenProject/jans/commit/321a8e95460f014bee8eeaaecc083d41b421e4f9))
* **jans-linux-setup:** restore changes after 4babe55a494c0edad899776f086d8c59368031f2 ([#835](https://github.com/JanssenProject/jans/issues/835)) ([c8f4b19](https://github.com/JanssenProject/jans/commit/c8f4b19b08eb1dbf3f90f68707e93437416e2e7f))
* **jans-linux-setup:** service install check for jetty 10 ([#911](https://github.com/JanssenProject/jans/issues/911)) ([a7e57aa](https://github.com/JanssenProject/jans/commit/a7e57aa30268738347ad6844e10191c468572b42))
* **jans-linux-setup:** uninstall does not prompt with -n ([#887](https://github.com/JanssenProject/jans/issues/887)) ([c009dd5](https://github.com/JanssenProject/jans/commit/c009dd521dbfac9e8932859c7554e414b2adf329))
* linux-setup apache config file name ([#719](https://github.com/JanssenProject/jans/issues/719)) ([46ce0ae](https://github.com/JanssenProject/jans/commit/46ce0ae4ca602392b90041c39415ffda7b027029))
* linux-setup mariadb json data types ([#714](https://github.com/JanssenProject/jans/issues/714)) ([4c21be2](https://github.com/JanssenProject/jans/commit/4c21be25abe3101e91365cc2cec1d52f687a824e))
* linux-setup suse httpd configuration ([#734](https://github.com/JanssenProject/jans/issues/734)) ([7767b5e](https://github.com/JanssenProject/jans/commit/7767b5e717a3fd11f7ff54fbec7ad11d6e9df8aa))
* newly added eddsa cause exception ([#727](https://github.com/JanssenProject/jans/issues/727)) ([6e5a865](https://github.com/JanssenProject/jans/commit/6e5a865d6c204240424710be8a496a5b513d647a))
* update wrong import [#905](https://github.com/JanssenProject/jans/issues/905) ([#906](https://github.com/JanssenProject/jans/issues/906)) ([af55a81](https://github.com/JanssenProject/jans/commit/af55a81f784191c1fcee5ff2fade499f778561c3))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))
