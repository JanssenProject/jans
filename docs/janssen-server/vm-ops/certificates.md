---
tags:
  - administration
  - vm
  - operations
  - certificates
---

# Janssen Certificates 

Janssen components have cryptographic keys and X.509 certificates that are stored on the filesystem at the time of installation. Details for certificates associated with each component are provided below. The following certificates are available in the `/etc/certs` folder.

| APACHE		       | Jans Auth CA Certificates |
|:---------------|---------------------------|
| httpd.crt	     | jans-auth-keys.p12        |
| https.csr	     |
| httpd.key      |
| httpd.key.orig |

## Custom Script JSON Files
Additionally the following json files are available which are used in different custom scripts for multi-factor authentication.

- `cert_creds.json`
- `duo_creds.json`
- `gplus_client_secrets.json`
- `otp_configuration.json`
- `super_gluu_creds.json`
- `vericloud_jans_creds.json`

# Updating certificates

On a fresh VM installation, Janssen generates self signed certificates. You will want to change these to real certificates. For this documentation we will use [certbot](https://certbot.eff.org) using [Let's Encrypt](https://letsencrypt.org/) certificates. Certbot recommends using `snap` to install certbot and obtain certificates. The following instructions are for Ubuntu 20; however, any platform supporting `snap` should work.

 - Backup the `/etc/certs` folder on your server
 - Install snap
   ```shell
   sudo snap install core; sudo snap refresh core
   ``` 
 - Remove any certbot OS packages. This varies across distributions. 
   For Ubuntu: 
   ```shell
   sudo apt remove certbot
   ```
 - Update repo:
   For Ubuntu:
   ```shell
   sudo apt update
   ```
 - Install python package for apache:
    For Ubuntu:
    ```shell
    sudo apt-get install python3-certbot-apache
    ```
 - Issue certificate: 
   ```shell
   certbot --apache -d fqdn_of_Gluu_server
   ```
 - Full certificate chain and key are available in: `/etc/letsencrypt/live/` location.
 - Reboot your server

# Installing intermediate certificates

Please follow these steps to install intermediate certificates:

- Place your intermediate certificate file in `/etc/certs`
- Modify `/etc/apache2/sites-available/https_jans.conf` and add `SSLCertificateChainFile /etc/certs/name_of_your_interm_root_cert.crt` under the line containing `SSLCertificateKeyFile`
- [Restart](../../janssen-server/vm-ops/restarting-services.md#restart) the `httpd/apache2` service

