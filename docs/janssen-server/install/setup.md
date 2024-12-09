---
tags:
  - administration
  - installation
  - setup
---

# Janssen Setup Script

## Running Setup

 After installation, executing `setup.py` will launch the SETUP Command Line by default. 
 
 To run the script, run the following command:
 
 ```bash
 python3 /opt/jans/jans-setup/setup.py
 ```
 
 A warning will pop up if the free disk space is less than the recommended 40 GB. The installer will check that all dependant packages are installed or not, and if missing it will ask to install. When prompted Y/y at the command prompt will install all required packages.
   
1. The installer will detect which operating system, init type, and Apache version are currently on the server.
  
2. The setup script will bring up a prompt to provide information for certificates as well as the IP Address and the hostname for the Janssen Authorization Server. Hit Enter to accept the default values.
  
  ```bash
  Enter IP Address:
  Enter hostname:
  Enter your city or locality:
  Enter your state or province two letter code:
  Enter two letter Country Code
  Enter Organization name:
  Enter email address for support at your organization:
  Enter maximum RAM for applications in MB:
  Enter Password for Admin User:
  ```
  
3. Next, pick a persistence mechanism. Choose from MySQL, PGSql that can be installed locally or remotely.
    
4. Next, pick which services should be installed for this deployment:

  ```bash
Install Jans Config API? [Yes] : 
Install Scim Server? [Yes] : 
Install Fido2 Server? [Yes] : 
Install Jans Link Server? [Yes] : 
Install Gluu Casa? [No] :
  ```
   
5. Finally, review the summary screen that gives an overview of the selections made during the setup process.

Note! After setup completed, you will be prompted to remove setup files (directories `/opt/dist` and `/opt/jans/jans-setup`).
If you are not going to do any post-setup operations, type **yes** to remove setup files. If you don't respond in 10 seconds,
setup files will be preserved.

### Avoiding common issues

Avoid setup issues by acknowledging the following:

- IP Address: Do not use localhost for either the IP address or hostname.

- Hostname:
  
   - Make sure to choose the hostname carefully. Changing the hostname after installation is not a simple task.
     
   - Use a real hostname--this can always be managed via host file entries if adding a DNS entry is too much work for testing.
     
   - For clustered deployments, use the hostname of the cluster that will be used by applications connecting to Janssen Authorization Server.
  
!!! Warning    
    Use a FQDN (fully qualified domain name) as hostname and refrain from using 127.0.0.1 as IP address or usage of private IP is not supported and not recommended.

## Script Command Line Options

To check usage of this script run help command
```
python3 /opt/jans/jans-setup/setup.py --help
```

```
usage: jans_setup.py [-h] [--version] [-c] [-d D] [-f F] [-n] [-N] [-u] [-csx] [-encode-salt ENCODE_SALT]
                     [-remote-rdbm {mysql,pgsql} | -local-rdbm {mysql,pgsql}] [-ip-address IP_ADDRESS]
                     [-host-name HOST_NAME] [-org-name ORG_NAME] [-email EMAIL] [-city CITY] [-state STATE]
                     [-country COUNTRY] [-rdbm-user RDBM_USER] [-rdbm-password RDBM_PASSWORD] [-rdbm-port RDBM_PORT]
                     [-rdbm-db RDBM_DB] [-rdbm-host RDBM_HOST] [--reset-rdbm-db] [--shell] [--dump-config-on-error]
                     [--no-progress] [-admin-password ADMIN_PASSWORD] [-jans-max-mem JANS_MAX_MEM]
                     [-properties-password PROPERTIES_PASSWORD] [-approved-issuer APPROVED_ISSUER] [--force-download]
                     [--download-exit] [-jans-app-version JANS_APP_VERSION] [-jans-build JANS_BUILD]
                     [-setup-branch SETUP_BRANCH] [--disable-config-api-security] [--cli-test-client]
                     [--import-ldif IMPORT_LDIF] [-enable-script ENABLE_SCRIPT] [-disable-script DISABLE_SCRIPT]
                     [-java-version {11,17}] [-stm] [-w] [-t] [-x] [--allow-pre-released-features] [--no-data]
                     [--no-jsauth] [--no-config-api] [--no-scim] [--no-fido2] [--install-jans-ldap-link]
                     [--install-jans-keycloak-link] [--with-casa] [--install-jans-saml] [--install-jans-lock]
                     [--install-opa] [--load-config-api-test] [-config-patch-creds CONFIG_PATCH_CREDS]
                     [-test-client-id TEST_CLIENT_ID] [-test-client-pw TEST_CLIENT_PW]
                     [-test-client-redirect-uri TEST_CLIENT_REDIRECT_URI] [--test-client-trusted]
```

