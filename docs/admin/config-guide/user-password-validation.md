---
tags:
  - administration
  - configuration
  - password
---

# Password validation
User password validation can be set to check the password strength, like the password must have at least one uppercase, or lowercase, number, and the length of the password
By default the attribute validation is not enabled.  


## Enable validation through Jans-Config-Api

1. Obtain the access token
```shell
curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
     -d  "grant_type=client_credentials&scope=https://jans.io/oauth/config/attributes.write"
```
   
2. Search for `userPassword` attribute details in json fileCheck
curl https://<your.jans.server>/jans-config-api/api/v1/attributes?pattern=userPassword \ 
    -H "Authorization: Bearer put_access_token_here"
```

3. Apply Patch for `userPassword` 
- Enter valid regex expression to check the strength of the password.
- Enter **Minimum length**,the minimum length of a value associated with this attribute.
- Enter **Maximum length**, the maximum length of a value associated with this attribute.
```
   curl -X PATCH -k -H 'Content-Type: application/json-patch+json' \ 
      -i 'https://<your.jans.server>/jans-config-api/api/v1/attributes/AAEE' \ 
      -H "Authorization: Bearer put_access_token_here" --data '[
         {
          "op": "add",
          "path": "/attributeValidatio",
          "value": [
                       {
                        "minLength": 8,
                        "maxLength":20,
                        "regexp": "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$"
                       }
                   ]
          }
         ]'
   ```



# Create User 
1. Create a new user using **User Management** screen.

2. Enter user details.

3. Enter password.

4. Save user details.

5. Error notification will be displayed if the password does not match the validation.

![update default authentication method](../../assets/image-pwd-validation-notification.png)

