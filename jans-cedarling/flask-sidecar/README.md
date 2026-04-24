# Cedarling Flask Sidecar

This is a Flask API that implements the [AuthZen](https://openid.github.io/authzen/) specification with the [cedarling](../) python binding.

## Running

To run the API, you will need python 3.11 installed.

- Install [uv](https://docs.astral.sh/uv/getting-started/installation/)
- Clone the [Janssen](https://github.com/JanssenProject/jans) repository:
  ```
  git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans \
  && cd jans \
  && git sparse-checkout init --cone \
  && git checkout main \
  && git sparse-checkout set jans-cedarling
  ```
- Navigate to `jans-cedarling/flask-sidecar`
- Run `uv sync --locked --python 3.11` to install dependencies
- Download the [binding library](https://github.com/JanssenProject/jans/releases/download/nightly/cedarling_python-0.0.0-cp311-cp311-manylinux_2_34_x86_64.whl)
- Install the binding to your local environment: `uv pip install ./cedarling_python-0.0.0-cp311-cp311-manylinux_2_34_x86_64.whl`
- Activate the virtual environment: `source .venv/bin/activate`
- Navigate to `main/`
- Run `flask run` to run the API on `http://127.0.0.1:5000`

## Configuration

For running via poetry, the sidecar supports the following environment variables:

| Variable name                   | Default value | Supported value(s)               |
| ------------------------------- | ------------- | -------------------------------- |
| APP_MODE                        | testing       | development, testing, production |
| CEDARLING_BOOTSTRAP_CONFIG_FILE | None          | Path to your configuration       |
| SIDECAR_DEBUG_RESPONSE          | False         | True, False                      |

- Navigate to `jans/jans-cedarling/flask-sidecar/main` and create a file named `.env`
- Set environment variables like so:

```
APP_MODE=development
CEDARLING_BOOTSTRAP_CONFIG_FILE=/path/to/bootstrap.json
SIDECAR_DEBUG_RESPONSE=False
```
## Tests

Not yet implemented

# Docker Instructions

- **Note**: Currently only remote policy stores via URI passing is supported on Docker builds.
- Create a file called `bootstrap.json`. You may use this [sample](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/flask-sidecar/demo/bootstrap.json) file.
- Modify the file to your specifications. Refer to the [Cedarling properties documentation](../../docs/cedarling/reference/cedarling-properties.md) for configuration values.
- Pull the docker image:
  ```
  docker pull ghcr.io/janssenproject/jans/cedarling-flask-sidecar:0.0.0-nightly
  ```
- Run the docker image, replacing `</absolute/path/to/bootstrap.json>` with the absolute path to your bootstrap file:

  ```bash
  docker run -d \
    -e APP_MODE='development' \
    -e CEDARLING_BOOTSTRAP_CONFIG_FILE=/bootstrap.json \
    -e SIDECAR_DEBUG_RESPONSE=False \
    --mount type=bind,src=</absolute/path/to/bootstrap.json>,dst=/bootstrap.json \
    -p 5000:5000 \
    ghcr.io/janssenproject/jans/cedarling-flask-sidecar:0.0.0-nightly
  ```
- The service is running on `http://0.0.0.0:5000`. OpenAPI documentation is available at `/swagger-ui`

## Docker Compose Instructions

- Clone the [Janssen](https://github.com/JanssenProject/jans) repository:

  ```bash
  git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans
  cd jans
  git sparse-checkout init --cone
  git checkout main
  git sparse-checkout set jans-cedarling
  cd jans-cedarling/flask-sidecar
  ```
- Modify the `demo/bootstrap.json` file to your specifications. Refer to the [Cedarling properties documentation](../../docs/cedarling/reference/cedarling-properties.md) for configuration values.
- Run `docker compose up`
- The service is running on `http://0.0.0.0:5000`. OpenAPI documentation is available at `/swagger-ui`

## OpenAPI

OpenAPI documentation is found in `flask-sidecar.yml`
