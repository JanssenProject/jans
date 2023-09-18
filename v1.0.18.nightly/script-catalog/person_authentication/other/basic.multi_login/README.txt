This is person authentication modules for oxAuth which allows to use several attributes as user name.

This module has next properties:

1) login_attributes_list - Comma separated list of attribute names. Specify list of IdP attributes which this module should use to map to local attributes.
   It's optional property.
   The count of attributes in this property should be equal to count attributes in local_login_attributes_list property.
   Example: uid, mail

2) local_login_attributes_list - Comma separated list of attribute names. Specify list of local attributes mapped from IdP attributes.
   It's optional property.
   The count of attributes in this property should be equal to count attributes in login_attributes_list property.
   Example: uid, mail
