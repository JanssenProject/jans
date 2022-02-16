# ThumbSignIn

ThumbSignIn can be integrated with Janssen Server to achieve strong authentication for enterprise applications. The administrator of an organization can deploy the ThumbSignIn Java SDK, UI components and custom Jython script in Janssen server.  Here, ThumbSignIn is integrated with Janssen server as a primary authenticator to achieve passwordless login. The user will be able to login to the Janssen server with just his/her biometrics and there is no need for the password.  For the first time user, the user can login with his/her LDAP credentials and then can register through ThumbSignIn mobile app. For the subsequent logins, the user can directly login to Janssen server with his/her biometric.  

- [Steps to perform Integration](https://thumbsignin.com/download/thumbsigninGluuIntegrationDoc)

For more information about ThumbSignIn, see their [website](http://thumbsignin.com)

Script contents 
[here](https://github.com/JanssenProject/jans/jans-linux-setup/static/extension/person_authentication/ThumbSignInExternalAuthenticator.py)