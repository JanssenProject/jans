import os, sys
from change_gluu_host import Installer, FakeRemote, ChangeGluuHostname

name_changer = ChangeGluuHostname(
    
    # Change these parameters here. If there are '' marks, leave them only replacing <entry> with your information.
    
    # The hostname currently in Gluu Server's configuration
    old_host='c1.gluu.org',
    
    # The hostname you would like your Gluu Server to have
    new_host='c2.gluu.org',
    
    #### Certificate Creation #### 
    cert_city='Austin',
    cert_mail='support@gluu.org',
    # State must be in 2 letter format i.e TX for Texas
    cert_state='TX',
    # Country also must be in 2 letter format i.e US for United States
    cert_country='US',
    ##############################
    
    # IP Address of the Gluu Server. Used to change the /etc/hosts file for Apache
    ip_address='159.89.43.71',
    
    # The hostname or IP of the LDAP server to make changes to static hostname data. 
    # The local parameter below, when True, disregards this variable to prevent accidentally connecting to a production instance.
    server='localhost',
    
    # The password to the the LDAP server
    ldap_password="Gluu1234.",
    
    # 'Ubuntu' or 'CentOS'
    os_type='CentOS',
    
    # Do not change to False unless you want the script to access that LDAP server remotely. 
    # Not recommended as the script should be run locally
    local = True,
    
    # Version of Gluu Server you're trying to modify. For example: 4.1.1 , nochroot-4.2.1
    gluu_version='4.2.1',
    
    )

r = name_changer.startup()
if not r:
    sys.exit(1)

name_changer.change_ldap_entries()
name_changer.change_httpd_conf()
name_changer.create_new_certs()
name_changer.change_host_name()
name_changer.modify_etc_hosts()
