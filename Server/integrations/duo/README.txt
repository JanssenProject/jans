This is DUO custom authentication modules for oxAuth. It allows to configure oxAuth to use 2 factor authentication for interactive user logins.
There is more information about DUO in this site: https://www.duosecurity.com

This module has few properties:
1) duo_creds_file - It's path to file in json format which contains ikey, skey, akey. These keys are required for DUO authentication.
   Example: /etc/certs/duo_creds.json
   Example content of this file:
   {"ikey": "ikey_value", "skey": "skey_value", "akey": "akey_value"}

2) duo_host - It's URL of DUO API server.
   Example: api-random.duosecurity.com

3) audit_attribute - Define person's attribute which script should use to check if user belong to duo_group or audit_group
   Example: memberOf

4) duo_group - Specify if DUO should be used for specific users. It's optinal attribute. If there is no this atribute custom authentication script
   should use DUO for all users authentication. If this attribute exists it use DUO only for users who has memberOf attribute value equal to
   duo_group value.

5) audit_group_email - It's administrator e-mail. DUO uses it if there is audit_group property. It's optinal attribute.

6) audit_group - Specify if module should send e-mail to administrator when user who has memberOf attribute value equal to audit_group value
   log in. It's optinal attribute.
