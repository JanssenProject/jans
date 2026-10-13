# Gluu to Flex Migration

## 1. Inside Gluu 4.x container:

   1.1 Download Janssen archieve: `wget https://github.com/JanssenProject/jans/archive/refs/heads/jans-linux-setup-gluu2flex-migration.zip`

   1.2 Extract archieve: `unzip jans-linux-setup-gluu2flex-migration.zip`

   1.3 Execute script: `python3 jans-jans-linux-setup-gluu2flex-migration/jans-linux-setup/tools/gluu2flex-migration/gluu2flex.py`

   1.4 You need do copy directory `jans-jans-linux-setup-gluu2flex-migration/jans-linux-setup/`
       to target Janssen instance, thus make tarball:
       `tar -zcf gluu_source.tgz jans-jans-linux-setup-gluu2flex-migration/jans-linux-setup/`

## 2. Install Jannsen instance (on a fresh VM):

   2.1 Get `gluu_source.tgz` file you created in Step-1.4, and extract: `tar -zxf gluu_source.tgz`

   2.2 You need salt, so `cat jans-jans-linux-setup-gluu2flex-migration/jans-linux-setup/tools/gluu2flex-migration/migration_source/data.json`

   2.3 Download Janssen installer: `wget https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/install.py`

   2.4 Install a fresh Janssen Instance with salt of Gluu Instance, for example: `python3 install.py --args="-encode-salt=0xZ6nwAjvZlz8nOovDIZFSJT"`
      NOTE!!: Replace your salt

   2.5 Execute migratin script: `python3 jans-jans-linux-setup-gluu2flex-migration/jans-linux-setup/tools/gluu2flex-migration/gluu2flex.py`
