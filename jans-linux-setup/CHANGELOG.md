# Changelog

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
