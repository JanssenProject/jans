# Cedarling Flask Sidecar

This is a Flask API that implements the [AuthZen](https://openid.github.io/authzen/) specification with the [cedarling](../) python binding. 

## Running

To run the API:

- Install [poetry](https://python-poetry.org/docs/#installation)
- Clone the [Janssen](https://github.com/JanssenProject/jans) repository
    ```
    git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans /tmp/jans \
        && cd /tmp/jans \
        && git sparse-checkout init --cone \
        && git checkout main \
        && git sparse-checkout set jans-cedarling
    ```
- Navigate to `jans/jans-cedarling/flask-sidecar/main`
- Run `poetry install` to install dependencies
- Run `poetry run flask run` to run the API on `http://127.0.0.1:5000` 

## Tests

Not yet implemented

## Docker Instructions

- Clone the [Janssen](https://github.com/JanssenProject/jans) repository
- Navigate to `jans/jans-cedarling/flask-sidecar/`
- Modify the `secrets/bootstrap.json` file to your specifications. Configuration values are described [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/cedarling_python.pyi).
- Run `docker compose up`
- The service is running on `http://0.0.0.0:5000`. OpenAPI documentation is available at `/swagger-ui`

## OpenAPI

OpenAPI documentation is found in `flask-sidecar.yml`
