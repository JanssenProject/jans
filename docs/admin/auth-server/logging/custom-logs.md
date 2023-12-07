---
tags:
  - administration
  - auth-server
  - logging
  - custom-logs
---

# Customize logs

Sometimes it can be useful to customize logging behavior or override AS loggers. 
It is possible to fully override AS logging configuration by specifying own `log4j2.xml` file in `externalLoggerConfiguration` AS configuration property.
It must point to valid `log4j2.xml` file. 

Note: invalid external `log4j2.xml` can lead to AS start up issues and no logs in [standard log files](standard-logs.md) or otherwise in other log files if such are defined by `log4j2.xml`.


## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).