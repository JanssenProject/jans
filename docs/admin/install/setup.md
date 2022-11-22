---
tags:
  - administration
  - installation
  - faq
---

# Setup Script

## Running Setup

 After installation, executing setup.py will launch the SETUP Command Line by default. 
 
 To run the script, run the following command:
 
 ```bash
 python3 /opt/jans/jans-setup/setup.py
 ```
 
 A warning will pop up if the free disk space is less than the recommended 40 GB. The installer will check that all dependant packages are installed or not, and if missing it will ask to install. When prompted Y/y at the command prompt will install all required packages.
   
1. The installer will detect which operating system, init type, and Apache version are currently on the server.
  
2. The setup script will bring up a prompt to provide information for certificates as well as the IP Address and the hostname for the Gluu Server. Hit Enter to accept the default values.
  
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
  
3. Next, pick a persistence mechanism. Choose from openDJ, MySQL ,PGSql, an LDAP that can be installed locally or remotely, or Couchbase, an enterprise NoSQL cloud database.
    
4. Next, pick which services should be installed for this deployment:

  ```bash
  Install Jans Config API? [Yes]:
  Install SCIM Server? [Yes]:
  Install Fido2 Server? [Yes]:
  ```
   
5. Finally, review the summary screen that gives an overview of the selections made during the setup process.

### Avoiding common issues

Avoid setup issues by acknowledging the following:

- IP Address: Do not use localhost for either the IP address or hostname.

- Hostname:
  
   - Make sure to choose the hostname carefully. Changing the hostname after installation is not a simple task.
     
   - Use a real hostname--this can always be managed via host file entries if adding a DNS entry is too much work for testing.
     
   - For clustered deployments, use the hostname of the cluster that will be used by applications connecting to Gluu.
  
!!! Warning    
    Use a FQDN (fully qualified domain name) as hostname and refrain from using 127.0.0.1 as IP address or usage of private IP is not supported and not recommended.

## Script Command Line Options

To check usage of this script run help command

```
    python3 /opt/jans/jans-setup/setup.py --help

      usage: setup.py [-h] [--version] [-c] [-d D] [-f F] [-n] [-N] [-u] [-csx] [-remote-rdbm {mysql,pgsql,spanner} | -local-rdbm 
      {mysql,pgsql}]
      [-ip-address IP_ADDRESS] [-host-name HOST_NAME] [-org-name ORG_NAME] [-email EMAIL] [-city CITY] [-state STATE] [-country COUNTRY] 
      [-rdbm-user RDBM_USER] [-rdbm-password RDBM_PASSWORD] [-rdbm-port RDBM_PORT] [-rdbm-db RDBM_DB] [-rdbm-host RDBM_HOST] 
      [--reset-rdbm-db] [--shell] [--dump-config-on-error] [--no-progress] [-admin-password ADMIN_PASSWORD]  [-jans-max-mem JANS_MAX_MEM] 
      [-properties-password PROPERTIES_PASSWORD] [-approved-issuer APPROVED_ISSUER] [--force-download] [--download-exit] 
      [-jans-app-version JANS_APP_VERSION] [-jans-build JANS_BUILD] [-setup-branch SETUP_BRANCH]  [--disable-config-api-security]
      [--cli-test-client] [--import-ldif IMPORT_LDIF] [-enable-script ENABLE_SCRIPT]  [-disable-script DISABLE_SCRIPT] [-stm] [-w]
      [-t] [-x] [--allow-pre-released-features] [--listen_all_interfaces] [--remote-ldap | --disable-local-ldap] [--remote-couchbase]
      [--local-couchbase] [-couchbase-admin-user COUCHBASE_ADMIN_USER]  [-couchbase-admin-password COUCHBASE_ADMIN_PASSWORD] 
      [-couchbase-bucket-prefix COUCHBASE_BUCKET_PREFIX][-couchbase-hostname COUCHBASE_HOSTNAME] [--no-data] [--no-jsauth] 
      [-ldap-admin-password LDAP_ADMIN_PASSWORD] [--no-config-api] [--no-scim] [--no-fido2] [--install-eleven] [--load-config-api-test] 
      [-config-patch-creds CONFIG_PATCH_CREDS] [-spanner-project SPANNER_PROJECT] [-spanner-instance SPANNER_INSTANCE] [
      -spanner-database SPANNER_DATABASE]  [-spanner-emulator-host SPANNER_EMULATOR_HOST | -google-application-credentials       
      GOOGLE_APPLICATION_CREDENTIALS]
```

Use this script to configure your Jans Server and to add initial data required for oxAuth and oxTrust to start. If setup.properties is found in this folder, these properties will automatically be used instead of the interactive setup.

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
| -remote-rdbm {mysql,pgsql,spanner} | Enables using remote RDBM server |
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
| -disable-script DISABLE_SCRIPT | inum of script to enable |
| -stm, --enable-scim-test-mode | Enable Scim Test Mode |
| -w | Get the development head war files |
| -t | Load test data |
| -x | Load test data and exit |
| --allow-pre-released-features | Enable options to install experimental features, not yet officially supported |
| --listen_all_interfaces | Allow the LDAP server to listen on all server interfaces |
| --remote-ldap | Enables using remote LDAP server |
| --disable-local-ldap  | Disables installing local LDAP server |
| --remote-couchbase | Enables using remote couchbase server |
| --local-couchbase | Enables installing couchbase server |
| -couchbase-admin-user COUCHBASE_ADMIN_USER | Couchbase admin user |
| -couchbase-admin-password COUCHBASE_ADMIN_PASSWORD | Couchbase admin user password |
| -couchbase-bucket-prefix COUCHBASE_BUCKET_PREFIX | Set prefix for couchbase buckets |
| -couchbase-hostname COUCHBASE_HOSTNAME | Remote couchbase server hostname |
| --no-data | Do not import any data to database backend, used for clustering |
| --no-jsauth | Do not install OAuth2 Authorization Server |
| -ldap-admin-password LDAP_ADMIN_PASSWORD | Used as the LDAP directory manager password |
| --no-config-api | Do not install Jans Auth Config Api |
| --no-scim | Do not install Scim Server |
| --no-fido2 | Do not install Fido2 Server |
| --install-eleven | Install Eleven Server |
| --load-config-api-test | Load Config Api Test Data |
| -config-patch-creds CONFIG_PATCH_CREDS | password:username for downloading auto test ciba password |
| -spanner-project SPANNER_PROJECT | Spanner project name |
| -spanner-instance SPANNER_INSTANCE | Spanner instance name |
| -spanner-database SPANNER_DATABASE | Spanner database name |
| -spanner-emulator-host SPANNER_EMULATOR_HOST | Use Spanner emulator host |
| -google-application-credentials GOOGLE_APPLICATION_CREDENTIALS | Path to Google application credentials json file |
