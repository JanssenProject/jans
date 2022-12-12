---
tags:
  - administration
  - vm
  - operations
---

# Web server certificate

## Certificates in Janssen

Janssen components have cryptographic keys and X.509 certificates that are stored on the filesystem at the time of installation. Details for certificates associated with each component are provided below. The following certificates are available in the `/etc/certs` folder.

|APACHE		       |OPENDJ         |Jans Auth CA Certificates|
|:---------------|:--------------|:------------------------|
|httpd.crt	     |opendj.crt	   |jans-auth-keys.p12       |
|https.csr	     |opendj.pksc12	 |
|httpd.key       |               |
|httpd.key.orig  |               |

### Custom Script JSON Files
Additionally the following json files are available which are used in different custom scripts for multi-factor authentication.

- `cert_creds.json`
- `duo_creds.json`
- `gplus_client_secrets.json`
- `otp_configuration.json`
- `super_gluu_creds.json`
- `vericloud_jans_creds.json`

## Web server certificate installation

We are using [certbot](https://certbot.eff.org) using [Let's Encrypt](https://letsencrypt.org/) certificates to write this documentation. Certbot recommends using `snap` to install certbot and obtain certificates. The following instructions are for Ubuntu 20; however, any platform supporting `snap` should work.

 - Backup the `/etc/certs` folder on your server
 - Install snap: `sudo snap install core; sudo snap refresh core` 
 - Remove any certbot OS packages. This varies across distributions. For Ubuntu: `sudo apt remove certbot`
 - Install certbot: `sudo snap install --classic certbot` 
 - Issue certificate: `certbot --apache -d fqdn_of_Gluu_server`
 - Full certificate chain and key are available in: `/etc/letsencrypt/live/` location.
 - Move the cert and key to `/etc/certs` and name them `httpd.crt` and `httpd.key` respectively.
 - Reboot your server

