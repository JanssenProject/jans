# Changelog

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
