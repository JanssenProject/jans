# Changelog

## [1.0.22](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.21...jans-cli-tui-v1.0.22) (2024-01-22)


### Features

* **jans-cli-tui:** grab values of tokenEndpointAuthMethod from swagger doc ([#7141](https://github.com/JanssenProject/jans/issues/7141)) ([d1e6974](https://github.com/JanssenProject/jans/commit/d1e697432193459f55387bdcc1bfb827feadb6b7))
* **jans-cli-tui:** inbound SAML ([#7147](https://github.com/JanssenProject/jans/issues/7147)) ([9bebb1a](https://github.com/JanssenProject/jans/commit/9bebb1a7403f1f6644b2976ada2422630dcd3ec8))
* **jans-cli-tui:** message configuration ([#7198](https://github.com/JanssenProject/jans/issues/7198)) ([5dd6786](https://github.com/JanssenProject/jans/commit/5dd6786b3f12e662658b99ab845109a54ced0676))
* **jans-cli-tui:** save client summary ([#7153](https://github.com/JanssenProject/jans/issues/7153)) ([f017df3](https://github.com/JanssenProject/jans/commit/f017df309458c7ba298a25c3c945476812567a40))


### Bug Fixes

* **jans-cli-tui:** CLI asks creds for unauthorized request ([#7207](https://github.com/JanssenProject/jans/issues/7207)) ([ec8afbe](https://github.com/JanssenProject/jans/commit/ec8afbe79991776b934b515ff1166e5044dba5a5))
* **jans-cli-tui:** display provider list and edit provider ([#7434](https://github.com/JanssenProject/jans/issues/7434)) ([5967da0](https://github.com/JanssenProject/jans/commit/5967da03bef4560aba71c60107fa1e206d740f45))
* **jans-cli-tui:** don't include client secret in summary ([#7161](https://github.com/JanssenProject/jans/issues/7161)) ([798a4d7](https://github.com/JanssenProject/jans/commit/798a4d7fdac64c7515ddd0d2f359f2ff905d7b7b))
* **jans-cli-tui:** LOCK configuration NULL to DISABLED ([#7256](https://github.com/JanssenProject/jans/issues/7256)) ([529ba5f](https://github.com/JanssenProject/jans/commit/529ba5fc000dde43ed5b1c300af061ab751766ef))
* **jans-cli-tui:** rename Message to Lock ([#7210](https://github.com/JanssenProject/jans/issues/7210)) ([19968e8](https://github.com/JanssenProject/jans/commit/19968e8c7be3215dfe22426f866edf366552c022))
* **jans-cli-tui:** save auth server logging config ([#7432](https://github.com/JanssenProject/jans/issues/7432)) ([04dcbde](https://github.com/JanssenProject/jans/commit/04dcbdecb260162734c8bcd1d4517beb6514fa4c))
* prepare for 1.0.22 release ([#7455](https://github.com/JanssenProject/jans/issues/7455)) ([4bf2562](https://github.com/JanssenProject/jans/commit/4bf2562050c86317658259c72bb641780a283579))

## [1.0.21](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.20...jans-cli-tui-v1.0.21) (2023-12-14)


### Features

* **jans-cli-tui:** JWT Response for OAuth Token Introspection ([#6574](https://github.com/JanssenProject/jans/issues/6574)) ([aef08c1](https://github.com/JanssenProject/jans/commit/aef08c1beaf42c0b8d9ef6a214c612c4209925ee))
* **jans-cli-tui:** provide list of enabled scripts for client scripts ([#6621](https://github.com/JanssenProject/jans/issues/6621)) ([f798b32](https://github.com/JanssenProject/jans/commit/f798b32e9b92af54fef683615cd17e4e688c16d4))
* **jans-cli-tui:** replace list by label container for scope claims ([#6521](https://github.com/JanssenProject/jans/issues/6521)) ([4f37aa2](https://github.com/JanssenProject/jans/commit/4f37aa2de920a4f5532f27dcde99edd64ef1d362))
* **jans-cli-tui:** SAML configuration interface ([#6591](https://github.com/JanssenProject/jans/issues/6591)) ([254fbdc](https://github.com/JanssenProject/jans/commit/254fbdcd3123bcef4ba80922dfcfc1c22cd0cbc5))
* **jans-linux-tui:** tag field for endpoint adminUIPermissions ([#6556](https://github.com/JanssenProject/jans/issues/6556)) ([8eb7ac0](https://github.com/JanssenProject/jans/commit/8eb7ac00d61e16553f96d5bdb9af75ef010de268))


### Bug Fixes

* **jans-cli-tui:** fix saving acr script ([#6593](https://github.com/JanssenProject/jans/issues/6593)) ([396576d](https://github.com/JanssenProject/jans/commit/396576dc5f201489a6563757d4f422def12e244a))
* **jans-cli-tui:** hide attrbiute requirePkce for clients ([#7066](https://github.com/JanssenProject/jans/issues/7066)) ([ff44f9c](https://github.com/JanssenProject/jans/commit/ff44f9c21332d7ccc64baf5e3629545d9a592a90))
* **jans-cli-tui:** import error ([#6786](https://github.com/JanssenProject/jans/issues/6786)) ([a71bd2a](https://github.com/JanssenProject/jans/commit/a71bd2ad9e93b886af468cf3d5db63aa6064186f))
* **jans-cli-tui:** saml tr issues ([#7068](https://github.com/JanssenProject/jans/issues/7068)) ([16c32b4](https://github.com/JanssenProject/jans/commit/16c32b43b1e5b13cba2c550ce6da8c1ebfccc3bd))
* **jans-cli-tui:** search attribute ([#6630](https://github.com/JanssenProject/jans/issues/6630)) ([361f52b](https://github.com/JanssenProject/jans/commit/361f52b4ed30a58c98363adee3bdf2f71e1226ff))
* **jans-linux-setup:** python requests-toolbelt library for tui ([#7052](https://github.com/JanssenProject/jans/issues/7052)) ([f0ecba7](https://github.com/JanssenProject/jans/commit/f0ecba7f75ee3f697ee5e0436a32b208f1a7bc0c))
* prepare for 1.0.21 release ([#7008](https://github.com/JanssenProject/jans/issues/7008)) ([2132de6](https://github.com/JanssenProject/jans/commit/2132de6683f67bf22d5a863b149770d657073a83))

## [1.0.20](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.19...jans-cli-tui-v1.0.20) (2023-11-08)


### Features

* **jans-cli-tui:** obtain script types from config-api ([#6314](https://github.com/JanssenProject/jans/issues/6314)) ([5d4e588](https://github.com/JanssenProject/jans/commit/5d4e5885489babaadd59c5aedf2208e94f564512))


### Bug Fixes

* **jans-cli-tui:** client uri, policy uri, logo uri proerties are missing for clients ([#6460](https://github.com/JanssenProject/jans/issues/6460)) ([908fcce](https://github.com/JanssenProject/jans/commit/908fcced38bc6c821beaae118286245498bb1ac0))
* **jans-cli-tui:** hide client scret ([#6372](https://github.com/JanssenProject/jans/issues/6372)) ([ac49f6c](https://github.com/JanssenProject/jans/commit/ac49f6caa0b40e81bc808c37663ccfb54f5a35d5))
* **jans-cli-tui:** remove claim from scope ([#6461](https://github.com/JanssenProject/jans/issues/6461)) ([42791ec](https://github.com/JanssenProject/jans/commit/42791ece6a0acad37660b852fc3c7bf73988082b))
* **jans-cli-tui:** ruamel.yaml&lt;0.18.0 ([#6411](https://github.com/JanssenProject/jans/issues/6411)) ([9da0502](https://github.com/JanssenProject/jans/commit/9da0502da45c8f5d05ad631e609dfda1b77a169c))
* **jans-cli-tui:** save changes before testing STMP config ([#6378](https://github.com/JanssenProject/jans/issues/6378)) ([1429b0b](https://github.com/JanssenProject/jans/commit/1429b0b83c5a715812eaffe700f8253f3c4f6e56))
* **jans-cli-tui:** save logging response is app properties not schema ([#6452](https://github.com/JanssenProject/jans/issues/6452)) ([f1a9950](https://github.com/JanssenProject/jans/commit/f1a9950e2caffd1bcf841fc9f02755ef84e13fa2))
* prepare for 1.0.20 release ([c6e806e](https://github.com/JanssenProject/jans/commit/c6e806eb31fed998d52cbef7a7d94c231d913102))

## [1.0.19](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.18...jans-cli-tui-v1.0.19) (2023-10-11)


### Bug Fixes

* **jans-cli-tui:** mail validation and link log level ([#6140](https://github.com/JanssenProject/jans/issues/6140)) ([f0a8a9a](https://github.com/JanssenProject/jans/commit/f0a8a9a5c389dc9a914aba46c92c63dab4c824e0))
* **jans-linux-setup:** script hide attribute ([#6181](https://github.com/JanssenProject/jans/issues/6181)) ([b4711cd](https://github.com/JanssenProject/jans/commit/b4711cd1074dccc0b8aad81fc4e4bd238c8a516f))
* prepare for 1.0.19 release ([554fd43](https://github.com/JanssenProject/jans/commit/554fd434f624c4b4be3b2031c472177709da8966))

## [1.0.18](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.17...jans-cli-tui-v1.0.18) (2023-09-23)


### Bug Fixes

* prepare for 1.0.18 release ([87af7e4](https://github.com/JanssenProject/jans/commit/87af7e4d41728ce2966362883b47e5354f8c3803))

## [1.0.17](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.16...jans-cli-tui-v1.0.17) (2023-09-17)


### Bug Fixes

* **jans-cli-tui:** check if host in config ([#5836](https://github.com/JanssenProject/jans/issues/5836)) ([04265de](https://github.com/JanssenProject/jans/commit/04265de820bccfc89ce2c9108f41669a605cf03c))
* **jans-cli-tui:** move agama archiever to jans-cli directory ([#5721](https://github.com/JanssenProject/jans/issues/5721)) ([61053f0](https://github.com/JanssenProject/jans/commit/61053f0a9cee48415b57289b8236733dcd725199))
* **jans-cli-tui:** table rows fith dialog ([#6028](https://github.com/JanssenProject/jans/issues/6028)) ([dbbced4](https://github.com/JanssenProject/jans/commit/dbbced44f95050684f18990f28cedf9c7d71923a))
* **jans-cli-tui:** table widget size for agama details dialog ([#6021](https://github.com/JanssenProject/jans/issues/6021)) ([d2a0b56](https://github.com/JanssenProject/jans/commit/d2a0b567f08fb5f33b3363583c28e2901be96674))
* prepare for 1.0.17 release ([4ba8c15](https://github.com/JanssenProject/jans/commit/4ba8c151734f02d762e902b46a35cae2d498fa8f))

## [1.0.16](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.15...jans-cli-tui-v1.0.16) (2023-08-02)


### Features

* add new methnod to fido2 extension to allow modify json ([#5688](https://github.com/JanssenProject/jans/issues/5688)) ([e0984ef](https://github.com/JanssenProject/jans/commit/e0984ef852b7f15bb1938b09f49e3fba0fc1c047)), closes [#5680](https://github.com/JanssenProject/jans/issues/5680)
* **jans-cli-tui:** Jans Link interface ([#5417](https://github.com/JanssenProject/jans/issues/5417)) ([2128dad](https://github.com/JanssenProject/jans/commit/2128dad6670f772a58377642c96d8b95f3edbcba))


### Bug Fixes

* **jans-cli-tui:** change schema reference JansLinkConfiguration to AppConfiguration ([#5653](https://github.com/JanssenProject/jans/issues/5653)) ([ad64cb5](https://github.com/JanssenProject/jans/commit/ad64cb5824fef174fbb58577b24f45fcf21d1d41))
* **jans-cli-tui:** get information for Agama and AgamaConfiguration ([#5530](https://github.com/JanssenProject/jans/issues/5530)) ([6807e11](https://github.com/JanssenProject/jans/commit/6807e1100566006aef6b803c955d1841c8e8b891))
* **jans-cli-tui:** inumdb server in jans link ([#5581](https://github.com/JanssenProject/jans/issues/5581)) ([8f0c66e](https://github.com/JanssenProject/jans/commit/8f0c66ef9d6a4423446c018891a94fc562d19297))
* **jans-cli-tui:** remove serverIpAddress from link configuration ([#5714](https://github.com/JanssenProject/jans/issues/5714)) ([1d0bc2c](https://github.com/JanssenProject/jans/commit/1d0bc2c4d165fbd156f94235466cca5776248bd4))
* **jans-cli-tui:** schema retreiveal with whitespace in path ([#5601](https://github.com/JanssenProject/jans/issues/5601)) ([9a83244](https://github.com/JanssenProject/jans/commit/9a8324476987dc6f1d7d2cea2528ccb9e583b903))
* **jans-cli-tui:** source ldap server dialog ([#5614](https://github.com/JanssenProject/jans/issues/5614)) ([897d45f](https://github.com/JanssenProject/jans/commit/897d45f6dc80ad0607d839882557b07a6183ec52))
* prepare for 1.0.16 release ([042ce79](https://github.com/JanssenProject/jans/commit/042ce7941b9597fade8d5f10e40a89d9e7662315))
* prepare for 1.0.16 release ([b2649c3](https://github.com/JanssenProject/jans/commit/b2649c33a9857f356f91df2f38787ec56269e6dd))

## [1.0.15](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.14...jans-cli-tui-v1.0.15) (2023-07-12)


### Features

* **jans-cli-tui:** configure screen changes ([#5389](https://github.com/JanssenProject/jans/issues/5389)) ([5d73ac3](https://github.com/JanssenProject/jans/commit/5d73ac34027a39bbdefcfe08e29991fb58f64e5a))


### Bug Fixes

* **jans-cli-tui:** combobox spacing ([#5307](https://github.com/JanssenProject/jans/issues/5307)) ([bf05c56](https://github.com/JanssenProject/jans/commit/bf05c563055cea3616b82f4b5f13b72d2114f46f))
* **jans-cli-tui:** error while saving auth creds ([#5446](https://github.com/JanssenProject/jans/issues/5446)) ([2555578](https://github.com/JanssenProject/jans/commit/2555578207c0837e1f261652a2ea5d699b094f95))
* **jans-cli-tui:** remove agama developer studio ([#5466](https://github.com/JanssenProject/jans/issues/5466)) ([205e6ca](https://github.com/JanssenProject/jans/commit/205e6cac829c391c135ce26836bb2090d5de6c2a))
* **jans-cli-tui:** set checkbox selected index to 0 upon filtering ([#5308](https://github.com/JanssenProject/jans/issues/5308)) ([d31c01c](https://github.com/JanssenProject/jans/commit/d31c01c088f404ad2f2db2a6c9dd8fe748d69783))
* **jans-cli:** remove local jwt token when session is revoked ([#5249](https://github.com/JanssenProject/jans/issues/5249)) ([788d2f7](https://github.com/JanssenProject/jans/commit/788d2f7d48bf269f1f54115d16159f5f5f728fa3))
* prepare for 1.0.15 release ([0e3cc2f](https://github.com/JanssenProject/jans/commit/0e3cc2f5ea287c2c35f45def54f074daa473ec49))

## [1.0.14](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.13...jans-cli-tui-v1.0.14) (2023-06-12)


### Features

* **jans-cli-tui:** log command line for write operations ([#5113](https://github.com/JanssenProject/jans/issues/5113)) ([1d213ad](https://github.com/JanssenProject/jans/commit/1d213ad4ff5e7ed897d58f7529c09e9033b92368))


### Bug Fixes

* **jans-cli-tui:** agama project upload ([#5161](https://github.com/JanssenProject/jans/issues/5161)) ([2bb9ecc](https://github.com/JanssenProject/jans/commit/2bb9ecc4482212217f09c907baad89b4cf2e0cfe))
* **jans-cli-tui:** height of checkbox field ([#4985](https://github.com/JanssenProject/jans/issues/4985)) ([c8dc113](https://github.com/JanssenProject/jans/commit/c8dc1136df5d81886108ca54bcd45ededd427055))
* **jans-cli-tui:** userPassword should not be sent in customAttributes ([#5130](https://github.com/JanssenProject/jans/issues/5130)) ([f02aa7e](https://github.com/JanssenProject/jans/commit/f02aa7e681cd8519ffd3f1973802b738bee6a173))
* prepare for 1.0.14 release ([25ccadf](https://github.com/JanssenProject/jans/commit/25ccadf85327ea14685c6066dc6609919e4f2865))

## [1.0.13](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.12...jans-cli-tui-v1.0.13) (2023-05-10)


### Features

* **jans-cli-tui:** obtain auth scripts with single query ([#4677](https://github.com/JanssenProject/jans/issues/4677)) ([f7bea1f](https://github.com/JanssenProject/jans/commit/f7bea1f3feb66d647dbdbccd19ab33f346b06adb))
* **jans-cli-tui:** smtp configuration ([#4262](https://github.com/JanssenProject/jans/issues/4262)) ([09999d7](https://github.com/JanssenProject/jans/commit/09999d791db22640d9f6ec38927b5bfb835ea89a))
* **jans-cli-tui:** warn the user when agama is disabled and an upload is attempted (ref: [#4702](https://github.com/JanssenProject/jans/issues/4702)) ([#4763](https://github.com/JanssenProject/jans/issues/4763)) ([4341e23](https://github.com/JanssenProject/jans/commit/4341e23ae3da4121d273f26368fd2055aa9f6650))
* **jans-fido2:** interception scripts issue 1485, swagger updates ([#4543](https://github.com/JanssenProject/jans/issues/4543)) ([80274ff](https://github.com/JanssenProject/jans/commit/80274ffd1a20318988d9cc99ee015c5c7d5984b7))


### Bug Fixes

* **jans-cli-tui:** add orgument --output-access-token for CLI ([#4671](https://github.com/JanssenProject/jans/issues/4671)) ([14d7854](https://github.com/JanssenProject/jans/commit/14d785480075bea9c9251a275af2ae62f9bd5d53))
* **jans-cli-tui:** change help content for agama page ([#4765](https://github.com/JanssenProject/jans/issues/4765)) ([3310bc7](https://github.com/JanssenProject/jans/commit/3310bc702ec5b83740b4b8019aaa0010d2ac0bed))
* **jans-cli-tui:** display agama popups only if on agama page ([#4665](https://github.com/JanssenProject/jans/issues/4665)) ([2bc852e](https://github.com/JanssenProject/jans/commit/2bc852e4346482dc8ab79a80b66421814e375c17))
* **jans-cli-tui:** endpoint arguments with multiple value ([#4673](https://github.com/JanssenProject/jans/issues/4673)) ([672071d](https://github.com/JanssenProject/jans/commit/672071d887839f659a27f9f47617a084153525cb))
* **jans-cli-tui:** f4 to close dialog ([#4736](https://github.com/JanssenProject/jans/issues/4736)) ([2f2d094](https://github.com/JanssenProject/jans/commit/2f2d094409427dea18526d44ae0c65df98473bbb))
* **jans-cli-tui:** process auth  endpoint has security constraint ([#4882](https://github.com/JanssenProject/jans/issues/4882)) ([9881767](https://github.com/JanssenProject/jans/commit/9881767f9a3ff3aa17df88614be30307ef8bf0f6))
* **jans-cli-tui:** remove duplicated fido value (ref: [#4838](https://github.com/JanssenProject/jans/issues/4838)) ([#4841](https://github.com/JanssenProject/jans/issues/4841)) ([324bdeb](https://github.com/JanssenProject/jans/commit/324bdeba8e0d5722da5f38f44d476861e1f85f60))
* **jans-cli-tui:** remove popup on agama screen ([#4712](https://github.com/JanssenProject/jans/issues/4712)) ([e15ab79](https://github.com/JanssenProject/jans/commit/e15ab79b2389bcfb9ae0bc6e4facf3842fcb24b7))
* **jans-cli-tui:** remove unused code ([#4679](https://github.com/JanssenProject/jans/issues/4679)) ([b9976f1](https://github.com/JanssenProject/jans/commit/b9976f1357ecf7effae9642431fbf8de31556b27))
* **jans-cli-tui:** set seleted to zero in JansVerticalNav when cleared ([#4799](https://github.com/JanssenProject/jans/issues/4799)) ([dafb391](https://github.com/JanssenProject/jans/commit/dafb391fe2cec7be81adf309f07b7c992f3474ca))
* **jans-cli-tui:** typo ([#4696](https://github.com/JanssenProject/jans/issues/4696)) ([d1aa680](https://github.com/JanssenProject/jans/commit/d1aa680fc593c8012f7a5653c2b9354f9c660e09))
* prepare for 1.0.13 release ([493478e](https://github.com/JanssenProject/jans/commit/493478e71f6231553c998b48c0f163c7f5869da4))


### Documentation

* **jans-cli-tui:** update readme ([#4741](https://github.com/JanssenProject/jans/issues/4741)) ([c7bbd63](https://github.com/JanssenProject/jans/commit/c7bbd631920d548e3d88c00832af37c89d19245e))

## [1.0.12](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.11...jans-cli-tui-v1.0.12) (2023-04-18)


### Features

* **jans-cli-tui:** acr values are listbox on client advanced properties ([#4548](https://github.com/JanssenProject/jans/issues/4548)) ([743bfca](https://github.com/JanssenProject/jans/commit/743bfca9598e9a50004c3da702ee66c184694b92))


### Bug Fixes

* **jans-cli-tui:** agama deployments fixes ([#4515](https://github.com/JanssenProject/jans/issues/4515)) ([0c80bf7](https://github.com/JanssenProject/jans/commit/0c80bf71677606a40344e6ce7ae5c3e9dc816596))
* **jans-cli-tui:** agama project romoval message ([#4573](https://github.com/JanssenProject/jans/issues/4573)) ([4c55459](https://github.com/JanssenProject/jans/commit/4c554596a530b1c7551849b4c584bcfc6d3b446e))
* **jans-cli-tui:** check if verification uri exists during authorization ([#4560](https://github.com/JanssenProject/jans/issues/4560)) ([4360b9d](https://github.com/JanssenProject/jans/commit/4360b9d4ac303434f9f59c90e0c471effe87b542))
* **jans-cli-tui:** container focus on page enter ([#4564](https://github.com/JanssenProject/jans/issues/4564)) ([41147ef](https://github.com/JanssenProject/jans/commit/41147ef56088d5ba5b48418f2194ee9ca332b5b6))
* **jans-cli-tui:** display jansId when editing client's scope ([#4547](https://github.com/JanssenProject/jans/issues/4547)) ([107a0fb](https://github.com/JanssenProject/jans/commit/107a0fb566b76a245d25f01dab07e430b2b7282e))
* **jans-cli-tui:** edit client's scope ([#4542](https://github.com/JanssenProject/jans/issues/4542)) ([d719420](https://github.com/JanssenProject/jans/commit/d719420889d19469cbfe5d4c790357f15ab25d4c))
* **jans-cli-tui:** null valued agama flow error ([#4571](https://github.com/JanssenProject/jans/issues/4571)) ([9efc388](https://github.com/JanssenProject/jans/commit/9efc3887e19b39f9bee863da9f0c6742f758c2e2))
* **jans-cli-tui:** save 'Suppress Authorization' ([#4572](https://github.com/JanssenProject/jans/issues/4572)) ([498c7fd](https://github.com/JanssenProject/jans/commit/498c7fdc306080b02b4a7eb8c9629cef363856b3))
* **jans-tui-cli:** Jans cli tui agama post fixes ([#4561](https://github.com/JanssenProject/jans/issues/4561)) ([25dfe69](https://github.com/JanssenProject/jans/commit/25dfe699a78399a19990c8b7410b257e6fd561ce))
* prepare for 1.0.12 release ([6f83197](https://github.com/JanssenProject/jans/commit/6f83197705511c39413456acdc64e9136a97ff39))

## [1.0.11](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.10...jans-cli-tui-v1.0.11) (2023-04-05)


### Features

* jans-cli-tui filter scopes (ref: [#4299](https://github.com/JanssenProject/jans/issues/4299)) ([#4414](https://github.com/JanssenProject/jans/issues/4414)) ([6142009](https://github.com/JanssenProject/jans/commit/614200937935e41c5a347278e2b1c6687d2b8cf1))
* **jans-cli-tui:** device verification url with code ([#4328](https://github.com/JanssenProject/jans/issues/4328)) ([cee9ab4](https://github.com/JanssenProject/jans/commit/cee9ab43adfe3d711f9ae52cf846e6bc19882c06))


### Bug Fixes

* **jans-cli-tui:** agama related issues ([#4438](https://github.com/JanssenProject/jans/issues/4438)) ([1180e31](https://github.com/JanssenProject/jans/commit/1180e3126abfdfa31e94ad878deebb8eb979ca6b))
* **jans-cli-tui:** display jans id in scope list ([#4263](https://github.com/JanssenProject/jans/issues/4263)) ([5d5e1cd](https://github.com/JanssenProject/jans/commit/5d5e1cd81e674a50f64126f458a575d1767bd74a))
* **jans-cli-tui:** fromisoformat function for py &lt; 3.7 ([#4365](https://github.com/JanssenProject/jans/issues/4365)) ([6756b8f](https://github.com/JanssenProject/jans/commit/6756b8fbd1fc870b86fd4a08e88530c4a1c090c7))
* **jans-cli-tui:** remove unused code ([#4367](https://github.com/JanssenProject/jans/issues/4367)) ([c944603](https://github.com/JanssenProject/jans/commit/c944603a90afe9b08343e0aa574d576fa38316b6))
* **jans-cli-tui:** use -dev isntead of -SNAPSHOT in build version ([#4363](https://github.com/JanssenProject/jans/issues/4363)) ([32ea135](https://github.com/JanssenProject/jans/commit/32ea135fd642a82ed93bb45a06256d92db9009d8))
* prepare for  release ([60775c0](https://github.com/JanssenProject/jans/commit/60775c09dc5ab9996bf80c03dcb457861d48dfb1))

## [1.0.10](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.9...jans-cli-tui-v1.0.10) (2023-03-16)


### Bug Fixes

* **jans-cli-tui:** dropdown widget raises error if not initial values provided ([#4142](https://github.com/JanssenProject/jans/issues/4142)) ([0aa51eb](https://github.com/JanssenProject/jans/commit/0aa51eba21e1c5edf4d8132a1e804d2bb23567f7))
* **jans-cli-tui:** working branch 11 ([#3980](https://github.com/JanssenProject/jans/issues/3980)) ([fdba800](https://github.com/JanssenProject/jans/commit/fdba80049e27a2cc89cf12bee960998061fa770e))
* prepare release for 1.0.10 ([e996926](https://github.com/JanssenProject/jans/commit/e99692692ef04d881468d120f7c7d462568dce36))

## [1.0.9](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.8...jans-cli-tui-v1.0.9) (2023-03-09)


### Bug Fixes

* prepare 1.0.9 release ([55f7e0c](https://github.com/JanssenProject/jans/commit/55f7e0c308b869c2c4b5668aca751d022138a678))
* update next SNAPSHOT and dev ([0df0e7a](https://github.com/JanssenProject/jans/commit/0df0e7ae06af64ac477955119c2522f03e0603c3))

## [1.0.8](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.7...jans-cli-tui-v1.0.8) (2023-03-01)


### Features

* **jans-cli-tui:** enable super gluu option ([#3970](https://github.com/JanssenProject/jans/issues/3970)) ([0200751](https://github.com/JanssenProject/jans/commit/020075109f9e204ad35b85a6cd9c0470977b805a))

## [1.0.7](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.6...jans-cli-tui-v1.0.7) (2023-02-22)


### Bug Fixes

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
* jans-linux-setup rename role_based_client as tui_client ([#3630](https://github.com/JanssenProject/jans/issues/3630)) ([b331ef3](https://github.com/JanssenProject/jans/commit/b331ef32b49c870e0a972fc0463e954939317f88))
* prepare 1.0.7 release ([ce02fd9](https://github.com/JanssenProject/jans/commit/ce02fd9322ab49d5bea4f6e88f316f931e9d2169))
* rename role_based_client as tui_client ([#3657](https://github.com/JanssenProject/jans/issues/3657)) ([c393cb2](https://github.com/JanssenProject/jans/commit/c393cb2052f7e73cc6a02b84bbc673bcc93dc13d))

## [1.0.6](https://github.com/JanssenProject/jans/compare/jans-cli-tui-v1.0.5...jans-cli-tui-v1.0.6) (2023-01-09)


### Bug Fixes

* jans-cli-tui ([#3287](https://github.com/JanssenProject/jans/issues/3287)) ([27e7518](https://github.com/JanssenProject/jans/commit/27e7518922aff6434bf9a87392e246af8f44d9ac))
* jans-cli-tui refactor mouse operations ([#3482](https://github.com/JanssenProject/jans/issues/3482)) ([39dc0c7](https://github.com/JanssenProject/jans/commit/39dc0c7b3e0b66e1180e5b8c8c165f992c711555))
* prepare for 1.0.6 release ([9e4c8fb](https://github.com/JanssenProject/jans/commit/9e4c8fb4c0a1ef4779934558e8d8d40d8fedbabf))

## 1.0.5 (2022-12-01)


### Features

* add jans cli tui ([#2384](https://github.com/JanssenProject/jans/issues/2384)) ([c9c502b](https://github.com/JanssenProject/jans/commit/c9c502b5328677bd3ef4895acf296aa3e05bb333))
* jans cli to jans-cli-tui ([#3063](https://github.com/JanssenProject/jans/issues/3063)) ([fc20e28](https://github.com/JanssenProject/jans/commit/fc20e287feb4cc1b7bb983c44e25a8ae936580f0))


### Bug Fixes

* client-name, logout, user ([#3122](https://github.com/JanssenProject/jans/issues/3122)) ([f374831](https://github.com/JanssenProject/jans/commit/f3748312dc72288d9eea9cf1efba8f2afb5278a9))
* getting ready for a release ([0bda832](https://github.com/JanssenProject/jans/commit/0bda832ebc1da7017231deb38fe9aa6c7c51360a))
* jans-cli-docs update links ([#3118](https://github.com/JanssenProject/jans/issues/3118)) ([04fbb98](https://github.com/JanssenProject/jans/commit/04fbb982324cf8849eb3cfac6800f917c63d5300))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))

## [1.0.4](https://github.com/JanssenProject/jans/compare/jans-cli-v1.0.3...jans-cli-v1.0.4) (2022-11-08)


### Documentation

* prepare for 1.0.4 release ([c23a2e5](https://github.com/JanssenProject/jans/commit/c23a2e505b7eb325a293975d60bbc65d5e367c7d))

## 1.0.3 (2022-11-01)


### Bug Fixes

* Jans cli SCIM fixes ([#2394](https://github.com/JanssenProject/jans/issues/2394)) ([a009943](https://github.com/JanssenProject/jans/commit/a009943847238f2d115f794a21b5e229c851db5e))
* jans-cli access token expiration ([#2352](https://github.com/JanssenProject/jans/issues/2352)) ([d506c8e](https://github.com/JanssenProject/jans/commit/d506c8e8960fbc899e3ad1072ccaa40e0713720a))
* jans-cli displayName for OpenID Clients with MySQL backend (ref: [#2314](https://github.com/JanssenProject/jans/issues/2314)) ([#2315](https://github.com/JanssenProject/jans/issues/2315)) ([e0dff68](https://github.com/JanssenProject/jans/commit/e0dff68524102aa78252725f6de4620bee944a29))
* jans-cli endpint param ([#2569](https://github.com/JanssenProject/jans/issues/2569)) ([f6faa71](https://github.com/JanssenProject/jans/commit/f6faa71b4c4803f42785f968c3cfef5d1c59affe))
* jans-cli fixes ([#2429](https://github.com/JanssenProject/jans/issues/2429)) ([c9673dc](https://github.com/JanssenProject/jans/commit/c9673dc12ab4f49fb4107b3695efebbf5b1652bd))
* jans-cli fixes ([#2515](https://github.com/JanssenProject/jans/issues/2515)) ([ccaacc8](https://github.com/JanssenProject/jans/commit/ccaacc8ae564e8f2ef5fd91134bc1c6512634bd5))
* jans-cli info for ConfigurationAgamaFlow ([#2561](https://github.com/JanssenProject/jans/issues/2561)) ([2c446a7](https://github.com/JanssenProject/jans/commit/2c446a7ce64407274e35639dda7e9a48f926988b))
* jans-cli tabulate attrbiutes ([#2321](https://github.com/JanssenProject/jans/issues/2321)) ([cb1e40d](https://github.com/JanssenProject/jans/commit/cb1e40d727bd652c71681509972097be5fae9b54))
* jans-cli user patch ([#2334](https://github.com/JanssenProject/jans/issues/2334)) ([fa3592b](https://github.com/JanssenProject/jans/commit/fa3592bbf76872a95524ec6bf9e2d24796d3f6e5))
* jans-config-api/plugins/sample/helloworld/pom.xml to reduce vulnerabilities ([#972](https://github.com/JanssenProject/jans/issues/972)) ([e2ae05e](https://github.com/JanssenProject/jans/commit/e2ae05e5515dd85a95c0a8520de57f673aba7918))
* jans-eleven/pom.xml to reduce vulnerabilities ([#2676](https://github.com/JanssenProject/jans/issues/2676)) ([d27a7f9](https://github.com/JanssenProject/jans/commit/d27a7f99f22cb8f4bd445a3400224a38cb91eedc))


### Miscellaneous Chores

* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))


### Documentation

* add remote connection section ([#2499](https://github.com/JanssenProject/jans/issues/2499)) ([8eb1376](https://github.com/JanssenProject/jans/commit/8eb1376a9216a170e65d428ad0d3fa9f7f320c2f))
* **jans-cli:** fix broken link ([110cb14](https://github.com/JanssenProject/jans/commit/110cb14894be2171a8c1535954a99031082402f9))

## 1.0.2 (2022-08-30)


### Features

* jans-cli tabulate scim user list ([#1518](https://github.com/JanssenProject/jans/issues/1518)) ([d370978](https://github.com/JanssenProject/jans/commit/d370978fcdad6e6a4027af1bb2610de3513653ba))


### Bug Fixes

* build from source ([#1793](https://github.com/JanssenProject/jans/issues/1793)) ([e389363](https://github.com/JanssenProject/jans/commit/e389363e3fdad7149cdd73ea6fcbc4058f38819a))
* Jans cli user userpassword ([#1542](https://github.com/JanssenProject/jans/issues/1542)) ([d2e13a2](https://github.com/JanssenProject/jans/commit/d2e13a2bcb16a14f63f3bd99c4b3dc83d2a96d9a))
* jans-cli download yaml files for build ([#1635](https://github.com/JanssenProject/jans/issues/1635)) ([31b7e49](https://github.com/JanssenProject/jans/commit/31b7e49043d86c9b266590f6437146d625412f60))
* jans-cli help message format and prompt values (ref: [#1352](https://github.com/JanssenProject/jans/issues/1352)) ([#1478](https://github.com/JanssenProject/jans/issues/1478)) ([37a9181](https://github.com/JanssenProject/jans/commit/37a91819bb7764d2dded27d6b5eafe25de083fe9))
* jans-cli hide menu item ([#1510](https://github.com/JanssenProject/jans/issues/1510)) ([b70fc52](https://github.com/JanssenProject/jans/commit/b70fc52073a3110c767fbc239bb10cc7924838e8))
* jans-cli user list failing for empty customAttributes ([#1525](https://github.com/JanssenProject/jans/issues/1525)) ([7cbf10b](https://github.com/JanssenProject/jans/commit/7cbf10b85187c554bf84bc0ceea6bfcf66cb0088))


### Documentation

* restructure assets directory ([04265c9](https://github.com/JanssenProject/jans/commit/04265c9f6115c491f958c86136f03f4300722ad2))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/jans-cli-v1.0.0...jans-cli-v1.0.1) (2022-07-06)


### Features

* jans-cli --no-suggestion for automated testing ([#1437](https://github.com/JanssenProject/jans/issues/1437)) ([187cc07](https://github.com/JanssenProject/jans/commit/187cc0742102e3d1a708c6b9fffce32d1ac1ebd2))
* jans-cli tabulate scim user list ([#1518](https://github.com/JanssenProject/jans/issues/1518)) ([d370978](https://github.com/JanssenProject/jans/commit/d370978fcdad6e6a4027af1bb2610de3513653ba))


### Bug Fixes

* Jans cli user userpassword ([#1542](https://github.com/JanssenProject/jans/issues/1542)) ([d2e13a2](https://github.com/JanssenProject/jans/commit/d2e13a2bcb16a14f63f3bd99c4b3dc83d2a96d9a))
* jans-cli download yaml files for build ([#1635](https://github.com/JanssenProject/jans/issues/1635)) ([31b7e49](https://github.com/JanssenProject/jans/commit/31b7e49043d86c9b266590f6437146d625412f60))
* jans-cli help message format and prompt values (ref: [#1352](https://github.com/JanssenProject/jans/issues/1352)) ([#1478](https://github.com/JanssenProject/jans/issues/1478)) ([37a9181](https://github.com/JanssenProject/jans/commit/37a91819bb7764d2dded27d6b5eafe25de083fe9))
* jans-cli hide menu item ([#1510](https://github.com/JanssenProject/jans/issues/1510)) ([b70fc52](https://github.com/JanssenProject/jans/commit/b70fc52073a3110c767fbc239bb10cc7924838e8))
* jans-cli user list failing for empty customAttributes ([#1525](https://github.com/JanssenProject/jans/issues/1525)) ([7cbf10b](https://github.com/JanssenProject/jans/commit/7cbf10b85187c554bf84bc0ceea6bfcf66cb0088))


### Documentation

* restructure assets directory ([04265c9](https://github.com/JanssenProject/jans/commit/04265c9f6115c491f958c86136f03f4300722ad2))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

## 1.0.0 (2022-05-20)


### Features

* config-cli enumerate scope type ([275533b](https://github.com/JanssenProject/jans/commit/275533b26f4715113d83ea9cabe4a66cd283a189))
* Jans linux setup ubuntu22 Installation ([#1325](https://github.com/JanssenProject/jans/issues/1325)) ([8597750](https://github.com/JanssenProject/jans/commit/85977502e307884423b4b248694cf74b9b66b96a))
* **jans-auth,jans-cli,jans-config-api:** changes to handle new attribute description in Client object and new custom script type ([d4a9f15](https://github.com/JanssenProject/jans/commit/d4a9f15c3244961cfef6e3229c2e2e49cf85ba0d))
* jans-cli display users in tabular form ([#1296](https://github.com/JanssenProject/jans/issues/1296)) ([7f75d39](https://github.com/JanssenProject/jans/commit/7f75d393cb2854fce58f02f40b90ac4fa9f2a100))
* jans-cli group common items in menu (ref: [#892](https://github.com/JanssenProject/jans/issues/892)) ([#1306](https://github.com/JanssenProject/jans/issues/1306)) ([819f8f7](https://github.com/JanssenProject/jans/commit/819f8f704ab176b70b4daa9c7aca5d662e39a39f))
* jans-cli obtain list of attrbiutes from server when creating user ([1f9b62d](https://github.com/JanssenProject/jans/commit/1f9b62dd133d442d66ef5d3ed6a8cd3ad6da5f7b))
* jans-cli tabulate attribute list ([#1313](https://github.com/JanssenProject/jans/issues/1313)) ([a684484](https://github.com/JanssenProject/jans/commit/a684484d403f9ed52e4c6749f21bd255523a134e))
* jans-cli use test client (ref: [#1283](https://github.com/JanssenProject/jans/issues/1283)) ([#1285](https://github.com/JanssenProject/jans/issues/1285)) ([6320af7](https://github.com/JanssenProject/jans/commit/6320af7ed82ea6fac5672c1c348aeecb7a4b5d7a))
* **jans-config-api:** added patch endpoint for custom script ([f8da77d](https://github.com/JanssenProject/jans/commit/f8da77df201f67055ea7c23c3410e5818a170785))
* **jans-config-api:** user management patch endpoint ([0a7ad7d](https://github.com/JanssenProject/jans/commit/0a7ad7dba82b419d412329414f5895c22fdcaa68))
* **jans-config-api:** user mgmt endpoint ([ad66713](https://github.com/JanssenProject/jans/commit/ad66713700b6988378c4e3c603fb3518f8ade247))
* **jans-config-api:** user mgmt endpoint - wip ([af30358](https://github.com/JanssenProject/jans/commit/af30358d6f8933c405e68449041d5a9e121f3b9f))
* **jans-config-api:** user mgmt patch endpoint ([12a08e1](https://github.com/JanssenProject/jans/commit/12a08e13105467c3912680fb47dc1943590b985a))
* **jans-config-api:** user mgmt patch endpoint ([cb7d36c](https://github.com/JanssenProject/jans/commit/cb7d36cd21ac04f683c38f73d4c9642654886c18))
* **jans-config-api:** user mgt plugin ([ae132cf](https://github.com/JanssenProject/jans/commit/ae132cfe829a3d2ff628b22b23ea716e879c769e))
* **jans-config-api:** user-management endpoints ([#1167](https://github.com/JanssenProject/jans/issues/1167)) ([d8e97c4](https://github.com/JanssenProject/jans/commit/d8e97c4b47b1ff38d4a0207d3f07f461fb807630))
* **jans-core:** remove UPDATE_USER and USER_REGISTRATION scripts [#1289](https://github.com/JanssenProject/jans/issues/1289) ([c34e75d](https://github.com/JanssenProject/jans/commit/c34e75d49db89999633249376c7b42c41bb1ce24))
* jans-linux-setup config-api fido2-plugin (ref: [#1303](https://github.com/JanssenProject/jans/issues/1303)) ([#1308](https://github.com/JanssenProject/jans/issues/1308)) ([ea929c0](https://github.com/JanssenProject/jans/commit/ea929c0637c40ee75f3adbd5377c5e08aebbe087))


### Bug Fixes

* **config-api:** scim user management endpoint failing due to conflict with user mgmt path ([#1181](https://github.com/JanssenProject/jans/issues/1181)) ([8ee47a0](https://github.com/JanssenProject/jans/commit/8ee47a0c62ac1d13ad4a62367744e106c759bbc9))
* jans cli update readme ([2f4f57f](https://github.com/JanssenProject/jans/commit/2f4f57f3d1d38e2c7ca8fd2edc1da798dc36d425))
* jans-cl update WebKeysConfiguration ([#1211](https://github.com/JanssenProject/jans/issues/1211)) ([54847bc](https://github.com/JanssenProject/jans/commit/54847bce0f066ca1ca5d3e0cf01420815c30868c))
* jans-cli allow emptying list attrbiutes by _null ([#1166](https://github.com/JanssenProject/jans/issues/1166)) ([571c5cd](https://github.com/JanssenProject/jans/commit/571c5cd38e42871dd27605f68058b5d766e1f91e))
* jans-cli code smells ([1dc5cb0](https://github.com/JanssenProject/jans/commit/1dc5cb0d05ab3a97c9e414d80e81f0d75586f087))
* jans-cli do not require client if access token is provided ([6b787ec](https://github.com/JanssenProject/jans/commit/6b787ec0313794ac04b81f949d5a2c5f1a5f21dc))
* jans-cli hardcode enums ([739a759](https://github.com/JanssenProject/jans/commit/739a7595dd98751142835957e7d006c59872c89e))
* jans-cli scope dn/id when creating client ([f056abf](https://github.com/JanssenProject/jans/commit/f056abfe98c478c76fad9c6ec1d30b5287b1e208))
* **jans-cli:** corrected typo ([#1050](https://github.com/JanssenProject/jans/issues/1050)) ([4d93a49](https://github.com/JanssenProject/jans/commit/4d93a4926e46e7d82980a187a0be49aac0df9c1c))
* Typo httpLoggingExludePaths jans-auth-server jans-cli jans-config-api jans-linux-setup docker-jans-persistence-loader ([47a20ee](https://github.com/JanssenProject/jans/commit/47a20eefa781d1ca07a9aa30a5adcde3793076d1))
* typo in jans-cli interactive mode ([25f5971](https://github.com/JanssenProject/jans/commit/25f59716aa2bccb2dcdb47a34a7039a0e83d0f5f))
* Use highest level script in case ACR script is not found. Added FF to keep existing behavior. ([#1070](https://github.com/JanssenProject/jans/issues/1070)) ([07473d9](https://github.com/JanssenProject/jans/commit/07473d9a8c3e31f6a75670a874e17341518bf0be))


### Documentation

* fix image paths ([1d07292](https://github.com/JanssenProject/jans/commit/1d07292f7d705b011bce04cd5cfce13b167f3126))
* fix link from README to new docs ([88584af](https://github.com/JanssenProject/jans/commit/88584af5d749dee19d2d4e9b7b61a613a1fb16fd))
* update image file names ([b95a7f1](https://github.com/JanssenProject/jans/commit/b95a7f1b93d3be0c1047c41cdc1eeafcb246d707))


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

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/jans-cli-v1.0.0-beta.15...jans-cli-v1.0.0-beta.16) (2022-03-14)


### Features

* **jans-cli:** get access token from arg ([#1013](https://github.com/JanssenProject/jans/issues/1013)) ([efd718a](https://github.com/JanssenProject/jans/commit/efd718ae39cadd97f2d464572901af2b82932284))
* support regex client attribute to validate redirect uris ([#1005](https://github.com/JanssenProject/jans/issues/1005)) ([a78ee1a](https://github.com/JanssenProject/jans/commit/a78ee1a3cfc4e7a6d08a500750edb5db0f7709a4))


### Bug Fixes

* change in swagger spec for jwks to return missing attributes ([477643b](https://github.com/JanssenProject/jans/commit/477643bf6cc1fc6226ce7790e05c1a981324d06e))
* jans cli build issues (update doc and fix requirements) ([#938](https://github.com/JanssenProject/jans/issues/938)) ([18d1507](https://github.com/JanssenProject/jans/commit/18d1507936a9fdcd8ee6daa46f2ca0af070ea4ba))
* **jans-linux-setup:** require python3-distutils for deb clones ([#967](https://github.com/JanssenProject/jans/issues/967)) ([9a76f23](https://github.com/JanssenProject/jans/commit/9a76f23e259e2e1b7290285f5ee9a70a66be9b0c))


### Documentation

* cli-cache-configuration.md ([#940](https://github.com/JanssenProject/jans/issues/940)) ([0034dc7](https://github.com/JanssenProject/jans/commit/0034dc750119aab1a8f7a6e96f24124e56253056))
* fix cli-jans-authorization-server.md ([#941](https://github.com/JanssenProject/jans/issues/941)) ([5713733](https://github.com/JanssenProject/jans/commit/5713733ab9dee7ea02f50a65ea30a04e2f55ff2e))


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

* jans-cli logout (ref: [#706](https://github.com/JanssenProject/jans/issues/706)) ([#723](https://github.com/JanssenProject/jans/issues/723)) ([0cc51bc](https://github.com/JanssenProject/jans/commit/0cc51bc18a40476ed6bc638225b6897a11c21c16))


### Bug Fixes

* jans-cli sync swagger file from jans-config-api ([#759](https://github.com/JanssenProject/jans/issues/759)) ([315c699](https://github.com/JanssenProject/jans/commit/315c699a84593e7d621a1a3740b053e361133ed4))
* **jans-cli:** jans cli pkg fixes ([#854](https://github.com/JanssenProject/jans/issues/854)) ([9e96e4c](https://github.com/JanssenProject/jans/commit/9e96e4c6b13bc44f4bb2d74222da1669d5b5ed22))
* **jans-cli:** retain scim client in config.ini ([#872](https://github.com/JanssenProject/jans/issues/872)) ([8346517](https://github.com/JanssenProject/jans/commit/83465172bf11ea0a787ee3de34c8dd8968bcdcf0))
* vm setup suse fixes ([#705](https://github.com/JanssenProject/jans/issues/705)) ([2f69a8a](https://github.com/JanssenProject/jans/commit/2f69a8a90747e6c0b67eb76f66edbf3166019264))


### Documentation

* remove sonar badges table from readme ([#694](https://github.com/JanssenProject/jans/issues/694)) ([08f4f1f](https://github.com/JanssenProject/jans/commit/08f4f1fa6d2a35517d15daca613fe47eaf1c682e))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))

## [0.3.0](https://www.github.com/JanssenProject/jans-cli/compare/v0.2.0...v0.3.0) (2021-09-10)


### Features

* display back and quit keys (ref: [#39](https://www.github.com/JanssenProject/jans-cli/issues/39)) ([5ab1619](https://www.github.com/JanssenProject/jans-cli/commit/5ab1619d918b15dfa73ad4324d2750a2bd862841))
* **stat:** spec changes for new stat endpoint jans-config-api issue[#35](https://www.github.com/JanssenProject/jans-cli/issues/35) ([a9a430e](https://www.github.com/JanssenProject/jans-cli/commit/a9a430ee495e6ba15a2eff3e6f91ba6e0c0628ee))


### Bug Fixes

* catch execptiob ([1bb8fae](https://www.github.com/JanssenProject/jans-cli/commit/1bb8faeefedb6d66ab2d492f8fc68f11191636e6))
* statistic results ([d5905ad](https://www.github.com/JanssenProject/jans-cli/commit/d5905ad50b78cd03b32794f481d69bb3e10dcbe7))
* update jca swagger client ([f2e25ac](https://www.github.com/JanssenProject/jans-cli/commit/f2e25acb07376c7e1975574ab5ea0329654145d8))
* update jca swagger client ([d942e73](https://www.github.com/JanssenProject/jans-cli/commit/d942e73378168e984406faca7cc5ac5f7632dcee))
* update swagger client ([1930016](https://www.github.com/JanssenProject/jans-cli/commit/1930016bc334d6e2b7c70a0d2fe4ba870fc5ea5d))
* update swagger client ([3fb65ea](https://www.github.com/JanssenProject/jans-cli/commit/3fb65ea7ff608532b6d378e78a8237c097b5c60f))
* update swagger client and fix for non-secure endpoints ([d833eab](https://www.github.com/JanssenProject/jans-cli/commit/d833eabb680cbff8b7c616d30cf76d0688031910))


### Documentation

* **cli-couchbase:** edited new operations ([7b2ed0e](https://www.github.com/JanssenProject/jans-cli/commit/7b2ed0e331f162aa2181fdf7ecef378483808024))
* **cli-jwk:** edited patch operations ([8faffc2](https://www.github.com/JanssenProject/jans-cli/commit/8faffc2708cb1c43191397c49f4a8c74cfe5a60e))
* **health:** added health liveness n readiness endpoints issue[#52](https://www.github.com/JanssenProject/jans-cli/issues/52) ([d333b41](https://www.github.com/JanssenProject/jans-cli/commit/d333b4129248654df8f3afbe4f4c3e7f2af53f6b))
* **im-jwk:** edited operations ([452c47f](https://www.github.com/JanssenProject/jans-cli/commit/452c47f749ba34c86f0205494f231807ea8e9977))
* updated spec for health endpoints - jansissue[#52](https://www.github.com/JanssenProject/jans-cli/issues/52) ([6c2e946](https://www.github.com/JanssenProject/jans-cli/commit/6c2e946c86c9343daff89b75e61c2c1aafc6d176))

## [0.2.0](https://www.github.com/JanssenProject/jans-cli/compare/v0.1.1...v0.2.0) (2021-07-08)


### Features

* **jwks:** new endpoints to add,get,patch delete for kid jans-config-api-issue[#36](https://www.github.com/JanssenProject/jans-cli/issues/36) ([524c45b](https://www.github.com/JanssenProject/jans-cli/commit/524c45b12d049b6e0ea7845864dd7fa2e24e66c7))
* **patch:** enable providing patch data with args as well as json file ([4ed73d3](https://www.github.com/JanssenProject/jans-cli/commit/4ed73d39f9e7ce8aee86ccd9535abb72e842b4cb))
* **persistence:** spec changes for new endpoints persistenceType and sql-resource ([bf55835](https://www.github.com/JanssenProject/jans-cli/commit/bf55835b6ef556e6b3d02ade4b8074652640338c))


### Bug Fixes

* **cl:** remove patch operation ([eb615d8](https://www.github.com/JanssenProject/jans-cli/commit/eb615d866fc161ef4b4ea5e3a8bb5c4e28d0afa4))
* data args (ref: [#35](https://www.github.com/JanssenProject/jans-cli/issues/35)) ([f30ba9b](https://www.github.com/JanssenProject/jans-cli/commit/f30ba9bf58fd221ffbb5a5e5d988c289d2df08d5))
* heading of each operation task ([9369aae](https://www.github.com/JanssenProject/jans-cli/commit/9369aaed54011d06c95d0900d9bca2d193b67caf))
* **jca-client:** re-update cleint ([9f3e52c](https://www.github.com/JanssenProject/jans-cli/commit/9f3e52c00ae690be4125f13823fb92ca7438d6f9))
* **jca-client:** update cleint ([abfe925](https://www.github.com/JanssenProject/jans-cli/commit/abfe925e7b85a01599b84e8254946128a9c39129))


### Documentation

* **cli-Authz:** updated authorization server ([c550365](https://www.github.com/JanssenProject/jans-cli/commit/c550365bcd6a28646bcea4c9b8b6dcc703aa1afa))
* **cli-couchbase:** added couchbase configuration ([f14dc0d](https://www.github.com/JanssenProject/jans-cli/commit/f14dc0d2adf41644949b5c1f96aae5af66da20e0))
* **cli-custom-scripts:** added custom script cli ([0bf61a7](https://www.github.com/JanssenProject/jans-cli/commit/0bf61a7ffae7c362ba80161dfc21a42bf3ff9656))
* **cli-custom-scripts:** updated operations ([1518f00](https://www.github.com/JanssenProject/jans-cli/commit/1518f00f26de22bf78901860576c2b8e474bc294))
* **cli-customs-scripts:** updated operations ([1cdd509](https://www.github.com/JanssenProject/jans-cli/commit/1cdd50965ee79fd9d23eb6d4e09be9dd9f4181ec))
* **cli-group:** added operations ([03cad75](https://www.github.com/JanssenProject/jans-cli/commit/03cad75a7679f26ea3827a04294fb0ea31dde8aa))
* **cli-index:** updated cli mode ([70f0201](https://www.github.com/JanssenProject/jans-cli/commit/70f02014406eb09a73fd4febfaccad6340701b80))
* **cli-jwk:** added and updated ([985445e](https://www.github.com/JanssenProject/jans-cli/commit/985445e3eb5d2cbfd90898be8bb1ad1170db81b6))
* **cli-jwk:** edited replace option ([d240a5d](https://www.github.com/JanssenProject/jans-cli/commit/d240a5d1c0aeabc189e309d2191008cf5be6853e))
* **cli-jwk:** updated new method ([53378d3](https://www.github.com/JanssenProject/jans-cli/commit/53378d3f39dabba822e33b3080eb425e419bedaf))
* **cli-ldap:** added Ldap Database Configuration ([cb9d7a7](https://www.github.com/JanssenProject/jans-cli/commit/cb9d7a76697a203ab3fafe1b2360796c5676a476))
* **cli-ldap:** edited operations ([18d6b8c](https://www.github.com/JanssenProject/jans-cli/commit/18d6b8c2037ced6a7ddcbe653e26b982ecf289f3))
* **cli-ldap:** updated operations ([9fbe413](https://www.github.com/JanssenProject/jans-cli/commit/9fbe413a4c6f88461a2327e7c60b3422eef67ef1))
* **cli-oauth-scopes:** edited operations ([492d0bd](https://www.github.com/JanssenProject/jans-cli/commit/492d0bdd1f27c6e8b1395c7e4faa7a1a5f9e30c3))
* **cli-oauthscope:** updated operation-id ([fe9c188](https://www.github.com/JanssenProject/jans-cli/commit/fe9c18869a6482d9453f0a62c3a8411246ee4d03))
* **cli-openid:** edited openid connect ([8ef7f00](https://www.github.com/JanssenProject/jans-cli/commit/8ef7f0012505aca05bc31a57adf1667eea4689b5))
* **cli-scim:** edited scim index ([787556c](https://www.github.com/JanssenProject/jans-cli/commit/787556c12ccc9a915f8a287106b11749adec628d))
* **cli-smtp:** updated headings ([48ed1ad](https://www.github.com/JanssenProject/jans-cli/commit/48ed1ad6959ce5870b821199df660e6f4ed6bfee))
* **cli-tips:** added patch-request features ([a938c7c](https://www.github.com/JanssenProject/jans-cli/commit/a938c7ca307ed9ba412067dec81088fc4c1ac033))
* **cli-user:** added headings ([571287f](https://www.github.com/JanssenProject/jans-cli/commit/571287f319b10e93cb7fdd4c22fcafed414a31cd))
* **cli:** updated ([4707b61](https://www.github.com/JanssenProject/jans-cli/commit/4707b617e23e74294250436e60e790ccb3460ea3))
* **cli:** updated patch request ([5ab5bec](https://www.github.com/JanssenProject/jans-cli/commit/5ab5becf623999d010954a899edec8e9da90a27a))
* **im-custom-scripts:** updated headings ([2da17b4](https://www.github.com/JanssenProject/jans-cli/commit/2da17b4b43c4773b3f740f0967c8b2947e9d9a28))
* **im-group:** updated docs headline ([d06c9dc](https://www.github.com/JanssenProject/jans-cli/commit/d06c9dce9e11ae4002f6223959dd638d176f1668))
* **im-ouath-scopes:** updated headings ([9102903](https://www.github.com/JanssenProject/jans-cli/commit/9102903b15a0edee54a29f4f33aeb05ea619ed8d))
* **im-user:** updated headings ([302b0c4](https://www.github.com/JanssenProject/jans-cli/commit/302b0c4616405e2b72f3fa8eb2b770c301539f64))
* **jwk:** updated list item ([9213515](https://www.github.com/JanssenProject/jans-cli/commit/9213515562337f9595e3e3584e27b69028668f3e))
* **README.md:** added documentation index ([4587447](https://www.github.com/JanssenProject/jans-cli/commit/4587447b1327437f54f604d29a228894f7545111))
* **readme:** added manual installtion ([c582d7f](https://www.github.com/JanssenProject/jans-cli/commit/c582d7f73d42f888798a6070b73c6986ffe4c481))
* **README:** index items updated ([7bc0e6f](https://www.github.com/JanssenProject/jans-cli/commit/7bc0e6f4aea430c7814644acdf99ab6c18da1a5e))
* **README:** updated index list ([d868756](https://www.github.com/JanssenProject/jans-cli/commit/d8687564ebbd6b086a58c859b67871818c4ec90a))
* **README:** updated installation ([1e19094](https://www.github.com/JanssenProject/jans-cli/commit/1e19094d1604b3d42f61ab6b41ac4244e1f6294d))
* **spec:** modified spec and test as pet auth-server AppConfiguration issues[#19](https://www.github.com/JanssenProject/jans-cli/issues/19) ([097769d](https://www.github.com/JanssenProject/jans-cli/commit/097769d7888e90bca5bae06cfda2159ae12cc23c))

### [0.1.1](https://www.github.com/JanssenProject/jans-cli/compare/v0.1.0...v0.1.1) (2021-05-07)


### Bug Fixes

* config path ([2befc3b](https://www.github.com/JanssenProject/jans-cli/commit/2befc3b1aeed3f5dea3646f2a0e32e5b56722c8c))
* path ([c429d21](https://www.github.com/JanssenProject/jans-cli/commit/c429d21c77d66e8657bb79031ca4bbe795baff9b))
* **pip:** installation ([b39e49e](https://www.github.com/JanssenProject/jans-cli/commit/b39e49ef43a7eb603fa64f85b63c41c6281b6ecf))


### Documentation

* **authconfig:** synched with jans-auth-server AppConfiguration properties jans-config-issue[#19](https://www.github.com/JanssenProject/jans-cli/issues/19) ([a28de85](https://www.github.com/JanssenProject/jans-cli/commit/a28de85eec90d35d2c6d526bb6e1fa7aca1b730d))

## 0.1.0 (2021-05-06)


### Features

* **conn:** check connection before displaying menu ([662e987](https://www.github.com/JanssenProject/jans-cli/commit/662e987aa91978cc70b1e5bd49ae0a69f01e4608))
* **error:** log error to file and display reason ([31a24ec](https://www.github.com/JanssenProject/jans-cli/commit/31a24ecddf18e5499db76b534fa0cbe662b8d425))
* **file:** render '_file /path/file.py' in json value ([6d7b7f8](https://www.github.com/JanssenProject/jans-cli/commit/6d7b7f87d631745ecd77b1a9745386cd23f4cea9))
* **jwt:** decode jwt encoded data ([6204ff3](https://www.github.com/JanssenProject/jans-cli/commit/6204ff3c8a1cd6e360ad45f1d63f9f6f6c9cafa6))
* package jans cli ([59b7ecd](https://www.github.com/JanssenProject/jans-cli/commit/59b7ecd8a48ffec0776e4d99cf75a2f58427e5a8))
* **ssl:** Add -cert-file and -key-file arguments ([58030e6](https://www.github.com/JanssenProject/jans-cli/commit/58030e673d6dc097c67c448bae7cf4e228e55186))
* user authentication (ref: [#20](https://www.github.com/JanssenProject/jans-cli/issues/20)) ([e67a57a](https://www.github.com/JanssenProject/jans-cli/commit/e67a57af1674fe10218463d1f66d60c9343c2c3a))


### Bug Fixes

* add params to swagger function (ref: [#24](https://www.github.com/JanssenProject/jans-cli/issues/24)) ([ed99ad5](https://www.github.com/JanssenProject/jans-cli/commit/ed99ad505319428563a24f543841eb3d16a77141))
* change inum to id for sector identifier enpoint (ref: ([0fe4e84](https://www.github.com/JanssenProject/jans-cli/commit/0fe4e84720344b4efa5dcabd1103e65a757c1dd5))
* change patch question ([775c671](https://www.github.com/JanssenProject/jans-cli/commit/775c671f2865ff650a4708f581bac84f4772a32a))
* check connection ([3c679ce](https://www.github.com/JanssenProject/jans-cli/commit/3c679ce425e0977791610e2e77fd0318b8df7fd9))
* delimate path by dot for scim (ref: [#22](https://www.github.com/JanssenProject/jans-cli/issues/22)) ([73c1b7a](https://www.github.com/JanssenProject/jans-cli/commit/73c1b7aea1a0460cfb93826098b5c55775464bf6))
* description for paramater (ref: [#23](https://www.github.com/JanssenProject/jans-cli/issues/23)) ([52960e8](https://www.github.com/JanssenProject/jans-cli/commit/52960e82b1253c937ba7ad2301135b2537c6959a))
* document ([f3c5a5d](https://www.github.com/JanssenProject/jans-cli/commit/f3c5a5d30c89b975da0b18064bab9bc955927c74))
* document --key-file, --cert-file and -noverify ([876285a](https://www.github.com/JanssenProject/jans-cli/commit/876285aedead55745adb43590a0771193e98b1ce))
* image path ([6e1ed53](https://www.github.com/JanssenProject/jans-cli/commit/6e1ed53cc7ed052d422b766245e9568c491fb8cc))
* include package data ([72affc9](https://www.github.com/JanssenProject/jans-cli/commit/72affc919842cf2e488e8c40101e82b61a865c65))
* set default value of noverify as True ([256cf48](https://www.github.com/JanssenProject/jans-cli/commit/256cf48ae6ff4eb8f02c5e9f777bb8118a3201ba))
* type filed -> field ([0d6a68a](https://www.github.com/JanssenProject/jans-cli/commit/0d6a68a42d5cfe322c13190d0ffc135be7f45952))


### Documentation

* **auth-properties:** added new properties for AppConfiguration jans-config-api issue[#19](https://www.github.com/JanssenProject/jans-cli/issues/19) ([6800b09](https://www.github.com/JanssenProject/jans-cli/commit/6800b092e0e528b54d6ed2648e72bbdf47354031))
* **cli-attribute:** edited patch option ([83be863](https://www.github.com/JanssenProject/jans-cli/commit/83be86382a508b0c6a14f607badcefeb113023af))
* **cli-cache-config:** added toc ([7d7fbf9](https://www.github.com/JanssenProject/jans-cli/commit/7d7fbf956e8217a52fc0c6fb1aea90a94d7e65ef))
* **cli-group:** added toc in scim-group ([6571286](https://www.github.com/JanssenProject/jans-cli/commit/6571286b88bac0ae6ee376dd0d123a70119d35f8))
* **cli-jans-auth:** added toc ([065ca0b](https://www.github.com/JanssenProject/jans-cli/commit/065ca0b057132fac7023ce470567f074afb2220d))
* **cli-jans-fido2:** edited toc ([ccac715](https://www.github.com/JanssenProject/jans-cli/commit/ccac71546981a934d0f7a795576179561bcbba1f))
* **cli-log:** edited toc ([0843656](https://www.github.com/JanssenProject/jans-cli/commit/08436568e7eb33471618a12f446ea825a2954ab5))
* **cli-logging-conf:** added toc ([1e3035c](https://www.github.com/JanssenProject/jans-cli/commit/1e3035cc11007055cddca82f01f590a0cd2658bb))
* **cli-oauthscope:** edited content ([02e1e0b](https://www.github.com/JanssenProject/jans-cli/commit/02e1e0b282ddced55769557c73faa14a5fa83104))
* **cli-tips:** added patch request basic ([40242ec](https://www.github.com/JanssenProject/jans-cli/commit/40242ec0e97971276180e9b30d684bedc3df253d))
* **cli-uma:** added toc, heading ([ae074b0](https://www.github.com/JanssenProject/jans-cli/commit/ae074b0548450d489a17969123962ce62df2ce72))
* **codacy:** Add badge ([51a60e8](https://www.github.com/JanssenProject/jans-cli/commit/51a60e8b06aacf217fef1469220266abd6bd1e0a))
* **default-auth:** added toc ([08e3d0b](https://www.github.com/JanssenProject/jans-cli/commit/08e3d0b066b611641243872855e885b49a8c4a27))
* **default-auth:** edited toc ([a130cbb](https://www.github.com/JanssenProject/jans-cli/commit/a130cbb82dad4142bf83d470bbb6d813110416a0))
* **jans-fido2:** added toc ([7db7805](https://www.github.com/JanssenProject/jans-cli/commit/7db7805364fb09411435293d8856d29dbb0d8aea))
* **ldap:** issue[#27](https://www.github.com/JanssenProject/jans-cli/issues/27) added post method for ldap ([47ff003](https://www.github.com/JanssenProject/jans-cli/commit/47ff0032935b8c6429d436bd98ea344b76189e1e))
* **readme:** added scim group cli operations ([991bade](https://www.github.com/JanssenProject/jans-cli/commit/991badeabac6b67bf22ab736a1bc9d4408fd5f88))
* **readme:** added scim query ([c614439](https://www.github.com/JanssenProject/jans-cli/commit/c614439482a348aedf7560795c025fe9e63e9e2a))
* **readme:** edited cli-attribute ([03ec2e5](https://www.github.com/JanssenProject/jans-cli/commit/03ec2e54b6aafde8fb60030c2279c339a017bb87))
* **readme:** edited documentation files ([c1c90e3](https://www.github.com/JanssenProject/jans-cli/commit/c1c90e3274a9816e3549dcb06b542b1ec916c1c1))
* **readme:** edited documentation path ([2a3cf68](https://www.github.com/JanssenProject/jans-cli/commit/2a3cf68ab3900b93d9c7be47885629a4744bb8c1))
* **readme:** edited documentation path ([a0d1ace](https://www.github.com/JanssenProject/jans-cli/commit/a0d1acea71fb12c39eb0b4d0132e7aa4d4dc13f3))
* **readme:** edited installation process ([447a2c7](https://www.github.com/JanssenProject/jans-cli/commit/447a2c7d1f5ac889d970f6f08d1363aab722f9cf))
* **readme:** edited scim query ([5ee8174](https://www.github.com/JanssenProject/jans-cli/commit/5ee81748f7403c94023f2150a899df06fa65a34c))
* **readme:** edited scim query ([a605cd5](https://www.github.com/JanssenProject/jans-cli/commit/a605cd5e8103b9ab023af946f55a90abbc464755))
* **readme:** edited scim query and CL ([1b1d32d](https://www.github.com/JanssenProject/jans-cli/commit/1b1d32de98c69ad255c973650103dd51a05c0efd))
* **readme:** edited scim user cli ([7b40cab](https://www.github.com/JanssenProject/jans-cli/commit/7b40cab1d82354fb13abbb8c70324509cf34c660))
* **readme:** edited scim user query ([698a211](https://www.github.com/JanssenProject/jans-cli/commit/698a211b95fdb208a2b5658eb4d624af16c5d5b0))
* **readme:** edited scim-cli group operations ([0298426](https://www.github.com/JanssenProject/jans-cli/commit/0298426ccbb0f07c3b67498675511c1ce2d7cfda))
* **readme:** edited scim-cli group update ([7c39052](https://www.github.com/JanssenProject/jans-cli/commit/7c39052db45143f32b85663ebc2d741538172cb2))
* **readme:** edited sector identifier ([ed7522e](https://www.github.com/JanssenProject/jans-cli/commit/ed7522ee9ea082a1d4490a605534a668d114fc0c))
* **readme:** edited sector identifier ([c3165bb](https://www.github.com/JanssenProject/jans-cli/commit/c3165bb1481843b90b297f69a526195e3541c64f))
* **readme:** edited typo error ([cc7e2f8](https://www.github.com/JanssenProject/jans-cli/commit/cc7e2f8c20d05f3ecb5a90e05f9f06aea9dfe506))
* **README:** edited user info ([e20a00b](https://www.github.com/JanssenProject/jans-cli/commit/e20a00beb88f08621ec2edcb21f3c65cd1047001))
* **sectoridentifiers:** Removed details for sectoridentifiers endpoint ([4af61ce](https://www.github.com/JanssenProject/jans-cli/commit/4af61ce9d50472498e7274458ea6442a2c3a6ba5))
* **smtp:** modified response for smtp test endpoint ([68fc113](https://www.github.com/JanssenProject/jans-cli/commit/68fc11371a482bcbb3f3a1269befbfa9ba01d03d))
* **swagger:** added discoveryAllowedKeys configuration property ([b2b7aa7](https://www.github.com/JanssenProject/jans-cli/commit/b2b7aa7db1a784bf3cd6aa96986ecb23435f9a75))
