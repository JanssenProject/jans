# Jans Tent

A reliable OpenID client to be used in auth testing.

## Installation

1. Navigate tho the project root folder `jans/demos/jans-tent`
2. Create virtual environment
  ```bash
  python3 -m venv venv
  ```
3. Activate the virtual virtual environment
  ```bash
   source venv/bin/activate 
  ```
4. Install dependencies
  ```bash
  pip install -r requirements.txt
  ```

## Setup

* Create client on Auth server, i.e.:
  * response_type `code`
  * redirect_uri `https://localhost:9090/oidc_callback`
  * Grants `authorization_code`
  * client authn at token endpoint `client_secret_post`
  * scopes `openid` `profile` `email`
  Please notice: You may also use the `register` endpoint, still to be documented.

* Edit configuration file `clientapp/config.py` according to your needs. I.e:
  * Input client_id and secret from above step
  * Set OpenID configuration endpoint URL (`SERVER_META_URL`)

* Generate test RP server self signed certs

Generate `key.pem` and `cert.pem` at `jans-tent` project root, i.e:
`openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 365 -nodes`

* Import your Auth Server certificate and add it to `CERT_PATH`, `SSL_CERT_FILE`, `REQUESTS_CA_BUNDLE`.

Example:
```bash
export CERT_PATH=$(python3 -m certifi)
export SSL_CERT_FILE=${CERT_PATH}
export REQUESTS_CA_BUNDLE=${CERT_PATH}
mv issuer.cer $(python3 -m certifi)
```

* Run server

```bash
python3 main.py
```

* navigate to `https://localhost:9090/protected-content`

## Extra Features

### Auto-register endpoint

Sending a `POST` request to `/register` endpoint containing a `JSON` with the OP/AS url and client url, like this:

```json
{
    "op_url": "https://oidc-provider.jans.io",
    "client_url": "https://my-client.mydomain.com"
}
```

Will return client id and client secret

### Auto-config endpoint

Sending a `POST` request to `/configuration` endpoint, containing client id, client secret, and metadata endpoint will fetch data from metadata url and override `config.py` settings during runtime.

```json
{
    "client_id": "e4f2c3a9-0797-4c6c-9268-35c5546fb3e9",
    "client_secret": "5c9e4775-0f1d-4a56-87c9-a629e1f88b9b",
    "op_metadata_url": "https://oidc-provider.jans.io/.well-known/openid-configuration"
}
```
