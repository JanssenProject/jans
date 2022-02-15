Script contents [here](https://github.com/JanssenProject/jans/jans-linux-setup/static/extension/person_authentication/OtpExternalAuthenticator.py)

This is a person authentication script for jans-auth-server which enables one-time password for user authentication.

The module has a few properties:

1) otp_type - It's mandatory property. It's specify OTP mode: HOTP/ TOTP.
   Allowed values: hotp/totp
   Example: hotp

2) issuer - It's mandatory property. It's company name.
   Example: Janssen Inc

3) otp_conf_file - It's mandatory property. It's specify path to OTP configuration JSON file.
   Example: /etc/certs/otp_configuration.json

4) label - It's label inside QR code. It's optional property.
    Example: Janssen OTP

5) qr_options - Specify width and height of QR image. It's optional property.
    Example: qr_options: { width: 400, height: 400 }

6) registration_uri - It's URL to page where user can register new account. It's optional property.
    Example: https://ce-dev.jans.org/identity/register
    