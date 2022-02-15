For testing purpouses there is archive with CA/Intermidiate/User certs in [archive](./sample/generated_certs.zip).

## 1. Create and sign Root CA

### 1.1. Generate password protected a 8192-bit long SHA-256 RSA key for root CA:

`openssl genrsa -aes256 -out rootca.key 8192`

Example output:
```
Generating RSA private key, 8192 bit long modulus (2 primes)
....................+++
........................................................................................................................................................................................................................................................................................................................................................................................+++
e is 65537 (0x010001)
Enter pass phrase for rootca.key:
Verifying - Enter pass phrase for rootca.key:
```

### 1.2 Create the self-signed root CA certificate ca.crt; you'll need to provide an identity for your root CA:

`openssl req -sha256 -new -x509 -days 1826 -key rootca.key -out rootca.crt`

Example output:
```
Enter pass phrase for rootca.key:
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:US
State or Province Name (full name) [Some-State]:TX
Locality Name (eg, city) []:Austin
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Gluu, Inc.
Organizational Unit Name (eg, section) []:Gluu CA
Common Name (e.g. server FQDN or YOUR name) []:Gluu Root CA
Email Address []:
```

### 1.3. Create `root-ca.conf` file:

```
[ ca ]
default_ca = gluuca

[ crl_ext ]
issuerAltName=issuer:copy
authorityKeyIdentifier=keyid:always

[ gluuca ]
dir = ./
new_certs_dir = $dir
unique_subject = no
certificate = $dir/rootca.crt
database = $dir/certindex
private_key = $dir/rootca.key
serial = $dir/certserial
default_days = 730
default_md = sha1
policy = gluuca_policy
x509_extensions = gluuca_extensions
crlnumber = $dir/crlnumber
default_crl_days = 730

[ gluuca_policy ]
commonName = supplied
stateOrProvinceName = supplied
countryName = optional
emailAddress = optional
organizationName = supplied
organizationalUnitName = optional

[ gluuca_extensions ]
basicConstraints = critical,CA:TRUE,pathlen:0
keyUsage = critical,any
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
keyUsage = digitalSignature,cRLSign,keyCertSign
extendedKeyUsage = serverAuth
crlDistributionPoints = @crl_section
subjectAltName  = @alt_names
authorityInfoAccess = @ocsp_section

[alt_names]
DNS.0 = Gluu Intermidiate CA 1
DNS.1 = Gluu CA Intermidiate 1

[crl_section]
URI.0 = http://pki.gluu.org/GluuRoot.crl
URI.1 = http://pki.backup.com/GluuRoot.crl

[ocsp_section]
caIssuers;URI.0 = http://pki.gluu.org/GluuRoot.crt
caIssuers;URI.1 = http://pki.backup.com/GluuRoot.crt
OCSP;URI.0 = http://pki.gluu.org/ocsp/
OCSP;URI.1 = http://pki.backup.com/ocsp/

```
### 1.4. Create a few files where the CA will store it's serials:

```
touch certindex
echo 1000 > certserial
echo 1000 > crlnumber
```

### 1.5. If you need to set a specific certificate start / expiry date, add the following to [gluuca]
```
# format: YYYYMMDDHHMMSS
default_enddate = 20191222035911
default_startdate = 20181222035911
```

## 2. Create and sign Intermediate 1 CA

### 2.1. Generate the intermediate CA's private key:

`openssl genrsa -out intermediate1.key 4096`

Example output:
```
Generating RSA private key, 4096 bit long modulus (2 primes)
........................................................++++
.........++++
e is 65537 (0x010001)
```
### 2.2. Generate the intermediate1 CA's CSR:

`openssl req -new -sha256 -key intermediate1.key -out intermediate1.csr`

