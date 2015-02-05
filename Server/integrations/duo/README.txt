This is a custom authentication module for oxAuth that enables [Duo Authentication](https://www.duosecurity.com) for user logins.

The module has a few properties:

1) duo_creds_file - This is the file path in json format which contains ikey, skey, akey. These keys are required for DUO authentication.
Example: `/etc/certs/duo_creds.json`
Example content of this file:
`{"ikey": "ikey_value", "skey": "skey_value", "akey": "akey_value"}`

2) duo_host - The URL of the DUO API server.
   Example: `api-random.duosecurity.com`

3) audit_attribute - This allows the admin to define an attribute which the script should check for to determine whether the user belongs to duo_group or audit_group
   Example: `memberOf`

4) duo_group - This is an optional attribute that enables admin to specify if DUO should be used for specific users. i.e. use DUO only for users who have `memberOf` attribute equal to `duo_group`. If there is none DUO will be enforced for all users. 

5) audit_group_email - This is the administrator's e-mail. DUO uses it if there is `audit_group` property. It's an optinal attribute.

6) audit_group - Specify if module should send an e-mail to administrator upon login of a user who has `memberOf` equal to `audit_group`. It's optinal attribute.
