# Certificate Generation

Cert authentication module requires certificate chain file and user crtificate in p12 format which user should import to browser certificate store.

For demo purpouses we can generate CA, Intermediate1 and User certs using steps above:

| Command Description | Commands | Sample output |
|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Create file with x509 v3 root-ca-v3.ext settings | cat > root-ca-v3.ext <br/><br/> basicConstraints=critical,CA:TRUE,pathlen:0 <br/> keyUsage = digitalSignature,keyEncipherment,cRLSign,keyCertSign <br/> subjectKeyIdentifier = hash <br/> authorityKeyIdentifier=keyid:always,issuer <br/> extendedKeyUsage = serverAuth,clientAuth,emailProtection |  |
| Generate the certificate for the self signed Root CA | openssl genrsa -aes256 -out root-ca.key 8192 <br/> openssl req -new -sha256 -x509 -days 1826 -key root-ca.key -out root-ca.crt | Country Name (2 letter code) [AU]:US <br/> State or Province Name (full name) [Some-State]:TX <br/> Locality Name (eg, city) []:Austin <br/> Organization Name (eg, company) [Internet Widgits Pty Ltd]:Gluu, Inc. <br/> Organizational Unit Name (eg, section) []:Gluu CA <br/> Common Name (e.g. server FQDN or YOUR name) []:Gluu Root CA <br/> Email Address []: |
| Create Intermediate 1 cert and CSR | openssl genrsa -out intermediate1.key 4096 <br/> openssl req -new -sha256 -key intermediate1.key -out intermediate1.csr | Country Name (2 letter code) [AU]:US <br/> State or Province Name (full name) [Some-State]:TX <br/> Locality Name (eg, city) []:Austin <br/> Organization Name (eg, company) [Internet Widgits Pty Ltd]:Gluu, Inc. <br/> Organizational Unit Name (eg, section) []:Gluu CA <br/> Common Name (e.g. server FQDN or YOUR name) []:Gluu Intermediate CA <br/> Email Address []: |
| Sign the Intermediate1 CSR with the Root CA | openssl x509 -req -in intermediate1.csr -extfile root-ca-v3.ext -CA root-ca.crt -CAkey root-ca.key -set_serial 100 -days 365 -out intermediate1.crt |  |
| Create the certificate chain file by concatenating the Root CA and Intermediate 1 certificates together | cat root-ca.crt intermediate1.crt > ca-chain.cert.pem |  |
| Create file with x509 v3 intermediate1-v3.ext settings | cat > intermediate1-v3.ext basicConstraints = critical,CA:FALSE keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment subjectKeyIdentifier = hash authorityKeyIdentifier = keyid:always,issuer extendedKeyUsage = clientAuth,emailProtection |  |
| Create folder for end user certs | mkdir enduser-certs |  |
| Generate the end user's private key and CSR | openssl genrsa -out enduser-certs/user-gluu.org.key 2048 <br/> openssl req -new -sha256 -key enduser-certs/user-gluu.org.key -out enduser-certs/user-gluu.org.csr | Country Name (2 letter code) [AU]:US <br/> State or Province Name (full name) [Some-State]:TX <br/> Locality Name (eg, city) []:Austin <br/> Organization Name (eg, company) [Internet Widgits Pty Ltd]:Gluu, Inc. <br/> Organizational Unit Name (eg, section) []:IT <br/> Common Name (e.g. server FQDN or YOUR name) []:Full User Name <br/> Email Address []: |
| Sign the user CSR with the Intermediate1 key | openssl x509 -req -in enduser-certs/user-gluu.org.csr -extfile intermediate1-v3.ext -CA intermediate1.crt -CAkey intermediate1.key -set_serial 100 -days 365 -out enduser-certs/user-gluu.org.crt |  |
| Bundle the user’s certificate and key into a p12 pack | openssl pkcs12 -export -out enduser-certs/user-gluu.org.p12 -inkey enduser-certs/user-gluu.org.key -in enduser-certs/user-gluu.org.crt -certfile ca-chain.cert.pem |  |

# Certification script configuration

This module allows to enable few user certificate validiation methods: generic (expiration), path, OCSP, CRL.
Valdiation methods "use_oscp_validator" and "use_crl_validator" can be used only with CA issued certs becuase these send requests to abtain data from issuer servers.

Property "chain_cert_file_path" value is full path to certificate chain file. It can work with self signed certificates too.

