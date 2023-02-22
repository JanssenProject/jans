# Changelog

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
