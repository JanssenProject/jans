If you are installing casa plugin for EMail OTP (**EMail 2FA**, **email_2fa_core**) in the **Jassen Server**, follow the steps below:

1. Use file **email_2fa_core.py** as jython sources, during creating of the **email_2fa_core** custom script;
1. Add/create **email_2fa_core** custom script in **janssen**,

Parameters: **Script Type**: **Person Authentication**, use follow **configuration properties** of the **email_2fa_core** custom script:

Parameters of the script:  
- **token_length**:     It determines the length of the characters of the One Time Password sent to the user:
    + required parameter;
    + default value: not defined;
- **token_lifetime**:   It determines the time period for which the sent token is active:
    + required parameter;
    + default value: not defined;
- **Signer_Cert_KeyStore**: File path of the Keystore
    + nonrequired parameter;
    + default value: value, defined in **Janssen** (**/opt/jans/jans-cli/config-cli-tui.py**): *SMTP*/*KeyStore*;
        * for example: */etc/certs/smtp-keys.pkcs12*;
- **Signer_Cert_KeyStorePassword**: Keystore Password
    + nonrequired parameter;
    + default value: value, defined in **Janssen** (**/opt/jans/jans-cli/config-cli-tui.py**): *SMTP*/*KeyStore Password*;
        * for example: *tRmJpb$1_&BzlEUC7*;
- **Signer_Cert_Alias**: Alias of the Keystore.
    + nonrequired parameter;
    + default value: value, defined in **Janssen** (**/opt/jans/jans-cli/config-cli-tui.py**): *SMTP*/*KeyStore Alias*;
        * for example: *smtp_sig_ec256*;
- **Signer_SignAlgorithm**: Name of Signing Algorithm
    + nonrequired parameter;
    + default value: value, defined in **Janssen** (**/opt/jans/jans-cli/config-cli-tui.py**): *SMTP*/*KeyStore Algorithm*;
    + by default algirithm is used by signing of certificate from the Keystore;
        * for example: *SHA256withECDSA*

3. Create directory: **/opt/jans/jetty/jans-auth/custom/pages/casa**, copy to this directory follow files: **otp_email.xhtml**, **otp_email_prompt.xhtml**;
1. Create directory: **/opt/jans/jetty/jans-auth/custom/i18n**, copy to this directory file **jans-auth.properties**;
1. Create directory: **/opt/jans/jetty/jans-auth/custom/static/img**, copy to this directory file **email-ver.png**.
