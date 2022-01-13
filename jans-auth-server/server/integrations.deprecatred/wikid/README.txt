This is a person authentication module for oxAuth that enables [Wikid Authentication](http://wikidsystems.com) for user authentication.

The module has a few properties:

1) wikid_server_host - It's mandatory property. IP address of WIKID server.
   Example: 111.111.111.111

2) wikid_server_port - It's mandatory property. TCP port number to connect to (default 8388).
   Example: 8388

3) wikid_cert_path - It's mandatory property. Path to the PKCS12 certificate file.
   Example: /etc/certs/wikid.p12

4) wikid_cert_pass - It's mandatory property. Passphrase to open the PKCS12 file.
   Example: changeit

5) wikid_ca_store_path - It's mandatory property. The certificate authority store for validating the WAS server certificat.
   Example: /etc/certs/CACertStore.dat

6) wikid_ca_store_pass - It's mandatory property. The passphrase securing the trust store file.
   Example: changeit

7) wikid_server_code - It's mandatory property. The 12-digit code that represents the server/domain.
   Example: 222222222222


This module require few java libraries. Before enabling this module it's mandatory to put next libraries into $TOMCAT_HOME/endorsed
1) https://www.wikidsystems.com/webdemo/wClient-3.5.0.jar
2) http://central.maven.org/maven2/org/jdom/jdom/1.1.3/jdom-1.1.3.jar
3) http://central.maven.org/maven2/log4j/log4j/1.2.17/log4j-1.2.17.jar
4) http://central.maven.org/maven2/com/thoughtworks/xstream/xstream/1.4.8/xstream-1.4.8.jar

More information about wClient library there is on this page: https://www.wikidsystems.com/downloads/network-clients

Also this 2F authentication method requires token client: https://www.wikidsystems.com/downloads/token-clients
Hence in order to use this person authentication module user should install and configure it for first time use. This demo explains how to do that:
https://www.wikidsystems.com/demo
