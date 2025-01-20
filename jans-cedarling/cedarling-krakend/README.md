# Cedarling KrakenD Plugin

This is a [KrakenD HTTP server plugin](https://www.krakend.io/docs/extending/http-server-plugins/) that intercepts a call to a selected endpoint, calls the [sidecar](../flask-sidecar) and allows access only if the sidecar responds with `true`.

 ## Building

Krakend recommends building via their builder docker image, to produce builds that match the target architecture and Go version. To build:

- Clone the Janssen repository
    ```
    git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans
    cd jans
    git sparse-checkout init --cone
    git checkout main
    git sparse-checkout set cedarling-krakend
    cd cedarling-krakend
    ```
- Build the plugin: 
    
    - For Docker targets:

    ```
    docker run -it -v "$PWD:/app" -w /app krakend/builder:2.9.0 go build -buildmode=plugin -o cedarling-krakend.so .
    ```

    - For on-premise installations:

    ```
    docker run -it -v "$PWD:/app" -w /app krakend/builder:2.9.0-linux-generic go build -buildmode=plugin -o yourplugin.so .
    ```

This will create a plugin build for KrakenD version `2.9.0`. If you are using a different version of KrakenD, replace the build tag in the command like so: `krakend/builder:x.y.z`.

## Configuration

See `krakend.json` to see an example KrakenD configuration which loads the plugin. The following table describes the plugin-specific configuration keys. These keys are mandatory and must be provided.

| Field | Type | Example | Description |
|-------|------|---------|-------------|
| path   | String  | /protected | KrakenD endpoint to protect |
| sidecar_endpoint | String | http://127.0.0.1:5000/cedarling/evaluation | Sidecar evaluation URL |
| namespace | String | Jans | Cedar namespace being used by the sidecar |
