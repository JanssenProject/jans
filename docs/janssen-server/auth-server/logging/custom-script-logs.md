---
tags:
  - administration
  - auth-server
  - custom-script-log
---

# Custom scripts Logs

## Overview

Janssen Server behavior is customized through
[interception scripts](../../developer/scripts/README.md) (also called custom scripts).
While writing or debugging any custom script, you'll often want visibility
into what your code is doing at runtime — which branch of logic ran, what
values were read, why a particular decision was made, and so on.

This page explains how to add your own log statements inside a custom
script, where those messages end up, and how to control their verbosity.
The mechanism described here works the same way regardless of which
interception script type you're implementing person authentication,
consent gathering, client registration, update token, introspection,
dynamic scope, application session, and the rest all support it identically.

## How custom logging works

Every interception script has access to a dedicated logger, separate from
the Auth Server's main application logger. Writing to this logger sends
your messages to a script-specific log file instead of mixing them into the
general Auth Server log, which makes it much easier to isolate the output
of your own code from everything else the server is doing.

- **Java scripts** use SLF4J's `Logger`, obtained from
  `LoggerFactory.getLogger(CustomScriptManager.class)`.
- **Python (Jython) scripts** a logger named `scriptLogger` is injected
  into the script's namespace automatically; you do not need to import or
  instantiate anything.

Anything written through this logger is routed to:

```text
/opt/jans/jetty/jans-auth/logs/jans-auth_script.log
```

on a VM/package installation, or to the `auth-server` container's
`script_log_target` output (`FILE` or `STDOUT`) on a container/Kubernetes
installation. This keeps custom script output separate from `jans-auth.log`,
`jans-auth_persistence.log`, and the other standard Auth Server logs, no
matter which script type wrote it.

## Adding log statements Python example

The example below shows the pattern in a generic script. The same
`scriptLogger` calls apply whether `init`, `destroy`, or any interface
method belongs to a `PersonAuthenticationType`, `ClientRegistrationType`,
`UpdateTokenType`, `ConsentGatheringType`, or any other script type.

```python
from io.jans.model.custom.script.type.SOME_PACKAGE import SomeScriptType

class CustomScript(SomeScriptType):
    def init(self, customScript, configurationAttributes):
        scriptLogger.info("Custom script. Initialized")
        return True

    def someInterfaceMethod(self, context):
        scriptLogger.debug("Entering someInterfaceMethod with context: %s", context)

        if context is None:
            scriptLogger.warning("Context was not supplied - aborting")
            return False

        scriptLogger.info("Processing completed successfully")
        return True

    def destroy(self, configurationAttributes):
        scriptLogger.info("Custom script. Destroyed")
        return True
```

`scriptLogger` supports the usual levels — `debug`, `info`, `warning`
(`warn`), `error`, `fatal` — matching SLF4J semantics.

## Adding log statements Java example

```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.SOME_PACKAGE.SomeScriptType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CustomScript implements SomeScriptType {

    // Writing to this logger sends output to jans-auth_script.log
    private static final Logger scriptLogger =
            LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(CustomScript customScript,
                         Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Custom script. Initialized");
        return true;
    }

    // Other interface methods omitted for brevity
}
```

> Do not use `System.out.println` or Python's built-in `print` for
> diagnostics in production scripts. Those calls bypass the logging
> framework entirely, are not level-controlled, and typically end up
> mixed into `jans-auth.log` or lost.

## Configuring the log level and target

Script log verbosity is controlled by the Auth Server's `script_log_level`
(and, on containerized deployments, `script_log_target`) setting, which is
managed the same way as other Auth Server logging properties, and applies
uniformly to every custom script regardless of type.

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
to raise or lower verbosity for example, switch to `DEBUG` or `TRACE`
temporarily while diagnosing a script issue, then return to `INFO`
afterward to avoid excessive log volume in production.

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
  lifecycle events (script init/destroy, a key decision point); use
  `warning`/`error` for conditions that need operator attention.
- **Never log secrets.** Avoid writing passwords, client secrets, tokens,
  or full request/response headers to the script log.
- **Include a correlation identifier.** Logging a request ID, session ID,
  or similar identifier alongside each message makes it far easier to trace
  a single flow through the log when several script types or executions
  are interleaved.
- **Keep it low-volume in production.** High-frequency `debug`/`trace`
  logging in a script that runs on every request can generate large log
  volumes; lower the level once you've finished diagnosing an issue.

## Have questions in the meantime?

If something here doesn't match what you're seeing, ask through
[GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or
the [community chat on Zulip](https://chat.gluu.org/join/wnsm743ho6byd57r4he2yihn/).

## Want to contribute?

If you'd like to improve or extend this page, see the
[Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
