# Changelog

## [1.0.22](https://github.com/JanssenProject/jans/compare/docs-v1.0.21...docs-v1.0.22) (2024-01-22)


### Features

* **docs:** ssa lifetime docs updated ([#7172](https://github.com/JanssenProject/jans/issues/7172)) ([be59efe](https://github.com/JanssenProject/jans/commit/be59efe027235157be3ccaec78397163e40bf094))
* **jans-auth-server:** support for OAuth 2.0 Rich Authorization Requests ([#7145](https://github.com/JanssenProject/jans/issues/7145)) ([c7d99c8](https://github.com/JanssenProject/jans/commit/c7d99c81efbaffd31b1b7d2963cd4f77768fd40e))
* use Bearer token if OPA started with it ([#7353](https://github.com/JanssenProject/jans/issues/7353)) ([4c47afd](https://github.com/JanssenProject/jans/commit/4c47afde485406c283abea7c82b1854672bd1124))


### Bug Fixes

* **docs:** add section for schema setup ([#7231](https://github.com/JanssenProject/jans/issues/7231)) ([7d36b24](https://github.com/JanssenProject/jans/commit/7d36b24aba0d3e877503ac76d90f54c0c2cfbc4a))
* **docs:** minor updates to the README ([#7366](https://github.com/JanssenProject/jans/issues/7366)) ([4859057](https://github.com/JanssenProject/jans/commit/4859057bd0bf505d6d96a027962f52de42c7e221))
* **docs:** remove mariadb from docs ([#7232](https://github.com/JanssenProject/jans/issues/7232)) ([6bf4860](https://github.com/JanssenProject/jans/commit/6bf48601ea5ce4743bad0202d0a73aa70db9b379))
* **docs:** remove oxtrust references ([#7390](https://github.com/JanssenProject/jans/issues/7390)) ([018423f](https://github.com/JanssenProject/jans/commit/018423fcad12d2e8c1926baf6bbd12cda56321e2))
* **jans-auth-server:** replace Gluu branding with jans branding ([#7225](https://github.com/JanssenProject/jans/issues/7225)) ([e11f454](https://github.com/JanssenProject/jans/commit/e11f454979eec898c3b62e56f6295e26e6e7351e))
* **kc-saml-plugin:** fixed IDP creation issue, enum values and removed kc lib dependency ([#7238](https://github.com/JanssenProject/jans/issues/7238)) ([d3401e3](https://github.com/JanssenProject/jans/commit/d3401e3b695f21a61c5ddc5675d242a30839ef06))
* prepare for 1.0.22 release ([#7455](https://github.com/JanssenProject/jans/issues/7455)) ([4bf2562](https://github.com/JanssenProject/jans/commit/4bf2562050c86317658259c72bb641780a283579))
* re-engineer how calls work in the engine ([#7152](https://github.com/JanssenProject/jans/issues/7152)) ([a940e7d](https://github.com/JanssenProject/jans/commit/a940e7dbc7da77c9f896dbe7bd73d7f9056231af))

## [1.0.21](https://github.com/JanssenProject/jans/compare/docs-v1.0.20...docs-v1.0.21) (2023-12-14)


### Features

* add support for custom scripts ([#6970](https://github.com/JanssenProject/jans/issues/6970)) ([37b171d](https://github.com/JanssenProject/jans/commit/37b171d69d835573800d3765957c7eb4ce78b43a))
* **agama:** use a mixed strategy for serialization ([#6883](https://github.com/JanssenProject/jans/issues/6883)) ([00aee0c](https://github.com/JanssenProject/jans/commit/00aee0c26565e8b0b574370610a75139c2155568))
* **jans-auth-server:** archived jwks ([#6503](https://github.com/JanssenProject/jans/issues/6503)) ([c86ae0a](https://github.com/JanssenProject/jans/commit/c86ae0a5a703ff96fd1e69fddcc110b5b754ad71))
* **jans-linux-setup:** use reference token for TUI ([#6585](https://github.com/JanssenProject/jans/issues/6585)) ([2918c11](https://github.com/JanssenProject/jans/commit/2918c11a25b50a395c71ad5dc252cf49d319a407))


### Bug Fixes

* **docs:** add more clarity to triage labels ([#6995](https://github.com/JanssenProject/jans/issues/6995)) ([5f93e2f](https://github.com/JanssenProject/jans/commit/5f93e2fb1e5595cc31fc4ab84fe8935c07c06a63))
* **docs:** fix dynamic install docs ([#6913](https://github.com/JanssenProject/jans/issues/6913)) ([2d1d7b9](https://github.com/JanssenProject/jans/commit/2d1d7b92de20cc60d07c2d4748fc232e08cac973))
* **jans-auth-server:** UpdateToken script is not invoked during Implicit Flow [#6561](https://github.com/JanssenProject/jans/issues/6561) ([#6573](https://github.com/JanssenProject/jans/issues/6573)) ([3ca1b24](https://github.com/JanssenProject/jans/commit/3ca1b24ecca8dacc2b9a53e862c49291c5c20c2c))
* **jans-auth:** [#6527](https://github.com/JanssenProject/jans/issues/6527) ([#6532](https://github.com/JanssenProject/jans/issues/6532)) ([87ed3d6](https://github.com/JanssenProject/jans/commit/87ed3d6e65b1c5cafe9a979b43a42c7bbab6dd08))
* prepare for 1.0.21 release ([#7008](https://github.com/JanssenProject/jans/issues/7008)) ([2132de6](https://github.com/JanssenProject/jans/commit/2132de6683f67bf22d5a863b149770d657073a83))

## [1.0.20](https://github.com/JanssenProject/jans/compare/docs-v1.0.19...docs-v1.0.20) (2023-11-08)


### Features

* **jans-auth-server:** added PKCE support to authz challenge endpoint [#6180](https://github.com/JanssenProject/jans/issues/6180) ([#6339](https://github.com/JanssenProject/jans/issues/6339)) ([d9a24bc](https://github.com/JanssenProject/jans/commit/d9a24bc4399f656915923395858ea085ca3dccfa))
* **jans-auth-server:** allow revoke any token - explicitly allow by config and scope [#6381](https://github.com/JanssenProject/jans/issues/6381) ([#6412](https://github.com/JanssenProject/jans/issues/6412)) ([47cbee9](https://github.com/JanssenProject/jans/commit/47cbee9cf917f0f79c53e9e0cfe1e2beab3108bc))
* **jans-auth-server:** multi-step authn challenge flow custom script ([#6275](https://github.com/JanssenProject/jans/issues/6275)) ([b28f1c8](https://github.com/JanssenProject/jans/commit/b28f1c8078a7a8a3358d6d589ba7e7c4585bf15c))
* **jans-pycloudlib:** add lock to prevent race condition ([#6329](https://github.com/JanssenProject/jans/issues/6329)) ([9dd82da](https://github.com/JanssenProject/jans/commit/9dd82da5c87ee829c73a1135ce8740b8353f8ab5))


### Bug Fixes

* **docs:** minor fixes in DPOP document ([#6374](https://github.com/JanssenProject/jans/issues/6374)) ([3431b85](https://github.com/JanssenProject/jans/commit/3431b85b922e7fa985c8201a13b990b3d99f7749))
* **docs:** remove additional Agama title ([#6361](https://github.com/JanssenProject/jans/issues/6361)) ([c2ab212](https://github.com/JanssenProject/jans/commit/c2ab212b78e0fa73bde2d9b694fa44b22675b4ca))
* prepare for 1.0.20 release ([c6e806e](https://github.com/JanssenProject/jans/commit/c6e806eb31fed998d52cbef7a7d94c231d913102))
* registration_uri for super gluu script defaults to an /identity ([#6369](https://github.com/JanssenProject/jans/issues/6369)) ([28c4714](https://github.com/JanssenProject/jans/commit/28c47141a22a1139762655e3ccab6cc34cf7798f)), closes [#6183](https://github.com/JanssenProject/jans/issues/6183)

## [1.0.19](https://github.com/JanssenProject/jans/compare/docs-v1.0.18...docs-v1.0.19) (2023-10-11)


### Features

* **agama:** add support for autoconfiguration  ([#6210](https://github.com/JanssenProject/jans/issues/6210)) ([18f15da](https://github.com/JanssenProject/jans/commit/18f15da64ec4ccf453b03ada92727ef1114a2043))
* **jans-auth-server:** added "authorization_challenge" scope enforcement [#5856](https://github.com/JanssenProject/jans/issues/5856) ([#6216](https://github.com/JanssenProject/jans/issues/6216)) ([b3db5c8](https://github.com/JanssenProject/jans/commit/b3db5c8cba829fc3e6aec350af7c0b4e5cf068c6))
* **jans-auth-server:** added DPoP to authorization code and PAR ([#6196](https://github.com/JanssenProject/jans/issues/6196)) ([be559bf](https://github.com/JanssenProject/jans/commit/be559bfebdf61068a296a05334e731a78a2cc91a))
* **jans-auth-server:** passing custom parameters in the body of POST authorization request and ROPC [#6141](https://github.com/JanssenProject/jans/issues/6141) ([#6148](https://github.com/JanssenProject/jans/issues/6148)) ([00673ae](https://github.com/JanssenProject/jans/commit/00673aea847eb5405d07ef0fbfb341eb0d6cc497))
* **jans-auth:** new lifetime attribute in ssa ([#6214](https://github.com/JanssenProject/jans/issues/6214)) ([b049e33](https://github.com/JanssenProject/jans/commit/b049e334bbe9d0c3b0214694e9fd6501019b8530))


### Bug Fixes

* **docs:** autogenerate docs ([#6261](https://github.com/JanssenProject/jans/issues/6261)) ([57137e4](https://github.com/JanssenProject/jans/commit/57137e446774f0769e54969b4edbc5d03b715298))
* prepare for 1.0.19 release ([554fd43](https://github.com/JanssenProject/jans/commit/554fd434f624c4b4be3b2031c472177709da8966))

## [1.0.18](https://github.com/JanssenProject/jans/compare/docs-v1.0.17...docs-v1.0.18) (2023-09-23)


### Bug Fixes

* prepare for 1.0.18 release ([87af7e4](https://github.com/JanssenProject/jans/commit/87af7e4d41728ce2966362883b47e5354f8c3803))

## [1.0.17](https://github.com/JanssenProject/jans/compare/docs-v1.0.16...docs-v1.0.17) (2023-09-17)


### Features

* add encrypted inum to session params [#6018](https://github.com/JanssenProject/jans/issues/6018) ([#6019](https://github.com/JanssenProject/jans/issues/6019)) ([aabc8a1](https://github.com/JanssenProject/jans/commit/aabc8a14e05004a94443f21714a135fe71658fbd))
* fido2 needs to search cache for session instead of persistent ([#6011](https://github.com/JanssenProject/jans/issues/6011)) ([0cc0c19](https://github.com/JanssenProject/jans/commit/0cc0c192735c0537c28bb7cc96a9db509d9628e0))
* **jans-auth-server:** add client_id parameter support to /end_session [#5942](https://github.com/JanssenProject/jans/issues/5942) ([#6032](https://github.com/JanssenProject/jans/issues/6032)) ([09ee345](https://github.com/JanssenProject/jans/commit/09ee345ae7ed1fd7fc19260e3533e7c2c652b9f1))
* **jans-auth-server:** added "The Use of Attestation in OAuth 2.0 Dynamic Client Registration" spec support [#5562](https://github.com/JanssenProject/jans/issues/5562) ([#5868](https://github.com/JanssenProject/jans/issues/5868)) ([38653c9](https://github.com/JanssenProject/jans/commit/38653c9cb9eb992213c5f230a5f36ce1187d0197))
* **jans-auth-server:** OAuth 2.0 for First-Party Native Applications ([#5654](https://github.com/JanssenProject/jans/issues/5654)) ([9d90e28](https://github.com/JanssenProject/jans/commit/9d90e28791c49bc86771623601c654f2c662b7a1))
* **jans-fido2:** mds optional ([#5409](https://github.com/JanssenProject/jans/issues/5409)) ([fad9961](https://github.com/JanssenProject/jans/commit/fad9961fbeeffb315d6ca495c43f8a4f000eac86))
* **jans-linux-setup:** salt with argument ([#5786](https://github.com/JanssenProject/jans/issues/5786)) ([d433827](https://github.com/JanssenProject/jans/commit/d4338271d910ec66e2788e77049c111046193a95))


### Bug Fixes

* **docs:** add missing interception scripts to the listing ([#5923](https://github.com/JanssenProject/jans/issues/5923)) ([84e39e9](https://github.com/JanssenProject/jans/commit/84e39e913a5fdd2ceb57a531025af9ad9a894bbd))
* **docs:** Adding custom attribute - mysql-ops.md ([#5849](https://github.com/JanssenProject/jans/issues/5849)) ([1b7152a](https://github.com/JanssenProject/jans/commit/1b7152a4fa2421cc6a02cba825bca3b330379ad4))
* **docs:** autogenerate docs ([#6065](https://github.com/JanssenProject/jans/issues/6065)) ([0f3cf5d](https://github.com/JanssenProject/jans/commit/0f3cf5d6c679f02b5a385b72003de2669f2bfb66))
* **docs:** Corrected path [#5810](https://github.com/JanssenProject/jans/issues/5810) ([#5811](https://github.com/JanssenProject/jans/issues/5811)) ([26a4671](https://github.com/JanssenProject/jans/commit/26a46713e4f8f9e2a6a478321d0105bdb65b8137))
* **docs:** initial commit - Update pgsql-ops.md ([#5850](https://github.com/JanssenProject/jans/issues/5850)) ([bd40c5b](https://github.com/JanssenProject/jans/commit/bd40c5b3ed82373225790e3d66ed7f7cd3e04ccb))
* **docs:** mysql operation for custom attr TUI ([#5848](https://github.com/JanssenProject/jans/issues/5848)) ([d1468a1](https://github.com/JanssenProject/jans/commit/d1468a19227aace3090f64ba9e48de6adc5df835))
* **docs:** mysql-operation-1 ([#5846](https://github.com/JanssenProject/jans/issues/5846)) ([f57cdb3](https://github.com/JanssenProject/jans/commit/f57cdb3058fa26fc6fef43a01c2438914d2a4b54))
* **docs:** remove selinux disabling instruction for RHEL Setup ([#5795](https://github.com/JanssenProject/jans/issues/5795)) ([546f0bd](https://github.com/JanssenProject/jans/commit/546f0bd80958ece93be48abad16b2ec9898d8934))
* **docs:** remove tmp file ([#5788](https://github.com/JanssenProject/jans/issues/5788)) ([20fe34b](https://github.com/JanssenProject/jans/commit/20fe34b706cb50f77c080c241b9721fd7d132cea))
* **docs:** removed Agama Developer Studio reference ([#5874](https://github.com/JanssenProject/jans/issues/5874)) ([676a647](https://github.com/JanssenProject/jans/commit/676a6479419a68a8f5a7a043810142553ac3c893))
* **docs:** Update mysql-ops.md ([#5847](https://github.com/JanssenProject/jans/issues/5847)) ([ec3faf6](https://github.com/JanssenProject/jans/commit/ec3faf6c6a7d751a9d0b74f5df5db0bf1dfb64c1))
* **docs:** Update pgsql-ops.md ([#5866](https://github.com/JanssenProject/jans/issues/5866)) ([9d4483c](https://github.com/JanssenProject/jans/commit/9d4483c1172d9a607465b4cdbbebdcfceaf954a7))
* fix typo in SG script ([#5936](https://github.com/JanssenProject/jans/issues/5936)) ([f06b6ca](https://github.com/JanssenProject/jans/commit/f06b6ca458305a47e5aa8f8821fad1d8335f5a20))
* prepare for 1.0.17 release ([4ba8c15](https://github.com/JanssenProject/jans/commit/4ba8c151734f02d762e902b46a35cae2d498fa8f))
* remove pending deployments when exceeding 5 minutes [#5636](https://github.com/JanssenProject/jans/issues/5636) ([#5762](https://github.com/JanssenProject/jans/issues/5762)) ([64ded2c](https://github.com/JanssenProject/jans/commit/64ded2ccccd78fc146f326ab85ba08e5a555a756))

## [1.0.16](https://github.com/JanssenProject/jans/compare/docs-v1.0.15...docs-v1.0.16) (2023-08-02)


### Features

* add dcr load test ([#5566](https://github.com/JanssenProject/jans/issues/5566)) ([0cfd4dd](https://github.com/JanssenProject/jans/commit/0cfd4dda546ce766bb10b852f84f33c884e32c9f))
* add new methnod to fido2 extension to allow modify json ([#5683](https://github.com/JanssenProject/jans/issues/5683)) ([256675b](https://github.com/JanssenProject/jans/commit/256675b2ad9e195ea793eee00257ed400f815a56)), closes [#5680](https://github.com/JanssenProject/jans/issues/5680)
* add new methnod to fido2 extension to allow modify json ([#5686](https://github.com/JanssenProject/jans/issues/5686)) ([6f56e51](https://github.com/JanssenProject/jans/commit/6f56e51706c0e44cd3a9baffa8d2758898b994ba)), closes [#5680](https://github.com/JanssenProject/jans/issues/5680)
* **jans-auth-server:** automatically provision scopes if they are present in the SSA for trusted issuer [#5164](https://github.com/JanssenProject/jans/issues/5164) ([#5553](https://github.com/JanssenProject/jans/issues/5553)) ([abaa10f](https://github.com/JanssenProject/jans/commit/abaa10f785c6318685f7a9d0129bae4a33dc79c4))
* **jans-linux-setup:** gluu/flex casa installer ([#5590](https://github.com/JanssenProject/jans/issues/5590)) ([2ce1152](https://github.com/JanssenProject/jans/commit/2ce11527485cece5dea4c714ae8f03b1b19510b1))


### Bug Fixes

* **docker-jans:** add test client with all scopes to CN-based setup ([#5682](https://github.com/JanssenProject/jans/issues/5682)) ([a81d301](https://github.com/JanssenProject/jans/commit/a81d301651643afce85be7d750897391bd097d33))
* **docs:** autogenerate docs ([#5515](https://github.com/JanssenProject/jans/issues/5515)) ([25f9566](https://github.com/JanssenProject/jans/commit/25f95667422d2eed828fb4afbae378a1cac71f32))
* **docs:** autogenerate docs ([#5749](https://github.com/JanssenProject/jans/issues/5749)) ([9a29ec1](https://github.com/JanssenProject/jans/commit/9a29ec194b80ecbd06a5a9f4ea34434492bb5cd1))
* enlarge column `adsPrjDeplDetails` ([#5644](https://github.com/JanssenProject/jans/issues/5644)) ([ae059fe](https://github.com/JanssenProject/jans/commit/ae059fe0018c3a7a059c2431e63ab0bd90d1f314))
* **jans-linux-setup:** casa install option --with-casa ([#5598](https://github.com/JanssenProject/jans/issues/5598)) ([4758bf5](https://github.com/JanssenProject/jans/commit/4758bf577bd61746c80d63ee8624fa19cdb3aeed))
* prepare for 1.0.16 release ([042ce79](https://github.com/JanssenProject/jans/commit/042ce7941b9597fade8d5f10e40a89d9e7662315))
* prepare for 1.0.16 release ([b2649c3](https://github.com/JanssenProject/jans/commit/b2649c33a9857f356f91df2f38787ec56269e6dd))

## [1.0.15](https://github.com/JanssenProject/jans/compare/docs-v1.0.14...docs-v1.0.15) (2023-07-12)


### Features

* add a prefix to Log statements [#5201](https://github.com/JanssenProject/jans/issues/5201) ([#5475](https://github.com/JanssenProject/jans/issues/5475)) ([ccb3f05](https://github.com/JanssenProject/jans/commit/ccb3f056f29d7262cad31ebb545f814f086fdf83))
* **docs:** added documentation for apple certificates and new fido configuration fields ([#5246](https://github.com/JanssenProject/jans/issues/5246)) ([7599071](https://github.com/JanssenProject/jans/commit/7599071530712d5f439c1d9ef4c48f8d507c134f))


### Bug Fixes

* **doc:** disable-selinux optin in vm-setup ([#5408](https://github.com/JanssenProject/jans/issues/5408)) ([c29834d](https://github.com/JanssenProject/jans/commit/c29834da3ecc5f6558347c9f4cc7cfa631e27015))
* prepare for 1.0.15 release ([0e3cc2f](https://github.com/JanssenProject/jans/commit/0e3cc2f5ea287c2c35f45def54f074daa473ec49))

## [1.0.14](https://github.com/JanssenProject/jans/compare/docs-v1.0.13...docs-v1.0.14) (2023-06-12)


### Features

* add authorization headers needed to access scan API from SG ([#5093](https://github.com/JanssenProject/jans/issues/5093)) ([631abf2](https://github.com/JanssenProject/jans/commit/631abf2f822c9ddd05f962718d0dc70d0f3ceb6f)), closes [#5092](https://github.com/JanssenProject/jans/issues/5092)
* Add DCR flow ([#5096](https://github.com/JanssenProject/jans/issues/5096)) ([4bdea42](https://github.com/JanssenProject/jans/commit/4bdea425bb3d2b174049d03f3664db559e449eb9)), closes [#5092](https://github.com/JanssenProject/jans/issues/5092)
* **agama:** allow flows to supply the identity of the user to authenticate with a parameterizable attribute ([#5010](https://github.com/JanssenProject/jans/issues/5010)) ([ca941ce](https://github.com/JanssenProject/jans/commit/ca941ce0c2b54a84cd0327f8ac21fe926b533660))
* **agama:** update deployer to account project's metadata `noDirectLaunch` ([#5182](https://github.com/JanssenProject/jans/issues/5182)) ([cb4ae38](https://github.com/JanssenProject/jans/commit/cb4ae38779e48a6c63351b444ec305c9cfcb90a9))
* **jans-auth-server:** added ability to set client expiration via DCR [#5057](https://github.com/JanssenProject/jans/issues/5057) ([#5185](https://github.com/JanssenProject/jans/issues/5185)) ([a15054b](https://github.com/JanssenProject/jans/commit/a15054b1c3350d6ee0bb9c92d39f6b2d992abfa1))
* **jans-auth-server:** made not found exceptions logging level configurable [#4973](https://github.com/JanssenProject/jans/issues/4973) ([#4982](https://github.com/JanssenProject/jans/issues/4982)) ([98be22b](https://github.com/JanssenProject/jans/commit/98be22b81d365b631d2b7ffcf76d1f3a5ea1935b))
* **jans-auth-server:** Support of Select Account interception script [#3452](https://github.com/JanssenProject/jans/issues/3452) ([#5149](https://github.com/JanssenProject/jans/issues/5149)) ([b062148](https://github.com/JanssenProject/jans/commit/b062148b7395e2828432061363058d7e1a9dd6db))
* move notify-client2 library to fido2 project [#5030](https://github.com/JanssenProject/jans/issues/5030) ([#5031](https://github.com/JanssenProject/jans/issues/5031)) ([ed5e09e](https://github.com/JanssenProject/jans/commit/ed5e09eff23dbea45e026728886d1e95f3e5cd95))
* update SG script and notify client to conform scan API [#5061](https://github.com/JanssenProject/jans/issues/5061) ([#5062](https://github.com/JanssenProject/jans/issues/5062)) ([7afc42b](https://github.com/JanssenProject/jans/commit/7afc42b2ec00d35cb980d35f286289de2bdadff2))
* update SG script to conform prod server ([#5103](https://github.com/JanssenProject/jans/issues/5103)) ([0ec3ca8](https://github.com/JanssenProject/jans/commit/0ec3ca8b0e4e8e7287c8041dc75be9b29632da81))


### Bug Fixes

* [#5084](https://github.com/JanssenProject/jans/issues/5084) ([#5086](https://github.com/JanssenProject/jans/issues/5086)) ([a0336a8](https://github.com/JanssenProject/jans/commit/a0336a8d696048004af34eb1069af9d55e8f2c32))
* **docker-jans-loadtesting-jmeter:** errors running flow executor ([#5001](https://github.com/JanssenProject/jans/issues/5001)) ([79c1948](https://github.com/JanssenProject/jans/commit/79c1948a12024a3371427da42909717f6f672e8c))
* **docs:** [#3675](https://github.com/JanssenProject/jans/issues/3675) ([#5068](https://github.com/JanssenProject/jans/issues/5068)) ([2aa76b2](https://github.com/JanssenProject/jans/commit/2aa76b2c90e15524299716bedf2a68d427deda62))
* **docs:** [#4707](https://github.com/JanssenProject/jans/issues/4707) ([#5090](https://github.com/JanssenProject/jans/issues/5090)) ([f93c750](https://github.com/JanssenProject/jans/commit/f93c75022432717a75821820d264cade8e9223d2))
* **docs:** [#4881](https://github.com/JanssenProject/jans/issues/4881) improvised ([#5035](https://github.com/JanssenProject/jans/issues/5035)) ([065edad](https://github.com/JanssenProject/jans/commit/065edade0bb1d96391b4c894498b8d7ec5f0ad79))
* **docs:** autogenerate docs ([#5225](https://github.com/JanssenProject/jans/issues/5225)) ([9c8e510](https://github.com/JanssenProject/jans/commit/9c8e510e6571362009b4ca422ab946ba711e0122))
* **docs:** Mention of Update Token script [#3465](https://github.com/JanssenProject/jans/issues/3465) ([#5069](https://github.com/JanssenProject/jans/issues/5069)) ([a3efe9b](https://github.com/JanssenProject/jans/commit/a3efe9b3dc577a889ecb1df09f0ec355f7d434c3))
* **docs:** minor addition [#4881](https://github.com/JanssenProject/jans/issues/4881) ([#5083](https://github.com/JanssenProject/jans/issues/5083)) ([948b9aa](https://github.com/JanssenProject/jans/commit/948b9aa0c2b562d20296921e511396f3e820a6ae))
* **docs:** renamed SampleScript.py to PersistenceExtension.py ([#5087](https://github.com/JanssenProject/jans/issues/5087)) ([9ed9c2e](https://github.com/JanssenProject/jans/commit/9ed9c2ebbd53af38a9f974c352abefb822cdd614))
* **jans-auth-server:** Illegal op_policy_uri parameter: - exclude entries with blank values from discovery response (oxauth counterpart) [#4888](https://github.com/JanssenProject/jans/issues/4888) ([#4934](https://github.com/JanssenProject/jans/issues/4934)) ([8603290](https://github.com/JanssenProject/jans/commit/8603290cee37c609f9572760c8cf299aba80160e))
* prepare for 1.0.14 release ([25ccadf](https://github.com/JanssenProject/jans/commit/25ccadf85327ea14685c6066dc6609919e4f2865))

## [1.0.13](https://github.com/JanssenProject/jans/compare/docs-v1.0.12...docs-v1.0.13) (2023-05-10)


### Features

* **jans-auth-server:** add "introspection" scope check on introspection endpoint access [#4557](https://github.com/JanssenProject/jans/issues/4557) ([#4716](https://github.com/JanssenProject/jans/issues/4716)) ([ce2d75c](https://github.com/JanssenProject/jans/commit/ce2d75c32df382eb2a28f89793778a3e72659700))
* **jans-auth-server:** log httpresponse body configurated by httpLoggingResponseBodyContent [#349](https://github.com/JanssenProject/jans/issues/349) ([#4417](https://github.com/JanssenProject/jans/issues/4417)) ([08d92b3](https://github.com/JanssenProject/jans/commit/08d92b303aa1d1733b2e51d258b0a09a21df6677))
* **jans-fido2:** interception scripts issue 1485, swagger updates ([#4543](https://github.com/JanssenProject/jans/issues/4543)) ([80274ff](https://github.com/JanssenProject/jans/commit/80274ffd1a20318988d9cc99ee015c5c7d5984b7))


### Bug Fixes

* **docker-jans-loadtesting-jmeter:** rename incorrect reference to OCI image ([#4908](https://github.com/JanssenProject/jans/issues/4908)) ([7db2c11](https://github.com/JanssenProject/jans/commit/7db2c11c8335a35873c08387060454e8eb30d8e2))
* **docs:** autogenerate docs ([#4933](https://github.com/JanssenProject/jans/issues/4933)) ([337239b](https://github.com/JanssenProject/jans/commit/337239ba8ae301a83eec58048a3f5141be54c8e6))
* **docs:** Jans cli tui update tui navigation docs ([#4767](https://github.com/JanssenProject/jans/issues/4767)) ([a8b055c](https://github.com/JanssenProject/jans/commit/a8b055cf80988d7918d99cdc45706cdde609b022))
* **jans-cli-tui:** f4 to close dialog ([#4736](https://github.com/JanssenProject/jans/issues/4736)) ([2f2d094](https://github.com/JanssenProject/jans/commit/2f2d094409427dea18526d44ae0c65df98473bbb))
* **jans-fido2:** interception script documentation ([#4751](https://github.com/JanssenProject/jans/issues/4751)) ([3b25801](https://github.com/JanssenProject/jans/commit/3b258017ed08b798f95fdb138ec4914aff6f6482))
* prepare for 1.0.13 release ([493478e](https://github.com/JanssenProject/jans/commit/493478e71f6231553c998b48c0f163c7f5869da4))

## [1.0.12](https://github.com/JanssenProject/jans/compare/docs-v1.0.11...docs-v1.0.12) (2023-04-18)


### Features

* **jans-auth-server:** redirect back to RP when session is expired or if not possible show error page [#4449](https://github.com/JanssenProject/jans/issues/4449) ([#4505](https://github.com/JanssenProject/jans/issues/4505)) ([0983e73](https://github.com/JanssenProject/jans/commit/0983e7397ea2aa99423e5e928690666cd67ca8b2))


### Bug Fixes

* adjust bleeding edge images ([4bb0b09](https://github.com/JanssenProject/jans/commit/4bb0b0983984314a51e7b43e547027d5226c84bb))
* **docs:** [#2487](https://github.com/JanssenProject/jans/issues/2487) ([#4624](https://github.com/JanssenProject/jans/issues/4624)) ([ba7c347](https://github.com/JanssenProject/jans/commit/ba7c3478a52d6d8e8dbd542fedbf3a2e44fc8da9))
* **docs:** [#3340](https://github.com/JanssenProject/jans/issues/3340) mention push notification server ([#4622](https://github.com/JanssenProject/jans/issues/4622)) ([7539857](https://github.com/JanssenProject/jans/commit/75398578b4a53c4bfa63e36fa8539fa5fc930b1b))
* **docs:** autogenerate docs ([#4652](https://github.com/JanssenProject/jans/issues/4652)) ([e353874](https://github.com/JanssenProject/jans/commit/e35387414ba9a4610a4f5a5e690fb0e26efdacdb))
* **docs:** Paraphrasing [#4369](https://github.com/JanssenProject/jans/issues/4369) ([#4512](https://github.com/JanssenProject/jans/issues/4512)) ([57e027e](https://github.com/JanssenProject/jans/commit/57e027ec73212c89e71779f4e0f1236b751de5fd))
* **docs:** session persistence [#3235](https://github.com/JanssenProject/jans/issues/3235) ([#4616](https://github.com/JanssenProject/jans/issues/4616)) ([0575c9f](https://github.com/JanssenProject/jans/commit/0575c9f6b6e2be3dcca1eb5bce6fa972dd3cf104))
* **jans-auth:** [#3340](https://github.com/JanssenProject/jans/issues/3340) incorrect folder location ([#4623](https://github.com/JanssenProject/jans/issues/4623)) ([ef0dbb4](https://github.com/JanssenProject/jans/commit/ef0dbb40b34c531d9ae5f33fb41e5bdecfda9524))
* **jans-config-api:** agama deployment detail endpoint not including all flows IDs ([#4565](https://github.com/JanssenProject/jans/issues/4565)) ([358c494](https://github.com/JanssenProject/jans/commit/358c49409a172d6419382dd800a21b845a8cc708))
* prepare for 1.0.12 release ([6f83197](https://github.com/JanssenProject/jans/commit/6f83197705511c39413456acdc64e9136a97ff39))

## [1.0.11](https://github.com/JanssenProject/jans/compare/docs-v1.0.10...docs-v1.0.11) (2023-04-05)


### Features

* **agama:** add a "default agama flow" to bridge ([#4309](https://github.com/JanssenProject/jans/issues/4309)) ([3b2248f](https://github.com/JanssenProject/jans/commit/3b2248fdb2a8e842cde1baca81132fa47613c356))
* **agama:** add means to selectively prevent flow crash when a subflow crashes ([#4436](https://github.com/JanssenProject/jans/issues/4436)) ([5d8f0ad](https://github.com/JanssenProject/jans/commit/5d8f0ad2d74f7d39a5eb4b79e807f175393959b5))
* **jans-auth-server:** added configurable acr to Device Flow [#4305](https://github.com/JanssenProject/jans/issues/4305) ([#4424](https://github.com/JanssenProject/jans/issues/4424)) ([fbd4ede](https://github.com/JanssenProject/jans/commit/fbd4edeaf7d5cb32b03653d4f2c944d41389407d))
* **jans-auth-server:** align JWT Response for OAuth Token Introspection with spec [#3240](https://github.com/JanssenProject/jans/issues/3240) ([#4151](https://github.com/JanssenProject/jans/issues/4151)) ([02e1595](https://github.com/JanssenProject/jans/commit/02e159516d9d41cfe3d81d13983256066f6e647d))
* **jans-auth-server:** increase sessionIdUnauthenticatedUnusedLifetime value in setup [#4445](https://github.com/JanssenProject/jans/issues/4445) ([#4446](https://github.com/JanssenProject/jans/issues/4446)) ([ecf9395](https://github.com/JanssenProject/jans/commit/ecf93955f391bcda17ad6a2f6ead00d79afee165))


### Bug Fixes

* **docs:** autogenerate docs ([#4486](https://github.com/JanssenProject/jans/issues/4486)) ([a9b3eab](https://github.com/JanssenProject/jans/commit/a9b3eabf749cc5dde98c12ffa1b9a1bb9a8091f6))
* **docs:** jans TUI configuration -- update user password ([#4435](https://github.com/JanssenProject/jans/issues/4435)) ([c34b156](https://github.com/JanssenProject/jans/commit/c34b156133ba314e6f04c290deedeb2fb4d2e59f))
* **docs:** minor ([#4245](https://github.com/JanssenProject/jans/issues/4245)) ([65f5944](https://github.com/JanssenProject/jans/commit/65f59447c569b418ba55b943dd60d01495e538ba))
* **docs:** post-install setup [#3340](https://github.com/JanssenProject/jans/issues/3340) ([#4327](https://github.com/JanssenProject/jans/issues/4327)) ([c214681](https://github.com/JanssenProject/jans/commit/c2146818fcd5d316e7a2e634bd90d9a27a55d8ea))
* **docs:** temporary record incase of one_step [#3340](https://github.com/JanssenProject/jans/issues/3340) ([#4326](https://github.com/JanssenProject/jans/issues/4326)) ([921fd20](https://github.com/JanssenProject/jans/commit/921fd20df7397a78253c5c9a3e46f2dde55d7d51))
* prepare for  release ([60775c0](https://github.com/JanssenProject/jans/commit/60775c09dc5ab9996bf80c03dcb457861d48dfb1))
* SG endpoint moved inside the fido2 server ([#4321](https://github.com/JanssenProject/jans/issues/4321)) ([dacb0fa](https://github.com/JanssenProject/jans/commit/dacb0faea5cd485b5cd01abb5c5680856e747daa))
* update push part in SG script to conform Jans config and API ([#4345](https://github.com/JanssenProject/jans/issues/4345)) ([e1cb416](https://github.com/JanssenProject/jans/commit/e1cb416080c6de9ad3f009f4cce8c9e812d73164))

## [1.0.10](https://github.com/JanssenProject/jans/compare/docs-v1.0.9...docs-v1.0.10) (2023-03-16)


### Features

* **jans-auth-server:** added online_access scope to issue session bound refresh token [#3012](https://github.com/JanssenProject/jans/issues/3012) ([#4106](https://github.com/JanssenProject/jans/issues/4106)) ([635f611](https://github.com/JanssenProject/jans/commit/635f6119fdf4cdf3b3aed33515854ef68257c98f))
* **jans-linux-setup:** enable agama engine by default  ([#4131](https://github.com/JanssenProject/jans/issues/4131)) ([7e432dc](https://github.com/JanssenProject/jans/commit/7e432dcde57657d1cfa1cd45bde2206156dc6905))


### Bug Fixes

* **docs:** autogenerate docs ([#4200](https://github.com/JanssenProject/jans/issues/4200)) ([e20f399](https://github.com/JanssenProject/jans/commit/e20f399249055d7b0a65f2c807867c0678e0c787))
* formating issues ([#4119](https://github.com/JanssenProject/jans/issues/4119)) ([c5b89ce](https://github.com/JanssenProject/jans/commit/c5b89ce892ddfd6cf5d7948604d71eadcee73abf))
* prepare release for 1.0.10 ([e996926](https://github.com/JanssenProject/jans/commit/e99692692ef04d881468d120f7c7d462568dce36))

## [1.0.9](https://github.com/JanssenProject/jans/compare/docs-v1.0.8...docs-v1.0.9) (2023-03-09)


### Features

* **agama:** update gama deployment endpoint to support configuration properties ([#4049](https://github.com/JanssenProject/jans/issues/4049)) ([392525c](https://github.com/JanssenProject/jans/commit/392525c19152fcd916e0c61e70c436a484bf391c))


### Bug Fixes

* **docs:** autogenerate docs ([#4050](https://github.com/JanssenProject/jans/issues/4050)) ([dcbb645](https://github.com/JanssenProject/jans/commit/dcbb64548cc5be5609f27371220406ab1585ff36))
* **docs:** autogenerate docs ([#4105](https://github.com/JanssenProject/jans/issues/4105)) ([da87cef](https://github.com/JanssenProject/jans/commit/da87cef4efd88796260d123054575c3aceb1ed38))
* prepare 1.0.9 release ([55f7e0c](https://github.com/JanssenProject/jans/commit/55f7e0c308b869c2c4b5668aca751d022138a678))
* update next SNAPSHOT and dev ([0df0e7a](https://github.com/JanssenProject/jans/commit/0df0e7ae06af64ac477955119c2522f03e0603c3))

## [1.0.8](https://github.com/JanssenProject/jans/compare/docs-v1.0.7...docs-v1.0.8) (2023-03-01)


### Features

* add to AS session the data passed in `Finish` ([#3978](https://github.com/JanssenProject/jans/issues/3978)) ([12bedb7](https://github.com/JanssenProject/jans/commit/12bedb756ae978678a77ceabfdc2879b6f9c1429))
* Include additional attributes on SSA Get endpoint ([#3983](https://github.com/JanssenProject/jans/issues/3983)) ([4fded3e](https://github.com/JanssenProject/jans/commit/4fded3e0ca337bf51176699c7699a7d93bd6d665))

## 1.0.7 (2023-02-22)


### Features

* add benchmark demo ([#3325](https://github.com/JanssenProject/jans/issues/3325)) ([26bbb0c](https://github.com/JanssenProject/jans/commit/26bbb0ca2ef9ec5ac72f80ee3641d222036d55b2))
* add custom Github External Authenticator script for ADS [#3625](https://github.com/JanssenProject/jans/issues/3625) ([#3626](https://github.com/JanssenProject/jans/issues/3626)) ([f922a7a](https://github.com/JanssenProject/jans/commit/f922a7a7b075a43750dd792a91a11399517dbb9b))
* added custom resource owner password script fro two-factor twilio authentication ([#3208](https://github.com/JanssenProject/jans/issues/3208)) ([eae0ca1](https://github.com/JanssenProject/jans/commit/eae0ca1704da961de84e7a7ce665a7c3b0bb3567))
* Change org_id to String type and Add status in get SSA ([#3763](https://github.com/JanssenProject/jans/issues/3763)) ([d01269a](https://github.com/JanssenProject/jans/commit/d01269aa6f51ec9f028da53962d9beaf1cf8a3f9))
* **docs:** jans TUI SCIM configuration -- screenshot ([#3318](https://github.com/JanssenProject/jans/issues/3318)) ([7b463b0](https://github.com/JanssenProject/jans/commit/7b463b01ec36153948110a59010fb9a4b347eae9))
* **docs:** jans TUI SCIM configuration feature - screenshot1 ([#3306](https://github.com/JanssenProject/jans/issues/3306)) ([d1adc98](https://github.com/JanssenProject/jans/commit/d1adc9826dee5bfaf4e36c6a080684e803d869ce))
* **docs:** jans TUI SCIM configuration feature ([#3305](https://github.com/JanssenProject/jans/issues/3305)) ([70e358e](https://github.com/JanssenProject/jans/commit/70e358e49949cec50a1784dc229678451b95c424))
* **jans-auth-server:** added flexible date formatter handler to AS (required by certification tools) [#3600](https://github.com/JanssenProject/jans/issues/3600) ([#3601](https://github.com/JanssenProject/jans/issues/3601)) ([f646d73](https://github.com/JanssenProject/jans/commit/f646d734d79f9da83cfe51103811efd1f8677d7f))
* **jans-auth-server:** renamed "key_ops" -&gt; "key_ops_type" [#3790](https://github.com/JanssenProject/jans/issues/3790) ([#3792](https://github.com/JanssenProject/jans/issues/3792)) ([7a6bcba](https://github.com/JanssenProject/jans/commit/7a6bcba5ca3597f7556d406e4a572c76a229bbdf))
* process lib directory in `.gama` files for ADS projects deployment ([#3644](https://github.com/JanssenProject/jans/issues/3644)) ([40268ad](https://github.com/JanssenProject/jans/commit/40268adda27ab2929115e3e2117d43fed499a2ce))
* Support Super Gluu one step authentication to Fido2 server [#3593](https://github.com/JanssenProject/jans/issues/3593) ([#3599](https://github.com/JanssenProject/jans/issues/3599)) ([c013b16](https://github.com/JanssenProject/jans/commit/c013b161f2eb47f5952cbb80c8740f8d62d302c3))


### Bug Fixes

* [#2201](https://github.com/JanssenProject/jans/issues/2201) ([#3365](https://github.com/JanssenProject/jans/issues/3365)) ([ebca16b](https://github.com/JanssenProject/jans/commit/ebca16bc8dae14b86582ed584292eb610efd0621))
* [#2201](https://github.com/JanssenProject/jans/issues/2201) ([#3451](https://github.com/JanssenProject/jans/issues/3451)) ([0417c2a](https://github.com/JanssenProject/jans/commit/0417c2a641a810dacee6fcd3dfc2a0d71eb32142))
* add link to api reference ([#3394](https://github.com/JanssenProject/jans/issues/3394)) ([f091045](https://github.com/JanssenProject/jans/commit/f0910452b33f24dc220d5123c853f11326a920df))
* docs/requirements.txt to reduce vulnerabilities ([#3523](https://github.com/JanssenProject/jans/issues/3523)) ([82efd8f](https://github.com/JanssenProject/jans/commit/82efd8f503bc2966483f07fff2a77f5c1321c7a2))
* **docs:** jans logging configuration - VM Operation Guide - 1 ([#3348](https://github.com/JanssenProject/jans/issues/3348)) ([e0f8c71](https://github.com/JanssenProject/jans/commit/e0f8c7120e8074ae54ff3abb9b4e654cdde64e44))
* **docs:** jans TUI administration -- Config Guide - TUI -- Auth server ([#3227](https://github.com/JanssenProject/jans/issues/3227)) ([16ab709](https://github.com/JanssenProject/jans/commit/16ab709cca92620a409611521885c14029b8ba0d))
* **docs:** jans TUI configuration -- Auth Server - TUI - image ([#3237](https://github.com/JanssenProject/jans/issues/3237)) ([3fbc9e7](https://github.com/JanssenProject/jans/commit/3fbc9e7d08ff0c9ef55f009dea77e675d652ee7e))
* **docs:** jans TUI configuration -- Config Guide - Auth Server - Client configuration - TUI ([#3233](https://github.com/JanssenProject/jans/issues/3233)) ([ee8e056](https://github.com/JanssenProject/jans/commit/ee8e0564a1eeadaee12ab80078c3802822ab4d1a))
* **docs:** jans TUI configuration -- Fido - TUI ([#3251](https://github.com/JanssenProject/jans/issues/3251)) ([cd6eef1](https://github.com/JanssenProject/jans/commit/cd6eef1503375c39468d82263e0e342b3aace5be))
* **docs:** jans TUI configuration -- Fido Administration - TUI ([#3252](https://github.com/JanssenProject/jans/issues/3252)) ([a371cda](https://github.com/JanssenProject/jans/commit/a371cda85da66a956ca2d0a15200a6c6d2c29ca0))
* **docs:** jans TUI installation -- Config Guide - TUI ([#3224](https://github.com/JanssenProject/jans/issues/3224)) ([cc00a71](https://github.com/JanssenProject/jans/commit/cc00a71af6b82ca969b62a835a24733041ac676d))
* **docs:** missing single quotes ([#3239](https://github.com/JanssenProject/jans/issues/3239)) ([9f38c6a](https://github.com/JanssenProject/jans/commit/9f38c6a216a0dd4afdbec4080f312180ab7858b3))
* hash ([cdb5204](https://github.com/JanssenProject/jans/commit/cdb52047c5847e2eafbaf2f7692211e72b8fde12))
* hash ([156fb2f](https://github.com/JanssenProject/jans/commit/156fb2f697dd4f292659ad9963e7044a7137a583))
* **jans-auth-server:** jansApp attribute only relevant for SG ([#3782](https://github.com/JanssenProject/jans/issues/3782)) ([6153a13](https://github.com/JanssenProject/jans/commit/6153a139d584e69088f8d9202ce072ae10a2dc73))
* minor ([#3334](https://github.com/JanssenProject/jans/issues/3334)) ([3225455](https://github.com/JanssenProject/jans/commit/32254553a7bb5c58f265f29c3613ecc8f81f44b8))
* prepare 1.0.7 release ([ce02fd9](https://github.com/JanssenProject/jans/commit/ce02fd9322ab49d5bea4f6e88f316f931e9d2169))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))
* rename role_based_client as tui_client ([#3657](https://github.com/JanssenProject/jans/issues/3657)) ([c393cb2](https://github.com/JanssenProject/jans/commit/c393cb2052f7e73cc6a02b84bbc673bcc93dc13d))

## 1.0.6 (2023-01-09)


### Features

* add benchmark demo ([#3325](https://github.com/JanssenProject/jans/issues/3325)) ([26bbb0c](https://github.com/JanssenProject/jans/commit/26bbb0ca2ef9ec5ac72f80ee3641d222036d55b2))
* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))
* add feature to include custom-claims in user-info endpoint of admin-ui plugin [#2969](https://github.com/JanssenProject/jans/issues/2969) ([#2970](https://github.com/JanssenProject/jans/issues/2970)) ([0549879](https://github.com/JanssenProject/jans/commit/054987944d91fe2021092c30b337e880421533ff))
* added custom resource owner password script fro two-factor twilio authentication ([#3208](https://github.com/JanssenProject/jans/issues/3208)) ([eae0ca1](https://github.com/JanssenProject/jans/commit/eae0ca1704da961de84e7a7ce665a7c3b0bb3567))
* **docs:** jans TUI SCIM configuration -- screenshot ([#3318](https://github.com/JanssenProject/jans/issues/3318)) ([7b463b0](https://github.com/JanssenProject/jans/commit/7b463b01ec36153948110a59010fb9a4b347eae9))
* **docs:** jans TUI SCIM configuration feature - screenshot1 ([#3306](https://github.com/JanssenProject/jans/issues/3306)) ([d1adc98](https://github.com/JanssenProject/jans/commit/d1adc9826dee5bfaf4e36c6a080684e803d869ce))
* **docs:** jans TUI SCIM configuration feature ([#3305](https://github.com/JanssenProject/jans/issues/3305)) ([70e358e](https://github.com/JanssenProject/jans/commit/70e358e49949cec50a1784dc229678451b95c424))
* documentation for ssa and remove softwareRoles query param of get ssa ([#3031](https://github.com/JanssenProject/jans/issues/3031)) ([d8e14eb](https://github.com/JanssenProject/jans/commit/d8e14ebbeee357c8c2c31808243cf82933ae4a9b))
* **jans-auth-server:** draft for - improve dcr / ssa validation for dynamic  registration [#2980](https://github.com/JanssenProject/jans/issues/2980) ([#3109](https://github.com/JanssenProject/jans/issues/3109)) ([233a78c](https://github.com/JanssenProject/jans/commit/233a78c8e48fb8de353629bc16fc6af1d80fb910))
* **jans-auth-server:** specify minimum acr for clients [#343](https://github.com/JanssenProject/jans/issues/343) ([#3083](https://github.com/JanssenProject/jans/issues/3083)) ([b0034ec](https://github.com/JanssenProject/jans/commit/b0034ec509ace1a4e30a7e9c6dd23dca48178c62))
* jans-linux-setup include permission of all user roles ([#3009](https://github.com/JanssenProject/jans/issues/3009)) ([62a421d](https://github.com/JanssenProject/jans/commit/62a421df821067432cbcced0e89cc2a410cd40be))


### Bug Fixes

* [#2201](https://github.com/JanssenProject/jans/issues/2201) ([#3365](https://github.com/JanssenProject/jans/issues/3365)) ([ebca16b](https://github.com/JanssenProject/jans/commit/ebca16bc8dae14b86582ed584292eb610efd0621))
* [#2201](https://github.com/JanssenProject/jans/issues/2201) ([#3451](https://github.com/JanssenProject/jans/issues/3451)) ([0417c2a](https://github.com/JanssenProject/jans/commit/0417c2a641a810dacee6fcd3dfc2a0d71eb32142))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - irrelevant folder, agama script is in agama-bridge ([#2993](https://github.com/JanssenProject/jans/issues/2993)) ([d19b13a](https://github.com/JanssenProject/jans/commit/d19b13ab31e1dbfee9288c6569b289eae213b528))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - SIWA and SIWG (Sign in with Apple-Google), moved to script-catalog ([#2983](https://github.com/JanssenProject/jans/issues/2983)) ([402e7ae](https://github.com/JanssenProject/jans/commit/402e7aebd20322ef465a3805d3834c7174bc9bbc))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) removing duplicated files ([#3007](https://github.com/JanssenProject/jans/issues/3007)) ([9f3d051](https://github.com/JanssenProject/jans/commit/9f3d051308e7b29e5f112e74601aa05c42ed559c))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) scripts-catalog folder restructuring ([#2999](https://github.com/JanssenProject/jans/issues/2999)) ([7b66f2b](https://github.com/JanssenProject/jans/commit/7b66f2b27517ba560555f64d0ab4e49f10ddb374))
* [#2666](https://github.com/JanssenProject/jans/issues/2666) ([#3011](https://github.com/JanssenProject/jans/issues/3011)) ([f98cbc5](https://github.com/JanssenProject/jans/commit/f98cbc5d43b12b56b81debd367fcfdfdc75830e4))
* [#2825](https://github.com/JanssenProject/jans/issues/2825) ([#2828](https://github.com/JanssenProject/jans/issues/2828)) ([5ce21aa](https://github.com/JanssenProject/jans/commit/5ce21aac5df54d2fe9402479fd221fffe9dc77ef))
* add link to api reference ([#3394](https://github.com/JanssenProject/jans/issues/3394)) ([f091045](https://github.com/JanssenProject/jans/commit/f0910452b33f24dc220d5123c853f11326a920df))
* docs/requirements.txt to reduce vulnerabilities ([#3523](https://github.com/JanssenProject/jans/issues/3523)) ([82efd8f](https://github.com/JanssenProject/jans/commit/82efd8f503bc2966483f07fff2a77f5c1321c7a2))
* **docs:** jans logging configuration - VM Operation Guide - 1 ([#3348](https://github.com/JanssenProject/jans/issues/3348)) ([e0f8c71](https://github.com/JanssenProject/jans/commit/e0f8c7120e8074ae54ff3abb9b4e654cdde64e44))
* **docs:** jans TUI administration -- Config Guide - TUI -- Auth server ([#3227](https://github.com/JanssenProject/jans/issues/3227)) ([16ab709](https://github.com/JanssenProject/jans/commit/16ab709cca92620a409611521885c14029b8ba0d))
* **docs:** jans TUI configuration -- Auth Server - TUI - image ([#3237](https://github.com/JanssenProject/jans/issues/3237)) ([3fbc9e7](https://github.com/JanssenProject/jans/commit/3fbc9e7d08ff0c9ef55f009dea77e675d652ee7e))
* **docs:** jans TUI configuration -- Config Guide - Auth Server - Client configuration - TUI ([#3233](https://github.com/JanssenProject/jans/issues/3233)) ([ee8e056](https://github.com/JanssenProject/jans/commit/ee8e0564a1eeadaee12ab80078c3802822ab4d1a))
* **docs:** jans TUI configuration -- Fido - TUI ([#3251](https://github.com/JanssenProject/jans/issues/3251)) ([cd6eef1](https://github.com/JanssenProject/jans/commit/cd6eef1503375c39468d82263e0e342b3aace5be))
* **docs:** jans TUI configuration -- Fido Administration - TUI ([#3252](https://github.com/JanssenProject/jans/issues/3252)) ([a371cda](https://github.com/JanssenProject/jans/commit/a371cda85da66a956ca2d0a15200a6c6d2c29ca0))
* **docs:** jans TUI installation -- Config Guide - TUI ([#3224](https://github.com/JanssenProject/jans/issues/3224)) ([cc00a71](https://github.com/JanssenProject/jans/commit/cc00a71af6b82ca969b62a835a24733041ac676d))
* **docs:** missing single quotes ([#3239](https://github.com/JanssenProject/jans/issues/3239)) ([9f38c6a](https://github.com/JanssenProject/jans/commit/9f38c6a216a0dd4afdbec4080f312180ab7858b3))
* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* hash ([cdb5204](https://github.com/JanssenProject/jans/commit/cdb52047c5847e2eafbaf2f7692211e72b8fde12))
* hash ([156fb2f](https://github.com/JanssenProject/jans/commit/156fb2f697dd4f292659ad9963e7044a7137a583))
* **jans-fido2:** [#2840](https://github.com/JanssenProject/jans/issues/2840) ([#2974](https://github.com/JanssenProject/jans/issues/2974)) ([d3351e1](https://github.com/JanssenProject/jans/commit/d3351e141fa546074ad98891e655247a0c23e30a))
* minor ([#2786](https://github.com/JanssenProject/jans/issues/2786)) ([3f67763](https://github.com/JanssenProject/jans/commit/3f677636cc2f871e5a9c683634334578405f18f3))
* minor ([#3334](https://github.com/JanssenProject/jans/issues/3334)) ([3225455](https://github.com/JanssenProject/jans/commit/32254553a7bb5c58f265f29c3613ecc8f81f44b8))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))
* typo ([#2950](https://github.com/JanssenProject/jans/issues/2950)) ([6df810b](https://github.com/JanssenProject/jans/commit/6df810b36c68303f458f694e4ba6ec9c3768364c))

## [1.0.5](https://github.com/JanssenProject/jans/compare/docs-v1.0.4...docs-v1.0.5) (2022-12-01)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))
* add feature to include custom-claims in user-info endpoint of admin-ui plugin [#2969](https://github.com/JanssenProject/jans/issues/2969) ([#2970](https://github.com/JanssenProject/jans/issues/2970)) ([0549879](https://github.com/JanssenProject/jans/commit/054987944d91fe2021092c30b337e880421533ff))
* documentation for ssa and remove softwareRoles query param of get ssa ([#3031](https://github.com/JanssenProject/jans/issues/3031)) ([d8e14eb](https://github.com/JanssenProject/jans/commit/d8e14ebbeee357c8c2c31808243cf82933ae4a9b))
* **jans-auth-server:** specify minimum acr for clients [#343](https://github.com/JanssenProject/jans/issues/343) ([#3083](https://github.com/JanssenProject/jans/issues/3083)) ([b0034ec](https://github.com/JanssenProject/jans/commit/b0034ec509ace1a4e30a7e9c6dd23dca48178c62))
* jans-linux-setup include permission of all user roles ([#3009](https://github.com/JanssenProject/jans/issues/3009)) ([62a421d](https://github.com/JanssenProject/jans/commit/62a421df821067432cbcced0e89cc2a410cd40be))


### Bug Fixes

* [#2487](https://github.com/JanssenProject/jans/issues/2487) - irrelevant folder, agama script is in agama-bridge ([#2993](https://github.com/JanssenProject/jans/issues/2993)) ([d19b13a](https://github.com/JanssenProject/jans/commit/d19b13ab31e1dbfee9288c6569b289eae213b528))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - SIWA and SIWG (Sign in with Apple-Google), moved to script-catalog ([#2983](https://github.com/JanssenProject/jans/issues/2983)) ([402e7ae](https://github.com/JanssenProject/jans/commit/402e7aebd20322ef465a3805d3834c7174bc9bbc))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) removing duplicated files ([#3007](https://github.com/JanssenProject/jans/issues/3007)) ([9f3d051](https://github.com/JanssenProject/jans/commit/9f3d051308e7b29e5f112e74601aa05c42ed559c))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) scripts-catalog folder restructuring ([#2999](https://github.com/JanssenProject/jans/issues/2999)) ([7b66f2b](https://github.com/JanssenProject/jans/commit/7b66f2b27517ba560555f64d0ab4e49f10ddb374))
* [#2666](https://github.com/JanssenProject/jans/issues/2666) ([#3011](https://github.com/JanssenProject/jans/issues/3011)) ([f98cbc5](https://github.com/JanssenProject/jans/commit/f98cbc5d43b12b56b81debd367fcfdfdc75830e4))
* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* **jans-fido2:** [#2840](https://github.com/JanssenProject/jans/issues/2840) ([#2974](https://github.com/JanssenProject/jans/issues/2974)) ([d3351e1](https://github.com/JanssenProject/jans/commit/d3351e141fa546074ad98891e655247a0c23e30a))
* typo ([#2950](https://github.com/JanssenProject/jans/issues/2950)) ([6df810b](https://github.com/JanssenProject/jans/commit/6df810b36c68303f458f694e4ba6ec9c3768364c))

## [1.0.4](https://github.com/JanssenProject/jans/compare/docs-v1.0.3...docs-v1.0.4) (2022-11-08)


### Bug Fixes

* [#2825](https://github.com/JanssenProject/jans/issues/2825) ([#2828](https://github.com/JanssenProject/jans/issues/2828)) ([5ce21aa](https://github.com/JanssenProject/jans/commit/5ce21aac5df54d2fe9402479fd221fffe9dc77ef))

## 1.0.3 (2022-11-01)


### Features

* **agama:** add utility classes for inbound identity ([#2204](https://github.com/JanssenProject/jans/issues/2204)) ([29f58ee](https://github.com/JanssenProject/jans/commit/29f58ee0e6c84b4af5493cabcb19167bc7ffbe40))
* **agama:** add utility classes for inbound identity ([#2231](https://github.com/JanssenProject/jans/issues/2231)) ([96e32a4](https://github.com/JanssenProject/jans/commit/96e32a407ec6c545b73a6fd103ed2ae5876bd500))
* **agama:** add utility classes for inbound identity ([#2280](https://github.com/JanssenProject/jans/issues/2280)) ([ca6fdc9](https://github.com/JanssenProject/jans/commit/ca6fdc90256e4ef103bf50dc27cb694c940ba70b))
* **agama:** add utility classes for inbound identity ([#2417](https://github.com/JanssenProject/jans/issues/2417)) ([2878bdd](https://github.com/JanssenProject/jans/commit/2878bdd737b4bd7f8f080113826a4bc4bf49ffba))
* **jans-linux-setup:** added token exchange grant type ([#2768](https://github.com/JanssenProject/jans/issues/2768)) ([b3abcfe](https://github.com/JanssenProject/jans/commit/b3abcfeb8fbaddd6d39eeacba018b6baaf6a2d75))
* **jans-scim:** make max no. of operations and payload size of bulks operations parameterizable ([#1872](https://github.com/JanssenProject/jans/issues/1872)) ([c27a45b](https://github.com/JanssenProject/jans/commit/c27a45bb0a19257c824c4e195f203e9b9b45ec88))


### Bug Fixes

* [#2143](https://github.com/JanssenProject/jans/issues/2143) ([#2144](https://github.com/JanssenProject/jans/issues/2144)) ([ff7f9f4](https://github.com/JanssenProject/jans/commit/ff7f9f4110d72b333aae0d2332b429dcbd067da3))
* [#2157](https://github.com/JanssenProject/jans/issues/2157) ([#2159](https://github.com/JanssenProject/jans/issues/2159)) ([dc8cb60](https://github.com/JanssenProject/jans/commit/dc8cb60990052256b46842f85ebf4961beee82dd))
* [#776](https://github.com/JanssenProject/jans/issues/776) ([#2503](https://github.com/JanssenProject/jans/issues/2503)) ([a564431](https://github.com/JanssenProject/jans/commit/a564431c8b6e503a36dbaf7ccc8f79e6b8adb95f))
* [#817](https://github.com/JanssenProject/jans/issues/817) - script for DUO should have the universal prompt, other APIs are deprecated + documentation minor fixes ([#2363](https://github.com/JanssenProject/jans/issues/2363)) ([ccc13af](https://github.com/JanssenProject/jans/commit/ccc13afdd2cfefecc19aa926a46815d119e6ad76))
* [#817](https://github.com/JanssenProject/jans/issues/817) ([#2364](https://github.com/JanssenProject/jans/issues/2364)) ([bbcd87a](https://github.com/JanssenProject/jans/commit/bbcd87a374f2efca3f87b00bfa21900b2b450c1b))
* **docs:** fix MarkupSafe hash ([#2699](https://github.com/JanssenProject/jans/issues/2699)) ([adf2a6d](https://github.com/JanssenProject/jans/commit/adf2a6d929082da8884973855a095153c1268269))
* **docs:** revert MarkupSafe hash ([#2701](https://github.com/JanssenProject/jans/issues/2701)) ([e722aed](https://github.com/JanssenProject/jans/commit/e722aedd2b8fbbba770cea1112e2757f6e32d10c))
* incorrect contents [#817](https://github.com/JanssenProject/jans/issues/817) ([#2365](https://github.com/JanssenProject/jans/issues/2365)) ([746b33f](https://github.com/JanssenProject/jans/commit/746b33f16c46e2be7381f266e631047e21f17565))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))
* **jans-linux-setup:** review columns size for Agama tables ([#2324](https://github.com/JanssenProject/jans/issues/2324)) ([55d7a7e](https://github.com/JanssenProject/jans/commit/55d7a7e855d1a2ba2cf550e0ddb3a8e9f948456a))
* **jans-scim:** improper handling response of get user operation ([#2420](https://github.com/JanssenProject/jans/issues/2420)) ([b9e00af](https://github.com/JanssenProject/jans/commit/b9e00af483f81280ae80ab6ad732d4f98c30c8a5))
* **jans-scim:** X509 cert not set after successful POST request ([#2407](https://github.com/JanssenProject/jans/issues/2407)) ([fd616c4](https://github.com/JanssenProject/jans/commit/fd616c49df088dfa7c6fae4bd56ed4ecf4e26418))
* minor ([#2470](https://github.com/JanssenProject/jans/issues/2470)) ([657b9f7](https://github.com/JanssenProject/jans/commit/657b9f7589538bb4f1d61ce44fbf6f4da0e63d39))
* minor ([#2786](https://github.com/JanssenProject/jans/issues/2786)) ([3f67763](https://github.com/JanssenProject/jans/commit/3f677636cc2f871e5a9c683634334578405f18f3))
* moved contents under scripts-catalog ([#2370](https://github.com/JanssenProject/jans/issues/2370)) ([fa2273a](https://github.com/JanssenProject/jans/commit/fa2273a6400d512d13fb08ea6685d6ff56faf973))
* update error pages ([#1957](https://github.com/JanssenProject/jans/issues/1957)) ([3d63f4d](https://github.com/JanssenProject/jans/commit/3d63f4d3d58e86d499271ebafdbc0dd56f5c4e98))


### Miscellaneous Chores

* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## 1.0.2 (2022-08-30)


### Features

* **agama:** add utility classes for inbound identity ([#2204](https://github.com/JanssenProject/jans/issues/2204)) ([29f58ee](https://github.com/JanssenProject/jans/commit/29f58ee0e6c84b4af5493cabcb19167bc7ffbe40))
* **agama:** add utility classes for inbound identity ([#2231](https://github.com/JanssenProject/jans/issues/2231)) ([96e32a4](https://github.com/JanssenProject/jans/commit/96e32a407ec6c545b73a6fd103ed2ae5876bd500))
* **jans-scim:** make max no. of operations and payload size of bulks operations parameterizable ([#1872](https://github.com/JanssenProject/jans/issues/1872)) ([c27a45b](https://github.com/JanssenProject/jans/commit/c27a45bb0a19257c824c4e195f203e9b9b45ec88))


### Bug Fixes

* [#2143](https://github.com/JanssenProject/jans/issues/2143) ([#2144](https://github.com/JanssenProject/jans/issues/2144)) ([ff7f9f4](https://github.com/JanssenProject/jans/commit/ff7f9f4110d72b333aae0d2332b429dcbd067da3))
* [#2157](https://github.com/JanssenProject/jans/issues/2157) ([#2159](https://github.com/JanssenProject/jans/issues/2159)) ([dc8cb60](https://github.com/JanssenProject/jans/commit/dc8cb60990052256b46842f85ebf4961beee82dd))
* **agama:** template overriding not working with more than one level of nesting ([#1841](https://github.com/JanssenProject/jans/issues/1841)) ([723922a](https://github.com/JanssenProject/jans/commit/723922a17b1babc49a1135030c06db367726ab63))
* correct the link to image ([#1660](https://github.com/JanssenProject/jans/issues/1660)) ([0943d81](https://github.com/JanssenProject/jans/commit/0943d813f782a3babaa5166f426533fd561419a5))
* update error pages ([#1957](https://github.com/JanssenProject/jans/issues/1957)) ([3d63f4d](https://github.com/JanssenProject/jans/commit/3d63f4d3d58e86d499271ebafdbc0dd56f5c4e98))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/docs-v1.0.0...docs-v1.0.1) (2022-07-06)


### Bug Fixes

* correct the link to image ([#1660](https://github.com/JanssenProject/jans/issues/1660)) ([0943d81](https://github.com/JanssenProject/jans/commit/0943d813f782a3babaa5166f426533fd561419a5))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

## 1.0.0 (2022-05-20)


### Features

* add script for Google login ([#1141](https://github.com/JanssenProject/jans/issues/1141)) ([bac9144](https://github.com/JanssenProject/jans/commit/bac9144ad8a5f8f2b378aa67663caab9f19f052b))
* **jans-core:** remove UPDATE_USER and USER_REGISTRATION scripts [#1289](https://github.com/JanssenProject/jans/issues/1289) ([c34e75d](https://github.com/JanssenProject/jans/commit/c34e75d49db89999633249376c7b42c41bb1ce24))


### Bug Fixes

* add issue guidelines to TOC ([#1188](https://github.com/JanssenProject/jans/issues/1188)) ([192165b](https://github.com/JanssenProject/jans/commit/192165b3eecc3cbcb03a5c9774a4dae212a9ffad))
* broken links ([86d0232](https://github.com/JanssenProject/jans/commit/86d023209fd8af5422153b1dd97e2a25a7b59c28))
* hyperlinks ([#1209](https://github.com/JanssenProject/jans/issues/1209)) ([d1e1ed6](https://github.com/JanssenProject/jans/commit/d1e1ed63d8cea3030b23bd218241fe2275ed6e52))
* typo in jans-cli interactive mode ([25f5971](https://github.com/JanssenProject/jans/commit/25f59716aa2bccb2dcdb47a34a7039a0e83d0f5f))


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

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/docs-v1.0.0-beta.15...docs-v1.0.0-beta.16) (2022-03-14)


### Features

* add support to import custom ldif ([#1002](https://github.com/JanssenProject/jans/issues/1002)) ([0b6334a](https://github.com/JanssenProject/jans/commit/0b6334acdb862ce458c628a8eb81ef0b8f7c5dcb))


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

* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([7bcad3b](https://github.com/JanssenProject/jans/commit/7bcad3b5b68ac748dec7be18651641146943bd22))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([932a45b](https://github.com/JanssenProject/jans/commit/932a45bc4c34235dad5c5813a10f157b0350966b))
* protecting Admin-UI Plugin Apis [#142](https://github.com/JanssenProject/jans/issues/142) ([52e8846](https://github.com/JanssenProject/jans/commit/52e8846f3eeb9c3b5d624f67ea736f62b455eeed))
* protecting Admin-UI Plugin Apis [#142](https://github.com/JanssenProject/jans/issues/142) ([a1a0d54](https://github.com/JanssenProject/jans/commit/a1a0d54c1c0860e8828afb47e3548b0876394e83))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))
