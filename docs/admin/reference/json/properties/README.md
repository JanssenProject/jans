---
tags:
  - administration
  - auth-server
  - reference
  - properties
---

# Properties

Janssen Server modules are configured using properties. Each module has its own set of properties (navigate through the
left navigation panel).

## Setting Property Values

An administrator can set properties using [TUI](../../../config-guide/config-tools/jans-tui/README.md). Each module has its own property
configuration section in TUI.

Properties are primarily of two types based on how the change in the value is loaded in the module at runtime.

- **Static Properties**: To load the changed property value, the respective Janssen Server module or server itself needs to be
  restarted manually
- **Dynamic Properties**: Changes to the dynamic property values get reloaded automatically. Reload process triggers after
  every 30 seconds. This does not need a restart of any server module.


!!! Contribute
If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)
