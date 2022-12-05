---
tags:
  - administration
  - vm
  - operations
---

# Overview

While fancy container orchestrated deployments are all the rage, there are still
lots of reasons to deploy Janssen on a good, old-fashioned, virtual machine,
or even an actual bare-metal server! Here is a partial list:
1. **Lower bar for system administration** - Cloud native requires specialized
training which your operational staff doesn't need if you stick with plain
Linux packages.
2. **Operational simplicity** - Simplicity = lower cost of ownership and in many
cases, better security. Running one server, doing a good job at monitoring and
backup--this will enable you to achieve 99.9% uptime (8 hours of downtime per
year), which may be enough to meet the SLA you've negotiated with your management.
3. **Air-gapped deployments** - Maybe you don't have a cloud because you are
deploying your server on a small air-gapped network.
4. **Developer** - If you are a developer who is used to running a bunch of VM's
on your workstation, this is still a nice option to have.
5. **Linux Zealot** - You love Linux and you're happy with it! Enough said.

If you fit into any of the above categories, or another one that leads you to
deploy Janssen Linux packages, this operational guide is a quick reference for
some of the things you need to do for the "care and feeding" of a healthy
federated identity platform.
