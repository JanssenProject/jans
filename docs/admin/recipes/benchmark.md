---
tags:
  - administration
  - helm
  - kubernetes
  - benchmarking
---

# Overview

The Gluu Server has been optimized with several container strategies that allow scaling micro-services and orchestrating them using Kubernetes. In this tutorial we will be running a load test from three different regions on a janssen setup on three different regions. For simplicity, we will be using [microk8s](https://microk8s.io) however we do recommend users to use the kubernetes cluster providers that they will be using in production. For instance, we run our loadtests across EKS, GKE, AKS and DOKS.

With this procedure you can expect to get the [following](#results) with a `10` million user database:

#### Results

!!!note
    The authorization code flow  hits a total of 4 steps, 3 authorization steps `/token`, `/authorize`, `/oxauth/login` and 1 redirect.
    
| Flow                    | Authentications per second |
|-------------------------|----------------------------|
| Authorization code flow | 800-1000                   |


## Installation

As mentioned in the overview we recommend you use the same Kubernetes cluster as you are planning in production. You can find guides to install on different clouds [here](../install/helm-install/README.md).

### Persistence

We recommend your persistence in production to be HA, backup supported and point in time recovery supported.

| Persistence      | # of nodes | RAM(GiB) | CPU | Total RAM (GiB) | Total CPU |
|------------------|------------|----------|-----|-----------------|-----------|
| MySQL            | 1          | 52       | 8   | 52              | 8         |

### Set up the cluster

#### Kubernetes Cluster Load Test Resources

!!!note
    Instance type can be selected to best suit the deployment intended. Keep in mind when selecting the instance type to strive for a `10` or up to `10` network bandwidth (Gbps). Above details the exact resources needed for this tutorial.

Resourcing is  critical as timeouts in connections can occur, resulting in failed authentications or cutoffs.

| Regions     | # of nodes | RAM(GiB) | CPU | Total RAM (GiB) | Total CPU |
|-------------|------------|----------|-----|-----------------|-----------|
| US-West     | 1          | 96       | 48  | 96              | 48        |
| US-East     | 1          | 96       | 48  | 96              | 48        |
| EU-Central  | 1          | 96       | 48  | 96              | 48        |
| Grand Total |            |          |     | 288 GB          | 144       |


You may choose to create a cluster with three nodes or more in one region and that's fine as long as the nodes are in multiple zones. We will continue with the above table and using microk8s.
   

1. Create three ubuntu 22.04 nodes and run on each one the following:

    ```bash
    sudo snap install microk8s --classic
    sudo snap alias microk8s.kubectl kubectl
    ```
2. Designate on of the nodes as the master. We will choose the us-west node to be our master. On the master node run:

   * Execute : 
     ```bash
     microk8s.enable ingress dashboard observability dns metrics-server hostpath-storage registry
     ```

   * All the other microk8s nodes must be resolvable from within the master. If you are not using registered fqdns open the `/etc/hosts` file in the master node and map each hostname of the other nodes. You can get the hostname of the other nodes by executing the command `hostname`.
     ```bash
     # On master
     echo "192.123.123.123 ubuntu-us-east" >> /etc/hosts
     echo "192.124.124.124 ubuntu-eu-central" >> /etc/hosts
     ```
   * Execute:  
   ```bash
     microk8s add-node
    ```
   
   Copy the output of the command above with `--worker` i.e `microk8s join 192.12.12.12:25000/88687d1cc5ecdee0db5014c4df9b82cb/adedf6a730eb --worker` and execute it in the other nodes to join them. You may need to reexecute step `2` for each worker node.

3. Make sure [helm](https://helm.sh/docs/intro/install/) is installed.

4. Prepare your [override.yaml](../install/helm-install/README.md). Copy the below into a file named override.yaml. At the time of writing this we are using image tags `1.0.6_dev` which are the bleeding edge images for release `1.0.6`. You should use stable images such as `1.0.6-1`.
   ```yaml
   config:
      image:
        repository: janssenproject/configurator
        tag: 1.0.6_dev 
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
        repository: janssenproject/auth-server
        tag: 1.0.6_dev
    config-api:
      image:
        pullPolicy: IfNotPresent
        repository: janssenproject/config-api
        tag: 1.0.6_dev
    persistence:
      image:
        pullPolicy: IfNotPresent
        repository: janssenproject/persistence-loader
        tag: 1.0.6_dev 
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

### Load-test

Our tests used 10 million users that were loaded. We have created a docker image to load users. That same image is also used to load test Janssen using jmeter tests for the `Authorization code` flow. More tests will come!. This image will load users and use a unique password for each user.

#### Loading users

1. Create a folder called `add_users`.

    ```bash
    mkdir add_users && cd add_users
    ```
2. Copy the following [yaml](https://github.com/JanssenProject/jans/blob/main/demos/benchmarking/docker-jans-loadtesting-jmeter/yaml/load-users/load_users_rdbms_job.yaml) into the folder under the name `load_users.yaml`.

3. Open the file and modify the sql connection parameters

4. Create a namespace for load-testing.

    ```bash
    kubectl create ns load
    ```
   
5. Create `load_users.yaml`

    ```bash
    kubectl create -f load_users.yaml -n load
    ```

Wait until all the users are up before moving forward. 

#### Load testing

##### Authorization code client

###### Resources needed for Authorization code client jmeter test

| NAME                                | # of pods | RAM(GiB) | CPU | Total RAM(GiB) | Total CPU |
|-------------------------------------|-----------|----------|-----|----------------|-----------|
| Authorization code flow jmeter test | 20        | 8        | 1.3 | 190            | 24        |
| Grand Total                         |           |          |     | 190 GiB        | 24        |

###### Setup Client

Create the client needed to run the test by executing the following. Make sure to change the `FQDN`  :

1. Create a folder called `load_test`.

    ```bash
    mkdir load_test && cd load_test
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
3. Copy the following [yaml](https://github.com/JanssenProject/jans/blob/main/demos/benchmarking/docker-jans-loadtesting-jmeter/yaml/load-test/load_test_auth_code.yaml) into the folder.

4. Download or build [config-cli-tui](../config-guide/tui.md) and run:

    ```bash
    # Notice the namespace is jans here . Change it if you changed it previously
    ROLE_BASED_CLIENT_ID=$(kubectl get cm cn -o json -n jans | grep '"role_based_client_id":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]')
    ROLE_BASED_CLIENT_SECRET=$(kubectl get secret cn -o json -n jans | grep '"role_based_client_pw":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d)
    # add -noverify if your fqdn is not registered
    ./config-cli-tui.pyz --host $FQDN --client-id $ROLE_BASED_CLIENT_ID --client-secret $ROLE_BASED_CLIENT_SECRET --no-tui --operation-id=post-oauth-openid-client --data=auth_code_client.json
    ```

5. Save the client id and secret from the response and enter them along with your FQDN in the yaml file `load_test_auth_code.yaml`  under `AUTHZ_CLIENT_ID`, `AUTHZ_CLIENT_SECRET` and `FQDN` respectively then execute :

    ```bash
    kubectl apply -f load_test_auth_code.yaml
    ```

6. The janssen setup by default installs an HPA which will automatically scale your pods if the metrics server is installed according to traffic. If you would like to load it very quickly scale the auth-server manually:
    ```bash
    kubectl scale deploy janssen-auth-server -n jans --replicas=40
   ```
   
7. Finally, scale the load test. You must control this replica number.
    ```bash
    kubectl scale deploy load-testing -n load --replicas=20
   ```


