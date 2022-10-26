Passport is a person authentication module for oxAuth that enables [Google+ Authentication](https://www.google.com), [Twitter Authentication](https://www.twitter.com), [Facebook Authentication](https://www.facebook.com) etc. for user authentication.

The module has a few properties:
   
   1) generic_remote_attributes_list - It's mandatory property. Comma separated list of attribute names. Specify list of User claims(attributes) which script should use to map to local attributes. The count of attributes in this property should be equal to count attributes in generic_local_attributes_list property.
Example: `username, email, name, name, givenName, familyName, provider`
   
   2) generic_local_attributes_list - It's mandatory property. Comma separated list of attribute names. Specify list of local attributes mapped from passport userInfo response. The count of attributes in this property should be equal to count attributes in generic_remote_attributes_list property. Local attributes list should contains next mandatory attributes: uid, mail, givenName, sn, cn.
Example: `uid, mail, cn, displayName, givenName, sn, provider`