Use this script to configure your Jans Server and to add initial data. If setup.properties is found in this folder, these properties will automatically be used instead of the interactive setup.

Below are the optional arguments:

| Argument | Description |
| --- | --- |
| -h, --help | show this help message and exit |
| --version | show program's version number and exit |
| -c | Use command line instead of TUI |
| -d D | Installation directory |
| -f F | Specify setup.properties file |
| -n  | No interactive prompt before install starts. Run with -f |
| -N, --no-httpd | No apache httpd server |
| -u | Update hosts file with IP address / hostname |
| -csx | Collect setup properties, save and exit |
| -remote-rdbm {mysql,pgsql} | Enables using remote RDBM server |
| -local-rdbm {mysql,pgsql} | Enables installing/configuring local RDBM server |
| -ip-address IP_ADDRESS | Used primarily by Apache httpd for the Listen directive |
| -host-name HOST_NAME | Internet-facing FQDN that is used to generate certificates and metadata. |
| -org-name ORG_NAME | Organization name field used for generating X.509 certificates |
| -email EMAIL | Email address for support at your organization used for generating X.509 certificates |
| -city CITY | City field used for generating X.509 certificates |
| -state STATE | State field used for generating X.509 certificates |
| -country COUNTRY | Two letters country coude used for generating X.509 certificates |
| -rdbm-user RDBM_USER | RDBM username |
| -rdbm-password RDBM_PASSWORD | RDBM password | 
| -rdbm-port RDBM_PORT | RDBM port |
| -rdbm-db RDBM_DB | RDBM database |
| -rdbm-host RDBM_HOST | RDBM host |
| --reset-rdbm-db | Deletes all tables on target database. Warning! You will lose all data on target database. |
| --shell | Drop into interactive shell before starting installation |
| --dump-config-on-error | Dump configuration on error |
| --no-progress | Use simple progress |
| -admin-password ADMIN_PASSWORD | Used as the Administrator password |
| -jans-max-mem JANS_MAX_MEM | Total memory (in KB) to be used by Jannsen Server |
| -properties-password PROPERTIES_PASSWORD | Encoded setup.properties file password |
| -approved-issuer APPROVED_ISSUER | Api Approved Issuer |
| --force-download | Force downloading files |
| --download-exit | Download files and exits |
| -jans-app-version JANS_APP_VERSION | Version for Jannsen applications |
| -jans-build JANS_BUILD | Build version for Janssen applications |
| -setup-branch SETUP_BRANCH | Jannsen setup github branch |
| --disable-config-api-security | Turn off oauth2 security validation for jans-config-api |
| --cli-test-client | Use config api test client for CLI |
| --import-ldif IMPORT_LDIF | Render ldif templates from directory and import them in Database
| -enable-script ENABLE_SCRIPT | inum of script to enable |
| -disable-script DISABLE_SCRIPT | inum of script to disable |
| -disable-selinux | Disable SELinux |
| -stm, --enable-scim-test-mode | Enable Scim Test Mode |
| -w | Get the development head war files |
| -t | Load test data |
| -x | Load test data and exit |
| --allow-pre-released-features | Enable options to install experimental features, not yet officially supported |
| --no-data | Do not import any data to database backend, used for clustering |
| --no-jsauth | Do not install OAuth2 Authorization Server |
| --no-config-api | Do not install Jans Auth Config Api |
| --no-scim | Do not install Scim Server |
| --no-fido2 | Do not install Fido2 Server |
| --install-jans-ldap-link | Install LDAP Link Server |
| --with-casa | Install Gluu/Flex Casa Server |
| --load-config-api-test | Load Config Api Test Data |
| --install-cache-refresh | Install Cache Refresh Server |
| -config-patch-creds CONFIG_PATCH_CREDS | password:username for downloading auto test ciba password |
| -test-client-id TEST_CLIENT_ID | ID of test client which has all available scopes. Must be in UUID format. |
| -test-client-pw TEST_CLIENT_PW | Secret for test client |
| -test-client-redirect-uri TEST_CLIENT_REDIRECT_URI | Redirect URI for test client |
| --test-client-trusted | Make test client trusted |
