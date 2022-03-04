# Changelog

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
