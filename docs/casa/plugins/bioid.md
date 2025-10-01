---
tags:
- Casa
- Biometrical
---


# BioID plugin


This plugin allows users to enroll their BioID facial biometrics. 

## Requirements
- A Janssen server installation with Casa installed
- A BioID account. Register on the [BioID site](https://bwsportal.bioid.com/register) 
- Application credentials from the BWS Portal. Please register an application against your account. You will need the app identifier, app secret, storage and partition. 

## Installation

1. [Download](https://maven.jans.io/maven/io/jans/casa/plugins/bioid-plugin/replace-janssen-version/bioid-plugin-replace-janssen-version-jar-with-dependencies.jar) the plugin jar.
1. Log into Casa as an administrator, navigate to `Administration Console > Casa plugins` and add the plugin jar
1. Restart the casa service: `sudo systemctl restart jans-casa`
1. Using the TUI, navigate to `Auth Server` > `Clients`, open the details for `Client for Casa`, and add the following redirect URI: `https://<hostname>/jans-casa/pl/bioid-plugin/user/interlude.zul`. Replace `<hostname>` with the hostname of your server, and save the client.
1. Run the following commands to generate the Agama flow file:

```
git clone --depth 1 --branch main --no-checkout https://github.com/JanssenProject/jans.git
cd jans/jans-casa/plugins/bioid/extras/agama
zip -r casa-bioid.gama ./*
```
1. Transfer the `casa-bioid.gama` file to the server, and deploy it using the TUI
1. Using the TUI, export the sample configuration, edit it according to the specification below and import it back in

## Agama Configuration
```
{
  "io.jans.agama.bioid.enroll": {
    "host": "https://<HOSTNAME>/jans-auth/fl/callback",
    "endpoint": "https://bws.bioid.com/extension/",
    "appIdentifier": "",
    "appSecret": "",
    "storage": "",
    "partition": ""
  }
}
```

- `host`: Replace `<HOSTNAME>` with the hostname of your server
- `endpoint`: BioID API endpoint. Leave as default
- `appIdentifier`: The app identifier string from BWS Portal - Configuration
- `appSecret`: The app secret from BWS Portal - Configuration
- `storage`: Storage value from BWS Portal - Configuration
- `partition`: Partition value from BWS Portal - Configuration

## How to use
The plugin provides a user menu. When clicking the `Click to Enroll` button, Casa launches the `io.jans.agama.bioid.enroll` flow on the authorization server. This flow queries the BioID database for existing enrollments for the user. If the user has not enrolled, the flow presents the BWS GUI for enrollment. Upon success, the flow redirects back to a Casa landing page. Deletion of credentials is not supported as of now because Casa is unaware of enrollment status of a user.
