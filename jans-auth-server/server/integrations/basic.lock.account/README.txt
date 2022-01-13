
This is person authentication module for oxAuth which do basic authentication.
It looks user account after specified number of unsuccessful login attempts.

This module has 2 properties:

1) invalid_login_count_attribute - Specify attribute where script stores count of invalid number of login attemps
   Default value: oxCountInvalidLogin

2) maximum_invalid_login_attemps - Specify how many times user can enter invalid password before application will lock account
   Allowed values: integer value greater that 0
   Example: 3
   Default value: 3
3) lock_expiration_time - Specify the time in seconds when lock will be expired
   Default value: 180
