---
tags:
  - administration
  - auth-server
  - logging-levels
---

# Log Levels

Log level for AS can be set in `loggingLevel` AS configuration property.
AS under the hood is using `log4j2` and `slf4j`. Thus logging levels in `loggingLevel` stick to `log4j2` and are following:

| Log Level | Description                                                                        |  
|---------- |------------------------------------------------------------------------------------|  
|OFF        | No events will be logged.                                                          |
|FATAL      | A fatal event that will prevent the application from continuing.                   |  
|ERROR      | An error in the application, possibly recoverable.                                 |  
|WARN       | An event that might possible lead to an error.                                     |  
|INFO       | An event for informational purposes.                                               |  
|DEBUG      | A general debugging event.                                                         |  
|TRACE      | A fine-grained debug message, typically capturing the flow through the application.|  
|ALL        | All events should be logged.                                                       |


# Select Log Levels on TUI

Go to Jans TUI  `jans tui`. Select `Auth Server` tab then select `Logging` tab after that select Auth Server `Log Level`
finally `save logging` and exit from TUI.

![logLevels](https://github.com/JanssenProject/jans/assets/43112579/26d014eb-43f2-4a02-b7b0-e24201b37298)


## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).