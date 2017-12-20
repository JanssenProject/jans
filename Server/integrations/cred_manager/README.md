The following are the configuration properties needed by this script:

* **u2f_app_id**: Normally points to the Gluu server itself, e.g. `https://mygluuhost.com/`

* **supergluu_app_id**: Use here the URL where cred-manager web application is accesible, e.g. `https://mygluuhost.com/cred-manager/`. If you are not planning to use the application, still provide a URL like `https://mygluuhost.com/cred-manager/`.

These two properties are used by the script to differentiate between fido devices: whether it's a u2f security key, or a super gluu device.

The following runtime library is required for the script to be loaded correctly:
* Twilio 7.12 [jar file](http://search.maven.org/remotecontent?filepath=com/twilio/sdk/twilio/7.12.0/twilio-7.12.0-jar-with-dependencies.jar)(~7MB).