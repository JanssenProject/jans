# Sample plugin for enrolling credentials

Steps to run the plugin -
1. Enable "basic" custom script in oxTrust
2. In the custom script for basic - add a new property 2fa_requisite = true
3. Log in to casa, in casa admin console, go to "Enabled authentication methods" from the menu. Select "basic" as a 2fa method for authentication.
4. Add the plugin jar file from the admin console
5. Notice the newly created menu that reads "sample device" in the menu bar
