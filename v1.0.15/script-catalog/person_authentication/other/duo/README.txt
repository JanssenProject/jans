Script contents [here](https://github.com/JanssenProject/jans/jans-linux-setup/static/extension/person_authentication/DuoExternalAuthenticator.py)

This is a person authentication script for jans-auth-server which enables [Duo Authentication](https://www.duosecurity.com) for user authentication.

The module has a few properties:

1) duo_creds_file - It's mandatory property. It's path to file which contains contains ikey, skey, akey. These keys are required for DUO authentication.
Example: `/etc/certs/duo_creds.json`
Example content of this file:
`{"ikey": "ikey_value", "skey": "skey_value", "akey": "akey_value"}`

2) duo_host - It's mandatory property. The URL of the DUO API server.
   Example: `api-random.duosecurity.com`

3) audit_attribute - It's optional property. It allows to define an attribute which the module should check for to determine whether the user belongs to duo_group or audit_group. Person DUO authentication module uses it if there is `duo_group` or `audit_group` property.
   Example: `memberOf`

4) duo_group - It's optional property. It's an optional attribute that alows to specify if DUO should be used for specific users. i.e. use DUO only for users who have audit_attribute (`memberOf`) attribute value equal to `duo_group`. If there is none DUO will be enforced for all users. 

5) audit_group - It's optional property. Specify if module should send an e-mail to administrator upon login of a user who has audit_attribute `memberOf` attribute value equal to `audit_group`.

6) audit_group_email - It's optional property. It's the administrator's e-mail. Person DUO authentication module uses it if there is `audit_group` property.

