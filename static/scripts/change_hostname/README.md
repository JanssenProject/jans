# Changing Gluu Server hostname

Used to change a Gluu Server from one hostname to another.

**Currently tested to work with Gluu Server 3.1.2 using Ubuntu 16 and CentOS 7**

Requirements:

- Python 2
- Python-pip
- ldap3
- test.py
- change_gluu_host.py

Install python-pip and ldap3

```
apt install python-pip
pip install ldap3
```

Download [test.py](https://github.com/GluuFederation/cluster-mgr/blob/master/testing/test.py) and [change_gluu_host.py](https://github.com/GluuFederation/cluster-mgr/blob/master/testing/change_gluu_host.py) on the Gluu Server you're trying to change the hostname of.

Modify the entries inside of `test.py` using the following template:

`-os` below needs to be either "Ubuntu" or "CentOS"

```
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
    local=True
    )
```
  
  Let's take the example of me using `dev.example.org` but my customer changed their domain requirements to `idp.customer.io`, the environment wouldn't fit the spec and I would have to rebuild. Fortunately with this script, a quick turnaround to another hostname, with new certificates to match that domain name, is one command-line away.

  To achieve this with the previous example, I would modify the `test.py` file like so:

```
name_changer = ChangeGluuHostname(
    old_host='dev.example.org',
    new_host='idp.customer.io',
    cert_city='Austin',
    cert_mail='admin@customer.io',
    cert_state='TX',
    cert_country='US',
    server='dev.example.org', <------ Whatever the server resolves as. This is to connect to LDAP for modifications.
    ip_address='10.36.101.25',
    ldap_password="MyS3crE71D4pPas$",
    os_type='Ubuntu',
    local=True
    )
```

  Now run `python test.py` outside of your Gluu chroot and once completed, restart your Gluu Server.
  
  All the endpoints inside LDAP, Apache2/HTTPD, `/etc/hosts` and all certificates have been successfully changed to the new hostname. 
