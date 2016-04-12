# oxEleven

## Installation

  1. Install [SoftHSM version 2](https://github.com/opendnssec/SoftHSMv2)

  2. Copy the file Server/conf/oxeleven-config.json to tomcat/conf/oxeleven-config.json

  3. Edit the configuration file tomcat/conf/oxeleven-config.json

  ```javascript
  {
    "pkcs11Config": {
      "name": "SoftHSM",
      "library": "/usr/local/lib/softhsm/libsofthsm2.so",
      "slot": "0",
      "showInfo": "true"
    },
    "pkcs11Pin": "1234",
    "dnName": "CN=oxAuth CA Certificate"
  }
  ```

  4. Deploy oxEleven.war in Tomcat
  
## Test

  1. Ensure oxEleven is deployed an running
  
  2. Edit the file Client/src/test/Resources/testng.xml to point to your oxEleven deployment
  
  3. cd Client
  
  4. mvn test

To access Gluu support, please register and open a ticket on [Gluu Support](http://support.gluu.org)
