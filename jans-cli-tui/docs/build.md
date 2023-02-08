#### Build
```
pip3 install shiv
wget https://github.com/JanssenProject/jans/archive/refs/heads/main.zip -O jans-main.zip
unzip jans-main.zip
cd jans-main/jans-cli-tui/
make zipapp
```

### Execute

```
./jans-cli-tui.pyz
```

It will ask credentials unless you have no ~/.config/jans-cli.ini. Login to Jans server and get
credentials:
```
cat /opt/jans/jans-setup/setup.properties.last | grep role
tui_client_encoded_pw=4jnkODv3KRV6xNm1oGQ8+g\=\=
tui_client_id=2000.eac308d1-95e3-4e38-87cf-1532af310a9e
tui_client_pw=GnEkCqg4Vsks
```
