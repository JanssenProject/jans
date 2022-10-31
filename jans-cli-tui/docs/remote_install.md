Install Requirements
---------------------
```
pip3 install prompt_toolkit
pip3 install urllib3
pip3 install requests
pip3 install pygments
sudo pip3 install pyjwt
pip3 install pyDes
```

Get Latest Jans-TUI
-------------------------
```
wget https://github.com/JanssenProject/jans/archive/refs/heads/jans-cli-tui-works.zip
unzip jans-cli-tui-works.zip
cd jans-jans-cli-tui-works/jans-cli-tui/
python3 jans-cli-tui.py
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
