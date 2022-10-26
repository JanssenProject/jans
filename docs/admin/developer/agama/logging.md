---
tags:
  - administration
  - developer
  - agama
---

# Logging

There are two sources of log data that may be of interest to flow developers: the [engine](README.md#agama-engine) and the flows themselves. The engine logs information related to the processing of flows plus tasks that run in the background. This usually provides low-level information and only deserve to be inspected when an error or unexpected behavior occurs. On the other hand, flows add data by means of the `Log` instruction used in the definition (source code) of flows.

Engine's log data is sent to the main log file of the authentication server, that is, file `/opt/jans/jetty/jans-auth/log/jans-auth.log`. Flows' log data is found in the scripting log of the server, namely `/opt/jans/jetty/jans-auth/log/jans-auth_script.log`. This log also contains the output of `print` statements used in standard Jython custom scripts.

Depending on the specificity required, you may have to change the logging level of the server so more or less details appear. This can be done by altering the `loggingLevel` property of the [auth server configuration](../../config-guide/jans-cli/im/im-jans-authorization-server.md). `DEBUG` usually suffices for troubleshooting. 

Useful resources:

- [`Log` instruction](./dsl-full.md#logging) from the full reference
- [How to send data to the flows' log from Java](./faq.md#how-to-append-data-to-a-flows-log-directly)
