---
tags:
  - administration
  - Jans Link
---

# Jans Link Components

The Jans Link is a group of independent components that provide synchronization
services to
update the Janssen User Store from an external authoritative data source.
Changes to the external
data in the external store are detected by comparing periodic snapshots. Currently,
Jans Link components are unidirectional where the Link component treats the
external data as authoritative. Meaning, that changes made in the Janssen user store
do not flow back to the external data source. Custom data
transformations, like adding claims or claims enhancement, can be applied
during the synchronization process. It is possible through
the use of the Link Interception Script, which is invoked for each user update.

Like all other Janssen Components, configuration and management of Jans Link
components is possible through Text-based user Interface (TUI). Refer to
subsections of this documentation for more details about each Jans Link
component.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
(https://github.com/JanssenProject/jans/discussions)