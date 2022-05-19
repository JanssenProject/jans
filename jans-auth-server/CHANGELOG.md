# Changelog

## 1.0.0 (2022-05-19)


### Features

* add script for Google login ([#1141](https://github.com/JanssenProject/jans/issues/1141)) ([bac9144](https://github.com/JanssenProject/jans/commit/bac9144ad8a5f8f2b378aa67663caab9f19f052b))
* create apis to verify and save license api-keys in Admin UI [#1196](https://github.com/JanssenProject/jans/issues/1196) ([#1203](https://github.com/JanssenProject/jans/issues/1203)) ([315faec](https://github.com/JanssenProject/jans/commit/315faeca46045716d8aa38fa5448c7581a5e4212))
* **jans-auth-server:** [#808](https://github.com/JanssenProject/jans/issues/808) sign-in with apple interception script ([c21183a](https://github.com/JanssenProject/jans/commit/c21183ab6331f95531d76c6d279646cc3c0b600e))
* **jans-auth-server:** adapted authorization ws to use authzrequest ([58c5336](https://github.com/JanssenProject/jans/commit/58c5336fe4978c3709d060cd46f1847c01782af3))
* **jans-auth-server:** added authzrequest abstraction ([af8faf0](https://github.com/JanssenProject/jans/commit/af8faf008eec21a952c3d474169e57a9aece9152))
* **jans-auth-server:** authorized acr values ([#1068](https://github.com/JanssenProject/jans/issues/1068)) ([26e576a](https://github.com/JanssenProject/jans/commit/26e576a5be90ac9597ed37e0d3629a2701008fcf))
* **jans-auth-server:** changed prog lang name python->jython ([b9ba291](https://github.com/JanssenProject/jans/commit/b9ba291e576b8443f37c774088747bab09db2db9))
* **jans-auth-server:** client registration language metadata ([#1237](https://github.com/JanssenProject/jans/issues/1237)) ([a8d0157](https://github.com/JanssenProject/jans/commit/a8d0157b0a8664e5e4d58a9524a0fa20df324381))
* **jans-auth-server:** enable person authn script to have multiple acr names ([#1074](https://github.com/JanssenProject/jans/issues/1074)) ([1dc9250](https://github.com/JanssenProject/jans/commit/1dc9250b9140cfe2a7ea3daff6c9e0d6383c4bce))
* **jans-auth-server:** force signed request object ([#1052](https://github.com/JanssenProject/jans/issues/1052)) ([28ebbc1](https://github.com/JanssenProject/jans/commit/28ebbc10d545ad69ceb4e9a625fbbf13e6360b75))
* **jans-auth-server:** hide 302 redirect exception in logs [#1294](https://github.com/JanssenProject/jans/issues/1294) ([00197c7](https://github.com/JanssenProject/jans/commit/00197c720b444e50c84f49f696fd14768f8fdb08))
* **jans-auth,jans-cli,jans-config-api:** changes to handle new attribute description in Client object and new custom script type ([d4a9f15](https://github.com/JanssenProject/jans/commit/d4a9f15c3244961cfef6e3229c2e2e49cf85ba0d))
* **jans-config-api:** user mgmt endpoint ([a093758](https://github.com/JanssenProject/jans/commit/a0937580eed7c32a0f8bf573bddb9ac8b7080e2c))
* **jans-config-api:** user mgmt endpoint ([0ea10fd](https://github.com/JanssenProject/jans/commit/0ea10fd10fdd82ea2f170ecfa990c494591ba653))
* **jans-core:** compile java code on the fly for custom script ([5da6e27](https://github.com/JanssenProject/jans/commit/5da6e2743761cbdf8f06b3dca9a5cf7c8af1abe3))
* **jans-core:** remove UPDATE_USER and USER_REGISTRATION scripts [#1289](https://github.com/JanssenProject/jans/issues/1289) ([c34e75d](https://github.com/JanssenProject/jans/commit/c34e75d49db89999633249376c7b42c41bb1ce24))
* **jans:** jetty 11 integration ([#1123](https://github.com/JanssenProject/jans/issues/1123)) ([6c1caa1](https://github.com/JanssenProject/jans/commit/6c1caa1c4c92d28571f8589cd701e6885d4d85ef))
* support regex client attribute to validate redirect uris ([#1005](https://github.com/JanssenProject/jans/issues/1005)) ([a78ee1a](https://github.com/JanssenProject/jans/commit/a78ee1a3cfc4e7a6d08a500750edb5db0f7709a4))


### Bug Fixes

* **admin-ui:** the backend issues related to jetty 11 migration [#1258](https://github.com/JanssenProject/jans/issues/1258) ([#1259](https://github.com/JanssenProject/jans/issues/1259)) ([d61be0b](https://github.com/JanssenProject/jans/commit/d61be0bf633020c6bd989e603bb983dc7a45b78b))
* bug(jans-auth-server): custom pages are not found [#1318](https://github.com/JanssenProject/jans/issues/1318) ([e1e0bf9](https://github.com/JanssenProject/jans/commit/e1e0bf943f35906430b0fae5333f3b76f05734c3))
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
* **jans-auth-server:** validate pkce after extraction data from request object ([#999](https://github.com/JanssenProject/jans/issues/999)) ([29fdfae](https://github.com/JanssenProject/jans/commit/29fdfae276b61890ed345804827aa83437acd428))
* **jans-auth-server:** validate redirect_uri blank and client redirect uris single item to return by default ([#1046](https://github.com/JanssenProject/jans/issues/1046)) ([aa139e4](https://github.com/JanssenProject/jans/commit/aa139e46e6d25c6135eb05e22dbc36fe84eb3e86))
* **jans-core:** corrected ExternalUmaClaimsGatheringService ([cfe1b6d](https://github.com/JanssenProject/jans/commit/cfe1b6d0eae75a699fc0505fea46e955a3480b57))
* Typo httpLoggingExludePaths jans-auth-server jans-cli jans-config-api jans-linux-setup docker-jans-persistence-loader ([47a20ee](https://github.com/JanssenProject/jans/commit/47a20eefa781d1ca07a9aa30a5adcde3793076d1))
* update mysql/spanner mappings [#1053](https://github.com/JanssenProject/jans/issues/1053) ([94fb2c6](https://github.com/JanssenProject/jans/commit/94fb2c6d0f5de061eca515c003be679f35757faa))
* Use highest level script in case ACR script is not found. Added FF to keep existing behavior. ([#1070](https://github.com/JanssenProject/jans/issues/1070)) ([07473d9](https://github.com/JanssenProject/jans/commit/07473d9a8c3e31f6a75670a874e17341518bf0be))


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

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/jans-auth-server-v1.0.0-beta.15...jans-auth-server-v1.0.0-beta.16) (2022-03-14)


### Features

* **jans-auth-server:** forbid plain pkce if fapi=true (fapi1-advanced-final-par-plain-pkce-rejected fail) [#946](https://github.com/JanssenProject/jans/issues/946) ([21cecb0](https://github.com/JanssenProject/jans/commit/21cecb04909a9b69da5da3a206c83ca52c9e2c8b))
* **jans-auth-server:** new client config option defaultpromptlogin [#979](https://github.com/JanssenProject/jans/issues/979) ([4e3de26](https://github.com/JanssenProject/jans/commit/4e3de2627f676d35186877a8570de6ce8950ec57))
* support regex client attribute to validate redirect uris ([#1005](https://github.com/JanssenProject/jans/issues/1005)) ([a78ee1a](https://github.com/JanssenProject/jans/commit/a78ee1a3cfc4e7a6d08a500750edb5db0f7709a4))


### Bug Fixes

* **jans-auth-server:** corrected ParValidatorTest [#946](https://github.com/JanssenProject/jans/issues/946) ([04a01fd](https://github.com/JanssenProject/jans/commit/04a01fd43e1969bc09494b2f08387bcc7d502ed7))
* **jans-auth-server:** corrected sonar reported issue ([7c88078](https://github.com/JanssenProject/jans/commit/7c8807820a217f33c66f496b05863e9d77d8c7e8))
* **jans-auth-server:** fix npe ([e6debb2](https://github.com/JanssenProject/jans/commit/e6debb24ea0ea1963290b543d74df7f0761efe3b))
* **jans-auth-server:** reduce noise in logs when session can't be found ([47afc47](https://github.com/JanssenProject/jans/commit/47afc47a239c48c090591d9fa561757e7749d96d))
* **jans-auth-server:** removed reference of removed tests [#996](https://github.com/JanssenProject/jans/issues/996) ([cabc4f2](https://github.com/JanssenProject/jans/commit/cabc4f2f6119e2aff0440fdb1bb4dd1f11dce2cd))
* **jans-auth-server:** validate pkce after extraction data from request object ([#999](https://github.com/JanssenProject/jans/issues/999)) ([29fdfae](https://github.com/JanssenProject/jans/commit/29fdfae276b61890ed345804827aa83437acd428))


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
* **jans-auth-server:** reject par without pkce for fapi ([332df41](https://github.com/JanssenProject/jans/commit/332df4168894220b5ea5acdf84d8977d8f8f161d))
* **jans-auth-server:** set public subject identifier per client ([#800](https://github.com/JanssenProject/jans/issues/800)) ([c303bbc](https://github.com/JanssenProject/jans/commit/c303bbc4c928b32144a657b9c119846ed29cd522))
* **jans-auth-server:** turn off consent for pairwise openid-only scope ([#708](https://github.com/JanssenProject/jans/issues/708)) ([a96007d](https://github.com/JanssenProject/jans/commit/a96007d03566a84bd1cffb90956451f5e3ad5b9f))
* **jans-config-api:** add deletable flag to admin-ui role object [#888](https://github.com/JanssenProject/jans/issues/888) ([#900](https://github.com/JanssenProject/jans/issues/900)) ([500a773](https://github.com/JanssenProject/jans/commit/500a77358ad6d811fc95de3a13829d6f983bc1b0))
* par should be able to register with nbf ([a4a2981](https://github.com/JanssenProject/jans/commit/a4a29817b9629baaff2aac9181b4483ae95f3447))


### Bug Fixes

* brazilob jarm fapi conformance test last7 issues ([#695](https://github.com/JanssenProject/jans/issues/695)) ([edab074](https://github.com/JanssenProject/jans/commit/edab0746e7febb12546c58a74500d815a71d94b2))
* code reformatting as suggested ([a70ceda](https://github.com/JanssenProject/jans/commit/a70cedad59c6ae561aafebb9cd913d2e3b7faa57))
* correction as suggested in review ([adddb1a](https://github.com/JanssenProject/jans/commit/adddb1a2df2af7c7b8a164a2a01958cfd040931f))
* early exit to avoid nested if(s) ([ab65ac9](https://github.com/JanssenProject/jans/commit/ab65ac9759032f03180a08621b638c2723113ecf))
* for JARM issue 310 311 and 314  ([ae0cdb9](https://github.com/JanssenProject/jans/commit/ae0cdb9845b144c7cc7d640b04c1240a2bfc41f4))
* import Nullable ([5057531](https://github.com/JanssenProject/jans/commit/5057531d4c1d936ecbcf06dc570bc78094f500e0))
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
* **jans-auth-server:** set par expiration to request object exp [#824](https://github.com/JanssenProject/jans/issues/824) ([#860](https://github.com/JanssenProject/jans/issues/860)) ([c835c38](https://github.com/JanssenProject/jans/commit/c835c38364bdc1ff4a7eb154ca58c07019b19c22))
* JARM tests fix ([3bfb95f](https://github.com/JanssenProject/jans/commit/3bfb95f82108061da9ce2154fdb45b25af752b02))
* newly added eddsa cause exception ([#727](https://github.com/JanssenProject/jans/issues/727)) ([6e5a865](https://github.com/JanssenProject/jans/commit/6e5a865d6c204240424710be8a496a5b513d647a))
* replace non UTF-8 characters ([#770](https://github.com/JanssenProject/jans/issues/770)) ([bb386cd](https://github.com/JanssenProject/jans/commit/bb386cdd3d188c3bb68a2874eab92b88ad1deaeb))
* upgrade commons-codec:commons-codec from 1.7 to 20041127.091804 ([3d319b8](https://github.com/JanssenProject/jans/commit/3d319b87ee0b3045ed6f2e7b268a55b916a4d000))
* upgrade oauth.signpost:signpost-commonshttp4 from 2.0.0 to 2.1.1 ([7246e8f](https://github.com/JanssenProject/jans/commit/7246e8fbf530c5ea16b6ef46081dfc3da17ef30a))
* upgrade org.apache.httpcomponents:httpcore from 4.4.5 to 4.4.15 ([82689d2](https://github.com/JanssenProject/jans/commit/82689d2470ad7a9fa6831ccf1aaa6f5145712d7f))
* upgrade org.bitbucket.b_c:jose4j from 0.6.4 to 0.7.9 ([874e2ad](https://github.com/JanssenProject/jans/commit/874e2ad2aaaf8361b0c9c5359279a455eaa0fd34))
* upgrade org.codehaus.jettison:jettison from 1.3.2 to 1.4.1 ([5ffe19d](https://github.com/JanssenProject/jans/commit/5ffe19da39385d6572b27d0553cd7e3e2fb57557))
* use diamond operator ([#766](https://github.com/JanssenProject/jans/issues/766)) ([57664b0](https://github.com/JanssenProject/jans/commit/57664b0c0fd5926b2986f6b6d738d909e4865bca))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))
