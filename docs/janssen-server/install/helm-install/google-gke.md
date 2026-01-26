---
tags:
  - administration
  - installation
  - helm
  - GKE
  - Google Cloud
  - GCP
---

# Install Janssen on GKE

## System Requirements

{% include "includes/cn-system-requirements.md" %}

## Initial Setup

1.  Enable [GKE API](https://console.cloud.google.com/kubernetes) if not enabled yet.

2.  If you are using `Cloud Shell`, you can skip to step 6.

3.  Install [gcloud](https://cloud.google.com/sdk/docs/quickstarts).

4.  Install `kubectl` using `gcloud components install kubectl` command.

5.  Install [Helm3](https://helm.sh/docs/intro/install/).

6.  Create cluster using a command such as the following example:

    ```  
    gcloud container clusters create janssen-cluster --num-nodes 2 --machine-type e2-standard-4 --zone us-west1-a
    ```
    You can adjust `num-nodes` and `machine-type` as per your desired cluster size

7.  Create `jans` namespace where our resources will reside
    ```
    kubectl create namespace jans
    ```

## Janssen Installation using Helm

### Ingress & Traffic Management

#### Option 1: Gateway API resources (Recommended)

1. **Gateway API CRDs [installation](https://gateway-api.sigs.k8s.io/guides/getting-started/#installing-gateway-api)**:

    If your cluster does not have the Gateway API Custom Resource Definitions yet, install them:

    `kubectl apply --server-side -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.4.1/standard-install.yaml`



2. **Ensure a Gateway Controller is installed.**
    You must have a [conformant Gateway Controller](https://gateway-api.sigs.k8s.io/implementations/#conformant) installed in your cluster.
    *Example: Installing the Nginx Gateway Fabric controller:*
    `helm install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric --create-namespace -n nginx-gateway`



3. **Gateway IP:** 

    Determine how your Gateway will get its IP address:

    * **Option A: Static IP (Recommended)**
        Reserve a static public IP with your cloud provider *before* installation. You will add this IP to `override.yaml` in the next step. The Gateway will listen on this address immediately.

    * **Option B: Dynamic IP**
    
        If you are relying on a dynamically assigned IP:

        1. Run the initial Helm install **without** setting `global.lbIp`.

        2. Wait for the cloud provider to assign an IP to the Gateway. Retrieve it using:

          `kubectl get gateway -n <janssen-namespace>`

        3. Add the retrieved IP to `global.lbIp` in your `override.yaml`.

        4. Run `helm upgrade` to apply the IP change to the application configuration.



4.  **Configure `override.yaml`:**

    Add the following snippet to your `override.yaml` to enable the Gateway API and disable the legacy Nginx-Ingress.

    ```yaml
    global:
      lbIp: # Add Static IP here. If Dynamic, leave empty for first install.
      gatewayApi:
        enabled: true
      nginx-ingress:
        enabled: false 
      fqdn: demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
      isFqdnRegistered: true # Leave it as false if you don't have a registered FQDN
    gatewayApi:
      # Set the gatewayClassName based on the controller used (e.g., 'nginx', 'istio')
      gatewayClassName: nginx
      # The name of the Gateway resource to be created
      name: jans-gateway
      # Gateway http port number
      httpPort: 80 
      # Gateway https port number
      httpsPort: 443
    ```


#### Option 2: Using Kubernetes Ingress resources(Legacy)

1.  Install the retired [ingress-nginx](https://github.com/kubernetes/ingress-nginx)
    
      ```
      helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
      helm repo add stable https://charts.helm.sh/stable
      helm repo update
      helm install nginx ingress-nginx/ingress-nginx
      ```

2.  Configure `override.yaml`:
    
        
    Get the Loadbalancer IP: 
      
    `kubectl get svc nginx-ingress-nginx-controller --output jsonpath='{.status.loadBalancer.ingress[0].ip}'`
          
      
    Then add the following yaml snippet to your `override.yaml` file:

    ```yaml
    global:
        lbIp: #Add the LoadBalancer IP from the previous command
        fqdn: demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
        isFqdnRegistered: true # Leave it as false if you don't have a registered FQDN
    nginx-ingress:
      ingress:
          path: /
          hosts:
          - demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
          tls:
          - secretName: tls-certificate
            hosts:
            - demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
    ```

### Persistence storage

#### PostgreSQL for persistence storage

In a production environment, a production-grade PostgreSQL server should be used such as `Cloud SQL`

For testing purposes, you can deploy it on the GKE cluster using the following commands:

```
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/pgsql.yaml
kubectl apply -f pgsql.yaml
```

Add the following yaml snippet to your `override.yaml` file:

```yaml
config:
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 5432
    cnSqlDbDialect: pgsql
    cnSqlDbHost: postgresql.jans.svc
    cnSqlDbUser: postgres
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#
```

#### MySQL for persistence storage

In a production environment, a production-grade MySQL server should be used such as `Cloud SQL`

For testing purposes, you can deploy it on the GKE cluster using the following commands:

```
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/mysql.yaml
kubectl apply -f mysql.yaml
```

Add the following yaml snippet to your `override.yaml` file:

```yaml        
config:
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 3306
    cnSqlDbDialect: mysql
    cnSqlDbHost: mysql.jans.svc
    cnSqlDbUser: root
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#
```

### Simple `override.yaml` configuration example

Here is a complete example using **Gateway API**, **MySQL**, and a registered **FQDN**:

```yaml
global:
  lbIp: "" #Add the LoadBalancer IP
  fqdn: demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
  isFqdnRegistered: true # Leave it as false if you don't have a registered FQDN
  gatewayApi:
    enabled: true
  nginx-ingress:
    enabled: false 
gatewayApi:
  gatewayClassName: nginx # Set the gatewayClassName based on the controller used
  name: jans-gateway  # The name of the Gateway resource to be created
config:
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 3306
    cnSqlDbDialect: mysql
    cnSqlDbHost: mysql.jans.svc
    cnSqlDbUser: root
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#
```

###  Install Janssen

After finishing all the tweaks to the `override.yaml` file, we can use it to install janssen.

```
helm repo add janssen https://docs.jans.io/charts
helm repo update
helm install janssen janssen/janssen -n jans -f override.yaml
```

## Configure Janssen
  You can use the [TUI](../../kubernetes-ops/tui-k8s.md) to configure Janssen components. The TUI calls the Config API to perform ad hoc configuration.
