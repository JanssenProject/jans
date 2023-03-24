# Client Registration Interception Script

## Overview
The Janssen Authorization Server uses **interception scripts** to enable you to customize the behavior of the OpenID Provider. During client registration, custom interception scripts can be used to implement custom business logic. For instance, data could be validated, extra client claims could be populated, scopes could be modified, or APIs could be called to determine whether the client should get registered at all.

## Configuration Prerequisites
- A Janssen Authorization Server installation
- [Client Registration script](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/jans-linux-setup/jans_setup/openbanking/static/extension/client_registration/Registration.py) - included in the default Janssen OpenBanking distribution
- Setting configuration parameters
- Setting third party library (Jose4j) in classpath 
 
## Adding the custom script

1. To add or update custom scripts, you can use either jans-cli or curl. jans-cli in interactive mode, option 13 enables you manage custom scripts. For more info, see the [docs](https://github.com/JanssenProject/home/wiki/Custom-Scripts-using-jans-cli).
1. jans-cli in command line argument mode is more conducive to scripting and automation. To display the available operations for custom scripts, use config-cli.py --info CustomScripts. See the [docs](../../../admin/config-guide/jans-cli/index.md) for more info.
1. To use `curl` see these [docs](../../../admin/config-guide/curl.md)

!!! Note
    You can normally find `jans-cli.py` in the `/opt/jans/jans-cli/` folder. 

## Configuring keys, certificates and SSA validation endpoints

The client registration custom script will use configuration parameters like SSA Validation endpoint, JWKS endpoints, keystores, trust stores etc which are listed in the tabular format below.

|	Property	|	Description	|	Example		|
|-----------------------|---------------|-----------------------|
|clientScopes           |Used in SSA validation|ASPSPReadAccess AuthoritiesReadAccess TPPReadAccess|
|keyId                  |Used in SSA Validation, kid used while encoding a JWT sent to token URL     | XkwIzWy44xWSlcWnMiEc8iq9s2G|
|signingCert            |Used in SSA Validation, location of cert used for signing |/etc/certs/obieDir/obsigning-axV5umCvTMBMjPwjFQgEvb_NO_UPLOAD.key		|
|signingKey             |Used in SSA Validation, location of key used for signing |/etc/certs/obieDir/obsigning-axV5umCvTMBMjPwjFQgEvb_NO_UPLOAD.key		|
|trustKeyStore          |Used in SSA Validation, Trust store |/etc/certs/obieDir/ob_transport_root.p12|
|trustKeyStorePassword  |Used in SSA Validation, Trust store Password, currently plaintext, but should be encrypted|abcdefg|
|transportKeyStore      |Used in SSA validation, a .p12 file presented by AS to the token URL |/etc/certs/obieDir/axv5umcvtmbmjpwjfqgevb_openbanking_pre_production_issuing_ca_.p12		|
|transportKeyStorePassword      |Used in SSA validation |abcdefg		|
|tokenUrl|Used in SSA validation to obtain token to query SCIM endpoint. Details here - https://openbanking.atlassian.net/wiki/spaces/DZ/pages/1150124033/Directory+2.0+Technical+Overview+v1.5#Directory2.0TechnicalOverviewv1.5-ManageDirectoryInformation |https://matls-sso.openbankingtest.org.uk/as/token.oauth2|
|tppUrl|Used in SSA validation to query SCIM endpoint. Details here - https://openbanking.atlassian.net/wiki/spaces/DZ/pages/1150124033/Directory+2.0+Technical+Overview+v1.5#Directory2.0TechnicalOverviewv1.5-ManageDirectoryInformation |https://matls-api.openbankingtest.org.uk/scim/v2/OBThirdPartyProviders/|
|jwks_endpoint |Used for signing software statement and request object for DCR|https://keystore.openbankingtest.org.uk/keystore/openbanking.jwks|
  
**Steps to add / edit / delete configuration parameters:**

1. Place a [JSON file](https://github.com/JanssenProject/jans-setup/blob/openbank/static/extension/client_registration/clientregistration.json) containing the above configuration parameters and the [custom script](https://github.com/JanssenProject/jans-setup/blob/openbank/static/extension/client_registration/Registration.py) in a folder. 

1. From this folder, run the following command: 

    ```
    python3 jans-cli-linux-amd64.pyz --operation-id post-config-scripts --data /clientregistration.json \
    --cert-file jans_cli_client.crt --key-file jans_cli_client.key
    ```

## Adding Jose4j library in classpath

This script uses [jose4j](https://bitbucket.org/b_c/jose4j/wiki/Home) library for JavaScript object signing and encryption.

**If using the VM distribution:**

1. Download the [`jose4j-0.7.7.jar`](https://bitbucket.org/b_c/jose4j/downloads/) and  Place  in `/opt/jans/jetty/jans-auth/custom/libs/`

1. Change your current working directory to `/opt/jans/jetty/jans-auth/webapps` and edit `jans-auth.xml` to add this line:  
 
    ```
    <Set name="extraClasspath">/opt/jans/jetty/jans-auth/custom/libs/jose4j-0.7.7.jar</Set>
    ```

1. Restart the Auth Server, `systemctl restart jans-auth` 

**If using the cloud-native distribution:**

1.  Download the [`jose4j-0.7.7.jar`](https://bitbucket.org/b_c/jose4j/downloads/)

1.   Create a `ConfigMap` containing the jar.

    ```bash
    kubectl create cm jose4j -n <gluu-namespace> --from-file=jose4j-0.7.7.jar
    ```

1. Add the volume and volume mount to `auth-server` in your [`override.yaml`](https://gluu.org/docs/openbanking/install-cn/#helm-valuesyaml) helm configuration.

    ```yaml
    volumes:
        - name: jose4j
          configMap:
            name: jose4j
    volumeMounts:
        - name: jose4j
          mountPath: "/opt/jans/jetty/jans-auth/custom/libs/jose4j-0.7.7.jar"
            subPath: jose4j-0.7.7.jar
    ```

1.  Run helm upgrade.

    ```bash
    helm upgrade gluu gluu/gluu -n <gluu-namespace> --version=5.0.0 -f override.yaml
    ```       
       
### Understanding the Script

The [Client Registration script](https://github.com/JanssenProject/jans-setup/blob/openbank/static/extension/client_registration/Registration.py) is available

The following are the ***mandatory*** functions which need to be implemented in order to perform registration:

1. Create a class of the type ```ClientRegistrationType``` and initialize the script

    ```python3
    class ClientRegistration(ClientRegistrationType):

        def __init__(self, currentTimeMillis):
            self.currentTimeMillis = currentTimeMillis
    
        def init(self, customScript, configurationAttributes):
            print "Client registration. Initialization"
 
            if (not configurationAttributes.containsKey("param")):
	       print "Client registration. Initialization. Property keyId is not specified"
	       return False
            else: 
               self.param = configurationAttributes.get("param").getValue2() 
            
            print "Client registration. Initialized successfully"
   
            return True

        def destroy(self, configurationAttributes):
            print "Client registration. Destroy"
            print "Client registration. Destroyed successfully"
            return True
    ```

2. The createClient method contains the main business logic, here the context refers to io.jans.as.server.service.external.context.DynamicClientRegistrationContext. It has several useful methods for SSA validations and options to create and throw custom exception (http status, error and error description):

    ```python3
    def createClient(self, context):

        # 1. obtain client id. certProperty contains the httpRequest.getHeader("X-ClientCert"), inshort client certificate passed to the /register endpoint
        cert = CertUtils.x509CertificateFromPem(configurationAttributes.get("certProperty").getValue1())
        cn = CertUtils.getCN(cert)

        # 2. validate SSA 
        valid = self.validateSoftwareStatement(cn)
        if valid == False:
             print "Invalid software statement"
             return False
        print "Client registration. cn: " + cn
        client.setDn("inum=" + cn + ",ou=clients,o=jans")

        # 3. Used to indicate that only this is a trusted client (note that consent is managed by Internal OP / consent app)
        client.setTrustedClient(True)
        client.setPersistClientAuthorizations(False)

        # 4. in order to run introspection script, assign it to run for this client
        client.setAccessTokenAsJwt(True)
        client.getAttributes().setRunIntrospectionScriptBeforeJwtCreation(True)  
        dnOfIntrospectionScript = "inum=CABA-2222,ou=scripts,o=jans"
        client.getAttributes().getIntrospectionScripts().add(dnOfIntrospectionScript)
        
        # 5. mandatory fields which should be set in the script
        client.setClientId(cn)
        client.setJwksUri(Jwt.parse(registerRequest.getSoftwareStatement()).getClaims().getClaimAsString("org_jwks_endpoint"))
        
        return True 
    ```

3. Miscellaneous mandatory functions

   Used for signing the software statement:
    
   ```python3
   def getSoftwareStatementJwks(self, context):
       return JwtUtil.getJSONWebKeys(self.jwks_endpoint).toString()
   ```

   HMAC not applicable, return an empty string:  
 
   ```python3
   def getDcrHmacSecret(self, context):
       return ""
   ```    

   Used for signing the request object (DCR): 
    
   ```python3
   def getDcrJwks(self, context):
       return JwtUtil.getJSONWebKeys(self.jwks_endpoint).toString()
   ```

4. An important method of DCR flow to update the client details 

* The updateClient method

  This updateClient method is called when the PUT method is called on registration endpoint to update client details. This method should return True for successful update and to reject any update request this method should return False when the request fulfills the condition to reject it:

    
   ```python3
   def   updateClient(self, context):
        print "Client registration. UpdateClient method"
        return True
   ```
