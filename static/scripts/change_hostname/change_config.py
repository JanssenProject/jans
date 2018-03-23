import os, sys
from change_gluu_host import Installer, FakeRemote, ChangeGluuHostname

name_changer = ChangeGluuHostname(
    old_host='<current_hostname>',
    new_host='<new_hostname>',
    cert_city='<city>',
    cert_mail='<email>',
    cert_state='<state_or_region>',
    cert_country='<country>',
    server='<actual_hostname_of_server>',
    ip_address='<ip_address_of_server>',
    ldap_password="<ldap_password>",
    os_type='<linux_distro>',
    local= True
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
