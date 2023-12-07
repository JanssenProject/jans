## Upgrading TUI from Github

Step to upgrade TUI from github main branch

1. `wget https://github.com/JanssenProject/jans/archive/refs/heads/main.zip -O main.zip`

2. `unzip main.zip`

3. `cp -v -r jans-main/jans-cli-tui/cli_tui/* /opt/jans/jans-cli/`

4. `cp jans-main/jans-config-api/docs/jans-config-api-swagger.yaml /opt/jans/jans-cli/cli/ops/jca/`

5. `cp jans-main/jans-config-api/plugins/docs/*.* /opt/jans/jans-cli/cli/ops/jca/`

6. `cp jans-main/jans-auth-server/docs/swagger.yaml /opt/jans/jans-cli/cli/ops/auth/`
