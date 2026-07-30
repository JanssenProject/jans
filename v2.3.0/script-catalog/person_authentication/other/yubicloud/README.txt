Yubicloud OTP Validataion
=========================

Script contents [here](https://github.com/JanssenProject/jans/jans-linux-setup/static/extension/person_authentication/YubicloudExternalAuthenticator.py)

This is a single step authentication workflow. Instead of a human entering a
password, Yubico's Yubikey OTP will be taken in as password.

This script uses the Yubicloud Service by Yubico for validation of the OTP.

Here are the steps required to setup the Yubico OTP as password.

    1. Setup a custom attribute named the `yubikeyId`.  Add this attribute the
       users who have to be authenticated via this method. Store the public part
       of the Yubikey Idendity (usually the first 12 chars of OTP) in this
       attribute against each user. This matches the user against the key.

        Setting up Custom Attributes: https://www.gluu.org/docs/customize/attributes/
        Find the Yubikey Idendity: https://demo.yubico.com

    2. Register for a Yubicloud API Key and get client ID and client Secret.
       https://upgrade.yubico.com/getapikey/

    3. Configure the custom script:
        i) Enter the value for `yubicloud_uri` as any one of the following:
            api.yubico.com
            api2.yubico.com
            api3.yubico.com
            api4.yubico.com
            api5.yubico.com
        ii) Enter the client secret as `yubicloud_api_key`
        iii) Enter the client ID as `yubicloud_id`

Now the method `yubicloud` can be set as the authentication mechanism and Yubikey
can be used in place of the password of the users for authentication.


