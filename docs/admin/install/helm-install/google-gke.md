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

The resources may be set to the minimum as below:

- 8 GiB RAM
- 8 CPU cores
- 50GB hard-disk

Use the listing below for detailed estimation of minimum required resources. Table contains the default resources recommendations per service. Depending on the use of each service the resources needs may be increase or decrease.

| Service           | CPU Unit | RAM   | Disk Space | Processor Type | Required                           |
| ----------------- | -------- | ----- | ---------- | -------------- | ---------------------------------- |
| Auth server       | 2.5      | 2.5GB | N/A        | 64 Bit         | Yes                                |
| LDAP (OpenDJ)     | 1.5      | 2GB   | 10GB       | 64 Bit         | Only if couchbase is not installed |
| fido2             | 0.5      | 0.5GB | N/A        | 64 Bit         | No                                 |
| scim              | 1.0      | 1.0GB | N/A        | 64 Bit         | No                                 |
| config - job      | 0.5      | 0.5GB | N/A        | 64 Bit         | Yes on fresh installs              |
| persistence - job | 0.5      | 0.5GB | N/A        | 64 Bit         | Yes on fresh installs              |
| client-api        | 1        | 0.4GB | N/A        | 64 Bit         | No                                 |
| nginx             | 1        | 1GB   | N/A        | 64 Bit         | Yes if not ALB                     |
| auth-key-rotation | 0.3      | 0.3GB | N/A        | 64 Bit         | No [Strongly recommended]          |
| config-api        | 0.5      | 0.5GB | N/A        | 64 Bit         | No                                 |

Releases of images are in style 1.0.0-beta.0, 1.0.0-0

## Initial Setup

1.  If you are using Cloud Shell, you can skip to step 4.

2.  Install [gcloud](https://cloud.google.com/sdk/docs/quickstarts)
    
3.  Install kubectl using `gcloud components install kubectl` command
    
4.  Create cluster using a command such as the following example:

    ```  
    gcloud container clusters create CLUSTER_NAME --num-nodes 2 --machine-type e2-highcpu-8 --zone ZONE_NAME
    ```
    where `CLUSTER_NAME` is the name you choose for the cluster and `ZONE_NAME` is the name of Availability Zone, for example: us-west1-a (https://cloud.google.com/compute/docs/regions-zones/) where the cluster resources live in.

5.  Install [Helm3](https://helm.sh/docs/intro/install/)    

6.  Configure enviroment variable `KUBECONFIG` to be able to authenticate against the cluster
    ```
    export KUBECONFIG=~/.kube/config 
    ```

## Jans Installation using Helm
1.  Install [Nginx-Ingress](https://github.com/kubernetes/ingress-nginx), if you are not using Istio ingress
    
      ```
      helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
      helm repo add stable https://charts.helm.sh/stable
      helm repo update
      helm install nginx ingress-nginx/ingress-nginx
      ```

2.  Create override.yaml file and add changes as per your desired configuration:

    - FQDN/domain not registered:
    
        
        Get the Loadbalancer IP: 
        ```
        kubectl get svc nginx-ingress-nginx-controller --output jsonpath='{.status.loadBalancer.ingress[0].ip}'
        ```

      
        
        Add this to override.yaml:

        ```yaml
        global:
            lbIp: #Add IP from previous command
            isFqdnRegistered: false
        ```

    - FQDN/domain registered:
    
        If the FQDN for jans i.e `demoexample.jans.org` is registered and globally resolvable, you have to map it to the loadbalancer IP created in the previous step by Nginx-Ingress.
        More details in this [guide](https://medium.com/@kungusamuel90/custom-domain-name-mapping-for-k8s-on-gcp-4dc263b2dabe).

        Add this to override.yaml:

        ```yaml
        global:
            lbIp: "" #Add LoadBalancer IP from previous command
            fqdn: demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
        nginx:
          ingress:
              enabled: true
              path: /
              hosts:
              - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
              tls:
              - secretName: tls-certificate
                hosts:
                - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
        ```






    -  LDAP/Opendj for persitence storage


          Add this to override.yaml:
          ```yaml
          cnPersistenceType: ldap
          storageClass:
            provisioner: kubernetes.io/gce-pd
          opendj:
            enabled: true
          ```

          So now configuring both LDAP and no-FQDN will look something like that:

          ```yaml
            cnPersistenceType: ldap
            lbIp: #Add Load Balancer IP from previous command
            isFqdnRegistered: false
            storageClass:
              provisioner: kubernetes.io/gce-pd
            opendj:
              enabled: true
          ```







    - MySQL for persistence storage
      ```
      sudo helm repo add bitnami https://charts.bitnami.com/bitnami
      sudo helm install my-release --set auth.rootPassword=Test1234#,auth.database=jans bitnami/mysql -n jans --kubeconfig="$KUBECONFIG"
      ```

        Add this to override.yaml:
      
        ```yaml
        global:
          cnPersistenceType: sql
          lbIp: #Add Load Balancer IP from previous command
          isFqdnRegistered: false
        config:
          configmap:
            cnSqlDbName: jans
            cnSqlDbPort: 3306
            cnSqlDbDialect: mysql
            cnSqlDbHost: my-release-mysql.jans.svc
            cnSqlDbUser: root
            cnSqlDbTimezone: UTC
            cnSqldbUserPassword: Test1234#
        ```

          Install the MySQL database:
            ```
            sudo helm install janssen janssen/janssen -n jans -f mysql.yaml --kubeconfig=$KUBECONFIG
            ```

3.  Install Jans



      After finishing all the tweaks to the `override.yaml` file, we can use it to install jans.

      ```
      sudo helm repo add janssen https://docs.jans.io
      sudo helm repo update
      kubectl create namespace jans
      sudo helm install janssen janssen/janssen -n jans -f override.yaml --kubeconfig=$KUBECONFIG
      ```