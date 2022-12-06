---
tags:
- installation
- administration
- configuration
- config
- tui
---

# Configuration Text-Based User Interface (TUI)

## Overview

## Installation

Installation of a Jans TUI ( Text-Based User Interface ) need a Jans server to be installed as base. After that download and build Jans-ClI-TUI and make it. 

### Build: 

 - `pip3 install shiv`
 - `wget https://github.com/JanssenProject/jans/archive/refs/heads/jans-cli-tui-works.zip`
 - `unzip jans-cli-tui-works.zip`
 - `cd jans-jans-cli-tui-works/jans-cli-tui/`
 - `make zipapp`

### Run 

 - `./config-cli-tui.pyz` 

## Administration

It's possible to manage Janssen server with Jans-Cli-TUI. There are lot of available options here which administrator can use for daily activities. 
Some of them are stated below. 

This is what it looks like when run `config-cli-tui.pyz`, this is the main panel. ![image](../../assets/Jans_TUI_Main_panel.png)

### Auth Server

With "Auth Server" option, administrator can operate openid client configuration in their Janssen server. 

 - In below screenshot, we are getting the list of existing clients. ![image](../../assets/Jans_TUI_Auth_Server_Get_client_list.png)
 - It's possible to get details of any client as well. Just hit enter on any client and  you will see information like this. ![image](../../assets/Jans_TUI_Auth_Server_Client_detail.png)
 - With the button which is located on right upper side named "Add Client", it's possible to create new client. ![image](../assets/Jans_TUI_Auth_Server_Add_new_client.png)

### FIDO

### SCIM

### Scripts

### Users
