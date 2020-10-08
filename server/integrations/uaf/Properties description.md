This is a person authentication module for oxAuth that enables [UAF](https://www.noknok.com) for user authentication.

The module has a few properties:

1) uaf_server_uri - It's mandatory property. It's URL to UAF server.
   Example: https://ce-dev.gluu.org

2) uaf_policy_name - Specify UAF policy name. It's optional property.
   Example: default

3) send_push_notifaction - Specify if UAF server should send push notifications to person mobile phone.
   It's optional property.
   Allowed values: true/false
   Example: false

4) registration_uri - It's URL to page where user can register new account. It's optional property.
    Example: https://ce-dev.gluu.org/identity/register

5) qr_options - Specify width and height of QR image. It's optional property.
    Example: qr_options: { width: 400, height: 400 }
