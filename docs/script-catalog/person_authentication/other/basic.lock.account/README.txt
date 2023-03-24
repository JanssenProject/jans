Script contents [here](https://github.com/JanssenProject/jans/jans-linux-setup/static/extension/person_authentication/BasicLockAccountExternalAuthenticator.py) 

This is a person authentication script for jans-auth-server which does basic authentication.
It looks user account after specified number of unsuccessful login attempts.

This module has 2 properties:

1) invalid_login_count_attribute - Specify attribute where script stores count of invalid number of login attemps
   Default value: jansCountInvalidLogin

2) maximum_invalid_login_attemps - Specify how many times user can enter invalid password before application will lock account
   Allowed values: integer value greater that 0
   Example: 3
   Default value: 3
3) lock_expiration_time - Specify the time in seconds when lock will be expired
   Default value: 180