Example output:
```
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:US
State or Province Name (full name) [Some-State]:TX
Locality Name (eg, city) []:Austin
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Gluu, Inc.
Organizational Unit Name (eg, section) []:Gluu CA
Common Name (e.g. server FQDN or YOUR name) []:Gluu Intermediate CA
Email Address []:

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

### 2.3. Sign the intermediate1 CSR with the Root CA:

`openssl ca -batch -config root-ca.conf -notext -in intermediate1.csr -out intermediate1.crt`

Example output:
```
Using configuration from root-ca.conf
Enter pass phrase for .//rootca.key:
Check that the request matches the signature
Signature ok
The Subject's Distinguished Name is as follows
countryName           :PRINTABLE:'US'
stateOrProvinceName   :ASN.1 12:'TX'
localityName          :ASN.1 12:'Austin'
organizationName      :ASN.1 12:'Gluu, Inc.'
organizationalUnitName:ASN.1 12:'Gluu CA'
commonName            :ASN.1 12:'Gluu Intermediate CA'
Certificate is to be certified until Dec 22 07:39:24 2021 GMT (730 days)

Write out database with 1 new entries
Data Base Updated
```

### 2.4. Generate the CRL (both in PEM and DER):

```
openssl ca -config root-ca.conf -gencrl -keyfile rootca.key -cert rootca.crt -out rootca.crl.pem
openssl crl -inform PEM -in rootca.crl.pem -outform DER -out rootca.crl
```

Generate the CRL after every certificate you sign with the CA.

### 2.5. Configuring the Intermediate CA 1

Create a new folder for this intermediate and move in to it:

```
mkdir intermediate1
cd ./intermediate1
```

Copy the Intermediate cert and key from the Root CA:

`mv ../intermediate1.* ./`

Create the index files:

```
touch certindex
echo 1000 > certserial
echo 1000 > crlnumber
```

### 2.6. Create a new intermediate-ca.conf file:

```
[ ca ]
default_ca = gluuca

[ crl_ext ]
issuerAltName=issuer:copy
authorityKeyIdentifier=keyid:always

[ gluuca ]
dir = ./
new_certs_dir = $dir
unique_subject = no
certificate = $dir/intermediate1.crt
database = $dir/certindex
private_key = $dir/intermediate1.key
serial = $dir/certserial
default_days = 365
default_md = sha1
policy = gluuca_policy
x509_extensions = gluuca_extensions
crlnumber = $dir/crlnumber
default_crl_days = 365

[ gluuca_policy ]
commonName = supplied
stateOrProvinceName = supplied
countryName = optional
emailAddress = optional
organizationName = supplied
organizationalUnitName = optional

[ gluuca_extensions ]
basicConstraints = critical,CA:FALSE
keyUsage = critical,any
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
keyUsage = digitalSignature, nonRepudiation, keyEncipherment
extendedKeyUsage = clientAuth
crlDistributionPoints = @crl_section
subjectAltName  = @alt_names
authorityInfoAccess = @ocsp_section

[alt_names]
DNS.0 = example.com
DNS.1 = example.org

[crl_section]
URI.0 = http://pki.gluu.org/GluuIntermidiate1.crl
URI.1 = http://pki.backup.com/GluuIntermidiate1.crl

[ocsp_section]
caIssuers;URI.0 = http://pki.gluu.org/GluuIntermediate1.crt
caIssuers;URI.1 = http://pki.backup.com/GluuIntermediate1.crt
OCSP;URI.0 = http://pki.gluu.org/ocsp/
OCSP;URI.1 = http://pki.backup.com/ocsp/
```

Change the [alt_names] section to whatever you need as Subject Alternative names. Remove it including the subjectAltName = @alt_names line if you don't want a Subject Alternative Name.

If you need to set a specific certificate start / expiry date, add the following to [gluuca]

```
# format: YYYYMMDDHHMMSS
default_enddate = 20191222035911
default_startdate = 20181222035911
```

Generate an empty CRL (both in PEM and DER):

```
openssl ca -config intermediate-ca.conf -gencrl -keyfile intermediate1.key -cert intermediate1.crt -out intermediate1.crl.pem
openssl crl -inform PEM -in intermediate1.crl.pem -outform DER -out intermediate1.crl
```

### 2.7. This is sample to show how to revoke cert. Use it only when you need to revoke the intermediate cert:

`openssl ca -config root-ca.conf -revoke intermediate1.crt -keyfile rootca.key -cert rootca.crt`


## 3. Creating end user certificates
We use this new intermediate CA to generate an end user certificate. Repeat these steps for every end user certificate you want to sign with this CA.

### 3.1. Create folder for end user certs:
`mkdir enduser-certs`

### 3.2. Generate the end user's private key:

`openssl genrsa -out enduser-certs/user-gluu.org.key 4096`

### 3.3. Generate the end user's CSR:

`openssl req -new -sha256 -key enduser-certs/user-gluu.org.key -out enduser-certs/user-gluu.org.csr`

Example output:
```
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:US
State or Province Name (full name) [Some-State]:TX
Locality Name (eg, city) []:Austin
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Gluu, Inc.
Organizational Unit Name (eg, section) []:User
Common Name (e.g. server FQDN or YOUR name) []:Full User Name
Email Address []:

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

