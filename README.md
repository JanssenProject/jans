
![couchbase](https://github.com/JanssenProject/jans-cloud-native/workflows/couchbase/badge.svg)
![opendj](https://github.com/JanssenProject/jans-cloud-native/workflows/opendj/badge.svg)
![opendj-istio](https://github.com/JanssenProject/jans-cloud-native/workflows/opendj-istio/badge.svg)

## System Requirements for cloud deployments

!!!note
    For local deployments like `minikube` and `microk8s`  or cloud installations in demo mode, resources may be set to the minimum and hence  can have `8GB RAM`, `4 CPU`, and `50GB disk` in total to run all services.
  
Please calculate the minimum required resources as per services deployed. The following table contains default recommended resources to start with. Depending on the use of each service the resources may be increased or decreased. 

|Service           | CPU Unit   |    RAM      |   Disk Space     | Processor Type | Required                           |
|------------------|------------|-------------|------------------|----------------|------------------------------------|
|Auth server       | 2.5        |    2.5GB    |   N/A            |  64 Bit        | Yes                                |
|LDAP (OpenDJ)     | 1.5        |    2GB      |   10GB           |  64 Bit        | Only if couchbase is not installed |
|fido2             | 0.5        |    0.5GB    |   N/A            |  64 Bit        | No                                 |
|scim              | 1.0        |    1.0GB    |   N/A            |  64 Bit        | No                                 |
|config - job      | 0.5        |    0.5GB    |   N/A            |  64 Bit        | Yes on fresh installs              |
|persistence - job | 0.5        |    0.5GB    |   N/A            |  64 Bit        | Yes on fresh installs              |
|client-api        | 1          |    0.4GB    |   N/A            |  64 Bit        | No                                 |
|nginx             | 1          |    1GB      |   N/A            |  64 Bit        | Yes if not ALB                     |
|auth-key-rotation | 0.3        |    0.3GB    |   N/A            |  64 Bit        | No [Strongly recommended]          |
|config-api        | 0.5        |    0.5GB    |   N/A            |  64 Bit        | No                                 |

# Quickstart Janssen with microk8s

Start a fresh ubuntu `18.04` or `20.04` and execute the following

```
sudo su -
wget https://raw.githubusercontent.com/JanssenProject/jans-cloud-native/master/automation/startdemo.sh && chmod u+x startdemo.sh && ./startdemo.sh
```

This will install docker, microk8s, helm and Janssen with the default settings the can be found inside [values.yaml](helm/values.yaml). Please map the `ip` of the instance running ubuntu to `demoexample.jans.io` and then access the endpoints at your browser such in the example in the table below.


|Service           | Example endpoint                                                       |   
|------------------|------------------------------------------------------------------------|
|Auth server       | `https://demoexample.jans.io/.well-known/openid-configuration`        |
|fido2             | `https://demoexample.jans.io/.well-known/fido2-configuration`         |
|scim              | `https://demoexample.jans.io/.well-known/scim-configuration`          |   

# Install with helm

1. Configure cloud or local kubernetes cluster:

    ## Amazon Web Services (AWS) - EKS
      
    ### Setup Cluster
    
    -  Follow this [guide](https://docs.aws.amazon.com/eks/latest/userguide/getting-started.html)
     to install a cluster with worker nodes. Please make sure that you have all the `IAM` policies for the AWS user that will be creating the cluster and volumes.
    
    ### Requirements
    
    -   The above guide should also walk you through installing `kubectl` , `aws-iam-authenticator` and `aws cli` on the VM you will be managing your cluster and nodes from. Check to make sure.
    
            aws-iam-authenticator help
            aws-cli
            kubectl version
    
    - **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports. If you have insalled istio ingress , a loadbalancer will have been created. Please save the address of loadblancer for use later during installation.

    !!!note
        Default  AWS deployment will install a classic load balancer with an `IP` that is not static. Don't worry about the `IP` changing. All pods will be updated automatically with our script when a change in the `IP` of the load balancer occurs. However, when deploying in production, **DO NOT** use our script. Instead, assign a CNAME record for the LoadBalancer DNS name, or use Amazon Route 53 to create a hosted zone. More details in this [AWS guide](https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/using-domain-names-with-elb.html?icmpid=docs_elb_console).
      
    ## GCE (Google Cloud Engine) - GKE
    
    ### Setup Cluster

    1.  Install [gcloud](https://cloud.google.com/sdk/docs/quickstarts)
    
    1.  Install kubectl using `gcloud components install kubectl` command
    
    1.  Create cluster using a command such as the following example:
    
            gcloud container clusters create exploringjanssen --num-nodes 2 --machine-type e2-highcpu-8 --zone us-west1-a
    
        where `CLUSTER_NAME` is the name you choose for the cluster and `ZONE_NAME` is the name of [zone](https://cloud.google.com/compute/docs/regions-zones/) where the cluster resources live in.
    
    1.  Configure `kubectl` to use the cluster:
    
            gcloud container clusters get-credentials CLUSTER_NAME --zone ZONE_NAME
    
        where `CLUSTER_NAME` is the name you choose for the cluster and `ZONE_NAME` is the name of [zone](https://cloud.google.com/compute/docs/regions-zones/) where the cluster resources live in.
    
        Afterwards run `kubectl cluster-info` to check whether `kubectl` is ready to interact with the cluster.
        
    1.  If a connection is not made to google consul using google account the call to the api will fail. Either connect to google consul using an associated google account and run any `kubectl` command like `kubectl get pod` or create a service account using a json key [file](https://cloud.google.com/docs/authentication/getting-started).
    
    - **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports. If you have insalled istio ingress , a loadbalancer will have been created. Please save the ip of loadblancer for use later during installation.
    
    ## DigitalOcean Kubernetes (DOKS)
    
    ### Setup Cluster
    
    -  Follow this [guide](https://www.digitalocean.com/docs/kubernetes/how-to/create-clusters/) to create digital ocean kubernetes service cluster and connect to it.

    - **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports. If you have insalled istio ingress , a loadbalancer will have been created. Please save the ip of loadblancer for use later during installation.

    ## Azure - AKS
    
    !!!warning
        Pending
        
    ### Requirements
    
    -  Follow this [guide](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest) to install Azure CLI on the VM that will be managing the cluster and nodes. Check to make sure.
    
    -  Follow this [section](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough#create-a-resource-group) to create the resource group for the AKS setup.
    
    -  Follow this [section](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough#create-aks-cluster) to create the AKS cluster
    
    -  Follow this [section](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough#connect-to-the-cluster) to connect to the AKS cluster
    
    - **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports. If you have insalled istio ingress , a loadbalancer will have been created. Please save the ip of loadblancer for use later during installation.

    ## Minikube
    
    ### Requirements
    
    1. Install [minikube](https://github.com/kubernetes/minikube/releases).
    
    1. Install [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/).
    
    1. Create cluster:
    
        ```bash
        minikube start
        ```
            
    1. Configure `kubectl` to use the cluster:
    
            kubectl config use-context minikube
            
    1. Enable ingress on minikube
    
        ```bash
        minikube addons enable ingress
        ```
        
    1. **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports.Please note that at the moment Istio ingress is not supported with Minikube. 
    
    ## MicroK8s
    
    ### Requirements
    
    1. Install [MicroK8s](https://microk8s.io/)
    
    1. Make sure all ports are open for [microk8s](https://microk8s.io/docs/)
    
    1. Enable `helm3`, `storage`, `ingress` and `dns`.
    
        ```bash
        sudo microk8s.enable helm3 storage ingress dns
        ```
        
    1. **Optional[alpha]:** If using Istio please enable it.  Please note that at the moment Istio ingress is not supported with Microk8s.
    
        ```bash
        sudo microk8s.enable istio
        ```   

1. Clone this repo

1. Edit the [values.yaml](helm/values.yaml) to configure your settings and use helm to install chart

    ```
        helm install <release-name> -f ./helm/values.yaml ./helm -n <namespace> 
    ```

   
### Configuration

#### global

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |
| `global.istio.ingress`                             | Enable use of Istio ingress  [Alpha]                                                                                             | `false`                             |
| `global.istio.enabled`                             | Enable use of Istio in Janssen namespace. This will inject sidecars into Janssen pods.  [Alpha]                                        | `false`                             |     
| `global.istio.namespace`                           | Istio namespace  [Alpha]                                                                                                         | `istio-system`                      |           
| `global.upgrade.enabled`                           | Disables immutable objects  when set to true                                                                                     | `false`                             |           
| `global.cloud.testEnviroment`                      | Test Environment doesn't include resources section in yaml                                                                       | `false`                             |
| `global.provisioner`                               | Which cloud provisioner to use when deploying                                                                                    | `k8s.io/minikube-hostpath`          |
| `global.pool`                                      | Storage class pool                                                                                                               | `default`                           |
| `global.fsType`                                    | Storage class fsType                                                                                                             | `ext4`                              |
| `global.gcePdStorageType`                          | Google cloud engine storage class disk type                                                                                      | `pd-standard`                       |
| `global.azureStorageAccountType`                   | Azure storage class disk type                                                                                                    | `Standard_LRS`                      |
| `global.azureStorageKind`                          | Azure storage class kind                                                                                                         | `Managed`                           |
| `global.reclaimPolicy`                             | Storage class reclaim policy                                                                                                     | `Retain`                            |
| `global.lbIp`                                      | IP address to be used with a FQDN                                                                                                | `192.168.99.100` (for minikube)     |
| `global.domain`                                    | DNS domain name                                                                                                                  | `demoexample.jans.io`              |
| `global.isDomainRegistered`                        | Whether the domain to be used is registered or not                                                                               | `false`                             |
| `global.cnPersistenceType`                         | Which database backend to use                                                                                                    | `ldap`                              |
| `global.auth-server.enabled`                       | Enable Auth Server                                                                                                               | `true`                              |
| `global.fido2.enabled`                             | Enable Fido2                                                                                                                     | `false`                             |
| `global.scim.enabled`                              | Enable SCIM                                                                                                                      | `false`                             |
| `global.config.enabled`                            | Enable Config                                                                                                                    | `true`                              |
| `global.config-api.enabled`                        | Enable Config API                                                                                                                   | `true`                              |        
| `global.persistence.enabled`                       | Enable Persistence                                                                                                               | `true`                              |
| `global.opendj.enabled`                            | Enable Opendj                                                                                                                    | `true`                              |
| `global.client-api.enabled`                        | Enable client-api                                                                                                                       | `false`                             |
| `global.nginx-ingress.enabled`                     | Enable Ingress nginx                                                                                                             | `true`                              |
| `global.auth-key-rotation.enabled`                 | Enable Auth server Key Rotation                                                                                                       | `false`                             |

#### config   

| Parameter                                             | Description                                                                                                                      | Default                                                     |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------- | 
| `config.orgName`                                      | Organisation Name                                                                                                                | `Janssen`                                                      |
| `config.email`                                        | Email address of the administrator usually. Used for certificate creation                                                        | `support@cn.org`                                          |
| `config.adminPass`                                    | Admin password to log in to the UI                                                                                               | `P@ssw0rd`                                                  |
| `config.ldapPass`                                     | Ldap admin password                                                                                                              | `P@ssw0rd`                                                  |
| `config.redisPass`                                    | Redis password                                                                                                                   | `P@ssw0rd`                                                  |
| `config.countryCode`                                  | Country code of where the Org is located                                                                                         | `US`                                                        |
| `config.state`                                        | State                                                                                                                            | `TX`                                                        |
| `config.city`                                         | City                                                                                                                             | `Austin`                                                    |
| `config.configmap.cnClientApiApplicationCertCn`           | client-api OAuth client application certificate common name                                                                             | `client-api`                                                |
| `config.configmap.cnClientApiAdminCertCn`                 | client-api OAuth client admin certificate common name                                                                                   | `client-api`                                                |
| `config.configmap.cnCouchbaseCrt`                   | Couchbase certificate authority                                                                                                  | `LS0tLS1CRUdJTiBDRVJ.....`                                  |
| `config.configmap.cnCouchbasePass`                  | Couchbase password                                                                                                               | `P@ssw0rd`                                                  |
| `config.configmap.cnCouchbaseSuperUserPass`         | Couchbase superuser password                                                                                                     | `P@ssw0rd`                                                  |        
| `config.configmap.cnCouchbaseUrl`                   | Couchbase URL. Used only when `global.cnPersistenceType` is `hybrid` or `couchbase`                                            | `cbjans.cbns.svc.cluster.local`                             |
| `config.configmap.cnCouchbaseUser`                  | Couchbase user. Used only when `global.cnPersistenceType` is `hybrid` or `couchbase`                                           | `jans`                                                      |
| `config.configmap.cnCouchbaseSuperUser`             | Couchbase superuser. Used only when `global.cnPersistenceType` is `hybrid` or `couchbase`                                      | `admin`                                                     |        
| `config.configmap.cnCouchbasePassFile`              | Location of `couchbase_password` file                                                                                            | `/etc/jans/conf/couchbase_password`                         |
| `config.configmap.cnCouchbaseSuperUserPassFile`     | Location of `couchbase_superuser_password` file                                                                                  | `/etc/jans/conf/couchbase_superuser_password`               |        
| `config.configmap.cnCouchbaseCertFile`              | Location of `couchbase.crt` used by cb for tls termination                                                                       | `/etc/jans/conf/couchbase.crt`                              |     
| `config.configmap.cnPersistenceLdapMapping`         | if cnPersistenceType is hybrid, what to store in ldap.                                                                         | `default`, `user`, `site`, `cache`, `statistic`             |     
| `config.configmap.cnCacheType`                      | if cnCacheType is hybrid, what to store in ldap.                                                                               | `REDIS`, `NATIVE_PERSISTENCE`, `IN_MEMORY`                  |     
| `config.configmap.cnMaxRamPercent`                  | Used in conjunction with pod memory limitations to identify the percentage of the maximum amount of heap memory                  | `false`                                                     |     
| `config.configmap.configAdapterName`                  | Configuration adapter.                                                                                                           | `kubernetes`                                                |     
| `config.configmap.containerMetadataName`              | The name of scheduler to pull container metadata                                                                                 | `kubernetes`                                                |     
| `config.configmap.configSecretAdapter`                | Secret adapter                                                                                                                   | `kubernetes`                                                |     
| `config.configmap.cnRedisUrl`                       | Redis url with port. Used when Redis is deployed for Cache                                                                       | `redis:6379`                                                |     
| `config.configmap.cnRedisUseSsl`                    | Redis SSL use                                                                                                                    | `"false"` or `"true"`                                       |
| `config.configmap.cnRedisType`                      | Type of Redis deployed.                                                                                                          | `"SHARDED"`, `"STANDALONE"`, `"CLUSTER"`, or `"SENTINEL"`   |
| `config.configmap.cnRedisSslTruststore`             | Redis SSL truststore. If using cloud provider services this is left empty.                                                       | ``                                                          |
| `config.configmap.cnRedisSentinelGroup`             | Redis Sentinel group                                                                                                             | ``                                                          |
| `config.configmap.cnAuthServerBackend`              | Auth Server backend address                                                                                                           | `oxauth:8080`                                               |
| `config.configmap.cnClientApiServerUrl`             | client-api Oauth client address                                                                                                         | `client-api:8443`                                           |
| `config.configmap.cnClientApiBindIpAddresses`       | client-api bind address                                                                                                          | `*`                                           |        
| `config.configmap.cnLdapUrl`                        | opendj server url. Port and service name of opendj server - should not be changed                                           |  `opendj:1636`                                              |
| `config.configmap.lbAddr`                             | Address of LB or nginx                                                                                                           |  i.e `axx-109xx52.us-west-2.elb.amazonaws.com`              |
| `config.configmap.ldapServiceName`                    | ldap service name. Used to connect other services to ldap                                                                        | `opendj`                                                    |
| `config.image.repository`                             | Config image repository                                                                                                          | `janssenproject/config-init`                                |
| `config.image.tag`                                    | Config image tag                                                                                                                 | `5.0.0_01`                                                  |

#### nginx-ingress

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |     
| `nginx-ingress.ingress.enabled`                    | Enable ingress                                                                                                                   | `true`                              |
| `nginx-ingress.ingress.path`                       | Main path in ingress                                                                                                             | `/`                                 |
| `nginx-ingress.ingress.hosts`                      | Host holding FQDN for janssen                                                                                                       | `[demoexample.jans.io]`            |
| `nginx-ingress.ingress.tls[0].secretName`          | Secret name of TLS certificate. This shouldn't be changed.                                                                       | `tls-certificate`                   |
| `nginx-ingress.ingress.tls[0].hosts`               | Host holding FQDN for janssen                                                                                                       | `demoexample.jans.io`              |

#### opendj

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |  
| `opendj.service.ldapServiceName`                   | Name of opendj service                                                                                                           | `opendj`                            |
| `opendj.replicas`                                  | Opendj replicas                                                                                                                  | `1`                                 |
| `opendj.persistence.size`                          | Storage for OpenDJ pod                                                                                                           | `5Gi`                               |
| `opendj.image.repository`                          | Opendj image repository                                                                                                          | `janssenproject/opendj`             |
| `opendj.image.tag`                                 | Opendj image tag repository                                                                                                      | `5.0.0_01`                          |
| `opendj.image.pullPolicy`                          | Opendj image pull policy                                                                                                         | `Always`                            |
| `opendj.resources.limits.cpu`                      | Opendj memory limit                                                                                                              | `2000Mi`                            |
| `opendj.resources.limits.memory`                   | Opendj cpu limit                                                                                                                 | `1500m`                             |
| `opendj.resources.requests.cpu`                    | Opendj memory request                                                                                                            | `2000Mi`                            |
| `opendj.resources.requests.memory`                 | Opendj cpu request                                                                                                               | `1500m`                             |    

#### persistence

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
| `persistence.image.repository`                     | Persistence image repository                                                                                                     | `janssenproject/persistence`        |
| `persistence.image.tag`                            | Persistence image tag repository                                                                                                 | `5.0.0_01`                          |
| `persistence.image.pullPolicy`                     | Persistence image pull policy                                                                                                    | `Always`                            |

#### auth-server

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
| `auth-server.service.authServerServiceName`             | Name of auth-server service                                                                                                           | `auth-server`                            |
| `auth-server.replicas`                                  | auth-server replicas                                                                                                                  | `1`                                 |
| `auth-server.image.repository`                          | auth-server image repository                                                                                                          | `janssenproject/auth-server`             |
| `auth-server.image.tag`                                 | auth-server image tag repository                                                                                                      | `5.0.0_01`                          |
| `auth-server.image.pullPolicy`                          | auth-server image pull policy                                                                                                         | `Always`                            |
| `auth-server.resources.limits.cpu`                      | auth-server memory limit                                                                                                              | `2500Mi`                            |
| `auth-server.resources.limits.memory`                   | auth-server cpu limit                                                                                                                 | `2500m`                             |
| `auth-server.resources.requests.cpu`                    | auth-server memory request                                                                                                            | `2500Mi`                            |
| `auth-server.resources.requests.memory`                 | auth-server cpu request                                                                                                               | `2500m`                             |


#### fido2

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |              
| `fido2.service.fido2ServiceName`                   | Name of fido2 service                                                                                                            | `fido2`                             |
| `fido2.replicas`                                   | Fido2 replicas                                                                                                                   | `1`                                 |
| `fido2.image.repository`                           | Fido2 image repository                                                                                                           | `janssenproject/fido2`              |
| `fido2.image.tag`                                  | Fido2 image tag repository                                                                                                       | `5.0.0_01`                          |
| `fido2.image.pullPolicy`                           | Fido2 image pull policy                                                                                                          | `Always`                            |
| `fido2.resources.limits.cpu`                       | Fido2 memory limit                                                                                                               | `500Mi`                             |
| `fido2.resources.limits.memory`                    | Fido2 cpu limit                                                                                                                  | `500m`                              |
| `fido2.resources.requests.cpu`                     | Fido2 memory request                                                                                                             | `500Mi`                             |
| `fido2.resources.requests.memory`                  | Fido2 cpu request                                                                                                                | `500m`                              |

#### scim

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
| `scim.service.scimServiceName`                     | Name of SCIM service                                                                                                             | `scim`                              |
| `scim.replicas`                                    | SCIM replicas                                                                                                                    | `1`                                 |
| `scim.image.repository`                            | SCIM image repository                                                                                                            | `janssenproject/scim`               |
| `scim.image.tag`                                   | SCIM image tag repository                                                                                                        | `5.0.0_01`                          |
| `scim.image.pullPolicy`                            | SCIM image pull policy                                                                                                           | `Always`                            |
| `scim.resources.limits.cpu`                        | SCIM memory limit                                                                                                                | `500Mi`                             |
| `scim.resources.limits.memory`                     | SCIM cpu limit                                                                                                                   | `500m`                              |
| `scim.resources.requests.cpu`                      | SCIM memory request                                                                                                              | `500Mi`                             |
| `scim.resources.requests.memory`                   | SCIM cpu request                                                                                                                 | `500m`                              |     

#### client-api

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
| `client-api.service.clientApiServerServiceName`    | Name of client-api Oauth client service                                                                                                 | `client-api`                        |
| `client-api.replicas`                              | client-api Oauth client replicas                                                                                                        | `1`                                 |
| `client-api.image.repository`                      | client-api Oauth client image repository                                                                                                | `janssenproject/client-api`         |
| `client-api.image.tag`                             | client-api Oauth client image tag repository                                                                                            | `5.0.0_01`                          |
| `client-api.image.pullPolicy`                      | client-api Oauth client image pull policy                                                                                               | `Always`                            |
| `client-api.resources.limits.cpu`                  | client-api Oauth client memory limit                                                                                                    | `400Mi`                             |
| `client-api.resources.limits.memory`               | client-api Oauth client cpu limit                                                                                                       | `1000m`                             |
| `client-api.resources.requests.cpu`                | client-api Oauth client memory request                                                                                                  | `400Mi`                             |
| `client-api.resources.requests.memory`             | client-api Oauth client cpu request                                                                                                     | `1000m`                             |     

#### auth-server-key-rotation

| Parameter                                          | Description                                                                                                                      | Default                             |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |              
| `auth-server-key-rotation.keysLife`                          | auth-server Key Rotation Keys life in hours                                                                                      | `48`                                |
| `auth-server-key-rotation.image.repository`                  | auth-server Key Rotation image repository                                                                                        | `janssenproject/certmanager`        |
| `auth-server-key-rotation.image.tag`                         | auth-server Key Rotation image tag repository                                                                                    | `5.0.0_01`                          |
| `auth-server-key-rotation.image.pullPolicy`                  | auth-server Key Rotation image pull policy                                                                                       | `Always`                            |
| `auth-server-key-rotation.resources.requests.limits.cpu`     | auth-server Key Rotation memory limit                                                                                            | `300Mi`                             |
| `auth-server-key-rotation.resources.requests.limits.memory`  | auth-server Key Rotation cpu limit                                                                                               | `300m`                              |
| `auth-server-key-rotation.resources.requests.cpu`            | auth-server Key Rotation memory request                                                                                          | `300Mi`                             |
| `auth-server-key-rotation.resources.requests.memory`         | auth-server Key Rotation cpu request                                                                                             | `300m`                              |     

# Rotating Certificates and Keys in Kubernetes setup

`jans-config-cm` in all examples refer to jans installation configuration parameters. In Helm the name is in the format of `<helms release name>-config-cm` and must be changed in the below examples.


## web (ingress)
    
| Associated certificates and keys |
| -------------------------------- |
| /etc/certs/web_https.crt         |
| /etc/certs/web_https.key         |
    
### Rotate
    
1. Create a file named `web-key-rotation.yaml` with the following contents :

    ```yaml
    # License terms and conditions for Janssen Cloud Native Edition:
    # https://www.apache.org/licenses/LICENSE-2.0
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: web-key-rotation
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"    
        spec:
          restartPolicy: Never
          containers:
            - name: web-key-rotation
              image: janssenproject/certmanager:5.0.0_01
              envFrom:
              - configMapRef:
                  name: jans-config-cm # This may be different in your setup
              args: ["patch", "web"]
    ```
    
1. Apply job

    ```bash
        kubectl apply -f web-key-rotation.yaml -n <jans-namespace>
    ```            
    
### Load from existing source

This will load `web_https.crt` and `web_https.key` from `/etc/certs`.
        
1. Create a secret with `web_https.crt` and `web_https.key`. Note that this may already exist in your deployment.

    ```bash
        kubectl create secret generic web-cert-key --from-file=web_https.crt --from-file=web_https.key -n <jans-namespace>` 
    ```
    
1. Create a file named `load-web-key-rotation.yaml` with the following contents :
                   
    ```yaml
    # License terms and conditions for Janssen Cloud Native Edition:
    # https://www.apache.org/licenses/LICENSE-2.0
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: load-web-key-rotation
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"    
        spec:
          restartPolicy: Never
          volumes:
          - name: web-cert
            secret:
              secretName: web-cert-key
              items:
                - key: web_https.crt
                  path: web_https.crt
          - name: web-key
            secret:
              secretName: web-cert-key
              items:
                - key: web_https.key
                  path: web_https.key                              
          containers:
            - name: load-web-key-rotation
              image: janssenproject/certmanager:5.0.0_01
              envFrom:
              - configMapRef:
                  name: jans-config-cm  #This may be different in your setup
              volumeMounts:
                - name: web-cert
                  mountPath: /etc/certs/web_https.crt
                  subPath: web_https.crt
                - name: web-key
                  mountPath: /etc/certs/web_https.key
                  subPath: web_https.key
              args: ["patch", "web", "--opts", "source:from-files"]
    ```

1. Apply job

    ```bash
        kubectl apply -f load-web-key-rotation.yaml -n <jans-namespace>
    ```            
        
### Auth server

Key rotation cronJob is usually installed with Janssen. Please make sure before deploying `kubectl get cronjobs -n <jans-namespace>`.

| Associated certificates and keys |
| -------------------------------- |
| /etc/certs/auth-keys.json        |
| /etc/certs/auth-keys.jks         |

1. Create a file named `auth-server-key-rotation.yaml` with the following contents :

    ```yaml
    # License terms and conditions for Janssen Cloud Native Edition:
    # https://www.apache.org/licenses/LICENSE-2.0
    kind: CronJob
    apiVersion: batch/v1beta1
    metadata:
      name: auth-key-rotation
    spec:
      schedule: "0 */48 * * *"
      concurrencyPolicy: Forbid
      jobTemplate:
        spec:
          template:
            metadata:
              annotations:
                sidecar.istio.io/inject: "false"    
            spec:
              containers:
                - name: auth-key-rotation
                  image: janssenproject/certmanager:1.0.0_dev
                  resources:
                    requests:
                      memory: "300Mi"
                      cpu: "300m"
                    limits:
                      memory: "300Mi"
                      cpu: "300m"
                  envFrom:
                    - configMapRef:
                        name: jans-config-cm
                  args: ["patch", "auth", "--opts", "interval:48"]
              restartPolicy: Never
    ```

    Key rotation cronJob will try to push `auth-keys.jks` and `auth-keys.json` to auth-server pods. If the service account user does not have permissions to list pods the above will fail with a `403` Forbidden message. This action can be disabled forcing auth-server pods to pull from Kubernetes `Secret`s instead by setting the environment variable `CN_SYNC_JKS_ENABLED` to `true` inside the main config map i.e `jans-config-cm` and adding to the `args` of the above yaml `"--opts", "push-to-container:false"` so the `args` section would look like `args: ["patch", "auth", "--opts", "interval:48", "--opts", "push-to-container:false"]`.
            
1. Apply cron job

    ```bash
        kubectl apply -f auth-server-key-rotation.yaml -n <jans-namespace>
    ```

### Client API server

| Associated certificates and keys           |
| ------------------------------------------ |
| /etc/certs/client_api_application.crt      |
| /etc/certs/client_api_application.key      |   
| /etc/certs/client_api_application.keystore |   
| /etc/certs/client_api_admin.crt            |   
| /etc/certs/client_api_admin.key            |   
| /etc/certs/client_api_admin.keystore       |

Application common name must match client-api service name. `kubectl get svc -n <jans-namespace>`. We assume it to be client-api server below.

1. Create a file named `client-api-key-rotation.yaml` with the following contents :

    ```yaml
    # License terms and conditions for Janssen Cloud Native Edition:
    # https://www.apache.org/licenses/LICENSE-2.0
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: client-api-key-rotation
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"    
        spec:
          restartPolicy: Never
          containers:
            - name: client-api-key-rotation
              image: janssenproject/certmanager:1.0.0_dev
              envFrom:
              - configMapRef:
                  name: jans-config-cm
              # Change application-cn:client-api and admin-cn:client-api to match client-api service name
              args: ["patch", "client-api", "--opts", "application-cn:client-api", "--opts", "admin-cn:client-api"]
    ``` 

1. Apply job

    ```bash
        kubectl apply -f client-api-key-rotation.yaml -n <jans-namespace>
    ```
                 
### LDAP

Subject Alt Name must match opendj service.
        
| Associated certificates and keys    |
| ----------------------------------- |
| /etc/certs/opendj.crt               |
| /etc/certs/opendj.key               |   
| /etc/certs/opendj.pem               |   
| /etc/certs/opendj.pkcs12            |   
    
1. Create a file named `ldap-key-rotation.yaml` with the following contents :

    ```yaml
    # License terms and conditions for Janssen Cloud Native Edition:
    # https://www.apache.org/licenses/LICENSE-2.0
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: ldap-key-rotation
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"   
        spec:
          restartPolicy: Never
          containers:
            - name: ldap-key-rotation
              image: janssenproject/certmanager:1.0.0_dev
              envFrom:
              - configMapRef:
                  name: jans-config-cm
              args: ["patch", "ldap", "--opts", "subj-alt-name:opendj"] 
    ```

1. Apply job

    ```bash
        kubectl apply -f ldap-key-rotation.yaml -n <jans-namespace>
    ```   
                
1. Restart pods.