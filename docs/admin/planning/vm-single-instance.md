---
tags:
  - administration
  - planning
---

While an active-active cluster enables you to achieve four or five nines
uptime, you may want to consider a single VM instance if simplicity of
operation trumps availability. If you do a good job at monitoring, and
can restore from backup quickly, it's certainly possible to meet an SLA
of 99.9% availability using just one server.

Janssen has linux packages available for SUSE, Red Hat and Ubuntu. One
operational consideration is that you should `hold` the version
of Jans Auth Server, to avoid update during the normal package update process.
As many applications may rely on a central IDP, you want to plan updates and
have a rollback plan if something goes wrong. An automated, unattended update
process for the IDP is too risky.

An important security tradeoffs when you run Janssen on a single VM is that
services which you want to control access to are running on a server with
an Internet-facing ethernet interface: the Config API, database, and SCIM
server. Even though these service are running on `localhost`, if the Internet
facing interface was compromised, network protection could be bypassed.

The main advantage of running a single VM is simplicity. Your sys admins won't
need any special training. And it will be easy to install, backup,
monitor, and configure your deployment. If you have many autonomous domains,
each with their own brand and user identity management process, the simplicity
of running a single IDP might be the best solution. If three nines is acceptable
to your business, the expense of deploying and operating a cluster may not be 
justified.
