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

{% include "includes/cn-system-requirements.md" %}

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

        Add the following yaml snippet to your `override.yaml` file:

        ```yaml
        global:
            lbIp: #Add the LoadBalancer IP from the previous command
            isFqdnRegistered: true
            fqdn: demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
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






    -  LDAP/Opendj for persistence storage

          Prepare cert and key for OpenDJ, for example:

          ```
          openssl req -x509 -newkey rsa:2048 -sha256 -days 365 -nodes -keyout opendj.key -out opendj.crt -subj '/CN=demoexample.jans.io' -addext 'subjectAltName=DNS:ldap,DNS:opendj'
          ```

          Extract the contents of OpenDJ cert and key files as base64 string:

          ```
          OPENDJ_CERT_B64=$(base64 opendj.crt -w0)
          OPENDJ_KEY_B64=$(base64 opendj.key -w0)
          ```

          Add the following yaml snippet to your `override.yaml` file:
          ```yaml
          global:
            cnPersistenceType: ldap
            storageClass:
              provisioner: disk.csi.azure.com
            opendj:
              enabled: true
          config:
            configmap:
              # -- contents of OpenDJ cert file in base64-string
              cnLdapCrt: <OPENDJ_CERT_B64>
              # -- contents of OpenDJ key file in base64-string
              cnLdapKey: <OPENDJ_KEY_B64>
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
           config:
             configmap:
               # -- contents of OpenDJ cert file in base64-string
               cnLdapCrt: <OPENDJ_CERT_B64>
               # -- contents of OpenDJ key file in base64-string
               cnLdapKey: <OPENDJ_KEY_B64>
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



    - Couchbase for pesistence storage
      
        Add the following yaml snippet to your `override.yaml` file:

        ```yaml
        global:
          cnPersistenceType: couchbase

        config:
          configmap:
            # The prefix of couchbase buckets. This helps with separation in between different environments and allows for the same couchbase cluster to be used by different setups of Janssen.
            cnCouchbaseBucketPrefix: jans
            # -- Couchbase certificate authority string. This must be encoded using base64. This can also be found in your couchbase UI Security > Root Certificate. In mTLS setups this is not required.
            cnCouchbaseCrt: SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo=
            # -- The number of replicas per index created. Please note that the number of index nodes must be one greater than the number of index replicas. That means if your couchbase cluster only has 2 index nodes you cannot place the number of replicas to be higher than 1.
            cnCouchbaseIndexNumReplica: 0
            # -- Couchbase password for the restricted user config.configmap.cnCouchbaseUser that is often used inside the services. The password must contain one digit, one uppercase letter, one lower case letter and one symbol
            cnCouchbasePassword: P@ssw0rd
            # -- The Couchbase super user (admin) username. This user is used during initialization only.
            cnCouchbaseSuperUser: admin
            # -- Couchbase password for the superuser config.configmap.cnCouchbaseSuperUser that is used during the initialization process. The password must contain one digit, one uppercase letter, one lower case letter and one symbol
            cnCouchbaseSuperUserPassword: Test1234#
            # -- Couchbase URL. This should be in FQDN format for either remote or local Couchbase clusters. The address can be an internal address inside the kubernetes cluster
            cnCouchbaseUrl: cbjanssen.default.svc.cluster.local
            # -- Couchbase restricted user
            cnCouchbaseUser: janssen
        ```

    - PostgreSQL for persistence storage

        In a production environment, a production grade PostgreSQL server should be used such as `Azure Database for PostgreSQL`

        For testing purposes, you can deploy it on the AKS cluster using the following command:

        ```
        helm install my-release --set auth.postgresPassword=Test1234#,auth.database=jans -n jans oci://registry-1.docker.io/bitnamicharts/postgresql
        ```

        Add the following yaml snippet to your `override.yaml` file:
        
        ```yaml
        
        global:
          cnPersistenceType: sql
        config:
          configmap:
            cnSqlDbName: jans
            cnSqlDbPort: 5432
            cnSqlDbDialect: pgsql
            cnSqlDbHost: my-release-postgresql.jans.svc
            cnSqlDbUser: postgres
            cnSqlDbTimezone: UTC
            cnSqldbUserPassword: Test1234#
        ```

    - MySQL for persistence storage

        In a production environment, a production grade MySQL server should be used such as `Azure Database for MySQL`

        For testing purposes, you can deploy it on the AKS cluster using the following command:

        ```
        helm install my-release --set auth.rootPassword=Test1234#,auth.database=jans -n jans oci://registry-1.docker.io/bitnamicharts/mysql
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
          fqdn: demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
        nginx-ingress:
          ingress:
              path: /
              hosts:
              - demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans
              tls:
              - secretName: tls-certificate
                hosts:
                - demoexample.jans.io #CHANGE-THIS to the FQDN used for Jans  
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

## Configure Janssen
  You can use the [TUI](../../kubernetes-ops/tui-k8s.md) to configure Janssen components. The TUI calls the Config API to perform ad hoc configuration.      
