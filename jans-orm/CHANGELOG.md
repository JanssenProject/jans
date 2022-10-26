# Changelog

## 1.0.2 (2022-08-30)


### Features

* add new methods to allow get/set list of custom attributes from ([#2105](https://github.com/JanssenProject/jans/issues/2105)) ([5ac23a1](https://github.com/JanssenProject/jans/commit/5ac23a18adf3b34fd41fd1199ab168bfc9602fc6)), closes [#2104](https://github.com/JanssenProject/jans/issues/2104)
* disable TLS in CB client by default ([#2167](https://github.com/JanssenProject/jans/issues/2167)) ([8ec5dd3](https://github.com/JanssenProject/jans/commit/8ec5dd3dc9818a53949468389a1918ed385c28a9))
* fix susrefire tests in filter module ([#2141](https://github.com/JanssenProject/jans/issues/2141)) ([118d77c](https://github.com/JanssenProject/jans/commit/118d77cd7025dcbe3031bc41450ec285afff4b9f))
* **jans-orm:** update Couchbase ORM to use SDK 3.x [#1851](https://github.com/JanssenProject/jans/issues/1851) ([#1852](https://github.com/JanssenProject/jans/issues/1852)) ([d9d5157](https://github.com/JanssenProject/jans/commit/d9d5157c3421f4995ee4abd6918c106f9a78dd5f))


### Bug Fixes

* don't execute next paged search if current result count less than ([#2171](https://github.com/JanssenProject/jans/issues/2171)) ([94a162f](https://github.com/JanssenProject/jans/commit/94a162f4471ec6e4798721894b4a5a583ad71370))
* **jans-auth-server:** sql localizedstring persistence SqlEntryManager ([#1475](https://github.com/JanssenProject/jans/issues/1475)) ([b959b94](https://github.com/JanssenProject/jans/commit/b959b94e235c8bb554fcbdc8abbc22e3df540dbe))
* **jans-orm:** fixed npe in filter processor and covered with tests ([ef46516](https://github.com/JanssenProject/jans/commit/ef4651677c415b92d8db01f6bf67eda4d9b9dd03))
* **jans-orm:** switch to 1.0.1-SNAPSHOT ([0030da7](https://github.com/JanssenProject/jans/commit/0030da76d16eedbdcfc74b72b99705a9fb63cb27))
* use iterator to correcly remove OC attribute ([#2138](https://github.com/JanssenProject/jans/issues/2138)) ([b590981](https://github.com/JanssenProject/jans/commit/b590981c53c26f4b1a8b6a0865ddc552e0a347b8))


### Documentation

* no docs required ([ef46516](https://github.com/JanssenProject/jans/commit/ef4651677c415b92d8db01f6bf67eda4d9b9dd03))


### Miscellaneous Chores

* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))
* release 1.0.2 ([43dead6](https://github.com/JanssenProject/jans/commit/43dead615f3508ca393c330c2db27a8fb9d1017a))

## [1.0.1](https://github.com/JanssenProject/jans/compare/jans-orm-v1.0.0...jans-orm-v1.0.1) (2022-07-06)


### Bug Fixes

* **jans-auth-server:** sql localizedstring persistence SqlEntryManager ([#1475](https://github.com/JanssenProject/jans/issues/1475)) ([b959b94](https://github.com/JanssenProject/jans/commit/b959b94e235c8bb554fcbdc8abbc22e3df540dbe))
* **jans-orm:** switch to 1.0.1-SNAPSHOT ([0030da7](https://github.com/JanssenProject/jans/commit/0030da76d16eedbdcfc74b72b99705a9fb63cb27))


### Miscellaneous Chores

* release 1.0.0 ([3df6f77](https://github.com/JanssenProject/jans/commit/3df6f7721a8e9d57e28d065ee29153d023dfe9ea))
* release 1.0.0 ([9644d1b](https://github.com/JanssenProject/jans/commit/9644d1bd29c291e57c140b0c9ac67243c322ac35))
* release 1.0.1 ([828bfe8](https://github.com/JanssenProject/jans/commit/828bfe80cee87e639839391f98ac3dc2f2d4a920))

## 1.0.0 (2022-05-19)


### Features

* **jans-auth-server:** client registration language metadata ([#1237](https://github.com/JanssenProject/jans/issues/1237)) ([a8d0157](https://github.com/JanssenProject/jans/commit/a8d0157b0a8664e5e4d58a9524a0fa20df324381))
* **jans-config-api:** user mgmt patch endpoint ([cb7d36c](https://github.com/JanssenProject/jans/commit/cb7d36cd21ac04f683c38f73d4c9642654886c18))
* **jans:** jetty 11 integration ([#1123](https://github.com/JanssenProject/jans/issues/1123)) ([6c1caa1](https://github.com/JanssenProject/jans/commit/6c1caa1c4c92d28571f8589cd701e6885d4d85ef))
* merge ORM from Gluu ([#1200](https://github.com/JanssenProject/jans/issues/1200)) ([685a159](https://github.com/JanssenProject/jans/commit/685a1593fb53e2310cfa38fcd49db94f3453042f))


### Bug Fixes

* **config-api:** scim user management endpoint failing due to conflict with user mgmt path ([#1181](https://github.com/JanssenProject/jans/issues/1181)) ([8ee47a0](https://github.com/JanssenProject/jans/commit/8ee47a0c62ac1d13ad4a62367744e106c759bbc9))
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

## [1.0.0-beta.16](https://github.com/JanssenProject/jans/compare/jans-orm-v1.0.0-beta.15...jans-orm-v1.0.0-beta.16) (2022-03-14)


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

* support MySQL with MariaDB engine gluu [#18](https://github.com/JanssenProject/jans/issues/18) ([#712](https://github.com/JanssenProject/jans/issues/712)) ([9071db4](https://github.com/JanssenProject/jans/commit/9071db4be4d51f7a4042e7fb81704f490e90cf38))


### Bug Fixes

* check MariaDB json in result set metadata ([96b6772](https://github.com/JanssenProject/jans/commit/96b67720018fd7d1100ddeeda8f1434bddca2816))
* fix method to determine if Db is MariaDB ([edf5a8d](https://github.com/JanssenProject/jans/commit/edf5a8d91cba64b575e9c195bc682976b55dc51f))
* fix RDBS export entry and DeleteNotifier ([#864](https://github.com/JanssenProject/jans/issues/864)) ([ce5b2e6](https://github.com/JanssenProject/jans/commit/ce5b2e61d5ebaf0c81f7ad34459a635780da0c38))


### Miscellaneous Chores

* release 1.0.0-beta.15 ([ee5b719](https://github.com/JanssenProject/jans/commit/ee5b719bee5cc4bdaebf81a5103e6a7ab0695dbb))
* release 1.0.0-beta.15 ([ca6d1c9](https://github.com/JanssenProject/jans/commit/ca6d1c9e2acb5e6422e1cd26ac277dd3eba4e56e))
* release 1.0.0-beta.15 ([b65bab2](https://github.com/JanssenProject/jans/commit/b65bab20530b7d6736dd404e26649abf47c0fb60))
