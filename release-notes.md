:robot: I have created a release *beep* *boop*
---


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


---
This PR was generated with [Release Please](https://github.com/googleapis/release-please). See [documentation](https://github.com/googleapis/release-please#release-please).
