# Jans Tent

To test an OpenID Provider ("OP"), you need a test Relying Party ("RP"). Jans
Tent is easy to configure RP which enables you to send different requests by
quickly modifying one file (`config.py`). It's a Python Flask application,
so it's easy to hack for other testing requirements.

By default, it uses `localhost` as the `redirect_uri`, so if you run it on your
laptop, all you need to do is specify the OP hostname to run it. Tent uses
dynamic client registration to obtain client credentials. But you can also use
an existing client_id if you like.

## Installation

**Important**: Ensure you have `Python >= 3.11`

**Mac Users**: We recommend using [pyenv - simple python version management](https://github.com/pyenv/pyenv) instead of Os x native python.

1. Navigate to the project root folder `jans/demos/jans-tent`
2. Create virtual environment
```bash
python3 -m venv venv
````
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

### 3. Import your OP TLS certificate

(remember to be inside your virtual environment)

Supply the hostname of the ISSUER after the `=`

```bash
export OP_HOSTNAME=
```

```bash
echo | openssl s_client -servername $OP_HOSTNAME -connect $OP_HOSTNAME:443 | sed -ne  '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > op_web_cert.cer
```

```bash
export CERT_PATH=$(python3 -m certifi)
```

```bash
export SSL_CERT_FILE=${CERT_PATH}
```

```bash
export REQUESTS_CA_BUNDLE=${CERT_PATH} && mv op_web_cert.cer $CERT_PATH
```

## Using the server

### Start the server

Please notice that your client will be automatically registered once the server
starts. If your client was already registered, when you start the server again,
it won't register. Remember to be inside your virtual environment!

```bash
python main.py
```

### Login!

Navigate your browser to `https://localhost:9090` and click the link to start.

## Manual client configuration

In case your OP doesn't support dynamic registration, manually configure your
client by creating a file caled `client_info.json` in the `jans-tent` folder
with the following claims:

```json
{
    "op_metadata_url": "https://op_hostname/.well-known/openid-configuration",
    "client_id": "e4f2c3a9-0797-4c6c-9268-35c5546fb3e9",
    "client_secret": "a3e71cf1-b9b4-44c5-a9e6-4c7b5c660a5d"
}
```

## Updating Tent to use a different OP

If you want to test a different OP, do the following:

1. Remove `op_web_cert` from the tent folder, and follow the procedure above
to download and install a new OP TLS certificate
2. Remove `client_info.json` from the tent folder
3. Update the value of `ISSUER` in `./clientapp/config.py`
4. Run `./register_new_client.py`

## Other Tent endpoints

### Auto-register endpoint

Sending a `POST` request to Jans Tent `/register` endpoint containing a `JSON`
with the OP/AS url and client url, like this:

```json
{
  "op_url": "https://OP_HOSTNAME",
  "client_url": "https://localhost:9090",
  "additional_params": {
    "scope": "openid mail profile"
  }
}
```
Please notice that `additional_params` is not required by endpoint.

The response will return the registered client id and client secret 

### Auto-config endpoint

Sending a `POST` request to the Tent `/configuration` endpoint, containing the
client id, client secret, and metadata endpoint will fetch data from OP metadata
url and override the `config.py` settings during runtime.

```json
{
    "client_id": "e4f2c3a9-0797-4c6c-9268-35c5546fb3e9",
    "client_secret": "5c9e4775-0f1d-4a56-87c9-a629e1f88b9b",
    "op_metadata_url": "https://OP_HOSTNAME/.well-known/openid-configuration"
}
```
