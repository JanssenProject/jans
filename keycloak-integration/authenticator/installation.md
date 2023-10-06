## Keycloak Installation configuration for use with Janssen Auth

### 1- Brief 

   This guide contains instructions on how to install keycloak for use with keycloak 
and run it in a production setting alongside Janssen.


### 2- Keycloak and Plugins Installation

  We will be using the quarkus distribution of keycloak which can be found 
[here](https://github.com/keycloak/keycloak/releases/download/22.0.3/keycloak-22.0.3.zip).
directory.
After downloading the binaries , it's suggested to unzip it in the `/opt/keycloak` directory.

#### 2.1 - Keycloak Authentication Plugin Installation

Installing the authentication plugin is straightforward.
It resides at the url
https://jenkins.jans.io/maven/io/jans/jans-authenticator/<version>/
Binaries of interest  have to be copied to the
`/opt/keycloak/providers/` directory. They are:
- `jans-authenticator-<version>.jar`
- `jans-authenticator-<version>-deps.zip`. It's contents have to 
be unzipped into the directory. These are the plugin's dependencies.

No further action is needed after copying these files.


### 3 - Running Keycloak

  The following assumptions will be made
- Keycloak has been installed under the directory `/opt/keycloak/`
- The Janssen Server's hostname is `janssen-with-kc.local`
- Keycloak will run behind a reverse proxy/ load balancer (e.g. apache )
  and will be listening only on the local interface on port 8092

From the terminal, run the following command 
```
/opt/keycloak/bin/kc.sh --log "console,file" --http-host=127.0.0.1 --http-port=8092 \
--hostname-url=https://janssen-with-kc.local --spi-connections-http-client-default-disable-trust-manager=true \
--proxy edge
```

#### 3.1 - Database Setup
  By default , in a non-production environment , keycloak relies on the embedded H2 database for operation.
In a production setting, a more appropriate database needs to be deployed. 
You can find a list of supported databases [here](https://www.keycloak.org/server/db).
Additional database configuration will need to be done.



#### 3.2 - Reverse Proxy 
As keycloak will run behind a proxy, there are a couple paths that need to be exposed (or not), with the full list
found [here](https://www.keycloak.org/server/reverseproxy).


### 5 - Configuration changes in Keycloak and Janssen-Auth
TBD

### 6 - Clustering 
TBD