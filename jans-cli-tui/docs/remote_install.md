Install Jans-TUI with pip from GitHub
--------------------------------------
```
pip3 install https://github.com/JanssenProject/jans/archive/refs/heads/main.zip#subdirectory=jans-cli-tui
```

Execute:

```
config-cli-tui
```

Get Credidentials for CLI
-------------------------
On Jans server

```
# cat /opt/jans/jans-setup/setup.properties.last | grep role
role_based_client_encoded_pw=dDpwNN3lv94JF+ibgVFT7A\=\=
role_based_client_id=2000.076aa5d9-fa8d-42a0-90d2-b83b5ea535d5
role_based_client_pw=mrF8tcBd6m9Q
```

`role_based_client_id` wile go **Client ID** and `role_based_client_pw` will go **Client Secret**
