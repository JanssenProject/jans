# Cedarling KrakenD Plugin

This is a [KrakenD HTTP server plugin](https://www.krakend.io/docs/extending/http-server-plugins/) that intercepts a call to a selected endpoint, calls the [sidecar](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/flask-sidecar/README.md) and allows access only if the sidecar responds with `true`.

## Functionality

When an end user calls the KrakenD endpoint protected by the plugin, the following steps happen:

1. The plugin intercepts the call, and creates an [AuthZen](https://openid.github.io/authzen/) access evaluation request.
2. The plugin sends the request to the sidecar, which is running on the same host as KrakenD
3. The sidecar calls cedarling's `authz()` interface to check if this call is allowed or not according to the policy store loaded
4. The sidecar responds back to the plugin. If the decision was `true`, the plugin allows the request to pass through to the KrakenD backend. Otherwise, it responds with 403 Forbidden.

[Sequence Diagram](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGFICYEMBO4QDsDm0DSKkBrSDAEWgAdwBXLTAKACMB7YYZgWwtVAGMRuGYAGdo9etxR8BSIXHQlgEniH6DgeAsTLKpqmXIDKIBJF6pd09XESp02cbAVCAtAD58REqQBc0AOIAogAq0AD0FCisZsCIADoY0C4AEtAARACC1MAAFswoIABeSKDMGH4AQpCokCjQADwAUgDqwW5p9J7apO7GpuYofgDeacLUjABWMWk+I8AAnhSQM2ktwWkANAlpJiu8tmiYWJvbkcxLepDCMyNIvPvCwgD67NorTa3tAL5fWxhpKCuzGoKH2NzSCyWK0asmEPh8yWCwQACk8AEqQACO1CuwBO-12PlGnEgT12GzSZwuoCu4Jy1VMKBuv22IPAdOYwjxRIAjAAmADsADoAAyioU8zaUko5FYRKKxXixBD4ykK5i8Zjsok5NgUNI-Fn-O6lf6zNIYJAcZZEmEYOE+DJKkBleFxNJBYLug1feh9MyodzwZCHbB+JDZHKFAAUdwez1eJAAvB82n9AcJgaDIEmhXm-iaXRhc3mAJT0JDgDTAFA4+jBuxHXomAODaA1uv+gbuLreYZpfogYRFmbt2uQX29shB5zAftnRXKskYABmzFHO1X699kHAwhgK8r+-rB3sWGb-VQfkPe8gfpb3Y8Wj70BGg+HZVHN-3k+f07cTggIofgACwigAzNAABi+SMCYpgYPQJAIEAA)

## Downloading

The cedarling-krakend plugin builds are available via [Janssen](https://github.com/JanssenProject/jans/releases) releases. Please note that builds are architecture, platform, and KrakenD version specific. This means that builds compiled against KrakenD version `2.9.0` will not work on other versions. The build tags are formatted as follows:

```
cedarling-krakend-<architecture>-<krakend version and platform>-<Janssen version>.so 
```

Use the following table to find which build you need:

|   | amd64 | arm64 |
| - | ----- | ----- |
| Docker | `amd64-builder-2.9.0-0.0.0.so` | `arm64-builder-2.9.0-0.0.0.so` |
| On-premise | `amd64-builder-2.9.0-linux-generic-0.0.0.so` | `arm64-builder-2.9.0-linux-generic-0.0.0.so` |

If you are running a different version of KrakenD, you can use the following steps to build the plugin yourself.

## Building

Krakend recommends building via their builder docker image, to produce builds that match the target architecture and Go version. To build:

- Clone the Janssen repository
    ```
    git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans
    cd jans
    git sparse-checkout init --cone
    git checkout main
    git sparse-checkout set jans-cedarling 
    cd cedarling-krakend
    ```
- Build the plugin, replacing `<x.y.z>` with the KrakenD version you want to build against: 
    
    - For Docker targets:

    ```
    docker run -it -v "$PWD:/app" -w /app krakend/builder:<x.y.z> go build -buildmode=plugin -o cedarling-krakend.so .
    ```

    - For on-premise installations:

    ```
    docker run -it -v "$PWD:/app" -w /app krakend/builder:<x.y.z>-linux-generic go build -buildmode=plugin -o yourplugin.so .
    ```

    - For ARM64 Docker targets:

    ```bash
    docker run -it -v "$PWD:/app" -w /app \
        -e "CGO_ENABLED=1" \
        -e "CC=aarch64-linux-musl-gcc" \
        -e "GOARCH=arm64" \
        -e "GOHOSTARCH=amd64" \
        krakend/builder:<x.y.z> \
        go build -ldflags='-extldflags=-fuse-ld=bfd -extld=aarch64-linux-musl-gcc' \
        -buildmode=plugin -o yourplugin.so .
    ```

    - For ARM64 on-premise installs:

    ```bash
    docker run -it -v "$PWD:/app" -w /app \
        -e "CGO_ENABLED=1" \
        -e "CC=aarch64-linux-gnu-gcc" \
        -e "GOARCH=arm64" \
        -e "GOHOSTARCH=amd64" \
        krakend/builder:<x.y.z>-linux-generic \
        go build -ldflags='-extldflags=-fuse-ld=bfd -extld=aarch64-linux-gnu-gcc' \
        -buildmode=plugin -o yourplugin.so .
    ```

Check [KrakenD documentation](https://www.krakend.io/docs/extending/injecting-plugins/) on how to load plugins.

## Prerequisites for testing

To test the plugin, you will need:

- A cedarling policy store with a policy for our gateway. To create this, please follow [these](https://github.com/JanssenProject/jans/wiki/Cedarling-Hello-World-%5BWIP%5D#1-author-policies) steps.
- An instance of the cedarling sidecar, using the policy store mentioned above. Please follow [these](https://github.com/JanssenProject/jans/wiki/Cedarling-Hello-World-%5BWIP%5D#2-deploy-cedarling-sidecar) steps. 
- For our demo, we will use this sample policy as outlined in the instructions:
    ```
    @id("allow_one")
    permit(
      principal is gatewayDemo::Workload,
      action == gatewayDemo::Action::"GET",
      resource is gatewayDemo::HTTP_Request
    )
    when {
        ((principal["access_token"])["scope"]).contains("profile")
    };
    ```
- This policy will allow access so long as the access token contains the `profile` scope.
- A [KrakenD server installation](https://www.krakend.io/docs/overview/installing/). For development purposes, the binary install is recommended. For production setups, the Docker method is recommended.
- The plugin `.so` file for your architecture. For Mac OS hosts, ARM64 is required.
- A configuration file. Sample configuration is provided in [krakend.json](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/cedarling-krakend/krakend.json).

## Configuration

See [krakend.json](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/cedarling-krakend/krakend.json) to see an example KrakenD configuration which loads the plugin. The following table describes the plugin-specific configuration keys. These keys are mandatory and must be provided. Additional configuration is described in [KrakenD documentation](https://www.krakend.io/docs/configuration/structure/).

The `namespace` field in the configuration needs to be the cedar namespace you used when creating the policy store. By default, Agama Lab sets the namespace to the name of the policy store. If you are following the demo, this value is `gatewayDemo`.

| Field | Type | Example | Description |
|-------|------|---------|-------------|
| path   | String  | /protected | KrakenD endpoint to protect |
| sidecar_endpoint | String | http://127.0.0.1:5000/cedarling/evaluation | Sidecar evaluation URL |
| namespace | String | gatewayDemo | Cedar namespace being used by the sidecar |

## Running

1. Start the cedarling sidecar. The sample config expects the sidecar to be running on port 5000
2. Place `krakend.json` and the plugin `.so` file in your current working directory
2. Run the KrakenD server: `krakend run -c krakend.json`
3. KrakenD is running on `http://127.0.0.1:8080`
4. Test with no authentication: `curl http://127.0.0.1:8080/protected`. You should get a 403 Forbidden
5. Test with authentication:

```bash
ACCESS_TOKEN=eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g

curl http://127.0.0.1:8080/protected -H "Authorization: Bearer $ACCESS_TOKEN"
```

KrakenD is configured to respond with the health check response if authentication succeeds. 
