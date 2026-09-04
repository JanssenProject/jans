---
tags:
  - administration
  - developer
  - customization
---

# Custom Client Logs

## Overview

Janssen Server client-facing operations — dynamic client registration, client
updates, client authentication, and token issuance for a client — are all
driven by [interception scripts](../scripts/README.md) (also called custom
scripts). When you write or customize one of these scripts, you often need
visibility into what the script is doing at runtime: which branch of logic
ran, what values were read from the request, why a client was accepted or
rejected, and so on.

This page explains how to add your own log statements inside client-related
custom scripts, where those log messages end up, and how to control their
verbosity.

Typical scripts where custom client logging is useful:

| Script type | Typical use case |
|---|---|
| `ClientRegistrationType` | Validate or enrich a client during Dynamic Client Registration (DCR) |
| `UpdateTokenType` | Add or modify claims in a token issued to a specific client |
| `IntrospectionType` | Add extra data to introspection responses based on the requesting client |
| `ConsentGatheringType` / `PersonAuthenticationType` | Log client-specific branching in authentication or consent flows |

## How custom logging works

Every interception script has access to a dedicated logger, separate from
the Auth Server's main application logger. Writing to this logger sends your
messages to the script-specific log file instead of mixing them into the
general Auth Server log, which makes it much easier to isolate the output of
your own code.

- **Java scripts** — use SLF4J's `Logger`, obtained from
  `LoggerFactory.getLogger(CustomScriptManager.class)`.
- **Python (Jython) scripts** — a logger named `scriptLogger` is injected
  into the script's namespace automatically; you do not need to import or
  instantiate anything.

Anything written through this logger is routed to:

```text
/opt/jans/jetty/jans-auth/logs/jans-auth_script.log
```

on a VM/package installation, or to the `auth-server` container's
`script_log_target` output (`FILE` or `STDOUT`) on a container/Kubernetes
installation. This keeps client-registration and other script debug output
separate from `jans-auth.log`, `jans-auth_persistence.log`, and the other
standard Auth Server logs.

## Adding log statements — Python example

The example below extends a `ClientRegistrationType` script and logs the
client's requested redirect URIs and the decision the script makes about
them.

```python
from io.jans.model.custom.script.type.client import ClientRegistrationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.model.config import StaticConfiguration

class ClientRegistration(ClientRegistrationType):
    def init(self, customScript, configurationAttributes):
        scriptLogger.info("Custom Client Logs script. Initialized")
        return True

    def createClient(self, context):
        registerRequest = context.getRegisterRequest()
        client = context.getClient()

        scriptLogger.info("DCR request for client name: %s, redirect_uris: %s",
                           registerRequest.getClientName(),
                           registerRequest.getRedirectUris())

        if registerRequest.getRedirectUris() is None:
            scriptLogger.warning("Rejecting client '%s' - no redirect_uris supplied",
                                  registerRequest.getClientName())
            return False

        scriptLogger.debug("Client '%s' accepted with inum %s",
                            registerRequest.getClientName(), client.getClientId())
        return True

    def destroy(self, configurationAttributes):
        scriptLogger.info("Custom Client Logs script. Destroyed")
        return True
```

`scriptLogger` supports the usual levels — `debug`, `info`, `warning`
(`warn`), `error`, `fatal` — matching SLF4J semantics.

## Adding log statements — Java example

```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.client.ClientRegistrationType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ClientRegistration implements ClientRegistrationType {

    // Writing to this logger sends output to jans-auth_script.log
    private static final Logger scriptLogger =
            LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(CustomScript customScript,
                         Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Custom Client Logs script. Initialized");
        return true;
    }

    // createClient(...) and other interface methods omitted for brevity
}
```

> Do not use `System.out.println` or Python's built-in `print` for
> diagnostics in production scripts. Those calls bypass the logging
> framework entirely, are not level-controlled, and typically end up
> mixed into `jans-auth.log` or lost.

## Configuring the log level and target

Script log verbosity is controlled by the Auth Server's `script_log_level`
(and, on containerized deployments, `script_log_target`) setting, which is
managed the same way as other Auth Server logging properties.

**Using Jans TUI**

1. Run `jans tui`.
2. Navigate to **Auth Server → Logging**.
3. Set the logging level (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`,
   or `OFF`) that should apply to script output, then save.

**Using Jans CLI**

```bash
jans cli --operation-id get-config-logging
```

Update the `loggingLevel` field and `PUT` it back with `put-config-logging`
to raise or lower verbosity — for example, switch to `DEBUG` or `TRACE`
temporarily while diagnosing a client-registration issue, then return to
`INFO` afterward to avoid excessive log volume in production.

**On Kubernetes**

Override `scriptLogLevel` (and optionally `scriptLogTarget`) under
`global.auth-server.appLoggers` in your Helm values file and apply with
`helm upgrade`.

## Viewing the logs

- **VM / package install:** tail the file directly:
  ```bash
  tail -f /opt/jans/jetty/jans-auth/logs/jans-auth_script.log
  ```
- **Kubernetes:**
  ```bash
  kubectl logs -f deployment/<helm-release-name>-auth-server -n <namespace>
  ```
  (only if `script_log_target` is set to `STDOUT`; otherwise read the file
  from inside the pod).

## Best practices

- **Log at the right level.** Use `debug`/`trace` for verbose, per-request
  detail you only need while developing; use `info` for high-level
  lifecycle events (script init/destroy, a client accepted/rejected); use
  `warning`/`error` for conditions that need operator attention.
- **Never log secrets.** Avoid writing client secrets, tokens, or full
  authorization headers to the script log.
- **Include an identifier.** Log the client ID or client name alongside
  each message so entries can be correlated with a specific client when
  troubleshooting a shared script.
- **Keep it low-volume in production.** High-frequency `debug`/`trace`
  logging in a script that runs on every token request can generate large
  log volumes; lower the level once you've finished diagnosing an issue.


## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Zulip](https://chat.gluu.org/join/wnsm743ho6byd57r4he2yihn/). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
