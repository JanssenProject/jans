I. Install CE with `-t` option to load data

II. Client keys deployment.
- cd /var/www/html/
- wget --no-check-certificate https://raw.githubusercontent.com/JansFederation/oxAuth/master/Client/src/test/resources/oxauth_test_client_keys.zip
- unzip oxauth_test_client_keys.zip
- rm -rf oxauth_test_client_keys.zip
- chown -R root.www-data jans-auth-client

III. These changes should be applied to oxAuth config.
1. "dynamicRegistrationCustomObjectClass":"oxAuthClientCustomAttributes",

2. "dynamicRegistrationCustomAttributes":[
       "oxAuthTrustedClient",
       "myCustomAttr1",
       "myCustomAttr2",
       "oxIncludeClaimsInIdToken"
   ],

3. "dynamicRegistrationExpirationTime":86400,

4. "dynamicGrantTypeDefault":[
      "authorization_code",
      "implicit",
      "password",
      "client_credentials",
      "refresh_token",
      "urn:ietf:params:oauth:grant-type:uma-ticket",
      "urn:ietf:params:oauth:grant-type:token-exchange"
   ],

5. "legacyIdTokenClaims":true
6. "authenticationFiltersEnabled":true
7. "clientAuthenticationFiltersEnabled":true
8. "keyRegenerationEnabled":true
9. "openidScopeBackwardCompatibility":false
10. "umaGrantAccessIfNoPolicies": true
11. "claimsParameterSupported" : true
12. "sessionIdRequestParameterEnabled" : true
13. "dynamicRegistrationPasswordGrantTypeEnabled": true
14. "returnClientSecretOnRead": true
15. "forceOfflineAccessScopeToEnableRefreshToken": false
16. "errorReasonEnabled": true 
17. "rejectJwtWithNoneAlg": false  
18. Add to "tokenEndpointAuthMethodsSupported" value "none" (required for public client)

IV. Next custom scripts should be enabled:
1. Enable all UMA RPT Policies and UMA Claims Gathering scripts.
2. Enable basic and basic_lock Person Authentication scripts.

V. Update system configuration
1. Disable token binding module
```
  a2dismod mod_token_binding
  systemctl restart apache2
```

VI. Restart oxAuth server

VII. Update LDAP schema (this is not needed for Couchbase)
1. cp ./output/test/oxauth/schema/102-oxauth_test.ldif /opt/opendj/config/schema/
2. cp ./output/test/scim-client/schema/103-scim_test.ldif /opt/opendj/config/schema/
3. Apply manual schema changes described in ./output/test/scim-client/schema/scim_test_manual_update.schema
4. Create /home/ldap/.pw with LDAP admin user pwd
5. Enable listen on all interfaes:
   /bin/su ldap -c cd /opt/opendj/bin ;  /opt/opendj/bin/dsconfig --trustAll --no-prompt --hostname localhost --port 4444 --bindDN "cn=directory manager" --bindPasswordFile /home/ldap/.pw set-connection-handler-prop --handler-name "LDAPS Connection Handler" --set listen-address:0.0.0.0
6. Restart OpenDJ
7. /bin/su ldap -c cd /opt/opendj/bin ;  /opt/opendj/bin/dsconfig create-backend-index --backend-name userRoot --type generic --index-name myCustomAttr1 --set index-type:equality --set index-entry-limit:4000 --hostName localhost --port 4444 --bindDN "cn=directory manager" -j /home/ldap/.pw --trustAll --noPropertiesFile --no-prompt
8. /bin/su ldap -c cd /opt/opendj/bin ;  /opt/opendj/bin/dsconfig create-backend-index --backend-name userRoot --type generic --index-name myCustomAttr2 --set index-type:equality --set index-entry-limit:4000 --hostName localhost --port 4444 --bindDN "cn=directory manager" -j /home/ldap/.pw --trustAll --noPropertiesFile --no-prompt
9. Remove /home/ldap/.pw

VIII. Update oxIDPAuthentication
1. Update property https://github.com/JansFederation/community-edition-setup/blob/master/templates/configuration.ldif#L26 in DB.
   We need to put DNS name instead of localhost to allow access it remotelly. This needed if we are plannign to run server side on another server.

IX. Prepare for tests run
- git clone https://github.com/JansFederation/oxAuth
- Download and unzip file test_data.zip from CE server.
- Create test profile folders oxAuth/Client/profiles/ce_test and oxAuth/Server/profiles/ce_test
- Copy files from unziped folder test/oxauth/client/* into oxAuth/Client/profiles/ce_test
- Copy files from unziped folder test/oxauth/server/* into oxAuth/Server/profiles/ce_test
- Import HTTP SSL CE Server cert (/etc/certs/httpd.crt) into default java truststore.
  Sample command: keytool -import -alias seed22.jans.org_httpd -keystore cacerts -file httpd.crt
- Import OpenDJ SSL CE Server cert (/etc/certs/opendj.crt) into default java truststore.
  Sample command: keytool -import -alias seed22.jans.org_opendj -keystore cacerts -file opendj.crt

X. Run oxAuth client tests
- cd into oxAuth/Client
- mvn package -Dcfg=ce_test
