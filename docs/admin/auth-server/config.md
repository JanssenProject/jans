---
tags:
  - administration
  - auth-server
  - configuration
---

# Configuration Overview

In some ways, this whole Auth Server admin guide is about configuration. But
you have to start somewhere! This page provides a quick overview before you dive
in!

## Config Server

The Configuration Server is a JSON/REST API that writes to the database,
generates the keys, and does much of the other work you need to make your
Janssen Project infrastructure solve your digital identity challenges.
See the [Config API Guide](../config-api/README.md for more information.

## Tools

1. API - you can call the configuration endpoints with the API tool of your
choice, like `curl`. With this mechanism, you can use client credential grant
to obtain an OAuth token--just make sure it has the required scope to call the
method / endpoint. See the [Curl Guide](../config-guide/curl.md) for more
info.
2. CLI - The CLI, or Command Line Interface, calls the API for you. It uses the
device flow to authenticate the person who is behind the config changes. See the
[Configuration CLI Guide](../config-guide/jans-cli/README.md) for more info.
3. TUI - The TUI, or Text User Interface, is a menu-based interactive tool that
provides a more intuitive experience. When you make changes via the TUI, it also
creates a log of the equivalent CLI command, in case you want a shortcut next
time around. See the [TUI Guide](../config-guide/jans-tui.md)
for more info.

## Client versus Server Configuration

Some behaviors are controlled at the server level. For example, should your
Auth Server allow dynamic client registration? Other features you can configure
either at the server or client level. For example, do you want user claims in
an `id_token`? Maybe you always want them (server), or maybe only sometimes
(client). See the [JSON Configuration/Properties](../reference/json/README.md)
for an overview of the server level configuration options. For client
configuration options, see the [Client Schema](./client-management/client-schema.md)
page.

## Interception Scripts

Janssen is a very flexible platform, that lets you customize many of the flows
to meet your exact requirements. The black magic behind this flexibility is
interception scripts--these are standard programming interfaces that enable
you to add or change the behavior of Jans endpoints. Just to give one example,
perhaps you need to add data from an external API into an OAuth access token?
In this case, you could use the Update Token interception script. There are
around 20 of these scripts. For a full list, see the
[Scripts Documentation](../developer/scripts/README.md).

## Customization

You don't want out of the box look and feel provided by Janssen. We know we
are UX-challenged, backend developer geeks. Check out the
[Customization Guide](../developer/customization/README.md) for info about how
to add adapt the user facing content to achieve your UX dreams.

## Operations

Janssen Project software has to run somewhere, right now... either on VM's or
in containers. There are specifics instructions about each contained in these
docs.

## Database and Cache

One of the biggest challenges in running the Janssen Platform is persistence.
Make sure you understand how Janssen Project stores your data, or in some
cases, caches it.
