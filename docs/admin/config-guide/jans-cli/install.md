---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

Configure the Janssen server using the `jans-cli-tui` a Text based User Interface app which can be installed on any PC

### To Build
```
pip3 install shiv
wget https://github.com/JanssenProject/jans/archive/refs/heads/main.zip -O jans-main.zip
unzip jans-main.zip
cd jans-main/jans-cli-tui/
make zipapp
```

### To Execute

```
./config-cli-tui.pyz
```

You will be prompted for credentials if you do not have ` ~/.config/jans-cli.ini`. 
Contact your administrator for credentials.

```
cat /opt/jans/jans-setup/setup.properties.last | grep role
tui_client_encoded_pw=4jnkODv3KRV6xNm1oGQ8+g\=\=
tui_client_id=2000.eac308d1-95e3-4e38-87cf-1532af310a9e
tui_client_pw=GnEkCqg4Vsks
```

### Installing with pip from GitHub

```
pip3 install https://github.com/JanssenProject/jans/archive/refs/heads/main.zip#subdirectory=jans-cli-tui
```

Execute:

```
config-cli-tui
```

Obtain Credidentials for CLI from the Janssen server:

```
# cat /opt/jans/jans-setup/setup.properties.last | grep role
tui_client_encoded_pw=dDpwNN3lv94JF+ibgVFT7A\=\=
tui_client_id=2000.076aa5d9-fa8d-42a0-90d2-b83b5ea535d5
tui_client_pw=mrF8tcBd6m9Q
```

`tui_client_id` is the **Client ID** and `tui_client_pw` is the **Client Secret**
