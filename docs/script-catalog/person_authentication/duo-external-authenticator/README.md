# Integrating Duo Security as an External Authenticator

# Integrate Duo Security with Janssen
Duo Security is a SaaS authentication provider. This document will explain how to use Gluu's Duo interception script to configure the Gluu Server for a two-step authentication process with username and password as the first step, and Duo as the second step.

In order to use this authentication mechanism your organization will need a Duo account and users will need to download the Duo mobile app.

- open trial account on Duo security
- create a free trial from duo.com
- sign up using email and any org name. Select org size as just me
- complete email verification
- click on get started to setup account and continue till you are presented with a QR code
- download duo mobile app
- click on setup a new account on app
- scan the QR code with mobile app

- go to janssen installation
- enable duo script by

```shell
cd /opt/jans/python/libs
wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/auth/lib/duo_web.py
sudo systemctl restart jans-auth
```
- Now use `jans-cli` to enable duo script using instructions [here](../../../admin/config-guide/jans-cli/im/im-custom-scripts.md#update-custom-scripts). Set the `enabled` property for duo script to `true`
- confirm that the script is enabled in [cli script listing](../../../admin/config-guide/jans-cli/im/im-custom-scripts.md#get-list-of-custom-scripts) 
- check that `enabled` is true and copy the inum for `duo` script
- get custom script schema using
```shell
sudo /opt/jans/jans-cli/config-cli.py --schema /components/schemas/CustomScript > ./duo-prop.json
```
- edit the json
```shell
sudo vim ./duo-prop.json
```

- populating `/etc/certs/duo_creds.json`
    - goto duo.com > login > after login go to applications -> select web-sdk, here you will find client id, secret and api hostname
    - ikey(integration key) is client id
    - skey(secret key) is secret key
    - akey is a randomly generated id
        - use instructions at https://duo.com/docs/duoweb-v2
            - copy content below in a new .py file
          ```
          import os, hashlib
          print(hashlib.sha1(os.urandom(32)).hexdigest())
          ```
          and then run the py file using python3
          ```
          python3 temp/p.py
          ```
          it'll print a alpha-numeric string e.g `f9998e03344e39cac0eee42e3c725b8ed975d6c3` which you can use as akey

