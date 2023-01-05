Script contents [here](https://github.com/JanssenProject/jans/jans-linux-setup/static/extension/person_authentication/UserCertExternalAuthenticator.py)

This is a person authentication script for jans-auth-server that enables user Certificate Authentication.

The module has a few properties:

1) chain_cert_file_path - It's mandatory property. It's path to file with cert chains in pem format.
   Example: '/etc/certs/chain_cert.pem'

2) map_user_cert - Specify if script should map new user to local account. If true, then on the first authentication, the script will prompt for a username/password in step 2, and then store the certificate fingerprint in the `oxExternalUid` attribute. 
   Allowed values: true/false
   Example: true

3) use_generic_validator, use_path_validator, use_ocsp_validator, use_crl_validator - Enable/Disable specific certificate validation.
   Allowed values: true/false
   Example: true
   
4) crl_max_response_size - Specify maximum allowed size of CRL response
   Allowed values: integer value greater that 0
   Example: 10485760
   Default value: 5242880

5) credentials_file - Patch to file with reCAPTCHA credentials.
   Example: '/etc/certs/cert_credentials.json'
