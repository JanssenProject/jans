# Changelog

## [1.9.0](https://github.com/JanssenProject/jans/compare/v1.8.0...v1.9.0) (2025-07-24)


### Features

* add check to prevent unsigned user-info jwt in the role_based_scopes_update_token script (Admin UI) ([#11724](https://github.com/JanssenProject/jans/issues/11724)) ([52c4682](https://github.com/JanssenProject/jans/commit/52c46821a4efbb7996fc25d9b8d55fd692b7d1c0))
* add the mandatory permissions for default roles in Admin UI ([#11711](https://github.com/JanssenProject/jans/issues/11711)) ([9a56669](https://github.com/JanssenProject/jans/commit/9a56669f8c3b91fe035c39ad75f5a701fca8b0a9))
* added essentialPermissionInAdminUI attribute to AdminPermission object ([#11714](https://github.com/JanssenProject/jans/issues/11714)) ([d945703](https://github.com/JanssenProject/jans/commit/d945703a61ac15353f3ef307f192f50ca1bf57f6))
* **cloud-native:** add the mandatory permissions for default roles in Admin UI ([#11739](https://github.com/JanssenProject/jans/issues/11739)) ([7831887](https://github.com/JanssenProject/jans/commit/7831887d2fea6df86db39fdc4de25a6d32ed857c))
* **config-api:** audit for config-api endpoints ([#11760](https://github.com/JanssenProject/jans/issues/11760)) ([be52530](https://github.com/JanssenProject/jans/commit/be52530e1aae3a673b302f228edc9779d2b00c3f))
* **config-api:** logs to indicate password related activity ([#11791](https://github.com/JanssenProject/jans/issues/11791)) ([838686b](https://github.com/JanssenProject/jans/commit/838686b75b241054d3cef4fa043accf4104d3b6a))
* **jans-auth-server:** deprecated /revoke_session endpoint (it duplicates Global Token Revocation functionality) [#11470](https://github.com/JanssenProject/jans/issues/11470) ([#11801](https://github.com/JanssenProject/jans/issues/11801)) ([d7178aa](https://github.com/JanssenProject/jans/commit/d7178aaab1dc7f92c709aa75ca7232a9216a3ec9))
* **jans-auth-server:** improved SessionIdService - added option to load session without local copy [#11366](https://github.com/JanssenProject/jans/issues/11366) ([#11761](https://github.com/JanssenProject/jans/issues/11761)) ([4510bd2](https://github.com/JanssenProject/jans/commit/4510bd24a4b8a85db99b125a0ca17d133f295bf6))
* **jans-auth-server:** small improvement of ssa doc [#11736](https://github.com/JanssenProject/jans/issues/11736) ([#11737](https://github.com/JanssenProject/jans/issues/11737)) ([7a6af91](https://github.com/JanssenProject/jans/commit/7a6af912169f8953078d9a864f6ce1976440c28c))
* **jans-auth:** exclude htmlunit-* dependencies from final artifacts ([#11830](https://github.com/JanssenProject/jans/issues/11830)) ([1c17b6d](https://github.com/JanssenProject/jans/commit/1c17b6df631dc91c3d771c48f9f8031e83edda5e))
* **jans-auth:** fix client_registration script imports ([#11705](https://github.com/JanssenProject/jans/issues/11705)) ([8b75934](https://github.com/JanssenProject/jans/commit/8b7593437704f09c8fd8dc1137a41ef7d4840150))
* **jans-cedarling:** add maven javadocs plugin in java binding ([#11745](https://github.com/JanssenProject/jans/issues/11745)) ([f68e936](https://github.com/JanssenProject/jans/commit/f68e9363ec457e140afb58f68981521a1073432c))
* **jans-cedarling:** add support for the optional SSA JWT ([#11653](https://github.com/JanssenProject/jans/issues/11653)) ([902f9d0](https://github.com/JanssenProject/jans/commit/902f9d09d7ccb405ac57a0115fd986e32f750980))
* **jans-cedarling:** implement JWT status list validation ([#11520](https://github.com/JanssenProject/jans/issues/11520)) ([f2e7f29](https://github.com/JanssenProject/jans/commit/f2e7f29901bffca65317f038dc4123c86ea1d84b))
* **jans-cedarling:** output cedar annotations when there are any policy failures ([#11588](https://github.com/JanssenProject/jans/issues/11588)) ([0714a17](https://github.com/JanssenProject/jans/commit/0714a170a1f495667ee53dec24846b2c6d7f7b3a))
* **jans-config-api:** add cedarlingLogType attribute in Admin UI configuration ([#11755](https://github.com/JanssenProject/jans/issues/11755)) ([d4da957](https://github.com/JanssenProject/jans/commit/d4da957d09cfdab976380e5fc33aa76aea7bb4bd))
* **jans-config-api:** add endpoint to reset license details in Admin UI configuration ([#11786](https://github.com/JanssenProject/jans/issues/11786)) ([56df1e6](https://github.com/JanssenProject/jans/commit/56df1e6508a510adb193450d408564765333ea67))
* **jans-config-api:** update OpenApi specs for Admin UI plugin ([#11729](https://github.com/JanssenProject/jans/issues/11729)) ([4dc0c9a](https://github.com/JanssenProject/jans/commit/4dc0c9a4621c24e57678d782e9f6ce6fd6c13888))
* **jans-fido2:** Add unit tests for attestation controller for handling missing username, invalid origin, and challenge, with successful register and verify scenarios. ([ca68fd0](https://github.com/JanssenProject/jans/commit/ca68fd0fbe71d34aaad989194bd625e2c7331c05))
* **jans-linux-setup:** add Debian 13 MySQL support ([#11759](https://github.com/JanssenProject/jans/issues/11759)) ([7fbcd10](https://github.com/JanssenProject/jans/commit/7fbcd10150a88985e4559b2daaa9c613e628878d))
* **jans-linux-setup:** create clients takes all possible arguments ([#11770](https://github.com/JanssenProject/jans/issues/11770)) ([eca4b0c](https://github.com/JanssenProject/jans/commit/eca4b0ce604666af90cfef1da9306c3b8e540e39))
* **jans-linux-setup:** debian 13 support for internal use ([#11685](https://github.com/JanssenProject/jans/issues/11685)) ([7b6e25b](https://github.com/JanssenProject/jans/commit/7b6e25b966e03ec06020bf3e6de00fa6ef9e03d6))
* **jans-orm:** add method to return internal information about tables ([#11695](https://github.com/JanssenProject/jans/issues/11695)) ([7039b74](https://github.com/JanssenProject/jans/commit/7039b740c334592658b8c3b63d006346e7b7c238))
* update OpenApi specs for Admin UI plugin ([4dc0c9a](https://github.com/JanssenProject/jans/commit/4dc0c9a4621c24e57678d782e9f6ce6fd6c13888))


### Bug Fixes

* add defeat the gorn ([5ed2dcd](https://github.com/JanssenProject/jans/commit/5ed2dcd691088695a7a4ddb63136795b45591275))
* **config-api:** user name validation modification ([#11776](https://github.com/JanssenProject/jans/issues/11776)) ([45386c1](https://github.com/JanssenProject/jans/commit/45386c18ea8f6fff12aaea6ac72d70c22de9f14c))
* **doc:** added default values for sessionId related properties to avoid confusion ([#11781](https://github.com/JanssenProject/jans/issues/11781)) ([0894860](https://github.com/JanssenProject/jans/commit/08948609df71de7f8a8fe0b5584a7952cbc3e9dc))
* **docs:** add documentation for configuration and session management ([#11091](https://github.com/JanssenProject/jans/issues/11091)) ([0184771](https://github.com/JanssenProject/jans/commit/01847717179334316784a6027e7dfd6da67be163))
* **docs:** add note on config refresh behavior in TUI ([#11789](https://github.com/JanssenProject/jans/issues/11789)) ([b237d19](https://github.com/JanssenProject/jans/commit/b237d196e5f76bbc7ff145b685096227f3743a42))
* **docs:** add upgrade note about manual custom script updates ([#11719](https://github.com/JanssenProject/jans/issues/11719)) ([8a6e3db](https://github.com/JanssenProject/jans/commit/8a6e3db49a8458e301be37013733da6077142acb))
* **docs:** docs fix cedarling propertie link issue ([aaa4eb9](https://github.com/JanssenProject/jans/commit/aaa4eb9a2802ad05a5438a82a4893e27a5954f1e))
* **docs:** docs fix Sample Scripts link issue ([bf617a3](https://github.com/JanssenProject/jans/commit/bf617a367cde0b89e393c21b0722083bcc849db1))
* **docs:** docs fix sample scripts link issue ([#11779](https://github.com/JanssenProject/jans/issues/11779)) ([bf617a3](https://github.com/JanssenProject/jans/commit/bf617a367cde0b89e393c21b0722083bcc849db1))
* **docs:** docs update jans readme ([#11687](https://github.com/JanssenProject/jans/issues/11687)) ([bf42440](https://github.com/JanssenProject/jans/commit/bf42440bd52cdc93ca934aefaf148ac6c35c155d))
* **docs:** fix Cedarling property link issue ([#11780](https://github.com/JanssenProject/jans/issues/11780)) ([aaa4eb9](https://github.com/JanssenProject/jans/commit/aaa4eb9a2802ad05a5438a82a4893e27a5954f1e))
* **docs:** fix incorrect link in Jans Casa docs ([#11798](https://github.com/JanssenProject/jans/issues/11798)) ([ec9a3a9](https://github.com/JanssenProject/jans/commit/ec9a3a90eed052839509883658e61947aee85b52))
* **docs:** remove attribute page ([00e0b2d](https://github.com/JanssenProject/jans/commit/00e0b2dac3359d1e906a69ca28981ebb9d346b80))
* **docs:** remove config-api attribute page ([#11722](https://github.com/JanssenProject/jans/issues/11722)) ([00e0b2d](https://github.com/JanssenProject/jans/commit/00e0b2dac3359d1e906a69ca28981ebb9d346b80))
* **docs:** remove converting data ([e729c89](https://github.com/JanssenProject/jans/commit/e729c890e3c1c7d7cde0b469652558ee6f7f388c))
* **docs:** remove the link to the converting data document from left nav ([#11720](https://github.com/JanssenProject/jans/issues/11720)) ([e729c89](https://github.com/JanssenProject/jans/commit/e729c890e3c1c7d7cde0b469652558ee6f7f388c))
* **docs:** reorganise supported OS versions for VM installation ([#11679](https://github.com/JanssenProject/jans/issues/11679)) ([329a113](https://github.com/JanssenProject/jans/commit/329a1139194fd9819506150ec73225b425767064))
* **docs:** update supported OS versions ([329a113](https://github.com/JanssenProject/jans/commit/329a1139194fd9819506150ec73225b425767064))
* **docs:** update testing document with Poetry installation instructions ([#11681](https://github.com/JanssenProject/jans/issues/11681)) ([e609156](https://github.com/JanssenProject/jans/commit/e60915647c6523a4b75b955cd3f6b8aceaa6c364))
* **jans-auth-server:** set sub claim to client identifier for "client credentials grant" for AT as JWT [#11413](https://github.com/JanssenProject/jans/issues/11413) ([#11778](https://github.com/JanssenProject/jans/issues/11778)) ([60373a7](https://github.com/JanssenProject/jans/commit/60373a7e4c91928ce46f2f84b3af9e3051a4a50e))
* **jans-auth:** duplicate entry exception in start login flow [#9322](https://github.com/JanssenProject/jans/issues/9322) ([#11808](https://github.com/JanssenProject/jans/issues/11808)) ([ee4d38c](https://github.com/JanssenProject/jans/commit/ee4d38c02a51df67f2637f63285e3e263b0d47d8))
* **jans-cli-tui:** add defeat the gorn ([#11825](https://github.com/JanssenProject/jans/issues/11825)) ([5ed2dcd](https://github.com/JanssenProject/jans/commit/5ed2dcd691088695a7a4ddb63136795b45591275))
* **jans-cli-tui:** adjust entries per page dynamiccally ([#11807](https://github.com/JanssenProject/jans/issues/11807)) ([03dca24](https://github.com/JanssenProject/jans/commit/03dca248b40a7db7b2a7f3cecb4e42c4d76ea3e4))
* **jans-cli-tui:** disable expiration verification for user data jwt ([#11669](https://github.com/JanssenProject/jans/issues/11669)) ([a9f59f4](https://github.com/JanssenProject/jans/commit/a9f59f4227d70c83494efba8e7c6bc1a210ece55))
* **jans-cli-tui:** dynamic script is list for dynamic scope ([#11734](https://github.com/JanssenProject/jans/issues/11734)) ([fea842c](https://github.com/JanssenProject/jans/commit/fea842c4658e5bc5790c70e8ec68565992098350))
* **jans-config-api:** changing the /admin-ui/license/resetConfig to DELETE Http method ([#11793](https://github.com/JanssenProject/jans/issues/11793)) ([39c48cc](https://github.com/JanssenProject/jans/commit/39c48cc5b3b9d0f1ce98067f09542e1b54847e6d))
* **jans-fido2:** resolve registration issue ([#11827](https://github.com/JanssenProject/jans/issues/11827)) ([b8b9927](https://github.com/JanssenProject/jans/commit/b8b9927c1f296f69a9d139913b28d8b79b78f8f7))
* **jans-kc-scheduler:** typo prevented proper loading of authorization scopes for jans-config-api client ([22718c5](https://github.com/JanssenProject/jans/commit/22718c533743320c8f217ada3f54240fa2023484))
* **jans-kc-scheduler:** typo prevented proper loading of authz scopes [#11802](https://github.com/JanssenProject/jans/issues/11802) ([#11813](https://github.com/JanssenProject/jans/issues/11813)) ([22718c5](https://github.com/JanssenProject/jans/commit/22718c533743320c8f217ada3f54240fa2023484))
* **jans-linux-setup:** data type of jansScr is LONGTEXT ([#11763](https://github.com/JanssenProject/jans/issues/11763)) ([0025728](https://github.com/JanssenProject/jans/commit/0025728172a87b6f99f26802f110da5c4df4f07e))
* **jans-linux-setup:** openbanking installation setup ([#11703](https://github.com/JanssenProject/jans/issues/11703)) ([4905ed7](https://github.com/JanssenProject/jans/commit/4905ed7a68ba9a4265a1a2e202050f5ddcdd89cb))
* **jans-linux-setup:** re-order app installations ([#11731](https://github.com/JanssenProject/jans/issues/11731)) ([c569fbf](https://github.com/JanssenProject/jans/commit/c569fbf8c89ae7fb04218f68f5776d2bb631451f))
* **jans-linux-setup:** scopes in /opt/kc-scheduler/conf/config.properties ([#11819](https://github.com/JanssenProject/jans/issues/11819)) ([ee93677](https://github.com/JanssenProject/jans/commit/ee93677fa5bbd06b19c2d73dd6b4a95459f27ee4))

## [1.8.0](https://github.com/JanssenProject/jans/compare/v1.7.0...v1.8.0) (2025-06-24)


### Features

* **charts:** add nodeSelector spec ([#11495](https://github.com/JanssenProject/jans/issues/11495)) ([90409f8](https://github.com/JanssenProject/jans/commit/90409f86e4b3713c9120723796320e604b4fa95e))
* **cloud-native:** introduce Logout Status JWT ([#11626](https://github.com/JanssenProject/jans/issues/11626)) ([6f5fe8f](https://github.com/JanssenProject/jans/commit/6f5fe8fe0245d6592f714af3b3b2f64d66a88adc))
* **cloud-native:** introduce session_jwt=true at Authorization Endpoint and Session JWT Status List Endpoint ([#11477](https://github.com/JanssenProject/jans/issues/11477)) ([5ae709f](https://github.com/JanssenProject/jans/commit/5ae709f4719af1f7fbdf6a84e812818861470513))
* **config-api:** scope search to include scopeType ([#11607](https://github.com/JanssenProject/jans/issues/11607)) ([b77b72d](https://github.com/JanssenProject/jans/commit/b77b72d2bb35b5b43751c0fb4dfa60a6eacfb85a))
* **config-api:** use orm method to provide persistence metadata  ([#11509](https://github.com/JanssenProject/jans/issues/11509)) ([dbdc1bd](https://github.com/JanssenProject/jans/commit/dbdc1bd92649e7620ee94e100d020dc8a748c42b))
* **config-api:** use orm method to provide persistence metadata [#11459](https://github.com/JanssenProject/jans/issues/11459) ([#11473](https://github.com/JanssenProject/jans/issues/11473)) ([736d7f6](https://github.com/JanssenProject/jans/commit/736d7f65f29a133278303c729a1e89ddf4cb45af))
* improve the display of cedarling result ([#11552](https://github.com/JanssenProject/jans/issues/11552)) ([5ec5268](https://github.com/JanssenProject/jans/commit/5ec526866e4a019cf40af143209243fceace059e))
* **jans-auth-server:** introduce session_jwt=true at Authorization Endpoint and Session JWT Status List Endpoint [#11229](https://github.com/JanssenProject/jans/issues/11229) ([#11251](https://github.com/JanssenProject/jans/issues/11251)) ([6b45f51](https://github.com/JanssenProject/jans/commit/6b45f516a7951d029f84eb5db491f45c1c181260))
* **jans-auth-server:** introduced Logout Status JWT [#11468](https://github.com/JanssenProject/jans/issues/11468) ([#11505](https://github.com/JanssenProject/jans/issues/11505)) ([92796fb](https://github.com/JanssenProject/jans/commit/92796fbf696c0dac5331ff1e9b2b493a2de42836))
* **jans-auth:** allow to override HttpService2 connection manager setting ([54c547a](https://github.com/JanssenProject/jans/commit/54c547aab349bc06440880774f701826bafe5362))
* **jans-auth:** allow to override HttpService2 connection manager settings ([#11586](https://github.com/JanssenProject/jans/issues/11586)) ([54c547a](https://github.com/JanssenProject/jans/commit/54c547aab349bc06440880774f701826bafe5362))
* **jans-auth:** update owasp ([#11610](https://github.com/JanssenProject/jans/issues/11610)) ([bda24dc](https://github.com/JanssenProject/jans/commit/bda24dcbad05973fd71dfab4d2e62d30c624d79f))
* **jans-cedarling:** implement sending logs to the lock server ([#11161](https://github.com/JanssenProject/jans/issues/11161)) ([9330821](https://github.com/JanssenProject/jans/commit/9330821de7f947f94345e8cee87ac2f60f1a2e0c))
* **jans-cedarling:** improve error message if json parsing of policy store failed ([#11508](https://github.com/JanssenProject/jans/issues/11508)) ([4904d90](https://github.com/JanssenProject/jans/commit/4904d907d281706fdfa6fcfec223a24661f3454a))
* **jans-cedarling:** jans cedarling java binding ([#11441](https://github.com/JanssenProject/jans/issues/11441)) ([da5423f](https://github.com/JanssenProject/jans/commit/da5423f45162c97b982f4da24e272dc477afe055))
* **jans-cedarling:** upgrade jna in cedarling-java binding ([#11614](https://github.com/JanssenProject/jans/issues/11614)) ([aa45972](https://github.com/JanssenProject/jans/commit/aa45972a3c74758e9a715dc2b7cd6ffbce1e5ea0))
* **jans-cli-tui:** display build date and build versions ([#11539](https://github.com/JanssenProject/jans/issues/11539)) ([59415c8](https://github.com/JanssenProject/jans/commit/59415c80f37cca27c1b857bf52f56a5af1ec8ce0))
* **jans-cli-tui:** load agama community projects in 20 mins interval ([#11627](https://github.com/JanssenProject/jans/issues/11627)) ([44873e0](https://github.com/JanssenProject/jans/commit/44873e0015f43cc162b6e792ca880f6fb346fae0))
* **jans-cli-tui:** required property for attribute ([#11593](https://github.com/JanssenProject/jans/issues/11593)) ([6b8c7a9](https://github.com/JanssenProject/jans/commit/6b8c7a9e53b8cec602b4bcbf7a6a1608c30e3f5a))
* **jans-config-api:** update the license API url called from config-api (Admin UI Plugin) ([#11559](https://github.com/JanssenProject/jans/issues/11559)) ([c8b7437](https://github.com/JanssenProject/jans/commit/c8b743763d6177fead693f916d0c14b42612daa9))
* **jans-core:** added diagnostic to java compiler ([#11562](https://github.com/JanssenProject/jans/issues/11562)) ([99bd129](https://github.com/JanssenProject/jans/commit/99bd1290a65cb051230fc3d52438cd2228879553))
* **jans-linux-setup:** optional rdbm db schema ([#11503](https://github.com/JanssenProject/jans/issues/11503)) ([f516bbc](https://github.com/JanssenProject/jans/commit/f516bbceab453b5fdb03b844e5433e421a821221))
* **jans-orm:** add method to provide persistence metadata ([#11450](https://github.com/JanssenProject/jans/issues/11450)) ([a066fcf](https://github.com/JanssenProject/jans/commit/a066fcfcb18d8086f6a1f3f7b1221292cd6b6303))
* **jans-orm:** implement solution to import hashed passwords ([7518dca](https://github.com/JanssenProject/jans/commit/7518dca1b87b637bbcb03f2dd27fea44a2294588))
* **jans-orm:** implement solution to import hashed passwords ([#11601](https://github.com/JanssenProject/jans/issues/11601)) ([d8f5faa](https://github.com/JanssenProject/jans/commit/d8f5faa23f4f7110e901da29b227a74b7249d2da))
* **jans-script:** add sample Argon2 Persistence Ezxtension script ([#11521](https://github.com/JanssenProject/jans/issues/11521)) ([3e2593f](https://github.com/JanssenProject/jans/commit/3e2593f3b44aba96e57d61bf5a55585a972f6db6))
* **jans-tui:** added Jans-Client header ([#11594](https://github.com/JanssenProject/jans/issues/11594)) ([13bd6c1](https://github.com/JanssenProject/jans/commit/13bd6c1ea5f338358cc47f6e2edf833cfce643f4))
* **jans=-script:** add sample Argon2 Persistence Ezxtension script ([3e2593f](https://github.com/JanssenProject/jans/commit/3e2593f3b44aba96e57d61bf5a55585a972f6db6))
* **orm:** fetch databaseName for PersistenceMetadata ([#11485](https://github.com/JanssenProject/jans/issues/11485)) ([f9ffcac](https://github.com/JanssenProject/jans/commit/f9ffcac06a14ab2d6c53f20f6f6a56676c98ab04))
* **orm:** fix doc file ([#11494](https://github.com/JanssenProject/jans/issues/11494)) ([0e71ad6](https://github.com/JanssenProject/jans/commit/0e71ad6aee97267d7eea474d149c0016bcd96853))
* **orm:** fix doc file ([#11497](https://github.com/JanssenProject/jans/issues/11497)) ([76d8f1d](https://github.com/JanssenProject/jans/commit/76d8f1d96dd092b5e8b74904e3c93947dad1f3d2))
* **orm:** support password hashing using Argon2 ([#11465](https://github.com/JanssenProject/jans/issues/11465)) ([5d48b27](https://github.com/JanssenProject/jans/commit/5d48b2775b7248bfa1fb205c2ade2adda98d835a))
* **orm:** support password hashing using Argon2 ([#11481](https://github.com/JanssenProject/jans/issues/11481)) ([47549af](https://github.com/JanssenProject/jans/commit/47549af8caf1abda02a7d86cdd19e363004319f7))
* update tf build docs ([#11632](https://github.com/JanssenProject/jans/issues/11632)) ([c9f37dd](https://github.com/JanssenProject/jans/commit/c9f37dd02e21f1081f2d19be102cdbef6d28f9d2))
* update the license API url called from config-api (Admin UI Plugin) ([c8b7437](https://github.com/JanssenProject/jans/commit/c8b743763d6177fead693f916d0c14b42612daa9))


### Bug Fixes

* build_cedarling_uniffi job in github workflow is failing ([355cb2a](https://github.com/JanssenProject/jans/commit/355cb2a68bde70615f5bf0482a6525f885b66ecc))
* **charts:** invalid template function calls when using AWS secrets manager ([#11467](https://github.com/JanssenProject/jans/issues/11467)) ([6a114a6](https://github.com/JanssenProject/jans/commit/6a114a69821cd5c7317c08fa5877494b39555486))
* **config-api:** Scope filter based on fieldValuePair  ([#11528](https://github.com/JanssenProject/jans/issues/11528)) ([560f7a3](https://github.com/JanssenProject/jans/commit/560f7a3855a89bfb3d112fde212e50db6920b3ea))
* **config-api:** Scope filter based on fieldValuePair [#11524](https://github.com/JanssenProject/jans/issues/11524) ([560f7a3](https://github.com/JanssenProject/jans/commit/560f7a3855a89bfb3d112fde212e50db6920b3ea))
* **docs:** Add `Cedarling Technical Overview` doc ([d04336d](https://github.com/JanssenProject/jans/commit/d04336d2081af053e521169a6f457a3ff7fef4ac))
* **docs:** add appropriate title to the document ([#11579](https://github.com/JanssenProject/jans/issues/11579)) ([2907197](https://github.com/JanssenProject/jans/commit/29071975d55e065b2d6d3b0000191b6baff39715))
* **docs:** add Cedarling technical overview doc ([#11581](https://github.com/JanssenProject/jans/issues/11581)) ([d04336d](https://github.com/JanssenProject/jans/commit/d04336d2081af053e521169a6f457a3ff7fef4ac))
* **docs:** correct jans api swagger reference ([#11474](https://github.com/JanssenProject/jans/issues/11474)) ([7072a83](https://github.com/JanssenProject/jans/commit/7072a83f5d1b209ff29318797dedaee6efa26100))
* **docs:** remove `overview` title ([2907197](https://github.com/JanssenProject/jans/commit/29071975d55e065b2d6d3b0000191b6baff39715))
* **docs:** update release urls to point to latest Janssen release ([#11557](https://github.com/JanssenProject/jans/issues/11557)) ([d13b291](https://github.com/JanssenProject/jans/commit/d13b29162d9bf71e1de6159e94ed4c1676e0e67e))
* **docs:** update standard claims list ([#11567](https://github.com/JanssenProject/jans/issues/11567)) ([99227b1](https://github.com/JanssenProject/jans/commit/99227b10a8f11a56c4e98b8994a172994abe4759))
* **docs:** update the instructions for certificates ([#10933](https://github.com/JanssenProject/jans/issues/10933)) ([dbdfbee](https://github.com/JanssenProject/jans/commit/dbdfbee7d671b70ce0fbebc01e638097ffd51a66))
* **jans-auth-server:** external libraries unavaiable from Java Interception Script [#11377](https://github.com/JanssenProject/jans/issues/11377) ([#11568](https://github.com/JanssenProject/jans/issues/11568)) ([aa3ffb5](https://github.com/JanssenProject/jans/commit/aa3ffb5de87c5f5761f83463e4ed6820641a3297))
* **jans-auth-server:** improve logging - do not print that user is logged in in logs if it failed to login [#11475](https://github.com/JanssenProject/jans/issues/11475) ([#11480](https://github.com/JanssenProject/jans/issues/11480)) ([faae9d4](https://github.com/JanssenProject/jans/commit/faae9d475be946c7c041a5b6cc357c3e50892a9a))
* **jans-cedarling:** build_cedarling_uniffi job in github workflow is failing ([#11542](https://github.com/JanssenProject/jans/issues/11542)) ([355cb2a](https://github.com/JanssenProject/jans/commit/355cb2a68bde70615f5bf0482a6525f885b66ecc))
* **jans-cli-tui:** display warnings for scriptType in edit clients ([#11499](https://github.com/JanssenProject/jans/issues/11499)) ([68f2b57](https://github.com/JanssenProject/jans/commit/68f2b5733d0971c6c7089b79f9b7767d1c851969))
* **jans-cli-tui:** edit script config proprty ([#11463](https://github.com/JanssenProject/jans/issues/11463)) ([bdc3bd7](https://github.com/JanssenProject/jans/commit/bdc3bd76425fa5cbde97566a24a403b325895280))
* **jans-cli-tui:** enforce setting SSA life time at least 5 minutes ([#11628](https://github.com/JanssenProject/jans/issues/11628)) ([d4f9f2f](https://github.com/JanssenProject/jans/commit/d4f9f2feb17a031acd031d7a7bcb6d947d940d84))
* **jans-cli-tui:** persistence type ([#11510](https://github.com/JanssenProject/jans/issues/11510)) ([d8c0cab](https://github.com/JanssenProject/jans/commit/d8c0cabbb0bd47fcd50004989ac8d759e0f45984))
* **jans-cli-tui:** user jansStatus is in body not custom attrbiute ([#11615](https://github.com/JanssenProject/jans/issues/11615)) ([d809779](https://github.com/JanssenProject/jans/commit/d809779db091b20e98ce9bd5e73d8a33eddca2dd))
* **jans-linux-setup:** re compatibility ([#11483](https://github.com/JanssenProject/jans/issues/11483)) ([3bc9193](https://github.com/JanssenProject/jans/commit/3bc9193ea15497c0b1b7597ad97b78c200a10406))
* **jans-orm:** use password.method prefix for all hash algs ([#11502](https://github.com/JanssenProject/jans/issues/11502)) ([62b322f](https://github.com/JanssenProject/jans/commit/62b322fb6debaa6d6efe3f6bffea456c7266a48f))
* **jans-scipt:** correct script imports ([#11516](https://github.com/JanssenProject/jans/issues/11516)) ([385219e](https://github.com/JanssenProject/jans/commit/385219e7e1485c8b3601846e6c1a8d194d2cd21d))
* **jans-scipt:** correct script imports ([#11517](https://github.com/JanssenProject/jans/issues/11517)) ([2b74d24](https://github.com/JanssenProject/jans/commit/2b74d24dcc00549928cf00189059dd85d05e1995))
* prepare release 1.8.0 ([#11647](https://github.com/JanssenProject/jans/issues/11647)) ([f29d9a3](https://github.com/JanssenProject/jans/commit/f29d9a32b0a52a3540ecadf137fbda4b3992559d))

## [1.7.0](https://github.com/JanssenProject/jans/compare/v1.6.0...v1.7.0) (2025-05-20)


### Features

* **jans-auth-server:** added refresh token lifetime to Token Endpoint response [#11400](https://github.com/JanssenProject/jans/issues/11400) ([#11414](https://github.com/JanssenProject/jans/issues/11414)) ([00d7c88](https://github.com/JanssenProject/jans/commit/00d7c887a764263f6fd5ffc64bd85d635ee17399))
* **jans-cedarling:** update plugin to use binding ([#11356](https://github.com/JanssenProject/jans/issues/11356)) ([c61ef53](https://github.com/JanssenProject/jans/commit/c61ef53c8310af9ed06bc4b8ee8b05af69d088d8))
* **jans-cli-tui:** ssa templates ([#11368](https://github.com/JanssenProject/jans/issues/11368)) ([ed18096](https://github.com/JanssenProject/jans/commit/ed180962ee7f336b5742ef723b0bd8b9c3f9f7cb))
* **jans-core:** add jakarta.mail for TLS support ([#11428](https://github.com/JanssenProject/jans/issues/11428)) ([8cf6b68](https://github.com/JanssenProject/jans/commit/8cf6b68bbdfd73d69008fd98eb94cdb041e1a2aa))
* **jans-lock:** try to reload /.well-known/openid-configuration on f… ([#11392](https://github.com/JanssenProject/jans/issues/11392)) ([00e2257](https://github.com/JanssenProject/jans/commit/00e225724193a1572192a34e396bb9348b52e815))
* **jans-lock:** try to reload /.well-known/openid-configuration on failure ([00e2257](https://github.com/JanssenProject/jans/commit/00e225724193a1572192a34e396bb9348b52e815))
* use cedarling uniffi kotlin binding in java project ([#11336](https://github.com/JanssenProject/jans/issues/11336)) ([067b3c1](https://github.com/JanssenProject/jans/commit/067b3c1833c0a0cab840b222434a331c2d085b98))


### Bug Fixes

* **docs:** add Config API OpenAPI Spec generation at build time ([#11407](https://github.com/JanssenProject/jans/issues/11407)) ([70277c5](https://github.com/JanssenProject/jans/commit/70277c5be676492f58c059aff50092d7b5aa9d46))
* **docs:** add sections to place ADRs and design decisions ([#11380](https://github.com/JanssenProject/jans/issues/11380)) ([4c0db93](https://github.com/JanssenProject/jans/commit/4c0db934a0c15ec4544c9b26da500fb743a54715))
* **docs:** refactor development docs to add content from Wiki ([#11409](https://github.com/JanssenProject/jans/issues/11409)) ([71d133f](https://github.com/JanssenProject/jans/commit/71d133f6f6f366039913220061879bf774358335))
* **docs:** update curl instructions for jans config api ([#11415](https://github.com/JanssenProject/jans/issues/11415)) ([39b6b82](https://github.com/JanssenProject/jans/commit/39b6b8215b65dadc9ef9a61ecf0b5c2b5dcf0c86))
* **jans-cedarling:** fix logs in jwt module not getting displayed correctly ([#11369](https://github.com/JanssenProject/jans/issues/11369)) ([e856545](https://github.com/JanssenProject/jans/commit/e856545e45f4b23d1d0f2c437fe661fb9eeaa1ef))
* **jans-cli-tui:** array type in auth properties ([#11399](https://github.com/JanssenProject/jans/issues/11399)) ([6e4d7e4](https://github.com/JanssenProject/jans/commit/6e4d7e41f7fba94c0cc275ea29cef4d5e93afff8))
* **jans-cli-tui:** object type configuration properties ([#11391](https://github.com/JanssenProject/jans/issues/11391)) ([7cf13a1](https://github.com/JanssenProject/jans/commit/7cf13a16f2c280b0edb7760e9313a61e6576da0d))
* mail dependencies ([#11426](https://github.com/JanssenProject/jans/issues/11426)) ([58c02b0](https://github.com/JanssenProject/jans/commit/58c02b0a2cd048f959f347567673d11369be064f))
* remove version_name attribute from manifest.json ([#11387](https://github.com/JanssenProject/jans/issues/11387)) ([0febbd9](https://github.com/JanssenProject/jans/commit/0febbd95aa58a517920e40d13095fa8b7c4695bc))


### Miscellaneous Chores

* prepare release 1.7.0 ([#11432](https://github.com/JanssenProject/jans/issues/11432)) ([13e4c41](https://github.com/JanssenProject/jans/commit/13e4c412034db6e642da456374a375e7726f46b2))

## [1.6.0](https://github.com/JanssenProject/jans/compare/v1.5.0...v1.6.0) (2025-05-08)


### Bug Fixes

* fix(jans-linux-setup): pass -n to setup.py when invoked by -yes by @devrimyatar in https://github.com/JanssenProject/jans/pull/11180
* fix(docs): fix image paths in SAML SSO document by @ossdhaval in https://github.com/JanssenProject/jans/pull/11183
* fix(docs): add missing script to index by @yurem in https://github.com/JanssenProject/jans/pull/11186
* fix(jans-auth-server): Access Token from and OIDC flow should not contain the code #11181 by @yuriyz in https://github.com/JanssenProject/jans/pull/11197
* docs(jans-cedarling): improve cedarling docs by @rmarinn in https://github.com/JanssenProject/jans/pull/11193
* docs(jans-cedarling): new quickstart using tarp by @SafinWasi in https://github.com/JanssenProject/jans/pull/11004
* chore: release nightly by @moabu in https://github.com/JanssenProject/jans/pull/11213
* feat(jans-auth-server): add none client authentication support to PAR endpoint #10573 by @yuriyz in https://github.com/JanssenProject/jans/pull/11201
* feat: add ability to use cedarling authz before and after authentication by @duttarnab in https://github.com/JanssenProject/jans/pull/11203
* fix(jans-cedarling)!: role entity not being created in the unsigned interface by @rmarinn in https://github.com/JanssenProject/jans/pull/11176
* fix(docs): proofread and update the Cedarling quick start guide by @ossdhaval in https://github.com/JanssenProject/jans/pull/11210
* fix(docs): proofread and update the TBAC Cedarling quick start guide by @ossdhaval in https://github.com/JanssenProject/jans/pull/11214
* fix(docs): update titles for the Cedarling quick start guides by @ossdhaval in https://github.com/JanssenProject/jans/pull/11220
* Update rhel.md for sha command update by @manojs1978 in https://github.com/JanssenProject/jans/pull/11189
* feat: refactor tarp to adjust with security changes in chrome browser by @duttarnab in https://github.com/JanssenProject/jans/pull/11232
* feat(jans-linux-setup): support for cleanUpInactiveClientAfterHoursOfInactivity for clients by @devrimyatar in https://github.com/JanssenProject/jans/pull/11231
* chore(deps): bump blazemeter/taurus from 1.16.38@sha256:5bb39436180f7c769e00140b781bb1054a1eb4592dd9b82f76dcde470811bf39 to sha256:aa22ab6b42d24ec87ea9f68e4d6db9118619eecf69db76c1c0711f3515897780 in /demos/benchmarking/docker-jans-loadtesting-jmeter by @dependabot in https://github.com/JanssenProject/jans/pull/11238
* fix(jans-cedarling): entity builder not finding the 'iss' entity by @rmarinn in https://github.com/JanssenProject/jans/pull/11235
* bug(jans-cedarling)!: Fix all tokens_metadata to token_metadata by @olehbozhok in https://github.com/JanssenProject/jans/pull/11215
* feat(jans-auth-server): add configurable way to put user claims to session attributes #9625 by @yuriyz in https://github.com/JanssenProject/jans/pull/11219
* fix: validate license configuration in the database when fetching license details from Agama Lab server upon expiry (Admin UI) by @duttarnab in https://github.com/JanssenProject/jans/pull/11245
* fix(jans-pycloudlib): resolve schema error caused by marshmallow upgrades by @iromli in https://github.com/JanssenProject/jans/pull/11247
* fix: remove ID from profile instead of nullify it by @jgomer2001 in https://github.com/JanssenProject/jans/pull/11253
* chore(cloud-native): sync jans-pycloudlib to handle marshmallow library API changes by @iromli in https://github.com/JanssenProject/jans/pull/11255
* feat: rename jans-tarp project to janssen-tarp by @duttarnab in https://github.com/JanssenProject/jans/pull/11249
* chore: updgrade nimbus so json-smart is bumped to 2.5.2 by @jgomer2001 in https://github.com/JanssenProject/jans/pull/11264
* chore(jans-cedarling): add post to cedar schema by @SafinWasi in https://github.com/JanssenProject/jans/pull/11227
* [Snyk] Security upgrade io.swagger.core.v3:swagger-core-jakarta from 2.2.7 to 2.2.11 by @mo-auto in https://github.com/JanssenProject/jans/pull/11166
* build(config-api): lib version to resolve vulnerabilities  by @pujavs in https://github.com/JanssenProject/jans/pull/11262
* feat(core): update resteasy to new version by @yurem in https://github.com/JanssenProject/jans/pull/11269
* Update resteasy by @yurem in https://github.com/JanssenProject/jans/pull/11273
* fix(jans-cedarling): fix usage outdated field resource_type by @olehbozhok in https://github.com/JanssenProject/jans/pull/11266
* fix(bom): use right jakarta.ws.rs-api version by @yurem in https://github.com/JanssenProject/jans/pull/11278
* feat(jans-cedarling): Optimize Cedar libraries size by @olehbozhok in https://github.com/JanssenProject/jans/pull/11281
* fix(jans-cedarling): fix docs bootstrap properties, remove redundant property by @olehbozhok in https://github.com/JanssenProject/jans/pull/11275
* fix(bom): deprecate jackson-module-jaxb-annotations by @yurem in https://github.com/JanssenProject/jans/pull/11286
* fix(bom): deprecate jackson-module-jaxb-annotations by @yurem in https://github.com/JanssenProject/jans/pull/11287
* fix(core): fix ClassNotFoundException XmlElement exception by @yurem in https://github.com/JanssenProject/jans/pull/11293
* fix(cloud-native): demo scripts fail to deploy cluster due to python externally-managed-environment error by @iromli in https://github.com/JanssenProject/jans/pull/11290
* feat(jans-cedarling): add some logging msgs for jwt service startup by @rmarinn in https://github.com/JanssenProject/jans/pull/11178
* feat: add field reset button in cedarling authz form by @duttarnab in https://github.com/JanssenProject/jans/pull/11260
* fix: update terraform provider docs and API calls by @moabu in https://github.com/JanssenProject/jans/pull/11301
* feat(jans-cli-tui): splash screen by @devrimyatar in https://github.com/JanssenProject/jans/pull/11298
* feat: allow linking to occur in a popup by @jgomer2001 in https://github.com/JanssenProject/jans/pull/11305
* fix(config-api): custom script creation failing #11307 by @pujavs in https://github.com/JanssenProject/jans/pull/11308
* Jans linux setup jetty 12 11276 by @devrimyatar in https://github.com/JanssenProject/jans/pull/11295
* fix(jans-cli-tui): add pylib to path before importing promptoolkit by @devrimyatar in https://github.com/JanssenProject/jans/pull/11313
* feat(jans-cedarling): go binding for cedarling by @olehbozhok in https://github.com/JanssenProject/jans/pull/11239
* fix(jans-linux-setup): jetty.sh script waits service started by @devrimyatar in https://github.com/JanssenProject/jans/pull/11319
* feat(jans-auth): add missing erver side tests dependecy by @yurem in https://github.com/JanssenProject/jans/pull/11315
* Lock api by @yurem in https://github.com/JanssenProject/jans/pull/11317
* Auth deps by @yuremm in https://github.com/JanssenProject/jans/pull/11321
* feat(jans-config-api): disable jetty ee9-jsp module in jans-config-api by @yurem in https://github.com/JanssenProject/jans/pull/11324
* chore(deps): bump setuptools from 70.0.0 to 80.1.0 in /docs by @dependabot in https://github.com/JanssenProject/jans/pull/11327
* chore(deps): bump blazemeter/taurus from 1.16.40 to 1.16.41 in /demos/benchmarking/docker-jans-loadtesting-jmeter by @dependabot in https://github.com/JanssenProject/jans/pull/11302
* chore(deps): bump docker/setup-qemu-action from 5306bad0baa6b616b9934712d4eba8da2112606d to 737ba1e397ec2caff0d098f75e1136f9a926dc0a by @dependabot in https://github.com/JanssenProject/jans/pull/11283
* chore(deps): bump actions/setup-python from 5.0.0 to 5.6.0 by @dependabot in https://github.com/JanssenProject/jans/pull/11282
* chore(deps): bump sigstore/cosign-installer from 3.5.0 to 3.8.2 by @dependabot in https://github.com/JanssenProject/jans/pull/11270
* fix(jans-fido2): #11331 minor edits by @maduvena in https://github.com/JanssenProject/jans/pull/11332
* chore: misc casa image updates by @jgomer2001 in https://github.com/JanssenProject/jans/pull/11334
* docs: add cedarling rust docs by @moabu in https://github.com/JanssenProject/jans/pull/11311
* chore(deps): bump org.quartz-scheduler:quartz from 2.3.2 to 2.5.0 in /jans-casa by @dependabot in https://github.com/JanssenProject/jans/pull/10206
* chore(deps): bump commons-io:commons-io from 2.17.0 to 2.19.0 in /jans-casa by @dependabot in https://github.com/JanssenProject/jans/pull/11217
* fix: fix the android and iOS sample app based on changes in cedarling uniffi binding by @duttarnab in https://github.com/JanssenProject/jans/pull/11294
* chore(deps): bump org.apache.maven.plugins:maven-clean-plugin from 2.5 to 3.4.1 in /jans-casa by @dependabot in https://github.com/JanssenProject/jans/pull/10903
* chore(deps): bump org.apache.maven.plugins:maven-war-plugin from 2.3 to 3.4.0 in /jans-casa by @dependabot in https://github.com/JanssenProject/jans/pull/9985
* chore(deps): bump org.apache.maven.plugins:maven-resources-plugin from 2.6 to 3.3.1 in /jans-casa by @dependabot in https://github.com/JanssenProject/jans/pull/9996
* chore(deps): bump org.apache.maven.plugins:maven-site-plugin from 2.1.1 to 3.21.0 in /jans-casa by @dependabot in https://github.com/JanssenProject/jans/pull/10064
* chore(ci): SBOM enrichment and upload as a release asset by @ossdhaval in https://github.com/JanssenProject/jans/pull/11267
* feat(cloud-native): upgrade to Jetty 12 by @iromli in https://github.com/JanssenProject/jans/pull/11297
* fix(jans-cedarling): switch cedarling instance to pointer by @SafinWasi in https://github.com/JanssenProject/jans/pull/11338
* fix(core): fix unable to decorate com.sun.faces.config.ConfigureListener by @yurem in https://github.com/JanssenProject/jans/pull/11345
* feat(jans-link): turn off Weld dev mode in production by @yuremm in https://github.com/JanssenProject/jans/pull/11347
* fix(charts): missing feature of jans-keycloak-link by @iromli in https://github.com/JanssenProject/jans/pull/11257
* fix(jans-cli-tui): include jans-logo.txt in package by @devrimyatar in https://github.com/JanssenProject/jans/pull/11359
* fix(jans-cli-tui): smtp test failing #11330 by @pujavs in https://github.com/JanssenProject/jans/pull/11358
* feat(jans-auth): restore Nashorn engine by @yurem in https://github.com/JanssenProject/jans/pull/11363
* fix: update javadocs plugin to allow generation of javadocs by @moabu in https://github.com/JanssenProject/jans/pull/11364
* chore: prepare release of 1.6.0 by @moabu in https://github.com/JanssenProject/jans/pull/11376
* fix: tf license by @moabu in https://github.com/JanssenProject/jans/pull/11382

## [1.5.0](https://github.com/JanssenProject/jans/compare/v1.4.0...v1.5.0) (2025-04-04)


### ⚠ BREAKING CHANGES

* **jans-cedarling:** move TOKEN_CONFIGS into the token_metadata schema ([#10972](https://github.com/JanssenProject/jans/issues/10972))

### Features

* add ability to decode token jwt so that users can check claims in payload ([#10930](https://github.com/JanssenProject/jans/issues/10930)) ([a88aab3](https://github.com/JanssenProject/jans/commit/a88aab32c5f6bbe5f733872df3c91798fdc163c5))
* add ability to show decision, system and metric logs in tarp based on user selection ([#10917](https://github.com/JanssenProject/jans/issues/10917)) ([4df25f0](https://github.com/JanssenProject/jans/commit/4df25f00e848fd001a8f40e80af6bbc0609b7064))
* add ability to to use cedarling authorize_unsigned in tarp ([#11147](https://github.com/JanssenProject/jans/issues/11147)) ([3185a89](https://github.com/JanssenProject/jans/commit/3185a899e5e1b289f1a562ccbf69f63ffdb11003))
* add tolerations ([03ae383](https://github.com/JanssenProject/jans/commit/03ae383d15e159cd867e554dee2cd4d455526dac))
* add tolerations to helm charts ([#11100](https://github.com/JanssenProject/jans/issues/11100)) ([03ae383](https://github.com/JanssenProject/jans/commit/03ae383d15e159cd867e554dee2cd4d455526dac))
* admin-ui should only make monthly request agama-lab to validate license ([#10966](https://github.com/JanssenProject/jans/issues/10966)) ([3647fe8](https://github.com/JanssenProject/jans/commit/3647fe8d648ee06752f5802447818e8dca91f8b4))
* apply password policy to set/reset password  ([#11149](https://github.com/JanssenProject/jans/issues/11149)) ([52f0f47](https://github.com/JanssenProject/jans/commit/52f0f475eb3a04d412a4b000f1b59a286dc7c03a))
* **charts:** cloudtools integration with Helm charts ([#11102](https://github.com/JanssenProject/jans/issues/11102)) ([5739aa3](https://github.com/JanssenProject/jans/commit/5739aa305b44fea84a36b9983e2b3b4026e2c7ab))
* **cloud-native:** create utility image to run commands for cluster ([#10987](https://github.com/JanssenProject/jans/issues/10987)) ([51e175c](https://github.com/JanssenProject/jans/commit/51e175cdbbd7ac6b6e618571d22a4a431736b78b))
* code refactoring for UI improvement in tarp ([#11151](https://github.com/JanssenProject/jans/issues/11151)) ([123e62f](https://github.com/JanssenProject/jans/commit/123e62f80076549545402217034c2b43a01b874a))
* **docs:** session management ([#11098](https://github.com/JanssenProject/jans/issues/11098)) ([d3fcf8b](https://github.com/JanssenProject/jans/commit/d3fcf8b4f8ed053f32cb94cc15a67466234eabdd))
* **jans-auth-server:** do not return claim in introspection response if it's null.  [#10865](https://github.com/JanssenProject/jans/issues/10865) ([#10877](https://github.com/JanssenProject/jans/issues/10877)) ([eb3de16](https://github.com/JanssenProject/jans/commit/eb3de16741179878486bf80e9710a3a853170805))
* **jans-auth-server:** introduce rate limit support for DCR ([#10991](https://github.com/JanssenProject/jans/issues/10991)) ([6a1ccce](https://github.com/JanssenProject/jans/commit/6a1cccea5dfa169381c73b2e6cc4dea7dee7383b))
* **jans-auth-server:** make all ssa attributes optional during creation [#10858](https://github.com/JanssenProject/jans/issues/10858) ([#10896](https://github.com/JanssenProject/jans/issues/10896)) ([a01b1ca](https://github.com/JanssenProject/jans/commit/a01b1cacc2de2d9875a7e4cd420f59c1ef9cf9fb))
* **jans-auth-server:** rate limit - use guava cache to auto-expire buckets during high load [#11054](https://github.com/JanssenProject/jans/issues/11054) ([#11059](https://github.com/JanssenProject/jans/issues/11059)) ([01a0a9f](https://github.com/JanssenProject/jans/commit/01a0a9fb4667e8e1fe2c75541fc3b9729f3a6474))
* **jans-auth-server:** removed CleanerTimer from AS (replaced by independent clean service)  ([#11096](https://github.com/JanssenProject/jans/issues/11096)) ([bb51848](https://github.com/JanssenProject/jans/commit/bb51848e3f86a544d9e7fa581d1c888bbf917e1c))
* **jans-auth-server:** requirePkce configuration property on client level ([#10962](https://github.com/JanssenProject/jans/issues/10962)) ([4cda61c](https://github.com/JanssenProject/jans/commit/4cda61c20d484c47d85fcecc5b7ee584a6386fcb))
* **jans-auth-server:** set expiration to the past (to clean up) on SSA revoke [#10916](https://github.com/JanssenProject/jans/issues/10916) ([#10939](https://github.com/JanssenProject/jans/issues/10939)) ([8c460ca](https://github.com/JanssenProject/jans/commit/8c460cad72ae4349abab9a110f5389ad95a5128d))
* **jans-cedarling:** add alias to load key policy_store_version` ([#10893](https://github.com/JanssenProject/jans/issues/10893)) ([21e4acc](https://github.com/JanssenProject/jans/commit/21e4acc54a683fa922109f42fe40e4aaa1c71aa8))
* **jans-cedarling:** add an example that runs profiling ([#10979](https://github.com/JanssenProject/jans/issues/10979)) ([dc38e10](https://github.com/JanssenProject/jans/commit/dc38e1047c5d4f1631573be90821db5471c89277))
* **jans-cedarling:** Add bootstrap properties of memory logger max_items and max_item_size ([#10924](https://github.com/JanssenProject/jans/issues/10924)) ([2189094](https://github.com/JanssenProject/jans/commit/2189094813c08635cf76cdd5e1abbb880f94731c))
* **jans-cedarling:** create uniffi binding for cedarling with sample ios app using it ([#10816](https://github.com/JanssenProject/jans/issues/10816)) ([026ed0b](https://github.com/JanssenProject/jans/commit/026ed0b765ff2a7960f2f52b7860278c564c92fc))
* **jans-cedarling:** implement loading types from other namespace ([#10807](https://github.com/JanssenProject/jans/issues/10807)) ([da05595](https://github.com/JanssenProject/jans/commit/da05595565b0f0a998cf416acbafbeace910c7ec))
* **jans-cedarling:** include a sample android app in cedarling uniffi binding ([#10994](https://github.com/JanssenProject/jans/issues/10994)) ([5b74f9d](https://github.com/JanssenProject/jans/commit/5b74f9d37769d6d2f696946785ffa270cfe20077))
* **jans-cedarling:** New interface: authorizeUnverified - send Principal as Object v. JWT ([#11035](https://github.com/JanssenProject/jans/issues/11035)) ([37b72ad](https://github.com/JanssenProject/jans/commit/37b72ad19e82258eacd616713230ca90473e62fd))
* **jans-cedarling:** Support JSON logic for principal boolean operations ([#10956](https://github.com/JanssenProject/jans/issues/10956)) ([cbe4dbc](https://github.com/JanssenProject/jans/commit/cbe4dbc61fabe63075a5f463bd92a5d05c2a21e8))
* **jans-cli-tui:** add hints to fido configuration screen ([#10864](https://github.com/JanssenProject/jans/issues/10864)) ([39ffa8a](https://github.com/JanssenProject/jans/commit/39ffa8a0757b0f9a533d1642baeaa3ad3537b089))
* **jans-cli-tui:** SSA custom attributes 10855 ([#10870](https://github.com/JanssenProject/jans/issues/10870)) ([6f2cb6b](https://github.com/JanssenProject/jans/commit/6f2cb6bb5bcbfc6e819de7f7ac3bde388ea01bd5))
* **jans-linux-setup:** data cleaning cron job task ([#10946](https://github.com/JanssenProject/jans/issues/10946)) ([33d5eb2](https://github.com/JanssenProject/jans/commit/33d5eb2e079cb8ba4f2155eb25799991235d964e))
* **jans-linux-setup:** LDAP Link installation is optional ([#10964](https://github.com/JanssenProject/jans/issues/10964)) ([673d7fb](https://github.com/JanssenProject/jans/commit/673d7fb2f7a3c5ec100508f7688811a81d3beae3))
* **jans-linux-setup:** link jans script to /usr/local/bin ([#11125](https://github.com/JanssenProject/jans/issues/11125)) ([d8c7a17](https://github.com/JanssenProject/jans/commit/d8c7a17b9bf0a1990a460ea342ebc8a8faabc1c3))
* **jans-linux-setup:** link jans script to /usr/localbin ([d8c7a17](https://github.com/JanssenProject/jans/commit/d8c7a17b9bf0a1990a460ea342ebc8a8faabc1c3))
* **jans-linux-setup:** ubuntu24 support ([#10949](https://github.com/JanssenProject/jans/issues/10949)) ([481fdca](https://github.com/JanssenProject/jans/commit/481fdcae5868e2ae95e3f79e946aba2e635f8fda))
* **jans-lock:** configure maven to update OpenAPI lock-server.yaml ([#10952](https://github.com/JanssenProject/jans/issues/10952)) ([11c2249](https://github.com/JanssenProject/jans/commit/11c2249b81b901e736145e50c35b7c9a8802e4c3))
* **jans-lock:** configure maven to update OpenAPI lock-server.yaml ([#10953](https://github.com/JanssenProject/jans/issues/10953)) ([a8f09f2](https://github.com/JanssenProject/jans/commit/a8f09f2a06510a8e2fb1b6128956077baa8c5407))


### Bug Fixes

* admin UI unable to fetch MAU _threshold on restart of config-api ([b4cfaa2](https://github.com/JanssenProject/jans/commit/b4cfaa2ae9a6ed6b71a6b450ac42c68ca95ddc60))
* **charts:** broken istio integration ([#11041](https://github.com/JanssenProject/jans/issues/11041)) ([8b536ab](https://github.com/JanssenProject/jans/commit/8b536ab21626254c6194c2876bef00b59f89cdce))
* **core:** remove jansAlias from jansDocument ([#11085](https://github.com/JanssenProject/jans/issues/11085)) ([a3a9bb2](https://github.com/JanssenProject/jans/commit/a3a9bb2828b33e4573289f0330e48a9e44500c12))
* **docs:** add Cedarling quick start guide and binding type documents ([#10906](https://github.com/JanssenProject/jans/issues/10906)) ([bc0686f](https://github.com/JanssenProject/jans/commit/bc0686f10be6eb010788128395c552e95d6543de))
* **docs:** Cedarling docs updates ([#10998](https://github.com/JanssenProject/jans/issues/10998)) ([903e29f](https://github.com/JanssenProject/jans/commit/903e29fb8ed0be70bd71b5b8de8c92bcb096e52c))
* **docs:** Cedarling documentation reorganisation ([#10900](https://github.com/JanssenProject/jans/issues/10900)) ([33920c2](https://github.com/JanssenProject/jans/commit/33920c2462703598f1c34f9aa154367ddb4dc16e))
* **docs:** incorrect package name in rhel document ([#10890](https://github.com/JanssenProject/jans/issues/10890)) ([1d98a9c](https://github.com/JanssenProject/jans/commit/1d98a9c567f6720eae087f6d2d900e8b56e66df3))
* **docs:** moving contributor license agreement to GitHub ([#11034](https://github.com/JanssenProject/jans/issues/11034)) ([6ce100a](https://github.com/JanssenProject/jans/commit/6ce100a0256c398fc859c51b9baca450feed6835))
* **docs:** remove agama lab quick start guide ([7a7ae38](https://github.com/JanssenProject/jans/commit/7a7ae3823ce6a340d3d27c80b7ac37442ababb3a))
* **docs:** remove agama lab quick start guide from Jans docs ([#11132](https://github.com/JanssenProject/jans/issues/11132)) ([7a7ae38](https://github.com/JanssenProject/jans/commit/7a7ae3823ce6a340d3d27c80b7ac37442ababb3a))
* **docs:** review and update Cedarling WASM document ([#10988](https://github.com/JanssenProject/jans/issues/10988)) ([2136bdf](https://github.com/JanssenProject/jans/commit/2136bdff3cfd126c0942ca98d82d1155cc280f81))
* **docs:** update cedarling docs navigation ([#11001](https://github.com/JanssenProject/jans/issues/11001)) ([4da085f](https://github.com/JanssenProject/jans/commit/4da085f8fba590973bc704a7481fbcf0054133e2))
* **docs:** update document titles for better search results ([#11105](https://github.com/JanssenProject/jans/issues/11105)) ([6b6815d](https://github.com/JanssenProject/jans/commit/6b6815dbf00b06b5eb697d7ca410e9db75d41fe5))
* **docs:** update titles for better docs search results ([#11131](https://github.com/JanssenProject/jans/issues/11131)) ([b9cb65e](https://github.com/JanssenProject/jans/commit/b9cb65e7f5124f17976c27de9dff5734b87e3d1b))
* error when deselect one of the tokens in the cedarling authz form ([#10986](https://github.com/JanssenProject/jans/issues/10986)) ([8444271](https://github.com/JanssenProject/jans/commit/84442715ede67e43d4f22903bd288fcf322a8c14))
* fox docs formatting ([3da3644](https://github.com/JanssenProject/jans/commit/3da3644ed21854fea20c2689f07180b2b1d57fe7))
* ignore null authz input tokens ([56af565](https://github.com/JanssenProject/jans/commit/56af5653da59bda838bf5211c57bbe568e07fa8b))
* **jans-auth-server:** correction after removing CleanerTimer from AS (replaced by independent clean service) [#10935](https://github.com/JanssenProject/jans/issues/10935) ([#11108](https://github.com/JanssenProject/jans/issues/11108)) ([13b5ea6](https://github.com/JanssenProject/jans/commit/13b5ea6ae78adbcbc375077628fc4b6eb7cea4ca))
* **jans-cedarling:** fix `CEDARLING_TOKEN_CONFIGS` to have namespace ([66dd8a7](https://github.com/JanssenProject/jans/commit/66dd8a734591067864066e6dc96105e905177d6f))
* **jans-cedarling:** fix documentation CEDARLING_POLICY_STORE_LOCAL can hold only string ([#11015](https://github.com/JanssenProject/jans/issues/11015)) ([2c6a112](https://github.com/JanssenProject/jans/commit/2c6a112c6ee50de6f7975f73f2f52950e6218f8f))
* **jans-cedarling:** fix log workload authz info ([87672d8](https://github.com/JanssenProject/jans/commit/87672d8d1f2a1c04ab1d127443e3ec333bdf256f))
* **jans-cedarling:** ignore null authz input tokens and improve error message ([#11063](https://github.com/JanssenProject/jans/issues/11063)) ([56af565](https://github.com/JanssenProject/jans/commit/56af5653da59bda838bf5211c57bbe568e07fa8b))
* **jans-cedarling:** reason missing from decision log ([#10895](https://github.com/JanssenProject/jans/issues/10895)) ([87672d8](https://github.com/JanssenProject/jans/commit/87672d8d1f2a1c04ab1d127443e3ec333bdf256f))
* **jans-cli-tui:** Catch errors when changing user password ([#11154](https://github.com/JanssenProject/jans/issues/11154)) ([9fc36c3](https://github.com/JanssenProject/jans/commit/9fc36c3233f45fb18901e1bb99ccde2f97e73b99))
* **jans-cli-tui:** multivalued claims for users ([#11011](https://github.com/JanssenProject/jans/issues/11011)) ([159b512](https://github.com/JanssenProject/jans/commit/159b5123143ab8d88e99ce803c61cf65ad0c8c9f))
* **jans-cli-tui:** null value in client grantTypes ([#10872](https://github.com/JanssenProject/jans/issues/10872)) ([a609f8c](https://github.com/JanssenProject/jans/commit/a609f8c91868c6f551f22bb4eac185858322654e))
* **jans-cli-tui:** properties acrMappings ([#11048](https://github.com/JanssenProject/jans/issues/11048)) ([f025083](https://github.com/JanssenProject/jans/commit/f025083d91fa13f31ca9f31ceaa372fe300e6ed5))
* **jans-cli-tui:** typo in editing attribute ([#11152](https://github.com/JanssenProject/jans/issues/11152)) ([f42dd4a](https://github.com/JanssenProject/jans/commit/f42dd4a9b6ba384b98266e150fe7cd731420a973))
* **jans-config-api:** admin UI unable to fetch MAU _threshold on restart of config-api ([#10969](https://github.com/JanssenProject/jans/issues/10969)) ([b4cfaa2](https://github.com/JanssenProject/jans/commit/b4cfaa2ae9a6ed6b71a6b450ac42c68ca95ddc60))
* **jans-core:** reverted back CleanerEvent (used by fido2) [#11113](https://github.com/JanssenProject/jans/issues/11113) ([#11115](https://github.com/JanssenProject/jans/issues/11115)) ([266583f](https://github.com/JanssenProject/jans/commit/266583f91563332b670ab8f083ca1b370d311e37))
* **jans-fido2:** [#10244](https://github.com/JanssenProject/jans/issues/10244) + docs ([#11057](https://github.com/JanssenProject/jans/issues/11057)) ([3912551](https://github.com/JanssenProject/jans/commit/391255150d8c1839b147046bb08be00ad7b063a3))
* **jans-fido2:** [#10947](https://github.com/JanssenProject/jans/issues/10947) syncing configuration parameters in docs and … ([#10948](https://github.com/JanssenProject/jans/issues/10948)) ([b635cbc](https://github.com/JanssenProject/jans/commit/b635cbc45db287166cc913918909d7d8192d1fed))
* **jans-fido2:** [#10947](https://github.com/JanssenProject/jans/issues/10947) syncing configuration parameters in docs and templates ([b635cbc](https://github.com/JanssenProject/jans/commit/b635cbc45db287166cc913918909d7d8192d1fed))
* **jans-fido2:** fixes to downloading mds3 blob [#11126](https://github.com/JanssenProject/jans/issues/11126) ([#11127](https://github.com/JanssenProject/jans/issues/11127)) ([3776c0c](https://github.com/JanssenProject/jans/commit/3776c0ca1b009493a23779712996aad4433d4f05))
* **jans-fido2:** U2F attestation and corrected auth_cert link [#10911](https://github.com/JanssenProject/jans/issues/10911) ([#10912](https://github.com/JanssenProject/jans/issues/10912)) ([b47ca75](https://github.com/JanssenProject/jans/commit/b47ca7565161c64ccfb4f363aa31e25940dde4e6))
* **jans-linux-setup:** lock client creation with setup.properties ([#10929](https://github.com/JanssenProject/jans/issues/10929)) ([f374ed2](https://github.com/JanssenProject/jans/commit/f374ed2f313b1c624ebde5dbef2e6da9dfca932d))
* **jans-linux-setup:** owner of service status script ([#11122](https://github.com/JanssenProject/jans/issues/11122)) ([aad24a3](https://github.com/JanssenProject/jans/commit/aad24a35c86af8b39dd15d8b186553e35fd715d0))
* **jans-linux-setup:** rename permission to role in attribute inum=6049 ([#10915](https://github.com/JanssenProject/jans/issues/10915)) ([359dc75](https://github.com/JanssenProject/jans/commit/359dc75ea52a904b6914a3f18e48fa25c0843bf7))
* **jans-tarp:** unsigned authorization form not working ([#11159](https://github.com/JanssenProject/jans/issues/11159)) ([cc9c7fd](https://github.com/JanssenProject/jans/commit/cc9c7fd921018b5685302448ae98e5c8b1cdc3b7))
* **logging:** improve detail formatting ([#10955](https://github.com/JanssenProject/jans/issues/10955)) ([9334d61](https://github.com/JanssenProject/jans/commit/9334d61e4b48a3e8d2dbeb9a6481976731a1ff90))
* return 500 instead of 404 on lookup error ([#11156](https://github.com/JanssenProject/jans/issues/11156)) ([ecc1eac](https://github.com/JanssenProject/jans/commit/ecc1eac863fc1c91daa576c852c1972f3b21ea9b))
* scope not getting added if dropdown is not used ([#10908](https://github.com/JanssenProject/jans/issues/10908)) ([63bc735](https://github.com/JanssenProject/jans/commit/63bc73501800abfdb1f14dc428f98bf823b5ccd2))
* show proper error message for when MAU threshold absent when checking flex license ([#11110](https://github.com/JanssenProject/jans/issues/11110)) ([40f9d09](https://github.com/JanssenProject/jans/commit/40f9d09357d598c11b213d5a1f86c7baf35d5732))
* show proper error message for when MAU threshold absent when checking flex license [#11093](https://github.com/JanssenProject/jans/issues/11093) ([40f9d09](https://github.com/JanssenProject/jans/commit/40f9d09357d598c11b213d5a1f86c7baf35d5732))
* ssaCustomAttributes is missing in response from Auth server Configuration endpoint ([#11120](https://github.com/JanssenProject/jans/issues/11120)) ([b4a4cf6](https://github.com/JanssenProject/jans/commit/b4a4cf6a0da0a999242224142935fdfab7970d08))


### Code Refactoring

* **jans-cedarling:** move TOKEN_CONFIGS into the token_metadata schema ([#10972](https://github.com/JanssenProject/jans/issues/10972)) ([533236a](https://github.com/JanssenProject/jans/commit/533236ae9e005ff013e0e00ea10f8d50641155c3))

## [1.4.0](https://github.com/JanssenProject/jans/compare/v1.3.0...v1.4.0) (2025-02-12)


### Features

* **agama:** add support for parameterizable enter/exit urls in flows ([#10716](https://github.com/JanssenProject/jans/issues/10716)) ([71fbcb7](https://github.com/JanssenProject/jans/commit/71fbcb7c453bebba5106dbfcca6974a3829c1da0))
* **agama:** allow integration of Agama flows in AS consent ([#10727](https://github.com/JanssenProject/jans/issues/10727)) ([4ef6c9a](https://github.com/JanssenProject/jans/commit/4ef6c9adb2e71b5b5c0dbacfc8716c452f77f2d2))
* **config-api:** lock stat endpoint and SAML TR fix ([#10755](https://github.com/JanssenProject/jans/issues/10755)) ([11bddd1](https://github.com/JanssenProject/jans/commit/11bddd1547eb8dacc240f69560c4a6deadc81121))
* **config-api:** testng framework cleanup changes ([#10736](https://github.com/JanssenProject/jans/issues/10736)) ([77d6646](https://github.com/JanssenProject/jans/commit/77d6646314503f2a14fcc1fcdac31692f10aec3a))
* **demo-tarp:** use @janssenproject/cedarling_wasm npm package [#10805](https://github.com/JanssenProject/jans/issues/10805) ([#10806](https://github.com/JanssenProject/jans/issues/10806)) ([b178d4a](https://github.com/JanssenProject/jans/commit/b178d4a7b748185671e1c77f9f89dac1da19c036))
* **jans-auth-server:** allow invoke consent script by acr [#10548](https://github.com/JanssenProject/jans/issues/10548) ([#10712](https://github.com/JanssenProject/jans/issues/10712)) ([e1982e1](https://github.com/JanssenProject/jans/commit/e1982e1d1bb332a0d16f6f80b71d43c6bba28bf4))
* **jans-cedarling:** add krakend plugin ([#10713](https://github.com/JanssenProject/jans/issues/10713)) ([78457b6](https://github.com/JanssenProject/jans/commit/78457b63f30864e2e7f9d0e332304d9872767228))
* **jans-cedarling:** custom tokens and putting tokens in principal attrs ([#10706](https://github.com/JanssenProject/jans/issues/10706)) ([c342a05](https://github.com/JanssenProject/jans/commit/c342a055b7d21a032114c9924b02f788830dc762))
* **jans-cedarling:** implement benchmarks for startup and authz ([#10775](https://github.com/JanssenProject/jans/issues/10775)) ([80f0b63](https://github.com/JanssenProject/jans/commit/80f0b63af7e8ffdab8719b57387716a507b8071e))
* **jans-cedarling:** implement environment variable loading for sidecar ([#10751](https://github.com/JanssenProject/jans/issues/10751)) ([21d8ef5](https://github.com/JanssenProject/jans/commit/21d8ef521288f013fc5e31d91c5fb6a125398aa5))
* **jans-cedarling:** Improve log searching and retrieval ([#10772](https://github.com/JanssenProject/jans/issues/10772)) ([9286f82](https://github.com/JanssenProject/jans/commit/9286f829c179545c25c39c5f8a2f984fb9a4c9ba))
* **jans-cedarling:** Load bootstrap properties from environment variables ([#10692](https://github.com/JanssenProject/jans/issues/10692)) ([d7200cb](https://github.com/JanssenProject/jans/commit/d7200cb9c82b49998f173cb9ac847a8f9ba49983))
* **jans-cedarling:** update sidecar for log retrieval ([#10786](https://github.com/JanssenProject/jans/issues/10786)) ([bd67f89](https://github.com/JanssenProject/jans/commit/bd67f89258e9c91e8382a2b1d5ed03be7b57f1c7))
* **jans-linux-setup:** restart admin-ui via jans ([#10740](https://github.com/JanssenProject/jans/issues/10740)) ([2a0aaf6](https://github.com/JanssenProject/jans/commit/2a0aaf6de1174188ad2a2581259e9e3b6253cece))
* **jans-lock:** redirect for consent if external script is enabled a… ([#10771](https://github.com/JanssenProject/jans/issues/10771)) ([c3e4a97](https://github.com/JanssenProject/jans/commit/c3e4a972254cc1a19855c8d1bf1c5fc2eaf2fe03))
* **jans-lock:** redirect for consent if external script is enabled and client is not authorized ([c3e4a97](https://github.com/JanssenProject/jans/commit/c3e4a972254cc1a19855c8d1bf1c5fc2eaf2fe03))
* update help section of tarp ([#10708](https://github.com/JanssenProject/jans/issues/10708)) ([bee683b](https://github.com/JanssenProject/jans/commit/bee683b255c2f8f0e554a20a098cbc1ab6c73b46))
* use @janssenproject/cedarling_wasm npm package [#10805](https://github.com/JanssenProject/jans/issues/10805) ([b178d4a](https://github.com/JanssenProject/jans/commit/b178d4a7b748185671e1c77f9f89dac1da19c036))


### Bug Fixes

* account absent start url in native flows ([#10731](https://github.com/JanssenProject/jans/issues/10731)) ([2c0cc62](https://github.com/JanssenProject/jans/commit/2c0cc6238544f32f022e7cd81cee927ad5c2230b))
* account absent start url in native flows [#10729](https://github.com/JanssenProject/jans/issues/10729) ([2c0cc62](https://github.com/JanssenProject/jans/commit/2c0cc6238544f32f022e7cd81cee927ad5c2230b))
* align consent script wrt latest AS updates ([#10780](https://github.com/JanssenProject/jans/issues/10780)) ([6cb802b](https://github.com/JanssenProject/jans/commit/6cb802b5ab67f074a31bc5cb12b9dc20e3e79ab8))
* **config-api:** asset upload config and saml document store changes ([#10734](https://github.com/JanssenProject/jans/issues/10734)) ([8e9d43e](https://github.com/JanssenProject/jans/commit/8e9d43e1c33f4c7a7b8b705e2d04c50330eff3b1))
* Corrected fido-2-devices link to heading in user management document. issue[#9636](https://github.com/JanssenProject/jans/issues/9636). ([8ffc483](https://github.com/JanssenProject/jans/commit/8ffc483369fd7e480eab3076dd024b02e9976353))
* **docs:** corrected fido-2-devices link to heading in user management document ([8ffc483](https://github.com/JanssenProject/jans/commit/8ffc483369fd7e480eab3076dd024b02e9976353))
* **docs:** update Agama Lab quick start guide ([#10779](https://github.com/JanssenProject/jans/issues/10779)) ([7403ed1](https://github.com/JanssenProject/jans/commit/7403ed18fdeee2516d14eafbd33d08f030368e30))
* **docs:** update broken link in custom claims document ([#10781](https://github.com/JanssenProject/jans/issues/10781)) ([40b7039](https://github.com/JanssenProject/jans/commit/40b70391f429531bf0f6ee657ba9345796479bf1))
* **docs:** update LDAP link document with configuration details ([#10824](https://github.com/JanssenProject/jans/issues/10824)) ([53dd493](https://github.com/JanssenProject/jans/commit/53dd49394ac9452a058ed9abe84022120a725e9e))
* **jans-auth-server:** always save access_token to persistence regardless of cache configuration [#10763](https://github.com/JanssenProject/jans/issues/10763) ([#10784](https://github.com/JanssenProject/jans/issues/10784)) ([cf8ce67](https://github.com/JanssenProject/jans/commit/cf8ce67a20b3ce4f6339fe1cb94209ed84d9b53c))
* **jans-auth-server:** second authorization challenge call does not invoke the right script [#10745](https://github.com/JanssenProject/jans/issues/10745) ([#10746](https://github.com/JanssenProject/jans/issues/10746)) ([3ba585c](https://github.com/JanssenProject/jans/commit/3ba585c33e8cb665a365c6dc14805d33bbf373e7))
* **jans-auth-server:** typo in determineConsentFlow method [#10758](https://github.com/JanssenProject/jans/issues/10758) ([#10759](https://github.com/JanssenProject/jans/issues/10759)) ([52cabdb](https://github.com/JanssenProject/jans/commit/52cabdb5e12c1d4a5364502ad9e836b7c5be33a5))
* **jans-cedarling:** fix token handling ([#10761](https://github.com/JanssenProject/jans/issues/10761)) ([2533e30](https://github.com/JanssenProject/jans/commit/2533e3086edacbd70b4d0f49950d7e3cf2ff64ec))
* **jans-cli-tui:** save config-api configuration ([#10724](https://github.com/JanssenProject/jans/issues/10724)) ([6a955a4](https://github.com/JanssenProject/jans/commit/6a955a4c0c7355d05d5fac95c965dd4a504e003b))
* **jans-cli-tui:** ScrollablePane for config-api main screen ([#10722](https://github.com/JanssenProject/jans/issues/10722)) ([4585f3a](https://github.com/JanssenProject/jans/commit/4585f3abd1f97e47009969c97bd8ca68d26aec26))
* **jans-linux-setup:** fido document store paths ([#10801](https://github.com/JanssenProject/jans/issues/10801)) ([881c1e0](https://github.com/JanssenProject/jans/commit/881c1e0818a0aed6b413280ee7d85bd02d92bbb6))
* **jans-linux-setup:** revert admin-ui restart ([27faefc](https://github.com/JanssenProject/jans/commit/27faefc4f8d9497adb853cce542997ac0733bd30))
* **jans-linux-setup:** Store fido2 authenticator_cert in DB ([#10697](https://github.com/JanssenProject/jans/issues/10697)) ([86966df](https://github.com/JanssenProject/jans/commit/86966df52d45d4963e04dcdbfc39a09dc9f81a4b))
* **jans-linux-setup:** update jansservices module post setup ([#10715](https://github.com/JanssenProject/jans/issues/10715)) ([cfa301f](https://github.com/JanssenProject/jans/commit/cfa301f40fc63ebcfa139fdb8a849d2c61f0a98e))
* references to file location_type ([#10797](https://github.com/JanssenProject/jans/issues/10797)) ([cfaab7e](https://github.com/JanssenProject/jans/commit/cfaab7e276192f677185b17cd77ff050ab82a03c))
* update resource endpoints for terraform ([#10836](https://github.com/JanssenProject/jans/issues/10836)) ([5dfee2b](https://github.com/JanssenProject/jans/commit/5dfee2bf40206cf8c5928723a5afb3d5f620807a))

## [1.3.0](https://github.com/JanssenProject/jans/compare/v1.2.0...v1.3.0) (2025-01-20)


### Features

* **agama:** modify RRF and RFAC behavior for non-web clients ([#10547](https://github.com/JanssenProject/jans/issues/10547)) ([58fd359](https://github.com/JanssenProject/jans/commit/58fd3598777a31b5a4f7b7be3fac31a9f2131268))
* allow integration of Agama flows into the authz challenge enpoint ([#10587](https://github.com/JanssenProject/jans/issues/10587)) ([856f9fe](https://github.com/JanssenProject/jans/commit/856f9fed1d58a6d41503a0459bbe04f52b0bb8e7))
* cedarling integration with tarp ([#10681](https://github.com/JanssenProject/jans/issues/10681)) ([4f44337](https://github.com/JanssenProject/jans/commit/4f443370153a074bf87834f44ba068266b2b9792))
* **cloud-native:** secure mounted configuration schema ([#10577](https://github.com/JanssenProject/jans/issues/10577)) ([57c266a](https://github.com/JanssenProject/jans/commit/57c266af928186f45c8d346ce0f7c8c0baba01b1))
* extract wasm tar in /jans-tarp folder ([#10689](https://github.com/JanssenProject/jans/issues/10689)) ([ca8f453](https://github.com/JanssenProject/jans/commit/ca8f45311747b645ac70458ff2dba4664abf47b7))
* integrate cedarling with jans-tarp ([#10662](https://github.com/JanssenProject/jans/issues/10662)) ([9f9ae8a](https://github.com/JanssenProject/jans/commit/9f9ae8a9d09f9da9cd6b02e07a591132604ee857))
* **jans-auth-server:** introduced new 'prepareAuthzRequest' method in authorization challenge script ([#10598](https://github.com/JanssenProject/jans/issues/10598)) ([02c240e](https://github.com/JanssenProject/jans/commit/02c240effba327358a3d2781ddd91b72213d6604))
* **jans-auth:** log current folder in UserJansExtUidAttributeTest test ([ca79ace](https://github.com/JanssenProject/jans/commit/ca79acec74cef8aeaa9274d3ce3a79657de755af))
* **jans-auth:** log current folder in UserJansExtUidAttributeTest test ([#10667](https://github.com/JanssenProject/jans/issues/10667)) ([51ce4dc](https://github.com/JanssenProject/jans/commit/51ce4dcfb10c903f81a4fc0310218c4264d7d0cf))
* **jans-auth:** Remove copyright footer ([#10666](https://github.com/JanssenProject/jans/issues/10666)) ([474661c](https://github.com/JanssenProject/jans/commit/474661cd100704592e7af7736330ed5aec877d96))
* **jans-auth:** Show valid client name or id in consent form ([#10649](https://github.com/JanssenProject/jans/issues/10649)) ([5a53d53](https://github.com/JanssenProject/jans/commit/5a53d53b50dd0b559254a7008b5db69e08e64f1b))
* **jans-cedarling:** add to decision log diagnostic info ([#10581](https://github.com/JanssenProject/jans/issues/10581)) ([6f8dc7c](https://github.com/JanssenProject/jans/commit/6f8dc7c08a0b3d810e2336abc7fe370fdd1f7147))
* **jans-cedarling:** add WASM bindings for Cedarling ([#10542](https://github.com/JanssenProject/jans/issues/10542)) ([ec7c7e1](https://github.com/JanssenProject/jans/commit/ec7c7e186c4b2508a53fe1a7666a4e4023829489))
* **jans-cedarling:** implement CEDARLING_ID_TOKEN_TRUST_MODE ([#10585](https://github.com/JanssenProject/jans/issues/10585)) ([d76f28c](https://github.com/JanssenProject/jans/commit/d76f28c64109a9f347058be2fa268abdca1d69e9))
* **jans-cedarling:** Make SparKV use generics, and update MemoryLogger to use those. ([#10593](https://github.com/JanssenProject/jans/issues/10593)) ([25c7a49](https://github.com/JanssenProject/jans/commit/25c7a49c4f41c5750d24950511909db57226dda6))
* **jans-fido2:** Add test cases for RP domain origin validation and handle multiple origins [#9248](https://github.com/JanssenProject/jans/issues/9248) ([22f0cbe](https://github.com/JanssenProject/jans/commit/22f0cbe4bdc41888c6caa5efa1ab249c6fc47298))
* **jans-fido2:** Add test cases for RP domain origin. ([#10572](https://github.com/JanssenProject/jans/issues/10572)) ([22f0cbe](https://github.com/JanssenProject/jans/commit/22f0cbe4bdc41888c6caa5efa1ab249c6fc47298))
* **jans-lock:** lock should collect MAU and MAC based on log entries… ([#10328](https://github.com/JanssenProject/jans/issues/10328)) ([b8a7e1a](https://github.com/JanssenProject/jans/commit/b8a7e1a493a2e7059a8e220eb6ff4305866bfc70))
* **jans-pycloudlib:** secure mounted configuration schema ([#10551](https://github.com/JanssenProject/jans/issues/10551)) ([2d27184](https://github.com/JanssenProject/jans/commit/2d27184ac81c57596b527143c0a60fec6761cf02))


### Bug Fixes

* **actions:** immutable github sha  instead of github head_ref ([5091b56](https://github.com/JanssenProject/jans/commit/5091b56102be0dd1d683d74703b8352c8cb27693))
* **agama:** update expected status code ([#10618](https://github.com/JanssenProject/jans/issues/10618)) ([c0dce75](https://github.com/JanssenProject/jans/commit/c0dce7530cf3dd304238b6d93ded693a5c3b81b5))
* build acct linking agama ([#10575](https://github.com/JanssenProject/jans/issues/10575)) ([85b95ec](https://github.com/JanssenProject/jans/commit/85b95ec91f17ca26964558bb085a5bbc9aad13d0))
* **cloud-native:** add missing endpoints to aio image ([#10595](https://github.com/JanssenProject/jans/issues/10595)) ([5fb1903](https://github.com/JanssenProject/jans/commit/5fb1903f5f5f49b188b6daf6c194ffb3ecadfd38))
* **cloud-native:** resolve image builds on slow network ([#10524](https://github.com/JanssenProject/jans/issues/10524)) ([3409098](https://github.com/JanssenProject/jans/commit/3409098777f8696d7a6485e0ae6b226f55cbb6ec))
* **config-api:** setting agama flow as auth method ([#10539](https://github.com/JanssenProject/jans/issues/10539)) ([3c00152](https://github.com/JanssenProject/jans/commit/3c0015224aec4a2333735478333496baa31ef9b6))
* **docker-jans-auth:** missing permissions on /app/templates ([#10641](https://github.com/JanssenProject/jans/issues/10641)) ([f1b3ca2](https://github.com/JanssenProject/jans/commit/f1b3ca2294f4eb56bcde3f78a4f6b05f0bb879d7))
* **docker-jans-persistence-loader:** exclude external tables when creating indexes ([#10522](https://github.com/JanssenProject/jans/issues/10522)) ([9610bc1](https://github.com/JanssenProject/jans/commit/9610bc15908331e8344dfaed16ee8a397bd999d5))
* **docs:** add documentation for `jans` wrapper command ([#10611](https://github.com/JanssenProject/jans/issues/10611)) ([b65f5e1](https://github.com/JanssenProject/jans/commit/b65f5e109bfc41fa6bc39da8466b4b94c6020788))
* **docs:** correct the file name for Keycloak link document ([#10680](https://github.com/JanssenProject/jans/issues/10680)) ([35e6ef0](https://github.com/JanssenProject/jans/commit/35e6ef01e8a8c330604249a4fe415d50dea4cf4f))
* **docs:** minor fixes to the Jans README ([#10604](https://github.com/JanssenProject/jans/issues/10604)) ([41bf8b8](https://github.com/JanssenProject/jans/commit/41bf8b892bc69c665a4e27b265de11eceab0ea6d))
* **docs:** minor URL fixes ([ba9908d](https://github.com/JanssenProject/jans/commit/ba9908dea395a9a8974e55a4cf9a079749bfcda3))
* **docs:** minor URL fixes ([#10632](https://github.com/JanssenProject/jans/issues/10632)) ([ba9908d](https://github.com/JanssenProject/jans/commit/ba9908dea395a9a8974e55a4cf9a079749bfcda3))
* **docs:** remove tent references ([040ff17](https://github.com/JanssenProject/jans/commit/040ff17942019bc10433ce17d819b8d8474f13c8))
* **docs:** remove tent references from documentation ([#10603](https://github.com/JanssenProject/jans/issues/10603)) ([040ff17](https://github.com/JanssenProject/jans/commit/040ff17942019bc10433ce17d819b8d8474f13c8))
* implement missing method from interface ([#10646](https://github.com/JanssenProject/jans/issues/10646)) ([2381a09](https://github.com/JanssenProject/jans/commit/2381a09cd7a51f2582c041bcab4941d7c5138696))
* **jans-auth-server:** access evaluation tests are failing on jenkins ([#10630](https://github.com/JanssenProject/jans/issues/10630)) ([8789289](https://github.com/JanssenProject/jans/commit/87892899b455b009d1493ef47b7ab7ae8dccb69b))
* **jans-auth-server:** access evaluation tests are failing on jenkins [#10629](https://github.com/JanssenProject/jans/issues/10629) ([37e177c](https://github.com/JanssenProject/jans/commit/37e177c1de0f3efd4dabc14da0bbd6fef3072d62))
* **jans-auth-server:** access evaluation tests are failing on jenkins [#10629](https://github.com/JanssenProject/jans/issues/10629) ([8789289](https://github.com/JanssenProject/jans/commit/87892899b455b009d1493ef47b7ab7ae8dccb69b))
* **jans-auth-server:** challenge endpoint returns 400 if authorize throws an unexpected exception ([#10553](https://github.com/JanssenProject/jans/issues/10553)) ([02c3df7](https://github.com/JanssenProject/jans/commit/02c3df77be977248529ccfc23145a37049e12633))
* **jans-auth-server:** failing test - SelectAccountHttpTest selectAccountTest [#10647](https://github.com/JanssenProject/jans/issues/10647) ([d19e34f](https://github.com/JanssenProject/jans/commit/d19e34f943a34cf7ed2fa5f5ece7e17c97eaa5a2))
* **jans-auth-server:** NPE during client name rendering [#10663](https://github.com/JanssenProject/jans/issues/10663)  ([9dbcb0d](https://github.com/JanssenProject/jans/commit/9dbcb0dead119e2bf780bda9153b84c8ce379266))
* **jans-auth-server:** test is failing - TokenRestWebServiceHttpTest requestAccessTokenFail [#10637](https://github.com/JanssenProject/jans/issues/10637) ([db38009](https://github.com/JanssenProject/jans/commit/db38009d97a29b57e9b180c8b1e7314fa4edb5f2))
* **jans-auth-server:** tests corrections ([ef8a07a](https://github.com/JanssenProject/jans/commit/ef8a07aced8eb7eafaac1bda7f36d26a6909bd85))
* **jans-auth:** log current folder in UserServiceTest test ([#10675](https://github.com/JanssenProject/jans/issues/10675)) ([1468b47](https://github.com/JanssenProject/jans/commit/1468b477b4b7956b731930ddb0381513b47d17a8))
* **jans-cedarling:** fix Cedarling WASM docs ([#10601](https://github.com/JanssenProject/jans/issues/10601)) ([7690030](https://github.com/JanssenProject/jans/commit/76900307ca0fab78a981c517585499f7b75685e0))
* **jans-kc-link:** remove default keycloak configs ([#10679](https://github.com/JanssenProject/jans/issues/10679)) ([261c936](https://github.com/JanssenProject/jans/commit/261c936acf732fa249231ff65193de68a47558ca))
* **jans-keycloak-link:** unstatisfied dependencies ([#10627](https://github.com/JanssenProject/jans/issues/10627)) ([721b8fe](https://github.com/JanssenProject/jans/commit/721b8fe5b68e23988ca298bb06cc405091945f67))
* **startjanssendemo:** enhance the script ([1ba8e98](https://github.com/JanssenProject/jans/commit/1ba8e9883ec7e1bcf1aa9b57bbe100211edfadb2))
* **startjanssendemo:** remove unneeded code and packages installation ([#10531](https://github.com/JanssenProject/jans/issues/10531)) ([1ba8e98](https://github.com/JanssenProject/jans/commit/1ba8e9883ec7e1bcf1aa9b57bbe100211edfadb2))
* update token script (role_based_scopes_update_token) should reje… ([#10536](https://github.com/JanssenProject/jans/issues/10536)) ([3cd5d88](https://github.com/JanssenProject/jans/commit/3cd5d88af2bf4850779b4107d939b97e1e79624b))
* update token script (role_based_scopes_update_token) should reject the tampered user-info-jwt [#10535](https://github.com/JanssenProject/jans/issues/10535) ([3cd5d88](https://github.com/JanssenProject/jans/commit/3cd5d88af2bf4850779b4107d939b97e1e79624b))

## [1.2.0](https://github.com/JanssenProject/jans/compare/v1.1.6...v1.2.0) (2024-12-24)


### Features

* add internationalization in selector page ([#10405](https://github.com/JanssenProject/jans/issues/10405)) ([00facf6](https://github.com/JanssenProject/jans/commit/00facf64cef5a961e9cca0fa1d9e8c7a528f6043))
* add programatic access to labels in Agama ([#10313](https://github.com/JanssenProject/jans/issues/10313)) ([1e91d9b](https://github.com/JanssenProject/jans/commit/1e91d9ba8910dcbbbf20bb16d1c6645177344c93))
* **config-api:** agama download endpoint ([#10463](https://github.com/JanssenProject/jans/issues/10463)) ([5bec96c](https://github.com/JanssenProject/jans/commit/5bec96c41d2484955ef8a95a01921e99b2ffa5fb))
* **config-api:** implemenetd agama repo endpoint and fixed user pwd validation for patch ([#10373](https://github.com/JanssenProject/jans/issues/10373)) ([03d3529](https://github.com/JanssenProject/jans/commit/03d3529d31aa807be809fca4e842aa593e74d37f))
* **jans-agama:** update htmlunit ([#10464](https://github.com/JanssenProject/jans/issues/10464)) ([3cc7c5a](https://github.com/JanssenProject/jans/commit/3cc7c5a34a23d34ea1fe23c7a2003ab670c8907a))
* **jans-auth-server:** access token lifetime from UpdateToken interception script has highest priority [#9748](https://github.com/JanssenProject/jans/issues/9748) ([#10379](https://github.com/JanssenProject/jans/issues/10379)) ([c2ef55d](https://github.com/JanssenProject/jans/commit/c2ef55db4bb9208820818996267961e1870eb0f0))
* **jans-auth-server:** added exp,nbf, and iat to UserInfo JWT ([#10390](https://github.com/JanssenProject/jans/issues/10390)) ([c99a71a](https://github.com/JanssenProject/jans/commit/c99a71abadd5665429d0ef0e56fd26dd05c54df4))
* **jans-auth-server:** allow to use openidSubAttribute for localAccountId for pairwise identifier look up [#9696](https://github.com/JanssenProject/jans/issues/9696) ([#10269](https://github.com/JanssenProject/jans/issues/10269)) ([5d72a06](https://github.com/JanssenProject/jans/commit/5d72a06d8834799a778c717fa6d8ae114bcb744e))
* **jans-auth-server:** introduced `/.well-known/authzen-configuration` endpoint ([#10321](https://github.com/JanssenProject/jans/issues/10321)) ([efb7ab6](https://github.com/JanssenProject/jans/commit/efb7ab68d72207770441560ef71ca8917e9ca3e4))
* **jans-auth-server:** updated first party native authn implementation ( in backwards compatibility way) [#10380](https://github.com/JanssenProject/jans/issues/10380) ([#10442](https://github.com/JanssenProject/jans/issues/10442)) ([bc431fb](https://github.com/JanssenProject/jans/commit/bc431fb58e32996a4b6b8152f4e51674ddb79c1f))
* **jans-cedarling:** add logging `cedarling` version on start application ([#10288](https://github.com/JanssenProject/jans/issues/10288)) ([20ed173](https://github.com/JanssenProject/jans/commit/20ed1733a8ae7baa88f4c644caa08f81b3059f8e))
* **jans-cedarling:** add support for Cedar schema action introspection ([#10358](https://github.com/JanssenProject/jans/issues/10358)) ([ed0edb9](https://github.com/JanssenProject/jans/commit/ed0edb9b3e10329ee5755301c2545ee1981e6c53))
* **jans-cedarling:** add well-known authzen configuration endpoint ([#10435](https://github.com/JanssenProject/jans/issues/10435)) ([cc6fc7b](https://github.com/JanssenProject/jans/commit/cc6fc7b33711f087da5524d045046eea4a6f660f))
* **jans-cedarling:** automatically add entity references into the context ([#10387](https://github.com/JanssenProject/jans/issues/10387)) ([ed44ec0](https://github.com/JanssenProject/jans/commit/ed44ec0908d4fab095334a4292a36f16780fffe2))
* **jans-cedarling:** Bootstrap support for JSON and YAML properties ([#10216](https://github.com/JanssenProject/jans/issues/10216)) ([e7ffb08](https://github.com/JanssenProject/jans/commit/e7ffb080972e434abc0f70c73da42cefece34a0c))
* **jans-cedarling:** implement loading policy store from CEDARLING_POLICY_STORE_URI ([#10336](https://github.com/JanssenProject/jans/issues/10336)) ([ffe9f49](https://github.com/JanssenProject/jans/commit/ffe9f493e4a5c6b05f2adeeb8a6eba7eb83b103e))
* **jans-cedarling:** implement loading role from many JWT tokens ([#10422](https://github.com/JanssenProject/jans/issues/10422)) ([8da040e](https://github.com/JanssenProject/jans/commit/8da040efee2aa7bcd338c9e5441b4d081393e563))
* **jans-cedarling:** implement new bootstrap configs for JWT validation ([#10306](https://github.com/JanssenProject/jans/issues/10306)) ([6d810a5](https://github.com/JanssenProject/jans/commit/6d810a53b1be7872952fae316648422ec22c9754))
* **jans-cedarling:** initialize flask sidecar ([#10270](https://github.com/JanssenProject/jans/issues/10270)) ([46f9a51](https://github.com/JanssenProject/jans/commit/46f9a51ee4b71d94f7e8c4d7e99d959c35510a89))
* **jans-cedarling:** pass entities data into the context ([#10275](https://github.com/JanssenProject/jans/issues/10275)) ([e2e4f89](https://github.com/JanssenProject/jans/commit/e2e4f89a4f41e2fb3fe0c0410516b17c653411ee))
* **jans-cli-tui:** user fido devices ([#10305](https://github.com/JanssenProject/jans/issues/10305)) ([811d953](https://github.com/JanssenProject/jans/commit/811d9533e4ac657637f0c468aa900c4e28aab0fd))
* **jans-fido2:** major FIDO2 / Passkeys upgrade ProjectPasskeys ([#10080](https://github.com/JanssenProject/jans/issues/10080)) ([e823bf7](https://github.com/JanssenProject/jans/commit/e823bf79ac18f80375068232144b87c17d9318c4))
* **jans-link:** add ingress resource for jans-link ([#10494](https://github.com/JanssenProject/jans/issues/10494)) ([2779a7e](https://github.com/JanssenProject/jans/commit/2779a7e70e23be1c0afc810abd27910c60fcd9b1))
* **jans-linux-setup:** jans-fido2-model auth lib ([#10468](https://github.com/JanssenProject/jans/issues/10468)) ([f99d870](https://github.com/JanssenProject/jans/commit/f99d870718b6860ee2bea42867aae0f42f44ee00))
* **jans-linux-setup:** location of service scripts, config-api plugins ([#10341](https://github.com/JanssenProject/jans/issues/10341)) ([7299fea](https://github.com/JanssenProject/jans/commit/7299fea12ec69cdf98851390de57278485b2365b))
* **jans-linux-setup:** Retreive Agama Lab project scripts ([#10335](https://github.com/JanssenProject/jans/issues/10335)) ([26713a8](https://github.com/JanssenProject/jans/commit/26713a82b14a67d5e65b9a7e72d6f1403314f679))
* migrate and fix e-mail otp plugin ([#10294](https://github.com/JanssenProject/jans/issues/10294)) ([d3f83cb](https://github.com/JanssenProject/jans/commit/d3f83cbf710de30173cf79830416748d067dc0a1))
* misc UI updates ([#10278](https://github.com/JanssenProject/jans/issues/10278)) ([c0a6639](https://github.com/JanssenProject/jans/commit/c0a6639f8a5a22d1ba678e3e06cbbab8f98168aa))
* **terraform-provider-jans:** update terraform provider with latest API changes ([#10485](https://github.com/JanssenProject/jans/issues/10485)) ([075650c](https://github.com/JanssenProject/jans/commit/075650ce542472a3f84475304b994aaf35f41afe))


### Bug Fixes

* **actions:** microk8s action passing correct arguments ([#10363](https://github.com/JanssenProject/jans/issues/10363)) ([a1517a0](https://github.com/JanssenProject/jans/commit/a1517a08767d08604ee937d07c8f764701a317dc))
* assign nightly version ([2dd3484](https://github.com/JanssenProject/jans/commit/2dd3484f7d25dccb038275a7953d1c6e7b61128f))
* **bom:** deprecate commons-lang due to conflict with commons-lang3 ([#10267](https://github.com/JanssenProject/jans/issues/10267)) ([396551e](https://github.com/JanssenProject/jans/commit/396551ec723b18eb02a7f0c69740fa69c3e2d5de))
* bug if version passed is a tag ([82694ca](https://github.com/JanssenProject/jans/commit/82694caa17afea9decb2b0858ccd8b18a79af647))
* **cloud-native:** applications are failing to start when prometheus metrics are enabled ([#10459](https://github.com/JanssenProject/jans/issues/10459)) ([b293ebe](https://github.com/JanssenProject/jans/commit/b293ebed1bb233a61553959350af8af150d72ce5))
* **config-api:** application status endpoint specification changes ([#10203](https://github.com/JanssenProject/jans/issues/10203)) ([c49a0af](https://github.com/JanssenProject/jans/commit/c49a0af6f74b87f75e060afc177878e0148edd9c))
* **config-api:** user password being displayed as clear text ([#10441](https://github.com/JanssenProject/jans/issues/10441)) ([4e7c13b](https://github.com/JanssenProject/jans/commit/4e7c13b3b64ddaa6feb748ff450df9baf10de81f))
* **config-api:** user password patch fix  ([#10396](https://github.com/JanssenProject/jans/issues/10396)) ([0345f11](https://github.com/JanssenProject/jans/commit/0345f11fa39a0c0bb9783f606a10f5575aa4d68c))
* **docker-jans-config-api:** resolve path to external healthcheck script ([#10450](https://github.com/JanssenProject/jans/issues/10450)) ([bef11a6](https://github.com/JanssenProject/jans/commit/bef11a6d47849ab0342bd5893badb9bd5bf05204))
* **docker-jans-monolith:** update scripts location ([#10481](https://github.com/JanssenProject/jans/issues/10481)) ([45fe7a5](https://github.com/JanssenProject/jans/commit/45fe7a5c9c85443a5154bb2bd497db55f84dcb52))
* **docs:** autogenerate docs ([#10232](https://github.com/JanssenProject/jans/issues/10232)) ([50e5957](https://github.com/JanssenProject/jans/commit/50e5957a1fc3782ca142132834ea2ef0e73dc64f))
* **jans-auth-server:** lower possibility to get data loss during status index pool update [#10284](https://github.com/JanssenProject/jans/issues/10284) ([#10285](https://github.com/JanssenProject/jans/issues/10285)) ([16371ee](https://github.com/JanssenProject/jans/commit/16371ee8076581bf9989e55005d3f1db141faddb))
* **jans-auth:** fido - [#10445](https://github.com/JanssenProject/jans/issues/10445) modified the script and properties file for fido ([#10446](https://github.com/JanssenProject/jans/issues/10446)) ([99285e9](https://github.com/JanssenProject/jans/commit/99285e94fcc81dd6a391e09d13901050883becd2))
* **jans-auth:** fix client side jans-auth tests failures [#10212](https://github.com/JanssenProject/jans/issues/10212) ([#10213](https://github.com/JanssenProject/jans/issues/10213)) ([c0bc881](https://github.com/JanssenProject/jans/commit/c0bc881c311dd3645bb48f78dcd1ce30a728fc94))
* **jans-casa:** [#10470](https://github.com/JanssenProject/jans/issues/10470) Enrollment of a passkey implies the enrollment of all three types of authenticator - client-device, hybrid, security-key ([b0a7da3](https://github.com/JanssenProject/jans/commit/b0a7da353d4ef298e5216ed75148da66b2d6b411))
* **jans-casa:** assign nightly version for email 2fa plugin ([#10300](https://github.com/JanssenProject/jans/issues/10300)) ([2dd3484](https://github.com/JanssenProject/jans/commit/2dd3484f7d25dccb038275a7953d1c6e7b61128f))
* **jans-casa:** enrollment of a passkey implies the enrollment… ([#10473](https://github.com/JanssenProject/jans/issues/10473)) ([b0a7da3](https://github.com/JanssenProject/jans/commit/b0a7da353d4ef298e5216ed75148da66b2d6b411))
* **jans-cedarling:** add handling nonexistent authorization decisions ([#10431](https://github.com/JanssenProject/jans/issues/10431)) ([29d9bc6](https://github.com/JanssenProject/jans/commit/29d9bc60dfe0ce6c1107f15cbb263a868f0c98e9))
* **jans-cedarling:** add missing fields on LogEntry struct ([#10297](https://github.com/JanssenProject/jans/issues/10297)) ([b91279f](https://github.com/JanssenProject/jans/commit/b91279fbf187c072e3c70a18367943fb01085390))
* **jans-cedarling:** fix sidecar docker ([#10361](https://github.com/JanssenProject/jans/issues/10361)) ([48e8eae](https://github.com/JanssenProject/jans/commit/48e8eaedcfa13631af78bf2800e1435c035aa461))
* **jans-cedarling:** revert "pass entities data into the context" ([#10290](https://github.com/JanssenProject/jans/issues/10290)) ([5e10625](https://github.com/JanssenProject/jans/commit/5e10625863ab6d973abf12bde6987b386fa2c9fb))
* **jans-cedarling:** update example authorize_without_jwt_validation ([#10308](https://github.com/JanssenProject/jans/issues/10308)) ([ccb376c](https://github.com/JanssenProject/jans/commit/ccb376cf189b7841d8040093f1f9274884cad309))
* **jans-cli-tui:** display error for session search ([#10251](https://github.com/JanssenProject/jans/issues/10251)) ([87a6c39](https://github.com/JanssenProject/jans/commit/87a6c39da75a73498ffb736852c8493bfb46ceae))
* **jans-cli-tui:** properties object with no keys ([#10411](https://github.com/JanssenProject/jans/issues/10411)) ([e0f55a0](https://github.com/JanssenProject/jans/commit/e0f55a09f73deb3b53a7c862eaac73a4b6e580fb))
* **jans-cli-tui:** update user password ([#10456](https://github.com/JanssenProject/jans/issues/10456)) ([927befc](https://github.com/JanssenProject/jans/commit/927befcf0cf16ffc96436f2317bf7f7fd70a1f65))
* **jans-config-api:** unable to update Admin-ui feature in webhook ([#10220](https://github.com/JanssenProject/jans/issues/10220)) ([1244488](https://github.com/JanssenProject/jans/commit/12444884d09afe3528280d7ddb769d6b53abaecd))
* **jans-fido:** Resolve dependecy issue  ([a71e866](https://github.com/JanssenProject/jans/commit/a71e866b621ac7bd9ca2165c9d37dd2a872675ea))
* **jans-fido:** Resolve dependecy issue [#10080](https://github.com/JanssenProject/jans/issues/10080) ([#10406](https://github.com/JanssenProject/jans/issues/10406)) ([b6b45e1](https://github.com/JanssenProject/jans/commit/b6b45e194cd966de452ba1c9745929c3197b0910))
* **jans-keycloak-integration:** kc startup issues [#10348](https://github.com/JanssenProject/jans/issues/10348) ([#10349](https://github.com/JanssenProject/jans/issues/10349)) ([ecd8e38](https://github.com/JanssenProject/jans/commit/ecd8e383d424fdb67b6b8b137319d8a1bb3d122c))
* **jans-linux-setup:** auth server test configuration ([#10365](https://github.com/JanssenProject/jans/issues/10365)) ([e68d275](https://github.com/JanssenProject/jans/commit/e68d275414904b5c0694dda9f87b18f47a02b852))
* **jans-linux-setup:** bug if version passed is a tag ([#10274](https://github.com/JanssenProject/jans/issues/10274)) ([82694ca](https://github.com/JanssenProject/jans/commit/82694caa17afea9decb2b0858ccd8b18a79af647))
* **jans-linux-setup:** config-api plugin installation ([#10389](https://github.com/JanssenProject/jans/issues/10389)) ([59ba1b0](https://github.com/JanssenProject/jans/commit/59ba1b08c4e85d2f1e28cbe8dcc08357a658a16a))
* **jans-linux-setup:** display CLI logs in jans script ([#10262](https://github.com/JanssenProject/jans/issues/10262)) ([ef0f4fe](https://github.com/JanssenProject/jans/commit/ef0f4fe0c37c7cefc3d96440e1690d919cd15334))
* **jans-linux-setup:** download from tags ([#10391](https://github.com/JanssenProject/jans/issues/10391)) ([c6d95a1](https://github.com/JanssenProject/jans/commit/c6d95a18e1e169005dad7174f2afd4fd2e16b5ea))
* **jans-orm:** merge ORM changes from Gluu ([#10293](https://github.com/JanssenProject/jans/issues/10293)) ([17e9443](https://github.com/JanssenProject/jans/commit/17e94436c841ec357c796364813f42318ca4cb8f))
* **monolithic:** update scripts location ([45fe7a5](https://github.com/JanssenProject/jans/commit/45fe7a5c9c85443a5154bb2bd497db55f84dcb52))

## [1.1.6](https://github.com/JanssenProject/jans/compare/v1.1.5...v1.1.6) (2024-11-20)
### ⚠ BREAKING CHANGES
* chore(charts)!: remove spanner support from Helm charts by @iromli in https://github.com/JanssenProject/jans/pull/10071
* chore(cloud-native)!: remove spanner support from OCI images by @iromli in https://github.com/JanssenProject/jans/pull/10070
* chore(jans-pycloudlib)!: remove couchbase support from pycloudlib by @iromli in https://github.com/JanssenProject/jans/pull/10129
* chore(cloud-native)!: remove couchbase support from OCI images by @iromli in https://github.com/JanssenProject/jans/pull/10132
* chore(charts)!: remove couchbase support from Helm charts by @iromli in https://github.com/JanssenProject/jans/pull/10138

## What's Changed
* chore(jans-linux-setup) remove spanner db by @devrimyatar in https://github.com/JanssenProject/jans/pull/10068
* chore(charts)!: remove spanner support from Helm charts by @iromli in https://github.com/JanssenProject/jans/pull/10071
* fix(config-api): date filter fix by @pujavs in https://github.com/JanssenProject/jans/pull/10075
* chore(docs): docs remove spanner refs by @ossdhaval in https://github.com/JanssenProject/jans/pull/10076
* feat(jans-cedarling): improve error handling for JWKS responses by @rmarinn in https://github.com/JanssenProject/jans/pull/9982
* feat(jans-cedarling): ensure that all cedarling test fixture files are human-readable. by @djellemah in https://github.com/JanssenProject/jans/pull/10036
* feat(jans-config-api): adding allowSmtpKeystoreEdit property in admin-ui configuration by @duttarnab in https://github.com/JanssenProject/jans/pull/10091
* fix(jans-cli-tui): more verbose in smtp test response by @devrimyatar in https://github.com/JanssenProject/jans/pull/10090
* fix(jans-bom): define plugins in base parent project by @yurem in https://github.com/JanssenProject/jans/pull/10087
* fix(jans-orm): throw exception if table in DB is not exists by @yurem in https://github.com/JanssenProject/jans/pull/10096
* fix(jans-casa): primary button not changing color when customized via custom branding plugin by @mjatin-dev in https://github.com/JanssenProject/jans/pull/10084
* fix(jans-cli-tui): remove filePath when putting asset by @devrimyatar in https://github.com/JanssenProject/jans/pull/10107
* fix(jans-lock): fix broken link in lock docs by @yurem in https://github.com/JanssenProject/jans/pull/10110
* feat(jans-cedarling): add env variable for python by @SafinWasi in https://github.com/JanssenProject/jans/pull/10115
* chore(cloud-native)!: remove spanner support from OCI images by @iromli in https://github.com/JanssenProject/jans/pull/10070
* chore: upgrade javascript libraries to newer versions in HTML files by @mjatin-dev in https://github.com/JanssenProject/jans/pull/10108
* fix: remove unnecessary mount for k8s setup by @moabu in https://github.com/JanssenProject/jans/pull/10112
* docs(update): custom scripts docs update by @mmrraju in https://github.com/JanssenProject/jans/pull/10125
* chore(jans-pycloudlib)!: remove couchbase support from pycloudlib by @iromli in https://github.com/JanssenProject/jans/pull/10129
* fix(docs): fix docs link by @ossdhaval in https://github.com/JanssenProject/jans/pull/10123
* chore(jans-linux-setup): drop couchbase support by @devrimyatar in https://github.com/JanssenProject/jans/pull/10133
* fix(docs): remove Couchbase references by @ossdhaval in https://github.com/JanssenProject/jans/pull/10119
* chore(jans-cedarling): remove ipaddr by @SafinWasi in https://github.com/JanssenProject/jans/pull/10140
* ci: move jenkins operations to GH by @moabu in https://github.com/JanssenProject/jans/pull/10082
* chore(cloud-native)!: remove couchbase support from OCI images by @iromli in https://github.com/JanssenProject/jans/pull/10132
* feat(jans-cedarling): Implement check authorization principals based on the schema for action by @olehbozhok in https://github.com/JanssenProject/jans/pull/10126
* fix(jans-orm): update SQL ORM tests to conform latest JSON Filter API by @yurem in https://github.com/JanssenProject/jans/pull/10150
* fix(jans-link): add missing configuration providers by @yurem in https://github.com/JanssenProject/jans/pull/10124
* feat(jans-cedarling): update Trusted Issuers schema in the policy store by @rmarinn in https://github.com/JanssenProject/jans/pull/10141
* fix(jans-bom): update libs by @yurem in https://github.com/JanssenProject/jans/pull/10154
* fix(config-api): adding missing scope in spec and udated example of search field by @pujavs in https://github.com/JanssenProject/jans/pull/10156
* fix(jans-linux-setup): use sqlconnection instead of mysqlconnection by @devrimyatar in https://github.com/JanssenProject/jans/pull/10161
* chore(cloud-native): upgrade libs to reduce vulnerabilities by @iromli in https://github.com/JanssenProject/jans/pull/10168
* chore(charts)!: remove couchbase support from Helm charts by @iromli in https://github.com/JanssenProject/jans/pull/10138
* fix(jans-linux-setup): missing scopes of api-admin by @devrimyatar in https://github.com/JanssenProject/jans/pull/10162
* feat(jans-cli-tui): session management by @devrimyatar in https://github.com/JanssenProject/jans/pull/10164
* feat: implement native internationalization for Agama projects by @jgomer2001 in https://github.com/JanssenProject/jans/pull/10165
* ci: remove python3 ldap package by @moabu in https://github.com/JanssenProject/jans/pull/10172
* chore: adjust projects that use localization labels by @jgomer2001 in https://github.com/JanssenProject/jans/pull/10174
* feat(jans-cedarling): implement mapping JWT payload to `cedar-policy` entity by @olehbozhok in https://github.com/JanssenProject/jans/pull/10169
* refactor(jans-cedarling): relax JWT validation to allow optional claims by @rmarinn in https://github.com/JanssenProject/jans/pull/10173
* fix(jans-cli-tui): file type scripts by @devrimyatar in https://github.com/JanssenProject/jans/pull/10181
* chore(jans-pycloudlib): remove ldap references from pycloudlib by @iromli in https://github.com/JanssenProject/jans/pull/10177
* fix: admin-ui plugin should send appropriate message on expiry/ for inactive license. #10178 by @duttarnab in https://github.com/JanssenProject/jans/pull/10189
* User auth test by @yurem in https://github.com/JanssenProject/jans/pull/10191
* chore(jans-cedarling): update python example and docs by @SafinWasi in https://github.com/JanssenProject/jans/pull/10183
* feat(jans-auth-server): openID AuthZEN implementation by @yuriyz in https://github.com/JanssenProject/jans/pull/10197
* chore(release): release 1.1.6 by @moabu in https://github.com/JanssenProject/jans/pull/10201


## [1.0.21](https://github.com/JanssenProject/jans/compare/v1.0.21...v1.0.21) (2023-12-14)


### Bug Fixes

* location for saml plugin ([#7097](https://github.com/JanssenProject/jans/issues/7097)) ([c57ba6c](https://github.com/JanssenProject/jans/commit/c57ba6c5f8133e73189c51368711d8a37c766751))
* remove agama inbound jar dependency ([#7095](https://github.com/JanssenProject/jans/issues/7095)) ([c58c55c](https://github.com/JanssenProject/jans/commit/c58c55cca0a88872d791941c9f7abca45a09813c))

## [1.0.20](https://github.com/JanssenProject/jans/compare/v1.0.20...v1.0.20) (2023-11-08)


### Features

* **jans-tarp:** user should be allowed to paste an SSA (or specify a file from disk) in DCR form [#6161](https://github.com/JanssenProject/jans/issues/6161) ([#6467](https://github.com/JanssenProject/jans/issues/6467)) ([9b1f694](https://github.com/JanssenProject/jans/commit/9b1f69442003289aa2f92686c49d6e98e4e0e2c0))


### Bug Fixes

* prepare for 1.0.20 release ([c6e806e](https://github.com/JanssenProject/jans/commit/c6e806eb31fed998d52cbef7a7d94c231d913102))

## [1.0.19](https://github.com/JanssenProject/jans/compare/v1.0.19...v1.0.19) (2023-10-11)


### Features

* **docker-jans:** upgrade base image to Java 17 ([#6231](https://github.com/JanssenProject/jans/issues/6231)) ([8ed40e9](https://github.com/JanssenProject/jans/commit/8ed40e91a56c256cb34262659b6e0657571f8c97))
* keycloak refactoring referred to Issue [#5330](https://github.com/JanssenProject/jans/issues/5330) review ([#6157](https://github.com/JanssenProject/jans/issues/6157)) ([7319120](https://github.com/JanssenProject/jans/commit/73191202f2e39bf040749b69a31d01bdfbcec8eb))


### Bug Fixes

* **charts:** use interval-based cronjob schedule syntax ([#6089](https://github.com/JanssenProject/jans/issues/6089)) ([2c0fc97](https://github.com/JanssenProject/jans/commit/2c0fc97dccc938f641596e85c96d1ed4523f8f63))
* **docs:** autogenerate docs ([#6261](https://github.com/JanssenProject/jans/issues/6261)) ([57137e4](https://github.com/JanssenProject/jans/commit/57137e446774f0769e54969b4edbc5d03b715298))
* **docs:** scripts should be shown alphabetically ([#6222](https://github.com/JanssenProject/jans/issues/6222)) ([f60010c](https://github.com/JanssenProject/jans/commit/f60010c8180e3da8f49450233e551cfefe51dd00))
* prepare for 1.0.19 release ([554fd43](https://github.com/JanssenProject/jans/commit/554fd434f624c4b4be3b2031c472177709da8966))

## [1.0.18](https://github.com/JanssenProject/jans/compare/v1.0.17...v1.0.18) (2023-09-23)


### Features

* **jans-auth-server:** included org_id in the response of DCR [#5787](https://github.com/JanssenProject/jans/issues/5787) ([#6095](https://github.com/JanssenProject/jans/issues/6095)) ([34a5f8f](https://github.com/JanssenProject/jans/commit/34a5f8f43aefaa7403cd52d84e0b732f9e1d396e))


### Bug Fixes

* **jans-auth-server:** redirect when session does not exist but client_id parameter is present ([#6104](https://github.com/JanssenProject/jans/issues/6104)) ([f8f9591](https://github.com/JanssenProject/jans/commit/f8f959144b527148f3b586088ae9dd6fcf1158cf))
* **jans-auth-server:** swagger is malformed due to typo [#6085](https://github.com/JanssenProject/jans/issues/6085) ([#6086](https://github.com/JanssenProject/jans/issues/6086)) ([e1ae899](https://github.com/JanssenProject/jans/commit/e1ae899ac4b1d82cd428276e5f00065b0b5a633e))
* prepare for 1.0.18 release ([87af7e4](https://github.com/JanssenProject/jans/commit/87af7e4d41728ce2966362883b47e5354f8c3803))
* remove content-type in header from /retrieve GET request [#6096](https://github.com/JanssenProject/jans/issues/6096) ([#6099](https://github.com/JanssenProject/jans/issues/6099)) ([a85d867](https://github.com/JanssenProject/jans/commit/a85d86743da62c10fef83f2b181d1a992210534f))

## [1.0.17](https://github.com/JanssenProject/jans/compare/v1.0.17...v1.0.17) (2023-09-17)


### Bug Fixes

* **docker-jans-fido2:** search cache for session instead of persistence ([#6040](https://github.com/JanssenProject/jans/issues/6040)) ([d8d4073](https://github.com/JanssenProject/jans/commit/d8d40731d935782dc3a8639d055a2440dbdcb3ec))
* **docker-jans-persistence-loader:** search session by deviceSecret ([#6038](https://github.com/JanssenProject/jans/issues/6038)) ([d299d7f](https://github.com/JanssenProject/jans/commit/d299d7f926e07c7e0af04d5069d51ba9b000393f))
* **docs:** autogenerate docs ([#6065](https://github.com/JanssenProject/jans/issues/6065)) ([0f3cf5d](https://github.com/JanssenProject/jans/commit/0f3cf5d6c679f02b5a385b72003de2669f2bfb66))
* prepare for 1.0.17 release ([4ba8c15](https://github.com/JanssenProject/jans/commit/4ba8c151734f02d762e902b46a35cae2d498fa8f))

## [1.0.16](https://github.com/JanssenProject/jans/compare/v1.0.16...v1.0.16) (2023-08-02)


### Features

* **jans-cli-tui:** agama-cli ([#5715](https://github.com/JanssenProject/jans/issues/5715)) ([5fec965](https://github.com/JanssenProject/jans/commit/5fec965f18429882ec0dc3686a5f83b6f9fb6086))


### Bug Fixes

* **docs:** autogenerate docs ([#5749](https://github.com/JanssenProject/jans/issues/5749)) ([9a29ec1](https://github.com/JanssenProject/jans/commit/9a29ec194b80ecbd06a5a9f4ea34434492bb5cd1))
* **jans-linux-setup:** load test data with setup.properties ([#5723](https://github.com/JanssenProject/jans/issues/5723)) ([b2fa5de](https://github.com/JanssenProject/jans/commit/b2fa5de84c6a9917e0ec0a57a529924b4409ba66))
* prepare for 1.0.16 release ([042ce79](https://github.com/JanssenProject/jans/commit/042ce7941b9597fade8d5f10e40a89d9e7662315))
* prepare for 1.0.16 release ([b2649c3](https://github.com/JanssenProject/jans/commit/b2649c33a9857f356f91df2f38787ec56269e6dd))

## [1.0.15](https://github.com/JanssenProject/jans/compare/v1.0.15...v1.0.15) (2023-07-12)


### Bug Fixes

* prepare for 1.0.15 release ([0e3cc2f](https://github.com/JanssenProject/jans/commit/0e3cc2f5ea287c2c35f45def54f074daa473ec49))

## [1.0.14](https://github.com/JanssenProject/jans/compare/v1.0.14...v1.0.14) (2023-06-12)


### Bug Fixes

* **docker-jans-auth-server:** handle missing secret when comparing contents ([#5187](https://github.com/JanssenProject/jans/issues/5187)) ([f2a373e](https://github.com/JanssenProject/jans/commit/f2a373e95b73d38c8f867fd686152aa18248c392))
* **docs:** autogenerate docs ([#5225](https://github.com/JanssenProject/jans/issues/5225)) ([9c8e510](https://github.com/JanssenProject/jans/commit/9c8e510e6571362009b4ca422ab946ba711e0122))
* prepare for 1.0.14 release ([25ccadf](https://github.com/JanssenProject/jans/commit/25ccadf85327ea14685c6066dc6609919e4f2865))

## [1.0.13](https://github.com/JanssenProject/jans/compare/v1.0.13...v1.0.13) (2023-05-10)


### Bug Fixes

* **docker-jans-loadtesting-jmeter:** rename incorrect reference to OCI image ([#4908](https://github.com/JanssenProject/jans/issues/4908)) ([7db2c11](https://github.com/JanssenProject/jans/commit/7db2c11c8335a35873c08387060454e8eb30d8e2))
* **docs:** autogenerate docs ([#4933](https://github.com/JanssenProject/jans/issues/4933)) ([337239b](https://github.com/JanssenProject/jans/commit/337239ba8ae301a83eec58048a3f5141be54c8e6))
* prepare for 1.0.13 release ([493478e](https://github.com/JanssenProject/jans/commit/493478e71f6231553c998b48c0f163c7f5869da4))

## [1.0.12](https://github.com/JanssenProject/jans/compare/v1.0.12...v1.0.12) (2023-04-18)


### Features

* **config-api:** search pattern, client auth response and security fix ([#4595](https://github.com/JanssenProject/jans/issues/4595)) ([4dbfcc2](https://github.com/JanssenProject/jans/commit/4dbfcc241353c4e03672d4103d10768cbc0c5bdd))


### Bug Fixes

* **docs:** autogenerate docs ([#4652](https://github.com/JanssenProject/jans/issues/4652)) ([e353874](https://github.com/JanssenProject/jans/commit/e35387414ba9a4610a4f5a5e690fb0e26efdacdb))
* prepare for 1.0.12 release ([6f83197](https://github.com/JanssenProject/jans/commit/6f83197705511c39413456acdc64e9136a97ff39))

## [1.0.11](https://github.com/JanssenProject/jans/compare/v1.0.11...v1.0.11) (2023-04-05)


### Features

* backend changes for admin-ui to call licenseSpring apis via. SCAN [#4461](https://github.com/JanssenProject/jans/issues/4461) ([#4462](https://github.com/JanssenProject/jans/issues/4462)) ([3617a95](https://github.com/JanssenProject/jans/commit/3617a95cc9b651691acb8072790784db70e1b152))


### Bug Fixes

* **docs:** autogenerate docs ([#4486](https://github.com/JanssenProject/jans/issues/4486)) ([a9b3eab](https://github.com/JanssenProject/jans/commit/a9b3eabf749cc5dde98c12ffa1b9a1bb9a8091f6))
* **jans-linux-setup:** re-orginize creating smtp configuration ([#4457](https://github.com/JanssenProject/jans/issues/4457)) ([5b543cd](https://github.com/JanssenProject/jans/commit/5b543cdab320de7918b9078735cfafff744ede23))
* prepare for  release ([60775c0](https://github.com/JanssenProject/jans/commit/60775c09dc5ab9996bf80c03dcb457861d48dfb1))

## [1.0.10](https://github.com/JanssenProject/jans/compare/v1.0.9...v1.0.10) (2023-03-16)


### Features

* **config-api:** agama deployment path param change and client authorization ([#4147](https://github.com/JanssenProject/jans/issues/4147)) ([22323ce](https://github.com/JanssenProject/jans/commit/22323cebe180c6e224baf28d66cb435982880df7))
* **docker-jans-persistence-loader:** add online_access scope and enable agama engine by default ([#4136](https://github.com/JanssenProject/jans/issues/4136)) ([8c0bbb3](https://github.com/JanssenProject/jans/commit/8c0bbb3b564623407c66ae934be77ca16ac94c42))
* **docker-jans:** add client auth scopes ([#4156](https://github.com/JanssenProject/jans/issues/4156)) ([1ae907b](https://github.com/JanssenProject/jans/commit/1ae907b3abda9f2df03cfcab805931fdbd1bfc11))
* include jansDeviceData in SCIM Fido2Resource schema [#4057](https://github.com/JanssenProject/jans/issues/4057) ([#4115](https://github.com/JanssenProject/jans/issues/4115)) ([04436dc](https://github.com/JanssenProject/jans/commit/04436dc9054458350180776c4d8d719028162a98))
* **jans-auth-server:** added online_access scope to issue session bound refresh token [#3012](https://github.com/JanssenProject/jans/issues/3012) ([#4106](https://github.com/JanssenProject/jans/issues/4106)) ([635f611](https://github.com/JanssenProject/jans/commit/635f6119fdf4cdf3b3aed33515854ef68257c98f))
* **jans-linux-setup:** enable agama engine by default  ([#4131](https://github.com/JanssenProject/jans/issues/4131)) ([7e432dc](https://github.com/JanssenProject/jans/commit/7e432dcde57657d1cfa1cd45bde2206156dc6905))
* **tent:** support additional params ([#4044](https://github.com/JanssenProject/jans/issues/4044)) ([f521f7a](https://github.com/JanssenProject/jans/commit/f521f7aec58defbf121a817e611f4bff18449882))


### Bug Fixes

* add custom permissions ([34336ac](https://github.com/JanssenProject/jans/commit/34336ac3799872303b9e4891b2405267ea9b6fd8))
* **config-api:** smtp password decryption and encryption logic ([#4161](https://github.com/JanssenProject/jans/issues/4161)) ([4aefb0d](https://github.com/JanssenProject/jans/commit/4aefb0d6a4db39d89b87d703a27e0186fa6780f9))
* **config-api:** user custom attribute changes and agama param changes ([#4123](https://github.com/JanssenProject/jans/issues/4123)) ([291004e](https://github.com/JanssenProject/jans/commit/291004e59248e3205e0113a9f5ce427f259da076))
* **docker-jans-config-api:** remove licenseSpringCredentials from admin-ui jansConfDyn ([#4125](https://github.com/JanssenProject/jans/issues/4125)) ([2cf8aa0](https://github.com/JanssenProject/jans/commit/2cf8aa01b65c809a9bb6c3576cf949ddd23a5fdd))
* **docs:** autogenerate docs ([#4200](https://github.com/JanssenProject/jans/issues/4200)) ([e20f399](https://github.com/JanssenProject/jans/commit/e20f399249055d7b0a65f2c807867c0678e0c787))
* formating issues ([#4119](https://github.com/JanssenProject/jans/issues/4119)) ([c5b89ce](https://github.com/JanssenProject/jans/commit/c5b89ce892ddfd6cf5d7948604d71eadcee73abf))
* **jans-cli-tui:** dropdown widget raises error if not initial values provided ([#4142](https://github.com/JanssenProject/jans/issues/4142)) ([0aa51eb](https://github.com/JanssenProject/jans/commit/0aa51eba21e1c5edf4d8132a1e804d2bb23567f7))
* **jans-cli-tui:** working branch 11 ([#3980](https://github.com/JanssenProject/jans/issues/3980)) ([fdba800](https://github.com/JanssenProject/jans/commit/fdba80049e27a2cc89cf12bee960998061fa770e))
* **jans-fido2:** handling exception fido2 get endpoints by invalid params ([#4139](https://github.com/JanssenProject/jans/issues/4139)) ([a50d2af](https://github.com/JanssenProject/jans/commit/a50d2af5eea4f88632870546eb9e4505cd5c7e2b))
* **jans-linux-setup:** set jansAuthMode - default acr mode ([#4162](https://github.com/JanssenProject/jans/issues/4162)) ([f7d0489](https://github.com/JanssenProject/jans/commit/f7d0489e47a86ce146846dda2064d378dd4a0897))
* javadoc errors ([#4088](https://github.com/JanssenProject/jans/issues/4088)) ([be10a09](https://github.com/JanssenProject/jans/commit/be10a09c4293ccd4c2d67db5a13e2660a2fd546c))
* prepare release for 1.0.10 ([e996926](https://github.com/JanssenProject/jans/commit/e99692692ef04d881468d120f7c7d462568dce36))
* prevent getFlowByName method crash by refactoring [#4128](https://github.com/JanssenProject/jans/issues/4128) ([#4129](https://github.com/JanssenProject/jans/issues/4129)) ([092989b](https://github.com/JanssenProject/jans/commit/092989b35308b6a2d6c3b6da4f36bab2b5518f64))
* **terraform-provider-jans:** update terraform module ([#4164](https://github.com/JanssenProject/jans/issues/4164)) ([073ef39](https://github.com/JanssenProject/jans/commit/073ef3931d665d76cee48a2048b01be6c5b8ef25))
* **terraform-provider-jans:** update terraform provider ([#4148](https://github.com/JanssenProject/jans/issues/4148)) ([92134c8](https://github.com/JanssenProject/jans/commit/92134c826a47a5bcc73456b07b318894d011c609))
* update chart image repositories ([8eea271](https://github.com/JanssenProject/jans/commit/8eea271c8f790ca33d619dbcd83994bc23a7509f))

## [1.0.9](https://github.com/JanssenProject/jans/compare/v1.0.8...v1.0.9) (2023-03-09)


### Features

* **agama:** update gama deployment endpoint to support configuration properties ([#4049](https://github.com/JanssenProject/jans/issues/4049)) ([392525c](https://github.com/JanssenProject/jans/commit/392525c19152fcd916e0c61e70c436a484bf391c))
* getting license credentials from SCAN ([#4052](https://github.com/JanssenProject/jans/issues/4052)) ([5c563b7](https://github.com/JanssenProject/jans/commit/5c563b7530847b8ec6b3201fb53676003ef107b0))
* **jans-auth-server:** introduced additional_token_endpoint_auth_method client's property [#3473](https://github.com/JanssenProject/jans/issues/3473) ([#4033](https://github.com/JanssenProject/jans/issues/4033)) ([79dcb60](https://github.com/JanssenProject/jans/commit/79dcb60491ca8fd9685e68fb8d770aef3c7e89ad))


### Bug Fixes

* **docs:** autogenerate docs ([#4050](https://github.com/JanssenProject/jans/issues/4050)) ([dcbb645](https://github.com/JanssenProject/jans/commit/dcbb64548cc5be5609f27371220406ab1585ff36))
* **docs:** autogenerate docs ([#4105](https://github.com/JanssenProject/jans/issues/4105)) ([da87cef](https://github.com/JanssenProject/jans/commit/da87cef4efd88796260d123054575c3aceb1ed38))
* **jans-auth-server:** bad indentation in AS swagger.yaml [#4108](https://github.com/JanssenProject/jans/issues/4108) ([#4109](https://github.com/JanssenProject/jans/issues/4109)) ([cdcefd2](https://github.com/JanssenProject/jans/commit/cdcefd2ed3fbeef21c7d713453b07994c71393fc))
* **jans-config-api:** getting license credentials from SCAN ([#4055](https://github.com/JanssenProject/jans/issues/4055)) ([407d618](https://github.com/JanssenProject/jans/commit/407d6187a09689a0d9e8b1ffc9ce9dce3fc6e253))
* prepare 1.0.9 release ([e6ea522](https://github.com/JanssenProject/jans/commit/e6ea52220824bd6b5d2dca0539d9d515dbeda362))
* prepare 1.0.9 release ([55f7e0c](https://github.com/JanssenProject/jans/commit/55f7e0c308b869c2c4b5668aca751d022138a678))
* update next SNAPSHOT and dev ([0df0e7a](https://github.com/JanssenProject/jans/commit/0df0e7ae06af64ac477955119c2522f03e0603c3))

## [1.0.8](https://github.com/JanssenProject/jans/compare/v1.0.7...v1.0.8) (2023-03-01)


### Features

* add to AS session the data passed in `Finish` ([#3978](https://github.com/JanssenProject/jans/issues/3978)) ([12bedb7](https://github.com/JanssenProject/jans/commit/12bedb756ae978678a77ceabfdc2879b6f9c1429))
* Include additional attributes on SSA Get endpoint ([#3983](https://github.com/JanssenProject/jans/issues/3983)) ([4fded3e](https://github.com/JanssenProject/jans/commit/4fded3e0ca337bf51176699c7699a7d93bd6d665))
* **jans-cli-tui:** enable super gluu option ([#3970](https://github.com/JanssenProject/jans/issues/3970)) ([0200751](https://github.com/JanssenProject/jans/commit/020075109f9e204ad35b85a6cd9c0470977b805a))


### Bug Fixes

* **docker-jans:** add missing logs due to improper configuration ([#3994](https://github.com/JanssenProject/jans/issues/3994)) ([c041f12](https://github.com/JanssenProject/jans/commit/c041f1274296fe89d997e6e3afaf91bc5c0af540))
* **docker-jans:** resolve required keys_ops_type for generating/rotating keys ([#3990](https://github.com/JanssenProject/jans/issues/3990)) ([0ed67fb](https://github.com/JanssenProject/jans/commit/0ed67fbdd5a9fb6a40072d0a8c027b626e3830b8))
* fix user publicKey search ([#3982](https://github.com/JanssenProject/jans/issues/3982)) ([d0584dd](https://github.com/JanssenProject/jans/commit/d0584dd3b67039c3bff76649547401e50012cce5))
* **jans-auth-server:** WebApplicationException is not propagated out of "Update Token" script [#3996](https://github.com/JanssenProject/jans/issues/3996) ([#3997](https://github.com/JanssenProject/jans/issues/3997)) ([d561f14](https://github.com/JanssenProject/jans/commit/d561f14a04fec8f3b8b56d60d53f5954c12482fa))
* **jans-linux-setup:** install cb before jans installation ([#3981](https://github.com/JanssenProject/jans/issues/3981)) ([dfed3b5](https://github.com/JanssenProject/jans/commit/dfed3b5457cae0bc378f5eaf845a4a5475bdf7e7))
* **jans-linux-setup:** install ncurses-compat-libs cb backend for el8 ([#3969](https://github.com/JanssenProject/jans/issues/3969)) ([412e07f](https://github.com/JanssenProject/jans/commit/412e07f4fcce17c1a801ab5161f1470dd949bab7))
* **jans-linux-setup:** start jans-auth after backend ([#3975](https://github.com/JanssenProject/jans/issues/3975)) ([4afbcee](https://github.com/JanssenProject/jans/commit/4afbcee6176aa2efc85c554da07058311f4e3233))
* **jans-pycloudlib:** split aws secrets when payload is larger than 65536 bytes ([#3971](https://github.com/JanssenProject/jans/issues/3971)) ([bd3d59b](https://github.com/JanssenProject/jans/commit/bd3d59b28259982fc803b0dccdbeda07f328bf92))
* solved error when generate jwt of ssa return error, but ssa persist in database ([#3985](https://github.com/JanssenProject/jans/issues/3985)) ([768fd04](https://github.com/JanssenProject/jans/commit/768fd0440e87930733cf3a463692823c5a105d4a))

## [1.0.7](https://github.com/JanssenProject/jans/compare/v1.0.6...v1.0.7) (2023-02-22)


### Features

* add -key_ops ALL to setup (ref: [#3747](https://github.com/JanssenProject/jans/issues/3747)) ([#3755](https://github.com/JanssenProject/jans/issues/3755)) ([3ce4bb2](https://github.com/JanssenProject/jans/commit/3ce4bb2847e7a776fc344750bb28292e83658cc0))
* add authentication SG flow tests ([#3877](https://github.com/JanssenProject/jans/issues/3877)) ([d5c3fac](https://github.com/JanssenProject/jans/commit/d5c3fac2ee16d0d8276da880b78cc5e676a2f302))
* add custom Github External Authenticator script for ADS [#3625](https://github.com/JanssenProject/jans/issues/3625) ([#3626](https://github.com/JanssenProject/jans/issues/3626)) ([f922a7a](https://github.com/JanssenProject/jans/commit/f922a7a7b075a43750dd792a91a11399517dbb9b))
* add debug SG logging ([#3730](https://github.com/JanssenProject/jans/issues/3730)) ([a0c9ca2](https://github.com/JanssenProject/jans/commit/a0c9ca28e3558a8c85502d134a81d9f5bdd78b12))
* add fast forward suport to skip step authentication flow [#3582](https://github.com/JanssenProject/jans/issues/3582) ([#3583](https://github.com/JanssenProject/jans/issues/3583)) ([25ee0af](https://github.com/JanssenProject/jans/commit/25ee0af896485d8785595e4679d9e19a671c0bd0))
* add Jupiter+Weld+Mockito+Extension to Fido2 ([#3875](https://github.com/JanssenProject/jans/issues/3875)) ([0152435](https://github.com/JanssenProject/jans/commit/01524358cbd720ad547c6b0d622c2cc32e76a125))
* add more loggers ([#3742](https://github.com/JanssenProject/jans/issues/3742)) ([919bc86](https://github.com/JanssenProject/jans/commit/919bc869fd3f2e0be143c5bfddc7ba3629178e86))
* add project metadata and related handling [#3476](https://github.com/JanssenProject/jans/issues/3476) ([#3584](https://github.com/JanssenProject/jans/issues/3584)) ([b95e53e](https://github.com/JanssenProject/jans/commit/b95e53e5eec972b8acb61bd83e327def1364c66c))
* add support for postgres testing ([3494925](https://github.com/JanssenProject/jans/commit/34949250f6c06ecf73be30628ee17fecedf70a9d))
* Change org_id to String type and Add status in get SSA ([#3763](https://github.com/JanssenProject/jans/issues/3763)) ([d01269a](https://github.com/JanssenProject/jans/commit/d01269aa6f51ec9f028da53962d9beaf1cf8a3f9))
* **config-api:** config api configuration endpoint ([#3648](https://github.com/JanssenProject/jans/issues/3648)) ([c798c4c](https://github.com/JanssenProject/jans/commit/c798c4c5a4756c6ba0466b4eeaa44d0e656098ce))
* **config-api:** data conversion, audit log and swagger enhancement ([#3588](https://github.com/JanssenProject/jans/issues/3588)) ([a87b75b](https://github.com/JanssenProject/jans/commit/a87b75bb257b00f71ba643bc81ed110e0c914b79))
* **config-api:** plugin endpoint and audit interceptor ([#3613](https://github.com/JanssenProject/jans/issues/3613)) ([95fadc6](https://github.com/JanssenProject/jans/commit/95fadc6c89c4e91c6d143f0ab9efce0b9395fb14))
* **config-api:** script default script validation for location ([#3786](https://github.com/JanssenProject/jans/issues/3786)) ([446de9e](https://github.com/JanssenProject/jans/commit/446de9e06b269a5f1b50842bfee4cbcdec9a902f))
* **docker-jans-configurator:** allow user-defined salt ([#3624](https://github.com/JanssenProject/jans/issues/3624)) ([cf743b7](https://github.com/JanssenProject/jans/commit/cf743b72298cea03b3667909215ca3251ac9f19b))
* **docker-jans-persistence-loader:** add ssa admin scope ([#3762](https://github.com/JanssenProject/jans/issues/3762)) ([540f619](https://github.com/JanssenProject/jans/commit/540f6196bfe4e5b239486ef5728e381b18d65bc8))
* **docker-jans:** add support for mounted hybrid properties file ([#3623](https://github.com/JanssenProject/jans/issues/3623)) ([8c58a5a](https://github.com/JanssenProject/jans/commit/8c58a5a9530cc9c44e7009ce3952064f8610cc69))
* **docker-jans:** change persistence entry check to read configuration ([#3618](https://github.com/JanssenProject/jans/issues/3618)) ([d465c19](https://github.com/JanssenProject/jans/commit/d465c191c8276c5d409e1200ddd7e3a364089e96))
* **docker-jans:** introduce key_ops when generating keys ([#3770](https://github.com/JanssenProject/jans/issues/3770)) ([2495842](https://github.com/JanssenProject/jans/commit/249584257c3e892f5106d0e3559d1c0caa4a8d77))
* **docs:** updated swagger for new endpoint get jwt of ssa, also added more documentation for scopes. ([7dcca94](https://github.com/JanssenProject/jans/commit/7dcca948f9953a76bf76417d9db02c1775d57074))
* **jans-auth-server:** add configurable rotation of client's registration access token [#3578](https://github.com/JanssenProject/jans/issues/3578) ([#3876](https://github.com/JanssenProject/jans/issues/3876)) ([83183c0](https://github.com/JanssenProject/jans/commit/83183c0f99b24eebd29f379c0a06dee1c6a12e29))
* **jans-auth-server:** added dynamicRegistrationDefaultCustomAttributes to provide default custom attributes during dcr [#3595](https://github.com/JanssenProject/jans/issues/3595) ([#3596](https://github.com/JanssenProject/jans/issues/3596)) ([6202230](https://github.com/JanssenProject/jans/commit/6202230fceec5b72f1541e70cf0833f58bc9ab76))
* **jans-auth-server:** added flexible date formatter handler to AS (required by certification tools) [#3600](https://github.com/JanssenProject/jans/issues/3600) ([#3601](https://github.com/JanssenProject/jans/issues/3601)) ([f646d73](https://github.com/JanssenProject/jans/commit/f646d734d79f9da83cfe51103811efd1f8677d7f))
* **jans-auth-server:** added flexible formatter handler for IdTokenFactory class ([#3605](https://github.com/JanssenProject/jans/issues/3605)) ([f4b0179](https://github.com/JanssenProject/jans/commit/f4b017911cd72718c942bc87e3324e903d755406))
* **jans-auth-server:** added sector_identifier_uri content validation (certification) [#3639](https://github.com/JanssenProject/jans/issues/3639) ([#3641](https://github.com/JanssenProject/jans/issues/3641)) ([2583e53](https://github.com/JanssenProject/jans/commit/2583e534219541312aba99178fef4c60e44b76bf))
* **jans-auth-server:** introduced key_ops for granular map of crypto service to rotation profile [#3415](https://github.com/JanssenProject/jans/issues/3415) ([#3642](https://github.com/JanssenProject/jans/issues/3642)) ([58693c5](https://github.com/JanssenProject/jans/commit/58693c5a8d6bc485a4f3645a2b4a77394d87e141))
* **jans-auth-server:** new endpoint for get jwt of ssa based on jti. ([#3724](https://github.com/JanssenProject/jans/issues/3724)) ([7dcca94](https://github.com/JanssenProject/jans/commit/7dcca948f9953a76bf76417d9db02c1775d57074))
* **jans-auth-server:** OAuth 2.0 Step-up - added acr and auth_time [#2589](https://github.com/JanssenProject/jans/issues/2589) ([#3887](https://github.com/JanssenProject/jans/issues/3887)) ([2bd7a67](https://github.com/JanssenProject/jans/commit/2bd7a67b41a4f9311906101a752d2b9d5800f20d))
* **jans-auth-server:** OAuth 2.0 Step-up - added acr and auth_time to introspection response [#2589](https://github.com/JanssenProject/jans/issues/2589) ([#3885](https://github.com/JanssenProject/jans/issues/3885)) ([a325998](https://github.com/JanssenProject/jans/commit/a32599869a497fe578658a0743786e9658adfa35))
* **jans-auth-server:** provide ability to ignore/bypass prompt=consent [#3721](https://github.com/JanssenProject/jans/issues/3721) ([#3851](https://github.com/JanssenProject/jans/issues/3851)) ([c0286ba](https://github.com/JanssenProject/jans/commit/c0286bae1e2540530d74b0f40f2d48c2490c22a2))
* **jans-auth-server:** provided ability to set scriptDns related attributes of client (e.g. introspectionScripts) [#3645](https://github.com/JanssenProject/jans/issues/3645) ([#3668](https://github.com/JanssenProject/jans/issues/3668)) ([cee2525](https://github.com/JanssenProject/jans/commit/cee252522061b43065eddbdc70d51bda9e2e85d7))
* **jans-auth-server:** provided convenient method to add claim to AT as JWT in modifyAccessToken() method [#3579](https://github.com/JanssenProject/jans/issues/3579) ([#3629](https://github.com/JanssenProject/jans/issues/3629)) ([cf0a824](https://github.com/JanssenProject/jans/commit/cf0a824c50d00b5b1d87ce6c9b72b4865cf0ae93))
* **jans-auth-server:** renamed "key_ops" -&gt; "key_ops_type" [#3790](https://github.com/JanssenProject/jans/issues/3790) ([#3791](https://github.com/JanssenProject/jans/issues/3791)) ([cadb3d6](https://github.com/JanssenProject/jans/commit/cadb3d6731c5148f1d56c66c000577b113cc8cb8))
* **jans-auth-server:** renamed "key_ops" -&gt; "key_ops_type" [#3790](https://github.com/JanssenProject/jans/issues/3790) ([#3792](https://github.com/JanssenProject/jans/issues/3792)) ([7a6bcba](https://github.com/JanssenProject/jans/commit/7a6bcba5ca3597f7556d406e4a572c76a229bbdf))
* **jans-auth-server:** use key_ops=ssa to generate jwt from ssa ([#3806](https://github.com/JanssenProject/jans/issues/3806)) ([2603bbb](https://github.com/JanssenProject/jans/commit/2603bbb1080345cab4fe814dca39024d8d0b5434))
* jans-linux-setup create test client with all available scopes ([#3696](https://github.com/JanssenProject/jans/issues/3696)) ([c2da52e](https://github.com/JanssenProject/jans/commit/c2da52e539cc2edf7e2792d507d02bd7886a901f))
* jans-linux-setup spanner rest client ([#3436](https://github.com/JanssenProject/jans/issues/3436)) ([e4d1d0c](https://github.com/JanssenProject/jans/commit/e4d1d0cc69dad7176d35af4608fd46c4c73ad4ba))
* jans-linux-setup ssa admin scope ([#3759](https://github.com/JanssenProject/jans/issues/3759)) ([485f7b4](https://github.com/JanssenProject/jans/commit/485f7b4c1718f9f088be9931d3b0312b78727bca))
* **jans-tent:** add Jans Tent product ([#3647](https://github.com/JanssenProject/jans/issues/3647)) ([80c53e0](https://github.com/JanssenProject/jans/commit/80c53e0ae890ff5eef926b93af16dff6886f6b86))
* optmize cleander job ([#3737](https://github.com/JanssenProject/jans/issues/3737)) ([2a864d9](https://github.com/JanssenProject/jans/commit/2a864d98b0d3e1983aead62ef9f95e6191248de5))
* process lib directory in `.gama` files for ADS projects deployment ([#3644](https://github.com/JanssenProject/jans/issues/3644)) ([40268ad](https://github.com/JanssenProject/jans/commit/40268adda27ab2929115e3e2117d43fed499a2ce))
* support cancel request ([#3733](https://github.com/JanssenProject/jans/issues/3733)) ([2741e51](https://github.com/JanssenProject/jans/commit/2741e511bc7244a764a43d4dd8d4bb13da87aabb))
* Support Super Gluu one step authentication to Fido2 server [#3593](https://github.com/JanssenProject/jans/issues/3593) ([#3599](https://github.com/JanssenProject/jans/issues/3599)) ([c013b16](https://github.com/JanssenProject/jans/commit/c013b161f2eb47f5952cbb80c8740f8d62d302c3))
* **terraform-provider-jans:** update terraform module ([#3813](https://github.com/JanssenProject/jans/issues/3813)) ([605dd16](https://github.com/JanssenProject/jans/commit/605dd1643f6a2abe86757510baa1558a4504110d))


### Bug Fixes

* add init containers for adjusting ownership on upgrade ([e20e817](https://github.com/JanssenProject/jans/commit/e20e817d136c15ad7606635d42b11d1089368af6))
* add init containers for opendj ([0264df2](https://github.com/JanssenProject/jans/commit/0264df2b58fd23ca3adce3aaf2805805a9e9b855))
* auto installing of the GithubAuthenticatorForADS.py has been removed; ([#3889](https://github.com/JanssenProject/jans/issues/3889)) ([bd6b7ad](https://github.com/JanssenProject/jans/commit/bd6b7ad89d16ae8f80c25c6b375860132de7c97e))
* cbor data stream lenght calculatro return wrong lengh [#3614](https://github.com/JanssenProject/jans/issues/3614) ([#3615](https://github.com/JanssenProject/jans/issues/3615)) ([22065ea](https://github.com/JanssenProject/jans/commit/22065ea14fcaa523531a738f53d6659a790155c5))
* **config-api:** agama swagger spec and admin-ui web key issue ([#3831](https://github.com/JanssenProject/jans/issues/3831)) ([1593997](https://github.com/JanssenProject/jans/commit/159399760c85146c50b54006e7331035de93c42d))
* **config-api:** fixed start-up issue due to scope objectclass case ([#3697](https://github.com/JanssenProject/jans/issues/3697)) ([eac6440](https://github.com/JanssenProject/jans/commit/eac644071d1ca711564ae07361e66dd6aad84366))
* **config-api:** plugin result subsequent call ([#3633](https://github.com/JanssenProject/jans/issues/3633)) ([3e4d513](https://github.com/JanssenProject/jans/commit/3e4d5130db1d0166272772300024880e5603c7be))
* **config-api:** user service conflict with fido2 and script enhancement ([#3767](https://github.com/JanssenProject/jans/issues/3767)) ([5753d39](https://github.com/JanssenProject/jans/commit/5753d3989b96d76699f234cc87f58e355ba313b0))
* **docker-jans-monolith:** use test client and secret ([42c9556](https://github.com/JanssenProject/jans/commit/42c95562cdfe250be893c4f363dc74aadbf05bb7))
* **docker-jans-persistence-loader:** add missing persist tokens config when upgrading from previous version ([#3849](https://github.com/JanssenProject/jans/issues/3849)) ([5b8036d](https://github.com/JanssenProject/jans/commit/5b8036dff57423f36ffa6f47bdd7a150db55d51e))
* **docker-jans:** handle failure on running API requests to Kubernetes API server in Google Cloud Run ([#3893](https://github.com/JanssenProject/jans/issues/3893)) ([a31dee3](https://github.com/JanssenProject/jans/commit/a31dee3fa43be70c64f267fc86f8162b9b48dce2))
* **fido2-client:** conflict of log4j config ([#3636](https://github.com/JanssenProject/jans/issues/3636)) ([77412d5](https://github.com/JanssenProject/jans/commit/77412d5ca1be8dd99797489db22a4e22c7a5cc13))
* fix authenticatorData encoding ([#3815](https://github.com/JanssenProject/jans/issues/3815)) ([687cb2a](https://github.com/JanssenProject/jans/commit/687cb2a3b12980f360a113c88e7cc295c1a4a752))
* fix fmt name ([#3900](https://github.com/JanssenProject/jans/issues/3900)) ([4a6a0c1](https://github.com/JanssenProject/jans/commit/4a6a0c19126220ae783a343d4fe1a54c99ed4475))
* fixes for cancel support ([#3735](https://github.com/JanssenProject/jans/issues/3735)) ([3e64530](https://github.com/JanssenProject/jans/commit/3e64530f3196e4f396c7e8a95d6fd722efa9a816))
* **jans-auth-server:** added testng to agama-inbound [#3714](https://github.com/JanssenProject/jans/issues/3714) ([#3719](https://github.com/JanssenProject/jans/issues/3719)) ([955ac8c](https://github.com/JanssenProject/jans/commit/955ac8c8170988a405e9905bcd5e3b654992f53a))
* **jans-auth-server:** AS complication fails on main [#3863](https://github.com/JanssenProject/jans/issues/3863) ([#3864](https://github.com/JanssenProject/jans/issues/3864)) ([e2aa1a6](https://github.com/JanssenProject/jans/commit/e2aa1a6c1b5bd0577f3a09b44f2fd5bfb7fc85de))
* **jans-auth-server:** corrected issue caught by RegisterRequestTest [#3683](https://github.com/JanssenProject/jans/issues/3683) ([#3684](https://github.com/JanssenProject/jans/issues/3684)) ([3e201d8](https://github.com/JanssenProject/jans/commit/3e201d89d0a9974e31fe76ec4fd4c4eb5ea82664))
* **jans-auth-server:** error from introspection interception script is not propagated during AT as JWT creation [#3904](https://github.com/JanssenProject/jans/issues/3904) ([#3905](https://github.com/JanssenProject/jans/issues/3905)) ([8c551c0](https://github.com/JanssenProject/jans/commit/8c551c0c09aaaf13898e047e7c30c96531d37518))
* **jans-auth-server:** jansApp attribute only relevant for SG ([#3782](https://github.com/JanssenProject/jans/issues/3782)) ([6153a13](https://github.com/JanssenProject/jans/commit/6153a139d584e69088f8d9202ce072ae10a2dc73))
* **jans-auth-server:** key_ops in jwks must be array [#3777](https://github.com/JanssenProject/jans/issues/3777) ([#3778](https://github.com/JanssenProject/jans/issues/3778)) ([2be2a03](https://github.com/JanssenProject/jans/commit/2be2a0346d7ed0541bb540e3c2ff32aa3a04dcf7))
* **jans-auth-server:** provided corrected public key for outdated keystores during id_token creation if key_ops_type is absent [#3840](https://github.com/JanssenProject/jans/issues/3840) ([#3841](https://github.com/JanssenProject/jans/issues/3841)) ([3291eab](https://github.com/JanssenProject/jans/commit/3291eab88622d036b174ed2199fcedc2dd274e96))
* **jans-auth-server:** wrong Client Authn Method at token endpoint throws npe [#3503](https://github.com/JanssenProject/jans/issues/3503) ([#3598](https://github.com/JanssenProject/jans/issues/3598)) ([e3bd1e8](https://github.com/JanssenProject/jans/commit/e3bd1e8a8baf8925c77555944c88864c1d38cc95))
* jans-cli-tui documentation and setup ([#3818](https://github.com/JanssenProject/jans/issues/3818)) ([74660db](https://github.com/JanssenProject/jans/commit/74660db65cb2e1b3c2a039a3296cfc6df11ba1af))
* jans-cli-tui include pyproject.toml (ref: [#3804](https://github.com/JanssenProject/jans/issues/3804)) ([#3835](https://github.com/JanssenProject/jans/issues/3835)) ([f250372](https://github.com/JanssenProject/jans/commit/f250372eb42d613d72be63ca4098fc3762cef995))
* jans-cli-tui rename config-api swagger file ([#3758](https://github.com/JanssenProject/jans/issues/3758)) ([a7e14a0](https://github.com/JanssenProject/jans/commit/a7e14a00c373661abafc1a59a7bba30b89c66108))
* jans-cli-tui SSA org_id, status and software_roles ([#3765](https://github.com/JanssenProject/jans/issues/3765)) ([4bd3e9a](https://github.com/JanssenProject/jans/commit/4bd3e9a6bcba66307dd682a71708ed113bfdd302))
* jans-cli-tui typo ([#3846](https://github.com/JanssenProject/jans/issues/3846)) ([aaab377](https://github.com/JanssenProject/jans/commit/aaab377deb34504bb12014aaa3c544b0dfaca252))
* jans-cli-tui typo organisation ([#3853](https://github.com/JanssenProject/jans/issues/3853)) ([58a974b](https://github.com/JanssenProject/jans/commit/58a974bae20cea8113e9537c2d27864964c1b84c))
* jans-cli-tui working branch 4 - many different improvements ([#3504](https://github.com/JanssenProject/jans/issues/3504)) ([e572552](https://github.com/JanssenProject/jans/commit/e572552d2d09b7100679c5d06ddf07fab8383641))
* jans-cli-tui working branch 5 ([#3649](https://github.com/JanssenProject/jans/issues/3649)) ([4d3f982](https://github.com/JanssenProject/jans/commit/4d3f982f38cc51854f5a41b6cf1d6076766c3937))
* jans-cli-tui working branch 6 ([#3794](https://github.com/JanssenProject/jans/issues/3794)) ([afea59d](https://github.com/JanssenProject/jans/commit/afea59d3195ea1b1fbf0c64f7f35a3467626edf6))
* jans-cli-tui working branch 7 ([#3824](https://github.com/JanssenProject/jans/issues/3824)) ([776bab3](https://github.com/JanssenProject/jans/commit/776bab37c1eab6fd548232504538078854ae546d))
* **jans-cli-tui:** working branch 9 ([#3871](https://github.com/JanssenProject/jans/issues/3871)) ([9f16d75](https://github.com/JanssenProject/jans/commit/9f16d75e4de5d1e3ccca6b21f004ecf6a219daae))
* **jans-config-api:** Fixing runtime ambiguity for RegistrationPersistenceService.java ([#3756](https://github.com/JanssenProject/jans/issues/3756)) ([83c7b50](https://github.com/JanssenProject/jans/commit/83c7b50fd6f49e7613273d9b03d8c950ff13593d))
* **jans-config-api:** runtime exceptions in config-api at startup ([#3725](https://github.com/JanssenProject/jans/issues/3725)) ([8748cc3](https://github.com/JanssenProject/jans/commit/8748cc35b29cce68ac6c5f61fd7b918be765047d))
* **jans-fido2:** RegistrationPersistenceService implemntation ([#3728](https://github.com/JanssenProject/jans/issues/3728)) ([d5b8b67](https://github.com/JanssenProject/jans/commit/d5b8b67e10bd6d9bc93d831bef8198f406873b0e))
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
* **jans-pycloudlib:** avoid overwritten data by using merge strategy for AWS wrappers ([#3832](https://github.com/JanssenProject/jans/issues/3832)) ([cf0d4e3](https://github.com/JanssenProject/jans/commit/cf0d4e3cff9478496fe1e63f9d4113c3dc81abe9))
* **jans-pycloudlib:** avoid overwritten data by using merge strategy for Google wrappers ([#3826](https://github.com/JanssenProject/jans/issues/3826)) ([809b9db](https://github.com/JanssenProject/jans/commit/809b9dbf08b4ed3ad1b0e545302502bf731298be))
* **jans-pycloudlib:** split google secrets when payload is larger than 65536 bytes ([#3890](https://github.com/JanssenProject/jans/issues/3890)) ([a86b098](https://github.com/JanssenProject/jans/commit/a86b098699e554f31f170d60de978f7797f76730))
* license overwrite ([055d72b](https://github.com/JanssenProject/jans/commit/055d72baa4799624876228661cd94f85d5b04e9c))
* missing comma delimiter for Postgres index fields ([#3741](https://github.com/JanssenProject/jans/issues/3741)) ([1a2d298](https://github.com/JanssenProject/jans/commit/1a2d298c5b4911bfaccdf80203df1a919ff7a6b8))
* opendj jans vlume mount ([4de4815](https://github.com/JanssenProject/jans/commit/4de4815d587fb51d367d1ccd03143e4df3297ac4))
* opendj statefulset permissions ([0d9a2a7](https://github.com/JanssenProject/jans/commit/0d9a2a735e96d00e6a4245dd86457039819fc7e2))
* prepare 1.0.7 release ([ce02fd9](https://github.com/JanssenProject/jans/commit/ce02fd9322ab49d5bea4f6e88f316f931e9d2169))
* remove upgrade flag ([bee17a7](https://github.com/JanssenProject/jans/commit/bee17a7a69de7872ee54ea1edc3dd01a7da21417))
* rename role_based_client as tui_client ([#3657](https://github.com/JanssenProject/jans/issues/3657)) ([c393cb2](https://github.com/JanssenProject/jans/commit/c393cb2052f7e73cc6a02b84bbc673bcc93dc13d))
* store correct script revision after script reload from file ([#3704](https://github.com/JanssenProject/jans/issues/3704)) ([2ca6a83](https://github.com/JanssenProject/jans/commit/2ca6a833132b129c29924106ed65db430917cb8c)), closes [#3703](https://github.com/JanssenProject/jans/issues/3703)
* **tent:** remove unknown module refference ([#3802](https://github.com/JanssenProject/jans/issues/3802)) ([b01a015](https://github.com/JanssenProject/jans/commit/b01a015323b8b89209898ce1fb9b3f82b4731382))
* **terraform-provider-jans:** update terraform module ([#3869](https://github.com/JanssenProject/jans/issues/3869)) ([30e3c67](https://github.com/JanssenProject/jans/commit/30e3c67f4412af81cba85710db0b092119fa87e5))
* update configmaps ENVs ([a797c61](https://github.com/JanssenProject/jans/commit/a797c6139da3f5ada9c054ef596a4a13b94f8a1b))

## [1.0.6](https://github.com/JanssenProject/jans/compare/v1.0.5...v1.0.6) (2023-01-09)


### Features

* add attributes and branch for ADS deployments [#3095](https://github.com/JanssenProject/jans/issues/3095) ([#3228](https://github.com/JanssenProject/jans/issues/3228)) ([2dc9267](https://github.com/JanssenProject/jans/commit/2dc9267423f464414b19718e5c2daa9f58283863))
* add benchmark demo ([#3325](https://github.com/JanssenProject/jans/issues/3325)) ([26bbb0c](https://github.com/JanssenProject/jans/commit/26bbb0ca2ef9ec5ac72f80ee3641d222036d55b2))
* add endpoint to do syntax check only [#3277](https://github.com/JanssenProject/jans/issues/3277) ([#3299](https://github.com/JanssenProject/jans/issues/3299)) ([3b23636](https://github.com/JanssenProject/jans/commit/3b236360ca7e2c3d7edcae9c356ffd2b193c42c2))
* add endpoints for MVP ADS projects management [#3094](https://github.com/JanssenProject/jans/issues/3094) ([#3262](https://github.com/JanssenProject/jans/issues/3262)) ([8546356](https://github.com/JanssenProject/jans/commit/8546356c7b6ee2e7f1fcc83f8fcafb889179c769))
* added custom resource owner password script fro two-factor twilio authentication ([#3208](https://github.com/JanssenProject/jans/issues/3208)) ([eae0ca1](https://github.com/JanssenProject/jans/commit/eae0ca1704da961de84e7a7ce665a7c3b0bb3567))
* **agama:** deploy flows from .gama files ([#3250](https://github.com/JanssenProject/jans/issues/3250)) ([df14f8a](https://github.com/JanssenProject/jans/commit/df14f8aee022ae14746af6ebd15dbca9622a4086))
* changes in admin-ui plugin to allow agama-developer-studio to use its OAuth2 apis [#3085](https://github.com/JanssenProject/jans/issues/3085) ([#3298](https://github.com/JanssenProject/jans/issues/3298)) ([9e9a7bd](https://github.com/JanssenProject/jans/commit/9e9a7bd17c9b7238b7e65359ffdd5f6b0474e9d1))
* **config-api:** audit log, agama ADS spec, fix for 0 index search ([#3369](https://github.com/JanssenProject/jans/issues/3369)) ([ea04e2c](https://github.com/JanssenProject/jans/commit/ea04e2ce5d83d4840638cd2e137fcbc67ee69c81))
* **config-api:** client claim enhancement, manual spec removed ([#3413](https://github.com/JanssenProject/jans/issues/3413)) ([bd2cdf8](https://github.com/JanssenProject/jans/commit/bd2cdf8501d60959498078bbb31650965c321c73))
* **config-api:** health check response rectification and Agama ADS swagger spec ([#3293](https://github.com/JanssenProject/jans/issues/3293)) ([faf2888](https://github.com/JanssenProject/jans/commit/faf2888f3d58d14fc6361d5a9ff5f743984cea4f))
* **docker-jans:** add admin-ui scopes ([#3530](https://github.com/JanssenProject/jans/issues/3530)) ([bc62673](https://github.com/JanssenProject/jans/commit/bc626739bf7a2ed10e7551eda6ea4cc45e7ea49a))
* **docker-jans:** enable prefix and group for stdout logs ([#3481](https://github.com/JanssenProject/jans/issues/3481)) ([e7684e7](https://github.com/JanssenProject/jans/commit/e7684e7f6da7c789d03311fe2df855c687aa7fa6))
* **docs:** jans TUI SCIM configuration -- screenshot ([#3318](https://github.com/JanssenProject/jans/issues/3318)) ([7b463b0](https://github.com/JanssenProject/jans/commit/7b463b01ec36153948110a59010fb9a4b347eae9))
* **docs:** jans TUI SCIM configuration feature - screenshot1 ([#3306](https://github.com/JanssenProject/jans/issues/3306)) ([d1adc98](https://github.com/JanssenProject/jans/commit/d1adc9826dee5bfaf4e36c6a080684e803d869ce))
* **docs:** jans TUI SCIM configuration feature ([#3305](https://github.com/JanssenProject/jans/issues/3305)) ([70e358e](https://github.com/JanssenProject/jans/commit/70e358e49949cec50a1784dc229678451b95c424))
* **jans-auth-server:** added ability to return error out of introspection and update_token custom script [#3255](https://github.com/JanssenProject/jans/issues/3255) ([#3356](https://github.com/JanssenProject/jans/issues/3356)) ([a3e5227](https://github.com/JanssenProject/jans/commit/a3e522745a28fddb3cb6677a553350868fcbaa45))
* **jans-auth-server:** added externalUriWhiteList configuration property before call external uri from AS [#3130](https://github.com/JanssenProject/jans/issues/3130) ([#3425](https://github.com/JanssenProject/jans/issues/3425)) ([6c7df6f](https://github.com/JanssenProject/jans/commit/6c7df6fc955812599a49937f98a6746d05b0badf))
* **jans-auth-server:** avoid compilation problem when version is flipped in test code [#3148](https://github.com/JanssenProject/jans/issues/3148) ([#3210](https://github.com/JanssenProject/jans/issues/3210)) ([4d61c7b](https://github.com/JanssenProject/jans/commit/4d61c7b1c5be70acd855f68ff51123342ac94490))
* **jans-auth-server:** block authentication flow originating from a webview ([#3204](https://github.com/JanssenProject/jans/issues/3204)) ([e48380e](https://github.com/JanssenProject/jans/commit/e48380e68653cd4bd25ec2265225e4900e20bec1))
* **jans-auth-server:** draft for - improve dcr / ssa validation for dynamic  registration [#2980](https://github.com/JanssenProject/jans/issues/2980) ([#3109](https://github.com/JanssenProject/jans/issues/3109)) ([233a78c](https://github.com/JanssenProject/jans/commit/233a78c8e48fb8de353629bc16fc6af1d80fb910))
* **jans-auth-server:** end session - if id_token is expired but signature is correct, we should make attempt to look up session by "sid" claim [#3231](https://github.com/JanssenProject/jans/issues/3231) ([#3291](https://github.com/JanssenProject/jans/issues/3291)) ([cd11750](https://github.com/JanssenProject/jans/commit/cd11750c064e4f18d7df759f8271338a7d079ad0))
* **jans-auth-server:** implemented auth server config property to disable prompt=login [#3006](https://github.com/JanssenProject/jans/issues/3006) ([#3522](https://github.com/JanssenProject/jans/issues/3522)) ([0233cd1](https://github.com/JanssenProject/jans/commit/0233cd161f07e793c9565d40338078b09d2c12c3))
* **jans-auth-server:** new configuration for userinfo has been added ([#3349](https://github.com/JanssenProject/jans/issues/3349)) ([3ccc4a9](https://github.com/JanssenProject/jans/commit/3ccc4a9ad8486a0795d733bf8961999bad319438))
* **jans-auth-server:** remove ox properties name ([#3285](https://github.com/JanssenProject/jans/issues/3285)) ([f70b207](https://github.com/JanssenProject/jans/commit/f70b207ecff565ff53e3efb13d897937d9aeaee0))
* **jans-auth-server:** renamed "code"-&gt;"random" uniqueness claims of id_token to avoid confusion with Authorization Code Flow [#3466](https://github.com/JanssenProject/jans/issues/3466) ([#3467](https://github.com/JanssenProject/jans/issues/3467)) ([dd9d049](https://github.com/JanssenProject/jans/commit/dd9d049d67bdd608dd3aea33c301817dd4cb0d8c))
* **jans-config-api:** added admin-ui scopes in config-api-rs-protect.json ([c348ae6](https://github.com/JanssenProject/jans/commit/c348ae6a44bf59eec5a3f20b2984f7f245cff307))
* **jans-core:** add AES utility class [#3215](https://github.com/JanssenProject/jans/issues/3215) ([#3242](https://github.com/JanssenProject/jans/issues/3242)) ([7e59795](https://github.com/JanssenProject/jans/commit/7e59795e21bc63b173802346b614e7ae6112de4e))
* jans-linux-setup script for adding sequenced users to rdbm backend ([#3311](https://github.com/JanssenProject/jans/issues/3311)) ([63c74ec](https://github.com/JanssenProject/jans/commit/63c74ecd05f4be9bac2caaa281e10157b3e6ea37))
* problems with handling custom attributes [#2752](https://github.com/JanssenProject/jans/issues/2752) ([#3378](https://github.com/JanssenProject/jans/issues/3378)) ([3028a94](https://github.com/JanssenProject/jans/commit/3028a94b8c7ef4eead7f45ea6b91b9e1a72e6368))


### Bug Fixes

* [#2201](https://github.com/JanssenProject/jans/issues/2201) ([#3365](https://github.com/JanssenProject/jans/issues/3365)) ([ebca16b](https://github.com/JanssenProject/jans/commit/ebca16bc8dae14b86582ed584292eb610efd0621))
* [#2201](https://github.com/JanssenProject/jans/issues/2201) ([#3451](https://github.com/JanssenProject/jans/issues/3451)) ([0417c2a](https://github.com/JanssenProject/jans/commit/0417c2a641a810dacee6fcd3dfc2a0d71eb32142))
* [#2201](https://github.com/JanssenProject/jans/issues/2201) removing an irrelevant head from mkdocs ([#3478](https://github.com/JanssenProject/jans/issues/3478)) ([9771205](https://github.com/JanssenProject/jans/commit/9771205d29658c74d6a1f2954c8c59eba7f9ab65))
* add 'java' to the list of restricted variable names [#3533](https://github.com/JanssenProject/jans/issues/3533) ([#3534](https://github.com/JanssenProject/jans/issues/3534)) ([a970d88](https://github.com/JanssenProject/jans/commit/a970d88d81f920973f3ba812db97448f135090a9))
* add link to api reference ([#3394](https://github.com/JanssenProject/jans/issues/3394)) ([f091045](https://github.com/JanssenProject/jans/commit/f0910452b33f24dc220d5123c853f11326a920df))
* **agama:** after moving agama to jans-auth-server agama model tests are not run [#3246](https://github.com/JanssenProject/jans/issues/3246) ([#3247](https://github.com/JanssenProject/jans/issues/3247)) ([9887e23](https://github.com/JanssenProject/jans/commit/9887e2333a4482100f28ccf448f99e07059490ac))
* **agama:** fix agama auth dependency which blocks build process [#3149](https://github.com/JanssenProject/jans/issues/3149) ([#3244](https://github.com/JanssenProject/jans/issues/3244)) ([8f9fee3](https://github.com/JanssenProject/jans/commit/8f9fee31c66ce08046258694e5e2d83a31e38b5d))
* **agama:** fixing tests run on jenkins [#3149](https://github.com/JanssenProject/jans/issues/3149) ([#3261](https://github.com/JanssenProject/jans/issues/3261)) ([cc6c5e1](https://github.com/JanssenProject/jans/commit/cc6c5e12f5deb17a5c0353fc765a50d1603c74a1))
* app_info.json value of JANS_BUILD ([#3199](https://github.com/JanssenProject/jans/issues/3199)) ([fe35e85](https://github.com/JanssenProject/jans/commit/fe35e855b91eac0a63903199f96dd4c6996cdce0))
* Broken swagger address. ([843f78b](https://github.com/JanssenProject/jans/commit/843f78b7a0d7cb27b07a041a76d07e61430e6ab1))
* catch org.eclipse.jetty.http.BadMessageException: in ([#3330](https://github.com/JanssenProject/jans/issues/3330)) ([1e0ff76](https://github.com/JanssenProject/jans/commit/1e0ff760651f5e3cd25044566835dbd20d4ab2c3)), closes [#3329](https://github.com/JanssenProject/jans/issues/3329)
* docs/requirements.txt to reduce vulnerabilities ([#3523](https://github.com/JanssenProject/jans/issues/3523)) ([82efd8f](https://github.com/JanssenProject/jans/commit/82efd8f503bc2966483f07fff2a77f5c1321c7a2))
* **docs:** jans logging configuration - VM Operation Guide - 1 ([#3348](https://github.com/JanssenProject/jans/issues/3348)) ([e0f8c71](https://github.com/JanssenProject/jans/commit/e0f8c7120e8074ae54ff3abb9b4e654cdde64e44))
* **docs:** jans TUI administration -- Config Guide - TUI -- Auth server ([#3227](https://github.com/JanssenProject/jans/issues/3227)) ([16ab709](https://github.com/JanssenProject/jans/commit/16ab709cca92620a409611521885c14029b8ba0d))
* **docs:** jans TUI configuration -- Auth Server - TUI - image ([#3237](https://github.com/JanssenProject/jans/issues/3237)) ([3fbc9e7](https://github.com/JanssenProject/jans/commit/3fbc9e7d08ff0c9ef55f009dea77e675d652ee7e))
* **docs:** jans TUI configuration -- Config Guide - Auth Server - Client configuration - TUI ([#3233](https://github.com/JanssenProject/jans/issues/3233)) ([ee8e056](https://github.com/JanssenProject/jans/commit/ee8e0564a1eeadaee12ab80078c3802822ab4d1a))
* **docs:** jans TUI configuration -- Fido - TUI ([#3251](https://github.com/JanssenProject/jans/issues/3251)) ([cd6eef1](https://github.com/JanssenProject/jans/commit/cd6eef1503375c39468d82263e0e342b3aace5be))
* **docs:** jans TUI configuration -- Fido Administration - TUI ([#3252](https://github.com/JanssenProject/jans/issues/3252)) ([a371cda](https://github.com/JanssenProject/jans/commit/a371cda85da66a956ca2d0a15200a6c6d2c29ca0))
* **docs:** jans TUI installation -- Config Guide - TUI ([#3224](https://github.com/JanssenProject/jans/issues/3224)) ([cc00a71](https://github.com/JanssenProject/jans/commit/cc00a71af6b82ca969b62a835a24733041ac676d))
* **docs:** missing single quotes ([#3239](https://github.com/JanssenProject/jans/issues/3239)) ([9f38c6a](https://github.com/JanssenProject/jans/commit/9f38c6a216a0dd4afdbec4080f312180ab7858b3))
* fix format string [#3278](https://github.com/JanssenProject/jans/issues/3278) ([#3281](https://github.com/JanssenProject/jans/issues/3281)) ([7104d9c](https://github.com/JanssenProject/jans/commit/7104d9c205900e08d85043aa23d4b00460861b3f))
* fix token indexes and clnId type ([#3434](https://github.com/JanssenProject/jans/issues/3434)) ([4a18904](https://github.com/JanssenProject/jans/commit/4a18904ebfc3c3562a3e2308ae3a7bf200c0d1bc))
* generate javadpcs envproperty ([9182aa5](https://github.com/JanssenProject/jans/commit/9182aa5c2769070620f2a70cf72bf20a1b3a8bcd))
* hash ([cdb5204](https://github.com/JanssenProject/jans/commit/cdb52047c5847e2eafbaf2f7692211e72b8fde12))
* hash ([156fb2f](https://github.com/JanssenProject/jans/commit/156fb2f697dd4f292659ad9963e7044a7137a583))
* jans-auth-server/pom.xml to reduce vulnerabilities ([#3314](https://github.com/JanssenProject/jans/issues/3314)) ([f3e8205](https://github.com/JanssenProject/jans/commit/f3e82051bcd47346986ba250b169a0cf5684b4ec))
* **jans-auth-server:** changed getAttributeValues to getAttributeObjectValues ([#3346](https://github.com/JanssenProject/jans/issues/3346)) ([a39b61e](https://github.com/JanssenProject/jans/commit/a39b61e6e686680e2b45b10e25b36fa41a4de76a))
* **jans-auth-server:** compilation error of server side tests [#3363](https://github.com/JanssenProject/jans/issues/3363) ([#3364](https://github.com/JanssenProject/jans/issues/3364)) ([e83c087](https://github.com/JanssenProject/jans/commit/e83c087a168367ef146b1e42a75d7325da05b695))
* **jans-auth-server:** corrected keys description "id_token &lt;purpose&gt;" -> "Connect <purpose>" [#3415](https://github.com/JanssenProject/jans/issues/3415) ([#3560](https://github.com/JanssenProject/jans/issues/3560)) ([75f99bd](https://github.com/JanssenProject/jans/commit/75f99bdf2bb676e607b86a71cf4b00a2e51ba251))
* **jans-auth-server:** Duplicate iss and aud on introspection as jwt [#3366](https://github.com/JanssenProject/jans/issues/3366) ([#3387](https://github.com/JanssenProject/jans/issues/3387)) ([8780e94](https://github.com/JanssenProject/jans/commit/8780e944f120a7f0d8edfb329e31f44a9b99d94a))
* **jans-auth-server:** parse string from object ([#3470](https://github.com/JanssenProject/jans/issues/3470)) ([db9b204](https://github.com/JanssenProject/jans/commit/db9b204d1bca9604086a841137c598bbe3ebffe4))
* **jans-auth-server:** when obtain new token using refresh token, check whether scope is null ([#3382](https://github.com/JanssenProject/jans/issues/3382)) ([22743d9](https://github.com/JanssenProject/jans/commit/22743d9fce0c99e794be0eb3969341987b1936ee))
* jans-cli-tui ([#3287](https://github.com/JanssenProject/jans/issues/3287)) ([27e7518](https://github.com/JanssenProject/jans/commit/27e7518922aff6434bf9a87392e246af8f44d9ac))
* jans-cli-tui refactor mouse operations ([#3482](https://github.com/JanssenProject/jans/issues/3482)) ([39dc0c7](https://github.com/JanssenProject/jans/commit/39dc0c7b3e0b66e1180e5b8c8c165f992c711555))
* jans-config-api/pom.xml to reduce vulnerabilities ([#3005](https://github.com/JanssenProject/jans/issues/3005)) ([3e642c2](https://github.com/JanssenProject/jans/commit/3e642c2ebbd6d17c84bdec940e403d9b37affc38))
* **jans-config-api:** corrected broken swagger address ([#3505](https://github.com/JanssenProject/jans/issues/3505)) ([843f78b](https://github.com/JanssenProject/jans/commit/843f78b7a0d7cb27b07a041a76d07e61430e6ab1))
* jans-eleven/pom.xml to reduce vulnerabilities ([#3315](https://github.com/JanssenProject/jans/issues/3315)) ([813cf98](https://github.com/JanssenProject/jans/commit/813cf983ecfb6ddc19ace5a07c2233ebca327999))
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
* login page doesn't display the correct localized characters ([#3528](https://github.com/JanssenProject/jans/issues/3528)) ([395b376](https://github.com/JanssenProject/jans/commit/395b3769750d2d32f624060c0b6e6ceeee7df0be)), closes [#1660](https://github.com/JanssenProject/jans/issues/1660)
* minor ([#3334](https://github.com/JanssenProject/jans/issues/3334)) ([3225455](https://github.com/JanssenProject/jans/commit/32254553a7bb5c58f265f29c3613ecc8f81f44b8))
* modify clnId column type ([#3459](https://github.com/JanssenProject/jans/issues/3459)) ([701394c](https://github.com/JanssenProject/jans/commit/701394c3d7463ffb2bd223daf9662921244ad34d))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))
* remove multiple sed commands ([#3526](https://github.com/JanssenProject/jans/issues/3526)) ([2b906db](https://github.com/JanssenProject/jans/commit/2b906db77cebe2ce2e8c5af1bd40aa23c4181691))
* TUI client pre-authorized ([#3399](https://github.com/JanssenProject/jans/issues/3399)) ([ab30953](https://github.com/JanssenProject/jans/commit/ab3095340d82c0b5b5b5342b0e21273b03017524))
* upgrade org.apache.httpcomponents:httpcore from 4.4.6 to 4.4.15 ([#642](https://github.com/JanssenProject/jans/issues/642)) ([069dceb](https://github.com/JanssenProject/jans/commit/069dceb5be540288e833628019c4265cd7a85344))
* upgrade org.mvel:mvel2 from 2.1.3.Final to 2.4.14.Final ([#648](https://github.com/JanssenProject/jans/issues/648)) ([c4034d1](https://github.com/JanssenProject/jans/commit/c4034d12f2bbd9396cc1824f8e485163b4407f68))
* use correct Ubuntu version in README ([#3393](https://github.com/JanssenProject/jans/issues/3393)) ([2673ccd](https://github.com/JanssenProject/jans/commit/2673ccdf72d48a7cdf72d230820fb16119985d76))
* user attributes not updated [#2753](https://github.com/JanssenProject/jans/issues/2753) ([#3326](https://github.com/JanssenProject/jans/issues/3326)) ([c0a0f66](https://github.com/JanssenProject/jans/commit/c0a0f66870e6f4c38dc3a336f1f8b783f4c911ca))
* user attributes not updated [#2753](https://github.com/JanssenProject/jans/issues/2753) ([#3403](https://github.com/JanssenProject/jans/issues/3403)) ([f793f92](https://github.com/JanssenProject/jans/commit/f793f92fa275da2e57b2302dcb5c6fdb27666e67))

## [1.0.5](https://github.com/JanssenProject/jans/compare/v1.0.4...v1.0.5) (2022-12-01)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))
* add feature to include custom-claims in user-info endpoint of admin-ui plugin [#2969](https://github.com/JanssenProject/jans/issues/2969) ([#2970](https://github.com/JanssenProject/jans/issues/2970)) ([0549879](https://github.com/JanssenProject/jans/commit/054987944d91fe2021092c30b337e880421533ff))
* add jans cli tui ([#2384](https://github.com/JanssenProject/jans/issues/2384)) ([c9c502b](https://github.com/JanssenProject/jans/commit/c9c502b5328677bd3ef4895acf296aa3e05bb333))
* allow to use like with lower together ([#2944](https://github.com/JanssenProject/jans/issues/2944)) ([1807629](https://github.com/JanssenProject/jans/commit/1807629b93a6dfba691183ad0185e19f981727ee))
* **charts:** add pdb and topology spread constrants ([ce575c2](https://github.com/JanssenProject/jans/commit/ce575c260989f4fb4405e54bb0c8ae86d0c48c26))
* documentation for ssa and remove softwareRoles query param of get ssa ([#3031](https://github.com/JanssenProject/jans/issues/3031)) ([d8e14eb](https://github.com/JanssenProject/jans/commit/d8e14ebbeee357c8c2c31808243cf82933ae4a9b))
* **image:** preserve attribute's values in jans-auth config ([#3013](https://github.com/JanssenProject/jans/issues/3013)) ([3e9e7fc](https://github.com/JanssenProject/jans/commit/3e9e7fc56c8d7890920d5e99f8c28f291afcf207))
* jans cli to jans-cli-tui ([#3063](https://github.com/JanssenProject/jans/issues/3063)) ([fc20e28](https://github.com/JanssenProject/jans/commit/fc20e287feb4cc1b7bb983c44e25a8ae936580f0))
* **jans-auth-server:** check offline_access implementation has all conditions defined in spec [#1945](https://github.com/JanssenProject/jans/issues/1945) ([#3004](https://github.com/JanssenProject/jans/issues/3004)) ([af30e4c](https://github.com/JanssenProject/jans/commit/af30e4c438372fffb7a3ac78a6aea5988af43d5f))
* **jans-auth-server:** corrected GluuOrganization - refactor getOrganizationName() [#2947](https://github.com/JanssenProject/jans/issues/2947) ([#2948](https://github.com/JanssenProject/jans/issues/2948)) ([9275576](https://github.com/JanssenProject/jans/commit/9275576ed0f925fcd3dbaf06e155e7185c797015))
* **jans-auth-server:** java docs for ssa ([#2995](https://github.com/JanssenProject/jans/issues/2995)) ([892b87a](https://github.com/JanssenProject/jans/commit/892b87a2af5fa82ba4f5dceb38baba28e2029182))
* **jans-auth-server:** remove redirect uri on client registration when grant types is password or client credentials ([#3076](https://github.com/JanssenProject/jans/issues/3076)) ([cd876b4](https://github.com/JanssenProject/jans/commit/cd876b46e6bbdec865f5cd1cfe40c2f3b2ca293c))
* **jans-auth-server:** specify minimum acr for clients [#343](https://github.com/JanssenProject/jans/issues/343) ([#3083](https://github.com/JanssenProject/jans/issues/3083)) ([b0034ec](https://github.com/JanssenProject/jans/commit/b0034ec509ace1a4e30a7e9c6dd23dca48178c62))
* **jans-auth-server:** swagger docs for ssa ([#2953](https://github.com/JanssenProject/jans/issues/2953)) ([7f93bca](https://github.com/JanssenProject/jans/commit/7f93bca9ff101d85f1ae389602f99c7c6af9bc17))
* **jans-auth-server:** updated mau on refreshing access token [#2955](https://github.com/JanssenProject/jans/issues/2955) ([#3025](https://github.com/JanssenProject/jans/issues/3025)) ([56de619](https://github.com/JanssenProject/jans/commit/56de61974ae0d2a3d8382191c2aae479a062e9b2))
* jans-linux-setup include permission of all user roles ([#3009](https://github.com/JanssenProject/jans/issues/3009)) ([62a421d](https://github.com/JanssenProject/jans/commit/62a421df821067432cbcced0e89cc2a410cd40be))
* **jans-pycloudlib:** add AWS Secrets Manager support for configuration layers ([#3112](https://github.com/JanssenProject/jans/issues/3112)) ([0522e61](https://github.com/JanssenProject/jans/commit/0522e61809b9052adce4fdb0db77e2d71558144e))


### Bug Fixes

* (jans-auth-server): fixed Client serialization/deserialization issue [#2946](https://github.com/JanssenProject/jans/issues/2946) ([#3064](https://github.com/JanssenProject/jans/issues/3064)) ([31b5bfc](https://github.com/JanssenProject/jans/commit/31b5bfc2d626a94998c6e0a1d9121579858437e3))
* (jans-auth-server): fixed client's sortby [#3075](https://github.com/JanssenProject/jans/issues/3075) ([#3079](https://github.com/JanssenProject/jans/issues/3079)) ([e6b0e58](https://github.com/JanssenProject/jans/commit/e6b0e58c7336c2c6537fb55557527abe09ab0811))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - fido script, doc already moved to script-catalog ([#2982](https://github.com/JanssenProject/jans/issues/2982)) ([10d8df5](https://github.com/JanssenProject/jans/commit/10d8df5480853a0545dbe6350f494d6b4abf3661))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - irrelevant folder, agama script is in agama-bridge ([#2993](https://github.com/JanssenProject/jans/issues/2993)) ([d19b13a](https://github.com/JanssenProject/jans/commit/d19b13ab31e1dbfee9288c6569b289eae213b528))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - removing inwebo ([#2975](https://github.com/JanssenProject/jans/issues/2975)) ([052f91f](https://github.com/JanssenProject/jans/commit/052f91fd45c888efb7480fc7cd403dc005ceca23))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - SIWA and SIWG (Sign in with Apple-Google), moved to script-catalog ([#2983](https://github.com/JanssenProject/jans/issues/2983)) ([402e7ae](https://github.com/JanssenProject/jans/commit/402e7aebd20322ef465a3805d3834c7174bc9bbc))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) removing duplicated files ([#3007](https://github.com/JanssenProject/jans/issues/3007)) ([9f3d051](https://github.com/JanssenProject/jans/commit/9f3d051308e7b29e5f112e74601aa05c42ed559c))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) scripts-catalog folder restructuring ([#2999](https://github.com/JanssenProject/jans/issues/2999)) ([7b66f2b](https://github.com/JanssenProject/jans/commit/7b66f2b27517ba560555f64d0ab4e49f10ddb374))
* [#2666](https://github.com/JanssenProject/jans/issues/2666) ([#3011](https://github.com/JanssenProject/jans/issues/3011)) ([f98cbc5](https://github.com/JanssenProject/jans/commit/f98cbc5d43b12b56b81debd367fcfdfdc75830e4))
* client-name, logout, user ([#3122](https://github.com/JanssenProject/jans/issues/3122)) ([f374831](https://github.com/JanssenProject/jans/commit/f3748312dc72288d9eea9cf1efba8f2afb5278a9))
* **config-api:** error handling for agama get and org patch ([#3028](https://github.com/JanssenProject/jans/issues/3028)) ([21dd6e5](https://github.com/JanssenProject/jans/commit/21dd6e5f273e968245508d6a03a8ac7b6cfd3125))
* **config-api:** fix for swagger spec for scope creation and sessoin endpoint filter ([#2949](https://github.com/JanssenProject/jans/issues/2949)) ([2989f1d](https://github.com/JanssenProject/jans/commit/2989f1dc151a77ecc66408ccccdfbb18d3b9dca8))
* **config-api:** swagger update for enum and error handling ([#2934](https://github.com/JanssenProject/jans/issues/2934)) ([6b61556](https://github.com/JanssenProject/jans/commit/6b61556b49cca96622c2e59b1e99244a7eaae3ab))
* **demo:** incorrect URL to helm charts location ([#2935](https://github.com/JanssenProject/jans/issues/2935)) ([b7e395b](https://github.com/JanssenProject/jans/commit/b7e395be337a0acca396dd4c684b7b1629a2304a))
* disable github authentication and interception scripts by default and other changes. [#3022](https://github.com/JanssenProject/jans/issues/3022) ([#3023](https://github.com/JanssenProject/jans/issues/3023)) ([13f5998](https://github.com/JanssenProject/jans/commit/13f599830c0d6b48bd1cd6f71f3d200ec6bddfe7))
* ensure non-empty array is generated [#2672](https://github.com/JanssenProject/jans/issues/2672) ([#3047](https://github.com/JanssenProject/jans/issues/3047)) ([47902bd](https://github.com/JanssenProject/jans/commit/47902bd8a5c7fe60cbac6f819787ee90726ac4a0))
* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* **jans-auth-server:** corrected regression made in token request [#2921](https://github.com/JanssenProject/jans/issues/2921) ([#2922](https://github.com/JanssenProject/jans/issues/2922)) ([deeae74](https://github.com/JanssenProject/jans/commit/deeae748aa465e3789114a93eee251628f9d365b))
* **jans-auth-server:** wrong import in GluuOrganization class which leads to failure on jans-config-api [#2957](https://github.com/JanssenProject/jans/issues/2957) ([#2958](https://github.com/JanssenProject/jans/issues/2958)) ([af4eda8](https://github.com/JanssenProject/jans/commit/af4eda83147b3fb13f3cc97153d6186c7dcdda74))
* **jans-auth-server:** wrong userinfo_encryption_enc_values_supported in OpenID Configuration [#2725](https://github.com/JanssenProject/jans/issues/2725) ([#2951](https://github.com/JanssenProject/jans/issues/2951)) ([bc1a8ca](https://github.com/JanssenProject/jans/commit/bc1a8ca8b2c7e3b286f2762d9e84205f402cce4a))
* jans-cli-docs update links ([#3118](https://github.com/JanssenProject/jans/issues/3118)) ([04fbb98](https://github.com/JanssenProject/jans/commit/04fbb982324cf8849eb3cfac6800f917c63d5300))
* **jans-config-api:** user attributes not updated [#2753](https://github.com/JanssenProject/jans/issues/2753) ([082cfe3](https://github.com/JanssenProject/jans/commit/082cfe32dca1321a333c520a142b95cab1cfa0d8))
* **jans-config-api:** user attributes not updated [#2753](https://github.com/JanssenProject/jans/issues/2753) ([#3110](https://github.com/JanssenProject/jans/issues/3110)) ([803468b](https://github.com/JanssenProject/jans/commit/803468b895e022bbb20ec1975f11aafd08a214f2))
* **jans-fido2:** [#1120](https://github.com/JanssenProject/jans/issues/1120) ([#2928](https://github.com/JanssenProject/jans/issues/2928)) ([0fea95a](https://github.com/JanssenProject/jans/commit/0fea95a181811de2f592debcec5af76f9adda5b2))
* **jans-fido2:** [#2840](https://github.com/JanssenProject/jans/issues/2840) ([#2974](https://github.com/JanssenProject/jans/issues/2974)) ([d3351e1](https://github.com/JanssenProject/jans/commit/d3351e141fa546074ad98891e655247a0c23e30a))
* **jans-fido2:** [#2971](https://github.com/JanssenProject/jans/issues/2971) ([#2972](https://github.com/JanssenProject/jans/issues/2972)) ([2f15cf8](https://github.com/JanssenProject/jans/commit/2f15cf8aba64410fe1dd0ef71e9860ae0ec919bd))
* jans-linux-setup fido2 script placeholder in scripts template ([#2986](https://github.com/JanssenProject/jans/issues/2986)) ([70a4fe0](https://github.com/JanssenProject/jans/commit/70a4fe0d4b6ee958cfaa0b1598092da0fde38620))
* jans-linux-setup remove opendj sysv script ([#2998](https://github.com/JanssenProject/jans/issues/2998)) ([13eebe4](https://github.com/JanssenProject/jans/commit/13eebe4bdcbc059eb40b3e33b9bfb4a2830e8a0b))
* jans-linux-setup service description for jans-auth ([#2989](https://github.com/JanssenProject/jans/issues/2989)) ([6566d27](https://github.com/JanssenProject/jans/commit/6566d272de1e2fdcb5040df6d1616bd3164ebdab))
* **jans:** added null check to avoid NullPointerException ([#3077](https://github.com/JanssenProject/jans/issues/3077)) ([42d49b2](https://github.com/JanssenProject/jans/commit/42d49b2ac2ffb50086b5941c93c810cdbaff75ea))
* the admin-ui role/permission/mapping delete apis are not protected by appropriate permissions [#2991](https://github.com/JanssenProject/jans/issues/2991) ([#2992](https://github.com/JanssenProject/jans/issues/2992)) ([7d68021](https://github.com/JanssenProject/jans/commit/7d680219c1db037fa4ee137a5d7241753c32b20a))
* typo ([#2950](https://github.com/JanssenProject/jans/issues/2950)) ([6df810b](https://github.com/JanssenProject/jans/commit/6df810b36c68303f458f694e4ba6ec9c3768364c))

## [1.0.4](https://github.com/JanssenProject/jans/compare/v1.0.3...v1.0.4) (2022-11-08)


### Features

* for file based scripts check both script revision and file ([#2878](https://github.com/JanssenProject/jans/issues/2878)) ([97ab071](https://github.com/JanssenProject/jans/commit/97ab0712e39605520b3ac8fb8df1c00bf0437797))
* **jans-auth-server:** added token exchange support to client [#2518](https://github.com/JanssenProject/jans/issues/2518) ([#2855](https://github.com/JanssenProject/jans/issues/2855)) ([943d99f](https://github.com/JanssenProject/jans/commit/943d99f2784e671d361c66c1ddb82c10f567a698))
* **jans-auth-server:** ssa validation endpoint ([#2842](https://github.com/JanssenProject/jans/issues/2842)) ([de8a86e](https://github.com/JanssenProject/jans/commit/de8a86ed1eb29bd02546e9e22fc6f668ac3217c4))
* ssa revoke endpoint ([#2865](https://github.com/JanssenProject/jans/issues/2865)) ([9c68f91](https://github.com/JanssenProject/jans/commit/9c68f914e155de492e54121033c8f0ed45d66817))


### Bug Fixes

* [#2825](https://github.com/JanssenProject/jans/issues/2825) ([#2828](https://github.com/JanssenProject/jans/issues/2828)) ([5ce21aa](https://github.com/JanssenProject/jans/commit/5ce21aac5df54d2fe9402479fd221fffe9dc77ef))
* avoid NPE when configuration is missing [#2857](https://github.com/JanssenProject/jans/issues/2857) ([#2863](https://github.com/JanssenProject/jans/issues/2863)) ([4a27091](https://github.com/JanssenProject/jans/commit/4a2709185bd7ba84c3230cb94d4efea940681742))
* **config-api:** fixes for client creation, enum handling ([#2854](https://github.com/JanssenProject/jans/issues/2854)) ([3121493](https://github.com/JanssenProject/jans/commit/312149393337ff2b2c794053a729c0f0919caa31))
* fix OR filters join when sub-filters uses lower ([#2850](https://github.com/JanssenProject/jans/issues/2850)) ([3dc6b32](https://github.com/JanssenProject/jans/commit/3dc6b329aacbb6958efd834c85143231595b0d99))
* **image:** add missing write access in filesystem ([#2846](https://github.com/JanssenProject/jans/issues/2846)) ([db4670d](https://github.com/JanssenProject/jans/commit/db4670d3adabc38b42411fe11bdcb6d6a9b4a0bd)), closes [#2844](https://github.com/JanssenProject/jans/issues/2844)
* **image:** multiple dynamic scopes created when using 2 replicas ([#2871](https://github.com/JanssenProject/jans/issues/2871)) ([5e0f1e6](https://github.com/JanssenProject/jans/commit/5e0f1e69023da264333e3786fcf994539054be71))
* **jans-auth-server:** fix language metadata format ([#2883](https://github.com/JanssenProject/jans/issues/2883)) ([e21e206](https://github.com/JanssenProject/jans/commit/e21e206df16b048b1743c3ee441d9fbdb1f8c67e))
* jans-linux-setup render webapps.xml ([#2839](https://github.com/JanssenProject/jans/issues/2839)) ([ed8fa84](https://github.com/JanssenProject/jans/commit/ed8fa8462b69b37f44d0e5b5bb65345ea96ecc45))
* **pycloudlib:** searching values from spanner returns empty set ([#2833](https://github.com/JanssenProject/jans/issues/2833)) ([861a065](https://github.com/JanssenProject/jans/commit/861a0657233f271ffa41c908ce68a2206ed970fd))

## 1.0.3 (2022-11-01)


### Features

* add inum claim in profile scope [#2095](https://github.com/JanssenProject/jans/issues/2095) ([#2096](https://github.com/JanssenProject/jans/issues/2096)) ([f67c32e](https://github.com/JanssenProject/jans/commit/f67c32e7891f95c7a00ad0fa263444214dcaecd5))
* add new methods to allow get/set list of custom attributes from ([#2105](https://github.com/JanssenProject/jans/issues/2105)) ([5ac23a1](https://github.com/JanssenProject/jans/commit/5ac23a18adf3b34fd41fd1199ab168bfc9602fc6)), closes [#2104](https://github.com/JanssenProject/jans/issues/2104)
* admin-ui apis refactoring [#2388](https://github.com/JanssenProject/jans/issues/2388) ([#2390](https://github.com/JanssenProject/jans/issues/2390)) ([c7b26e9](https://github.com/JanssenProject/jans/commit/c7b26e90430a1db5d4788d510fc8bf5ce63c4fd3))
* **agama:** add utility classes for inbound identity ([#2204](https://github.com/JanssenProject/jans/issues/2204)) ([29f58ee](https://github.com/JanssenProject/jans/commit/29f58ee0e6c84b4af5493cabcb19167bc7ffbe40))
* **agama:** add utility classes for inbound identity ([#2231](https://github.com/JanssenProject/jans/issues/2231)) ([96e32a4](https://github.com/JanssenProject/jans/commit/96e32a407ec6c545b73a6fd103ed2ae5876bd500))
* **agama:** add utility classes for inbound identity ([#2280](https://github.com/JanssenProject/jans/issues/2280)) ([ca6fdc9](https://github.com/JanssenProject/jans/commit/ca6fdc90256e4ef103bf50dc27cb694c940ba70b))
* **agama:** add utility classes for inbound identity ([#2417](https://github.com/JanssenProject/jans/issues/2417)) ([2878bdd](https://github.com/JanssenProject/jans/commit/2878bdd737b4bd7f8f080113826a4bc4bf49ffba))
* **config-api:** multiple pattern handling for search request ([#2590](https://github.com/JanssenProject/jans/issues/2590)) ([46886fb](https://github.com/JanssenProject/jans/commit/46886fb1ec80724ddb0b948fc25f4554566ee8ab))
* **config-api:** multiple pattern search in attribute api ([#2491](https://github.com/JanssenProject/jans/issues/2491)) ([9f646ff](https://github.com/JanssenProject/jans/commit/9f646ff066b9bcb6525e77e29664832e5f20077e))
* disable TLS in CB client by default ([#2167](https://github.com/JanssenProject/jans/issues/2167)) ([8ec5dd3](https://github.com/JanssenProject/jans/commit/8ec5dd3dc9818a53949468389a1918ed385c28a9))
* **docker-jans-fido2:** allow creating initial persistence entry ([#2029](https://github.com/JanssenProject/jans/issues/2029)) ([41dfab7](https://github.com/JanssenProject/jans/commit/41dfab7a6d09d10b05954d4f5fae2437eb81a885))
* **docker-jans-monolith:** add docker jans monolith ([#2323](https://github.com/JanssenProject/jans/issues/2323)) ([ba511c3](https://github.com/JanssenProject/jans/commit/ba511c3e23230da4f924ec48ea5d8fb9e02027f9))
* **docker-jans-scim:** allow creating initial persistence entry ([#2035](https://github.com/JanssenProject/jans/issues/2035)) ([e485618](https://github.com/JanssenProject/jans/commit/e4856186d566f9ac7b08395b59cd75e389e5c161))
* fix susrefire tests in filter module ([#2141](https://github.com/JanssenProject/jans/issues/2141)) ([118d77c](https://github.com/JanssenProject/jans/commit/118d77cd7025dcbe3031bc41450ec285afff4b9f))
* generate zip files with CB/Spanner dependencies ([#2715](https://github.com/JanssenProject/jans/issues/2715)) ([e4e2670](https://github.com/JanssenProject/jans/commit/e4e2670a33c6753ab2309ee473ab6b23fe5577d2))
* **image:** add custom libs for couchbase and spanner persistence ([#2784](https://github.com/JanssenProject/jans/issues/2784)) ([db559dd](https://github.com/JanssenProject/jans/commit/db559ddc5e74cc7387720af7f084766c054541b5))
* **image:** add token-exchange and device secret ([#2788](https://github.com/JanssenProject/jans/issues/2788)) ([87c4676](https://github.com/JanssenProject/jans/commit/87c467631a60567d6de8d3ed8d9815be85f4f1d0))
* **image:** generate scopes from swagger/openapi files ([#2759](https://github.com/JanssenProject/jans/issues/2759)) ([63722ea](https://github.com/JanssenProject/jans/commit/63722ea7372f33bf2ad2c3ff01b068383e81e746))
* **jans-auth-server:** add access_token_singing_alg_values_supported to discovery [#2372](https://github.com/JanssenProject/jans/issues/2372) ([#2403](https://github.com/JanssenProject/jans/issues/2403)) ([3784c83](https://github.com/JanssenProject/jans/commit/3784c837073c7a45871efc11dac1b721ae710cf1))
* **jans-auth-server:** added allowSpontaneousScopes AS json config [#2074](https://github.com/JanssenProject/jans/issues/2074) ([#2111](https://github.com/JanssenProject/jans/issues/2111)) ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* **jans-auth-server:** added convenient idTokenLifetime client property [#2656](https://github.com/JanssenProject/jans/issues/2656) ([#2668](https://github.com/JanssenProject/jans/issues/2668)) ([f97bfce](https://github.com/JanssenProject/jans/commit/f97bfceae5917442e7d6c3134e5601149d5235e0))
* **jans-auth-server:** added creator info to scope (time/id/type) [#1934](https://github.com/JanssenProject/jans/issues/1934) ([#2023](https://github.com/JanssenProject/jans/issues/2023)) ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* **jans-auth-server:** allow authentication for max_age=0 [#2361](https://github.com/JanssenProject/jans/issues/2361) ([#2362](https://github.com/JanssenProject/jans/issues/2362)) ([aed6ee3](https://github.com/JanssenProject/jans/commit/aed6ee3dd570e15fa91a9baf3ffb2461a212cdc0))
* **jans-auth-server:** allow end session with expired id_token_hint (by checking signature and sid) [#2430](https://github.com/JanssenProject/jans/issues/2430) ([#2431](https://github.com/JanssenProject/jans/issues/2431)) ([1b46b44](https://github.com/JanssenProject/jans/commit/1b46b44c6a1bac9c52c7d45358ced4c2c60a9314))
* **jans-auth-server:** Draft support of OpenID Connect Native SSO  ([#2711](https://github.com/JanssenProject/jans/issues/2711)) ([595d1aa](https://github.com/JanssenProject/jans/commit/595d1aa8ce93c00aa13fb726499ca26d8f2a41b6))
* **jans-auth-server:** extended client schema - added jansClientGroup [#1824](https://github.com/JanssenProject/jans/issues/1824) ([#2299](https://github.com/JanssenProject/jans/issues/2299)) ([29cfd4e](https://github.com/JanssenProject/jans/commit/29cfd4edaff1248c65d43d956b7b1db0f684d294))
* **jans-auth-server:** renamed "enabledComponents" conf property -&gt; "featureFlags" [#2290](https://github.com/JanssenProject/jans/issues/2290) ([#2319](https://github.com/JanssenProject/jans/issues/2319)) ([56a33c4](https://github.com/JanssenProject/jans/commit/56a33c40a2bf58ebeb87c6f1724f60a836dc29d2))
* **jans-auth-server:** updating arquillian tests 1247 ([#2017](https://github.com/JanssenProject/jans/issues/2017)) ([ee200a7](https://github.com/JanssenProject/jans/commit/ee200a7dce5d750f3c4a9d536aa8d92a89926711))
* **jans-config-api:** added new attributes ([#1940](https://github.com/JanssenProject/jans/issues/1940)) ([757b22f](https://github.com/JanssenProject/jans/commit/757b22fcc03c28d950eb98b4503d1915fa15b025))
* **jans-config-api:** agama flow endpoint ([#1898](https://github.com/JanssenProject/jans/issues/1898)) ([0e73306](https://github.com/JanssenProject/jans/commit/0e73306f7642a74a3ed2cf8a8687a1ea447aa7bd))
* **jans-config-api:** agama patch endpoint ([#2028](https://github.com/JanssenProject/jans/issues/2028)) ([0b96a95](https://github.com/JanssenProject/jans/commit/0b96a95399cac02fee614523ae5b552c99c1e254))
* **jans-config-api:** endpoint to get UmaResource based on clientId and swagger changes ([#1912](https://github.com/JanssenProject/jans/issues/1912)) ([a3f9145](https://github.com/JanssenProject/jans/commit/a3f91453dd8fa5e9c903827b458bc58e735eda55))
* **jans-config-api:** enhancement to agama and uma resource endpoint  ([#2015](https://github.com/JanssenProject/jans/issues/2015)) ([f2c19a1](https://github.com/JanssenProject/jans/commit/f2c19a14b0b5b869eb97afd24ac7169328f22b2f))
* **jans-config-api:** fetch the associated clients_id in GET scopes api response  ([#1946](https://github.com/JanssenProject/jans/issues/1946)) ([ffe743c](https://github.com/JanssenProject/jans/commit/ffe743ca007d3a1ca7011f47df9c0a4124c93e5c))
* **jans-config-api:** new endpoint to fetch scope by creator and type ([#2098](https://github.com/JanssenProject/jans/issues/2098)) ([cf15d67](https://github.com/JanssenProject/jans/commit/cf15d678f26de3bb2e645040ad25bcb21a03691f))
* **jans-config-api:** pagination functionality for attribute endoint ([#2232](https://github.com/JanssenProject/jans/issues/2232)) ([8bb8b70](https://github.com/JanssenProject/jans/commit/8bb8b700ae4986707d35af39e8811c80a5073023))
* **jans-config-api:** pagination functionality for endpoints and swagger spec rectification ([#2397](https://github.com/JanssenProject/jans/issues/2397)) ([d893e13](https://github.com/JanssenProject/jans/commit/d893e13efd57871ed0a09688b9b02f4294a10d4f))
* **jans-config-api:** Scope object changes for creator details ([#2033](https://github.com/JanssenProject/jans/issues/2033)) ([a8b8d76](https://github.com/JanssenProject/jans/commit/a8b8d76640ff6520a462ff2bb477db50c2b60207))
* **jans-config-api:** session management endpoint ([#2158](https://github.com/JanssenProject/jans/issues/2158)) ([30f6e1a](https://github.com/JanssenProject/jans/commit/30f6e1a4bacb90a711ed6f91bc124267d44b9d44))
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
* merge ORM from Gluu ([#2468](https://github.com/JanssenProject/jans/issues/2468)) ([93149fd](https://github.com/JanssenProject/jans/commit/93149fd2d1db1eacf37d76af9df25ed40fb16b79))
* need to fetch the associated clients_id in GET scopes api response [#1923](https://github.com/JanssenProject/jans/issues/1923) ([#1949](https://github.com/JanssenProject/jans/issues/1949)) ([88606a5](https://github.com/JanssenProject/jans/commit/88606a5ad01b9444f533ee4ea85ea0ca57dc49d8))
* ssa creation endpoint ([#2495](https://github.com/JanssenProject/jans/issues/2495)) ([61c83e3](https://github.com/JanssenProject/jans/commit/61c83e3305beeaf1a3dbde39d70324153281f218))
* sync mds v3 config in fido2 image ([#2531](https://github.com/JanssenProject/jans/issues/2531)) ([56c8442](https://github.com/JanssenProject/jans/commit/56c84422b9352dd16c8dbce8608b5327a92a2c2e))
* update Coucbase ORM to conform SDK 3.x (config updates) [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#2118](https://github.com/JanssenProject/jans/issues/2118)) ([fceec83](https://github.com/JanssenProject/jans/commit/fceec8332fb36826e5dccb797ee79b769859e126))
* update search by example to use multivalued property ([#2298](https://github.com/JanssenProject/jans/issues/2298)) ([8ed3007](https://github.com/JanssenProject/jans/commit/8ed3007ec3406ac7c0adf81ce339f6672b76a147))
* upgrade javax.servlet:javax.servlet-api from 3.1.0 to 4.0.1 ([#646](https://github.com/JanssenProject/jans/issues/646)) ([d186a05](https://github.com/JanssenProject/jans/commit/d186a05fd566095860f3b17ce4aa2b32551b2bc6))
* upgrade org.jboss.resteasy:resteasy-servlet-initializer from 4.5.10.Final to 5.0.1.Final ([#645](https://github.com/JanssenProject/jans/issues/645)) ([a9a712d](https://github.com/JanssenProject/jans/commit/a9a712dcaa69c63ffac46206d5dfc13978efc7fb))
* upgrade org.jetbrains:annotations from 18.0.0 to 23.0.0 ([#637](https://github.com/JanssenProject/jans/issues/637)) ([e5fca5a](https://github.com/JanssenProject/jans/commit/e5fca5a09e8aa68dc834de6115490a80fb9da3ff))
* use entry in contain entry ([#2311](https://github.com/JanssenProject/jans/issues/2311)) ([de9d00a](https://github.com/JanssenProject/jans/commit/de9d00a9e3cf053cfad70726317becf3b2d9eac5))


### Bug Fixes

* [#2143](https://github.com/JanssenProject/jans/issues/2143) ([#2144](https://github.com/JanssenProject/jans/issues/2144)) ([ff7f9f4](https://github.com/JanssenProject/jans/commit/ff7f9f4110d72b333aae0d2332b429dcbd067da3))
* [#2157](https://github.com/JanssenProject/jans/issues/2157) ([#2159](https://github.com/JanssenProject/jans/issues/2159)) ([dc8cb60](https://github.com/JanssenProject/jans/commit/dc8cb60990052256b46842f85ebf4961beee82dd))
* [#776](https://github.com/JanssenProject/jans/issues/776) ([#2503](https://github.com/JanssenProject/jans/issues/2503)) ([a564431](https://github.com/JanssenProject/jans/commit/a564431c8b6e503a36dbaf7ccc8f79e6b8adb95f))
* [#817](https://github.com/JanssenProject/jans/issues/817) - script for DUO should have the universal prompt, other APIs are deprecated + documentation minor fixes ([#2363](https://github.com/JanssenProject/jans/issues/2363)) ([ccc13af](https://github.com/JanssenProject/jans/commit/ccc13afdd2cfefecc19aa926a46815d119e6ad76))
* [#817](https://github.com/JanssenProject/jans/issues/817) ([#2364](https://github.com/JanssenProject/jans/issues/2364)) ([bbcd87a](https://github.com/JanssenProject/jans/commit/bbcd87a374f2efca3f87b00bfa21900b2b450c1b))
* admin-ui plugin should use encoded client_secret for authentication [#2717](https://github.com/JanssenProject/jans/issues/2717) ([#2718](https://github.com/JanssenProject/jans/issues/2718)) ([cc0020e](https://github.com/JanssenProject/jans/commit/cc0020ec94b8cfe18c75310eb77c26bfa6e85750))
* backticks CB attributes in N1QL ([#2313](https://github.com/JanssenProject/jans/issues/2313)) ([d6db13d](https://github.com/JanssenProject/jans/commit/d6db13d15fa0176eab52d37e01f01c6d60f1d124))
* cb ttl update sdk 3 ([#2434](https://github.com/JanssenProject/jans/issues/2434)) ([534c6cb](https://github.com/JanssenProject/jans/commit/534c6cb01d2e3e2a3e066d2af71780c28e01e282))
* close connections after reading metadata ([#2327](https://github.com/JanssenProject/jans/issues/2327)) ([00f4f79](https://github.com/JanssenProject/jans/commit/00f4f790fc01292e23139072bd0d19925349c7a6))
* **config-api:** client default value handling ([#2585](https://github.com/JanssenProject/jans/issues/2585)) ([fbcbbad](https://github.com/JanssenProject/jans/commit/fbcbbad0817cd17e645a2491d1732a18b5159cf1))
* **config-api:** fix for acr error handling and spec enhancement for example ([#2443](https://github.com/JanssenProject/jans/issues/2443)) ([8113841](https://github.com/JanssenProject/jans/commit/8113841f160f937b765b005bc9078bbcc7bb3ec7))
* **config-api:** fix for assosiated client not fetched for scope ([#2540](https://github.com/JanssenProject/jans/issues/2540)) ([08488d1](https://github.com/JanssenProject/jans/commit/08488d158841a8c074e68cba4d2a12df6feab9b6))
* **config-api:** fix for returning associated-clients for scope ([#2567](https://github.com/JanssenProject/jans/issues/2567)) ([e623f64](https://github.com/JanssenProject/jans/commit/e623f644d1a410a95edf9c0d66085dd4503e7cb3))
* **config-api:** fixing discrepancies in the api ([#2216](https://github.com/JanssenProject/jans/issues/2216)) ([af4d3a5](https://github.com/JanssenProject/jans/commit/af4d3a51ce2cbe8c531f8dca213d0c3ef087aad5))
* **config-api:** rectified sortBy field for Agama resource ([#2513](https://github.com/JanssenProject/jans/issues/2513)) ([bb3ac95](https://github.com/JanssenProject/jans/commit/bb3ac957615a46d53b8622d1aeead1b26a703b70))
* **config-api:** removing CB and MySqlDB endpoints and swagger fixes ([#2480](https://github.com/JanssenProject/jans/issues/2480)) ([cc68cc9](https://github.com/JanssenProject/jans/commit/cc68cc9209c3612dbd002daf5775f3dd5916427b))
* **config-api:** scope addiotion while client creation ([#2714](https://github.com/JanssenProject/jans/issues/2714)) ([d51ae24](https://github.com/JanssenProject/jans/commit/d51ae2421b77dd88f7a1751235200b19f4a0b305))
* **config-api:** search filter logic for attribute resource ([#2310](https://github.com/JanssenProject/jans/issues/2310)) ([c75ff1d](https://github.com/JanssenProject/jans/commit/c75ff1dff7a1c4262704320a309de540bae40cc2))
* **config-api:** specifying JSON values for Attribute enum properties ([#2663](https://github.com/JanssenProject/jans/issues/2663)) ([55d20c8](https://github.com/JanssenProject/jans/commit/55d20c8c4812cee333e63335772ff6da9a43d188))
* **config-api:** swagger fixes for missing scope and admin-ui paths ([#2697](https://github.com/JanssenProject/jans/issues/2697)) ([6dcbff3](https://github.com/JanssenProject/jans/commit/6dcbff37d0e37502b7383bded22f04bf708d7afc))
* **config-api:** swagger spec changes for session, properties endpoint and ignoring customobject for non LDAP DB ([#2348](https://github.com/JanssenProject/jans/issues/2348)) ([c6acaac](https://github.com/JanssenProject/jans/commit/c6acaacf06f564b1acde7ed9bb466e5e9528ccbc))
* **docs:** fix MarkupSafe hash ([#2699](https://github.com/JanssenProject/jans/issues/2699)) ([adf2a6d](https://github.com/JanssenProject/jans/commit/adf2a6d929082da8884973855a095153c1268269))
* **docs:** revert MarkupSafe hash ([#2701](https://github.com/JanssenProject/jans/issues/2701)) ([e722aed](https://github.com/JanssenProject/jans/commit/e722aedd2b8fbbba770cea1112e2757f6e32d10c))
* don't backticks all in N1QL ([#2316](https://github.com/JanssenProject/jans/issues/2316)) ([7cc721e](https://github.com/JanssenProject/jans/commit/7cc721e9d0d831b7d11804511e0279c8c292c0b2))
* don't execute next paged search if current result count less than ([#2171](https://github.com/JanssenProject/jans/issues/2171)) ([94a162f](https://github.com/JanssenProject/jans/commit/94a162f4471ec6e4798721894b4a5a583ad71370))
* fix search with % ([#2307](https://github.com/JanssenProject/jans/issues/2307)) ([90987d7](https://github.com/JanssenProject/jans/commit/90987d74118b93637c568c73d747233861b2bd9b))
* fixed multiple encoding issue during authz ([#2152](https://github.com/JanssenProject/jans/issues/2152)) ([fb0b6d7](https://github.com/JanssenProject/jans/commit/fb0b6d738e3e6292453958d44cb14fdcf03ab416))
* **forgot_password:** update imports to jans locations ([#1637](https://github.com/JanssenProject/jans/issues/1637)) ([6c6eeb3](https://github.com/JanssenProject/jans/commit/6c6eeb334ffd4e09c91a7e5b7e5c3de7d5fdf037)), closes [#1601](https://github.com/JanssenProject/jans/issues/1601)
* **image:** add missing script for openbanking installation ([#2618](https://github.com/JanssenProject/jans/issues/2618)) ([de775a7](https://github.com/JanssenProject/jans/commit/de775a731fe00ccd6044aa331cb039625231a174))
* **image:** add missing ssa configuration ([#2613](https://github.com/JanssenProject/jans/issues/2613)) ([b70b8b2](https://github.com/JanssenProject/jans/commit/b70b8b2b06f08d82bca0e47292dd68b0651c8dee))
* **image:** handle vulnerabilities reported by artifacthub scanner ([#2436](https://github.com/JanssenProject/jans/issues/2436)) ([77d8d88](https://github.com/JanssenProject/jans/commit/77d8d888bf414e519345704d033e65fbf4bc4128))
* **image:** missing configuration for openbanking discovery ([#2796](https://github.com/JanssenProject/jans/issues/2796)) ([ba664f0](https://github.com/JanssenProject/jans/commit/ba664f012a39d5e43df6e0d42993f4a1a5ba5d3a))
* **image:** remove metadata-root-ca.cer inside fido2 image ([#2603](https://github.com/JanssenProject/jans/issues/2603)) ([9461fbc](https://github.com/JanssenProject/jans/commit/9461fbcb193f0e9b929f53006d706bf3ef5bfc1a))
* **images:** conform to new couchbase persistence configuration ([#2188](https://github.com/JanssenProject/jans/issues/2188)) ([c708542](https://github.com/JanssenProject/jans/commit/c7085427fd298f74e8809ef4d6c39f780fa83776))
* include idtoken with dynamic scopes for ciba ([#2108](https://github.com/JanssenProject/jans/issues/2108)) ([d9b5341](https://github.com/JanssenProject/jans/commit/d9b5341d50de972c910883c12785ce6d2758588f))
* incorrect contents [#817](https://github.com/JanssenProject/jans/issues/817) ([#2365](https://github.com/JanssenProject/jans/issues/2365)) ([746b33f](https://github.com/JanssenProject/jans/commit/746b33f16c46e2be7381f266e631047e21f17565))
* **jans auth server:** well known uppercase grant_types response_mode ([#2706](https://github.com/JanssenProject/jans/issues/2706)) ([39f613d](https://github.com/JanssenProject/jans/commit/39f613dbba9a218d9498baa43cc6baba0269b56a))
* Jans cli SCIM fixes ([#2394](https://github.com/JanssenProject/jans/issues/2394)) ([a009943](https://github.com/JanssenProject/jans/commit/a009943847238f2d115f794a21b5e229c851db5e))
* **jans-auth-server:** "login:prompt" property passed in request object JWT breaks authentication [#2493](https://github.com/JanssenProject/jans/issues/2493) ([#2537](https://github.com/JanssenProject/jans/issues/2537)) ([9d4d84a](https://github.com/JanssenProject/jans/commit/9d4d84a617999ba120a0b42376aa890e96f7c933))
* jans-auth-server/pom.xml to reduce vulnerabilities ([#2466](https://github.com/JanssenProject/jans/issues/2466)) ([86e62f9](https://github.com/JanssenProject/jans/commit/86e62f97362e7d834300fdd8300aee5db3f242fc))
* jans-auth-server/pom.xml to reduce vulnerabilities ([#2520](https://github.com/JanssenProject/jans/issues/2520)) ([f927692](https://github.com/JanssenProject/jans/commit/f92769225ad2e799c32621af8258fd4f9fead87e))
* **jans-auth-server:** added schema for ssa, corrected persistence, added ttl [#2543](https://github.com/JanssenProject/jans/issues/2543) ([#2544](https://github.com/JanssenProject/jans/issues/2544)) ([ce2bc3f](https://github.com/JanssenProject/jans/commit/ce2bc3f34d78dd9e11414d0db2c5870c77265177))
* **jans-auth-server:** client tests expects "scope to claim" mapping which are disabled by default [#1873](https://github.com/JanssenProject/jans/issues/1873) ([958cc92](https://github.com/JanssenProject/jans/commit/958cc9232fafa618cb326c7251486f0add7a15c1))
* **jans-auth-server:** fixing client tests effected by "scope to claim" mapping which is disabled by default [#1873](https://github.com/JanssenProject/jans/issues/1873) ([#1910](https://github.com/JanssenProject/jans/issues/1910)) ([6d81792](https://github.com/JanssenProject/jans/commit/6d81792a141ca725004c23f1bdd0a42314ffcb5f))
* **jans-auth-server:** generate description during built-in key rotation [#1790](https://github.com/JanssenProject/jans/issues/1790) ([#2068](https://github.com/JanssenProject/jans/issues/2068)) ([cd1a77d](https://github.com/JanssenProject/jans/commit/cd1a77dd36a59b19e975c013c8081610a23106ba))
* **jans-auth-server:** increased period of session authn time check ([#1918](https://github.com/JanssenProject/jans/issues/1918)) ([a41905a](https://github.com/JanssenProject/jans/commit/a41905abba38c051acc7e7d57131da4b7c3a1616))
* **jans-auth-server:** native sso - return device secret if device_sso scope is present [#2790](https://github.com/JanssenProject/jans/issues/2790) ([#2791](https://github.com/JanssenProject/jans/issues/2791)) ([9fa213f](https://github.com/JanssenProject/jans/commit/9fa213f12d4b2bafa399fb03ca207f692c44e01f))
* **jans-auth-server:** npe - regression in token endpoint ([#2763](https://github.com/JanssenProject/jans/issues/2763)) ([fe659d7](https://github.com/JanssenProject/jans/commit/fe659d76daf18439a72ca61421904a0f724a6755))
* **jans-auth-server:** NPE during OB discovery [#2793](https://github.com/JanssenProject/jans/issues/2793) ([#2794](https://github.com/JanssenProject/jans/issues/2794)) ([fb3ee86](https://github.com/JanssenProject/jans/commit/fb3ee86704e3255c51a121baff3ebf89eceb7f2a))
* **jans-auth-server:** npe in discovery if SSA endpoint is absent [#2497](https://github.com/JanssenProject/jans/issues/2497) ([#2498](https://github.com/JanssenProject/jans/issues/2498)) ([c3b00b4](https://github.com/JanssenProject/jans/commit/c3b00b4dac70f164216642cfa5b7f4e8e6a6d9dc))
* **jans-auth-server:** perform redirect_uri validation if FAPI flag is true [#2500](https://github.com/JanssenProject/jans/issues/2500) ([#2502](https://github.com/JanssenProject/jans/issues/2502)) ([aad0460](https://github.com/JanssenProject/jans/commit/aad04603ca714ed3b01dcaeddf2b80a8dccccdf4))
* **jans-auth-server:** PKCE parameters from first SSO request retains in further calls ([#2620](https://github.com/JanssenProject/jans/issues/2620)) ([de98b41](https://github.com/JanssenProject/jans/commit/de98b41ebd9285a087535d42a82dc004e885bd60))
* **jans-auth-server:** ssa get endpoint ([#2719](https://github.com/JanssenProject/jans/issues/2719)) ([35ffbf0](https://github.com/JanssenProject/jans/commit/35ffbf041e7da7376e07d8e7425a2925ce31f403))
* **jans-auth-server:** structure, instance customAttributes, initial data for ssa ([#2577](https://github.com/JanssenProject/jans/issues/2577)) ([f11f789](https://github.com/JanssenProject/jans/commit/f11f789e595762af0c38f1b93de4541ac456d282))
* jans-cli access token expiration ([#2352](https://github.com/JanssenProject/jans/issues/2352)) ([d506c8e](https://github.com/JanssenProject/jans/commit/d506c8e8960fbc899e3ad1072ccaa40e0713720a))
* jans-cli displayName for OpenID Clients with MySQL backend (ref: [#2314](https://github.com/JanssenProject/jans/issues/2314)) ([#2315](https://github.com/JanssenProject/jans/issues/2315)) ([e0dff68](https://github.com/JanssenProject/jans/commit/e0dff68524102aa78252725f6de4620bee944a29))
* jans-cli endpint param ([#2569](https://github.com/JanssenProject/jans/issues/2569)) ([f6faa71](https://github.com/JanssenProject/jans/commit/f6faa71b4c4803f42785f968c3cfef5d1c59affe))
* jans-cli fixes ([#2429](https://github.com/JanssenProject/jans/issues/2429)) ([c9673dc](https://github.com/JanssenProject/jans/commit/c9673dc12ab4f49fb4107b3695efebbf5b1652bd))
* jans-cli fixes ([#2515](https://github.com/JanssenProject/jans/issues/2515)) ([ccaacc8](https://github.com/JanssenProject/jans/commit/ccaacc8ae564e8f2ef5fd91134bc1c6512634bd5))
* jans-cli info for ConfigurationAgamaFlow ([#2561](https://github.com/JanssenProject/jans/issues/2561)) ([2c446a7](https://github.com/JanssenProject/jans/commit/2c446a7ce64407274e35639dda7e9a48f926988b))
* jans-cli tabulate attrbiutes ([#2321](https://github.com/JanssenProject/jans/issues/2321)) ([cb1e40d](https://github.com/JanssenProject/jans/commit/cb1e40d727bd652c71681509972097be5fae9b54))
* jans-cli user patch ([#2334](https://github.com/JanssenProject/jans/issues/2334)) ([fa3592b](https://github.com/JanssenProject/jans/commit/fa3592bbf76872a95524ec6bf9e2d24796d3f6e5))
* **jans-client-api:** upgrade seleniumhq version from 3.x to 4.x ([#2110](https://github.com/JanssenProject/jans/issues/2110)) ([d48271e](https://github.com/JanssenProject/jans/commit/d48271e872de72c7085e592988ad2e4e8950116d))
* jans-config-api parameter month is not mandatory for endpoint /stat ([#2459](https://github.com/JanssenProject/jans/issues/2459)) ([0654f98](https://github.com/JanssenProject/jans/commit/0654f981953da31b2b06b98ae93f4ffe27032155))
* jans-config-api/plugins/sample/demo/pom.xml to reduce vulnerabilities ([#2625](https://github.com/JanssenProject/jans/issues/2625)) ([14dd8a6](https://github.com/JanssenProject/jans/commit/14dd8a646b433bbbd655b02cabe548f8afb78196))
* jans-config-api/plugins/sample/demo/pom.xml to reduce vulnerabilities ([#853](https://github.com/JanssenProject/jans/issues/853)) ([2792b53](https://github.com/JanssenProject/jans/commit/2792b53c4e1baa4a06dc9d0fbdc3c15f285c08de))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#2630](https://github.com/JanssenProject/jans/issues/2630)) ([0e39fb7](https://github.com/JanssenProject/jans/commit/0e39fb7409be631418bcc9de58e2a4bb2d692268))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#2727](https://github.com/JanssenProject/jans/issues/2727)) ([5f42948](https://github.com/JanssenProject/jans/commit/5f42948b9453154fe795373f673fa872fa45e75b))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-config-api/pom.xml to reduce vulnerabilities ([#1464](https://github.com/JanssenProject/jans/issues/1464)) ([c832f98](https://github.com/JanssenProject/jans/commit/c832f9885bdec3a6fecffa2c2e3b7aa2ea7c8dd4))
* jans-config-api/pom.xml to reduce vulnerabilities ([#1746](https://github.com/JanssenProject/jans/issues/1746)) ([9dfe60e](https://github.com/JanssenProject/jans/commit/9dfe60e66f957d837bdeb0eb1c8bf9bedd30b60c))
* jans-config-api/pom.xml to reduce vulnerabilities ([#1780](https://github.com/JanssenProject/jans/issues/1780)) ([3252ff7](https://github.com/JanssenProject/jans/commit/3252ff75445603d955cd73214b47e3421dc5227b))
* jans-config-api/pom.xml to reduce vulnerabilities ([#2655](https://github.com/JanssenProject/jans/issues/2655)) ([499ff89](https://github.com/JanssenProject/jans/commit/499ff895f3b00ddeeaf3244e0becf35d7ede328a))
* **jans-config-api:** avoid loss of attributes in agama endpoints ([#2058](https://github.com/JanssenProject/jans/issues/2058)) ([3c8f816](https://github.com/JanssenProject/jans/commit/3c8f816b62b0efdfffc0e3f53d8371f4510d3ef6))
* **jans-config-api:** config-api compilation failed in main [#2030](https://github.com/JanssenProject/jans/issues/2030) ([#2031](https://github.com/JanssenProject/jans/issues/2031)) ([1659da1](https://github.com/JanssenProject/jans/commit/1659da1ff4d1d930300ef9c3b3e040eabc7bc0fb))
* **jans-config-api:** Fix to not update Metadata for PUT and PATCH agama endpoint ([#2046](https://github.com/JanssenProject/jans/issues/2046)) ([da93050](https://github.com/JanssenProject/jans/commit/da93050442d3bc1812d3a8076686ca3e02800c26))
* **jans-config-api:** issue UMA scope request being saved as OAUTH ([#2063](https://github.com/JanssenProject/jans/issues/2063)) ([81472aa](https://github.com/JanssenProject/jans/commit/81472aa3da4b02af7ed1bd47753d6938ec0c3e01))
* **jans-config-api:** rectified endpoint url in swagger spec for uma resource ([#1965](https://github.com/JanssenProject/jans/issues/1965)) ([0dc3b2e](https://github.com/JanssenProject/jans/commit/0dc3b2e60825f9921f28c9eeff30ffefa8bda269))
* **jans-core:** removed redundant reference [#1927](https://github.com/JanssenProject/jans/issues/1927) ([#1928](https://github.com/JanssenProject/jans/issues/1928)) ([064cbb8](https://github.com/JanssenProject/jans/commit/064cbb8040f4d7b21ff13e5f48c7f923c38f67b1))
* jans-eleven/pom.xml to reduce vulnerabilities ([#1463](https://github.com/JanssenProject/jans/issues/1463)) ([b6def37](https://github.com/JanssenProject/jans/commit/b6def37e3ca754a4294584b7c464455e3ed4b7eb))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2471](https://github.com/JanssenProject/jans/issues/2471)) ([903bda2](https://github.com/JanssenProject/jans/commit/903bda2c07cf299a72ccedae1d9fb427d4f993bb))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2519](https://github.com/JanssenProject/jans/issues/2519)) ([c0a2bbe](https://github.com/JanssenProject/jans/commit/c0a2bbe30bedb7153757d5ba6caa86b199e42a0a))
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
* **jans-scim:** improper handling response of get user operation ([#2420](https://github.com/JanssenProject/jans/issues/2420)) ([b9e00af](https://github.com/JanssenProject/jans/commit/b9e00af483f81280ae80ab6ad732d4f98c30c8a5))
* **jans-scim:** X509 cert not set after successful POST request ([#2407](https://github.com/JanssenProject/jans/issues/2407)) ([fd616c4](https://github.com/JanssenProject/jans/commit/fd616c49df088dfa7c6fae4bd56ed4ecf4e26418))
* **jans:** config api and client api ([#2408](https://github.com/JanssenProject/jans/issues/2408)) ([003af55](https://github.com/JanssenProject/jans/commit/003af55fc3657c3138b98f18d549ffa985d4c873))
* localized String should be converted to JSON ([#2542](https://github.com/JanssenProject/jans/issues/2542)) ([30225f9](https://github.com/JanssenProject/jans/commit/30225f99035fc023bd7b159f64b1a53cf03bf9ac))
* minor ([#2470](https://github.com/JanssenProject/jans/issues/2470)) ([657b9f7](https://github.com/JanssenProject/jans/commit/657b9f7589538bb4f1d61ce44fbf6f4da0e63d39))
* minor ([#2786](https://github.com/JanssenProject/jans/issues/2786)) ([3f67763](https://github.com/JanssenProject/jans/commit/3f677636cc2f871e5a9c683634334578405f18f3))
* moved contents under scripts-catalog ([#2370](https://github.com/JanssenProject/jans/issues/2370)) ([fa2273a](https://github.com/JanssenProject/jans/commit/fa2273a6400d512d13fb08ea6685d6ff56faf973))
* moved to script-catalog ([#2485](https://github.com/JanssenProject/jans/issues/2485)) ([960b87f](https://github.com/JanssenProject/jans/commit/960b87ff5ace40c63aada576816cae648e82d65c))
* **orm:** length check added before accessing CustomObjectAttribute values ([#2505](https://github.com/JanssenProject/jans/issues/2505)) ([6ff718f](https://github.com/JanssenProject/jans/commit/6ff718f2b2369e7669b3ce15d5442e4b0584ae7b))
* **pycloudlib:** handle type mismatch for iterable ([#2004](https://github.com/JanssenProject/jans/issues/2004)) ([46e0b2e](https://github.com/JanssenProject/jans/commit/46e0b2e4aff70a97cdcdcd0102dc83d294e45fdc))
* **pycloudlib:** set default values for JSONB column ([#2651](https://github.com/JanssenProject/jans/issues/2651)) ([9b536ab](https://github.com/JanssenProject/jans/commit/9b536ab2b5d398a41733790f2eeb70339f993fb7))
* random password for keystores ([#2102](https://github.com/JanssenProject/jans/issues/2102)) ([b7d9af1](https://github.com/JanssenProject/jans/commit/b7d9af12ecf946498d279a5f577db0528e5522bc))
* remove request-body from delete endpoints of admin-ui plugin [#2341](https://github.com/JanssenProject/jans/issues/2341) ([#2342](https://github.com/JanssenProject/jans/issues/2342)) ([1429a85](https://github.com/JanssenProject/jans/commit/1429a854e4fe2a80765d85ad8006706cc0cac15d))
* scan docs/script-catalog for custom scripts ([#2533](https://github.com/JanssenProject/jans/issues/2533)) ([5a0521e](https://github.com/JanssenProject/jans/commit/5a0521e78e701d16ce6a7ebead0ad40c3b2f638c))
* select first sig key if none requested ([#2494](https://github.com/JanssenProject/jans/issues/2494)) ([31fb464](https://github.com/JanssenProject/jans/commit/31fb464560563cf1463e94682d5939e531cabe81))
* update authn schemes in yaml descriptor [#2414](https://github.com/JanssenProject/jans/issues/2414) ([#2415](https://github.com/JanssenProject/jans/issues/2415)) ([4b239af](https://github.com/JanssenProject/jans/commit/4b239af72d8aa9e3a09ff6de19cf315b3863bda2))
* update chart repo ([8e347a3](https://github.com/JanssenProject/jans/commit/8e347a3c4321c1ba142c05be632630ffc8836cea))
* update chart repo ([011af9d](https://github.com/JanssenProject/jans/commit/011af9d4ce8cff51d18005d319f5552362c94f19))
* update error pages ([#1957](https://github.com/JanssenProject/jans/issues/1957)) ([3d63f4d](https://github.com/JanssenProject/jans/commit/3d63f4d3d58e86d499271ebafdbc0dd56f5c4e98))
* upgrade com.google.http-client:google-http-client-jackson2 from 1.26.0 to 1.40.1 ([#644](https://github.com/JanssenProject/jans/issues/644)) ([31bc823](https://github.com/JanssenProject/jans/commit/31bc823e1625e76b8f5d4b2b17357a62fde6e6a2))
* use iterator to correcly remove OC attribute ([#2138](https://github.com/JanssenProject/jans/issues/2138)) ([b590981](https://github.com/JanssenProject/jans/commit/b590981c53c26f4b1a8b6a0865ddc552e0a347b8))


### Miscellaneous Chores

* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))
* release 1.0.2-1 ([d01b51a](https://github.com/JanssenProject/jans/commit/d01b51a847bb2f67b52da433ebd1c5e4a66b7c1a))

## 1.0.2 (2022-08-30)


### Features

* add inum claim in profile scope [#2095](https://github.com/JanssenProject/jans/issues/2095) ([#2096](https://github.com/JanssenProject/jans/issues/2096)) ([f67c32e](https://github.com/JanssenProject/jans/commit/f67c32e7891f95c7a00ad0fa263444214dcaecd5))
* add new methods to allow get/set list of custom attributes from ([#2105](https://github.com/JanssenProject/jans/issues/2105)) ([5ac23a1](https://github.com/JanssenProject/jans/commit/5ac23a18adf3b34fd41fd1199ab168bfc9602fc6)), closes [#2104](https://github.com/JanssenProject/jans/issues/2104)
* add newly redesigned jans-client-api ([#1540](https://github.com/JanssenProject/jans/issues/1540)) ([4582ae5](https://github.com/JanssenProject/jans/commit/4582ae563ddf2492c519fdbc7685af2ce3c5529d))
* add support for date ranges in statistic client [#1575](https://github.com/JanssenProject/jans/issues/1575) ([#1653](https://github.com/JanssenProject/jans/issues/1653)) ([8048cd9](https://github.com/JanssenProject/jans/commit/8048cd9b6ab393b8a3e4a1aaf36e09abe20f605b))
* add support for requestUriBlockList config ([#1572](https://github.com/JanssenProject/jans/issues/1572)) ([63b3b74](https://github.com/JanssenProject/jans/commit/63b3b7418861e8f4234c54a8d81fe5d21f56387f))
* added config to disable attempt to update before insert in cache ([#1787](https://github.com/JanssenProject/jans/issues/1787)) ([d9a07ff](https://github.com/JanssenProject/jans/commit/d9a07ffb8dc1af290be6bc9b4978ad21c6797a3f))
* **agama:** add utility classes for inbound identity ([#2204](https://github.com/JanssenProject/jans/issues/2204)) ([29f58ee](https://github.com/JanssenProject/jans/commit/29f58ee0e6c84b4af5493cabcb19167bc7ffbe40))
* **agama:** add utility classes for inbound identity ([#2231](https://github.com/JanssenProject/jans/issues/2231)) ([96e32a4](https://github.com/JanssenProject/jans/commit/96e32a407ec6c545b73a6fd103ed2ae5876bd500))
* **agama:** allow the config-api to perform syntax check of flows ([#1621](https://github.com/JanssenProject/jans/issues/1621)) ([2e99d3a](https://github.com/JanssenProject/jans/commit/2e99d3a9bec389f68086c606062280967ce338ce))
* **agama:** reject usage of repeated input names ([#1484](https://github.com/JanssenProject/jans/issues/1484)) ([aed8cf3](https://github.com/JanssenProject/jans/commit/aed8cf33d89b98f0ac6aae52e145a84a0937d60e))
* disable TLS in CB client by default ([#2167](https://github.com/JanssenProject/jans/issues/2167)) ([8ec5dd3](https://github.com/JanssenProject/jans/commit/8ec5dd3dc9818a53949468389a1918ed385c28a9))
* **docker-jans-fido2:** allow creating initial persistence entry ([#2029](https://github.com/JanssenProject/jans/issues/2029)) ([41dfab7](https://github.com/JanssenProject/jans/commit/41dfab7a6d09d10b05954d4f5fae2437eb81a885))
* **docker-jans-scim:** allow creating initial persistence entry ([#2035](https://github.com/JanssenProject/jans/issues/2035)) ([e485618](https://github.com/JanssenProject/jans/commit/e4856186d566f9ac7b08395b59cd75e389e5c161))
* endpoint to get details of connected FIDO devices registered to users [#1465](https://github.com/JanssenProject/jans/issues/1465) ([#1466](https://github.com/JanssenProject/jans/issues/1466)) ([62522fe](https://github.com/JanssenProject/jans/commit/62522fe5aaa2971835c76e8e9b0d4280fee1db32))
* expose prometheus metrics via jmx exporter ([#1573](https://github.com/JanssenProject/jans/issues/1573)) ([205e320](https://github.com/JanssenProject/jans/commit/205e3206cf87bdb7cf0908bfdd7ee777d1ab955d))
* fix susrefire tests in filter module ([#2141](https://github.com/JanssenProject/jans/issues/2141)) ([118d77c](https://github.com/JanssenProject/jans/commit/118d77cd7025dcbe3031bc41450ec285afff4b9f))
* fix the dependencies and code issues ([#1473](https://github.com/JanssenProject/jans/issues/1473)) ([f4824c6](https://github.com/JanssenProject/jans/commit/f4824c6c6c6a036c5d01b7a6710f51477a49a3fb))
* introduce new hybrid persistence mapping ([#1505](https://github.com/JanssenProject/jans/issues/1505)) ([a77ab60](https://github.com/JanssenProject/jans/commit/a77ab602d15cb6bdf4751aaa11c2be9485b04a34))
* jans linux setup enable/disable script via arg ([#1634](https://github.com/JanssenProject/jans/issues/1634)) ([0b3cf16](https://github.com/JanssenProject/jans/commit/0b3cf16f524add8f27ca321ce2d82f6d61660456))
* jans linux setup openbanking CLI and certificate automation ([#1472](https://github.com/JanssenProject/jans/issues/1472)) ([62b5868](https://github.com/JanssenProject/jans/commit/62b5868e1e864a000be210d250602d43a2719b51))
* **jans-auth-server:** add support for ranges in statistic endpoint (UI team request) ([fd66720](https://github.com/JanssenProject/jans/commit/fd667203564951ba4fc450bf9fb77ba0e70a75ec))
* **jans-auth-server:** added allowSpontaneousScopes AS json config [#2074](https://github.com/JanssenProject/jans/issues/2074) ([#2111](https://github.com/JanssenProject/jans/issues/2111)) ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* **jans-auth-server:** added convenient method for up-scoping or down-scoping AT scopes [#1218](https://github.com/JanssenProject/jans/issues/1218) ([5d71655](https://github.com/JanssenProject/jans/commit/5d716553c6eb409c2f264864da8b65c0a0bcbe81))
* **jans-auth-server:** added creator info to scope (time/id/type) [#1934](https://github.com/JanssenProject/jans/issues/1934) ([#2023](https://github.com/JanssenProject/jans/issues/2023)) ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* **jans-auth-server:** added restriction for request_uri parameter (blocklist and allowed client.request_uri) [#1503](https://github.com/JanssenProject/jans/issues/1503) ([0696d92](https://github.com/JanssenProject/jans/commit/0696d92094eeb2ed36f6b0075680634acbf8992f))
* **jans-auth-server:** added sid and authn_time for active sessions response ([bf9b572](https://github.com/JanssenProject/jans/commit/bf9b572b835d37cc23b2c57437a3830a8ebf55f0))
* **jans-auth-server:** if applicationType is not set during client registration AS should default to 'web' [#1687](https://github.com/JanssenProject/jans/issues/1687) ([f9695e1](https://github.com/JanssenProject/jans/commit/f9695e1c11389c6f3c0614199b0774f491de8030))
* **jans-auth-server:** improve client assertion creation code (ClientAuthnRequest) [#1182](https://github.com/JanssenProject/jans/issues/1182) ([81946b2](https://github.com/JanssenProject/jans/commit/81946b22023e9eade94b9202adc6fb32b21652cf))
* **jans-auth-server:** improved TokenRestWebServiceValidator and added test for it [#1591](https://github.com/JanssenProject/jans/issues/1591) ([929048e](https://github.com/JanssenProject/jans/commit/929048eb41e3c79b25c7474c0a2596b013a3e91c))
* **jans-auth-server:** jwt "exp" must consider "keyRegenerationInterval" [#1233](https://github.com/JanssenProject/jans/issues/1233) ([023cf8a](https://github.com/JanssenProject/jans/commit/023cf8a1a1cf5ece4e0780fccd62b3acbefa768c))
* **jans-auth-server:** make check whether user is active case insensitive [#1550](https://github.com/JanssenProject/jans/issues/1550) ([d141837](https://github.com/JanssenProject/jans/commit/d14183708a04cdc6406167acc3126f253f212efa))
* **jans-auth-server:** persist org_id from software statement into client's "o" attribute ([021d3bd](https://github.com/JanssenProject/jans/commit/021d3bd17f8a9814e5a0d59b4f28b0c19da88ced))
* **jans-auth-server:** removed dcrSkipSignatureValidation configuration property [#1623](https://github.com/JanssenProject/jans/issues/1623) ([6550247](https://github.com/JanssenProject/jans/commit/6550247ca727d9437ffceec3fa12b9fef93b81e4))
* **jans-auth-server:** removed id_generation_endpoint and other claims from discovery response [#1827](https://github.com/JanssenProject/jans/issues/1827) ([4068197](https://github.com/JanssenProject/jans/commit/40681972a84d691b5d138bc603f32ec80de84fa2))
* **jans-auth-server:** split grant validation logic into TokenRestWebServiceValidator  [#1591](https://github.com/JanssenProject/jans/issues/1591) ([812e605](https://github.com/JanssenProject/jans/commit/812e605bf1c0d9041db008fba81515455ab38fab))
* **jans-auth-server:** split validation logic to TokenRestWebServiceValidator  [#1591](https://github.com/JanssenProject/jans/issues/1591) ([f9f6f49](https://github.com/JanssenProject/jans/commit/f9f6f49c8874cb0a1c71ff0bc0a75e244077feb9))
* **jans-auth-server:** updating arquillian tests 1247 ([#2017](https://github.com/JanssenProject/jans/issues/2017)) ([ee200a7](https://github.com/JanssenProject/jans/commit/ee200a7dce5d750f3c4a9d536aa8d92a89926711))
* jans-cli tabulate scim user list ([#1518](https://github.com/JanssenProject/jans/issues/1518)) ([d370978](https://github.com/JanssenProject/jans/commit/d370978fcdad6e6a4027af1bb2610de3513653ba))
* **jans-client-api:** migration to Weld/Resteasy and Jetty 11 - Issue 260 ([#1319](https://github.com/JanssenProject/jans/issues/1319)) ([420ffc3](https://github.com/JanssenProject/jans/commit/420ffc3329b91c52d5c9996d7c1e600d9b6fead2))
* **jans-client-api:** Use injectable operations and remove serviceprovider ([#1643](https://github.com/JanssenProject/jans/issues/1643)) ([982cab3](https://github.com/JanssenProject/jans/commit/982cab3bc3f499d3ec3fbefbd10cb87f58333998))
* **jans-config-api:** added new attributes ([#1940](https://github.com/JanssenProject/jans/issues/1940)) ([757b22f](https://github.com/JanssenProject/jans/commit/757b22fcc03c28d950eb98b4503d1915fa15b025))
* **jans-config-api:** agama configuration integration ([#1501](https://github.com/JanssenProject/jans/issues/1501)) ([e84575b](https://github.com/JanssenProject/jans/commit/e84575b018f1910860ca6fbf13f5418e8fa131f6))
* **jans-config-api:** agama flow endpoint ([#1898](https://github.com/JanssenProject/jans/issues/1898)) ([0e73306](https://github.com/JanssenProject/jans/commit/0e73306f7642a74a3ed2cf8a8687a1ea447aa7bd))
* **jans-config-api:** agama patch endpoint ([#2028](https://github.com/JanssenProject/jans/issues/2028)) ([0b96a95](https://github.com/JanssenProject/jans/commit/0b96a95399cac02fee614523ae5b552c99c1e254))
* **jans-config-api:** endpoint to get UmaResource based on clientId and swagger changes ([#1912](https://github.com/JanssenProject/jans/issues/1912)) ([a3f9145](https://github.com/JanssenProject/jans/commit/a3f91453dd8fa5e9c903827b458bc58e735eda55))
* **jans-config-api:** enhancement to agama and uma resource endpoint  ([#2015](https://github.com/JanssenProject/jans/issues/2015)) ([f2c19a1](https://github.com/JanssenProject/jans/commit/f2c19a14b0b5b869eb97afd24ac7169328f22b2f))
* **jans-config-api:** enhancement to expose user inum at root level of response ([#1477](https://github.com/JanssenProject/jans/issues/1477)) ([1e4b6bc](https://github.com/JanssenProject/jans/commit/1e4b6bc9955a0cd91d6dff000a860ca96b6bd822))
* **jans-config-api:** fetch the associated clients_id in GET scopes api response  ([#1946](https://github.com/JanssenProject/jans/issues/1946)) ([ffe743c](https://github.com/JanssenProject/jans/commit/ffe743ca007d3a1ca7011f47df9c0a4124c93e5c))
* **jans-config-api:** fixed user management swagger spec for mandatory fields ([#1519](https://github.com/JanssenProject/jans/issues/1519)) ([29ff812](https://github.com/JanssenProject/jans/commit/29ff812c7d6cb94e98886ea7cab0ab08a44879dd))
* **jans-config-api:** new endpoint to fetch scope by creator and type ([#2098](https://github.com/JanssenProject/jans/issues/2098)) ([cf15d67](https://github.com/JanssenProject/jans/commit/cf15d678f26de3bb2e645040ad25bcb21a03691f))
* **jans-config-api:** new functionality and swagger fix  ([#1802](https://github.com/JanssenProject/jans/issues/1802)) ([fc81d1d](https://github.com/JanssenProject/jans/commit/fc81d1d8a974350547bb6d1d22a8818140325f57))
* **jans-config-api:** Scope object changes for creator details ([#2033](https://github.com/JanssenProject/jans/issues/2033)) ([a8b8d76](https://github.com/JanssenProject/jans/commit/a8b8d76640ff6520a462ff2bb477db50c2b60207))
* **jans-config-api:** session management endpoint ([#2158](https://github.com/JanssenProject/jans/issues/2158)) ([30f6e1a](https://github.com/JanssenProject/jans/commit/30f6e1a4bacb90a711ed6f91bc124267d44b9d44))
* **jans-config-api:** swagger spec change to expose user inum at root level of response ([#1483](https://github.com/JanssenProject/jans/issues/1483)) ([c202705](https://github.com/JanssenProject/jans/commit/c202705f2585c4f8f8c9259ad41b388e97f97573))
* **jans-config-api:** user management endpoint 418 ([#1548](https://github.com/JanssenProject/jans/issues/1548)) ([b95fa7b](https://github.com/JanssenProject/jans/commit/b95fa7bcd56ef39f8478a9e879c493f815b29dd3))
* **jans-core:** added StandaloneJavaCustomScriptManagerTest ([48ba08b](https://github.com/JanssenProject/jans/commit/48ba08b2f336c2cef1f244d1411c71859fe337a4))
* jans-linux-setup add forgot password script ([#1587](https://github.com/JanssenProject/jans/issues/1587)) ([b2e3eb3](https://github.com/JanssenProject/jans/commit/b2e3eb3f07bfc877ee6aee9a3fdd187d7abbf52b))
* jans-linux-setup agama ([#1486](https://github.com/JanssenProject/jans/issues/1486)) ([6b23bfe](https://github.com/JanssenProject/jans/commit/6b23bfe19ef960039f76df4de167c159312dd930))
* jans-linux-setup debian 11 packages ([#1769](https://github.com/JanssenProject/jans/issues/1769)) ([6fbef91](https://github.com/JanssenProject/jans/commit/6fbef91dcb4e14aaa78a898d945ef2c2e38ca722))
* jans-linux-setup Script for Keystroke Authentication ([#1853](https://github.com/JanssenProject/jans/issues/1853)) ([11a9e04](https://github.com/JanssenProject/jans/commit/11a9e040923925d2a3009bfc208321c9ea7ad33c))
* **jans-linux-setup:** [#1731](https://github.com/JanssenProject/jans/issues/1731) ([#1732](https://github.com/JanssenProject/jans/issues/1732)) ([6fad15b](https://github.com/JanssenProject/jans/commit/6fad15b339c5e6b29055e3acf350f455c47ddc93))
* **jans-linux-setup:** added discoveryDenyKeys [#1827](https://github.com/JanssenProject/jans/issues/1827) ([f77a6da](https://github.com/JanssenProject/jans/commit/f77a6da20a4a699998cac7c5dc098d09519c2fe4))
* **jans-orm:** update Couchbase ORM to use SDK 3.x [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#1852](https://github.com/JanssenProject/jans/issues/1852)) ([d9d5157](https://github.com/JanssenProject/jans/commit/d9d5157c3421f4995ee4abd6918c106f9a78dd5f))
* **jans-scim:** make max no. of operations and payload size of bulks operations parameterizable ([#1872](https://github.com/JanssenProject/jans/issues/1872)) ([c27a45b](https://github.com/JanssenProject/jans/commit/c27a45bb0a19257c824c4e195f203e9b9b45ec88))
* need to fetch the associated clients_id in GET scopes api response [#1923](https://github.com/JanssenProject/jans/issues/1923) ([#1949](https://github.com/JanssenProject/jans/issues/1949)) ([88606a5](https://github.com/JanssenProject/jans/commit/88606a5ad01b9444f533ee4ea85ea0ca57dc49d8))
* proper plugin activation of config-api container ([#1627](https://github.com/JanssenProject/jans/issues/1627)) ([07cabb9](https://github.com/JanssenProject/jans/commit/07cabb9c310fb0b00afa419599b2e032c7cf1652))
* update Coucbase ORM to conform SDK 3.x (config updates) [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#2118](https://github.com/JanssenProject/jans/issues/2118)) ([fceec83](https://github.com/JanssenProject/jans/commit/fceec8332fb36826e5dccb797ee79b769859e126))
* update DSL to support shorthand for finish [#1628](https://github.com/JanssenProject/jans/issues/1628) ([71e4652](https://github.com/JanssenProject/jans/commit/71e46524492d48fccf2ed2840ede3d6ae525a3e3))


### Bug Fixes

* : start_date and end_date not required in /stat reponse (swagger specs) [#1767](https://github.com/JanssenProject/jans/issues/1767) ([#1768](https://github.com/JanssenProject/jans/issues/1768)) ([c21452a](https://github.com/JanssenProject/jans/commit/c21452a95567da2f7441e57268268b8d6cb65cfb))
* [#2143](https://github.com/JanssenProject/jans/issues/2143) ([#2144](https://github.com/JanssenProject/jans/issues/2144)) ([ff7f9f4](https://github.com/JanssenProject/jans/commit/ff7f9f4110d72b333aae0d2332b429dcbd067da3))
* [#2157](https://github.com/JanssenProject/jans/issues/2157) ([#2159](https://github.com/JanssenProject/jans/issues/2159)) ([dc8cb60](https://github.com/JanssenProject/jans/commit/dc8cb60990052256b46842f85ebf4961beee82dd))
* a workaround for fido2 dependency ([#1590](https://github.com/JanssenProject/jans/issues/1590)) ([527c928](https://github.com/JanssenProject/jans/commit/527c928d5769320a57d203d59175077e10c2d30a))
* add path parameter to /fido2/registration/entries [#1465](https://github.com/JanssenProject/jans/issues/1465) ([#1508](https://github.com/JanssenProject/jans/issues/1508)) ([808d0c4](https://github.com/JanssenProject/jans/commit/808d0c4a9b2701c9238926141e22662b918e5990))
* **agama:** template overriding not working with more than one level of nesting ([#1841](https://github.com/JanssenProject/jans/issues/1841)) ([723922a](https://github.com/JanssenProject/jans/commit/723922a17b1babc49a1135030c06db367726ab63))
* build from source ([#1793](https://github.com/JanssenProject/jans/issues/1793)) ([e389363](https://github.com/JanssenProject/jans/commit/e389363e3fdad7149cdd73ea6fcbc4058f38819a))
* **config-api:** fixing discrepancies in the api ([#2216](https://github.com/JanssenProject/jans/issues/2216)) ([af4d3a5](https://github.com/JanssenProject/jans/commit/af4d3a51ce2cbe8c531f8dca213d0c3ef087aad5))
* correct the link to image ([#1660](https://github.com/JanssenProject/jans/issues/1660)) ([0943d81](https://github.com/JanssenProject/jans/commit/0943d813f782a3babaa5166f426533fd561419a5))
* docker-jans-persistence-loader/Dockerfile to reduce vulnerabilities ([#1829](https://github.com/JanssenProject/jans/issues/1829)) ([8e4ae15](https://github.com/JanssenProject/jans/commit/8e4ae15de9c93414e3e6e03539cfebc45e71e03e))
* don't execute next paged search if current result count less than ([#2171](https://github.com/JanssenProject/jans/issues/2171)) ([94a162f](https://github.com/JanssenProject/jans/commit/94a162f4471ec6e4798721894b4a5a583ad71370))
* fido2-plugin throwing error during deployment [#1632](https://github.com/JanssenProject/jans/issues/1632) ([#1633](https://github.com/JanssenProject/jans/issues/1633)) ([90d2c8a](https://github.com/JanssenProject/jans/commit/90d2c8ace819b784a293df698e316c13a8548fd1))
* fix typos and other issues in jans-config-api swagger specs [#1665](https://github.com/JanssenProject/jans/issues/1665) ([#1668](https://github.com/JanssenProject/jans/issues/1668)) ([3c3a0f4](https://github.com/JanssenProject/jans/commit/3c3a0f47f6274c8b106bebabc38df927a4238ac3))
* **images:** conform to new couchbase persistence configuration ([#2188](https://github.com/JanssenProject/jans/issues/2188)) ([c708542](https://github.com/JanssenProject/jans/commit/c7085427fd298f74e8809ef4d6c39f780fa83776))
* include idtoken with dynamic scopes for ciba ([#2108](https://github.com/JanssenProject/jans/issues/2108)) ([d9b5341](https://github.com/JanssenProject/jans/commit/d9b5341d50de972c910883c12785ce6d2758588f))
* indentation ([#1821](https://github.com/JanssenProject/jans/issues/1821)) ([8353092](https://github.com/JanssenProject/jans/commit/83530920d920a6fd71bfb65545816af2e7f8511d))
* jans app and java version ([#1492](https://github.com/JanssenProject/jans/issues/1492)) ([1257e49](https://github.com/JanssenProject/jans/commit/1257e4923eee28e20018720c8815cd518c28bd2f))
* Jans cli user userpassword ([#1542](https://github.com/JanssenProject/jans/issues/1542)) ([d2e13a2](https://github.com/JanssenProject/jans/commit/d2e13a2bcb16a14f63f3bd99c4b3dc83d2a96d9a))
* **jans-auth-server:** client tests expects "scope to claim" mapping which are disabled by default [#1873](https://github.com/JanssenProject/jans/issues/1873) ([958cc92](https://github.com/JanssenProject/jans/commit/958cc9232fafa618cb326c7251486f0add7a15c1))
* **jans-auth-server:** corrected npe in JwtAuthorizationRequest ([9c9e7bf](https://github.com/JanssenProject/jans/commit/9c9e7bf6442637e9f98e9b7765eb373714130d1d))
* **jans-auth-server:** disable surefire for jans-auth-static ([7869efa](https://github.com/JanssenProject/jans/commit/7869efabd5bc4b32fd8bf8347093fa87ab774957))
* **jans-auth-server:** fix missing jsonobject annotation ([#1651](https://github.com/JanssenProject/jans/issues/1651)) ([be5b82a](https://github.com/JanssenProject/jans/commit/be5b82a3ccbc7a0fe9f4ebbb97fa8054657227dc))
* **jans-auth-server:** fixed NPE during getting AT lifetime [#1233](https://github.com/JanssenProject/jans/issues/1233) ([f8be086](https://github.com/JanssenProject/jans/commit/f8be08658c1478acd59fbbfcd609d78179cb00e9))
* **jans-auth-server:** fixing client tests effected by "scope to claim" mapping which is disabled by default [#1873](https://github.com/JanssenProject/jans/issues/1873) ([#1910](https://github.com/JanssenProject/jans/issues/1910)) ([6d81792](https://github.com/JanssenProject/jans/commit/6d81792a141ca725004c23f1bdd0a42314ffcb5f))
* **jans-auth-server:** generate description during built-in key rotation [#1790](https://github.com/JanssenProject/jans/issues/1790) ([#2068](https://github.com/JanssenProject/jans/issues/2068)) ([cd1a77d](https://github.com/JanssenProject/jans/commit/cd1a77dd36a59b19e975c013c8081610a23106ba))
* **jans-auth-server:** increased period of session authn time check ([#1918](https://github.com/JanssenProject/jans/issues/1918)) ([a41905a](https://github.com/JanssenProject/jans/commit/a41905abba38c051acc7e7d57131da4b7c3a1616))
* **jans-auth-server:** sql localizedstring persistence SqlEntryManager ([#1475](https://github.com/JanssenProject/jans/issues/1475)) ([b959b94](https://github.com/JanssenProject/jans/commit/b959b94e235c8bb554fcbdc8abbc22e3df540dbe))
* jans-cli download yaml files for build ([#1635](https://github.com/JanssenProject/jans/issues/1635)) ([31b7e49](https://github.com/JanssenProject/jans/commit/31b7e49043d86c9b266590f6437146d625412f60))
* jans-cli help message format and prompt values (ref: [#1352](https://github.com/JanssenProject/jans/issues/1352)) ([#1478](https://github.com/JanssenProject/jans/issues/1478)) ([37a9181](https://github.com/JanssenProject/jans/commit/37a91819bb7764d2dded27d6b5eafe25de083fe9))
* jans-cli hide menu item ([#1510](https://github.com/JanssenProject/jans/issues/1510)) ([b70fc52](https://github.com/JanssenProject/jans/commit/b70fc52073a3110c767fbc239bb10cc7924838e8))
* jans-cli user list failing for empty customAttributes ([#1525](https://github.com/JanssenProject/jans/issues/1525)) ([7cbf10b](https://github.com/JanssenProject/jans/commit/7cbf10b85187c554bf84bc0ceea6bfcf66cb0088))
* **jans-client-api:** minor observations PR13119 - typo transalation code-improvement ([#1806](https://github.com/JanssenProject/jans/issues/1806)) ([6df2e42](https://github.com/JanssenProject/jans/commit/6df2e422879d8726f2b1d6574fe5492355317bf9))
* **jans-client-api:** remove jans-config-api dependency and solve wrong  test dependencies ([#1737](https://github.com/JanssenProject/jans/issues/1737)) ([97dbe9c](https://github.com/JanssenProject/jans/commit/97dbe9cc3072ca17e9f092cc6d3df5a510778ac2))
* **jans-client-api:** upgrade seleniumhq version from 3.x to 4.x ([#2110](https://github.com/JanssenProject/jans/issues/2110)) ([d48271e](https://github.com/JanssenProject/jans/commit/d48271e872de72c7085e592988ad2e4e8950116d))
* jans-config-api add JAVA to programmingLanguage (ref: [#1656](https://github.com/JanssenProject/jans/issues/1656)) ([#1667](https://github.com/JanssenProject/jans/issues/1667)) ([a885a92](https://github.com/JanssenProject/jans/commit/a885a925cdd711158435fedd643f1dd67afad736))
* **jans-config-api:** avoid loss of attributes in agama endpoints ([#2058](https://github.com/JanssenProject/jans/issues/2058)) ([3c8f816](https://github.com/JanssenProject/jans/commit/3c8f816b62b0efdfffc0e3f53d8371f4510d3ef6))
* **jans-config-api:** config-api compilation failed in main [#2030](https://github.com/JanssenProject/jans/issues/2030) ([#2031](https://github.com/JanssenProject/jans/issues/2031)) ([1659da1](https://github.com/JanssenProject/jans/commit/1659da1ff4d1d930300ef9c3b3e040eabc7bc0fb))
* **jans-config-api:** Fix to not update Metadata for PUT and PATCH agama endpoint ([#2046](https://github.com/JanssenProject/jans/issues/2046)) ([da93050](https://github.com/JanssenProject/jans/commit/da93050442d3bc1812d3a8076686ca3e02800c26))
* **jans-config-api:** fixed due to couchbase cluster changes([#1863](https://github.com/JanssenProject/jans/issues/1863)) ([c996b51](https://github.com/JanssenProject/jans/commit/c996b516cb3f0c4880c4bc78038a5eba666a62c6))
* **jans-config-api:** fixes for path conflict for SCIM config and spec for UMA Resource mandatory fields ([#1805](https://github.com/JanssenProject/jans/issues/1805)) ([6d8cff6](https://github.com/JanssenProject/jans/commit/6d8cff64d74634e16e100193f34b06990c356d1c))
* **jans-config-api:** issue UMA scope request being saved as OAUTH ([#2063](https://github.com/JanssenProject/jans/issues/2063)) ([81472aa](https://github.com/JanssenProject/jans/commit/81472aa3da4b02af7ed1bd47753d6938ec0c3e01))
* **jans-config-api:** rectified endpoint url in swagger spec for uma resource ([#1965](https://github.com/JanssenProject/jans/issues/1965)) ([0dc3b2e](https://github.com/JanssenProject/jans/commit/0dc3b2e60825f9921f28c9eeff30ffefa8bda269))
* **jans-config-api:** removed java_script from programmingLanguages ([8b935d8](https://github.com/JanssenProject/jans/commit/8b935d8249ab97f912993a07be0a093b89e52c8b))
* **jans-config-api:** swagger spec change to add missing attributes for Client ([#1786](https://github.com/JanssenProject/jans/issues/1786)) ([e623771](https://github.com/JanssenProject/jans/commit/e62377194616a8a39c86689b30cab92235124fc3))
* **jans-config-api:** switch to 1.0.1-SNAPSHOT ([e8a9186](https://github.com/JanssenProject/jans/commit/e8a918647da488038ff593da875614b6d7c60cc2))
* **jans-core:** removed redundant reference [#1927](https://github.com/JanssenProject/jans/issues/1927) ([#1928](https://github.com/JanssenProject/jans/issues/1928)) ([064cbb8](https://github.com/JanssenProject/jans/commit/064cbb8040f4d7b21ff13e5f48c7f923c38f67b1))
* **jans-core:** switch to 1.0.1-SNAPSHOT ([dbe9355](https://github.com/JanssenProject/jans/commit/dbe9355d97618a267df1ab7aa5c0780e125a3420))
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
* **jans-orm:** fixed npe in filter processor and covered with tests ([ef46516](https://github.com/JanssenProject/jans/commit/ef4651677c415b92d8db01f6bf67eda4d9b9dd03))
* **jans-orm:** switch to 1.0.1-SNAPSHOT ([0030da7](https://github.com/JanssenProject/jans/commit/0030da76d16eedbdcfc74b72b99705a9fb63cb27))
* **login.xhtml:** add google client js ([#1666](https://github.com/JanssenProject/jans/issues/1666)) ([daf9849](https://github.com/JanssenProject/jans/commit/daf9849da1f92707b05517f73bfede1a69103365))
* main docker version ([1220e1c](https://github.com/JanssenProject/jans/commit/1220e1c892c4462b74039bcf64f0cd0cedb80d1f))
* **pycloudlib:** handle type mismatch for iterable ([#2004](https://github.com/JanssenProject/jans/issues/2004)) ([46e0b2e](https://github.com/JanssenProject/jans/commit/46e0b2e4aff70a97cdcdcd0102dc83d294e45fdc))
* random password for keystores ([#2102](https://github.com/JanssenProject/jans/issues/2102)) ([b7d9af1](https://github.com/JanssenProject/jans/commit/b7d9af12ecf946498d279a5f577db0528e5522bc))
* test data for login ([#1757](https://github.com/JanssenProject/jans/issues/1757)) ([e043949](https://github.com/JanssenProject/jans/commit/e0439497d08d09080d7ebd161cd24bcdadfee10f))
* update chart repo ([8e347a3](https://github.com/JanssenProject/jans/commit/8e347a3c4321c1ba142c05be632630ffc8836cea))
* update chart repo ([011af9d](https://github.com/JanssenProject/jans/commit/011af9d4ce8cff51d18005d319f5552362c94f19))
* update error pages ([#1957](https://github.com/JanssenProject/jans/issues/1957)) ([3d63f4d](https://github.com/JanssenProject/jans/commit/3d63f4d3d58e86d499271ebafdbc0dd56f5c4e98))
* update external modules for otp/fido2 ([#1589](https://github.com/JanssenProject/jans/issues/1589)) ([fc42181](https://github.com/JanssenProject/jans/commit/fc4218110e5130878836a663aba72e67dcefcd10))
* use iterator to correcly remove OC attribute ([#2138](https://github.com/JanssenProject/jans/issues/2138)) ([b590981](https://github.com/JanssenProject/jans/commit/b590981c53c26f4b1a8b6a0865ddc552e0a347b8))


### Miscellaneous Chores

* prepare docker images release 1.0.1-1 ([12660a8](https://github.com/JanssenProject/jans/commit/12660a800bacb210bd3fb4b35c9156e9c5445343))
* prepare helm chart release 1.0.1 ([ae78b76](https://github.com/JanssenProject/jans/commit/ae78b760aa536ecde3b7e7972070e144d6c3c072))
* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))
* release 1.0.2-1 ([d01b51a](https://github.com/JanssenProject/jans/commit/d01b51a847bb2f67b52da433ebd1c5e4a66b7c1a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/v1.0.0...v1.0.1) (2022-07-06)


### Features

* add newly redesigned jans-client-api ([#1540](https://github.com/JanssenProject/jans/issues/1540)) ([4582ae5](https://github.com/JanssenProject/jans/commit/4582ae563ddf2492c519fdbc7685af2ce3c5529d))
* add support for date ranges in statistic client [#1575](https://github.com/JanssenProject/jans/issues/1575) ([#1653](https://github.com/JanssenProject/jans/issues/1653)) ([8048cd9](https://github.com/JanssenProject/jans/commit/8048cd9b6ab393b8a3e4a1aaf36e09abe20f605b))
* add support for requestUriBlockList config ([#1572](https://github.com/JanssenProject/jans/issues/1572)) ([63b3b74](https://github.com/JanssenProject/jans/commit/63b3b7418861e8f4234c54a8d81fe5d21f56387f))
* **agama:** allow the config-api to perform syntax check of flows ([#1621](https://github.com/JanssenProject/jans/issues/1621)) ([2e99d3a](https://github.com/JanssenProject/jans/commit/2e99d3a9bec389f68086c606062280967ce338ce))
* **agama:** improve flows timeout ([#1447](https://github.com/JanssenProject/jans/issues/1447)) ([ccfb62e](https://github.com/JanssenProject/jans/commit/ccfb62ec13d371c96a0d597d5a0229864f044373))
* **agama:** reject usage of repeated input names ([#1484](https://github.com/JanssenProject/jans/issues/1484)) ([aed8cf3](https://github.com/JanssenProject/jans/commit/aed8cf33d89b98f0ac6aae52e145a84a0937d60e))
* endpoint to get details of connected FIDO devices registered to users [#1465](https://github.com/JanssenProject/jans/issues/1465) ([#1466](https://github.com/JanssenProject/jans/issues/1466)) ([62522fe](https://github.com/JanssenProject/jans/commit/62522fe5aaa2971835c76e8e9b0d4280fee1db32))
* enhance error handling [#1434](https://github.com/JanssenProject/jans/issues/1434) ([a3f6314](https://github.com/JanssenProject/jans/commit/a3f631421f125501f9c7cf0be207744860d856cc))
* expose prometheus metrics via jmx exporter ([#1573](https://github.com/JanssenProject/jans/issues/1573)) ([205e320](https://github.com/JanssenProject/jans/commit/205e3206cf87bdb7cf0908bfdd7ee777d1ab955d))
* fix the dependencies and code issues ([#1473](https://github.com/JanssenProject/jans/issues/1473)) ([f4824c6](https://github.com/JanssenProject/jans/commit/f4824c6c6c6a036c5d01b7a6710f51477a49a3fb))
* introduce new hybrid persistence mapping ([#1505](https://github.com/JanssenProject/jans/issues/1505)) ([a77ab60](https://github.com/JanssenProject/jans/commit/a77ab602d15cb6bdf4751aaa11c2be9485b04a34))
* jans linux setup enable/disable script via arg ([#1634](https://github.com/JanssenProject/jans/issues/1634)) ([0b3cf16](https://github.com/JanssenProject/jans/commit/0b3cf16f524add8f27ca321ce2d82f6d61660456))
* jans linux setup openbanking CLI and certificate automation ([#1472](https://github.com/JanssenProject/jans/issues/1472)) ([62b5868](https://github.com/JanssenProject/jans/commit/62b5868e1e864a000be210d250602d43a2719b51))
* **jans-auth-server:** add support for ranges in statistic endpoint (UI team request) ([fd66720](https://github.com/JanssenProject/jans/commit/fd667203564951ba4fc450bf9fb77ba0e70a75ec))
* **jans-auth-server:** added convenient method for up-scoping or down-scoping AT scopes [#1218](https://github.com/JanssenProject/jans/issues/1218) ([5d71655](https://github.com/JanssenProject/jans/commit/5d716553c6eb409c2f264864da8b65c0a0bcbe81))
* **jans-auth-server:** added restriction for request_uri parameter (blocklist and allowed client.request_uri) [#1503](https://github.com/JanssenProject/jans/issues/1503) ([0696d92](https://github.com/JanssenProject/jans/commit/0696d92094eeb2ed36f6b0075680634acbf8992f))
* **jans-auth-server:** added sid and authn_time for active sessions response ([bf9b572](https://github.com/JanssenProject/jans/commit/bf9b572b835d37cc23b2c57437a3830a8ebf55f0))
* **jans-auth-server:** improve client assertion creation code (ClientAuthnRequest) [#1182](https://github.com/JanssenProject/jans/issues/1182) ([81946b2](https://github.com/JanssenProject/jans/commit/81946b22023e9eade94b9202adc6fb32b21652cf))
* **jans-auth-server:** make check whether user is active case insensitive [#1550](https://github.com/JanssenProject/jans/issues/1550) ([d141837](https://github.com/JanssenProject/jans/commit/d14183708a04cdc6406167acc3126f253f212efa))
* **jans-auth-server:** persist org_id from software statement into client's "o" attribute ([021d3bd](https://github.com/JanssenProject/jans/commit/021d3bd17f8a9814e5a0d59b4f28b0c19da88ced))
* **jans-auth-server:** removed dcrSkipSignatureValidation configuration property [#1623](https://github.com/JanssenProject/jans/issues/1623) ([6550247](https://github.com/JanssenProject/jans/commit/6550247ca727d9437ffceec3fa12b9fef93b81e4))
* jans-cli --no-suggestion for automated testing ([#1437](https://github.com/JanssenProject/jans/issues/1437)) ([187cc07](https://github.com/JanssenProject/jans/commit/187cc0742102e3d1a708c6b9fffce32d1ac1ebd2))
* jans-cli tabulate scim user list ([#1518](https://github.com/JanssenProject/jans/issues/1518)) ([d370978](https://github.com/JanssenProject/jans/commit/d370978fcdad6e6a4027af1bb2610de3513653ba))
* **jans-client-api:** migration to Weld/Resteasy and Jetty 11 - Issue 260 ([#1319](https://github.com/JanssenProject/jans/issues/1319)) ([420ffc3](https://github.com/JanssenProject/jans/commit/420ffc3329b91c52d5c9996d7c1e600d9b6fead2))
* **jans-config-api:** agama configuration integration ([#1501](https://github.com/JanssenProject/jans/issues/1501)) ([e84575b](https://github.com/JanssenProject/jans/commit/e84575b018f1910860ca6fbf13f5418e8fa131f6))
* **jans-config-api:** enhancement to expose user inum at root level of response ([#1477](https://github.com/JanssenProject/jans/issues/1477)) ([1e4b6bc](https://github.com/JanssenProject/jans/commit/1e4b6bc9955a0cd91d6dff000a860ca96b6bd822))
* **jans-config-api:** fixed user management swagger spec for mandatory fields ([#1519](https://github.com/JanssenProject/jans/issues/1519)) ([29ff812](https://github.com/JanssenProject/jans/commit/29ff812c7d6cb94e98886ea7cab0ab08a44879dd))
* **jans-config-api:** swagger spec change to expose user inum at root level of response ([#1483](https://github.com/JanssenProject/jans/issues/1483)) ([c202705](https://github.com/JanssenProject/jans/commit/c202705f2585c4f8f8c9259ad41b388e97f97573))
* **jans-config-api:** user management endpoint 418 ([#1548](https://github.com/JanssenProject/jans/issues/1548)) ([b95fa7b](https://github.com/JanssenProject/jans/commit/b95fa7bcd56ef39f8478a9e879c493f815b29dd3))
* **jans-core:** added Discovery.java script and sample external service ([440f2dd](https://github.com/JanssenProject/jans/commit/440f2dd41a0dafc915fd409b21da454f8cf1e046))
* **jans-core:** added StandaloneJavaCustomScriptManagerTest ([48ba08b](https://github.com/JanssenProject/jans/commit/48ba08b2f336c2cef1f244d1411c71859fe337a4))
* **jans-core:** added test dependencies to scripts ([53e5f67](https://github.com/JanssenProject/jans/commit/53e5f6725648521a983a86a533f62587b902f951))
* jans-linux-setup add forgot password script ([#1587](https://github.com/JanssenProject/jans/issues/1587)) ([b2e3eb3](https://github.com/JanssenProject/jans/commit/b2e3eb3f07bfc877ee6aee9a3fdd187d7abbf52b))
* jans-linux-setup agama ([#1486](https://github.com/JanssenProject/jans/issues/1486)) ([6b23bfe](https://github.com/JanssenProject/jans/commit/6b23bfe19ef960039f76df4de167c159312dd930))
* proper plugin activation of config-api container ([#1627](https://github.com/JanssenProject/jans/issues/1627)) ([07cabb9](https://github.com/JanssenProject/jans/commit/07cabb9c310fb0b00afa419599b2e032c7cf1652))
* update DSL to support shorthand for finish [#1628](https://github.com/JanssenProject/jans/issues/1628) ([71e4652](https://github.com/JanssenProject/jans/commit/71e46524492d48fccf2ed2840ede3d6ae525a3e3))


### Bug Fixes

* a workaround for fido2 dependency ([#1590](https://github.com/JanssenProject/jans/issues/1590)) ([527c928](https://github.com/JanssenProject/jans/commit/527c928d5769320a57d203d59175077e10c2d30a))
* add path parameter to /fido2/registration/entries [#1465](https://github.com/JanssenProject/jans/issues/1465) ([#1508](https://github.com/JanssenProject/jans/issues/1508)) ([808d0c4](https://github.com/JanssenProject/jans/commit/808d0c4a9b2701c9238926141e22662b918e5990))
* correct the link to image ([#1660](https://github.com/JanssenProject/jans/issues/1660)) ([0943d81](https://github.com/JanssenProject/jans/commit/0943d813f782a3babaa5166f426533fd561419a5))
* fido2-plugin throwing error during deployment [#1632](https://github.com/JanssenProject/jans/issues/1632) ([#1633](https://github.com/JanssenProject/jans/issues/1633)) ([90d2c8a](https://github.com/JanssenProject/jans/commit/90d2c8ace819b784a293df698e316c13a8548fd1))
* fix typos and other issues in jans-config-api swagger specs [#1665](https://github.com/JanssenProject/jans/issues/1665) ([#1668](https://github.com/JanssenProject/jans/issues/1668)) ([3c3a0f4](https://github.com/JanssenProject/jans/commit/3c3a0f47f6274c8b106bebabc38df927a4238ac3))
* jans app and java version ([#1492](https://github.com/JanssenProject/jans/issues/1492)) ([1257e49](https://github.com/JanssenProject/jans/commit/1257e4923eee28e20018720c8815cd518c28bd2f))
* Jans cli user userpassword ([#1542](https://github.com/JanssenProject/jans/issues/1542)) ([d2e13a2](https://github.com/JanssenProject/jans/commit/d2e13a2bcb16a14f63f3bd99c4b3dc83d2a96d9a))
* **jans-auth-server:** added SessionRestWebService to rest initializer ([f0ebf67](https://github.com/JanssenProject/jans/commit/f0ebf67703d52d35c2788b1f528a9f7081dcab6a))
* **jans-auth-server:** corrected npe in JwtAuthorizationRequest ([9c9e7bf](https://github.com/JanssenProject/jans/commit/9c9e7bf6442637e9f98e9b7765eb373714130d1d))
* **jans-auth-server:** disable surefire for jans-auth-static ([7869efa](https://github.com/JanssenProject/jans/commit/7869efabd5bc4b32fd8bf8347093fa87ab774957))
* **jans-auth-server:** fix missing jsonobject annotation ([#1651](https://github.com/JanssenProject/jans/issues/1651)) ([be5b82a](https://github.com/JanssenProject/jans/commit/be5b82a3ccbc7a0fe9f4ebbb97fa8054657227dc))
* **jans-auth-server:** sql localizedstring persistence SqlEntryManager ([#1475](https://github.com/JanssenProject/jans/issues/1475)) ([b959b94](https://github.com/JanssenProject/jans/commit/b959b94e235c8bb554fcbdc8abbc22e3df540dbe))
* jans-cli download yaml files for build ([#1635](https://github.com/JanssenProject/jans/issues/1635)) ([31b7e49](https://github.com/JanssenProject/jans/commit/31b7e49043d86c9b266590f6437146d625412f60))
* jans-cli help message format and prompt values (ref: [#1352](https://github.com/JanssenProject/jans/issues/1352)) ([#1478](https://github.com/JanssenProject/jans/issues/1478)) ([37a9181](https://github.com/JanssenProject/jans/commit/37a91819bb7764d2dded27d6b5eafe25de083fe9))
* jans-cli hide menu item ([#1510](https://github.com/JanssenProject/jans/issues/1510)) ([b70fc52](https://github.com/JanssenProject/jans/commit/b70fc52073a3110c767fbc239bb10cc7924838e8))
* jans-cli user list failing for empty customAttributes ([#1525](https://github.com/JanssenProject/jans/issues/1525)) ([7cbf10b](https://github.com/JanssenProject/jans/commit/7cbf10b85187c554bf84bc0ceea6bfcf66cb0088))
* jans-config-api add JAVA to programmingLanguage (ref: [#1656](https://github.com/JanssenProject/jans/issues/1656)) ([#1667](https://github.com/JanssenProject/jans/issues/1667)) ([a885a92](https://github.com/JanssenProject/jans/commit/a885a925cdd711158435fedd643f1dd67afad736))
* **jans-config-api:** removed java_script from programmingLanguages ([8b935d8](https://github.com/JanssenProject/jans/commit/8b935d8249ab97f912993a07be0a093b89e52c8b))
* **jans-config-api:** switch to 1.0.1-SNAPSHOT ([e8a9186](https://github.com/JanssenProject/jans/commit/e8a918647da488038ff593da875614b6d7c60cc2))
* **jans-core:** switch to 1.0.1-SNAPSHOT ([dbe9355](https://github.com/JanssenProject/jans/commit/dbe9355d97618a267df1ab7aa5c0780e125a3420))
* jans-linux-setup add gcs module path for downloading apps ([#1538](https://github.com/JanssenProject/jans/issues/1538)) ([e540738](https://github.com/JanssenProject/jans/commit/e540738e2d0e6816562b4c927c0ce4bfbaafea56))
* jans-linux-setup add gcs path after packages check (ref: [#1514](https://github.com/JanssenProject/jans/issues/1514)) ([#1516](https://github.com/JanssenProject/jans/issues/1516)) ([31dd609](https://github.com/JanssenProject/jans/commit/31dd609ebe3fb36213cbbafa1db74c6fc50e01a2))
* jans-linux-setup disable script Forgot_Password_2FA_Token ([#1662](https://github.com/JanssenProject/jans/issues/1662)) ([377affc](https://github.com/JanssenProject/jans/commit/377affc238bca236324dd8eeb9d9e6750879560f))
* jans-linux-setup displayName of forgot-password script ([#1595](https://github.com/JanssenProject/jans/issues/1595)) ([07a5ea0](https://github.com/JanssenProject/jans/commit/07a5ea017c8d120b28e2bc578045160e4d3ff0ba))
* jans-linux-setup download jans-auth for --download-exit ([#1659](https://github.com/JanssenProject/jans/issues/1659)) ([879ed87](https://github.com/JanssenProject/jans/commit/879ed87035265f6bb714ba6283fb274fcdb2fca4))
* jans-linux-setup enable forgot-password script ([#1597](https://github.com/JanssenProject/jans/issues/1597)) ([149d19c](https://github.com/JanssenProject/jans/commit/149d19cc358b30c4cdd9d1383ace04d911402886))
* jans-linux-setup multiple argument --import-ldif ([#1476](https://github.com/JanssenProject/jans/issues/1476)) ([5556f36](https://github.com/JanssenProject/jans/commit/5556f36073fab29f8d379fe763326c736f5186da))
* jans-linux-setup python executable when launching setup ([#1683](https://github.com/JanssenProject/jans/issues/1683)) ([87ac58c](https://github.com/JanssenProject/jans/commit/87ac58ca72fdeaafc230183ebe0375537d1c24be))
* jans-linux-setup remove temporary link file ([#1495](https://github.com/JanssenProject/jans/issues/1495)) ([673859a](https://github.com/JanssenProject/jans/commit/673859a864023e7f2a0ba4a7c36d6fa4a164faaa))
* **jans-orm:** switch to 1.0.1-SNAPSHOT ([0030da7](https://github.com/JanssenProject/jans/commit/0030da76d16eedbdcfc74b72b99705a9fb63cb27))
* main docker version ([1220e1c](https://github.com/JanssenProject/jans/commit/1220e1c892c4462b74039bcf64f0cd0cedb80d1f))
* remove jans-auth-common dependency [#1459](https://github.com/JanssenProject/jans/issues/1459) ([75f4fb5](https://github.com/JanssenProject/jans/commit/75f4fb5487b8adc6300c939ea9a0a3302b235d0e))
* update external modules for otp/fido2 ([#1589](https://github.com/JanssenProject/jans/issues/1589)) ([fc42181](https://github.com/JanssenProject/jans/commit/fc4218110e5130878836a663aba72e67dcefcd10))
* update pom [#1438](https://github.com/JanssenProject/jans/issues/1438) ([#1439](https://github.com/JanssenProject/jans/issues/1439)) ([66b9962](https://github.com/JanssenProject/jans/commit/66b996286a2285986845677ea039f177f756d962))


### Miscellaneous Chores

* prepare docker images release 1.0.1-1 ([12660a8](https://github.com/JanssenProject/jans/commit/12660a800bacb210bd3fb4b35c9156e9c5445343))
* prepare helm chart release 1.0.1 ([ae78b76](https://github.com/JanssenProject/jans/commit/ae78b760aa536ecde3b7e7972070e144d6c3c072))
* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

## 1.0.0 (2022-05-20)


### Features

* add helper to create persistence entry from LDIF file ([#1262](https://github.com/JanssenProject/jans/issues/1262)) ([f2e653e](https://github.com/JanssenProject/jans/commit/f2e653ef917efd017195f2330b64e64c333f4699))
* add schema updates [#1390](https://github.com/JanssenProject/jans/issues/1390) ([c9023b3](https://github.com/JanssenProject/jans/commit/c9023b3435fbc8079aabe5c70de3177ec9112308))
* add script for Google login ([#1141](https://github.com/JanssenProject/jans/issues/1141)) ([bac9144](https://github.com/JanssenProject/jans/commit/bac9144ad8a5f8f2b378aa67663caab9f19f052b))
* add super-jans project ([1ac74d0](https://github.com/JanssenProject/jans/commit/1ac74d05a7f78bf156e7735d157559b84fad3974))
* adding logs to debug license issues[#1258](https://github.com/JanssenProject/jans/issues/1258) ([#1281](https://github.com/JanssenProject/jans/issues/1281)) ([8a08771](https://github.com/JanssenProject/jans/commit/8a08771014e3394d4d7b0864d603a1e4b91e2d81))
* adjust ownership and permission to avoid bloated images ([#1312](https://github.com/JanssenProject/jans/issues/1312)) ([d016682](https://github.com/JanssenProject/jans/commit/d0166821baf52665934c0eaa38de8b2f51825456))
* allow flows to access their metadata properties [#1340](https://github.com/JanssenProject/jans/issues/1340) ([344ba04](https://github.com/JanssenProject/jans/commit/344ba0448c73e3c56e05f529eea5009b2157c1fc))
* call id-generation script when creating user/group [#1145](https://github.com/JanssenProject/jans/issues/1145) ([3a9a03a](https://github.com/JanssenProject/jans/commit/3a9a03a101536f6616aa6b94d841bd1457815b31))
* config-cli enumerate scope type ([275533b](https://github.com/JanssenProject/jans/commit/275533b26f4715113d83ea9cabe4a66cd283a189))
* create apis to verify and save license api-keys in Admin UI [#1196](https://github.com/JanssenProject/jans/issues/1196) ([#1203](https://github.com/JanssenProject/jans/issues/1203)) ([315faec](https://github.com/JanssenProject/jans/commit/315faeca46045716d8aa38fa5448c7581a5e4212))
* initial agama commit [#1322](https://github.com/JanssenProject/jans/issues/1322) ([#1323](https://github.com/JanssenProject/jans/issues/1323)) ([0148bc8](https://github.com/JanssenProject/jans/commit/0148bc8a32a005e47ba9d090e895282775148a95))
* Jans linux setup refactor ([#1328](https://github.com/JanssenProject/jans/issues/1328)) ([79d3a75](https://github.com/JanssenProject/jans/commit/79d3a756bf0477907e4364c9887a316d4730c07a))
* Jans linux setup ubuntu22 Installation ([#1325](https://github.com/JanssenProject/jans/issues/1325)) ([8597750](https://github.com/JanssenProject/jans/commit/85977502e307884423b4b248694cf74b9b66b96a))
* **jans-auth-config:** user mgmt endpoint - wip ([9c8094a](https://github.com/JanssenProject/jans/commit/9c8094aaed4802d399da812898e1270fe0a0cae5))
* **jans-auth-server:** [#808](https://github.com/JanssenProject/jans/issues/808) sign-in with apple interception script ([c21183a](https://github.com/JanssenProject/jans/commit/c21183ab6331f95531d76c6d279646cc3c0b600e))
* **jans-auth-server:** adapted authorization ws to use authzrequest ([58c5336](https://github.com/JanssenProject/jans/commit/58c5336fe4978c3709d060cd46f1847c01782af3))
* **jans-auth-server:** added authzrequest abstraction ([af8faf0](https://github.com/JanssenProject/jans/commit/af8faf008eec21a952c3d474169e57a9aece9152))
* **jans-auth-server:** authorized acr values ([#1068](https://github.com/JanssenProject/jans/issues/1068)) ([26e576a](https://github.com/JanssenProject/jans/commit/26e576a5be90ac9597ed37e0d3629a2701008fcf))
* **jans-auth-server:** changed prog lang name python->jython ([b9ba291](https://github.com/JanssenProject/jans/commit/b9ba291e576b8443f37c774088747bab09db2db9))
* **jans-auth-server:** client registration language metadata ([#1237](https://github.com/JanssenProject/jans/issues/1237)) ([a8d0157](https://github.com/JanssenProject/jans/commit/a8d0157b0a8664e5e4d58a9524a0fa20df324381))
* **jans-auth-server:** enable person authn script to have multiple acr names ([#1074](https://github.com/JanssenProject/jans/issues/1074)) ([1dc9250](https://github.com/JanssenProject/jans/commit/1dc9250b9140cfe2a7ea3daff6c9e0d6383c4bce))
* **jans-auth-server:** force signed request object ([#1052](https://github.com/JanssenProject/jans/issues/1052)) ([28ebbc1](https://github.com/JanssenProject/jans/commit/28ebbc10d545ad69ceb4e9a625fbbf13e6360b75))
* **jans-auth-server:** hide 302 redirect exception in logs [#1294](https://github.com/JanssenProject/jans/issues/1294) ([00197c7](https://github.com/JanssenProject/jans/commit/00197c720b444e50c84f49f696fd14768f8fdb08))
* **jans-auth,jans-cli,jans-config-api:** changes to handle new attribute description in Client object and new custom script type ([d64e042](https://github.com/JanssenProject/jans/commit/d64e0424063c79e35b135f4a8bd48f04591b043c))
* **jans-auth,jans-cli,jans-config-api:** changes to handle new attribute description in Client object and new custom script type ([a096110](https://github.com/JanssenProject/jans/commit/a096110d157dec7a0c047692e158c53872fe92fe))
* **jans-auth,jans-cli,jans-config-api:** changes to handle new attribute description in Client object and new custom script type ([d4a9f15](https://github.com/JanssenProject/jans/commit/d4a9f15c3244961cfef6e3229c2e2e49cf85ba0d))
* jans-cli display users in tabular form ([#1296](https://github.com/JanssenProject/jans/issues/1296)) ([7f75d39](https://github.com/JanssenProject/jans/commit/7f75d393cb2854fce58f02f40b90ac4fa9f2a100))
* jans-cli group common items in menu (ref: [#892](https://github.com/JanssenProject/jans/issues/892)) ([#1306](https://github.com/JanssenProject/jans/issues/1306)) ([819f8f7](https://github.com/JanssenProject/jans/commit/819f8f704ab176b70b4daa9c7aca5d662e39a39f))
* jans-cli obtain list of attrbiutes from server when creating user ([1f9b62d](https://github.com/JanssenProject/jans/commit/1f9b62dd133d442d66ef5d3ed6a8cd3ad6da5f7b))
* jans-cli tabulate attribute list ([#1313](https://github.com/JanssenProject/jans/issues/1313)) ([a684484](https://github.com/JanssenProject/jans/commit/a684484d403f9ed52e4c6749f21bd255523a134e))
* jans-cli use test client (ref: [#1283](https://github.com/JanssenProject/jans/issues/1283)) ([#1285](https://github.com/JanssenProject/jans/issues/1285)) ([6320af7](https://github.com/JanssenProject/jans/commit/6320af7ed82ea6fac5672c1c348aeecb7a4b5d7a))
* **jans-config-api:** added custom script patch endpoint ([6daa4f6](https://github.com/JanssenProject/jans/commit/6daa4f61d3a72a0523912bb79566e2e62a6d84be))
* **jans-config-api:** added patch endpoint for custom script ([e274e20](https://github.com/JanssenProject/jans/commit/e274e206188e76654de759bc687a56a80cf4bfbc))
* **jans-config-api:** added patch endpoint for custom script ([f8da77d](https://github.com/JanssenProject/jans/commit/f8da77df201f67055ea7c23c3410e5818a170785))
* **jans-config-api:** added scope DN validation while client creation ([#1293](https://github.com/JanssenProject/jans/issues/1293)) ([f276605](https://github.com/JanssenProject/jans/commit/f276605cb3a1cedb869733253a48d2d63be2fcdc))
* **jans-config-api:** converting fido2 endpoint to plugin  ([#1304](https://github.com/JanssenProject/jans/issues/1304)) ([88c3fff](https://github.com/JanssenProject/jans/commit/88c3fffe177840324fa5f3c4437fa3d02b9ead9b))
* **jans-config-api:** exposed attributes at root value ([3c3df7a](https://github.com/JanssenProject/jans/commit/3c3df7a1beee57fb851a4c820fcbbffb8418bd78))
* **jans-config-api:** exposed attributes at root value ([40570a7](https://github.com/JanssenProject/jans/commit/40570a7b9d2d03358cbfb0d8c3964a4111e15bb5))
* **jans-config-api:** fixed build issue due to LocalizedString change ([#1329](https://github.com/JanssenProject/jans/issues/1329)) ([3b5ab78](https://github.com/JanssenProject/jans/commit/3b5ab783ed9ddf7c7b47612508cc6ab4c334375d))
* **jans-config-api:** ignore client.customObjectClasses value for persistence type other than LDAP ([#1073](https://github.com/JanssenProject/jans/issues/1073)) ([622bcf4](https://github.com/JanssenProject/jans/commit/622bcf4afae94cddff2a19ca5178f2b8230165d5))
* **jans-config-api:** rectified test properties file  ([#1222](https://github.com/JanssenProject/jans/issues/1222)) ([5b80f67](https://github.com/JanssenProject/jans/commit/5b80f672508ea4fa6956c466bfa971684892b6ce))
* **jans-config-api:** removed encrypttion and decryption of user password ([7f50ad0](https://github.com/JanssenProject/jans/commit/7f50ad064a68412ec67145f1b0866f136804761b))
* **jans-config-api:** removed unused import ([8a41484](https://github.com/JanssenProject/jans/commit/8a41484412cbcf5cfc00c550506ad81fb0c70e7c))
* **jans-config-api:** user custom attributes at root level - 1348 ([5b3f0a1](https://github.com/JanssenProject/jans/commit/5b3f0a13e25cd842e0bbd4be3d21eb48ab1d108f))
* **jans-config-api:** user management api ([b367d44](https://github.com/JanssenProject/jans/commit/b367d440fbcc34fff923bc3040dc4e6026d165fd))
* **jans-config-api:** user management api ([517e7f2](https://github.com/JanssenProject/jans/commit/517e7f235b536f833fec11e2f0da49b8ab0f26c8))
* **jans-config-api:** user management api ([a034bc3](https://github.com/JanssenProject/jans/commit/a034bc3f2b80e4eaaa3ed8fba29b52692a5f91a2))
* **jans-config-api:** user management endpoint ([f28f3b8](https://github.com/JanssenProject/jans/commit/f28f3b86d36199fc137714160cfbdad08e9265f9))
* **jans-config-api:** user management enhancement to chk mandatory feilds ([903ba5a](https://github.com/JanssenProject/jans/commit/903ba5a9a778c87de6280332dfee0eea71eaf2f5))
* **jans-config-api:** user management enhancement to chk mandatory feilds ([0bc2282](https://github.com/JanssenProject/jans/commit/0bc22822a390fdd82370c54ed4e15cad06064e02))
* **jans-config-api:** user management enhancement to chk mandatory feilds ([e6e2781](https://github.com/JanssenProject/jans/commit/e6e2781c70ffbbffe92cbaf87e8607854c3d8da1))
* **jans-config-api:** user management mandatory field chk changes ([e242ec6](https://github.com/JanssenProject/jans/commit/e242ec6c2d5edf86cc773d467fc4cd848e4bce13))
* **jans-config-api:** user management patch endpoint ([0a7ad7d](https://github.com/JanssenProject/jans/commit/0a7ad7dba82b419d412329414f5895c22fdcaa68))
* **jans-config-api:** user mgmt endpoint ([a093758](https://github.com/JanssenProject/jans/commit/a0937580eed7c32a0f8bf573bddb9ac8b7080e2c))
* **jans-config-api:** user mgmt endpoint ([ad66713](https://github.com/JanssenProject/jans/commit/ad66713700b6988378c4e3c603fb3518f8ade247))
* **jans-config-api:** user mgmt endpoint ([0f7a723](https://github.com/JanssenProject/jans/commit/0f7a723bc24dd89fdaf3f71afdb4565e1e25f7fe))
* **jans-config-api:** user mgmt endpoint ([379ca09](https://github.com/JanssenProject/jans/commit/379ca09461c31ec9817d1094aebf5b0fa8c16148))
* **jans-config-api:** user mgmt endpoint ([f98c59e](https://github.com/JanssenProject/jans/commit/f98c59e15bb1199037cf6ad9caa67ffff23ca451))
* **jans-config-api:** user mgmt endpoint ([0ea10fd](https://github.com/JanssenProject/jans/commit/0ea10fd10fdd82ea2f170ecfa990c494591ba653))
* **jans-config-api:** user mgmt endpoint - wip ([70987f6](https://github.com/JanssenProject/jans/commit/70987f65c920943a7e214b9b742cd1f83e877995))
* **jans-config-api:** user mgmt endpoint - wip ([af30358](https://github.com/JanssenProject/jans/commit/af30358d6f8933c405e68449041d5a9e121f3b9f))
* **jans-config-api:** user mgmt endpoint - wip ([aadbf8b](https://github.com/JanssenProject/jans/commit/aadbf8b094d42f8acbf031ead6d4324c9925bed0))
* **jans-config-api:** user mgmt endpoint -wip ([ac35327](https://github.com/JanssenProject/jans/commit/ac35327db9183bd75d314ff162e304e70132a035))
* **jans-config-api:** user mgmt endpoints ([1d53b2e](https://github.com/JanssenProject/jans/commit/1d53b2e2acd1d0b4f33b37088ad90506aea522f6))
* **jans-config-api:** user mgmt endpoints ([5cd1ad5](https://github.com/JanssenProject/jans/commit/5cd1ad5fbfddf43dfdbe6f6abf557f386437133d))
* **jans-config-api:** user mgmt patch endpoint ([1180068](https://github.com/JanssenProject/jans/commit/1180068431f92cd6c21fe024371d363cf5eedd01))
* **jans-config-api:** user mgmt patch endpoint ([12a08e1](https://github.com/JanssenProject/jans/commit/12a08e13105467c3912680fb47dc1943590b985a))
* **jans-config-api:** user mgmt patch endpoint ([0427186](https://github.com/JanssenProject/jans/commit/0427186a48def0f16465c96d7839134d8c7902d9))
* **jans-config-api:** user mgmt patch endpoint ([cb7d36c](https://github.com/JanssenProject/jans/commit/cb7d36cd21ac04f683c38f73d4c9642654886c18))
* **jans-config-api:** user mgt plugin ([ccc56f8](https://github.com/JanssenProject/jans/commit/ccc56f888cbcda657cb2b66e2915e1cae8c96f88))
* **jans-config-api:** user mgt plugin ([ae132cf](https://github.com/JanssenProject/jans/commit/ae132cfe829a3d2ff628b22b23ea716e879c769e))
* **jans-config-api:** user-management endpoints ([#1167](https://github.com/JanssenProject/jans/issues/1167)) ([d8e97c4](https://github.com/JanssenProject/jans/commit/d8e97c4b47b1ff38d4a0207d3f07f461fb807630))
* **jans-core:** added more error logs if script is not loaded ([4084aeb](https://github.com/JanssenProject/jans/commit/4084aebc7076ac612f569f72478941a9f1284930))
* **jans-core:** added pure java discovery sample custom script ([1d01ba7](https://github.com/JanssenProject/jans/commit/1d01ba7b67ca5096c987c87c7315e163d632d39a))
* **jans-core:** compile java code on the fly for custom script ([5da6e27](https://github.com/JanssenProject/jans/commit/5da6e2743761cbdf8f06b3dca9a5cf7c8af1abe3))
* **jans-core:** corrected StandaloneCustomScriptManager ([0a52ec8](https://github.com/JanssenProject/jans/commit/0a52ec872d5ad7cbe065fbf3868c35df6e015393))
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
* merge ORM from Gluu ([#1200](https://github.com/JanssenProject/jans/issues/1200)) ([685a159](https://github.com/JanssenProject/jans/commit/685a1593fb53e2310cfa38fcd49db94f3453042f))
* move file downloads to setup ([2680bd0](https://github.com/JanssenProject/jans/commit/2680bd01aa9227aa517de5c7de97c51b9b123a28))
* pre-populate role scope mapping dynamically ([#1201](https://github.com/JanssenProject/jans/issues/1201)) ([3ab6a11](https://github.com/JanssenProject/jans/commit/3ab6a1167328625c26b32d2b3b7cc46d37216261))
* remove Jython's pip from images ([#1176](https://github.com/JanssenProject/jans/issues/1176)) ([e3f374f](https://github.com/JanssenProject/jans/commit/e3f374f5bc3c385374593455243c88e2f7dfc00d))
* user management enhancement to chk mandatory feilds ([3ac4b19](https://github.com/JanssenProject/jans/commit/3ac4b19ada28b11a27707c56ad266ce282f13b60))


### Bug Fixes

* [#1107](https://github.com/JanssenProject/jans/issues/1107) - not required ([cf46672](https://github.com/JanssenProject/jans/commit/cf466722c5ddd70b491d79a82e080557a32ce161))
* [#1107](https://github.com/JanssenProject/jans/issues/1107) jansCodeChallengeHash missing ([65ac184](https://github.com/JanssenProject/jans/commit/65ac1846f19e3d8e7d4833e009cb5cdd58ff2c09))
* add issue guidelines to TOC ([#1188](https://github.com/JanssenProject/jans/issues/1188)) ([192165b](https://github.com/JanssenProject/jans/commit/192165b3eecc3cbcb03a5c9774a4dae212a9ffad))
* add missing permission and defaultPermissionInToken attribute in role-scope mapping ([#1270](https://github.com/JanssenProject/jans/issues/1270)) ([e2c67ec](https://github.com/JanssenProject/jans/commit/e2c67ec8e662adbaab7c5d735217aa5bcbf8495c))
* adjust beans and schema [#1107](https://github.com/JanssenProject/jans/issues/1107) ([#1248](https://github.com/JanssenProject/jans/issues/1248)) ([369129d](https://github.com/JanssenProject/jans/commit/369129d0c2614afb536d0e1329ac106fd7da187d))
* **admin-ui:** the backend issues related to jetty 11 migration [#1258](https://github.com/JanssenProject/jans/issues/1258) ([#1259](https://github.com/JanssenProject/jans/issues/1259)) ([d61be0b](https://github.com/JanssenProject/jans/commit/d61be0bf633020c6bd989e603bb983dc7a45b78b))
* **agama:** adjust pom version [#1402](https://github.com/JanssenProject/jans/issues/1402) ([#1403](https://github.com/JanssenProject/jans/issues/1403)) ([930f080](https://github.com/JanssenProject/jans/commit/930f0801177d516d6bfa9c536d590556144cbd61))
* **agama:** adjust pom version [#1402](https://github.com/JanssenProject/jans/issues/1402) ([#1404](https://github.com/JanssenProject/jans/issues/1404)) ([86bf614](https://github.com/JanssenProject/jans/commit/86bf61420bf6b8d236b8200835a2ff05f430308b))
* avoid duplicated client when re-running persistence-loader and configurator ([#1134](https://github.com/JanssenProject/jans/issues/1134)) ([5567ba9](https://github.com/JanssenProject/jans/commit/5567ba90d0484128b5a875fdc5f1406ce2c69e8a))
* broken links ([86d0232](https://github.com/JanssenProject/jans/commit/86d023209fd8af5422153b1dd97e2a25a7b59c28))
* bug(jans-auth-server): custom pages are not found [#1318](https://github.com/JanssenProject/jans/issues/1318) ([e1e0bf9](https://github.com/JanssenProject/jans/commit/e1e0bf943f35906430b0fae5333f3b76f05734c3))
* change column size of jansFido2AuthnEntry.jansAuthData column ([#1066](https://github.com/JanssenProject/jans/issues/1066)) ([f1c3ffa](https://github.com/JanssenProject/jans/commit/f1c3ffa7fa72114b7e6dc2685789dade0feadf42))
* code smells ([e5aaad7](https://github.com/JanssenProject/jans/commit/e5aaad7da310b26c4d6a6b8cfb6ed00e442b1629))
* **config-api:** scim user management endpoint failing due to conflict with user mgmt path ([#1181](https://github.com/JanssenProject/jans/issues/1181)) ([8ee47a0](https://github.com/JanssenProject/jans/commit/8ee47a0c62ac1d13ad4a62367744e106c759bbc9))
* Data too long for column [#1107](https://github.com/JanssenProject/jans/issues/1107) ([8eb2c70](https://github.com/JanssenProject/jans/commit/8eb2c70c95c2e60486ff1dd5a9e00acd9d70dc3b))
* errors adding/upgrading data into couchbase persistence ([#1226](https://github.com/JanssenProject/jans/issues/1226)) ([db71324](https://github.com/JanssenProject/jans/commit/db71324ee7a94ac06a5505f7ed0993bf8f1c4f79))
* extract directory ([fe7a3c5](https://github.com/JanssenProject/jans/commit/fe7a3c564fb867fda4b28181c3158e8d282da238))
* fix license apis[#1258](https://github.com/JanssenProject/jans/issues/1258) ([#1271](https://github.com/JanssenProject/jans/issues/1271)) ([14c6a2b](https://github.com/JanssenProject/jans/commit/14c6a2b757bf94116faf9c0f13ab8c5e64c31f32))
* handle index error for JSON columns ([#1205](https://github.com/JanssenProject/jans/issues/1205)) ([90f77c3](https://github.com/JanssenProject/jans/commit/90f77c39beeb8a4c30a46819a7877514fdaa4531))
* hyperlinks ([#1209](https://github.com/JanssenProject/jans/issues/1209)) ([d1e1ed6](https://github.com/JanssenProject/jans/commit/d1e1ed63d8cea3030b23bd218241fe2275ed6e52))
* invalid LDAP schema reading token_server client ID ([#1321](https://github.com/JanssenProject/jans/issues/1321)) ([db4f080](https://github.com/JanssenProject/jans/commit/db4f0809bb697cc2e88a7ad58917006f132ea5e5))
* jans cli update readme ([2f4f57f](https://github.com/JanssenProject/jans/commit/2f4f57f3d1d38e2c7ca8fd2edc1da798dc36d425))
* **jans-auth-server:** added faces context as source of locale ([#1189](https://github.com/JanssenProject/jans/issues/1189)) ([ce770ae](https://github.com/JanssenProject/jans/commit/ce770aed92c4279647d0bdd541a943ada9e6c743))
* **jans-auth-server:** authorize page message policy ([#1096](https://github.com/JanssenProject/jans/issues/1096)) ([f10ccb1](https://github.com/JanssenProject/jans/commit/f10ccb166307cf281cbd36c757972eb3e1babf2e))
* **jans-auth-server:** corrected fallback value of checkUserPresenceOnRefreshToken ([a822ae5](https://github.com/JanssenProject/jans/commit/a822ae5546934f4d9cabd1c3e4540b4a23d5abe0))
* **jans-auth-server:** corrected log vulnerability ([1000a60](https://github.com/JanssenProject/jans/commit/1000a60d3a4263784250960565c73d98a52f200a))
* **jans-auth-server:** corrected npe in response type class ([941248d](https://github.com/JanssenProject/jans/commit/941248d9deed74b82453e161ffd8d5badd00546a))
* **jans-auth-server:** corrected signature algorithm identification with java 11 and later ([3e203f2](https://github.com/JanssenProject/jans/commit/3e203f27e4b6bdb59d25cb59f823c12675c3ffd3))
* **jans-auth-server:** corrected thread-safety bug in ApplicationAuditLogger [#803](https://github.com/JanssenProject/jans/issues/803) ([ef73c2b](https://github.com/JanssenProject/jans/commit/ef73c2b375f021f16117b1e987a3bd487596bd5b))
* **jans-auth-server:** disabled issuing AT by refresh token if user status=inactive ([3df72a8](https://github.com/JanssenProject/jans/commit/3df72a83a59d11b2ac32aad80ec8207560f4813e))
* **jans-auth-server:** do not serialize jwkThumbprint ([d8634fe](https://github.com/JanssenProject/jans/commit/d8634fef2aa497787b0c7e5bb37179f8259eb415))
* **jans-auth-server:** during encryption AS must consider client's jwks too, not only jwks_uri ([475b154](https://github.com/JanssenProject/jans/commit/475b1547dc35608925b4dc07a70130b34c355d1b))
* **jans-auth-server:** dynamic client registration managment delete event ([911e54b](https://github.com/JanssenProject/jans/commit/911e54b0858b02d97178ee6f03192d6a5919e47d))
* **jans-auth-server:** escape login_hint before rendering ([e1a682a](https://github.com/JanssenProject/jans/commit/e1a682aadd083e3000f51fa950dc4feb83680f1c))
* **jans-auth-server:** fixed equals/hashcode by removing redundant dn field ([d27659d](https://github.com/JanssenProject/jans/commit/d27659d99200246de68387273c308bda012f39af))
* **jans-auth-server:** fixed server and tests after jetty 11 migration ([#1354](https://github.com/JanssenProject/jans/issues/1354)) ([3fa19f4](https://github.com/JanssenProject/jans/commit/3fa19f491b6ef810eb679ca23551abcbdf2086cb))
* **jans-auth-server:** gluuStatus -> jansStatus ([7f86d6d](https://github.com/JanssenProject/jans/commit/7f86d6d5d7539259d279f7f5eb1ab4320617c598))
* **jans-auth-server:** isolate regex redirection uri validation test ([#1075](https://github.com/JanssenProject/jans/issues/1075)) ([cca0551](https://github.com/JanssenProject/jans/commit/cca055127dc57f29b6bc4e913b7a2a52ad5a1a88))
* **jans-auth-server:** removed CONFIG_API from AS supported script types [#1286](https://github.com/JanssenProject/jans/issues/1286) ([c209868](https://github.com/JanssenProject/jans/commit/c209868c4fa94caf135e5726e3caa5b4462fd38d))
* **jans-auth-server:** removed ThumbSignInExternalAuthenticator ([a13ca51](https://github.com/JanssenProject/jans/commit/a13ca51a753bc7f779899e0c86865c1a6bdb0374))
* **jans-auth-server:** renamed localization resoruces files [#1198](https://github.com/JanssenProject/jans/issues/1198) ([#1199](https://github.com/JanssenProject/jans/issues/1199)) ([4561f2a](https://github.com/JanssenProject/jans/commit/4561f2a7f5194aba76e9644a3eb7627badb58c76))
* **jans-auth-server:** restored id generator call to external custom script ([#1128](https://github.com/JanssenProject/jans/issues/1128)) ([5ba98c1](https://github.com/JanssenProject/jans/commit/5ba98c13104a8559242ba7240fe8bbfe314fc0c5))
* **jans-auth-server:** use duration class instead of custom util to calculate seconds from date to now ([#1249](https://github.com/JanssenProject/jans/issues/1249)) ([5ae76ab](https://github.com/JanssenProject/jans/commit/5ae76ab0298995e971635f63b6c83cf455b16e14))
* **jans-auth-server:** validate redirect_uri blank and client redirect uris single item to return by default ([#1046](https://github.com/JanssenProject/jans/issues/1046)) ([aa139e4](https://github.com/JanssenProject/jans/commit/aa139e46e6d25c6135eb05e22dbc36fe84eb3e86))
* jans-cl update WebKeysConfiguration ([#1211](https://github.com/JanssenProject/jans/issues/1211)) ([54847bc](https://github.com/JanssenProject/jans/commit/54847bce0f066ca1ca5d3e0cf01420815c30868c))
* jans-cli allow emptying list attrbiutes by _null ([#1166](https://github.com/JanssenProject/jans/issues/1166)) ([571c5cd](https://github.com/JanssenProject/jans/commit/571c5cd38e42871dd27605f68058b5d766e1f91e))
* jans-cli code smells ([1dc5cb0](https://github.com/JanssenProject/jans/commit/1dc5cb0d05ab3a97c9e414d80e81f0d75586f087))
* jans-cli do not require client if access token is provided ([6b787ec](https://github.com/JanssenProject/jans/commit/6b787ec0313794ac04b81f949d5a2c5f1a5f21dc))
* jans-cli hardcode enums ([739a759](https://github.com/JanssenProject/jans/commit/739a7595dd98751142835957e7d006c59872c89e))
* jans-cli scope dn/id when creating client ([518f971](https://github.com/JanssenProject/jans/commit/518f97147970c3a2465f4ef7d14481b05129f346))
* jans-cli scope dn/id when creating client ([f056abf](https://github.com/JanssenProject/jans/commit/f056abfe98c478c76fad9c6ec1d30b5287b1e208))
* **jans-cli:** corrected typo ([#1050](https://github.com/JanssenProject/jans/issues/1050)) ([4d93a49](https://github.com/JanssenProject/jans/commit/4d93a4926e46e7d82980a187a0be49aac0df9c1c))
* jans-client-api replace netstat with ss in startup script ([#1246](https://github.com/JanssenProject/jans/issues/1246)) ([cde3fb1](https://github.com/JanssenProject/jans/commit/cde3fb1ef1d8f33c74983b0936485e8298e155bf))
* **jans-config-api:** corrected typo in swagger spec ([3c11556](https://github.com/JanssenProject/jans/commit/3c115566c843e42ae9827a76496145ddc6288155))
* **jans-config-api:** LDAP test endpoint fix ([#1320](https://github.com/JanssenProject/jans/issues/1320)) ([fb0e132](https://github.com/JanssenProject/jans/commit/fb0e13251ee645862d8f02cbade5d64a2673a0b6))
* **jans-core:** corrected ExternalUmaClaimsGatheringService ([cfe1b6d](https://github.com/JanssenProject/jans/commit/cfe1b6d0eae75a699fc0505fea46e955a3480b57))
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
* set permission for jans-auth.xml explicitly ([#1315](https://github.com/JanssenProject/jans/issues/1315)) ([80f33a2](https://github.com/JanssenProject/jans/commit/80f33a23902af7498fa85d2785abe1af77a1751e))
* submit button is missing from the Properties page [#175](https://github.com/JanssenProject/jans/issues/175) ([2424965](https://github.com/JanssenProject/jans/commit/242496594af8fd5d82960747c53078149a7e1e57))
* the admin-ui backend issues related to jetty 11 migration [#1258](https://github.com/JanssenProject/jans/issues/1258) ([cf94d5f](https://github.com/JanssenProject/jans/commit/cf94d5f56f43b523f3bfd06429992f4705a0a4ae))
* typo and indexing error ([#1125](https://github.com/JanssenProject/jans/issues/1125)) ([dc87dc0](https://github.com/JanssenProject/jans/commit/dc87dc01c4c63d6fcc2b967ce97a52880083b95f))
* Typo httpLoggingExludePaths jans-auth-server jans-cli jans-config-api jans-linux-setup docker-jans-persistence-loader ([47a20ee](https://github.com/JanssenProject/jans/commit/47a20eefa781d1ca07a9aa30a5adcde3793076d1))
* typo in jans-cli interactive mode ([25f5971](https://github.com/JanssenProject/jans/commit/25f59716aa2bccb2dcdb47a34a7039a0e83d0f5f))
* update api-admin permissions from config api yaml ([#1183](https://github.com/JanssenProject/jans/issues/1183)) ([438c896](https://github.com/JanssenProject/jans/commit/438c8967bbd925779e8ec7b84b9021de32ec409c))
* update mysql/spanner mappings [#1053](https://github.com/JanssenProject/jans/issues/1053) ([94fb2c6](https://github.com/JanssenProject/jans/commit/94fb2c6d0f5de061eca515c003be679f35757faa))
* update templates [#1053](https://github.com/JanssenProject/jans/issues/1053) ([2e33a43](https://github.com/JanssenProject/jans/commit/2e33a43f1d1cc029bcb96992b7bd468956d738fc))
* Use highest level script in case ACR script is not found. Added FF to keep existing behavior. ([#1070](https://github.com/JanssenProject/jans/issues/1070)) ([07473d9](https://github.com/JanssenProject/jans/commit/07473d9a8c3e31f6a75670a874e17341518bf0be))
* use secure http urls for maven repositories ([#1353](https://github.com/JanssenProject/jans/issues/1353)) ([496b5b2](https://github.com/JanssenProject/jans/commit/496b5b296b85e9d9c2b87dbfb93fc6f4bb6430b5))
* use shutil instead of zipfile ([c0a0cde](https://github.com/JanssenProject/jans/commit/c0a0cde87874a73a61bcf87efdfedada1e4f4f10))


### Miscellaneous Chores

* prepare release 1.0.0-1 ([8985928](https://github.com/JanssenProject/jans/commit/89859286d69e7de7885bd9da9f50720c8371e797))
* release 1.0.0 ([9644d1b](https://github.com/JanssenProject/jans/commit/9644d1bd29c291e57c140b0c9ac67243c322ac35))
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

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/v1.0.0-beta.15...v1.0.0-beta.16) (2022-03-14)


### Features

* add acrValues property in admin-ui configuration. [#1016](https://github.com/JanssenProject/jans/issues/1016) ([#1017](https://github.com/JanssenProject/jans/issues/1017)) ([88b591a](https://github.com/JanssenProject/jans/commit/88b591a64bf9ed0fb49942b770d9f0e334b7433c))
* add support for role-based client (i.e. jans-cli) ([#956](https://github.com/JanssenProject/jans/issues/956)) ([306bd52](https://github.com/JanssenProject/jans/commit/306bd524bb1f3139aaed9ca3b3be91390de70fe7))
* add support to import custom ldif ([#1002](https://github.com/JanssenProject/jans/issues/1002)) ([0b6334a](https://github.com/JanssenProject/jans/commit/0b6334acdb862ce458c628a8eb81ef0b8f7c5dcb))
* add validity length (in days) for certs ([#981](https://github.com/JanssenProject/jans/issues/981)) ([abc89dc](https://github.com/JanssenProject/jans/commit/abc89dc6fadae5627a68a97ab4f4f5ceb56af809))
* **jans-auth-server:** forbid plain pkce if fapi=true (fapi1-advanced-final-par-plain-pkce-rejected fail) [#946](https://github.com/JanssenProject/jans/issues/946) ([21cecb0](https://github.com/JanssenProject/jans/commit/21cecb04909a9b69da5da3a206c83ca52c9e2c8b))
* **jans-auth-server:** new client config option defaultpromptlogin [#979](https://github.com/JanssenProject/jans/issues/979) ([4e3de26](https://github.com/JanssenProject/jans/commit/4e3de2627f676d35186877a8570de6ce8950ec57))
* **jans-cli:** get access token from arg ([#1013](https://github.com/JanssenProject/jans/issues/1013)) ([efd718a](https://github.com/JanssenProject/jans/commit/efd718ae39cadd97f2d464572901af2b82932284))
* **jans-config-api:** swagger spec change to add extension ([4f9d76c](https://github.com/JanssenProject/jans/commit/4f9d76cef689649f993df25e88e56526cfd26d02))
* **jans-config-api:** swagger spec change to add extension to differentiate plugin en… ([4f9d76c](https://github.com/JanssenProject/jans/commit/4f9d76cef689649f993df25e88e56526cfd26d02))
* **jans-linux-setup:** check availibility of ports for OpenDJ backend ([#949](https://github.com/JanssenProject/jans/issues/949)) ([a2944c1](https://github.com/JanssenProject/jans/commit/a2944c1ee432985c2bb8e8d52c22710ec73f7039))
* **jans-linux-setup:** install mod_auth_openidc (ref: [#909](https://github.com/JanssenProject/jans/issues/909)) ([#952](https://github.com/JanssenProject/jans/issues/952)) ([270a7b6](https://github.com/JanssenProject/jans/commit/270a7b6e1f83f08a2a3caadb2ef1ee36e4233957))
* **jans-linux-setup:** refactored argsp ([#969](https://github.com/JanssenProject/jans/issues/969)) ([409d364](https://github.com/JanssenProject/jans/commit/409d364383a1777ce0c5ef85fc19b432bce6c6d1))
* support regex client attribute to validate redirect uris ([#1005](https://github.com/JanssenProject/jans/issues/1005)) ([a78ee1a](https://github.com/JanssenProject/jans/commit/a78ee1a3cfc4e7a6d08a500750edb5db0f7709a4))
* swagger spec change to add extension to differentiate plugin endpoint ([bb3b88a](https://github.com/JanssenProject/jans/commit/bb3b88a59376ff8875e1b38048a9c360e01de8de))


### Bug Fixes

* ** jans-linux-setup:** added to extraClasspath ([#968](https://github.com/JanssenProject/jans/issues/968)) ([bfb0bfe](https://github.com/JanssenProject/jans/commit/bfb0bfe63abdc86a1384badfe15e3d985213001e))
* add missing values for openbanking ([#939](https://github.com/JanssenProject/jans/issues/939)) ([b140892](https://github.com/JanssenProject/jans/commit/b140892d3c697226b642e18402ace6ea69b38f48))
* avoid jetty hot-deployment issue ([#1012](https://github.com/JanssenProject/jans/issues/1012)) ([a343215](https://github.com/JanssenProject/jans/commit/a34321594055305d52aa855b32d060b113313652))
* change in swagger spec for jwks to return missing attributes ([477643b](https://github.com/JanssenProject/jans/commit/477643bf6cc1fc6226ce7790e05c1a981324d06e))
* **ci:** fix change identification logic ([#966](https://github.com/JanssenProject/jans/issues/966)) ([e964291](https://github.com/JanssenProject/jans/commit/e964291807b475ecf930ffb4ba86fd3501058f96))
* jans cli build issues (update doc and fix requirements) ([#938](https://github.com/JanssenProject/jans/issues/938)) ([18d1507](https://github.com/JanssenProject/jans/commit/18d1507936a9fdcd8ee6daa46f2ca0af070ea4ba))
* **jans-auth-server:** corrected ParValidatorTest [#946](https://github.com/JanssenProject/jans/issues/946) ([04a01fd](https://github.com/JanssenProject/jans/commit/04a01fd43e1969bc09494b2f08387bcc7d502ed7))
* **jans-auth-server:** corrected sonar reported issue ([7c88078](https://github.com/JanssenProject/jans/commit/7c8807820a217f33c66f496b05863e9d77d8c7e8))
* **jans-auth-server:** fix npe ([e6debb2](https://github.com/JanssenProject/jans/commit/e6debb24ea0ea1963290b543d74df7f0761efe3b))
* **jans-auth-server:** reduce noise in logs when session can't be found ([47afc47](https://github.com/JanssenProject/jans/commit/47afc47a239c48c090591d9fa561757e7749d96d))
* **jans-auth-server:** removed reference of removed tests [#996](https://github.com/JanssenProject/jans/issues/996) ([cabc4f2](https://github.com/JanssenProject/jans/commit/cabc4f2f6119e2aff0440fdb1bb4dd1f11dce2cd))
* **jans-auth-server:** validate pkce after extraction data from request object ([#999](https://github.com/JanssenProject/jans/issues/999)) ([29fdfae](https://github.com/JanssenProject/jans/commit/29fdfae276b61890ed345804827aa83437acd428))
* **jans-config-api:** create openid client throwing 502 ([#1004](https://github.com/JanssenProject/jans/issues/1004)) ([3f58aff](https://github.com/JanssenProject/jans/commit/3f58affce39a15e051a1188c619b40115607f437))
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

* [#836](https://github.com/JanssenProject/jans/issues/836) support push token update on finish authentication ([#837](https://github.com/JanssenProject/jans/issues/837)) ([4d6d916](https://github.com/JanssenProject/jans/commit/4d6d9162c72d067b315bd56fe0993c50e7ad6ecb))
* add correlation id in pages and rest endpoints to track logs ([#410](https://github.com/JanssenProject/jans/issues/410)) ([b9c39af](https://github.com/JanssenProject/jans/commit/b9c39afad262a24df91f023b78081b96c039b1d0))
* add correlation id in pages and rest endpoints to track logs ([#410](https://github.com/JanssenProject/jans/issues/410)) ([27fab9f](https://github.com/JanssenProject/jans/commit/27fab9f6dc9b84dcc789c1ad5a228aa6ea48585b))
* add deletable flag to admin-ui role object [#888](https://github.com/JanssenProject/jans/issues/888) ([#901](https://github.com/JanssenProject/jans/issues/901)) ([5b95a55](https://github.com/JanssenProject/jans/commit/5b95a552130f69df91b3d841e07df5d7d64e3c74))
* add Gluu Casa support ([608a9b8](https://github.com/JanssenProject/jans/commit/608a9b857872d7ccc65931a4dd9307a064e55492))
* add Gluu Casa support ([608a9b8](https://github.com/JanssenProject/jans/commit/608a9b857872d7ccc65931a4dd9307a064e55492))
* add Gluu Casa support ([089a872](https://github.com/JanssenProject/jans/commit/089a87214a9349916b537ef6755a10ef468f6221))
* add Gluu Casa support ([089a872](https://github.com/JanssenProject/jans/commit/089a87214a9349916b537ef6755a10ef468f6221))
* add Helm chart for Core Janssen Distro ([#753](https://github.com/JanssenProject/jans/issues/753)) ([edb35d7](https://github.com/JanssenProject/jans/commit/edb35d7f865018562d48c628bf3140aad8b56f62))
* add jansClaimName to all attrbiutes ([8f219fb](https://github.com/JanssenProject/jans/commit/8f219fb6f1ee63c259ca9482f74a9c4e37bf6507))
* add jansClaimName to all attrbiutes ([0947757](https://github.com/JanssenProject/jans/commit/0947757c8a674c008104a881a1b95f04a3fbf9ee))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([41d5913](https://github.com/JanssenProject/jans/commit/41d59139f2ac723cf0b76ca78af4cc6b75c37a63))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([85e777b](https://github.com/JanssenProject/jans/commit/85e777b1a9253e2fe80a431ad60294e8ac89dae6))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([6ac57dc](https://github.com/JanssenProject/jans/commit/6ac57dc4332ff1e975948fe118796977b11e6ce1))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([d5b28fc](https://github.com/JanssenProject/jans/commit/d5b28fc1de665eeca6ff5addbdb2dca64211bba5))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([bd34c29](https://github.com/JanssenProject/jans/commit/bd34c292fddcaf3698d96918450031097938f84d))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([149aeb1](https://github.com/JanssenProject/jans/commit/149aeb1069078ff09d0651b05bf6bb8e4515da31))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([7bcad3b](https://github.com/JanssenProject/jans/commit/7bcad3b5b68ac748dec7be18651641146943bd22))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([932a45b](https://github.com/JanssenProject/jans/commit/932a45bc4c34235dad5c5813a10f157b0350966b))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([cc4bc91](https://github.com/JanssenProject/jans/commit/cc4bc9149ee5b4fbaf1ca74a9fddb157d792ed23))
* add POST /rolePermissionsMapping for adding new rolePermissionsMapping entry [#144](https://github.com/JanssenProject/jans/issues/144) ([4b2bea7](https://github.com/JanssenProject/jans/commit/4b2bea76e5f25d66722a2cf8f50a705ab6cf1eff))
* added ability remove all tokens per client by token type ([3cd3ada](https://github.com/JanssenProject/jans/commit/3cd3ada04681ad467553273611b828f8f0760bf5))
* added ability remove all tokens per client by token type ([5e8fad9](https://github.com/JanssenProject/jans/commit/5e8fad95a83f3451d85e02de12d693658ea68c50))
* allow jwt tokens for scim service protection [#834](https://github.com/JanssenProject/jans/issues/834) ([#856](https://github.com/JanssenProject/jans/issues/856)) ([392b3fc](https://github.com/JanssenProject/jans/commit/392b3fc94fdfc7d45d5d85f953974188f9a04b6a))
* ce-setup add python cryptography library to dependency ([d312479](https://github.com/JanssenProject/jans/commit/d3124791c506cf5f6b0c2a666ae69e2471cdbf74))
* ce-setup: deploy facter script (ref: [#622](https://github.com/JanssenProject/jans/issues/622)) ([#624](https://github.com/JanssenProject/jans/issues/624)) ([9426517](https://github.com/JanssenProject/jans/commit/9426517749db916488760eba1266fc88ae98e24b))
* enable/disable admin-ui frontend plugins (ref: [#150](https://github.com/JanssenProject/jans/issues/150)) ([aaf8ca1](https://github.com/JanssenProject/jans/commit/aaf8ca189ac92917936b7dd093f5bffb95f8351a))
* enable/disable admin-ui frontend plugins (ref: [#150](https://github.com/JanssenProject/jans/issues/150)) ([66ca69f](https://github.com/JanssenProject/jans/commit/66ca69f060498fd8a805ace0ca6e0c93aa4b669e))
* if value is null return null to token type hint ([78ed12f](https://github.com/JanssenProject/jans/commit/78ed12f9fb36ae87c5bb5cede9569d9c086cb665))
* if value is null return null to token type hint ([848f1ca](https://github.com/JanssenProject/jans/commit/848f1cae282da76df39c7ca896016cf6f5ad2eb4))
* **image:** implement role-based scope script ([#813](https://github.com/JanssenProject/jans/issues/813)) ([bd68898](https://github.com/JanssenProject/jans/commit/bd6889861847fcba2f18cbc63a8b7f63ebbea341))
* **image:** push otp and super_gluu configuration to secrets ([#784](https://github.com/JanssenProject/jans/issues/784)) ([87bd7fe](https://github.com/JanssenProject/jans/commit/87bd7fe66dad6a652e965597e44a424dd6f92c62))
* **jans-auth-server:** add methods to dynamic client registration script to modify POST, PUT and GET responses ([#661](https://github.com/JanssenProject/jans/issues/661)) ([2aa2ba8](https://github.com/JanssenProject/jans/commit/2aa2ba86a2a639336079a1151ec38aca93ed9360))
* **jans-auth-server:** added cache support to /stat endpoint ([e1dba92](https://github.com/JanssenProject/jans/commit/e1dba928dc36e5cc27212f1a3c51613fd2842ef0))
* **jans-auth-server:** added new stat response service with test ([9d60629](https://github.com/JanssenProject/jans/commit/9d60629d44967e541d963195fe43e1846dfa6279))
* **jans-auth-server:** added post response modification method ([db936f9](https://github.com/JanssenProject/jans/commit/db936f9de9ec31f3fa62d23a5e96922c5e892227))
* **jans-auth-server:** added put response modification method ([00a24f2](https://github.com/JanssenProject/jans/commit/00a24f22c15343207ab797155714722171cc3df8))
* **jans-auth-server:** added read response modification method ([ec8864b](https://github.com/JanssenProject/jans/commit/ec8864bad991f2f27a2ab8969001d2cc59a00979))
* **jans-auth-server:** added to par extra nbf and exp (for 60min) validation ([#838](https://github.com/JanssenProject/jans/issues/838)) ([9db47a4](https://github.com/JanssenProject/jans/commit/9db47a49f2dcfec67fb35b4e6b82537e50709c43))
* **jans-auth-server:** allow return custom authz params to rp in response ([#756](https://github.com/JanssenProject/jans/issues/756)) ([0e865fb](https://github.com/JanssenProject/jans/commit/0e865fbace7d12634ad57b510a1ad81a9067f01f))
* **jans-auth-server:** extending crypto support sub pr4 ([#670](https://github.com/JanssenProject/jans/issues/670)) ([fe07d76](https://github.com/JanssenProject/jans/commit/fe07d76b9c60b10f87213cb4e6eb7e6198d641e9))
* **jans-auth-server:** invoke custom script methods for response modification ([da44d5a](https://github.com/JanssenProject/jans/commit/da44d5a033b7a6343747e7aad01d3899984cc38d))
* **jans-auth-server:** preparations for revoke refresh tokens for client ([b1cf69b](https://github.com/JanssenProject/jans/commit/b1cf69baa54973c07b241c4b6432290f0738936d))
* **jans-auth-server:** preparations for revoke refresh tokens for client ([2641574](https://github.com/JanssenProject/jans/commit/264157458d26bb5298bf1fd544aa8ca825d10ffe))
* **jans-auth-server:** reject par without pkce for fapi ([332df41](https://github.com/JanssenProject/jans/commit/332df4168894220b5ea5acdf84d8977d8f8f161d))
* **jans-auth-server:** set public subject identifier per client ([#800](https://github.com/JanssenProject/jans/issues/800)) ([c303bbc](https://github.com/JanssenProject/jans/commit/c303bbc4c928b32144a657b9c119846ed29cd522))
* **jans-auth-server:** turn off consent for pairwise openid-only scope ([#708](https://github.com/JanssenProject/jans/issues/708)) ([a96007d](https://github.com/JanssenProject/jans/commit/a96007d03566a84bd1cffb90956451f5e3ad5b9f))
* **jans-ce-setup:** ce-setup suse compatibility ([#692](https://github.com/JanssenProject/jans/issues/692)) ([f0076fc](https://github.com/JanssenProject/jans/commit/f0076fc38529b4d0aa8c4143d90b1aa24878a61a))
* **jans-ce-setup:** extending crypto support, sub pr4; [#142](https://github.com/JanssenProject/jans/issues/142); ([#669](https://github.com/JanssenProject/jans/issues/669)) ([b8fda09](https://github.com/JanssenProject/jans/commit/b8fda09b8ece52ce4dcd0d5f883f4bedd1f23c4e))
* **jans-ce-setup:** updated client registration script with newly added methods ([595bfd4](https://github.com/JanssenProject/jans/commit/595bfd40106cc0a0ee24481187da2ad8ef8d1539))
* jans-cli logout (ref: [#706](https://github.com/JanssenProject/jans/issues/706)) ([#723](https://github.com/JanssenProject/jans/issues/723)) ([0cc51bc](https://github.com/JanssenProject/jans/commit/0cc51bc18a40476ed6bc638225b6897a11c21c16))
* **jans-config-api:** add deletable flag to admin-ui role object [#888](https://github.com/JanssenProject/jans/issues/888) ([#900](https://github.com/JanssenProject/jans/issues/900)) ([500a773](https://github.com/JanssenProject/jans/commit/500a77358ad6d811fc95de3a13829d6f983bc1b0))
* **jans-config-api:** all config-api plugins should be in same plugins folder in maven repo [#851](https://github.com/JanssenProject/jans/issues/851) ([#852](https://github.com/JanssenProject/jans/issues/852)) ([cca93b2](https://github.com/JanssenProject/jans/commit/cca93b2cf6befad9488a8c0bdbf2554d4886faf1))
* **jans-config-api:** config api interception script ([#840](https://github.com/JanssenProject/jans/issues/840)) ([8e4c688](https://github.com/JanssenProject/jans/commit/8e4c68889f9286e68ddd79d05ebd0d1bebd68097))
* **jans-config-api:** organization configuration management endpoints ([#790](https://github.com/JanssenProject/jans/issues/790)) ([40ca464](https://github.com/JanssenProject/jans/commit/40ca464b17a5dc0324d01b0510dac8b0beec9bd0))
* **jans-config-api:** scim config endpoint issue [#271](https://github.com/JanssenProject/jans/issues/271) ([#665](https://github.com/JanssenProject/jans/issues/665)) ([a6e9a04](https://github.com/JanssenProject/jans/commit/a6e9a0435a30816fd738b9287a4842fe03321a6c))
* **jans-config-api:** security issue - upgrade dependencies ([#883](https://github.com/JanssenProject/jans/issues/883)) ([10568ff](https://github.com/JanssenProject/jans/commit/10568ff1123bc27900254bcf865d23f6be4c59ad))
* **jans-config-api:** swagger update for default value ([#862](https://github.com/JanssenProject/jans/issues/862)) ([8f59921](https://github.com/JanssenProject/jans/commit/8f599219f00f85f1624d89745ec74eaf5c17df49))
* **jans-config-api:** underlying server stats [#275](https://github.com/JanssenProject/jans/issues/275) ([ae6f2d7](https://github.com/JanssenProject/jans/commit/ae6f2d7f89ae3c72e62bcb42b1e62c9c350f657e))
* **jans-core:** added methods for register response modification ([9f18613](https://github.com/JanssenProject/jans/commit/9f1861313982b1b702f854260b94637eac768a68))
* **jans-core:** added read response modification method ([74bbe38](https://github.com/JanssenProject/jans/commit/74bbe38ef5f4e3940e36a665c621daf95ca84bde))
* **jans-linux-setup:** added config-api interception script (ref: [#831](https://github.com/JanssenProject/jans/issues/831)) ([#882](https://github.com/JanssenProject/jans/issues/882)) ([48a3195](https://github.com/JanssenProject/jans/commit/48a3195addf27ee1fa92b1e901b358ec5304e0ea))
* **jans-linux-setup:** import custom ldif ([#873](https://github.com/JanssenProject/jans/issues/873)) ([363cf0e](https://github.com/JanssenProject/jans/commit/363cf0e63e8d43e360da05a70de2caf1540b1eae))
* **jans-linux-setup:** Jans linux setup pkg mysql and uninstall ([#827](https://github.com/JanssenProject/jans/issues/827)) ([0fb53e1](https://github.com/JanssenProject/jans/commit/0fb53e1efddb65441c25da69e95d60dc70780f8c))
* **jans-setup:** updated sample revoke script ([45e43db](https://github.com/JanssenProject/jans/commit/45e43db62f5794ac016526ee37ffa372599cb3dc))
* **jans-setup:** updated sample revoke script ([607a23c](https://github.com/JanssenProject/jans/commit/607a23c8e42d6c0a252d1eb661c9bfb85e9e818c))
* linux-setup node installer ([662a27f](https://github.com/JanssenProject/jans/commit/662a27f5810bb1cd95105fc9d6c84fd29c178ff3))
* par should be able to register with nbf ([a4a2981](https://github.com/JanssenProject/jans/commit/a4a29817b9629baaff2aac9181b4483ae95f3447))
* protecting Admin-UI Plugin Apis [#142](https://github.com/JanssenProject/jans/issues/142) ([52e8846](https://github.com/JanssenProject/jans/commit/52e8846f3eeb9c3b5d624f67ea736f62b455eeed))
* protecting Admin-UI Plugin Apis [#142](https://github.com/JanssenProject/jans/issues/142) ([a1a0d54](https://github.com/JanssenProject/jans/commit/a1a0d54c1c0860e8828afb47e3548b0876394e83))
* removed revoke token context ([84b27cd](https://github.com/JanssenProject/jans/commit/84b27cdb5509bdc83aa3bd73eb2a33fca1f87067))
* removed revoke token context ([4f71ae2](https://github.com/JanssenProject/jans/commit/4f71ae2f20333de3310cdda95662509cb5be534c))
* support MySQL with MariaDB engine gluu [#18](https://github.com/JanssenProject/jans/issues/18) ([#712](https://github.com/JanssenProject/jans/issues/712)) ([9071db4](https://github.com/JanssenProject/jans/commit/9071db4be4d51f7a4042e7fb81704f490e90cf38))
* underlying server stats ([ae6f2d7](https://github.com/JanssenProject/jans/commit/ae6f2d7f89ae3c72e62bcb42b1e62c9c350f657e))
* underlying server stats ([0f36336](https://github.com/JanssenProject/jans/commit/0f36336da9cacad8de8f1bfc060da66235494b79))
* underlying server stats ([56b72e9](https://github.com/JanssenProject/jans/commit/56b72e9272d0bd69b4a31bb0f8320d662233988a))
* update base images [#672](https://github.com/JanssenProject/jans/issues/672) ([#673](https://github.com/JanssenProject/jans/issues/673)) ([0a23d08](https://github.com/JanssenProject/jans/commit/0a23d085ea8fe16d0b4cd21cd3ec8cde59df9f9a))
* update config-api image ([#874](https://github.com/JanssenProject/jans/issues/874)) ([b9f56c3](https://github.com/JanssenProject/jans/commit/b9f56c3ee24e695c0ecd46a551c2872932c3d080))
* use ExecutionContext instead of token revoke context in custom scripts ([b67af11](https://github.com/JanssenProject/jans/commit/b67af1198a4aefcc0a944a1fa5bd37d7c0cdd3ed))
* use ExecutionContext instead of token revoke context in custom scripts ([e1ba0b4](https://github.com/JanssenProject/jans/commit/e1ba0b444e40a3fa20ffb7a82c2286419f05d68a))


### Bug Fixes

* brazilob jarm fapi conformance test last7 issues ([#695](https://github.com/JanssenProject/jans/issues/695)) ([edab074](https://github.com/JanssenProject/jans/commit/edab0746e7febb12546c58a74500d815a71d94b2))
* ce-setup: typo ([af37066](https://github.com/JanssenProject/jans/commit/af37066c680af747404b2b10770f9c164a9021ee))
* **certmanager:** patches for auth handler ([#626](https://github.com/JanssenProject/jans/issues/626)) ([d95453f](https://github.com/JanssenProject/jans/commit/d95453f3a2234518b6c6d8ffd136543ba08bd238))
* check MariaDB json in result set metadata ([96b6772](https://github.com/JanssenProject/jans/commit/96b67720018fd7d1100ddeeda8f1434bddca2816))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([76593c5](https://github.com/JanssenProject/jans/commit/76593c53920ab710c8ac18cce4c698a456cdfdd5))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([1cc118f](https://github.com/JanssenProject/jans/commit/1cc118f38b6a6fc8602efd91e373c565f58b0a7c))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([5570336](https://github.com/JanssenProject/jans/commit/557033639aa6e92a5f40be02e0b1ff46f644fac7))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([ef5cc52](https://github.com/JanssenProject/jans/commit/ef5cc52febd78cb1a35d1fc749b0dfa66e582879))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([776ce2a](https://github.com/JanssenProject/jans/commit/776ce2aa768f08333379c90562978c39a9f204a8))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([8818dbc](https://github.com/JanssenProject/jans/commit/8818dbcbfe7e7d9d736bbd06793c83715e605432))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([87e0963](https://github.com/JanssenProject/jans/commit/87e096347425014bd63e6c436c47e7a5f5b234f8))
* client tests have been restored [#1595](https://github.com/JanssenProject/jans/issues/1595); ([b43f3cd](https://github.com/JanssenProject/jans/commit/b43f3cdd0de271439fdc9996a18d9472b5de7126))
* client tests have been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([17ab35d](https://github.com/JanssenProject/jans/commit/17ab35d07b3ef1553b0bfb9ae5b1aba8f66e17df))
* client tests have been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([4c352df](https://github.com/JanssenProject/jans/commit/4c352df88a5bb2691648f5d3e2b912657a04be1f))
* client tests have been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([e705bc9](https://github.com/JanssenProject/jans/commit/e705bc9320691554a4fefc15b3b8fde7d31105ca))
* client tests have been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([c4d9c2e](https://github.com/JanssenProject/jans/commit/c4d9c2ebe1a30fc405a874b4fd197299fab1553c))
* codacy warnings ([0aca641](https://github.com/JanssenProject/jans/commit/0aca641bbb3c64d2fb757358d1e245107148d22e))
* codacy warnings ([1f5b246](https://github.com/JanssenProject/jans/commit/1f5b2466a814d29fe011135632e2835b31015e18))
* code reformatting as suggested ([a70ceda](https://github.com/JanssenProject/jans/commit/a70cedad59c6ae561aafebb9cd913d2e3b7faa57))
* copyrights have been added [#1595](https://github.com/JanssenProject/jans/issues/1595); ([72b4ad9](https://github.com/JanssenProject/jans/commit/72b4ad90449cd11ecd6e59143c48bd6ebdf470ed))
* copyrights have been added [#1595](https://github.com/JanssenProject/jans/issues/1595); ([b6f6ae5](https://github.com/JanssenProject/jans/commit/b6f6ae59573cf72c16775c58b84bf05655251083))
* corrected uma test failures ([12299fd](https://github.com/JanssenProject/jans/commit/12299fdbc6ff2acc9d61f770353fda0faf08ff9c))
* corrected uma test failures ([ff26824](https://github.com/JanssenProject/jans/commit/ff26824f6cf73067668c40521bb868ae8a58efe0))
* correction as suggested in review ([adddb1a](https://github.com/JanssenProject/jans/commit/adddb1a2df2af7c7b8a164a2a01958cfd040931f))
* data_provider, multiplying redundant calls of u test functions, listener has been added; ([fab2f75](https://github.com/JanssenProject/jans/commit/fab2f75e98c540f61825f8cfcd2bcd0b04237251))
* data_provider, multiplying redundant calls of u test functions, listener has been added; ([096b0ac](https://github.com/JanssenProject/jans/commit/096b0accf3abae9f9e6b9cda0945ec64a6dea517))
* doc_id for base ([#149](https://github.com/JanssenProject/jans/issues/149)) ([e7b4747](https://github.com/JanssenProject/jans/commit/e7b47474183fa56fa7c205995c95eecd1db5bd74))
* doc_id for base ([#149](https://github.com/JanssenProject/jans/issues/149)) ([eb0801e](https://github.com/JanssenProject/jans/commit/eb0801ec885fcbdcc9be4274c49abacd8a707b7c))
* early exit to avoid nested if(s) ([ab65ac9](https://github.com/JanssenProject/jans/commit/ab65ac9759032f03180a08621b638c2723113ecf))
* error has been added to the unit tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([53b4daa](https://github.com/JanssenProject/jans/commit/53b4daa493e0c424d1021a0327b0341cc0dda7ba))
* error has been added to the unit tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([ac35a91](https://github.com/JanssenProject/jans/commit/ac35a91b528f22939448183dbaabcf507da14681))
* error has been removed from unit tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([fdecdc2](https://github.com/JanssenProject/jans/commit/fdecdc2dc05bf5d6d844460b9ceabb66d9658465))
* error has been removed from unit tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([e7330d6](https://github.com/JanssenProject/jans/commit/e7330d6306734c5fd05f9d81ddffab3e485c3cb2))
* fail has been added (for testing) [#1595](https://github.com/JanssenProject/jans/issues/1595); ([e5b7161](https://github.com/JanssenProject/jans/commit/e5b71618749ffa03e50630b3720c5fdda631d1ea))
* fail has been added (for testing) [#1595](https://github.com/JanssenProject/jans/issues/1595); ([aef4f3a](https://github.com/JanssenProject/jans/commit/aef4f3aff8a2ab78e1b70a28bc93cf65f251a3cd))
* fail has been removed [#1595](https://github.com/JanssenProject/jans/issues/1595); ([cfa61e7](https://github.com/JanssenProject/jans/commit/cfa61e7ee908e3a9ca83d430cde2fae8f1ee0ccf))
* fail has been removed [#1595](https://github.com/JanssenProject/jans/issues/1595); ([fd65b1c](https://github.com/JanssenProject/jans/commit/fd65b1c2fb64b19a7f607d339d4d873f500f63c9))
* fix method to determine if Db is MariaDB ([edf5a8d](https://github.com/JanssenProject/jans/commit/edf5a8d91cba64b575e9c195bc682976b55dc51f))
* fix RDBS export entry and DeleteNotifier ([#864](https://github.com/JanssenProject/jans/issues/864)) ([ce5b2e6](https://github.com/JanssenProject/jans/commit/ce5b2e61d5ebaf0c81f7ad34459a635780da0c38))
* for JARM issue 310 311 and 314  ([ae0cdb9](https://github.com/JanssenProject/jans/commit/ae0cdb9845b144c7cc7d640b04c1240a2bfc41f4))
* gprcio bug in build error ([0ee6386](https://github.com/JanssenProject/jans/commit/0ee638635ea2dcbe14f0f3b1d2e538a9496afc9a))
* gprcio bug in build error ([664a4fe](https://github.com/JanssenProject/jans/commit/664a4fe4f611496e937428a0517f22aed1a564f4))
* **image:** update images ([#775](https://github.com/JanssenProject/jans/issues/775)) ([b31059c](https://github.com/JanssenProject/jans/commit/b31059c8ed1d895c023126bb39b1e5d390521c2c))
* import Nullable ([5057531](https://github.com/JanssenProject/jans/commit/5057531d4c1d936ecbcf06dc570bc78094f500e0))
* improving usage data_provider name; ([6c47925](https://github.com/JanssenProject/jans/commit/6c47925c058e46fdce349f12b188b3cb18f2f384))
* improving usage data_provider name; ([5acd2e2](https://github.com/JanssenProject/jans/commit/5acd2e2617697691b40370b317bd0f491ae003bd))
* **jans-auth-server:** check alg none to display error JARM issue310 ([#786](https://github.com/JanssenProject/jans/issues/786)) ([b21a052](https://github.com/JanssenProject/jans/commit/b21a05216ca90cea68c503247c69b12c4d8aaf87))
* **jans-auth-server:** corrected 500 error if absent redirect_uri in object for fapi ([89e586a](https://github.com/JanssenProject/jans/commit/89e586a039912757298d8936d09d515d7474e46a))
* **jans-auth-server:** corrected error code for absent redirect_uri in object (fapi) ([f73430c](https://github.com/JanssenProject/jans/commit/f73430cb5585563fa825c70a44385eea8867319e))
* **jans-auth-server:** corrected jarm error response ([1d4b53b](https://github.com/JanssenProject/jans/commit/1d4b53babb95c894f7dcb1eedb14c7e031edaa1f))
* **jans-auth-server:** corrected jarm isuue [#310](https://github.com/JanssenProject/jans/issues/310) ([#773](https://github.com/JanssenProject/jans/issues/773)) ([e1cdc19](https://github.com/JanssenProject/jans/commit/e1cdc1930523c19be51d9d2a39af467d48495c74))
* **jans-auth-server:** corrected jarm response mode ([9e3bf69](https://github.com/JanssenProject/jans/commit/9e3bf6970ab27e6f251dc738458dbf94d7cec545))
* **jans-auth-server:** corrected npe in jarm ([5cae544](https://github.com/JanssenProject/jans/commit/5cae544bb7ca4d03f7cf6f5700e3c39fbf075df3))
* **jans-auth-server:** corrected wrong expires_in ([428c5b3](https://github.com/JanssenProject/jans/commit/428c5b344362e3ed94865bd20be38ce73f94300e))
* **jans-auth-server:** covered one more case when consent is off ([8b59739](https://github.com/JanssenProject/jans/commit/8b59739090acad7516ced0a2fe1f37f148aa6788))
* **jans-auth-server:** don't fail registration without custom script ([#711](https://github.com/JanssenProject/jans/issues/711)) ([277be82](https://github.com/JanssenProject/jans/commit/277be82c4a0bf924e496a0c6e5f19ba52b6702e3))
* **jans-auth-server:** error code correction unregister redirect_uri ([#814](https://github.com/JanssenProject/jans/issues/814)) [#816](https://github.com/JanssenProject/jans/issues/816) ([fe4d6a0](https://github.com/JanssenProject/jans/commit/fe4d6a05474f0ce8e564778a9afed0b9451e8b49))
* **jans-auth-server:** fixed device authz tests ([8a952d7](https://github.com/JanssenProject/jans/commit/8a952d724f2d044de6019a6c4872caa55cb2b3c7))
* **jans-auth-server:** fixed error code during error response creation ([0d47490](https://github.com/JanssenProject/jans/commit/0d4749025e2c39c521de883ec1f6c9ce8a90b55d))
* **jans-auth-server:** for issue[#315](https://github.com/JanssenProject/jans/issues/315) JARM registered redirect uri ([#752](https://github.com/JanssenProject/jans/issues/752)) ([fe2dc59](https://github.com/JanssenProject/jans/commit/fe2dc59232f4ac302aec71bff640e909e8a5b74a))
* **jans-auth-server:** if consent is off then check whether response already have access_tokne ([81ad31b](https://github.com/JanssenProject/jans/commit/81ad31b6058907830adeac4183ddb70a38110b7a))
* **jans-auth-server:** if consent is off then check whether response already have code ([294bb22](https://github.com/JanssenProject/jans/commit/294bb22616e4f422cbd878e1da51cf07a5a2d44f))
* **jans-auth-server:** jarm failing tests ([#745](https://github.com/JanssenProject/jans/issues/745)) ([5d0b401](https://github.com/JanssenProject/jans/commit/5d0b4016d5fc10ec8cda8fa6b2948cb05dbf576e))
* **jans-auth-server:** jarm tests fix ([ddf3423](https://github.com/JanssenProject/jans/commit/ddf3423a3ecbe12bc574cf048e50832edba4c15e))
* **jans-auth-server:** made tknCde consistency=true for UmaRPT ([298a35a](https://github.com/JanssenProject/jans/commit/298a35a8480a075d1e069ba896687e66c3973d80))
* **jans-auth-server:** made tknCde consistency=true for UmaRPT ([0554882](https://github.com/JanssenProject/jans/commit/0554882d1af77961f8afdd72c630ccaf30d2b87a))
* **jans-auth-server:** set par expiration to request object exp [#824](https://github.com/JanssenProject/jans/issues/824) ([#860](https://github.com/JanssenProject/jans/issues/860)) ([c835c38](https://github.com/JanssenProject/jans/commit/c835c38364bdc1ff4a7eb154ca58c07019b19c22))
* jans-ce-setup monorepo tweaks ([36c2d0b](https://github.com/JanssenProject/jans/commit/36c2d0b219dd4aa4485b3f64b1f1fdd0cee0fcf8))
* jans-ce-setup: add npm run plugin:clean to admin-ui setup ([70f01bf](https://github.com/JanssenProject/jans/commit/70f01bf1b9480565a6fde953791f1ba3e2d7da83))
* jans-cli sync swagger file from jans-config-api ([#759](https://github.com/JanssenProject/jans/issues/759)) ([315c699](https://github.com/JanssenProject/jans/commit/315c699a84593e7d621a1a3740b053e361133ed4))
* jans-client-api/server/pom.xml to reduce vulnerabilities ([89756bb](https://github.com/JanssenProject/jans/commit/89756bb35beb154eabe5760e611f23c3b44a4d79))
* **jans-client-api:** corrected test cases configuration [#724](https://github.com/JanssenProject/jans/issues/724) ([#726](https://github.com/JanssenProject/jans/issues/726)) ([f98db00](https://github.com/JanssenProject/jans/commit/f98db0061ccb5d61f70e52e065861e33d1958e9f))
* **jans-cli:** jans cli pkg fixes ([#854](https://github.com/JanssenProject/jans/issues/854)) ([9e96e4c](https://github.com/JanssenProject/jans/commit/9e96e4c6b13bc44f4bb2d74222da1669d5b5ed22))
* **jans-cli:** retain scim client in config.ini ([#872](https://github.com/JanssenProject/jans/issues/872)) ([8346517](https://github.com/JanssenProject/jans/commit/83465172bf11ea0a787ee3de34c8dd8968bcdcf0))
* **jans-config-api:** excluded test from execution ([#760](https://github.com/JanssenProject/jans/issues/760)) ([3af6672](https://github.com/JanssenProject/jans/commit/3af6672401f9d7782b2fc13e5bf67f763b58e9b4))
* **jans-config-api:** license validity period should be read only [#731](https://github.com/JanssenProject/jans/issues/731) ([f88095b](https://github.com/JanssenProject/jans/commit/f88095b1f52f0639221e4109ed7262099e06d0e9))
* **jans-config-api:** multiple custom lib not working  ([#907](https://github.com/JanssenProject/jans/issues/907)) ([9ef6fa4](https://github.com/JanssenProject/jans/commit/9ef6fa49afe0efb64ee87aa2485f95a7716e4259))
* **jans-config-api:** sql configuration endpoints are not found [#793](https://github.com/JanssenProject/jans/issues/793) ([#794](https://github.com/JanssenProject/jans/issues/794)) ([d8f2ea9](https://github.com/JanssenProject/jans/commit/d8f2ea949aa7735c3d236a5685d30c4085f7892c))
* **jans-fido2:** use diamond operator ([#764](https://github.com/JanssenProject/jans/issues/764)) ([5950a26](https://github.com/JanssenProject/jans/commit/5950a26f5c5dcf1731224d4ec56f7a5191a13d3d))
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
* JARM tests fix ([3bfb95f](https://github.com/JanssenProject/jans/commit/3bfb95f82108061da9ce2154fdb45b25af752b02))
* license validity period should be read only [#731](https://github.com/JanssenProject/jans/issues/731) ([#746](https://github.com/JanssenProject/jans/issues/746)) ([73931f5](https://github.com/JanssenProject/jans/commit/73931f56d9bd216f172008ec3c3a713e40cb4645))
* linux-setup apache config file name ([#719](https://github.com/JanssenProject/jans/issues/719)) ([46ce0ae](https://github.com/JanssenProject/jans/commit/46ce0ae4ca602392b90041c39415ffda7b027029))
* linux-setup mariadb json data types ([#714](https://github.com/JanssenProject/jans/issues/714)) ([4c21be2](https://github.com/JanssenProject/jans/commit/4c21be25abe3101e91365cc2cec1d52f687a824e))
* linux-setup suse httpd configuration ([#734](https://github.com/JanssenProject/jans/issues/734)) ([7767b5e](https://github.com/JanssenProject/jans/commit/7767b5e717a3fd11f7ff54fbec7ad11d6e9df8aa))
* listener class has been fixed [#1595](https://github.com/JanssenProject/jans/issues/1595); ([af141a3](https://github.com/JanssenProject/jans/commit/af141a36c8aa67d952eb5678518c1b11b5152f89))
* listener class has been fixed [#1595](https://github.com/JanssenProject/jans/issues/1595); ([8c7e0c3](https://github.com/JanssenProject/jans/commit/8c7e0c3f7ef5db8c6b8b4b90bfaf13f8d7aa8b96))
* listener has been renamed; ([d4cb3a7](https://github.com/JanssenProject/jans/commit/d4cb3a7b0bf11f9e7dde6a2b5b0f274d988849bb))
* listener has been renamed; ([7f283f3](https://github.com/JanssenProject/jans/commit/7f283f31fc8947233810e8c0dc7431eb9930d985))
* listener has been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([527d1c2](https://github.com/JanssenProject/jans/commit/527d1c2cd0a8c218d4f8b6f9c4d17d96d87f00b1))
* listener has been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([03bac01](https://github.com/JanssenProject/jans/commit/03bac01d056adfe23ee3f3d7fb884099de1613e1))
* listener has been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([c31cf42](https://github.com/JanssenProject/jans/commit/c31cf42a954d71e11d43c2f879e152f06702db4d))
* listener has been updated [#1595](https://github.com/JanssenProject/jans/issues/1595); ([21b8f56](https://github.com/JanssenProject/jans/commit/21b8f569cac0d812172127985227f256f610cfd7))
* listener has been updated; ([f3cbc35](https://github.com/JanssenProject/jans/commit/f3cbc350cd89cef14ae9de022efccc9cafdb1add))
* listener has been updated; ([0673a15](https://github.com/JanssenProject/jans/commit/0673a150ac32fbb6a8c313c50a45c45180c17f78))
* newly added eddsa cause exception ([#727](https://github.com/JanssenProject/jans/issues/727)) ([6e5a865](https://github.com/JanssenProject/jans/commit/6e5a865d6c204240424710be8a496a5b513d647a))
* **pycloudlib:** missing tar option to not restore file timestamp [#613](https://github.com/JanssenProject/jans/issues/613) ([#627](https://github.com/JanssenProject/jans/issues/627)) ([d19fbfd](https://github.com/JanssenProject/jans/commit/d19fbfd6891d03fb0c76073dfa8ba2ffc44a3b9b))
* remove remote theme ([bcca289](https://github.com/JanssenProject/jans/commit/bcca28975f526026da72bcd0af644cd2cda72e1c))
* reorder java modifiers ([#750](https://github.com/JanssenProject/jans/issues/750)) ([e5401b2](https://github.com/JanssenProject/jans/commit/e5401b24c0d31d5c73933c9f6c32284775192489))
* replace non UTF-8 characters ([#770](https://github.com/JanssenProject/jans/issues/770)) ([bb386cd](https://github.com/JanssenProject/jans/commit/bb386cdd3d188c3bb68a2874eab92b88ad1deaeb))
* temp removing client tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([a04fa23](https://github.com/JanssenProject/jans/commit/a04fa23f76e01263f4d0bdded4afabecbc010a68))
* temp removing client tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([b6b965f](https://github.com/JanssenProject/jans/commit/b6b965f70051127cac1bfcdc90c4d4f9ab571f30))
* temp removing client tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([494f788](https://github.com/JanssenProject/jans/commit/494f788b66aacd0847855f0e292dd2192d9fb45e))
* temp removing client tests [#1595](https://github.com/JanssenProject/jans/issues/1595); ([8261a1f](https://github.com/JanssenProject/jans/commit/8261a1fc3742a818b0b83e292329171f5987129d))
* update admin ui properties ([#778](https://github.com/JanssenProject/jans/issues/778)) ([2052d02](https://github.com/JanssenProject/jans/commit/2052d0229bad9bf6e4e2542731b0627367fa5469))
* update config github pages ([#771](https://github.com/JanssenProject/jans/issues/771)) ([5c5b979](https://github.com/JanssenProject/jans/commit/5c5b979f015fc8b9c4550e0bb3f47419252a8fe4))
* update scripts ([#765](https://github.com/JanssenProject/jans/issues/765)) ([8b9aaca](https://github.com/JanssenProject/jans/commit/8b9aaca64e83a17b81498ea6e99523ff7f0f311b))
* update wrong import [#905](https://github.com/JanssenProject/jans/issues/905) ([#906](https://github.com/JanssenProject/jans/issues/906)) ([af55a81](https://github.com/JanssenProject/jans/commit/af55a81f784191c1fcee5ff2fade499f778561c3))
* upgrade commons-codec:commons-codec from 1.7 to 20041127.091804 ([3d319b8](https://github.com/JanssenProject/jans/commit/3d319b87ee0b3045ed6f2e7b268a55b916a4d000))
* upgrade oauth.signpost:signpost-commonshttp4 from 2.0.0 to 2.1.1 ([7246e8f](https://github.com/JanssenProject/jans/commit/7246e8fbf530c5ea16b6ef46081dfc3da17ef30a))
* upgrade org.apache.httpcomponents:httpcore from 4.4.5 to 4.4.15 ([82689d2](https://github.com/JanssenProject/jans/commit/82689d2470ad7a9fa6831ccf1aaa6f5145712d7f))
* upgrade org.bitbucket.b_c:jose4j from 0.6.4 to 0.7.9 ([874e2ad](https://github.com/JanssenProject/jans/commit/874e2ad2aaaf8361b0c9c5359279a455eaa0fd34))
* upgrade org.codehaus.jettison:jettison from 1.3.2 to 1.4.1 ([5ffe19d](https://github.com/JanssenProject/jans/commit/5ffe19da39385d6572b27d0553cd7e3e2fb57557))
* use diamond operator ([#766](https://github.com/JanssenProject/jans/issues/766)) ([57664b0](https://github.com/JanssenProject/jans/commit/57664b0c0fd5926b2986f6b6d738d909e4865bca))
* vm setup suse fixes ([#705](https://github.com/JanssenProject/jans/issues/705)) ([2f69a8a](https://github.com/JanssenProject/jans/commit/2f69a8a90747e6c0b67eb76f66edbf3166019264))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))
