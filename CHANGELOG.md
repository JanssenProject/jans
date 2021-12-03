# Changelog

## 1.0.0-beta.13 (2021-12-03)


### Features

* add support for plugins ([#67](https://www.github.com/JanssenProject/jans-cloud-native/issues/67)) ([7f2204c](https://www.github.com/JanssenProject/jans-cloud-native/commit/7f2204cb186902ebdc0d1f6ae1d321f3c5eeea5b))
* **google:** add google secret as the config and secret layer ([#21](https://www.github.com/JanssenProject/jans-cloud-native/issues/21)) ([1593f0e](https://www.github.com/JanssenProject/jans-cloud-native/commit/1593f0e944e4395ecd8a26f78d31d2b03313e069))
* **helm:** Update Helm Chart ([#1](https://www.github.com/JanssenProject/jans-cloud-native/issues/1)) ([8870dac](https://www.github.com/JanssenProject/jans-cloud-native/commit/8870dac4afaf80063fba6314d288ee6c98d83d69))
* **helm:** Update helm chart ([#18](https://www.github.com/JanssenProject/jans-cloud-native/issues/18)) ([39f2e97](https://www.github.com/JanssenProject/jans-cloud-native/commit/39f2e977bb8ce7b94e642a46a35af1dc9b71c887))
* **helm:** update Helm Chart ([#35](https://www.github.com/JanssenProject/jans-cloud-native/issues/35)) ([ac96328](https://www.github.com/JanssenProject/jans-cloud-native/commit/ac9632806b2fb33fcdb87a10ce357daae4b8cad0))
* **helm:** Update Helm Chart ([#4](https://www.github.com/JanssenProject/jans-cloud-native/issues/4)) ([0eb1d13](https://www.github.com/JanssenProject/jans-cloud-native/commit/0eb1d13314d4ffcd2d2303c9eb902129669b7188))
* **helm:** Update Helm Chart ([#6](https://www.github.com/JanssenProject/jans-cloud-native/issues/6)) ([1cbdfb8](https://www.github.com/JanssenProject/jans-cloud-native/commit/1cbdfb8a946bc13d0fda7191b0970f0287fe49d3))
* **ingress:** Allow control of applying ingress objects ([4b333ab](https://www.github.com/JanssenProject/jans-cloud-native/commit/4b333ab68306252ed1e1eefdadb4b27eee560c9f)), closes [#9](https://www.github.com/JanssenProject/jans-cloud-native/issues/9)
* **interception-debug:** Add instructions ([c07378a](https://www.github.com/JanssenProject/jans-cloud-native/commit/c07378a1c47f75ddbea3cdd4631a5bbc1590fa89))
* **mutli-cluster:** Add helm configurations ([bc1b83a](https://www.github.com/JanssenProject/jans-cloud-native/commit/bc1b83aa0ebc81bf084c1eb5d15b6f7a86dfc613)), closes [#16](https://www.github.com/JanssenProject/jans-cloud-native/issues/16)
* **pre-release:** Prepare release of 1.0.0-a1 ([200e1fb](https://www.github.com/JanssenProject/jans-cloud-native/commit/200e1fb7c7d36c421e7a7dd415cad4c19b6932fd))
* **release:** release 1.0.0_a2 ([8841e1b](https://www.github.com/JanssenProject/jans-cloud-native/commit/8841e1bc74e5db8a7893f9707ab3f695ab839546))
* **release:** release 1.0.0-a3 ([4508478](https://www.github.com/JanssenProject/jans-cloud-native/commit/4508478a103e2bab311c6cecce9be3f372f92d46))
* **scim:** support testmode ([c35eb38](https://www.github.com/JanssenProject/jans-cloud-native/commit/c35eb38cd8e71c58e557a7f96962925d52b8d75b))
* **sql:** Add sql support ([e9dc86c](https://www.github.com/JanssenProject/jans-cloud-native/commit/e9dc86c4e7f02727588a7da587ad1d14216d3c59))
* **storageclass:** Allow custom StorageClass parameters ([a34627c](https://www.github.com/JanssenProject/jans-cloud-native/commit/a34627c55cca28421cd4a674edab7c9da0438272)), closes [#14](https://www.github.com/JanssenProject/jans-cloud-native/issues/14)
* update helm charts ([#23](https://www.github.com/JanssenProject/jans-cloud-native/issues/23)) ([416d773](https://www.github.com/JanssenProject/jans-cloud-native/commit/416d7732253ed9d985bdcc88f04347f76657eb8a))


### Bug Fixes

* **_config.yml:** adjust link to debug ([9eb0469](https://www.github.com/JanssenProject/jans-cloud-native/commit/9eb04690d286725be355435329f87c14f2334ba2))
* add optional scopes parameter ([1c41ae5](https://www.github.com/JanssenProject/jans-cloud-native/commit/1c41ae5c59415838e8f0c5eff214551f67d3007b)), closes [#10](https://www.github.com/JanssenProject/jans-cloud-native/issues/10)
* assign separate owner to .github directory ([#51](https://www.github.com/JanssenProject/jans-cloud-native/issues/51)) ([eb38729](https://www.github.com/JanssenProject/jans-cloud-native/commit/eb387297c41de6208b28caeb152e8d436e839bd1))
* clean up unused envs ([a3dd9b3](https://www.github.com/JanssenProject/jans-cloud-native/commit/a3dd9b306187aaf998ededc4a9be1f4893e3f832))
* config-api endpoint ([63ad0e6](https://www.github.com/JanssenProject/jans-cloud-native/commit/63ad0e6706b34c04489fa0a5a05e92bca75a9351))
* **config-api:** use http port number ([de58437](https://www.github.com/JanssenProject/jans-cloud-native/commit/de584378da812549d45c99e19f3b457ea35f682c))
* **couchbase:** fix bucket prefix call ([177a794](https://www.github.com/JanssenProject/jans-cloud-native/commit/177a79472e64f70881eaa973b81fce2d79512e24))
* do not install protected endpoints ingress by default ([85c8a6f](https://www.github.com/JanssenProject/jans-cloud-native/commit/85c8a6f5954300542b8924134a3df4d533fcb1bd))
* forcefully patch tls-certificate secret per config job run ([0fd3b53](https://www.github.com/JanssenProject/jans-cloud-native/commit/0fd3b532cd60e82b9ecb5d1c12a7cd1b0271bd48))
* **helm:** comment parameters ([890860f](https://www.github.com/JanssenProject/jans-cloud-native/commit/890860f60332319597c55ea6194458f95e98aeab))
* **helminstall:** fix helm install values path ([b032970](https://www.github.com/JanssenProject/jans-cloud-native/commit/b0329702801fa4a18cc5b2b02ce265f54b60c86a))
* **ingress:** Use older apiVersion ([b7583e2](https://www.github.com/JanssenProject/jans-cloud-native/commit/b7583e251553cdf69e178330d609f3462d8af222)), closes [#7](https://www.github.com/JanssenProject/jans-cloud-native/issues/7)
* merge ([8e2d5a1](https://www.github.com/JanssenProject/jans-cloud-native/commit/8e2d5a124bb0c070672af67d72d42f34429831a0))
* **opendj:** Adjust opendj version ([1967b29](https://www.github.com/JanssenProject/jans-cloud-native/commit/1967b2917c7f9117a7f35546e054af1db6906d45))
* reference to nginx ([be72521](https://www.github.com/JanssenProject/jans-cloud-native/commit/be72521fe24834a162164ed7fbe65c503ae06903))
* **roles:** User release namespace ([7ce934c](https://www.github.com/JanssenProject/jans-cloud-native/commit/7ce934c9bca8dcffe6f52316d3297f9802dd4d5c)), closes [#13](https://www.github.com/JanssenProject/jans-cloud-native/issues/13)
* **schema:** remove fqdn schema check temp ([b788623](https://www.github.com/JanssenProject/jans-cloud-native/commit/b788623638f4bdc1428e4a6057359c678bc47102))
* skip test case execution during analysis ([#48](https://www.github.com/JanssenProject/jans-cloud-native/issues/48)) ([a868e1c](https://www.github.com/JanssenProject/jans-cloud-native/commit/a868e1c9cce944727a7e946d3cfffe7003f73030))
* **startdemo.sh:** Fix typo and update get ip ([e561144](https://www.github.com/JanssenProject/jans-cloud-native/commit/e561144496d8214dc58d86883f741e106e960222))
* **tls:** use supported name of secret ([692b380](https://www.github.com/JanssenProject/jans-cloud-native/commit/692b380e4403f104bbb182b5265bd395c607374d))
* update healthcheck endpoints ([48fe672](https://www.github.com/JanssenProject/jans-cloud-native/commit/48fe67294d140e3ac5248416075278c061d04d90))
* **values:** Adjust for values schema ([3d67b02](https://www.github.com/JanssenProject/jans-cloud-native/commit/3d67b0246c60eab8d7cc6db8bf7fc142ff24719a))


### Documentation

* fix chart annotation ([6f99782](https://www.github.com/JanssenProject/jans-cloud-native/commit/6f997824a50b60b9d92d47a1a204b15f5d84a032))
* **interception-script:** fix display ([#20](https://www.github.com/JanssenProject/jans-cloud-native/issues/20)) ([3e2627a](https://www.github.com/JanssenProject/jans-cloud-native/commit/3e2627a732dcb9a6eb72681051d27c6c71313451))
* **README:** disable istio injections on jobs ([4edb2e7](https://www.github.com/JanssenProject/jans-cloud-native/commit/4edb2e7d1a5c882956a13f76afca30be14b28883))
* **README:** Run demo as sudo ([4facb93](https://www.github.com/JanssenProject/jans-cloud-native/commit/4facb93fa94e72f3311d86468ded4f05fb87cb08))
* **rotating_cert_keys:** Add key and crt rotate instructions ([29a9ef9](https://www.github.com/JanssenProject/jans-cloud-native/commit/29a9ef9a4838b71fd78a63b4486d52cfc5e3f0c5))


### Miscellaneous Chores

* release 1.0.0-beta.13 ([8b4823c](https://www.github.com/JanssenProject/jans-cloud-native/commit/8b4823c3ccf4c95e9ce1382f88ee732b499c0410))
