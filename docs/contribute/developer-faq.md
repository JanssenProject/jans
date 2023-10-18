---
tags:
- developer
- faq
---

# Developer FAQs

----------------------------

## How to enable debug logs for jans-cli and TUI configuration tools

By default, logging is not enabled for CLI or TUI tools on Janssen Server. Follow the steps below
to enable and configure logging for CLI and TUI tools:

- Log in as root user
- open config file for editing `~/.config/jans-cli.ini`
- Update value for `debug` to `true` and add new entry for `log_dir` key with value pointing to directory where logs need to be generated. e.g 
  ```
  debug = true
  log_dir = /opt/jans
  ```
- Close currently open TUI session if any and open a new one
- Logs should get available at location configured in `log_dir` in files `cli_debug.log` and `dev-tui.log`
## How to get certificate from Let's encrypt

 To generate Let’s Encrypt CA certificate run below commands:
### Suse
```
sudo zypper -n install certbot
sudo certbot certonly --webroot -w /srv/www/htdocs -d FQDN
```
### Ubuntu
```bash 
sudo apt update && sudo apt install certbot python3-certbot-apache
sudo certbot --apache -d FQDN
```

to check certbot status
```
sudo systemctl status certbot.timer
```
to renew certificate run
```
sudo certbot renew --dry-run
```
### RHEL
```
sudo yum install certbot python3-certbot-apache 
sudo certbot certonly --apache
```
## How to install Janssen Server OpenBanking for testing?

!!! Note
    - Use this installation for testing only
    - Good understanding of Janssen Server installation process in general is a prerequisite. Here we are just highlighting steps without a lot of details. Visit [installation](../admin/install/README.md) documentation for complete understanding.


This installation uses Gluu Testing certificate.

### Download Installer

```
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-linux-setup/jans_setup/install.py -O install.py
```

### Execute Installer

```
python3 install.py --profile=openbanking --args="-ob-key-fn=/opt/jans/jans-setup/openbanking/static/ob-gluu-test.key -static-kid=ob-gluu-test -jwks-uri=https://ox.gluu.org/icrby8xcvbcv/ob/ob-gluu-test.jwks --disable-ob-auth-script -ob-alias=ob-gluu-test"
```

Please enter defaults for the following questions (press just enter key), it will download certificate from **jwksUri**

```
Use external key? [Y|n] : y
  Openbanking Key File [/opt/jans/jans-setup/openbanking/static/ob-gluu-test.key] : 
  Openbanking Certificate File [/root/obsigning.pem] : 
  Openbanking Key Alias [ob-gluu-test] : 
```

### Configure Certificate

```
 /opt/jans/jans-cli/config-cli.py -CC /opt/jans/jans-setup/output/CA/client.crt -CK /opt/jans/jans-setup/output/CA/client.key
```

## Test

This test uses Gluu Testing certificate.

### device authentication
After installation we have to complete device authentication to use openbanking.

### Testing using IM mode
launch jans-cli using below command

```
/opt/jans/jans-cli/config-cli.py -CC /opt/jans/jans-setup/output/CA/client.crt -CK /opt/jans/jans-setup/output/CA/client.key
```
further testing is same as jans server

### Testing using command line mode

we can run below command at command line.
for ex:
```
/opt/jans/jans-cli/config-cli.py -CC /opt/jans/jans-setup/output/CA/client.crt -CK /opt/jans/jans-setup/output/CA/client.key –operation-id get-oauth-openid-clients
```

same way we can run  other commands.
rest is same for jans and openbanking
