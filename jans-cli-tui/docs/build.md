#### Build
```
pip3 install shiv
wget https://github.com/JanssenProject/jans/archive/refs/heads/jans-cli-tui-works.zip
unzip jans-cli-tui-works.zip
cd jans-jans-cli-tui-works/jans-cli-tui/
make zipapp
```

### Execute

```
./config-cli-tui.pyz
```

It will ask credentials unless you have no ~/.config/jans-cli.ini. Login to Jans server and get
credentials:
```
cat /opt/jans/jans-setup/setup.properties.last | grep role
role_based_client_encoded_pw=4jnkODv3KRV6xNm1oGQ8+g\=\=
role_based_client_id=2000.eac308d1-95e3-4e38-87cf-1532af310a9e
role_based_client_pw=GnEkCqg4Vsks
```