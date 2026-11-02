### 1. Create a user_cert.conf file and update `user_dn` section:

```
[ req ]
days                   = 365
prompt                 = no
distinguished_name     = user_dn
x509_extensions        = v3_ca

[ user_dn ]
           countryName = US
   stateOrProvinceName = TX
          localityName = Austin
      organizationName = Gluu, Inc.
organizationalUnitName = Gluu SSL department
            commonName = secure.gluu.com
          emailAddress = john@gluu.org

[ v3_ca ]
basicConstraints       = CA:FALSE
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always, issuer
extendedKeyUsage       = clientAuth, emailProtection
```

### 2. Generate user cert

```
openssl req -x509 -config user_cert.conf -nodes -newkey rsa:4096 -keyout user_cert.key -out user_cert.crt
```

### 3. Export end user certificate to PKCS#12
```
openssl pkcs12 -export -inkey user_cert.key -in user_cert.crt -out user_cert.p12
```
