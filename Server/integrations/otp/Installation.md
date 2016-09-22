This list of steps needed to do to enable SAML person authentication module.

1. Confire new custom module in oxTrust:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Custom Scripts" page.
    - Select "Person Authentication" tab.
    - Click on "Add custom script configuration" link.
    - Enter name = otp
    - Enter level = 0-100 (priority of this method).
    - Select usage type "Interactive".
    - Add custom required and optional properties which specified in "Properties description.md".
    - Copy/paste script from TotpExternalAuthenticator.py.
    - Activate it via "Enabled" checkbox.
    - Click "Update" button at the bottom of this page.

2. Configure oxAuth to use OTP authentication by default:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Authentication" page.
    - Scroll to "Default Authentication Method" panel. Select "otp" authentication mode.
    - Click "Update" button at the bottom of this page.

3. Try to log in using OTP authentication method:
    - Wait 30 seconds and try to log in again. During this time oxAuth reload list of available person authentication modules.
    - Open second browser or second browsing session and try to log in again. It's better to try to do that from another browser session because we can return back to previous authentication method if something will go wrong.

There are log messages in this custom authentication script. In order to debug this module we can use command like this:
tail -f /opt/tomcat/logs/wrapper.log | grep "OTP"
