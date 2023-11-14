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
|-------------------|----------|-------|------------|----------------|------------------------------------|
| Auth server       | 2.5      | 2.5GB | N/A        | 64 Bit         | Yes                                |
| LDAP (OpenDJ)     | 1.5      | 2GB   | 10GB       | 64 Bit         | Only if couchbase is not installed |
| fido2             | 0.5      | 0.5GB | N/A        | 64 Bit         | No                                 |
| scim              | 1.0      | 1.0GB | N/A        | 64 Bit         | No                                 |
| config - job      | 0.5      | 0.5GB | N/A        | 64 Bit         | Yes on fresh installs              |
| persistence - job | 0.5      | 0.5GB | N/A        | 64 Bit         | Yes on fresh installs              |
| nginx             | 1        | 1GB   | N/A        | 64 Bit         | Yes if not ALB                     |
| auth-key-rotation | 0.3      | 0.3GB | N/A        | 64 Bit         | No [Strongly recommended]          |
| config-api        | 1        | 1GB   | N/A        | 64 Bit         | No                                 |

Releases of images are in style 1.0.0-beta.0, 1.0.0-0

## Initial Setup

1.  Enable [GKE API](https://console.cloud.google.com/kubernetes) if not enabled yet.

2.  If you are using `Cloud Shell`, you can skip to step 6.

3.  Install [gcloud](https://cloud.google.com/sdk/docs/quickstarts).
    
4.  Install `kubectl` using `gcloud components install kubectl` command.
    
5.  Install [Helm3](https://helm.sh/docs/intro/install/).

6.  Create cluster using a command such as the following example:

    ```  
    gcloud container clusters create janssen-cluster --num-nodes 2 --machine-type e2-highcpu-8 --zone us-west1-a
    ```
    You can adjust `num-nodes` and `machine-type` as per your desired cluster size

7.  Create `jans` namespace where our resources will reside
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
              provisioner: kubernetes.io/gce-pd
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
               provisioner: kubernetes.io/gce-pd
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
            # -- The prefix of couchbase buckets. This helps with separation in between different environments and allows for the same couchbase cluster to be used by different setups of Janssen.
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

        In a production environment, a production grade PostgreSQL server should be used such as `Cloud SQL`

        For testing purposes, you can deploy it on the GKE cluster using the following command:

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
            cnSqlDbHost: my-release-mysql.jans.svc
            cnSqlDbUser: postgres
            cnSqlDbTimezone: UTC
            cnSqldbUserPassword: Test1234#
        ```

    - MySQL for persistence storage

        In a production environment, a production grade MySQL server should be used such as `Cloud SQL`

        For testing purposes, you can deploy it on the GKE cluster using the following command:

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
