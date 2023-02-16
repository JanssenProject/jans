# Changelog

## 1.0.6 (2023-01-09)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))
* **agama:** deploy flows from .gama files ([#3250](https://github.com/JanssenProject/jans/issues/3250)) ([df14f8a](https://github.com/JanssenProject/jans/commit/df14f8aee022ae14746af6ebd15dbca9622a4086))
* changes in admin-ui plugin to allow agama-developer-studio to use its OAuth2 apis [#3085](https://github.com/JanssenProject/jans/issues/3085) ([#3298](https://github.com/JanssenProject/jans/issues/3298)) ([9e9a7bd](https://github.com/JanssenProject/jans/commit/9e9a7bd17c9b7238b7e65359ffdd5f6b0474e9d1))
* **config-api:** audit log, agama ADS spec, fix for 0 index search ([#3369](https://github.com/JanssenProject/jans/issues/3369)) ([ea04e2c](https://github.com/JanssenProject/jans/commit/ea04e2ce5d83d4840638cd2e137fcbc67ee69c81))
* documentation for ssa and remove softwareRoles query param of get ssa ([#3031](https://github.com/JanssenProject/jans/issues/3031)) ([d8e14eb](https://github.com/JanssenProject/jans/commit/d8e14ebbeee357c8c2c31808243cf82933ae4a9b))
* **jans-auth-server:** added ability to return error out of introspection and update_token custom script [#3255](https://github.com/JanssenProject/jans/issues/3255) ([#3356](https://github.com/JanssenProject/jans/issues/3356)) ([a3e5227](https://github.com/JanssenProject/jans/commit/a3e522745a28fddb3cb6677a553350868fcbaa45))
* **jans-auth-server:** added externalUriWhiteList configuration property before call external uri from AS [#3130](https://github.com/JanssenProject/jans/issues/3130) ([#3425](https://github.com/JanssenProject/jans/issues/3425)) ([6c7df6f](https://github.com/JanssenProject/jans/commit/6c7df6fc955812599a49937f98a6746d05b0badf))
* **jans-auth-server:** added token exchange support to client [#2518](https://github.com/JanssenProject/jans/issues/2518) ([#2855](https://github.com/JanssenProject/jans/issues/2855)) ([943d99f](https://github.com/JanssenProject/jans/commit/943d99f2784e671d361c66c1ddb82c10f567a698))
* **jans-auth-server:** avoid compilation problem when version is flipped in test code [#3148](https://github.com/JanssenProject/jans/issues/3148) ([#3210](https://github.com/JanssenProject/jans/issues/3210)) ([4d61c7b](https://github.com/JanssenProject/jans/commit/4d61c7b1c5be70acd855f68ff51123342ac94490))
* **jans-auth-server:** block authentication flow originating from a webview ([#3204](https://github.com/JanssenProject/jans/issues/3204)) ([e48380e](https://github.com/JanssenProject/jans/commit/e48380e68653cd4bd25ec2265225e4900e20bec1))
* **jans-auth-server:** check offline_access implementation has all conditions defined in spec [#1945](https://github.com/JanssenProject/jans/issues/1945) ([#3004](https://github.com/JanssenProject/jans/issues/3004)) ([af30e4c](https://github.com/JanssenProject/jans/commit/af30e4c438372fffb7a3ac78a6aea5988af43d5f))
* **jans-auth-server:** corrected GluuOrganization - refactor getOrganizationName() [#2947](https://github.com/JanssenProject/jans/issues/2947) ([#2948](https://github.com/JanssenProject/jans/issues/2948)) ([9275576](https://github.com/JanssenProject/jans/commit/9275576ed0f925fcd3dbaf06e155e7185c797015))
* **jans-auth-server:** draft for - improve dcr / ssa validation for dynamic  registration [#2980](https://github.com/JanssenProject/jans/issues/2980) ([#3109](https://github.com/JanssenProject/jans/issues/3109)) ([233a78c](https://github.com/JanssenProject/jans/commit/233a78c8e48fb8de353629bc16fc6af1d80fb910))
* **jans-auth-server:** end session - if id_token is expired but signature is correct, we should make attempt to look up session by "sid" claim [#3231](https://github.com/JanssenProject/jans/issues/3231) ([#3291](https://github.com/JanssenProject/jans/issues/3291)) ([cd11750](https://github.com/JanssenProject/jans/commit/cd11750c064e4f18d7df759f8271338a7d079ad0))
* **jans-auth-server:** implemented auth server config property to disable prompt=login [#3006](https://github.com/JanssenProject/jans/issues/3006) ([#3522](https://github.com/JanssenProject/jans/issues/3522)) ([0233cd1](https://github.com/JanssenProject/jans/commit/0233cd161f07e793c9565d40338078b09d2c12c3))
* **jans-auth-server:** java docs for ssa ([#2995](https://github.com/JanssenProject/jans/issues/2995)) ([892b87a](https://github.com/JanssenProject/jans/commit/892b87a2af5fa82ba4f5dceb38baba28e2029182))
* **jans-auth-server:** new configuration for userinfo has been added ([#3349](https://github.com/JanssenProject/jans/issues/3349)) ([3ccc4a9](https://github.com/JanssenProject/jans/commit/3ccc4a9ad8486a0795d733bf8961999bad319438))
* **jans-auth-server:** remove ox properties name ([#3285](https://github.com/JanssenProject/jans/issues/3285)) ([f70b207](https://github.com/JanssenProject/jans/commit/f70b207ecff565ff53e3efb13d897937d9aeaee0))
* **jans-auth-server:** remove redirect uri on client registration when grant types is password or client credentials ([#3076](https://github.com/JanssenProject/jans/issues/3076)) ([cd876b4](https://github.com/JanssenProject/jans/commit/cd876b46e6bbdec865f5cd1cfe40c2f3b2ca293c))
* **jans-auth-server:** renamed "code"-&gt;"random" uniqueness claims of id_token to avoid confusion with Authorization Code Flow [#3466](https://github.com/JanssenProject/jans/issues/3466) ([#3467](https://github.com/JanssenProject/jans/issues/3467)) ([dd9d049](https://github.com/JanssenProject/jans/commit/dd9d049d67bdd608dd3aea33c301817dd4cb0d8c))
* **jans-auth-server:** specify minimum acr for clients [#343](https://github.com/JanssenProject/jans/issues/343) ([#3083](https://github.com/JanssenProject/jans/issues/3083)) ([b0034ec](https://github.com/JanssenProject/jans/commit/b0034ec509ace1a4e30a7e9c6dd23dca48178c62))
* **jans-auth-server:** ssa validation endpoint ([#2842](https://github.com/JanssenProject/jans/issues/2842)) ([de8a86e](https://github.com/JanssenProject/jans/commit/de8a86ed1eb29bd02546e9e22fc6f668ac3217c4))
* **jans-auth-server:** swagger docs for ssa ([#2953](https://github.com/JanssenProject/jans/issues/2953)) ([7f93bca](https://github.com/JanssenProject/jans/commit/7f93bca9ff101d85f1ae389602f99c7c6af9bc17))
* **jans-auth-server:** updated mau on refreshing access token [#2955](https://github.com/JanssenProject/jans/issues/2955) ([#3025](https://github.com/JanssenProject/jans/issues/3025)) ([56de619](https://github.com/JanssenProject/jans/commit/56de61974ae0d2a3d8382191c2aae479a062e9b2))
* ssa revoke endpoint ([#2865](https://github.com/JanssenProject/jans/issues/2865)) ([9c68f91](https://github.com/JanssenProject/jans/commit/9c68f914e155de492e54121033c8f0ed45d66817))


### Bug Fixes

* (jans-auth-server): fixed Client serialization/deserialization issue [#2946](https://github.com/JanssenProject/jans/issues/2946) ([#3064](https://github.com/JanssenProject/jans/issues/3064)) ([31b5bfc](https://github.com/JanssenProject/jans/commit/31b5bfc2d626a94998c6e0a1d9121579858437e3))
* (jans-auth-server): fixed client's sortby [#3075](https://github.com/JanssenProject/jans/issues/3075) ([#3079](https://github.com/JanssenProject/jans/issues/3079)) ([e6b0e58](https://github.com/JanssenProject/jans/commit/e6b0e58c7336c2c6537fb55557527abe09ab0811))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - removing inwebo ([#2975](https://github.com/JanssenProject/jans/issues/2975)) ([052f91f](https://github.com/JanssenProject/jans/commit/052f91fd45c888efb7480fc7cd403dc005ceca23))
* **agama:** after moving agama to jans-auth-server agama model tests are not run [#3246](https://github.com/JanssenProject/jans/issues/3246) ([#3247](https://github.com/JanssenProject/jans/issues/3247)) ([9887e23](https://github.com/JanssenProject/jans/commit/9887e2333a4482100f28ccf448f99e07059490ac))
* **agama:** fix agama auth dependency which blocks build process [#3149](https://github.com/JanssenProject/jans/issues/3149) ([#3244](https://github.com/JanssenProject/jans/issues/3244)) ([8f9fee3](https://github.com/JanssenProject/jans/commit/8f9fee31c66ce08046258694e5e2d83a31e38b5d))
* **agama:** fixing tests run on jenkins [#3149](https://github.com/JanssenProject/jans/issues/3149) ([#3261](https://github.com/JanssenProject/jans/issues/3261)) ([cc6c5e1](https://github.com/JanssenProject/jans/commit/cc6c5e12f5deb17a5c0353fc765a50d1603c74a1))
* catch org.eclipse.jetty.http.BadMessageException: in ([#3330](https://github.com/JanssenProject/jans/issues/3330)) ([1e0ff76](https://github.com/JanssenProject/jans/commit/1e0ff760651f5e3cd25044566835dbd20d4ab2c3)), closes [#3329](https://github.com/JanssenProject/jans/issues/3329)
* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* jans-auth-server/pom.xml to reduce vulnerabilities ([#3314](https://github.com/JanssenProject/jans/issues/3314)) ([f3e8205](https://github.com/JanssenProject/jans/commit/f3e82051bcd47346986ba250b169a0cf5684b4ec))
* **jans-auth-server:** changed getAttributeValues to getAttributeObjectValues ([#3346](https://github.com/JanssenProject/jans/issues/3346)) ([a39b61e](https://github.com/JanssenProject/jans/commit/a39b61e6e686680e2b45b10e25b36fa41a4de76a))
* **jans-auth-server:** compilation error of server side tests [#3363](https://github.com/JanssenProject/jans/issues/3363) ([#3364](https://github.com/JanssenProject/jans/issues/3364)) ([e83c087](https://github.com/JanssenProject/jans/commit/e83c087a168367ef146b1e42a75d7325da05b695))
* **jans-auth-server:** corrected keys description "id_token &lt;purpose&gt;" -> "Connect <purpose>" [#3415](https://github.com/JanssenProject/jans/issues/3415) ([#3560](https://github.com/JanssenProject/jans/issues/3560)) ([75f99bd](https://github.com/JanssenProject/jans/commit/75f99bdf2bb676e607b86a71cf4b00a2e51ba251))
* **jans-auth-server:** corrected regression made in token request [#2921](https://github.com/JanssenProject/jans/issues/2921) ([#2922](https://github.com/JanssenProject/jans/issues/2922)) ([deeae74](https://github.com/JanssenProject/jans/commit/deeae748aa465e3789114a93eee251628f9d365b))
* **jans-auth-server:** Duplicate iss and aud on introspection as jwt [#3366](https://github.com/JanssenProject/jans/issues/3366) ([#3387](https://github.com/JanssenProject/jans/issues/3387)) ([8780e94](https://github.com/JanssenProject/jans/commit/8780e944f120a7f0d8edfb329e31f44a9b99d94a))
* **jans-auth-server:** fix language metadata format ([#2883](https://github.com/JanssenProject/jans/issues/2883)) ([e21e206](https://github.com/JanssenProject/jans/commit/e21e206df16b048b1743c3ee441d9fbdb1f8c67e))
* **jans-auth-server:** native sso - return device secret if device_sso scope is present [#2790](https://github.com/JanssenProject/jans/issues/2790) ([#2791](https://github.com/JanssenProject/jans/issues/2791)) ([9fa213f](https://github.com/JanssenProject/jans/commit/9fa213f12d4b2bafa399fb03ca207f692c44e01f))
* **jans-auth-server:** parse string from object ([#3470](https://github.com/JanssenProject/jans/issues/3470)) ([db9b204](https://github.com/JanssenProject/jans/commit/db9b204d1bca9604086a841137c598bbe3ebffe4))
* **jans-auth-server:** when obtain new token using refresh token, check whether scope is null ([#3382](https://github.com/JanssenProject/jans/issues/3382)) ([22743d9](https://github.com/JanssenProject/jans/commit/22743d9fce0c99e794be0eb3969341987b1936ee))
* **jans-auth-server:** wrong import in GluuOrganization class which leads to failure on jans-config-api [#2957](https://github.com/JanssenProject/jans/issues/2957) ([#2958](https://github.com/JanssenProject/jans/issues/2958)) ([af4eda8](https://github.com/JanssenProject/jans/commit/af4eda83147b3fb13f3cc97153d6186c7dcdda74))
* **jans-auth-server:** wrong userinfo_encryption_enc_values_supported in OpenID Configuration [#2725](https://github.com/JanssenProject/jans/issues/2725) ([#2951](https://github.com/JanssenProject/jans/issues/2951)) ([bc1a8ca](https://github.com/JanssenProject/jans/commit/bc1a8ca8b2c7e3b286f2762d9e84205f402cce4a))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))
* upgrade org.mvel:mvel2 from 2.1.3.Final to 2.4.14.Final ([#648](https://github.com/JanssenProject/jans/issues/648)) ([c4034d1](https://github.com/JanssenProject/jans/commit/c4034d12f2bbd9396cc1824f8e485163b4407f68))
* user attributes not updated [#2753](https://github.com/JanssenProject/jans/issues/2753) ([#3326](https://github.com/JanssenProject/jans/issues/3326)) ([c0a0f66](https://github.com/JanssenProject/jans/commit/c0a0f66870e6f4c38dc3a336f1f8b783f4c911ca))
* user attributes not updated [#2753](https://github.com/JanssenProject/jans/issues/2753) ([#3403](https://github.com/JanssenProject/jans/issues/3403)) ([f793f92](https://github.com/JanssenProject/jans/commit/f793f92fa275da2e57b2302dcb5c6fdb27666e67))


### Documentation

* jmeter benchmark authorization code flow test description ([#3312](https://github.com/JanssenProject/jans/issues/3312)) ([6e0c04d](https://github.com/JanssenProject/jans/commit/6e0c04daeb2f000383e433ce2b8533bd8adf98f6))
* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## [1.0.5](https://github.com/JanssenProject/jans/compare/jans-auth-server-v1.0.4...jans-auth-server-v1.0.5) (2022-12-01)


### Features

* add custom annotation for configuration property and feature flag documentation ([#2852](https://github.com/JanssenProject/jans/issues/2852)) ([9991d1c](https://github.com/JanssenProject/jans/commit/9991d1ce1fe1b8ce3a65a72e0a72aeee78ba6c2e))
* documentation for ssa and remove softwareRoles query param of get ssa ([#3031](https://github.com/JanssenProject/jans/issues/3031)) ([d8e14eb](https://github.com/JanssenProject/jans/commit/d8e14ebbeee357c8c2c31808243cf82933ae4a9b))
* **jans-auth-server:** check offline_access implementation has all conditions defined in spec [#1945](https://github.com/JanssenProject/jans/issues/1945) ([#3004](https://github.com/JanssenProject/jans/issues/3004)) ([af30e4c](https://github.com/JanssenProject/jans/commit/af30e4c438372fffb7a3ac78a6aea5988af43d5f))
* **jans-auth-server:** corrected GluuOrganization - refactor getOrganizationName() [#2947](https://github.com/JanssenProject/jans/issues/2947) ([#2948](https://github.com/JanssenProject/jans/issues/2948)) ([9275576](https://github.com/JanssenProject/jans/commit/9275576ed0f925fcd3dbaf06e155e7185c797015))
* **jans-auth-server:** java docs for ssa ([#2995](https://github.com/JanssenProject/jans/issues/2995)) ([892b87a](https://github.com/JanssenProject/jans/commit/892b87a2af5fa82ba4f5dceb38baba28e2029182))
* **jans-auth-server:** remove redirect uri on client registration when grant types is password or client credentials ([#3076](https://github.com/JanssenProject/jans/issues/3076)) ([cd876b4](https://github.com/JanssenProject/jans/commit/cd876b46e6bbdec865f5cd1cfe40c2f3b2ca293c))
* **jans-auth-server:** specify minimum acr for clients [#343](https://github.com/JanssenProject/jans/issues/343) ([#3083](https://github.com/JanssenProject/jans/issues/3083)) ([b0034ec](https://github.com/JanssenProject/jans/commit/b0034ec509ace1a4e30a7e9c6dd23dca48178c62))
* **jans-auth-server:** swagger docs for ssa ([#2953](https://github.com/JanssenProject/jans/issues/2953)) ([7f93bca](https://github.com/JanssenProject/jans/commit/7f93bca9ff101d85f1ae389602f99c7c6af9bc17))
* **jans-auth-server:** updated mau on refreshing access token [#2955](https://github.com/JanssenProject/jans/issues/2955) ([#3025](https://github.com/JanssenProject/jans/issues/3025)) ([56de619](https://github.com/JanssenProject/jans/commit/56de61974ae0d2a3d8382191c2aae479a062e9b2))


### Bug Fixes

* (jans-auth-server): fixed Client serialization/deserialization issue [#2946](https://github.com/JanssenProject/jans/issues/2946) ([#3064](https://github.com/JanssenProject/jans/issues/3064)) ([31b5bfc](https://github.com/JanssenProject/jans/commit/31b5bfc2d626a94998c6e0a1d9121579858437e3))
* (jans-auth-server): fixed client's sortby [#3075](https://github.com/JanssenProject/jans/issues/3075) ([#3079](https://github.com/JanssenProject/jans/issues/3079)) ([e6b0e58](https://github.com/JanssenProject/jans/commit/e6b0e58c7336c2c6537fb55557527abe09ab0811))
* [#2487](https://github.com/JanssenProject/jans/issues/2487) - removing inwebo ([#2975](https://github.com/JanssenProject/jans/issues/2975)) ([052f91f](https://github.com/JanssenProject/jans/commit/052f91fd45c888efb7480fc7cd403dc005ceca23))
* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* **jans-auth-server:** corrected regression made in token request [#2921](https://github.com/JanssenProject/jans/issues/2921) ([#2922](https://github.com/JanssenProject/jans/issues/2922)) ([deeae74](https://github.com/JanssenProject/jans/commit/deeae748aa465e3789114a93eee251628f9d365b))
* **jans-auth-server:** wrong import in GluuOrganization class which leads to failure on jans-config-api [#2957](https://github.com/JanssenProject/jans/issues/2957) ([#2958](https://github.com/JanssenProject/jans/issues/2958)) ([af4eda8](https://github.com/JanssenProject/jans/commit/af4eda83147b3fb13f3cc97153d6186c7dcdda74))
* **jans-auth-server:** wrong userinfo_encryption_enc_values_supported in OpenID Configuration [#2725](https://github.com/JanssenProject/jans/issues/2725) ([#2951](https://github.com/JanssenProject/jans/issues/2951)) ([bc1a8ca](https://github.com/JanssenProject/jans/commit/bc1a8ca8b2c7e3b286f2762d9e84205f402cce4a))

## [1.0.4](https://github.com/JanssenProject/jans/compare/jans-auth-server-v1.0.3...jans-auth-server-v1.0.4) (2022-11-08)


### Features

* **jans-auth-server:** added token exchange support to client [#2518](https://github.com/JanssenProject/jans/issues/2518) ([#2855](https://github.com/JanssenProject/jans/issues/2855)) ([943d99f](https://github.com/JanssenProject/jans/commit/943d99f2784e671d361c66c1ddb82c10f567a698))
* **jans-auth-server:** ssa validation endpoint ([#2842](https://github.com/JanssenProject/jans/issues/2842)) ([de8a86e](https://github.com/JanssenProject/jans/commit/de8a86ed1eb29bd02546e9e22fc6f668ac3217c4))
* ssa revoke endpoint ([#2865](https://github.com/JanssenProject/jans/issues/2865)) ([9c68f91](https://github.com/JanssenProject/jans/commit/9c68f914e155de492e54121033c8f0ed45d66817))


### Bug Fixes

* **jans-auth-server:** fix language metadata format ([#2883](https://github.com/JanssenProject/jans/issues/2883)) ([e21e206](https://github.com/JanssenProject/jans/commit/e21e206df16b048b1743c3ee441d9fbdb1f8c67e))


### Documentation

* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## 1.0.3 (2022-11-01)


### Features

* **agama:** add utility classes for inbound identity ([#2280](https://github.com/JanssenProject/jans/issues/2280)) ([ca6fdc9](https://github.com/JanssenProject/jans/commit/ca6fdc90256e4ef103bf50dc27cb694c940ba70b))
* disable TLS in CB client by default ([#2167](https://github.com/JanssenProject/jans/issues/2167)) ([8ec5dd3](https://github.com/JanssenProject/jans/commit/8ec5dd3dc9818a53949468389a1918ed385c28a9))
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
* **jans-linux-setup:** added token exchange grant type ([#2768](https://github.com/JanssenProject/jans/issues/2768)) ([b3abcfe](https://github.com/JanssenProject/jans/commit/b3abcfeb8fbaddd6d39eeacba018b6baaf6a2d75))
* ssa creation endpoint ([#2495](https://github.com/JanssenProject/jans/issues/2495)) ([61c83e3](https://github.com/JanssenProject/jans/commit/61c83e3305beeaf1a3dbde39d70324153281f218))
* update Coucbase ORM to conform SDK 3.x (config updates) [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#2118](https://github.com/JanssenProject/jans/issues/2118)) ([fceec83](https://github.com/JanssenProject/jans/commit/fceec8332fb36826e5dccb797ee79b769859e126))
* upgrade org.jetbrains:annotations from 18.0.0 to 23.0.0 ([#637](https://github.com/JanssenProject/jans/issues/637)) ([e5fca5a](https://github.com/JanssenProject/jans/commit/e5fca5a09e8aa68dc834de6115490a80fb9da3ff))


### Bug Fixes

* **config-api:** client default value handling ([#2585](https://github.com/JanssenProject/jans/issues/2585)) ([fbcbbad](https://github.com/JanssenProject/jans/commit/fbcbbad0817cd17e645a2491d1732a18b5159cf1))
* fixed multiple encoding issue during authz ([#2152](https://github.com/JanssenProject/jans/issues/2152)) ([fb0b6d7](https://github.com/JanssenProject/jans/commit/fb0b6d738e3e6292453958d44cb14fdcf03ab416))
* include idtoken with dynamic scopes for ciba ([#2108](https://github.com/JanssenProject/jans/issues/2108)) ([d9b5341](https://github.com/JanssenProject/jans/commit/d9b5341d50de972c910883c12785ce6d2758588f))
* **jans auth server:** well known uppercase grant_types response_mode ([#2706](https://github.com/JanssenProject/jans/issues/2706)) ([39f613d](https://github.com/JanssenProject/jans/commit/39f613dbba9a218d9498baa43cc6baba0269b56a))
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
* **jans-auth-server:** npe in discovery if SSA endpoint is absent [#2497](https://github.com/JanssenProject/jans/issues/2497) ([#2498](https://github.com/JanssenProject/jans/issues/2498)) ([c3b00b4](https://github.com/JanssenProject/jans/commit/c3b00b4dac70f164216642cfa5b7f4e8e6a6d9dc))
* **jans-auth-server:** perform redirect_uri validation if FAPI flag is true [#2500](https://github.com/JanssenProject/jans/issues/2500) ([#2502](https://github.com/JanssenProject/jans/issues/2502)) ([aad0460](https://github.com/JanssenProject/jans/commit/aad04603ca714ed3b01dcaeddf2b80a8dccccdf4))
* **jans-auth-server:** PKCE parameters from first SSO request retains in further calls ([#2620](https://github.com/JanssenProject/jans/issues/2620)) ([de98b41](https://github.com/JanssenProject/jans/commit/de98b41ebd9285a087535d42a82dc004e885bd60))
* **jans-auth-server:** ssa get endpoint ([#2719](https://github.com/JanssenProject/jans/issues/2719)) ([35ffbf0](https://github.com/JanssenProject/jans/commit/35ffbf041e7da7376e07d8e7425a2925ce31f403))
* **jans-auth-server:** structure, instance customAttributes, initial data for ssa ([#2577](https://github.com/JanssenProject/jans/issues/2577)) ([f11f789](https://github.com/JanssenProject/jans/commit/f11f789e595762af0c38f1b93de4541ac456d282))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))
* select first sig key if none requested ([#2494](https://github.com/JanssenProject/jans/issues/2494)) ([31fb464](https://github.com/JanssenProject/jans/commit/31fb464560563cf1463e94682d5939e531cabe81))
* upgrade com.google.http-client:google-http-client-jackson2 from 1.26.0 to 1.40.1 ([#644](https://github.com/JanssenProject/jans/issues/644)) ([31bc823](https://github.com/JanssenProject/jans/commit/31bc823e1625e76b8f5d4b2b17357a62fde6e6a2))


### Miscellaneous Chores

* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))


### Documentation

* no docs ([529745d](https://github.com/JanssenProject/jans/commit/529745dddc72a99ace9f49abcba9374f32d90f1d))
* no docs ([ce2bc3f](https://github.com/JanssenProject/jans/commit/ce2bc3f34d78dd9e11414d0db2c5870c77265177))
* no docs ([9d4d84a](https://github.com/JanssenProject/jans/commit/9d4d84a617999ba120a0b42376aa890e96f7c933))
* no docs ([abfd466](https://github.com/JanssenProject/jans/commit/abfd466bf835a4360395f8d9e02360693dce04f6))
* no docs ([aad0460](https://github.com/JanssenProject/jans/commit/aad04603ca714ed3b01dcaeddf2b80a8dccccdf4))
* no docs ([c3b00b4](https://github.com/JanssenProject/jans/commit/c3b00b4dac70f164216642cfa5b7f4e8e6a6d9dc))
* no docs ([f1f0b8d](https://github.com/JanssenProject/jans/commit/f1f0b8d6248c4fb13ce74c78c5355770d4d09a83))
* no docs ([3784c83](https://github.com/JanssenProject/jans/commit/3784c837073c7a45871efc11dac1b721ae710cf1))
* no docs ([2e02d5e](https://github.com/JanssenProject/jans/commit/2e02d5e3a426361da72ae10849cd9affe0607c75))
* no docs ([5c752d1](https://github.com/JanssenProject/jans/commit/5c752d1218997a6b0611f316a6df56a888aa3a14))
* no docs ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* no docs ([e488d10](https://github.com/JanssenProject/jans/commit/e488d10d2cf40a368a0ef9c90d019fb128bbb688))
* no docs ([685be30](https://github.com/JanssenProject/jans/commit/685be30f555af5f8844eb47eb7df77f23552cefb))
* no docs ([cd1a77d](https://github.com/JanssenProject/jans/commit/cd1a77dd36a59b19e975c013c8081610a23106ba))
* no docs ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* no docs ([9b54357](https://github.com/JanssenProject/jans/commit/9b543572bcb893683b4d425ebe2b36f5ccfc0ee9))
* no docs (config-api swagger updated) ([56a33c4](https://github.com/JanssenProject/jans/commit/56a33c40a2bf58ebeb87c6f1724f60a836dc29d2))
* no docs (swagger is updated) ([1b46b44](https://github.com/JanssenProject/jans/commit/1b46b44c6a1bac9c52c7d45358ced4c2c60a9314))
* no docs (swagger updated) ([aed6ee3](https://github.com/JanssenProject/jans/commit/aed6ee3dd570e15fa91a9baf3ffb2461a212cdc0))
* no docs (swagger updated) ([29cfd4e](https://github.com/JanssenProject/jans/commit/29cfd4edaff1248c65d43d956b7b1db0f684d294))
* no docs required ([a41905a](https://github.com/JanssenProject/jans/commit/a41905abba38c051acc7e7d57131da4b7c3a1616))
* no docs required ([958cc92](https://github.com/JanssenProject/jans/commit/958cc9232fafa618cb326c7251486f0add7a15c1))
* updated ([739b939](https://github.com/JanssenProject/jans/commit/739b9393fe4d5fe2a99868d15dc514b69ed44419))

## 1.0.2 (2022-08-30)


### Features

* add support for date ranges in statistic client [#1575](https://github.com/JanssenProject/jans/issues/1575) ([#1653](https://github.com/JanssenProject/jans/issues/1653)) ([8048cd9](https://github.com/JanssenProject/jans/commit/8048cd9b6ab393b8a3e4a1aaf36e09abe20f605b))
* disable TLS in CB client by default ([#2167](https://github.com/JanssenProject/jans/issues/2167)) ([8ec5dd3](https://github.com/JanssenProject/jans/commit/8ec5dd3dc9818a53949468389a1918ed385c28a9))
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
* update Coucbase ORM to conform SDK 3.x (config updates) [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#2118](https://github.com/JanssenProject/jans/issues/2118)) ([fceec83](https://github.com/JanssenProject/jans/commit/fceec8332fb36826e5dccb797ee79b769859e126))


### Bug Fixes

* include idtoken with dynamic scopes for ciba ([#2108](https://github.com/JanssenProject/jans/issues/2108)) ([d9b5341](https://github.com/JanssenProject/jans/commit/d9b5341d50de972c910883c12785ce6d2758588f))
* **jans-auth-server:** client tests expects "scope to claim" mapping which are disabled by default [#1873](https://github.com/JanssenProject/jans/issues/1873) ([958cc92](https://github.com/JanssenProject/jans/commit/958cc9232fafa618cb326c7251486f0add7a15c1))
* **jans-auth-server:** corrected npe in JwtAuthorizationRequest ([9c9e7bf](https://github.com/JanssenProject/jans/commit/9c9e7bf6442637e9f98e9b7765eb373714130d1d))
* **jans-auth-server:** disable surefire for jans-auth-static ([7869efa](https://github.com/JanssenProject/jans/commit/7869efabd5bc4b32fd8bf8347093fa87ab774957))
* **jans-auth-server:** fix missing jsonobject annotation ([#1651](https://github.com/JanssenProject/jans/issues/1651)) ([be5b82a](https://github.com/JanssenProject/jans/commit/be5b82a3ccbc7a0fe9f4ebbb97fa8054657227dc))
* **jans-auth-server:** fixed NPE during getting AT lifetime [#1233](https://github.com/JanssenProject/jans/issues/1233) ([f8be086](https://github.com/JanssenProject/jans/commit/f8be08658c1478acd59fbbfcd609d78179cb00e9))
* **jans-auth-server:** fixing client tests effected by "scope to claim" mapping which is disabled by default [#1873](https://github.com/JanssenProject/jans/issues/1873) ([#1910](https://github.com/JanssenProject/jans/issues/1910)) ([6d81792](https://github.com/JanssenProject/jans/commit/6d81792a141ca725004c23f1bdd0a42314ffcb5f))
* **jans-auth-server:** generate description during built-in key rotation [#1790](https://github.com/JanssenProject/jans/issues/1790) ([#2068](https://github.com/JanssenProject/jans/issues/2068)) ([cd1a77d](https://github.com/JanssenProject/jans/commit/cd1a77dd36a59b19e975c013c8081610a23106ba))
* **jans-auth-server:** increased period of session authn time check ([#1918](https://github.com/JanssenProject/jans/issues/1918)) ([a41905a](https://github.com/JanssenProject/jans/commit/a41905abba38c051acc7e7d57131da4b7c3a1616))
* **login.xhtml:** add google client js ([#1666](https://github.com/JanssenProject/jans/issues/1666)) ([daf9849](https://github.com/JanssenProject/jans/commit/daf9849da1f92707b05517f73bfede1a69103365))


### Documentation

* no docs ([3083a3f](https://github.com/JanssenProject/jans/commit/3083a3f28f6d6c6a9de319f23fd745ac69477249))
* no docs ([e488d10](https://github.com/JanssenProject/jans/commit/e488d10d2cf40a368a0ef9c90d019fb128bbb688))
* no docs ([685be30](https://github.com/JanssenProject/jans/commit/685be30f555af5f8844eb47eb7df77f23552cefb))
* no docs ([cd1a77d](https://github.com/JanssenProject/jans/commit/cd1a77dd36a59b19e975c013c8081610a23106ba))
* no docs ([ca65b24](https://github.com/JanssenProject/jans/commit/ca65b246808d30a9f8965806c4ce963cc6dea8db))
* no docs ([9b54357](https://github.com/JanssenProject/jans/commit/9b543572bcb893683b4d425ebe2b36f5ccfc0ee9))
* no docs required ([a41905a](https://github.com/JanssenProject/jans/commit/a41905abba38c051acc7e7d57131da4b7c3a1616))
* no docs required ([958cc92](https://github.com/JanssenProject/jans/commit/958cc9232fafa618cb326c7251486f0add7a15c1))
* no docs required ([4068197](https://github.com/JanssenProject/jans/commit/40681972a84d691b5d138bc603f32ec80de84fa2))
* no docs required ([812e605](https://github.com/JanssenProject/jans/commit/812e605bf1c0d9041db008fba81515455ab38fab))
* no docs required ([f9f6f49](https://github.com/JanssenProject/jans/commit/f9f6f49c8874cb0a1c71ff0bc0a75e244077feb9))
* no docs required ([929048e](https://github.com/JanssenProject/jans/commit/929048eb41e3c79b25c7474c0a2596b013a3e91c))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/jans-auth-server-v1.0.0...jans-auth-server-v1.0.1) (2022-07-06)


### Features

* add support for date ranges in statistic client [#1575](https://github.com/JanssenProject/jans/issues/1575) ([#1653](https://github.com/JanssenProject/jans/issues/1653)) ([8048cd9](https://github.com/JanssenProject/jans/commit/8048cd9b6ab393b8a3e4a1aaf36e09abe20f605b))
* **agama:** improve flows timeout ([#1447](https://github.com/JanssenProject/jans/issues/1447)) ([ccfb62e](https://github.com/JanssenProject/jans/commit/ccfb62ec13d371c96a0d597d5a0229864f044373))
* **jans-auth-server:** add support for ranges in statistic endpoint (UI team request) ([fd66720](https://github.com/JanssenProject/jans/commit/fd667203564951ba4fc450bf9fb77ba0e70a75ec))
* **jans-auth-server:** added convenient method for up-scoping or down-scoping AT scopes [#1218](https://github.com/JanssenProject/jans/issues/1218) ([5d71655](https://github.com/JanssenProject/jans/commit/5d716553c6eb409c2f264864da8b65c0a0bcbe81))
* **jans-auth-server:** added restriction for request_uri parameter (blocklist and allowed client.request_uri) [#1503](https://github.com/JanssenProject/jans/issues/1503) ([0696d92](https://github.com/JanssenProject/jans/commit/0696d92094eeb2ed36f6b0075680634acbf8992f))
* **jans-auth-server:** added sid and authn_time for active sessions response ([bf9b572](https://github.com/JanssenProject/jans/commit/bf9b572b835d37cc23b2c57437a3830a8ebf55f0))
* **jans-auth-server:** improve client assertion creation code (ClientAuthnRequest) [#1182](https://github.com/JanssenProject/jans/issues/1182) ([81946b2](https://github.com/JanssenProject/jans/commit/81946b22023e9eade94b9202adc6fb32b21652cf))
* **jans-auth-server:** make check whether user is active case insensitive [#1550](https://github.com/JanssenProject/jans/issues/1550) ([d141837](https://github.com/JanssenProject/jans/commit/d14183708a04cdc6406167acc3126f253f212efa))
* **jans-auth-server:** persist org_id from software statement into client's "o" attribute ([021d3bd](https://github.com/JanssenProject/jans/commit/021d3bd17f8a9814e5a0d59b4f28b0c19da88ced))
* **jans-auth-server:** removed dcrSkipSignatureValidation configuration property [#1623](https://github.com/JanssenProject/jans/issues/1623) ([6550247](https://github.com/JanssenProject/jans/commit/6550247ca727d9437ffceec3fa12b9fef93b81e4))


### Bug Fixes

* **jans-auth-server:** added SessionRestWebService to rest initializer ([f0ebf67](https://github.com/JanssenProject/jans/commit/f0ebf67703d52d35c2788b1f528a9f7081dcab6a))
* **jans-auth-server:** corrected npe in JwtAuthorizationRequest ([9c9e7bf](https://github.com/JanssenProject/jans/commit/9c9e7bf6442637e9f98e9b7765eb373714130d1d))
* **jans-auth-server:** disable surefire for jans-auth-static ([7869efa](https://github.com/JanssenProject/jans/commit/7869efabd5bc4b32fd8bf8347093fa87ab774957))
* **jans-auth-server:** fix missing jsonobject annotation ([#1651](https://github.com/JanssenProject/jans/issues/1651)) ([be5b82a](https://github.com/JanssenProject/jans/commit/be5b82a3ccbc7a0fe9f4ebbb97fa8054657227dc))


### Miscellaneous Chores

* release 1.0.0 ([3df6f77](https://github.com/JanssenProject/jans/commit/3df6f7721a8e9d57e28d065ee29153d023dfe9ea))
* release 1.0.0 ([9644d1b](https://github.com/JanssenProject/jans/commit/9644d1bd29c291e57c140b0c9ac67243c322ac35))
* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

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
