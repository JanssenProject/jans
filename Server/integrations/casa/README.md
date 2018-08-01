The following are the configuration properties needed by this script:

* **u2f_app_id**: Normally points to the Gluu server itself, e.g. `https://mygluuhost.com/`

* **supergluu_app_id**: Use here the URL where Gluu Casa application is accesible, e.g. `https://mygluuhost.com/casa/`. If you are not planning to use the application, still provide a URL like `https://mygluuhost.com/casa/`.

These two properties are used by the script to differentiate between fido devices: whether it's a u2f security key, or a super gluu device.

See the installation docs for more info: gluu.org/docs/casa/