### 3.4. Sign the end user's CSR with the Intermediate 1 CA:

`openssl ca -batch -config intermediate-ca.conf -notext -in enduser-certs/user-gluu.org.csr -out enduser-certs/user-gluu.org.crt`

Example output:
```
Using configuration from intermediate-ca.conf
Check that the request matches the signature
Signature ok
The Subject's Distinguished Name is as follows
countryName           :PRINTABLE:'US'
stateOrProvinceName   :ASN.1 12:'TX'
localityName          :ASN.1 12:'Austin'
organizationName      :ASN.1 12:'Gluu, Inc.'
organizationalUnitName:ASN.1 12:'User'
commonName            :ASN.1 12:'Full User Name'
Certificate is to be certified until Dec 22 08:07:15 2020 GMT (365 days)

Write out database with 1 new entries
Data Base Updated
```

### 3.5. Generate the CRL (both in PEM and DER):

```
openssl ca -config intermediate-ca.conf -gencrl -keyfile intermediate1.key -cert intermediate1.crt -out intermediate1.crl.pem
openssl crl -inform PEM -in intermediate1.crl.pem -outform DER -out intermediate1.crl
```

Generate the CRL after every certificate you sign with the CA.

### 3.6. Create the certificate chain file by concatenating the Root and intermediate 1 certificates together.

`cat ../rootca.crt intermediate1.crt > enduser-certs/user-gluu.org.chain`

Send the following files to the end user:

```
user-gluu.org.crt
user-gluu.org.key
user-gluu.org.chain
```

You can also let the end user supply their own CSR and just send them the .crt file. Do not delete that from the server, otherwise you cannot revoke it.

### 3.7. This is sample to show how to revoke cert. Use it only when you need to revoke the end users cert:

`openssl ca -config intermediate-ca.conf -revoke enduser-certs/enduser-gluu.org.crt -keyfile intermediate1.key -cert intermediate1.crt`

## 4. Validating the certificate

### 4.1. You can validate the end user certificate against the chain using the following command:

`openssl verify -CAfile enduser-certs/user-gluu.org.chain enduser-certs/user-gluu.org.crt`

Example output:
```
enduser-certs/user-gluu.org.crt: OK
```

### 4.2. You can also validate it against the CRL. Concatenate the PEM CRL and the chain together first:

`cat ../rootca.crt intermediate1.crt intermediate1.crl.pem > enduser-certs/user-gluu.org.crl.chain`

Verify the certificate:

`openssl verify -crl_check -CAfile enduser-certs/user-gluu.org.crl.chain enduser-certs/user-gluu.org.crt`

Example output:
```
enduser-certs/user-gluu.org.crt: OK
```

### 5. Export end user certificate to PKCS#12

Convert a PEM certificate file and a private key to PKCS#12 (.pfx .p12)

```
openssl pkcs12 -export -out enduser-certs/user-gluu.org.pfx -inkey enduser-certs/user-gluu.org.key -in enduser-certs/user-gluu.org.crt -certfile enduser-certs/user-gluu.org.chain
```
                                                                                           
