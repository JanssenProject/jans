# Jans Tent

A reliable OpenID client to be used in auth testing.

## Installation

**Note**: *If you are using Mac, **or** want to use different python versions, use **Pyenv**.*

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

### 1. Edit configuration file `clientapp/config.py` according to your needs:
  * Set `ISSUER`, replace `op_hostname` (required)
  * Set any other desired configuration

### 2. Generate test RP server self signed certs

Generate `key.pem` and `cert.pem` at `jans-tent` project root folder (`jans/demos/jans-tent`). i.e: 
```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 365 -nodes
```

### 3. Import your Auth Server certificate and add it to `CERT_PATH`, `SSL_CERT_FILE`, `REQUESTS_CA_BUNDLE`.

(remember to be inside your virtual environment)

Replace `OP_HOSTNAME` with the op hostname being used.
```bash
echo | openssl s_client -servername OP_HOSTNAME -connect OP_HOSTNAME:443 | sed -ne \
 '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > op_web_cert.cer
```

```bash
export CERT_PATH=$(python3 -m certifi) && export SSL_CERT_FILE=${CERT_PATH} \
&& export REQUESTS_CA_BUNDLE=${CERT_PATH} && mv op_web_cert.cer $CERT_PATH

```

## Using the server

### Start the server

Please notice that your client will be automatically registered once the server starts.
If your server was already registered, when you start the server again, it won't register.

(remember to be inside your virtual environment)
```bash
python main.py
```

### Start the flow

Navigate to `https://localhost:9090` and click the link to start.


## Extra Features

### Manual client configuration

In case your OP don't support dynamic registration, manually configure your client.

create a file caled `client_info.json` in `jans-tent`folder as the example:
```json
{
    "op_metadata_url": "https://op_hostname/.well-known/openid-configuration",
    "client_id": "e4f2c3a9-0797-4c6c-9268-35c5546fb3e9",
    "client_secret": "a3e71cf1-b9b4-44c5-a9e6-4c7b5c660a5d"
}
```


### Auto-register endpoint

Sending a `POST` request to `/register` endpoint containing a `JSON` with the OP/AS url and client url, like this:

```json
{
    "op_url": "https://oidc-provider.jans.io",
    "client_url": "https://localhost:9090"
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
