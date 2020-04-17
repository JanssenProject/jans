# Forgot Password Interception Script

## Description

This script enables user to reset its password:

* Step 1: User enters e-mail and if e-mail exists, user receive a token via email
* Step 2: User enters token receiven by e-mail.
* Step 3: User enters new password

## Instalation

* Make sure you have your **SMTP settings correctly Gluu Server** - Navigate to Configuration > Organization Configuration > SMTP Server Configuration
* Enable the custom script. 

Please notice the xhtml files for this script are currently located at `oxAuth/Server/src/main/webapp/auth/forgot_password/`


