import os, sys
from change_gluu_host import Installer, FakeRemote, ChangeGluuHostname

name_changer = ChangeGluuHostname(
    
    # Change these parameters here. If there are '' marks, leave them only replacing <entry> with your information.
    
    # The hostname currently in Gluu Server's configuration
    old_host='<current_hostname>',
    
    # The hostname you would like your Gluu Server to have
    new_host='<new_hostname>',
    
    #### Certificate Creation #### 
    cert_city='<city>',
    cert_mail='<email>',
    # State must be in 2 letter format i.e TX for Texas
    cert_state='<state_or_region>',
    # Country also must be in 2 letter format i.e US for United States
    cert_country='<country>',
    ##############################
    
    # IP Address of the Gluu Server. Used to change the /etc/hosts file for Apache
    ip_address='<ip_address_of_server>',
    
    # The hostname or IP of the LDAP server to make changes to static hostname data. 
    # The local parameter below, when True, disregards this variable to prevent accidentally connecting to a production instance.
    server='<LDAP_server>',
    
    # The password to the the LDAP server
    ldap_password="<ldap_password>",
    
    # 'Ubuntu' or 'CentOS'
    os_type='<linux_distro>',
    
    # Do not change to False unless you want the script to access that LDAP server remotely. 
    # Not recommended as the script should be run locally
    local = True,
    
    # Version of Gluu Server you're trying to modify. For example: '3.1.3'
    gluu_version='<gluu_server_version>',
    
    #Type of LDAP Server, it will be either openldap or opendj
    ldap_type='<openldap or opendj>'
    )

r = name_changer.startup()
if not r:
    sys.exit(1)

name_changer.change_appliance_config()
name_changer.change_clients()
name_changer.change_uma()
name_changer.change_httpd_conf()
name_changer.create_new_certs()
name_changer.change_host_name()
name_changer.modify_etc_hosts()
name_changer.modify_saml_passport()
name_changer.change_custom_scripts()
name_changer.change_casa()
