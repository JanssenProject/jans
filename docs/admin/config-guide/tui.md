---
tags:
- installation
- administration
- configuration
- config
- tui
---

## Overview

The "Text User Interface" or "TUI" is a tool for admins to perform *ad
hoc* configuration of the Janssen Project software components.

You can run it by executing:

```
/opt/jans/jans-cli/jans_cli_tui.py

```

The TUI calls the Jans Config API. A TUI client is configured out of the
box when you run either the VM or container Jans setup. The TUI utilizes the
OAuth Device Flow for user authentication--the default `admin` user created
during setup has the correct roles to use all the TUI features. Client
credentials, tokens and other data is stored in file `~/.config/jans-cli.ini` in
encoded format. The user-role mappings roles are defined in a Jans Auth Server
[introspection script](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/introspection/introspection-role-based-scope/introspection_role_based_scope.py)

```
~/.config/jans-cli.ini
```

## Plugins

It's possible to extend the TUI by writing a plugin. Each plugin is loaded
dynamically according to the numeric priority of the folders in
`/opt/jans/jans-cli/plugins`. To enable a plugin, you need to edit
`jans-cli.ini`. The default plugins are:

```
jca_plugins = user-mgt,scim,fido2,admin-ui

```

## Administration

When you run the TUI, this is the home panel:
![image](../../assets/Jans_TUI_Main_panel.png)

### Auth Server

The most complicated configuration belongs to Auth Server. This should be no
surprise, because this component is responsible for the implementation of
OpenID Provider, OAuth Authorization Server, and UMA Authorization Server
endpoints.

 - In below screenshot, we are getting the list of existing clients. ![image](../../assets/Jans_TUI_Auth_Server_Get_client_list.png)
 - It's possible to get details of any client as well. Just hit enter on any client and  you will see information like this. ![image](../../assets/Jans_TUI_Auth_Server_Client_detail.png)
 - With the button which is located on right upper side named "Add Client", it's possible to create new client. ![image](../../assets/Jans_TUI_Auth_Server_Add_new_client.png)

### FIDO

The Janssen FIDO Server implements FIDO 2 and FIDO U2F endpoints. Using the
TUI, you can view/update details of the FIDO configuration.

There are two configurations included here:

 - Dynamic Configuration ![image](../../assets/Jans_TUI_Fido_Dynamic_Configuration.png)
 - Static Configuration ![image](../../assets/Jans_TUI_Fido_Static_Configuration.png)

### SCIM

System for Cross-domain Identity Management, in short SCIM, is a specification
that simplifies the exchange of user identity information across different
domains. The Janssen Server provides implementation for the SCIM specification.

With Janssen CLI-TUI, it's possible to view / update SCIM configuration. ![image](../../assets/Janssen_TUI_SCIM_1.png)

## Standalone Installation

Installation of a Jans TUI ( Text-Based User Interface ) need a Jans server to be installed as base. After that download and build Jans-ClI-TUI and make it.

### Build:

 - `pip3 install shiv`
 - `wget https://github.com/JanssenProject/jans/archive/refs/heads/jans-cli-tui-works.zip`
 - `unzip jans-cli-tui-works.zip`
 - `cd jans-jans-cli-tui-works/jans-cli-tui/`
 - `make zipapp`

### Run

 - `./config-cli-tui.pyz`
