## 1. Generate a certificate authority (CA) cert.

### 1. Generate your CA certificate using this command:

`openssl req -newkey rsa:4096 -keyform PEM -keyout ca.key -x509 -days 3650 -outform PEM -out ca.cer`

## 2. Generate a client SSL certificate

### 1. Generate a private key for the SSL client.

`openssl genrsa -out client.key 4096`

### 2. Use the client's private key to generate a cert request.

`openssl req -new -key client.key -out client.req`

### 3. Issue the client certificate using the cert request and the CA cert/key.

`openssl x509 -req -in client.req -CA ca.cer -CAkey ca.key -set_serial 101 -extensions client -days 365 -outform PEM -out client.cer`

### 4. Convert the client certificate and private key to pkcs#12 format for use by browsers.

`openssl pkcs12 -export -inkey client.key -in client.cer -out client.p12`

### 5. Clean up - remove the client private key, client cert and client request files as the pkcs12 has everything needed.

`rm client.key client.cer client.req`
