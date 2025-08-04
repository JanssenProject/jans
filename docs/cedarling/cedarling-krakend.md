---
tags:
  - Cedar
  - Cedarling
  - KrakenD
---

# Cedarling KrakenD Plugin

This is a [KrakenD HTTP server plugin](https://www.krakend.io/docs/extending/http-server-plugins/) that intercepts a call to a selected endpoint, uses the embedded [Cedarling](https://docs.jans.io/head/cedarling/cedarling-getting-started/) PDP and allows access only if the authorization result is `true`.

## Functionality

1. During startup, the KrakenD plugin reads the environment variables and initializes a Cedarling instance
2. When the protected endpoint is called, the plugin intercepts the call and creates a Cedarling authorization request.
3. If the result of the authorization request is `true`, the plugin allows the endpoint to respond. Otherwise, the plugin responds with `403 Forbidden`

[Sequence Diagram](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGFICYEMBO4QDsDm0DSKkBrSDAEWgAdwBXLTAKACMB7YYZgWwtVAGMRuGYAGdo9etxR8BSIXHQlgEniH6DgeAsTLKpqmXPjI0mLOPxESpALQA+C9tIAuaJjAgk6AF4wjqdNjQAGYonAA6GCQAbiChGByK0FGoHoxQwvSwCkJ2DlYuAOIAogAq0AD0FKHAkLw1CBHQ1gAS0ABEAILUwAAWzCggXkigzBguAEKQqJAo0AA8AFIA6iW2bfR5ZHZ+JtguAN5twtSMAFa1wG1Oh+zawleHSLy8kMLCAPq3JFdtiytrAF8AQAaCJtFCvZjUFAvB5tYAATwokB+C1kwicTmaJRKAAV3gAlSAAR2or0uwLaIAQP2EnEg72pbUpVWYyL0rzaEWubR6UwQMweILa0PAcL6wkuTjaAEYAEwAdgAdAAGVVKmXMtrcXo-SrVC6IZlg1nsXjMMXSnpsChtIEgsFPEYYOEYJAJVHozEdOogUaYsJtYolQN2gH0TwaYAoMmZRD+Uy5LT5aDR2ObGy2LIgRQHbUGuqIRkYILMK7tTClu30SDgYQwIKeetx4wBLBJyxkFyNuuQDbJrZZ7LAFwAFhVAGZoAAxfqMakCjA1jAIIA)

## Downloading

The cedarling-krakend plugin builds are available via [Janssen releases](https://github.com/JanssenProject/jans/releases/latest). Please note that builds are architecture, platform, and KrakenD version specific. This means that builds compiled against KrakenD version `2.9.0` for Windows will not work on other versions or operating systems. The build tags are formatted as follows:

```
cedarling-krakend-<architecture>-<krakend version and platform>-<Janssen version>.so
```

Use the following table to find which build you need:

|            | amd64                                        | arm64                                        |
| ---------- | -------------------------------------------- | -------------------------------------------- |
| Docker     | `amd64-builder-2.9.0-0.0.0.so`               | `arm64-builder-2.9.0-0.0.0.so`               |
| On-premise | `amd64-builder-2.9.0-linux-generic-0.0.0.so` | `arm64-builder-2.9.0-linux-generic-0.0.0.so` |

If you are running a different version of KrakenD, you can use the following steps to build the plugin yourself.

## Building

- Clone the `cedarling-krakend` folder of the Janssen Repository:
  ```
  git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans
  cd jans
  git sparse-checkout init --cone
  git checkout main
  git sparse-checkout set jans-cedarling
  cd cedarling-krakend
  ```
- Download the dynamic shared object file(s) for the `cedarling_go` binding, compiled for your platform:
  - Windows:
    - [cedarling_go.dll](https://github.com/JanssenProject/jans/releases/download/nightly/cedarling_go-0.0.0.dll)
    - [cedarling_go.lib](https://github.com/JanssenProject/jans/releases/download/nightly/cedarling_go-0.0.0.lib)
  - Mac OS: [cedarling_go.dylib](https://github.com/JanssenProject/jans/releases/download/nightly/libcedarling_go-0.0.0.dylib)
  - Linux: [libcedarling_go.so](https://github.com/JanssenProject/jans/releases/download/nightly/libcedarling_go-0.0.0.so)
- Build the plugin, replacing `<x.y.z>` with the KrakenD version you want to build against:

  - For Docker targets:

  ```
  docker run -it -v "$PWD:/app" -w /app krakend/builder:<x.y.z> go build -buildmode=plugin -o cedarling-krakend.so .
  ```

  - For on-premise installations:

  ```
  docker run -it -v "$PWD:/app" -w /app krakend/builder:<x.y.z>-linux-generic go build -buildmode=plugin -o cedarling-krakend.so .
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
      -buildmode=plugin -o cedarling-krakend.so .
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
      -buildmode=plugin -o cedarling-krakend.so .
  ```

## Prerequisites for testing

To test the plugin, you will need:

- A cedarling policy store with a policy for our gateway. To create this, please follow [these](https://github.com/JanssenProject/jans/wiki/Cedarling-Hello-World#1-author-policies) steps.
- For our demo, we will use this sample policy as outlined in the instructions:
  ```
  @id("allow_one")
  permit(
    principal is gatewayDemo::Workload,
    action == gatewayDemo::Action::"GET",
    resource is gatewayDemo::HTTP_Request
  )
  when {
      principal has access_token.scope &&
      principal.access_token.scope.contains("profile")
  };
  ```
- This policy will allow access so long as the access token contains the `profile` scope.
- A [KrakenD server installation](https://www.krakend.io/docs/overview/installing/). For development purposes, the binary install is recommended. For production setups, the Docker method is recommended.
- The plugin `.so` file for your architecture. For Mac OS hosts, ARM64 is required.
- The dynamic shared object file(s) for the `cedarling_go` binding, compiled for your platform:
  - Windows:
    - [cedarling_go.dll](https://github.com/JanssenProject/jans/releases/download/nightly/cedarling_go-0.0.0.dll)
    - [cedarling_go.lib](https://github.com/JanssenProject/jans/releases/download/nightly/cedarling_go-0.0.0.lib)
  - Mac OS: [cedarling_go.dylib](https://github.com/JanssenProject/jans/releases/download/nightly/libcedarling_go-0.0.0.dylib)
  - Linux: [libcedarling_go.so](https://github.com/JanssenProject/jans/releases/download/nightly/libcedarling_go-0.0.0.so)
- A configuration file. Sample configuration is provided in [krakend.json](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/cedarling-krakend/krakend.json).

## Configuration

Check the sample configuration in `krakend.json` to see an example KrakenD configuration which loads the plugin. The following table describes the plugin-specific configuration keys. These keys are mandatory and must be provided. Additional configuration is described in [KrakenD documentation](https://www.krakend.io/docs/configuration/structure/).

The `namespace` field in the configuration needs to be the cedar namespace you used when creating the policy store. By default, Agama Lab sets the namespace to `Jans`. If you are following the demo, you will use the same name.

| Field     | Type   | Example    | Description                                    |
| --------- | ------ | ---------- | ---------------------------------------------- |
| path      | String | /protected | KrakenD endpoint to protect                    |
| namespace | String | Jans       | Cedar namespace being used by the policy store |

## Running

**Warning**: Windows and Mac OS are untested. Only Linux has been fully tested against KrakenD.

1. Place `krakend.json` and the dynamic shared object file(s) in your current working directory
2. Create a folder named `plugin` in the current working directory and place the plugin `.so` file in that folder
3. To properly load the shared library, Linux and Mac OS platforms need to be told where to find your shared object file. On Windows this is detected automatically so long as the object file is placed in the current working directory.

   - Mac OS: `export DYLD_LIBRARY_PATH=$(pwd):$DYLD_LIBRARY_PATH`
   - Linux: `export LD_LIBRARY_PATH=$(pwd):$LD_LIBRARY_PATH`

     On Linux, you can verify the dynamic linking like so:

     ```
     $ ldd plugin/*.so
     ...
     libcedarling_go.so => /path/to/current/directory/libcedarling_go.so
     ```

4. Set the Cedarling [bootstrap](https://docs.jans.io/head/cedarling/cedarling-properties/) variables in your environment.

   - For gateway functionality, at minimum you will need the following properties set:

   ```
   CEDARLING_APPLICATION_NAME=Gateway
   CEDARLING_POLICY_STORE_URI=<Your policy store URI>
   CEDARLING_WORKLOAD_AUTHZ=enabled
   CEDARLING_PRINCIPAL_BOOLEAN_OPERATION={\"===\":[{\"var\":\"Jans::Workload\"},\"ALLOW\"]}
   CEDARLING_ID_TOKEN_TRUST_MODE=never
   ```

5. Run the KrakenD server: `krakend run -c krakend.json`
6. KrakenD is running on `http://127.0.0.1:8080`
7. Test with no authentication: `curl http://127.0.0.1:8080/protected`. You should get a 403 Forbidden
8. Test with authentication (a sample token is provided):

```bash
export ACCESS_TOKEN=eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g

curl http://127.0.0.1:8080/protected -H "Authorization: Bearer $ACCESS_TOKEN"
```

KrakenD is configured to respond with the health check response if authentication succeeds.
