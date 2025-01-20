# Cedarling Flask Sidecar

This is a Flask API that implements the [AuthZen](https://openid.github.io/authzen/) specification with the [cedarling](../) python binding. 

## Running

To run the API:

- Install [poetry](https://python-poetry.org/docs/#installation)
- Clone the [Janssen](https://github.com/JanssenProject/jans) repository:
    ```
    git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans
    ```
    ```
    cd jans
    ```
    ```
    git sparse-checkout init --cone
    ```
    ```
    git checkout main
    ```

    ```
    git sparse-checkout set jans-cedarling
    ```
- Navigate to `jans-cedarling/flask-sidecar`
- Run `poetry install` to install dependencies
- Navigate to `main/`
- Run `poetry run flask run` to run the API on `http://127.0.0.1:5000` 

## Configuration

For running via poetry, the sidecar supports the following environment variables:

| Variable name | Default value | Supported value(s) |
| ------------- | ------------- | ------------------ |
| APP_MODE | testing | development, testing, production |
| CEDARLING_BOOTSTRAP_CONFIG_FILE | None | Path to your configuration |
| SIDECAR_DEBUG_RESPONSE | False | True, False |

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

- Create a file called `bootstrap.json`. You may use this [sample](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/flask-sidecar/secrets/bootstrap.json) file. 
- Modify the file to your specifications. Configuration values are described [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/cedarling_python.pyi).
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
		-p 5000:5000\
		ghcr.io/janssenproject/jans/cedarling-flask-sidecar:0.0.0-nightly
	```
- The service is running on `http://0.0.0.0:5000`. OpenAPI documentation is available at `/swagger-ui`

## Docker Compose Instructions (for development)

- Clone the [Janssen](https://github.com/JanssenProject/jans) repository
- Navigate to `jans/jans-cedarling/flask-sidecar/`
- Modify the `secrets/bootstrap.json` file to your specifications. Configuration values are described [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/cedarling_python.pyi).
    - The default configuration expects you to provide a URL to a policy store file via `CEDARLING_POLICY_STORE_URI`. If you want to use a local policy store via `CEDARLING_POLICY_STORE_FN`, you need to mount it inside the docker image. Place your policy store file in the `secrets` folder and edit the Dockerfile at line 46 to add this line:

    ```
    ...
    COPY --chown=1000:1000 ./secrets/<policy store file>.json /api/
    ...
    ```
- Run `docker compose up`
- The service is running on `http://0.0.0.0:5000`. OpenAPI documentation is available at `/swagger-ui`

## OpenAPI

OpenAPI documentation is found in `flask-sidecar.yml`
