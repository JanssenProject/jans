Janssen CLI also supports SMTP configuration. You can do the following things as stated below:
- `View/Get`
- `Add/Delete`
- `Update`
- `Test`

Simply select option '10' from Main Menu, It will show some options as below:
```text
Configuration – SMTP
--------------------
1 Returns SMTP server configuration
2 Adds SMTP server configuration
3 Updates SMTP server configuration
4 Deletes SMTP server configuration
5 Test SMTP server configuration
```
Just go with the option and perform operation.

- **__view / find__** : select option 1, it will return as below:

```text
Returns SMTP server configuration
---------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/smtp.readonly

{
  "host": null,
  "port": 0,
  "requiresSsl": null,
  "serverTrust": null,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": null,
  "userName": null,
  "password": null
}
```
- **__Add SMTP Server__**
To add a smtp server, chose option 2 from SMTP Configuration Menu:
  
```text
Selection: 2

«Hostname of the SMTP server. Type: string»
host: 

«Port number of the SMTP server. Type: integer»
port: 

«Boolean value with default value false. If true, SSL will be enabled. Type: boolean»
requiresSsl  [false]: 

«Boolean value with default value false. Type: boolean»
serverTrust  [false]: 

«Name of the sender. Type: string»
fromName: 

«Email Address of the Sender. Type: string»
fromEmailAddress: 

«Boolean value with default value false. It true it will enable sender authentication. Type: boolean»
requiresAuthentication  [false]: 

«Username of the SMTP. Type: string»
userName: 

«Password for the SMTP. Type: string»
password: 
Obtained Data:

{
  "host": null,
  "port": null,
  "requiresSsl": false,
  "serverTrust": false,
  "fromName": null,
  "fromEmailAddress": null,
  "requiresAuthentication": false,
  "userName": null,
  "password": null
}

Continue? 
```

Fill each property with the correct information.
- **Test SMTP Server**

If the server is running, and all the information you have entered is correct. You can test SMTP server from the following option 5, it will respond if the server is configured properly.

