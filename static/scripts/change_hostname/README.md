# Changing Gluu Server hostname

Used to change a Gluu Server from one hostname to another.

**Currently tested to work with Gluu Server 3.1.2 using Ubuntu 16 and CentOS 7**

Requirements:

- Python 2
- Python-pip
- ldap3
- change_config.py
- change_gluu_host.py

Ubuntu Users
-------------
Install python-pip and ldap3

```
apt install python-pip
pip install ldap3
```

CentOS7 Users
--------------

```
curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
python get-pip.py
pip install ldap3
```

Download [change_config.py](https://github.com/GluuFederation/community-edition-setup/blob/master/static/scripts/change_hostname/change_config.py) and [change_gluu_host.py](https://github.com/GluuFederation/community-edition-setup/blob/master/static/scripts/change_hostname/change_gluu_host.py) on the Gluu Server you're trying to change the hostname of, outside the chroot.

```
wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/scripts/change_hostname/change_config.py
wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/scripts/change_hostname/change_gluu_host.py
```

We only need to modify the entries inside of `change_config.py` using the following template:

```
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
    gluu_version='<gluu_server_version>'
)
```
  
  Let's take the example of me using `dev.example.org` but my customer changed their domain requirements to `idp.customer.io`, the environment wouldn't fit the spec and I would have to rebuild. Fortunately with this script, a quick turnaround to another hostname, with new certificates to match that domain name, is one command-line away.

  To achieve this with the previous example, I would modify the `change_config.py` file like so:

```
name_changer = ChangeGluuHostname(
    old_host='dev.example.org',
    new_host='idp.customer.io',
    cert_city='Austin',
    cert_mail='admin@customer.io',
    cert_state='TX',
    cert_country='US',
    server='localhost', 
    ip_address='10.36.101.25',
    ldap_password="MyS3crE71D4pPas$",
    os_type='Ubuntu',
    local=True
    )
```

  Now run `python change_config.py` outside of your Gluu chroot and once completed, restart your Gluu Server.
  
  All the endpoints inside LDAP, Apache2/HTTPD, `/etc/hosts`, `/opt/shibboleth-idp/conf/idp.properties` and all certificates have been successfully changed to the new hostname. 
