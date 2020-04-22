# Identifier-first authentication

This custom script allows administrators to implement the following workflow:

1. User is presented with a form asking for username only
2. User is shown a form asking for password only (username field is not editable)
3. User is taken to a page where he is challenged to present a second factor (strong credential) for completion of authentication

After form submission in step 1, the suitable user entry is looked up and an attribute is expected to contain an acr value corresponding to an already existing and enabled custom script that determines the second factor.

If the attribute is not existing, the "basic" acr will be used. Thus, the basic script needs to be enabled as well in Gluu server.


For numerals 2 and 3 to work properly, all custom scripts to be used (basic, u2f, super_gluu, ect.) should be edited so that the getPageForStep uses the custom page named "alter_login.xhtml".  In other words, comment out the line showing

	`return ""`

by prefixing it with a `#` character and add one with

	`return "/auth/idfirst/alter_login.xhtml"`
	
in the subroutine "getPageForStep" of every script.

## Requirements:

* To configure the user attribute to lookup for an acr (and thus a second factor), add "*acr_attribute*" as a property of the Identifier-first custom script via oxTrust.

Example:

name: acr_attribute
value: oxPreferredMethod

where `oxPreferredMethod` is an LDAP attribute part of GluuPerson object class.

* Copy the accompanying [custom pages](https://github.com/GluuFederation/oxAuth/tree/master/Server/src/main/webapp/auth/idfirst) to `/opt/gluu/jetty/oxauth/custom/pages/idfirst`, namely `alter_login.xhtml`, `alter_login.page.xml` (3.0.2 only), and `idfirst_login.xhtml`. This is only required if your Gluu Server wasn't originally bundled with this script

* Ensure this script has a low level set in oxTrust. All other scripts to which this scripts forwards to must be greater in level than this.