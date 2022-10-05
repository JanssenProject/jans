---
tags:
- developer
- faq
---

# Developer FAQs

----------------------------

## How to install Janssen Server OpenBanking for testing?

!!! Note
    - Use this installation for testing only
    - Good understanding of Janssen Server installation process in general is a prerequisite. Here we are just highlighting steps without a lot of details. Visit [installation](../admin/install/README.md) documentation for complete understanding.


This installation uses Gluu Testing certificate.

### Download Installer

```
wget https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/install.py -O install.py
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