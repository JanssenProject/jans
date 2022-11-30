---
tags:
  - administration
  - installation
  - helm
  - AKS
  - Microsoft
  - Azure
---

# Install Janssen on AKS

## System Requirements

The resources may be set to the minimum as below:

- 8 GiB RAM
- 8 CPU cores
- 50GB hard-disk

Use the listing below for detailed estimation of minimum required resources. Table contains the default resources recommendations per service. Depending on the use of each service the resources needs may be increase or decrease.

| Service           | CPU Unit | RAM   | Disk Space | Processor Type | Required                           |
|-------------------|----------|-------|------------|----------------|------------------------------------|
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

1.  Install [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)

2.  Create a [Resource Group](https://learn.microsoft.com/en-us/azure/aks/learn/quick-kubernetes-deploy-cli#create-a-resource-group)
    ```
    az group create --name janssen-resource-group --location eastus
    ```
  
3.  Create an [AKS cluster](https://learn.microsoft.com/en-us/azure/aks/learn/quick-kubernetes-deploy-cli#create-aks-cluster) such as the following example:
    ```
    az aks create -g janssen-resource-group -n janssen-cluster --enable-managed-identity --node-vm-size NODE_TYPE --node-count 2 --enable-addons monitoring --enable-msi-auth-for-monitoring  --generate-ssh-keys 
    ``` 
    You can adjust `node-count` and `node-vm-size` as per your desired cluster size


4.  Connect to the [cluster](https://learn.microsoft.com/en-us/azure/aks/learn/quick-kubernetes-deploy-cli#connect-to-the-cluster) 
    ```
    az aks install-cli
    az aks get-credentials --resource-group janssen-resource-group --name janssen-cluster
    ```

5.  Install [Helm3](https://helm.sh/docs/intro/install/)

6.  Create `jans` namespace where our resources will reside
    ```
    kubectl create namespace jans
    ```

## Jans Installation using Helm
1.  Install [Nginx-Ingress](https://github.com/kubernetes/ingress-nginx), if you are not using Istio ingress
    
      ```
      helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
      helm repo add stable https://charts.helm.sh/stable
      helm repo update
      helm install nginx ingress-nginx/ingress-nginx
      ```

2.  Create a file named `override.yaml` and add changes as per your desired configuration:

    - FQDN/domain is *not* registered:
    
        
        Get the Loadbalancer IP: 
        ```
        kubectl get svc nginx-ingress-nginx-controller --output jsonpath='{.status.loadBalancer.ingress[0].ip}'
        ```

      
        
        Add the following yaml snippet to your `override.yaml` file:

        ```yaml
        global:
            lbIp: #Add the Loadbalance IP from the previous command
            isFqdnRegistered: false
        ```

    - FQDN/domain is registered:

        Add the following yaml snippet to your `override.yaml` file`:

        ```yaml
        global:
            lbIp: #Add the LoadBalancer IP from the previous command
            isFqdnRegistered: true
            fqdn: demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
        nginx-ingress:
          ingress:
              path: /
              hosts:
              - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
              tls:
              - secretName: tls-certificate
                hosts:
                - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
        ```






    -  LDAP/Opendj for persistence storage


          Add the following yaml snippet to your `override.yaml` file:
          ```yaml
          global:
            cnPersistenceType: ldap
            storageClass:
              provisioner: disk.csi.azure.com
            opendj:
              enabled: true
          ```

          So if your desired configuration has no-FQDN and LDAP, the final `override.yaml` file will look something like that:

          ```yaml
           global:
             cnPersistenceType: ldap
             lbIp: #Add the Loadbalancer IP from the previous command
             isFqdnRegistered: false
             storageClass:
               provisioner: disk.csi.azure.com
             opendj:
               enabled: true
           nginx-ingress:
            ingress:
                path: /
                hosts:
                - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
                tls:
                - secretName: tls-certificate
                  hosts:
                  - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans      
          ```







    - MySQL for persistence storage

      In a production environment, a production grade MySQL server should be used such as `Azure Database for MySQL`

      For testing purposes, you can deploy it on the AKS cluster using the following commands:

      ```
      helm repo add bitnami https://charts.bitnami.com/bitnami
      helm install my-release --set auth.rootPassword=Test1234#,auth.database=jans bitnami/mysql -n jans
      ```

      Add the following yaml snippet to your `override.yaml` file:
      
      ```yaml
      
      global:
        cnPersistenceType: sql
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

      So if your desired configuration has FQDN and MySQL, the final `override.yaml` file will look something like that:

      ```yaml
      global:
        cnPersistenceType: sql
        lbIp: "" #Add the LoadBalancer IP from previous command
        isFqdnRegistered: true
        fqdn: demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
      nginx-ingress:
        ingress:
            path: /
            hosts:
            - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans
            tls:
            - secretName: tls-certificate
              hosts:
              - demoexample.jans.org #CHANGE-THIS to the FQDN used for Jans  
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

3.  Install Jans



      After finishing all the tweaks to the `override.yaml` file, we can use it to install jans.

      ```
      helm repo add janssen https://docs.jans.io/charts
      helm repo update
      helm install janssen janssen/janssen -n jans -f override.yaml
      ```