---
tags:
  - administration
  - helm
  - kubernetes
  - benchmarking
---

# Overview

The Janssen Server has been optimized with several container strategies that allow scaling microservices and orchestrating them using Kubernetes. In this tutorial we will be running a load test from three different regions on a janssen setup on three different regions. For simplicity, we will be using [microk8s](https://microk8s.io) however we do recommend users to use the kubernetes cluster providers that they will be using in production. For instance, we run our loadtests across EKS, GKE, AKS and DOKS.

With this procedure the [following](#results) with a `10` million user database is expected:

#### Results

!!!note
    The authorization code flow  hits a total of 4 steps, 3 authorization steps `/token`, `/authorize`, `/jans-auth/login` and 1 redirect.
    
| Flow                    | Authentications per second |
|-------------------------|----------------------------|
| Authorization code flow | 800-1000                   |


## Installation

As mentioned in the overview we recommend using the same Kubernetes cluster as planned in production. More guides to install on different clouds can be found [here](../install/helm-install/README.md).

### Persistence

We recommend your persistence in production to be HA, backup supported and point in time recovery supported. Below is a table of the persistence used and resources set for this test.

| Persistence      | # of nodes | RAM(GiB) | CPU | Total RAM (GiB) | Total CPU |
|------------------|------------|----------|-----|-----------------|-----------|
| MySQL            | 1          | 52       | 8   | 52              | 8         |

### Set up the cluster

#### Kubernetes Cluster Load Test Resources

!!!note
    Instance type can be selected to best suit the deployment intended. Keep in mind when selecting the instance type to strive for a `10` or up to `10` network bandwidth (Gbps). Below details the exact resources needed for this tutorial. This is in addition to the persistence resources listed above.

Resourcing is  critical as timeouts in connections can occur, resulting in failed authentications or cutoffs.

| Regions     | # of nodes | RAM(GiB) | CPU | Total RAM (GiB) | Total CPU |
|-------------|------------|----------|-----|-----------------|-----------|
| US-West     | 1          | 96       | 48  | 96              | 48        |
| US-East     | 1          | 96       | 48  | 96              | 48        |
| EU-Central  | 1          | 96       | 48  | 96              | 48        |
| Grand Total |            |          |     | 288 GB          | 144       |


A Kubernetes cluster can be created with three nodes or more in one region and that's fine as long as the nodes are in multiple zones. We will continue with the above table and using [microk8s](https://microk8s.io).
   

1. Create three ubuntu 22.04 nodes and run on each one the following:

    ```bash
    sudo snap install microk8s --classic
    sudo snap alias microk8s.kubectl kubectl
    ```
2. Designate one of the nodes as the master. We will choose the `us-west` node to be our master. On the master node run:

   1. Execute : 

      ```bash
      microk8s.enable ingress dashboard observability dns metrics-server hostpath-storage registry
      ```

   2. All the other microk8s nodes must be resolvable from within the master. If fqdns are not globally resolved (registered) open the `/etc/hosts` file in the master node and map each hostname of the other nodes. YThe hostname of the other nodes can be obtained by executing the command `hostname`.

      ```bash
      # If the hostnames are not globally resolvable on master
      echo "192.123.123.123 ubuntu-us-east" >> /etc/hosts
      echo "192.124.124.124 ubuntu-eu-central" >> /etc/hosts
      ```
   3. Execute:  

      ```bash
      microk8s add-node
      ```
   
   Copy the output of the command above with `--worker` i.e. `microk8s join 192.12.12.12:25000/88687d1cc5ecdee0db5014c4df9b82cb/adedf6a730eb --worker` and execute it in the other nodes (worker nodes) to join them. Step `iii` ( this step) needs to be repeated for each worker node.

3. Make sure [helm](https://helm.sh/docs/intro/install/) is installed.

4. Prepare your [override.yaml](../install/helm-install/README.md). Copy the below into a file named override.yaml. At the time of writing this we are using image tags `replace-janssen-version_dev` which are the bleeding edge images for release `replace-janssen-version`. Stable images such as `replace-janssen-version-1` should be used.
   
   ```yaml
   config:
      image:
        repository: ghcr.io/janssenproject/jans/configurator
        tag: replace-janssen-version_dev 
      countryCode: US
      email: support@gluu.org
      orgName: Gluu
      city: Austin
      configmap:
        cnSqlDbName: test
        cnSqlDbPort: 3306
        cnSqlDbDialect: mysql
        cnSqlDbHost: mycool.cloud.mysql
        cnSqlDbUser: root
        cnSqlDbTimezone: UTC
        cnSqldbUserPassword: Test1234#
   global:
     auth-server:
       enabled: true
     config-api:
       enabled: true
     cnPersistenceType: sql
     cloud:
       testEnviroment: false
     fqdn: example.gluu.info
     isFqdnRegistered: true
     # In the event the fqdn above is not resolvable from the internet comment the above line and uncomment the below two setting your lbIp to your master ndoe ip.
     #isFqdnRegistered: false
     #lbIp: 192.12.12.12
     istio:
       enabled: false
       ingress: false
     nginx-ingress:
       enabled: true
     fido2:
       enabled: false
       ingress:
         fido2ConfigEnabled: false
     scim:
       enabled: false
       ingress:
         scimConfigEnabled: false
         scimEnabled: false
   auth-server:
     image:
       pullPolicy: IfNotPresent
       repository: ghcr.io/janssenproject/jans/auth-server
       tag: replace-janssen-version_dev
   config-api:
     image:
       pullPolicy: IfNotPresent
       repository: ghcr.io/janssenproject/jans/config-api
       tag: replace-janssen-version_dev
   persistence:
     image:
       pullPolicy: IfNotPresent
       repository: ghcr.io/janssenproject/jans/persistence-loader
       tag: replace-janssen-version_dev 
   nginx-ingress:
     ingress:
       path: /
       hosts:
         - example.gluu.info
       tls:
         - secretName: tls-certificate
           hosts:
             - example.gluu.info
   ```

5. Run the following:
   ```bash
    kubectl create ns jans
    helm repo add janssen https://docs.jans.io/charts
    helm repo update
    helm install janssen janssen/janssen -n jans -f override.yaml
   ```

## Load-test

Our tests used 10 million users that were loaded. We have created a docker image to load users. That same image is also used to load test Janssen using jmeter tests for the `Authorization code` flow. More tests will come!. This image will load users and use a unique password for each user.

### Loading users

Loading users requires a hefty but temporary amount of resources. By default, the resources ask for `10` vCPU and `5` Gis. However, to speed up the process increase the number of CPUs as the job in step two below uses parallel tasks. If left as is 10 million users would load in around 17 hours or so.

1. Create a folder called `add_users`.

    ```bash
    mkdir -p add_users && cd add_users
    ```

2. Copy the following [yaml](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/demos/benchmarking/docker-jans-loadtesting-jmeter/yaml/load-users/load_users_rdbms_job.yaml) into the folder under the name `load_users.yaml`.

3.  Open the file and modify the required parameters. Note that the following environments can be used as configmaps data to configure the pod.

    | ENV                              | Description                                                                                                   | Default                |
    |----------------------------------|---------------------------------------------------------------------------------------------------------------|------------------------|
    | `TEST_USERS_PREFIX_STRING`       | The user prefix string attached to the test users loaded                                                      | `test_user`            |
    | `COUCHBASE_URL`                  | Couchbase URL if Couchbase is the persistence to load users in.                                               | ``                     |
    | `COUCHBASE_PW`                   | Couchbase PW if Couchbase is the persistence to load users in.                                                | ``                     |
    | `USER_NUMBER_STARTING_POINT`     | The user number to start from . This is appended to the username i.e test_user0                               | `0`                    |
    | `USER_NUMBER_ENDING_POINT`       | The user number to end at.                                                                                    | `50000000`             |
    | `LOAD_USERS_TO_COUCHBASE`        | Enable loading users to Couchbase persistence. `true` or `false` == ``                                        | `false`                |
    | `LOAD_USERS_TO_LDAP`             | Enable loading users to LDAP persistence. `true` or `false` == ``                                             | `false`                |
    | `LOAD_USERS_TO_SPANNER`          | Enable loading users to Spanner persistence. `true` or `false` == ``                                          | `false`                |
    | `LOAD_USERS_TO_RDBMS`            | Enable loading users to RDBMS persistence. `true` or `false` == ``                                            | `false`                |
    | `USER_SPLIT_PARALLEL_THREADS`    | The number of parallel threads to break the total number users across. This number heavily effects CPU usage. | `20`                   |
    | `GOOGLE_APPLICATION_CREDENTIALS` | Google Credentials JSON SA file. **Used with Spanner**                                                        | ``                     |
    | `GOOGLE_PROJECT_ID`              | Google Project ID. **Used with Spanner**                                                                      | ``                     |
    | `GOOGLE_SPANNER_INSTANCE_ID`     | Google Spanner Instance ID. **Used with Spanner**                                                             | ``                     |
    | `GOOGLE_SPANNER_DATABASE_ID`     | Google Spanner Database ID. **Used with Spanner**                                                             | ``                     |
    | `LDAP_URL`                       | LDAP URL if LDAP is the persistence to load users in.                                                         | `opendj:1636`          |
    | `LDAP_PW`                        | LDAP PW  if LDAP is the persistence to load users in.                                                         | ``                     |
    | `LDAP_DN`                        | LDAP DN if LDAP is the persistence to load users in.                                                          | `cn=directory manager` |
    | `RDBMS_TYPE`                     | RDBMS type if `mysql` or `pgsql` is the persistence to load users in.                                         | `mysql`                |
    | `RDBMS_DB`                       | RDBMS Database name if `mysql` or `pgsql` is the persistence to load users in.                                | `jans`                 |
    | `RDBMS_USER`                     | RDBMS user if `mysql` or `pgsql` is the persistence to load users in.                                         | `jans`                 |
    | `RDBMS_PASSWORD`                 | RDBMS user password if `mysql` or `pgsql` is the persistence to load users in. .                              | ``                     |
    | `RDBMS_HOST`                     | RDBMS host if `mysql` or `pgsql` is the persistence to load users in.                                         | `localhost`            |

    __Tips:__ To speed the loading process, increase the CPU requests and limits of the pod.

4. Create a namespace for load-testing.

    ```bash
    kubectl create ns load
    ```
   
5. Create `load_users.yaml`

    ```bash
    kubectl create -f load_users.yaml -n load
    cd ..
    ```

Wait until all the users are up before moving forward. Tail the logs by running `kubectl logs deployment/load-users -n load`.

### Load testing

#### Authorization code flow

##### Resources needed for Authorization code client jmeter test

 The below resources were [calculated](#kubernetes-cluster-load-test-resources) when creating the nodes above.

| NAME                                | # of pods | RAM(GiB) | CPU | Total RAM(GiB) | Total CPU |
|-------------------------------------|-----------|----------|-----|----------------|-----------|
| Authorization code flow jmeter test | 20        | 8        | 1.3 | 190            | 24        |
| Grand Total                         |           |          |     | 190 GiB        | 24        |

##### Setup Client

Create the client needed to run the test by executing the following. Make sure to change the `FQDN`  :

1. Create a folder called `load_test`.

    ```bash
    mkdir -p load_test && cd load_test
    ```

2. Create the client json file

    ```bash
    FQDN=example.gluu.info
    cat << EOF > auth_code_client.json
    {
        "dn": null,
        "inum": null,
        "displayName": "Auth Code Flow Load Test Client",
        "redirectUris": [
          "https://$FQDN"
        ],
        "responseTypes": [
          "id_token",
          "code"
        ],
        "grantTypes": [
          "authorization_code",
          "implicit",
          "refresh_token"
        ],
        "tokenEndpointAuthMethod": "client_secret_basic",
        "scopes": [
          "openid",
          "profile",
          "email",
          "user_name"
        ],
        "trustedClient": true,
        "includeClaimsInIdToken": false,
        "accessTokenAsJwt": false,
        "disabled": false,
        "deletable": false,
        "description": "Auth Code Flow Load Testing Client"
    }
    EOF
    ```
3. Copy the following [yaml](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/demos/benchmarking/docker-jans-loadtesting-jmeter/yaml/load-test/load_test_auth_code.yaml) into the folder.

4. Download or build [config-cli-tui](../config-guide/jans-tui/README.md) and run:

    ```bash
    # Notice the namespace is jans here . Change it if it was changed during installation of janssen previously
    TUI_CLIENT_ID=$(kubectl get cm cn -o json -n jans | grep '"tui_client_id":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]')
    TUI_CLIENT_SECRET=$(kubectl get secret cn -o json -n jans | grep '"tui_client_pw":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d)
    # add -noverify if your fqdn is not registered
    ./config-cli-tui.pyz --host $FQDN --client-id $TUI_CLIENT_ID --client-secret $TUI_CLIENT_SECRET --no-tui --operation-id=post-oauth-openid-client --data=auth_code_client.json
    ```

5. Save the client id and secret from the response and enter them along with your FQDN in the yaml file `load_test_auth_code.yaml`  under `AUTHZ_CLIENT_ID`, `AUTHZ_CLIENT_SECRET` and `FQDN` respectively then execute :

    ```bash
    kubectl apply -f load_test_auth_code.yaml
    ```

6. The janssen setup by default installs an HPA which will automatically scale your pods if the metrics server is installed according to traffic. To load it very quickly scale the auth-server manually:
    ```bash
    kubectl scale deploy janssen-auth-server -n jans --replicas=40
   ```
   
7. Finally, scale the load test. The replica number here should be manually controlled.
    ```bash
    kubectl scale deploy load-testing-authz -n load --replicas=20
   ```

#### Resource Owner Password Credentials (ROPC) flow

##### Resources needed for ROPC client jmeter test

 The below resources were [calculated](#kubernetes-cluster-load-test-resources) when creating the nodes above.

| NAME                  | # of pods | RAM(GiB) | CPU | Total RAM(GiB) | Total CPU |
|-----------------------|-----------|----------|-----|----------------|-----------|
| ROPC flow jmeter test | 20        | 8        | 1.3 | 190            | 24        |
| Grand Total           |           |          |     | 190 GiB        | 24        |

##### Setup Client

Create the client needed to run the test by executing the following. Make sure to change the `FQDN`  :

1. Create a folder called `load_test`.

    ```bash
    mkdir -p load_test && cd load_test
    ```

2. Create the client json file

    ```bash
    FQDN=example.gluu.info
    cat << EOF > ropc_client.json
    {
        "dn": null,
        "inum": null,
        "displayName": "ROPC Flow Load Test Client",
        "redirectUris": [
          "https://$FQDN"
        ],
        "responseTypes": [
          "id_token",
          "code"
        ],
        "grantTypes": [
          "authorization_code",
          "implicit",
          "refresh_token",
          "password"
        ],
        "tokenEndpointAuthMethod": "client_secret_basic",
        "scopes": [
          "openid",
          "profile",
          "email",
          "user_name"
        ],
        "trustedClient": true,
        "includeClaimsInIdToken": false,
        "accessTokenAsJwt": false,
        "disabled": false,
        "deletable": false,
        "description": "ROPC Flow Load Testing Client"
    }
    EOF
    ```

3. Copy the following [yaml](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/demos/benchmarking/docker-jans-loadtesting-jmeter/yaml/load-test/load_test_ropc.yaml) into the folder.

4. Download or build [config-cli-tui](../config-guide/jans-tui/README.md) and run:

    ```bash
    # Notice the namespace is jans here . Change it if it was changed during installation of janssen previously
    TUI_CLIENT_ID=$(kubectl get cm cn -o json -n jans | grep '"tui_client_id":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]')
    TUI_CLIENT_SECRET=$(kubectl get secret cn -o json -n jans | grep '"tui_client_pw":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d)
    # add -noverify if your fqdn is not registered
    ./config-cli-tui.pyz --host $FQDN --client-id $TUI_CLIENT_ID --client-secret $TUI_CLIENT_SECRET --no-tui --operation-id=post-oauth-openid-client --data=ropc_client.json
    ```

5. Save the client id and secret from the response and enter them along with your FQDN in the yaml file `load_test_ropc.yaml`  under `ROPC_CLIENT_ID`, `ROPC_CLIENT_SECRET` and `FQDN` respectively then execute :

    ```bash
    kubectl apply -f load_test_ropc.yaml
    ```

6. The janssen setup by default installs an HPA which will automatically scale your pods if the metrics server is installed according to traffic. To load it very quickly scale the auth-server manually:
    ```bash
    kubectl scale deploy janssen-auth-server -n jans --replicas=40
   ```
   
7. Finally, scale the load test. The replica number here should be manually controlled.
    ```bash
    kubectl scale deploy load-testing-ropc -n load --replicas=20
   ```

#### DCR flow

##### Resources needed for DCR client jmeter test

 The below resources were [calculated](#kubernetes-cluster-load-test-resources) when creating the nodes above.

| NAME        | # of pods | RAM(GiB) | CPU | Total RAM(GiB) | Total CPU |
|-------------|-----------|----------|-----|----------------|-----------|
| DCR test    | 20        | 8        | 1.3 | 190            | 24        |
| Grand Total |           |          |     | 190 GiB        | 24        |

##### Setup Client

Create the client needed to run the test by executing the following. Make sure to change the `FQDN`  :

1. Create a folder called `load_test`.

    ```bash
    mkdir -p load_test && cd load_test
    ```

2. Create the client json file

    ```bash
    FQDN=example.gluu.info
    cat << EOF > dcr_client.json
    {
        "dn": null,
        "inum": null,
        "displayName": "DCR Flow Load Test Client",
        "redirectUris": [
          "https://$FQDN"
        ],
        "responseTypes": [
          "id_token",
          "code"
        ],
        "grantTypes": [
          "authorization_code",
          "implicit",
          "refresh_token",
          "password"
        ],
        "tokenEndpointAuthMethod": "client_secret_basic",
        "scopes": [
          "openid",
          "profile",
          "email",
          "user_name"
        ],
        "trustedClient": true,
        "includeClaimsInIdToken": false,
        "accessTokenAsJwt": false,
        "disabled": false,
        "deletable": false,
        "description": "DCR Flow Load Testing Client"
    }
    EOF
    ```

3. Copy the following [yaml](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/demos/benchmarking/docker-jans-loadtesting-jmeter/yaml/load-test/load_test_dcr.yaml) into the folder.


4. Download or build [config-cli-tui](../config-guide/jans-tui/README.md) and run:

    ```bash
    # Notice the namespace is jans here . Change it if it was changed during installation of janssen previously
    TUI_CLIENT_ID=$(kubectl get cm cn -o json -n jans | grep '"tui_client_id":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]')
    TUI_CLIENT_SECRET=$(kubectl get secret cn -o json -n jans | grep '"tui_client_pw":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d)
    # add -noverify if your fqdn is not registered
    ./config-cli-tui.pyz --host $FQDN --client-id $TUI_CLIENT_ID --client-secret $TUI_CLIENT_SECRET --no-tui --operation-id=post-oauth-openid-client --data=dcr_client.json
    ```

5. You will need to load the sectorIdentifier into your persistence.For MySQL that statement would be the following taking into account the `FQDN`:

    ```sql
    INSERT INTO jansSectorIdentifier (doc_id, dn, jansId, jansRedirectURI, objectClass)
    VALUES (
      'a55ede29-8f5a-461d-b06e-76caee8d40b5',
      'jansId=a55ede29-8f5a-461d-b06e-76caee8d40b5,ou=sector_identifiers,o=jans',
      'a55ede29-8f5a-461d-b06e-76caee8d40b5',
      '{"v": ["https://www.jans.org", "http://localhost:80/jans-auth-rp/home.htm", "https://localhost:8443/jans-auth-rp/home.htm", "https://$FQDN/jans-auth-rp/home.htm", "https://$FQDN/jans-auth-client/test/resources/jwks.json", "https://client.example.org/callback", "https://client.example.org/callback2", "https://client.other_company.example.net/callback", "https://client.example.com/cb", "https://client.example.com/cb1", "https://client.example.com/cb2"]}',
      'jansSectorIdentifier'
    );
    ```

6. Save the client id and secret from the response and enter them along with your FQDN in the yaml file `load_test_ropc.yaml`  under `DCR_CLIENT_ID`, `DCR_CLIENT_SECRET` and `FQDN` respectively then execute :

    ```bash
    kubectl apply -f load_test_dcr.yaml
    ```

7. The janssen setup by default installs an HPA which will automatically scale your pods if the metrics server is installed according to traffic. To load it very quickly scale the auth-server manually:
    ```bash
    kubectl scale deploy janssen-auth-server -n jans --replicas=40
   ```
   
8. Finally, scale the load test. The replica number here should be manually controlled.
    ```bash
    kubectl scale deploy load-testing-ropc -n load --replicas=20
   ```
