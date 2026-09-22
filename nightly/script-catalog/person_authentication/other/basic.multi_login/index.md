This is person authentication modules for oxAuth which allows to use several attributes as user name.

## Install

This list of steps needed to do to enable Basic Multi Login person authentication module.

1. This module depends on python libraries. In order to use it we need to install Jython. Please use next articles to proper Jython installation:
    - Installation notest: http://ox.gluu.org/doku.php?id=oxtauth:customauthscript#jython_installation_optional
    - Jython integration: http://ox.gluu.org/doku.php?id=oxtauth:customauthscript#jython_python_integration

2. Copy shared required python libraries from ../shared_libs folder to $CATALINA_HOME/conf/python folder.

3. Confire new custom module in oxTrust:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Custom Scripts" page.
    - Select "Person Authentication" tab.
    - Click on "Add custom script configuration" link.
    - Enter name = basic_multi_login
    - Enter level = 0-100 (priority of this method).
    - Select usage type "Interactive".
    - Add custom required and optional properties which specified in README.txt.
    - Copy/paste script from BasicMultiLoginExternalAuthenticator.py.
    - Activate it via "Enabled" checkbox.
    - Click "Update" button at the bottom of this page.

4. Configure oxAuth to use Basic Multi Login authentication by default:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Authentication" page.
    - Scroll to "Default Authentication Method" panel. Select "basic_multi_login" authentication mode.
    - Click "Update" button at the bottom of this page.

5. Try to log in using Basic Multi Login authentication method:
    - Wait 30 seconds and try to log in again. During this time oxAuth reload list of available person authentication modules.
    - Open second browser or second browsing session and try to log in again. It's better to try to do that from another browser session because we can return back to previous authentication method if something will go wrong.

There are log messages in this custom authentication script. In order to debug this module we can use command like this:
tail -f /opt/tomcat/logs/wrapper.log | grep "Basic (multi login)"


## Properties

1) login_attributes_list - Comma separated list of attribute names. Specify list of IdP attributes which this module should use to map to local attributes.
   It's optional property.
   The count of attributes in this property should be equal to count attributes in local_login_attributes_list property.
   Example: uid, mail

2) local_login_attributes_list - Comma separated list of attribute names. Specify list of local attributes mapped from IdP attributes.
   It's optional property.
   The count of attributes in this property should be equal to count attributes in login_attributes_list property.
   Example: uid, mail
