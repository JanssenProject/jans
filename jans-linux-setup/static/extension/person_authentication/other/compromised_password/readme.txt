This is a person authentication module for oxAuth to verify if user password has been compromised and allows user to change password immediately after providing answer to secret question set by admin.

1) credentials_file = /etc/certs/vericloud_gluu_creds.json
2) secret_question  = Set this parameter for the question to be disaplayed to user
3) secret_answer    = Set this parameter for the answer to be provided by user to reset password

Update vericloud_gluu_creds.json file with Vericloud API username and secret