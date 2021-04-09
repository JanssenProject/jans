# The Kubernetes recipes

## System Requirements for cloud deployments

!!!note
    For local deployments like `minikube` and `microk8s`  or cloud installations for demoing Janssen may set the resources to the minimum and hence can have `8GB RAM`, `4 CPU`, and `50GB disk` in total to run all services.
  
Please calculate the minimum required resources as per services deployed. The following table contains default recommended resources to start with. Depending on the use of each service the resources may be increased or decreased. 

|Service           | CPU Unit   |    RAM      |   Disk Space     | Processor Type | Required                                    |
|------------------|------------|-------------|------------------|----------------|---------------------------------------------|
|oxAuth            | 2.5        |    2.5GB    |   N/A            |  64 Bit        | Yes                                         |
|LDAP              | 1.5        |    2GB      |   10GB           |  64 Bit        | if using hybrid or ldap for persistence     |
|[Couchbase](#minimum-couchbase-system-requirements-for-cloud-deployments)         |    -       |      -      |      -           |     -          | If using hybrid or couchbase for persistence|
|fido2             | 0.5        |    0.5GB    |   N/A            |  64 Bit        | No                                          |
|scim              | 1.0        |    1.0GB    |   N/A            |  64 Bit        | No                                          |
|config - job      | 0.5        |    0.5GB    |   N/A            |  64 Bit        | Yes on fresh installs                       |
|jackrabbit        | 1.5        |    1GB      |   10GB           |  64 Bit        | Yes                                         |
|persistence - job | 0.5        |    0.5GB    |   N/A            |  64 Bit        | Yes on fresh installs                       |
|oxTrust           | 1.0        |    1.0GB    |   N/A            |  64 Bit        | No                                          |
|oxShibboleth      | 1.0        |    1.0GB    |   N/A            |  64 Bit        | No                                          |  
|oxPassport        | 0.7        |    0.9GB    |   N/A            |  64 Bit        | No                                          |
|oxd-server        | 1          |    0.4GB    |   N/A            |  64 Bit        | No                                          |
|nginx             | 1          |    1GB      |   N/A            |  64 Bit        | Yes if not ALB                              |
|key-rotation      | 0.3        |    0.3GB    |   N/A            |  64 Bit        | No                                          |
|cr-rotate         | 0.2        |    0.2GB    |   N/A            |  64 Bit        | No                                          |
|casa              | 0.5        |    0.5GB    |   N/A            |  64 Bit        | No                                          |
|radius            | 0.7        |    0.7GB    |   N/A            |  64 Bit        | No                                          |


1. Configure cloud or local kubernetes cluster:

=== "EKS"
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
      

=== "GKE"
    ## GCE (Google Cloud Engine) - GKE
    
    ### Setup Cluster

    1.  Install [gcloud](https://cloud.google.com/sdk/docs/quickstarts)
    
    1.  Install kubectl using `gcloud components install kubectl` command
    
    1.  Create cluster using a command such as the following example:
    
            gcloud container clusters create exploringjans --num-nodes 2 --machine-type e2-highcpu-8 --zone us-west1-a
    
        where `CLUSTER_NAME` is the name you choose for the cluster and `ZONE_NAME` is the name of [zone](https://cloud.google.com/compute/docs/regions-zones/) where the cluster resources live in.
    
    1.  Configure `kubectl` to use the cluster:
    
            gcloud container clusters get-credentials CLUSTER_NAME --zone ZONE_NAME
    
        where `CLUSTER_NAME` is the name you choose for the cluster and `ZONE_NAME` is the name of [zone](https://cloud.google.com/compute/docs/regions-zones/) where the cluster resources live in.
    
        Afterwards run `kubectl cluster-info` to check whether `kubectl` is ready to interact with the cluster.
        
    1.  If a connection is not made to google consul using google account the call to the api will fail. Either connect to google consul using an associated google account and run any `kubectl` command like `kubectl get pod` or create a service account using a json key [file](https://cloud.google.com/docs/authentication/getting-started).
    
    - **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports. If you have insalled istio ingress , a loadbalancer will have been created. Please save the ip of loadblancer for use later during installation.

    
=== "DOKS"
    ## DigitalOcean Kubernetes (DOKS)
    
    ### Setup Cluster
    
    -  Follow this [guide](https://www.digitalocean.com/docs/kubernetes/how-to/create-clusters/) to create digital ocean kubernetes service cluster and connect to it.

    - **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports. If you have insalled istio ingress , a loadbalancer will have been created. Please save the ip of loadblancer for use later during installation.

=== "AKS"
    ## Azure - AKS
    
    !!!warning
        Pending
        
    ### Requirements
    
    -  Follow this [guide](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest) to install Azure CLI on the VM that will be managing the cluster and nodes. Check to make sure.
    
    -  Follow this [section](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough#create-a-resource-group) to create the resource group for the AKS setup.
    
    -  Follow this [section](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough#create-aks-cluster) to create the AKS cluster
    
    -  Follow this [section](https://docs.microsoft.com/en-us/azure/aks/kubernetes-walkthrough#connect-to-the-cluster) to connect to the AKS cluster
    
    - **Optional[alpha]:** If using Istio please [install](https://istio.io/latest/docs/setup/install/standalone-operator/) it prior to installing Janssen. You may choose to use any installation method Istio supports. If you have insalled istio ingress , a loadbalancer will have been created. Please save the ip of loadblancer for use later during installation.

      
=== "Minikube"
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
    
=== "MicroK8s"
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
      
2. Install using one of the following :

=== "Kustomize"
    ## Install Janssen using `pyjans-kubernetes`
    
    1. Download [`pyjans-kubernetes.pyz`](https://github.com/JanssenFederation/cloud-native-edition/releases). This package can be built [manually](#build-pyjans-kubernetespyz-manually).

    1. **Optional:** If using couchbase as the persistence backend. Download the couchbase [kubernetes](https://www.couchbase.com/downloads) operator package for linux and place it in the same directory as `pyjans-kubernetes.pyz`
    
    
    1. Run :
    
        ```bash
        ./pyjans-kubernetes.pyz install
        ```
        
    !!!note
        Prompts will ask for the rest of the information needed. You may generate the manifests (yaml files) and continue to deployment or just generate the  manifests (yaml files) during the execution of `pyjans-kubernetes.pyz`. `pyjans-kubernetes.pyz` will output a file called `settings.json` holding all the parameters. More information about this file and the vars it holds is [below](#settingsjson-parameters-file-contents) but  please don't manually create this file as the script can generate it using [`pyjans-kubernetes.pyz generate-settings`](https://github.com/JanssenFederation/cloud-native-edition/releases). 
    
    ### Uninstall

    1. Run :
    
        ```bash
        ./pyjans-kubernetes.pyz uninstall
        ```

=== "Helm"
    ## Install Janssen using Helm
    
    ### Prerequisites
    
    - Kubernetes 1.x
    - Persistent volume provisioner support in the underlying infrastructure
    - Install [Helm3](https://helm.sh/docs/using_helm/)
    
    ### Quickstart
    
    1. Download [`pyjans-kubernetes.pyz`](https://github.com/JanssenFederation/cloud-native-edition/releases). This package can be built [manually](#build-pyjans-kubernetespyz-manually).
    
    1. **Optional:** If using couchbase as the persistence backend. Download the couchbase [kubernetes](https://www.couchbase.com/downloads) operator package for linux and place it in the same directory as `pyjans-kubernetes.pyz`
    
    1. Run :
    
      ```bash
      ./pyjans-kubernetes.pyz helm-install
      ```
      
    #### Installing Janssen using Helm manually
    
    1. **Optional if not using istio ingress:** Install [nginx-ingress](https://github.com/kubernetes/ingress-nginx) Helm [Chart](https://github.com/helm/charts/tree/master/stable/nginx-ingress).
    
       ```bash
       helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
       helm repo add stable https://charts.helm.sh/stable
       helm repo update
       helm install <nginx-release-name> ingress-nginx/ingress-nginx --namespace=<nginx-namespace>
       ```
    
    1.  - If the FQDN for jans i.e `demoexample.jans.io` is registered and globally resolvable, forward it to the loadbalancers address created in the previous step by nginx-ingress. A record can be added on most cloud providers to forward the domain to the loadbalancer. Forexample, on AWS assign a CNAME record for the LoadBalancer DNS name, or use Amazon Route 53 to create a hosted zone. More details in this [AWS guide](https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/using-domain-names-with-elb.html?icmpid=docs_elb_console). Another example on [GCE](https://medium.com/@kungusamuel90/custom-domain-name-mapping-for-k8s-on-gcp-4dc263b2dabe).
    
        - If the FQDN is not registered acquire the loadbalancers ip if on **GCE**, or **Azure** using `kubectl get svc <release-name>-nginx-ingress-controller --output jsonpath='{.status.loadBalancer.ingress[0].ip}'` and if on **AWS** get the loadbalancers addresss using `kubectl -n ingress-nginx get svc ingress-nginx \--output jsonpath='{.status.loadBalancer.ingress[0].hostname}'`.
    
    1.  - If deploying on the cloud make sure to take a look at the helm cloud specific notes before continuing.
    
          * [EKS](#eks-helm-notes)
          * [GKE](#gke-helm-notes)
    
        - If deploying locally make sure to take a look at the helm specific notes bellow before continuing.
    
          * [Minikube](#minikube-helm-notes)
          * [MicroK8s](#microk8s-helm-notes)
    
    1.  **Optional:** If using couchbase as the persistence backend.
        
        1. Download [`pyjans-kubernetes.pyz`](https://github.com/JanssenFederation/cloud-native-edition/releases). This package can be built [manually](#build-pyjans-kubernetespyz-manually).
        
        1. Download the couchbase [kubernetes](https://www.couchbase.com/downloads) operator package for linux and place it in the same directory as `pyjans-kubernetes.pyz`
    
        1.  Run:
        
           ```bash
           ./pyjans-kubernetes.pyz couchbase-install
           ```
           
        1. Open `settings.json` file generated from the previous step and copy over the values of `COUCHBASE_URL` and `COUCHBASE_USER`   to `global.jansCouchbaseUrl` and `global.jansCouchbaseUser` in `values.yaml` respectively. 
    
    1.  Make sure you are in the same directory as the `values.yaml` file and run:
    
       ```bash
       helm install <release-name> -f values.yaml -n <namespace> .
       ```
       or
       
       ```
       helm repo add jans https://gluufederation.github.io/cloud-native-edition/pyjans/kubernetes/templates/helm
       helm repo update
       helm install <release-name> jans/jans -n <namespace> -f overide-values.yaml
       ```
    
    ### EKS helm notes
    
    #### Required changes to the `values.yaml`
    
      Inside the global `values.yaml` change the marked keys with `CHANGE-THIS`  to the appropriate values :
    
      ```yaml
      #global values to be used across charts
      global:
        provisioner: kubernetes.io/aws-ebs #CHANGE-THIS
        lbAddr: "" #CHANGE-THIS to the address received in the previous step axx-109xx52.us-west-2.elb.amazonaws.com
        domain: demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
        isDomainRegistered: "false" # CHANGE-THIS  "true" or "false" to specify if the domain above is registered or not.
    
      nginx:
        ingress:
          enabled: true
          path: /
          hosts:
            - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
          tls:
            - secretName: tls-certificate
              hosts:
                - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
      ```    
    
      Tweak the optional [parameters](#configuration) in `values.yaml` to fit the setup needed.
    
    ### GKE helm notes
    
    #### Required changes to the `values.yaml`
    
      Inside the global `values.yaml` change the marked keys with `CHANGE-THIS`  to the appopriate values :
    
      ```yaml
      #global values to be used across charts
      global:
        provisioner: kubernetes.io/gce-pd #CHANGE-THIS
        lbAddr: ""
        domain: demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
        # Networking configs
        lbIp: "" #CHANGE-THIS  to the IP received from the previous step
        isDomainRegistered: "false" # CHANGE-THIS  "true" or "false" to specify if the domain above is registered or not.
      nginx:
        ingress:
          enabled: true
          path: /
          hosts:
            - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
          tls:
            - secretName: tls-certificate
              hosts:
                - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
      ```
    
      Tweak the optional [parameters](#configuration) in `values.yaml` to fit the setup needed.
    
    ### Minikube helm notes
    
    #### Required changes to the `values.yaml`
    
      Inside the global `values.yaml` change the marked keys with `CHANGE-THIS`  to the appopriate values :
    
      ```yaml
      #global values to be used across charts
      global:
        provisioner: k8s.io/minikube-hostpath #CHANGE-THIS
        lbAddr: ""
        domain: demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
        lbIp: "" #CHANGE-THIS  to the IP of minikube <minikube ip>
    
      nginx:
        ingress:
          enabled: true
          path: /
          hosts:
            - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
          tls:
            - secretName: tls-certificate
              hosts:
                - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
      ```
    
      Tweak the optional [parameters](#configuration) in `values.yaml` to fit the setup needed.
    
    - Map janss FQDN at `/etc/hosts` file  to the minikube IP as shown below.
    
        ```bash
        ##
        # Host Database
        #
        # localhost is used to configure the loopback interface
        # when the system is booting.  Do not change this entry.
        ##
        192.168.99.100	demoexample.jans.io #minikube IP and example domain
        127.0.0.1	localhost
        255.255.255.255	broadcasthost
        ::1             localhost
        ```
    
    ### Microk8s helm notes
      
    #### Required changes to the `values.yaml`
    
      Inside the global `values.yaml` change the marked keys with `CHANGE-THIS`  to the appopriate values :
    
      ```yaml
      #global values to be used across charts
      global:
        provisioner: microk8s.io/hostpath #CHANGE-THIS
        lbAddr: ""
        domain: demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
        lbIp: "" #CHANGE-THIS  to the IP of the microk8s vm
    
      nginx:
        ingress:
          enabled: true
          path: /
          hosts:
            - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
          tls:
            - secretName: tls-certificate
              hosts:
                - demoexample.jans.io #CHANGE-THIS to the FQDN used for Janssen
      ```
    
      Tweak the optional [parameteres](#configuration) in `values.yaml` to fit the setup needed.
    
    - Map janss FQDN at `/etc/hosts` file  to the microk8s vm IP as shown below.
    
      ```bash
      ##
      # Host Database
      #
      # localhost is used to configure the loopback interface
      # when the system is booting.  Do not change this entry.
      ##
      192.168.99.100	demoexample.jans.io #microk8s IP and example domain
      127.0.0.1	localhost
      255.255.255.255	broadcasthost
      ::1             localhost
      ```
      
    ### Uninstalling the Chart
    
    To uninstall/delete `my-release` deployment:
    
    `helm delete <my-release>`
    
    If during installation the release was not defined, release name is checked by running `$ helm ls` then deleted using the previous command and the default release name.
    
    ### Configuration
    
    === "global"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |
        | `global.istio.ingress`                             | Enable use of Istio ingress  [Alpha]                                                                                             | `false`                             |
        | `global.istio.enabled`                             | Enable use of Istio in Janssen namespace. This will inject sidecars into Janssen pods.  [Alpha]                                        | `false`                             |     
        | `global.istio.namespace`                           | Istio namespace  [Alpha]                                                                                                         | `istio-system`                      |           
        | `global.upgrade.enabled`                           | Disables immutable objects  when set to true                                                                                     | `false`                             |           
        | `global.cloud.testEnviroment`                      | Test Environment doesn't include resources section in yaml                                                                       | `false`                             |
        | `global.storageClass.provisioner`                  | Which cloud provisioner to use when deploying                                                                                    | `k8s.io/minikube-hostpath`          |
        | `global.storageClass.parameters`                   | StorageClass parameters map which is used when using provisioners other than microk8s.io/hostpath, k8s.io/minikube-hostpath, kubernetes.io/aws-ebs, kubernetes.io/gce-pd, dobs.csi.digitalocean.com, openebs.io/local, kubernetes.io/azure-disk                                                        | ``                                  |
        | `global.storageClass.reclaimPolicy`                | [reclaimPolicy](https://kubernetes.io/docs/concepts/storage/storage-classes/#reclaim-policy)                                     | `Retain`                            |        
        | `global.storageClass.allowVolumeExpansion`         | [allowVolumeExpansion](https://kubernetes.io/docs/concepts/storage/storage-classes/#allow-volume-expansion)                      | `true`                              |
        | `global.storageClass.mountOptions`                 | [mountOptions](https://kubernetes.io/docs/concepts/storage/storage-classes/#mount-options)                                       | `[debug]`                           |        
        | `global.storageClass.volumeBindingMode`            | [volumeBindingMode](https://kubernetes.io/docs/concepts/storage/storage-classes/#volume-binding-mode)                            | `WaitForFirstConsumer`              |
        | `global.storageClass.allowedTopologies`            | [allowedTopologies](https://kubernetes.io/docs/concepts/storage/storage-classes/#allowed-topologies)                             | ``                                  |
        | `global.gcePdStorageType`                          | Google cloud engine storage class disk type                                                                                      | `pd-standard`                       |
        | `global.azureStorageAccountType`                   | Azure storage class disk type                                                                                                    | `Standard_LRS`                      |
        | `global.azureStorageKind`                          | Azure storage class kind                                                                                                         | `Managed`                           |
        | `global.lbIp`                                      | IP address to be used with a FQDN                                                                                                | `192.168.99.100` (for minikube)     |
        | `global.domain`                                    | DNS domain name                                                                                                                  | `demoexample.jans.io`              |
        | `global.isDomainRegistered`                        | Whether the domain to be used is registered or not                                                                               | `false`                             |
        | `global.jansPersistenceType`                       | Which database backend to use                                                                                                    | `ldap`                              |
        | `global.oxauth.enabled`                            | Enable oxAuth                                                                                                                    | `true`                              |
        | `global.fido2.enabled`                             | Enable Fido2                                                                                                                     | `false`                             |
        | `global.scim.enabled`                              | Enable SCIM                                                                                                                      | `false`                             |
        | `global.config.enabled`                            | Enable Config                                                                                                                    | `true`                              |
        | `global.jackrabbit.enabled`                        | Enable Jackrabbit                                                                                                                | `true`                              |
        | `global.persistence.enabled`                       | Enable Persistence                                                                                                               | `true`                              |
        | `global.oxtrust.enabled`                           | Enable oxTrust                                                                                                                   | `true`                              |
        | `global.opendj.enabled`                            | Enable Opendj                                                                                                                    | `true`                              |
        | `global.oxshibboleth.enabled`                      | Enable oxShibboleth                                                                                                              | `false`                             |
        | `global.oxd-server.enabled`                        | Enable oxd                                                                                                                       | `false`                             |
        | `global.nginx-ingress.enabled`                     | Enable Ingress nginx                                                                                                             | `true`                              |
        | `global.oxauth-key-rotation.enabled`               | Enable oxAuth Key Rotation                                                                                                       | `false`                             |
        | `global.cr-rotate.enabled`                         | Enable Cache Rotate                                                                                                              | `false`                             |
        
    === "config"   
    
        | Parameter                                             | Description                                                                                                                      | Default                                                     |
        | ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------- | 
        | `config.orgName`                                      | Organisation Name                                                                                                                | `Janssen`                                                      |
        | `config.email`                                        | Email address of the administrator usually. Used for certificate creation                                                        | `support@jans.io`                                          |
        | `config.adminPass`                                    | Admin password to log in to the UI                                                                                               | `P@ssw0rd`                                                  |
        | `config.ldapPass`                                     | Ldap admin password                                                                                                              | `P@ssw0rd`                                                  |
        | `config.redisPass`                                    | Redis password                                                                                                                   | `P@ssw0rd`                                                  |
        | `config.countryCode`                                  | Country code of where the Org is located                                                                                         | `US`                                                        |
        | `config.state`                                        | State                                                                                                                            | `TX`                                                        |
        | `config.city`                                         | City                                                                                                                             | `Austin`                                                    |
        | `config.configmap.jansOxdApplicationCertCn`           | oxd OAuth client application certificate common name                                                                             | `oxd-server`                                                |
        | `config.configmap.jansOxdAdminCertCn`                 | oxd OAuth client admin certificate common name                                                                                   | `oxd-server`                                                |
        | `config.configmap.jansCouchbaseCrt`                   | Couchbase certificate authority                                                                                                  | `LS0tLS1CRUdJTiBDRVJ.....`                                  |
        | `config.configmap.jansCouchbaseBucketPrefix`          | Prefix for Couchbase buckets                                                                                                     | `jans`                                                      |
        | `config.configmap.jansCouchbasePass`                  | Couchbase password                                                                                                               | `P@ssw0rd`                                                  |
        | `config.configmap.jansCouchbaseSuperUserPass`         | Couchbase superuser password                                                                                                     | `P@ssw0rd`                                                  |        
        | `config.configmap.jansCouchbaseUrl`                   | Couchbase URL. Used only when `global.jansPersistenceType` is `hybrid` or `couchbase`                                            | `cbjans.cbns.svc.cluster.local`                             |
        | `config.configmap.jansCouchbaseUser`                  | Couchbase user. Used only when `global.jansPersistenceType` is `hybrid` or `couchbase`                                           | `jans`                                                      |
        | `config.configmap.jansCouchbaseSuperUser`             | Couchbase superuser. Used only when `global.jansPersistenceType` is `hybrid` or `couchbase`                                      | `admin`                                                     |        
        | `config.configmap.jansCouchbasePassFile`              | Location of `couchbase_password` file                                                                                            | `/etc/jans/conf/couchbase_password`                         |
        | `config.configmap.jansCouchbaseSuperUserPassFile`     | Location of `couchbase_superuser_password` file                                                                                  | `/etc/jans/conf/couchbase_superuser_password`               |        
        | `config.configmap.jansCouchbaseCertFile`              | Location of `couchbase.crt` used by cb for tls termination                                                                       | `/etc/jans/conf/couchbase.crt`                              |     
        | `config.configmap.jansPersistenceLdapMapping`         | if jansPersistenceType is hybrid, what to store in ldap.                                                                         | `default`, `user`, `site`, `cache`, `statistic`             |     
        | `config.configmap.jansCacheType`                      | if jansCacheType is hybrid, what to store in ldap.                                                                               | `REDIS`, `NATIVE_PERSISTENCE`, `IN_MEMORY`                  |     
        | `config.configmap.jansSyncShibManifests`              | Sync Shibboleth files.                                                                                                           | `false`                                                     |     
        | `config.configmap.jansSyncCasaManifests`              | Sync Casa files.                                                                                                                 | `false`                                                     |     
        | `config.configmap.jansMaxRamPercent`                  | Used in conjunction with pod memory limitations to identify the percentage of the maximum amount of heap memory                  | `false`                                                     |     
        | `config.configmap.configAdapterName`                  | Configuration adapter.                                                                                                           | `kubernetes`                                                |     
        | `config.configmap.containerMetadataName`              | The name of scheduler to pull container metadata                                                                                 | `kubernetes`                                                |     
        | `config.configmap.configSecretAdapter`                | Secret adapter                                                                                                                   | `kubernetes`                                                |     
        | `config.configmap.jansRedisUrl`                       | Redis url with port. Used when Redis is deployed for Cache                                                                       | `redis:6379`                                                |     
        | `config.configmap.jansRedisUseSsl`                    | Redis SSL use                                                                                                                    | `"false"` or `"true"`                                       |
        | `config.configmap.jansRedisType`                      | Type of Redis deployed.                                                                                                          | `"SHARDED"`, `"STANDALONE"`, `"CLUSTER"`, or `"SENTINEL"`   |
        | `config.configmap.jansRedisSslTruststore`             | Redis SSL truststore. If using cloud provider services this is left empty.                                                       | ``                                                          |
        | `config.configmap.jansRedisSentinelGroup`             | Redis Sentinel group                                                                                                             | ``                                                          |
        | `config.configmap.jansOxtrustBackend`                 | oxTrust backend address                                                                                                          | `oxtrust:8080`                                              |
        | `config.configmap.jansOxauthBackend`                  | oxAuth backend address                                                                                                           | `oxauth:8080`                                               |
        | `config.configmap.jansOxdServerUrl`                   | oxd Oauth client address                                                                                                         | `oxd-server:8443`                                           |
        | `config.configmap.jansLdapUrl`                        | opendj server url. Port and service name of opendj server - should not be changed                                           |  `opendj:1636`                                              |
        | `config.configmap.jansJackrabbitSyncInterval`         | Jackrabbit sync interval                                                                                                         |  `300`                                                      |
        | `config.configmap.jansJackrabbitUrl`                  | Jackrabbit url. Port and service name of Jackrabbit                                                                              |  `jackrabbit:8080`                                          |
        | `config.configmap.jansJackrabbitAdminId`              | Jackrabbit admin user                                                                                                            |  i.e `admin`                                                |
        | `config.configmap.jansJackrabbitAdminPassFile`        | Jackrabbit admin password file location                                                                                          |  `/etc/jans/conf/jackrabbit_admin_password`                 |
        | `config.configmap.jansJackrabbitPostgresUser`         | Jackrabbit postgres user                                                                                                         |  i.e `admin`                                                |
        | `config.configmap.jansJackrabbitPostgresPasswordFile` | Jackrabbit postgres password file location                                                                                       |  `/etc/jans/conf/jackrabbit_admin_password`                 |
        | `config.configmap.jansJackrabbitPostgresDatabaseName` | Jackrabbit postgres database name                                                                                                |  i.e `jackrabbbit`                                          |
        | `config.configmap.jansJackrabbitPostgresHost`         | Jackrabbit postgres host                                                                                                         |  i.e `postgres.postgres.svc.cluster.local`                  |
        | `config.configmap.jansJackrabbitPostgresPort`         | Jackrabbit postgres port                                                                                                         |  `5432`                                                     |
        | `config.configmap.jansJackrabbitSyncInterval`         | Interval between files sync in seconds                                                                                           |  `300`                                                      |
        | `config.configmap.jansDocumentStoreType`              | Jackrabbit document store type                                                                                                   |  `LOCAL`, `JCA`                                             |
        | `config.configmap.lbAddr`                             | Address of LB or nginx                                                                                                           |  i.e `axx-109xx52.us-west-2.elb.amazonaws.com`              |
        | `config.configmap.ldapServiceName`                    | ldap service name. Used to connect other services to ldap                                                                        | `opendj`                                                    |
        | `config.configmap.jansOxtrustApiEnabled`              | Enable oxTrust API                                                                                                               | `false`                                                     |
        | `config.configmap.jansOxtrustApiTestMode`             | Enable oxTrust API Test mode                                                                                                     | `false`                                                     |
        | `config.configmap.jansPassportEnabled`                | Auto install passport service chart                                                                                              | `false`                                                     |
        | `config.configmap.jansCasaEnabled`                    | Enable Casa                                                                                                                      | `false`                                                     |
        | `config.configmap.jansRadiusEnabled`                  | Enable Radius                                                                                                                    | `false`                                                     |
        | `config.configmap.jansSamlEnabled`                    | Enable SAML                                                                                                                      | `false`                                                     |
        | `config.image.repository`                             | Config image repository                                                                                                          | `gluufederation/config-init`                                |
        | `config.image.tag`                                    | Config image tag                                                                                                                 | `4.2.2_02`                                                  |

    === "nginx-ingress"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |     
        | `nginx-ingress.ingress.enabled`                    | Enable ingress                                                                                                                   | `true`                              |
        | `nginx-ingress.ingress.path`                       | Main path in ingress                                                                                                             | `/`                                 |
        | `nginx-ingress.ingress.hosts`                      | Host holding FQDN for jans                                                                                                       | `[demoexample.jans.io]`            |
        | `nginx-ingress.ingress.tls[0].secretName`          | Secret name of TLS certificate. This shouldn't be changed.                                                                       | `tls-certificate`                   |
        | `nginx-ingress.ingress.tls[0].hosts`               | Host holding FQDN for jans                                                                                                       | `demoexample.jans.io`              |

    === "jackrabbit"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |     
        | `jackrabbit.service.jackRabbitServiceName`         | Name of jackrabbit service                                                                                                       | `jackrabbit`                        |
        | `jackrabbit.replicas`                              | Jackrabbit replicas                                                                                                              | `1`                                 |
        | `jackrabbit.storage.size`                          | Storage for Jackrabbit pod                                                                                                       | `5Gi`                               |
        | `jackrabbit.image.repository`                      | Jackrabbit image repository                                                                                                      | `gluufederation/jackrabbit`         |
        | `jackrabbit.image.tag`                             | Jackrabbit image tag repository                                                                                                  | `4.2.2_02`                          |
        | `jackrabbit.image.pullPolicy`                      | Jackrabbit image pull policy                                                                                                     | `Always`                            |
        | `jackrabbit.resources.limits.cpu`                  | Jackrabbit memory limit                                                                                                          | `1000Mi`                            |
        | `jackrabbit.resources.limits.memory`               | Jackrabbit cpu limit                                                                                                             | `1500m`                             |
        | `jackrabbit.resources.requests.cpu`                | Jackrabbit memory request                                                                                                        | `1000Mi`                            |
        | `jackrabbit.resources.requests.memory`             | Jackrabbit cpu request                                                                                                           | `1500m`                             |

    === "opendj"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |
        | `opendj.multiCluster.enabled`                      | HELM-ALPHA-FEATURE: Enable ldap multi cluster. One pod per k8 cluster only allowed currently.                                    | `opendj`                            |
        | `opendj.multiCluster.serfAdvertiseAddr`            | HELM-ALPHA-FEATURE: Addvertised opendj pod address. This must be resolvable                                                      | `demoexample.jans.io:31946`        |
        | `opendj.multiCluster.serfKey`                      | HELM-ALPHA-FEATURE: The key size must be a 16, 24, or 32 bytes encoded as base64 string.                                         | `Z51b6PgKU1MZ75NCZOTGGoc0LP2OF3qvF6sjxHyQCYk=`        |
        | `opendj.multiCluster.serfPeers`                    | HELM-ALPHA-FEATURE: All opendj serf advertised addresses. This must be resolvable                                                | `["firstldap.jans.org:30946", "secondldap.jans.org:31946"]`        |
        | `opendj.service.ldapServiceName`                   | Name of opendj service                                                                                                           | `opendj`                            |
        | `opendj.replicas`                                  | Opendj replicas                                                                                                                  | `1`                                 |
        | `opendj.persistence.size`                          | Storage for OpenDJ pod                                                                                                           | `5Gi`                               |
        | `opendj.image.repository`                          | Opendj image repository                                                                                                          | `gluufederation/opendj`             |
        | `opendj.image.tag`                                 | Opendj image tag repository                                                                                                      | `4.2.2_02`                          |
        | `opendj.image.pullPolicy`                          | Opendj image pull policy                                                                                                         | `Always`                            |
        | `opendj.resources.limits.cpu`                      | Opendj memory limit                                                                                                              | `2000Mi`                            |
        | `opendj.resources.limits.memory`                   | Opendj cpu limit                                                                                                                 | `1500m`                             |
        | `opendj.resources.requests.cpu`                    | Opendj memory request                                                                                                            | `2000Mi`                            |
        | `opendj.resources.requests.memory`                 | Opendj cpu request                                                                                                               | `1500m`                             |    
        | `opendj.ports.tcp-ldaps.port`                      | Opendj ldaps port                                                                                                                | `1636`                              |
        | `opendj.ports.tcp-ldaps.targetPort`                | Opendj ldaps target port                                                                                                         | `1636`                              |    
        | `opendj.ports.tcp-ldaps.protocol`                  | Opendj ldaps protocol                                                                                                            | `TCP`                               |    
        | `opendj.ports.tcp-ldaps.nodePort`                  | Opendj ldaps node port. Used in ldap multi cluster                                                                               | `""`                                |    
        | `opendj.ports.tcp-ldap.port`                       | Opendj ldap port                                                                                                                 | `1389`                              |
        | `opendj.ports.tcp-ldap.targetPort`                 | Opendj ldap target port                                                                                                          | `1389`                              |    
        | `opendj.ports.tcp-ldap.protocol`                   | Opendj ldap protocol                                                                                                             | `TCP`                               |    
        | `opendj.ports.tcp-ldap.nodePort`                   | Opendj ldap node port. Used in ldap multi cluster                                                                                | `""`                                |    
        | `opendj.ports.tcp-repl.port`                       | Opendj replication port                                                                                                          | `8989`                              |
        | `opendj.ports.tcp-repl.targetPort`                 | Opendj replication target port                                                                                                   | `8989`                              |    
        | `opendj.ports.tcp-repl.protocol`                   | Opendj replication protocol                                                                                                      | `TCP`                               |    
        | `opendj.ports.tcp-repl.nodePort`                   | Opendj replication node port. Used in ldap multi cluster                                                                         | `""`                                |    
        | `opendj.ports.tcp-admin.port`                      | Opendj admin port                                                                                                                | `4444`                              |
        | `opendj.ports.tcp-admin.targetPort`                | Opendj admin target port                                                                                                         | `4444`                              |    
        | `opendj.ports.tcp-admin.protocol`                  | Opendj admin protocol                                                                                                            | `TCP`                               |    
        | `opendj.ports.tcp-admin.nodePort`                  | Opendj admin node port. Used in ldap multi cluster                                                                               | `""`                                |    
        | `opendj.ports.tcp-serf.port`                       | Opendj serf port                                                                                                                 | `7946`                              |
        | `opendj.ports.tcp-serf.targetPort`                 | Opendj serf target port                                                                                                          | `7946`                              |    
        | `opendj.ports.tcp-serf.protocol`                   | Opendj serf protocol                                                                                                             | `TCP`                               |    
        | `opendj.ports.tcp-serf.nodePort`                   | Opendj serf node port. Used in ldap multi cluster                                                                                | `""`                                |    
        | `opendj.ports.udp-serf.port`                       | Opendj serf port                                                                                                                 | `7946`                              |
        | `opendj.ports.udp-serf.targetPort`                 | Opendj serf target port                                                                                                          | `7946`                              |    
        | `opendj.ports.udp-serf.protocol`                   | Opendj serf protocol                                                                                                             | `UDP`                               |    
        | `opendj.ports.udp-serf.nodePort`                   | Opendj serf node port. Used in ldap multi cluster                                                                                | `""`                                |    
                            
    === "persistence"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
        | `persistence.image.repository`                     | Persistence image repository                                                                                                     | `gluufederation/persistence`        |
        | `persistence.image.tag`                            | Persistence image tag repository                                                                                                 | `4.2.2_02`                          |
        | `persistence.image.pullPolicy`                     | Persistence image pull policy                                                                                                    | `Always`                            |
        
    === "oxauth"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
        | `oxauth.service.oxAuthServiceName`                 | Name of oxAuth service                                                                                                           | `oxauth`                            |
        | `oxauth.replicas`                                  | oxAuth replicas                                                                                                                  | `1`                                 |
        | `oxauth.image.repository`                          | oxAuth image repository                                                                                                          | `gluufederation/oxauth`             |
        | `oxauth.image.tag`                                 | oxAuth image tag repository                                                                                                      | `4.2.2_03`                          |
        | `oxauth.image.pullPolicy`                          | oxAuth image pull policy                                                                                                         | `Always`                            |
        | `oxauth.resources.limits.cpu`                      | oxAuth memory limit                                                                                                              | `2500Mi`                            |
        | `oxauth.resources.limits.memory`                   | oxAuth cpu limit                                                                                                                 | `2500m`                             |
        | `oxauth.resources.requests.cpu`                    | oxAuth memory request                                                                                                            | `2500Mi`                            |
        | `oxauth.resources.requests.memory`                 | oxAuth cpu request                                                                                                               | `2500m`                             |
        
    === "oxtrust"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
        | `oxtrust.service.oxTrustServiceName`               | Name of oxTrust service                                                                                                          | `oxtrust`                           |
        | `oxtrust.replicas`                                 | oxTrust replicas                                                                                                                 | `1`                                 |
        | `oxtrust.image.repository`                         | oxTrust image repository                                                                                                         | `gluufederation/oxtrust`            |
        | `oxtrust.image.tag`                                | oxTrust image tag repository                                                                                                     | `4.2.2_02`                          |
        | `oxtrust.image.pullPolicy`                         | oxTrust image pull policy                                                                                                        | `Always`                            |
        | `oxtrust.resources.limits.cpu`                     | oxTrust memory limit                                                                                                             | `500Mi`                             |
        | `oxtrust.resources.limits.memory`                  | oxTrust cpu limit                                                                                                                | `500m`                              |
        | `oxtrust.resources.requests.cpu`                   | oxTrust memory request                                                                                                           | `500Mi`                             |
        | `oxtrust.resources.requests.memory`                | oxTrust cpu request                                                                                                              | `500m`                              |
      
    === "fido2"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |              
        | `fido2.service.fido2ServiceName`                   | Name of fido2 service                                                                                                            | `fido2`                             |
        | `fido2.replicas`                                   | Fido2 replicas                                                                                                                   | `1`                                 |
        | `fido2.image.repository`                           | Fido2 image repository                                                                                                           | `gluufederation/fido2`              |
        | `fido2.image.tag`                                  | Fido2 image tag repository                                                                                                       | `4.2.2_02`                          |
        | `fido2.image.pullPolicy`                           | Fido2 image pull policy                                                                                                          | `Always`                            |
        | `fido2.resources.limits.cpu`                       | Fido2 memory limit                                                                                                               | `500Mi`                             |
        | `fido2.resources.limits.memory`                    | Fido2 cpu limit                                                                                                                  | `500m`                              |
        | `fido2.resources.requests.cpu`                     | Fido2 memory request                                                                                                             | `500Mi`                             |
        | `fido2.resources.requests.memory`                  | Fido2 cpu request                                                                                                                | `500m`                              |
        
    === "scim"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
        | `scim.service.scimServiceName`                     | Name of SCIM service                                                                                                             | `scim`                              |
        | `scim.replicas`                                    | SCIM replicas                                                                                                                    | `1`                                 |
        | `scim.image.repository`                            | SCIM image repository                                                                                                            | `gluufederation/scim`               |
        | `scim.image.tag`                                   | SCIM image tag repository                                                                                                        | `4.2.2_02`                          |
        | `scim.image.pullPolicy`                            | SCIM image pull policy                                                                                                           | `Always`                            |
        | `scim.resources.limits.cpu`                        | SCIM memory limit                                                                                                                | `500Mi`                             |
        | `scim.resources.limits.memory`                     | SCIM cpu limit                                                                                                                   | `500m`                              |
        | `scim.resources.requests.cpu`                      | SCIM memory request                                                                                                              | `500Mi`                             |
        | `scim.resources.requests.memory`                   | SCIM cpu request                                                                                                                 | `500m`                              |     
        
    === "oxd-server"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
        | `oxd-server.service.oxdServerServiceName`          | Name of oxd Oauth client service                                                                                                 | `oxd-server`                        |
        | `oxd-server.replicas`                              | oxd Oauth client replicas                                                                                                        | `1`                                 |
        | `oxd-server.image.repository`                      | oxd Oauth client image repository                                                                                                | `gluufederation/oxd-server`         |
        | `oxd-server.image.tag`                             | oxd Oauth client image tag repository                                                                                            | `4.2.2_02`                          |
        | `oxd-server.image.pullPolicy`                      | oxd Oauth client image pull policy                                                                                               | `Always`                            |
        | `oxd-server.resources.limits.cpu`                  | oxd Oauth client memory limit                                                                                                    | `400Mi`                             |
        | `oxd-server.resources.limits.memory`               | oxd Oauth client cpu limit                                                                                                       | `1000m`                             |
        | `oxd-server.resources.requests.cpu`                | oxd Oauth client memory request                                                                                                  | `400Mi`                             |
        | `oxd-server.resources.requests.memory`             | oxd Oauth client cpu request                                                                                                     | `1000m`                             |     
        
    === "casa"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |              
        | `casa.service.casaServiceName`                     | Name of casa service                                                                                                             | `casa`                              |
        | `casa.replicas`                                    | oxd Oauth client replicas                                                                                                        | `1`                                 |
        | `casa.image.repository`                            | Casa image repository                                                                                                            | `gluufederation/casa`               |
        | `casa.image.tag`                                   | Casa image tag repository                                                                                                        | `4.2.2_02`                          |
        | `casa.image.pullPolicy`                            | Casa image pull policy                                                                                                           | `Always`                            |
        | `casa.resources.requests.limits.cpu`               | Casa memory limit                                                                                                                | `500Mi`                             |
        | `casa.resources.requests.limits.memory`            | Casa cpu limit                                                                                                                   | `500m`                              |
        | `casa.resources.requests.cpu`                      | Casa memory request                                                                                                              | `500Mi`                             |
        | `casa.resources.requests.memory`                   | Casa cpu request                                                                                                                 | `500m`                              |
        
    === "oxpassport"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |              
        | `oxpassport.service.oxPassportServiceName`         | Name of oxPassport service                                                                                                       | `oxpassport`                        |
        | `oxpassport.replicas`                              | oxPassport replicas                                                                                                              | `1`                                 |
        | `oxpassport.image.repository`                      | oxPassport image repository                                                                                                      | `gluufederation/oxpassport`         |
        | `oxpassport.image.tag`                             | oxPassport image tag repository                                                                                                  | `4.2.2_02`                          |
        | `oxpassport.image.pullPolicy`                      | oxPassport image pull policy                                                                                                     | `Always`                            |
        | `oxpassport.resources.requests.limits.cpu`         | oxPassport memory limit                                                                                                          | `700Mi`                             |
        | `oxpassport.resources.requests.limits.memory`      | oxPassport cpu limit                                                                                                             | `500m`                              |
        | `oxpassport.resources.requests.cpu`                | oxPassport memory request                                                                                                        | `700Mi`                             |
        | `oxpassport.resources.requests.memory`             | oxPassport cpu request                                                                                                           | `500m`                              |
        
    === "oxshibboleth"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
        | `oxshibboleth.service.oxShibbolethServiceName`     | Name of oxShibboleth service                                                                                                     | `oxshibboleth`                      |
        | `oxshibboleth.replicas`                            | oxShibboleth replicas                                                                                                            | `1`                                 |
        | `oxshibboleth.image.repository`                    | oxShibboleth image repository                                                                                                    | `gluufederation/oxshibboleth`       |
        | `oxshibboleth.image.tag`                           | oxShibboleth image tag repository                                                                                                | `4.2.2_02`                          |
        | `oxshibboleth.image.pullPolicy`                    | oxShibboleth image pull policy                                                                                                   | `Always`                            |
        | `oxshibboleth.resources.requests.limits.cpu`       | oxShibboleth memory limit                                                                                                        | `500Mi`                             |
        | `oxshibboleth.resources.requests.limits.memory`    | oxShibboleth cpu limit                                                                                                           | `500m`                              |
        | `oxshibboleth.resources.requests.cpu`              | oxShibboleth memory request                                                                                                      | `500Mi`                             |
        | `oxshibboleth.resources.requests.memory`           | oxShibboleth cpu request                                                                                                         | `500m`                              |     
    
    === "radius"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |          
        | `radius.service.radiusServiceName`                 | Name of Radius service                                                                                                           | `radius`                            |
        | `radius.replicas`                                  | Radius replicas                                                                                                                  | `1`                                 |
        | `radius.image.repository`                          | Radius image repository                                                                                                          | `gluufederation/radius`             |
        | `radius.image.tag`                                 | Radius image tag repository                                                                                                      | `4.2.2_02`                          |
        | `radius.image.pullPolicy`                          | Radius image pull policy                                                                                                         | `Always`                            |
        | `radius.resources.requests.limits.cpu`             | Radius memory limit                                                                                                              | `700Mi`                             |
        | `radius.resources.requests.limits.memory`          | Radius cpu limit                                                                                                                 | `700m`                              |
        | `radius.resources.requests.cpu`                    | Radius memory request                                                                                                            | `700Mi`                             |
        | `radius.resources.requests.memory`                 | Radius cpu request                                                                                                               | `700m`                              |     
        
    === "cr-rotate"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |              
        | `cr-rotate.service.crRotateServiceName`            | Name of Cache Refresh Rotate service                                                                                             | `cr-rotate`                         |
        | `cr-rotate.replicas`                               | Cache Refresh replicas                                                                                                           | `1`                                 |
        | `cr-rotate.image.repository`                       | Cache Refresh image repository                                                                                                   | `gluufederation/cr-rotate`          |
        | `cr-rotate.image.tag`                              | Cache Refresh image tag repository                                                                                               | `4.2.2_02`                          |
        | `cr-rotate.image.pullPolicy`                       | Cache Refresh image pull policy                                                                                                  | `Always`                            |
        | `cr-rotate.resources.requests.limits.cpu`          | Cache Refresh memory limit                                                                                                       | `200Mi`                             |
        | `cr-rotate.resources.requests.limits.memory`       | Cache Refresh cpu limit                                                                                                          | `200m`                              |
        | `cr-rotate.resources.requests.cpu`                 | Cache Refresh memory request                                                                                                     | `200Mi`                             |
        | `cr-rotate.resources.requests.memory`              | Cache Refresh cpu request                                                                                                        | `200m`                              |     
       
    === "oxauth-key-rotation"
    
        | Parameter                                          | Description                                                                                                                      | Default                             |
        | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------- |              
        | `oxauth-key-rotation.keysLife`                          | oxAuth Key Rotation Keys life in hours                                                                                      | `48`                                |
        | `oxauth-key-rotation.image.repository`                  | oxAuth Key Rotation image repository                                                                                        | `gluufederation/certmanager`        |
        | `oxauth-key-rotation.image.tag`                         | oxAuth Key Rotation image tag repository                                                                                    | `4.2.2_02`                          |
        | `oxauth-key-rotation.image.pullPolicy`                  | oxAuth Key Rotation image pull policy                                                                                       | `Always`                            |
        | `oxauth-key-rotation.resources.requests.limits.cpu`     | oxAuth Key Rotation memory limit                                                                                            | `300Mi`                             |
        | `oxauth-key-rotation.resources.requests.limits.memory`  | oxAuth Key Rotation cpu limit                                                                                               | `300m`                              |
        | `oxauth-key-rotation.resources.requests.cpu`            | oxAuth Key Rotation memory request                                                                                          | `300Mi`                             |
        | `oxauth-key-rotation.resources.requests.memory`         | oxAuth Key Rotation cpu request                                                                                             | `300m`                              |     

    
    ### Instructions on how to install different services
    
    Enabling the following services automatically install the corresponding associated chart. To enable/disable them set `true` or `false` in the persistence configs as shown below.  
    
    ```yaml
    config:
      configmap:
        # Auto install other services. If enabled the respective service chart will be installed
        jansPassportEnabled: false
        jansCasaEnabled: false
        jansRadiusEnabled: false
        jansSamlEnabled: false
    ```
    
    ### Casa
    
    - Casa is dependant on `oxd-server`. To install it `oxd-server` must be enabled.
    
    ### Other optional services
    
    Other optional services like `key-rotation`, `cr-rotation`, and `radius` are enabled by setting their corresponding values to `true` under the global block.
    
    For example, to enable `cr-rotate` set
    
    ```yaml
    global:
      cr-rotate:
        enabled: true
    ```
    
=== "GUI-alpha"
    ## Install Janssen using the gui installer
    
    !!!warning
        The GUI installer is currently alpha. Please report any bugs found by opening an [issue](https://github.com/JanssenFederation/cloud-native-edition/issues/new/choose).
        
    1.  Create the GUI installer job
    
        ```bash
        cat <<EOF | kubectl apply -f -
        apiVersion: batch/v1
        kind: Job
        metadata:
          name: cloud-native-installer
          labels:
            APP_NAME: cloud-native-installer
        spec:
          template:
            metadata:
              labels:
                APP_NAME: cloud-native-installer
            spec:
              restartPolicy: Never
              containers:
                - name: cloud-native-installer
                  image: gluufederation/cloud-native:4.2.1_a4
        ---
        kind: Service
        apiVersion: v1
        metadata:
          name: cloud-native-installer
        spec:
          type: LoadBalancer
          selector:
            app: cloud-native-installer
          ports:
            - name: http
              port: 80
              targetPort: 5000           
        EOF
        ```
    
    1.  Grab the Loadbalancer address , ip or Nodeport and follow installation setup.
    
        === "AWS"
        
            ```bash
            kubectl -n default get svc cloud-native-installer --output jsonpath='{.status.loadBalancer.ingress[0].hostname}'
            ```
            
        === "GKE"
        
            ```bash
            kubectl -n default get svc cloud-native-installer --output jsonpath='{.status.loadBalancer.ingress[0].ip}'
            ```
            
        === "Azure"
        
            ```bash
            kubectl -n default get svc cloud-native-installer --output jsonpath='{.status.loadBalancer.ingress[0].ip}'
            ```
            
        === "DigitalOcean"
        
            ```bash
            kubectl -n default get svc cloud-native-installer --output jsonpath='{.status.loadBalancer.ingress[0].ip}'
            ```
            
        === "Microk8s"
        
            1. Get ip of microk8s vm
            
            1. Get `NodePort` of the GUI installer service
            
               ```bash
               kubectl -n default get svc cloud-native-installer
               ```
            
        === "Minikube"
        
            1. Get ip of minikube vm
            
               ```bash
               minikube ip
               ```
            
            1. Get `NodePort` of the GUI installer service
            
               ```bash
               kubectl -n default get svc cloud-native-installer
               ```
                
    1. Head to the address from previous step to start the installation.

    

### `settings.json` parameters file contents

This is the main parameter file used with the [`pyjans-kubernetes.pyz`](https://github.com/JanssenFederation/cloud-native-edition/releases) cloud native edition installer.

!!!note
    Please generate this file using [`pyjans-kubernetes.pyz generate-settings`](https://github.com/JanssenFederation/cloud-native-edition/releases).

| Parameter                                       | Description                                                                      | Options                                                                                     |
| ----------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `ACCEPT_JANS_LICENSE`                           | Accept the [License](https://jans.io/license/cloud-native-edition/)         | `"Y"` or `"N"`                                                                              |
| `TEST_ENVIRONMENT`                              | Allows installation with no resources limits and requests defined.               | `"Y"` or `"N"`                                                                              |
| `ADMIN_PW`                                      | Password of oxTrust 6 chars min: 1 capital, 1 small, 1 digit and 1 special char  | `"P@ssw0rd"`                                                                                |
| `JANS_VERSION`                                  | Janssen version to be installed                                                     | `"4.2"`                                                                                     |
| `JANS_UPGRADE_TARGET_VERSION`                   | Janssen upgrade version                                                             | `"4.2"`                                                                                     |
| `JANS_HELM_RELEASE_NAME`                        | Janssen Helm release name                                                           | `"<name>"`                                                                                  |
| `KONG_HELM_RELEASE_NAME`                        | Janssen Gateway (Kong) Helm release name                                            | `"<name>"`                                                                                  |
| `NGINX_INGRESS_NAMESPACE`                       | Nginx namespace                                                                  | `"<name>"`                                                                                  |
| `NGINX_INGRESS_RELEASE_NAME`                    | Nginx Helm release name                                                          | `"<name>"`                                                                                  |
| `JANS_GATEWAY_UI_HELM_RELEASE_NAME`             |  Janssen Gateway UI release name                                                    | `"<name>"`                                                                                  |
| `INSTALL_JANS_GATEWAY`                          | Install Janssen Gateway Database mode                                               | `"Y"` or `"N"`                                                                              |
| `USE_ISTIO`                                     | Enable use of Istio. This will inject sidecars in Janssen pods.[Alpha]              | `"Y"` or `"N"`                                                                              |
| `USE_ISTIO_INGRESS`                             | Enable Istio ingress.[Alpha]                                                     | `"Y"` or `"N"`                                                                              |
| `ISTIO_SYSTEM_NAMESPACE`                        | Postgres namespace - Janssen Gateway [Alpha]                                        | `"<name>"`                                                                                  |
| `POSTGRES_NAMESPACE`                            | Postgres namespace - Janssen Gateway                                                | `"<name>"`                                                                                  |
| `KONG_NAMESPACE`                                | Kong namespace - Janssen Gateway                                                    | `"<name>"`                                                                                  |
| `JANS_GATEWAY_UI_NAMESPACE`                     | Janssen Gateway UI namespace - Janssen Gateway                                         | `"<name>"`                                                                                  |
| `KONG_PG_USER`                                  | Kong Postgres user - Janssen Gateway                                                | `"<name>"`                                                                                  |
| `KONG_PG_PASSWORD`                              | Kong Postgres password - Janssen Gateway                                            | `"<name>"`                                                                                  |
| `JANS_GATEWAY_UI_PG_USER`                       | Janssen Gateway UI Postgres user - Janssen Gateway                                     | `"<name>"`                                                                                  |
| `JANS_GATEWAY_UI_PG_PASSWORD`                   | Janssen Gateway UI Postgres password - Janssen Gateway                                 | `"<name>"`                                                                                  |
| `KONG_DATABASE`                                 | Kong Postgres Database name - Janssen Gateway                                       | `"<name>"`                                                                                  |
| `JANS_GATEWAY_UI_DATABASE`                      | Janssen Gateway UI Postgres Database name - Janssen Gateway                            | `"<name>"`                                                                                  |
| `POSTGRES_REPLICAS`                             | Postgres number of replicas - Janssen Gateway                                       | `"<name>"`                                                                                  |
| `POSTGRES_URL`                                  | Postgres URL ( Can be local or remote) - Janssen Gateway                            |  i.e `"<servicename>.<namespace>.svc.cluster.local"`                                        |
| `NODES_IPS`                                     | List of kubernetes cluster node ips                                              | `["<ip>", "<ip2>", "<ip3>"]`                                                                |
| `NODES_ZONES`                                   | List of kubernetes cluster node zones                                            | `["<node1_zone>", "<node2_zone>", "<node3_zone>"]`                                          |
| `NODES_NAMES`                                   | List of kubernetes cluster node names                                            | `["<node1_name>", "<node2_name>", "<node3_name>"]`                                          |
| `NODE_SSH_KEY`                                  | nodes ssh key path location                                                      | `"<pathtosshkey>"`                                                                          |
| `HOST_EXT_IP`                                   | Minikube or Microk8s vm ip                                                       | `"<ip>"`                                                                                    |
| `VERIFY_EXT_IP`                                 | Verify the Minikube or Microk8s vm ip placed                                     | `"Y"` or `"N"`                                                                              |
| `AWS_LB_TYPE`                                   | AWS loadbalancer type                                                            | `""` , `"clb"` or `"nlb"`                                                                   |
| `USE_ARN`                                       | Use ssl provided from ACM AWS                                                    | `""`, `"Y"` or `"N"`                                                                        |
| `VPC_CIDR`                                      | VPC CIDR in use for the Kubernetes cluster                                       | `""`, i.e `192.168.1.116`                                                                   |
| `ARN_AWS_IAM`                                   | The arn string                                                                   | `""` or `"<arn:aws:acm:us-west-2:XXXXXXXX:certificate/XXXXXX-XXXXXXX-XXXXXXX-XXXXXXXX>"`    |
| `LB_ADD`                                        | AWS loadbalancer address                                                         | `"<loadbalancer_address>"`                                                                  |
| `DEPLOYMENT_ARCH`                               | Deployment architecture                                                          | `"microk8s"`, `"minikube"`, `"eks"`, `"gke"`, `"aks"`, `"do"` or `"local"`                  |
| `PERSISTENCE_BACKEND`                           | Backend persistence type                                                         | `"ldap"`, `"couchbase"` or `"hybrid"`                                                       |
| `REDIS_URL`                                     | Redis url with port. Used when Redis is deployed for Cache.                      | i.e `"redis:6379"`, `"clustercfg.testing-redis.icrbdv.euc1.cache.amazonaws.com:6379"`       |
| `REDIS_TYPE`                                    | Type of Redis deployed                                                           | `"SHARDED"`, `"STANDALONE"`, `"CLUSTER"`, or `"SENTINEL"`                                   |
| `REDIS_PW`                                      | Redis Password if used. This may be empty. If not choose a long password.        | i.e `""`, `"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURUakNDQWphZ0F3SUJBZ0lVV2Y0TExEb"`     |
| `REDIS_USE_SSL`                                 | Redis SSL use                                                                    |  `"false"` or `"true"`                                                                      |
| `REDIS_SSL_TRUSTSTORE`                          | Redis SSL truststore. If using cloud provider services this is left empty.       | i.e `""`, `"/etc/myredis.pem"`                                                              |
| `REDIS_SENTINEL_GROUP`                          | Redis Sentinel group                                                             | i.e `""`                                                                                    |
| `REDIS_MASTER_NODES`                            | Number of Redis master node if Redis is to be installed                          | i.e `3`                                                                                     |
| `REDIS_NODES_PER_MASTER`                        | Number of nodes per Redis master node if Redis is to be installed                | i.e `2`                                                                                     |
| `REDIS_NAMESPACE`                               | Redis Namespace if Redis is to be installed                                      | i.e `"jans-redis-cluster"`                                                                  |
| `INSTALL_REDIS`                                 | Install Redis                                                                    | `"Y"` or `"N"`                                                                              |
| `INSTALL_COUCHBASE`                             | Install couchbase                                                                | `"Y"` or `"N"`                                                                              |
| `COUCHBASE_NAMESPACE`                           | Couchbase namespace                                                              | `"<name>"`                                                                                  |
| `COUCHBASE_VOLUME_TYPE`                         | Persistence Volume type                                                          | `"io1"`,`"ps-ssd"`, `"Premium_LRS"`                                                         |
| `COUCHBASE_CLUSTER_NAME`                        | Couchbase cluster name                                                           | `"<name>"`                                                                                  |
| `COUCHBASE_URL`                                 | Couchbase internal address to the cluster                                        | `""` or i.e `"<clustername>.<namespace>.svc.cluster.local"`                                 |
| `COUCHBASE_USER`                                | Couchbase username                                                               | `""` or i.e `"jans"`                                                                        |
| `COUCHBASE_BUCKET_PREFIX`                       | Prefix for Couchbase buckets                                                     | `jans`                                                                                      |
| `COUCHBASE_PASSWORD`                            | Password of CB 6 chars min: 1 capital, 1 small, 1 digit and 1 special char       | `"P@ssw0rd"`                                                                                |
| `COUCHBASE_SUPERUSER`                           | Couchbase superuser username                                                     | `""` or i.e `"admin"`                                                                       |
| `COUCHBASE_SUPERUSER_PASSWORD`                  | Password of CB 6 chars min: 1 capital, 1 small, 1 digit and 1 special char       | `"P@ssw0rd"`                                                                                |
| `COUCHBASE_CRT`                                 | Couchbase CA certification                                                       | `""` or i.e `<crt content not encoded>`                                                     |
| `COUCHBASE_CN`                                  | Couchbase certificate common name                                                | `""`                                                                                        |
| `COUCHBASE_INDEX_NUM_REPLICA`                   | Couchbase number of replicas per index                                           | `0`                                                                                         |
| `COUCHBASE_SUBJECT_ALT_NAME`                    | Couchbase SAN                                                                    | `""` or i.e `"cb.jans.org"`                                                                 |
| `COUCHBASE_CLUSTER_FILE_OVERRIDE`               | Override `couchbase-cluster.yaml` with a custom `couchbase-cluster.yaml`         | `"Y"` or `"N"`                                                                              |
| `COUCHBASE_USE_LOW_RESOURCES`                   | Use very low resources for Couchbase deployment. For demo purposes               | `"Y"` or `"N"`                                                                              |
| `COUCHBASE_DATA_NODES`                          | Number of Couchbase data nodes                                                   | `""` or i.e `"4"`                                                                           |
| `COUCHBASE_QUERY_NODES`                         | Number of Couchbase query nodes                                                  | `""` or i.e `"3"`                                                                           |
| `COUCHBASE_INDEX_NODES`                         | Number of Couchbase index nodes                                                  | `""` or i.e `"3"`                                                                           | 
| `COUCHBASE_SEARCH_EVENTING_ANALYTICS_NODES`     | Number of Couchbase search, eventing and analytics nodes                         | `""` or i.e `"2"`                                                                           |
| `COUCHBASE_GENERAL_STORAGE`                     | Couchbase general storage size                                                   | `""` or i.e `"2"`                                                                           |
| `COUCHBASE_DATA_STORAGE`                        | Couchbase data storage size                                                      | `""` or i.e `"5Gi"`                                                                         |
| `COUCHBASE_INDEX_STORAGE`                       | Couchbase index storage size                                                     | `""` or i.e `"5Gi"`                                                                         |
| `COUCHBASE_QUERY_STORAGE`                       | Couchbase query storage size                                                     | `""` or i.e `"5Gi"`                                                                         |
| `COUCHBASE_ANALYTICS_STORAGE`                   | Couchbase search, eventing and analytics storage size                            | `""` or i.e `"5Gi"`                                                                         |
| `COUCHBASE_INCR_BACKUP_SCHEDULE`                | Couchbase incremental backup schedule                                            |  i.e `"*/30 * * * *"`                                                                       |
| `COUCHBASE_FULL_BACKUP_SCHEDULE`                | Couchbase  full backup  schedule                                                 |  i.e `"0 2 * * 6"`                                                                          |
| `COUCHBASE_BACKUP_RETENTION_TIME`               | Couchbase time to retain backups in s,m or h                                     |  i.e `"168h`                                                                                |
| `COUCHBASE_BACKUP_STORAGE_SIZE`                 | Couchbase backup storage size                                                    | i.e `"20Gi"`                                                                                |
| `NUMBER_OF_EXPECTED_USERS`                      | Number of expected users [couchbase-resource-calc-alpha]                         | `""` or i.e `"1000000"`                                                                     |
| `EXPECTED_TRANSACTIONS_PER_SEC`                 | Expected transactions per second [couchbase-resource-calc-alpha]                 | `""` or i.e `"2000"`                                                                        |
| `USING_CODE_FLOW`                               | If using code flow [couchbase-resource-calc-alpha]                               | `""`, `"Y"` or `"N"`                                                                        |
| `USING_SCIM_FLOW`                               | If using SCIM flow [couchbase-resource-calc-alpha]                               | `""`, `"Y"` or `"N"`                                                                        |
| `USING_RESOURCE_OWNER_PASSWORD_CRED_GRANT_FLOW` | If using password flow [couchbase-resource-calc-alpha]                           | `""`, `"Y"` or `"N"`                                                                        |
| `DEPLOY_MULTI_CLUSTER`                          | Deploying a Multi-cluster [alpha]                                                | `"Y"` or `"N"`                                                                              |
| `HYBRID_LDAP_HELD_DATA`                         | Type of data to be held in LDAP with a hybrid installation of couchbase and LDAP | `""`, `"default"`, `"user"`, `"site"`, `"cache"` or `"token"`                               |
| `LDAP_JACKRABBIT_VOLUME`                        | LDAP/Jackrabbit Volume type                                                      | `""`, `"io1"`,`"ps-ssd"`, `"Premium_LRS"`                                                   |
| `APP_VOLUME_TYPE`                               | Volume type for LDAP persistence                                                 | [options](#app_volume_type-options)                                                         |
| `INSTALL_JACKRABBIT`                            | Install Jackrabbit                                                               | `"Y"` or `"N"`                                                                              |
| `JACKRABBIT_STORAGE_SIZE`                       | Jackrabbit volume storage size                                                   | `""` i.e `"4Gi"`                                                                            |
| `JACKRABBIT_URL`                                | http:// url for Jackrabbit                                                       | i.e `"http://jackrabbit:8080"`                                                              |
| `JACKRABBIT_ADMIN_ID`                           | Jackrabbit admin ID                                                              | i.e `"admin"`                                                                               |
| `JACKRABBIT_ADMIN_PASSWORD`                     | Jackrabbit admin password                                                        | i.e `"admin"`                                                                           |
| `JACKRABBIT_CLUSTER`                            | Jackrabbit Cluster mode                                                          | `"N"` or `"Y"`                                                                              |
| `JACKRABBIT_PG_USER`                            | Jackrabbit postgres username                                                     | i.e `"jackrabbit"`                                                                          |
| `JACKRABBIT_PG_PASSWORD`                        | Jackrabbit postgres password                                                     | i.e `"jackrabbbit"`                                                                         |
| `JACKRABBIT_DATABASE`                           | Jackrabbit postgres database name                                                | i.e `"jackrabbit"`                                                                          |
| `LDAP_STATIC_VOLUME_ID`                         | LDAP static volume id (AWS EKS)                                                  | `""` or `"<static-volume-id>"`                                                              |
| `LDAP_STATIC_DISK_URI`                          | LDAP static disk uri (GCE GKE or Azure)                                          | `""` or `"<disk-uri>"`                                                                      |
| `LDAP_BACKUP_SCHEDULE`                          | LDAP back up cron job frequency                                                  |  i.e `"*/30 * * * *"`                                                                       |
| `JANS_CACHE_TYPE`                               | Cache type to be used                                                            | `"IN_MEMORY"`, `"REDIS"` or `"NATIVE_PERSISTENCE"`                                          |
| `JANS_NAMESPACE`                                | Namespace to deploy Janssen in                                                      | `"<name>"`                                                                                  |
| `JANS_FQDN`                                     | Janssen FQDN                                                                        | `"<FQDN>"` i.e `"demoexample.jans.io"`                                                     |
| `COUNTRY_CODE`                                  | Janssen country code                                                                | `"<country code>"` i.e `"US"`                                                               |
| `STATE`                                         | Janssen state                                                                       | `"<state>"` i.e `"TX"`                                                                      |
| `EMAIL`                                         | Janssen email                                                                       | `"<email>"` i.e `"support@jans.io"`                                                        |
| `CITY`                                          | Janssen city                                                                        | `"<city>"` i.e `"Austin"`                                                                   |
| `ORG_NAME`                                      | Janssen organization name                                                           | `"<org-name>"` i.e `"Janssen"`                                                                 |
| `LDAP_PW`                                       | Password of LDAP 6 chars min: 1 capital, 1 small, 1 digit and 1 special char     | `"P@ssw0rd"`                                                                                |
| `GMAIL_ACCOUNT`                                 | Gmail account for GKE installation                                               | `""` or`"<gmail>"` i.e                                                                      |
| `GOOGLE_NODE_HOME_DIR`                          | User node home directory, used if the hosts volume is used                       | `"Y"` or `"N"`                                                                              |
| `IS_JANS_FQDN_REGISTERED`                       | Is Janssen FQDN globally resolvable                                                 | `"Y"` or `"N"`                                                                              |
| `OXD_APPLICATION_KEYSTORE_CN`                   | OXD application keystore common name                                             | `"<name>"` i.e `"oxd_server"`                                                               |
| `OXD_ADMIN_KEYSTORE_CN`                         | OXD admin keystore common name                                                   | `"<name>"` i.e `"oxd_server"`                                                               |
| `LDAP_STORAGE_SIZE`                             | LDAP volume storage size                                                         | `""` i.e `"4Gi"`                                                                            |
| `OXAUTH_KEYS_LIFE`                              | oxAuth Key life span in hours                                                    | `48`                                                               |
| `FIDO2_REPLICAS`                                | Number of FIDO2 replicas                                                         | min `"1"`                                                                                   |
| `SCIM_REPLICAS`                                 | Number of SCIM replicas                                                          | min `"1"`                                                                                   |
| `OXAUTH_REPLICAS`                               | Number of oxAuth replicas                                                        | min `"1"`                                                                                   |
| `OXTRUST_REPLICAS`                              | Number of oxTrust replicas                                                       | min `"1"`                                                                                   |
| `LDAP_REPLICAS`                                 | Number of LDAP replicas                                                          | min `"1"`                                                                                   |
| `OXSHIBBOLETH_REPLICAS`                         | Number of oxShibboleth replicas                                                  | min `"1"`                                                                                   |
| `OXPASSPORT_REPLICAS`                           | Number of oxPassport replicas                                                    | min `"1"`                                                                                   |
| `OXD_SERVER_REPLICAS`                           | Number of oxdServer replicas                                                     | min `"1"`                                                                                   |
| `CASA_REPLICAS`                                 | Number of Casa replicas                                                          | min `"1"`                                                                                   |
| `RADIUS_REPLICAS`                               | Number of Radius replica                                                         | min `"1"`                                                                                   |
| `ENABLE_OXTRUST_API`                            | Enable oxTrust-api                                                               | `"Y"` or `"N"`                                                                              |
| `ENABLE_OXTRUST_TEST_MODE`                      | Enable oxTrust Test Mode                                                         | `"Y"` or `"N"`                                                                              |
| `ENABLE_CACHE_REFRESH`                          | Enable cache refresh rotate installation                                         | `"Y"` or `"N"`                                                                              |
| `ENABLE_OXD`                                    | Enable oxd server installation                                                   | `"Y"` or `"N"`                                                                              |
| `ENABLE_RADIUS`                                 | Enable Radius installation                                                       | `"Y"` or `"N"`                                                                              |
| `ENABLE_OXPASSPORT`                             | Enable oxPassport installation                                                   | `"Y"` or `"N"`                                                                              |
| `ENABLE_OXSHIBBOLETH`                           | Enable oxShibboleth installation                                                 | `"Y"` or `"N"`                                                                              |
| `ENABLE_CASA`                                   | Enable Casa installation                                                         | `"Y"` or `"N"`                                                                              |
| `ENABLE_FIDO2`                                  | Enable Fido2 installation                                                        | `"Y"` or `"N"`                                                                              |
| `ENABLE_SCIM`                                   | Enable SCIM installation                                                         | `"Y"` or `"N"`                                                                              |
| `ENABLE_OXAUTH_KEY_ROTATE`                      | Enable key rotate installation                                                   | `"Y"` or `"N"`                                                                              |
| `ENABLE_OXTRUST_API_BOOLEAN`                    | Used by `pyjans-kubernetes`                                                      | `"false"`                                                                                   |
| `ENABLE_OXTRUST_TEST_MODE_BOOLEAN`              | Used by `pyjans-kubernetes`                                                      | `"false"`                                                                                   |
| `ENABLE_RADIUS_BOOLEAN`                         | Used by `pyjans-kubernetes`                                                      | `"false"`                                                                                   |
| `ENABLE_OXPASSPORT_BOOLEAN`                     | Used by `pyjans-kubernetes`                                                      | `"false"`                                                                                   |
| `ENABLE_CASA_BOOLEAN`                           | Used by `pyjans-kubernetes`                                                      | `"false"`                                                                                   |
| `ENABLE_SAML_BOOLEAN`                           | Used by `pyjans-kubernetes`                                                      | `"false"`                                                                                   |
| `ENABLED_SERVICES_LIST`                         | Used by `pyjans-kubernetes`. List of all enabled services                        | `"[]"`                                                                                   |
| `EDIT_IMAGE_NAMES_TAGS`                         | Manually place the image source and tag                                          | `"Y"` or `"N"`                                                                              |
| `JACKRABBIT_IMAGE_NAME`                         | Jackrabbit image repository name                                                 | i.e `"gluufederation/jackrabbit"`                                                           |
| `JACKRABBIT_IMAGE_TAG`                          | Jackrabbit image tag                                                             | i.e `"4.2.2_02"`                                                                            |
| `CASA_IMAGE_NAME`                               | Casa image repository name                                                       | i.e `"gluufederation/casa"`                                                                 |
| `CASA_IMAGE_TAG`                                | Casa image tag                                                                   | i.e `"4.2.2_02"`                                                                            |
| `CONFIG_IMAGE_NAME`                             | Config image repository name                                                     | i.e `"gluufederation/config-init"`                                                          |
| `CONFIG_IMAGE_TAG`                              | Config image tag                                                                 | i.e `"4.2.2_02"`                                                                            |
| `CACHE_REFRESH_ROTATE_IMAGE_NAME`               | Cache refresh image repository name                                              | i.e `"gluufederation/cr-rotate"`                                                            |
| `CACHE_REFRESH_ROTATE_IMAGE_TAG`                | Cache refresh  image tag                                                         | i.e `"4.2.2_02"`                                                                            |
| `CERT_MANAGER_IMAGE_NAME`                       | Janssens Certificate management image repository name                               | i.e `"gluufederation/certmanager"`                                                          |
| `CERT_MANAGER_IMAGE_TAG`                        | Janssens Certificate management image tag                                           | i.e `"4.2.2_02"`                                                                            |
| `LDAP_IMAGE_NAME`                               | LDAP image repository name                                                       | i.e `"gluufederation/opendj"`                                                               |
| `LDAP_IMAGE_TAG`                                | LDAP image tag                                                                   | i.e `"4.2.2_02"`                                                                            |
| `OXAUTH_IMAGE_NAME`                             | oxAuth image repository name                                                     | i.e `"gluufederation/oxauth"`                                                               |
| `OXAUTH_IMAGE_TAG`                              | oxAuth image tag                                                                 | i.e `"4.2.2_03"`                                                                            |
| `OXD_IMAGE_NAME`                                | oxd image repository name                                                        | i.e `"gluufederation/oxd-server"`                                                           |
| `OXD_IMAGE_TAG`                                 | oxd image tag                                                                    | i.e `"4.2.2_02"`                                                                            |
| `OXPASSPORT_IMAGE_NAME`                         | oxPassport image repository name                                                 | i.e `"gluufederation/oxpassport"`                                                           |
| `OXPASSPORT_IMAGE_TAG`                          | oxPassport image tag                                                             | i.e `"4.2.2_02"`                                                                            |
| `FIDO2_IMAGE_NAME`                              | FIDO2 image repository name                                                      | i.e `"gluufederation/oxpassport"`                                                           |
| `FIDO2_IMAGE_TAG`                               | FIDO2 image tag                                                                  | i.e `"4.2.2_02"`                                                                            |
| `SCIM_IMAGE_NAME`                               | SCIM image repository name                                                       | i.e `"gluufederation/oxpassport"`                                                           |
| `SCIM_IMAGE_TAG`                                | SCIM image tag                                                                   | i.e `"4.2.2_02"`                                                                            |
| `OXSHIBBOLETH_IMAGE_NAME`                       | oxShibboleth image repository name                                               | i.e `"gluufederation/oxshibboleth"`                                                         |
| `OXSHIBBOLETH_IMAGE_TAG`                        | oxShibboleth image tag                                                           | i.e `"4.2.2_02"`                                                                            |
| `OXTRUST_IMAGE_NAME`                            | oxTrust image repository name                                                    | i.e `"gluufederation/oxtrust"`                                                              |
| `OXTRUST_IMAGE_TAG`                             | oxTrust image tag                                                                | i.e `"4.2.2_02"`                                                                            |
| `PERSISTENCE_IMAGE_NAME`                        | Persistence image repository name                                                | i.e `"gluufederation/persistence"`                                                          |
| `PERSISTENCE_IMAGE_TAG`                         | Persistence image tag                                                            | i.e `"4.2.2_02"`                                                                            |
| `RADIUS_IMAGE_NAME`                             | Radius image repository name                                                     | i.e `"gluufederation/radius"`                                                               |
| `RADIUS_IMAGE_TAG`                              | Radius image tag                                                                 | i.e `"4.2.2_02"`                                                                            |
| `JANS_GATEWAY_IMAGE_NAME`                       | Janssen Gateway image repository name                                               | i.e `"gluufederation/jans-gateway"`                                                         |
| `JANS_GATEWAY_IMAGE_TAG`                        | Janssen Gateway image tag                                                           | i.e `"4.2.2_01"`                                                                            |
| `JANS_GATEWAY_UI_IMAGE_NAME`                    | Janssen Gateway UI image repository name                                            | i.e `"gluufederation/jans-gateway-ui"`                                                      |
| `JANS_GATEWAY_UI_IMAGE_TAG`                     | Janssen Gateway UI image tag                                                        | i.e `"4.2.2_01"`                                                                            |
| `UPGRADE_IMAGE_NAME`                            | Janssen upgrade image repository name                                               | i.e `"gluufederation/upgrade"`                                                              |
| `UPGRADE_IMAGE_TAG`                             | Janssen upgrade image tag                                                           | i.e `"4.2.2_02"`                                                                            |
| `CONFIRM_PARAMS`                                | Confirm using above options                                                      | `"Y"` or `"N"`                                                                              |
| `JANS_LDAP_MULTI_CLUSTER`                       | HELM-ALPHA-FEATURE: Enable LDAP multi cluster environment                        |`"Y"` or `"N"`                                                                               |
| `JANS_LDAP_SERF_PORT`                           | HELM-ALPHA-FEATURE: Serf UDP and TCP port                                        | i.e `30946`                                                                                 |
| `JANS_LDAP_ADVERTISE_ADDRESS`                   | HELM-ALPHA-FEATURE: LDAP pod advertise address                                   | i.e `demoexample.jans.io:30946"`                                                           |
| `JANS_LDAP_ADVERTISE_ADMIN_PORT`                | HELM-ALPHA-FEATURE: LDAP serf advertise admin port                               | i.e `30444`                                                                                 |
| `JANS_LDAP_ADVERTISE_LDAPS_PORT`                | HELM-ALPHA-FEATURE: LDAP serf advertise LDAPS port                               | i.e `30636`                                                                                 |
| `JANS_LDAP_ADVERTISE_REPLICATION_PORT`          | HELM-ALPHA-FEATURE: LDAP serf advertise replication port                         | i.e `30989`                                                                                 |
| `JANS_LDAP_SECONDARY_CLUSTER`                   | HELM-ALPHA-FEATURE: Is this the first kubernetes cluster or not                  | `"Y"` or `"N"`                                                                              |
| `JANS_LDAP_SERF_PEERS`                          | HELM-ALPHA-FEATURE: All opendj serf advertised addresses. This must be resolvable | `["firstldap.jans.org:30946", "secondldap.jans.org:31946"]` |

### `APP_VOLUME_TYPE`-options

`APP_VOLUME_TYPE=""` but if `PERSISTENCE_BACKEND` is `OpenDJ` options are :

| Options  | Deployemnt Architecture  | Volume Type                                   |
| -------- | ------------------------ | --------------------------------------------- |
| `1`      | Microk8s                 | volumes on host                          |
| `2`      | Minikube                 | volumes on host                          |
| `6`      | EKS                      | volumes on host                          |
| `7`      | EKS                      | EBS volumes dynamically provisioned      |
| `8`      | EKS                      | EBS volumes statically provisioned       |
| `11`     | GKE                      | volumes on host                          |
| `12`     | GKE                      | Persistent Disk  dynamically provisioned |
| `13`     | GKE                      | Persistent Disk  statically provisioned  |
| `16`     | Azure                    | volumes on host                          |
| `17`     | Azure                    | Persistent Disk  dynamically provisioned |
| `18`     | Azure                    | Persistent Disk  statically provisioned  |
| `21`     | Digital Ocean            | volumes on host                          |
| `22`     | Digital Ocean            | Persistent Disk  dynamically provisioned |
| `23`     | Digital Ocean            | Persistent Disk  statically provisioned  |
    

## Use Couchbase solely as the persistence layer

### Requirements
  - If you are installing on microk8s or minikube please ignore the below notes as a low resource `couchbase-cluster.yaml` will be applied automatically, however the VM being used must at least have 8GB RAM and 2 cpu available .
  
  - An `m5.xlarge` EKS cluster with 3 nodes at the minimum or `n2-standard-4` GKE cluster with 3 nodes. We advice contacting Janssen regarding production setups.

- [Install couchbase Operator](https://www.couchbase.com/downloads) linux version `2.1.0` is recommended but version `2.0.3` is also supported. Place the tar.gz file inside the same directory as the `pyjans-kubernetes.pyz`.

- A modified `couchbase/couchbase-cluster.yaml` will be generated but in production it is likely that this file will be modified.
  * To override the `couchbase-cluster.yaml` place the file inside `/couchbase` folder after running `./pyjans-kubernetes.pyz`. More information on the properties [couchbase-cluster.yaml](https://docs.couchbase.com/operator/1.2/couchbase-cluster-config.html).

!!!note
    Please note the `couchbase/couchbase-cluster.yaml` file must include at least three defined `spec.servers` with the labels `couchbase_services: index`, `couchbase_services: data` and `couchbase_services: analytics`

**If you wish to get started fast just change the values of `spec.servers.name` and `spec.servers.serverGroups` inside `couchbase/couchbase-cluster.yaml` to the zones of your EKS nodes and continue.**

- Run `./pyjans-kubernetes.pyz install-couchbase` and follow the prompts to install couchbase solely with Janssen.


## Use remote Couchbase as the persistence layer

- [Install couchbase](https://docs.couchbase.com/server/current/install/install-intro.html)

- Obtain the Public DNS or FQDN of the couchbase node.

- Head to the FQDN of the couchbase node to [setup](https://docs.couchbase.com/server/current/manage/manage-nodes/create-cluster.html) your couchbase cluster. When setting up please use the FQDN as the hostname of the new cluster.

- Couchbase URL base , user, and password will be needed for installation when running `pyjans-kubernetes.pyz`


### How to expand EBS volumes

1. Make sure the `StorageClass` used in your deployment has the `allowVolumeExpansion` set to true. If you have used our EBS volume deployment strategy then you will find that this property has already been set for you.

1. Edit your persistent volume claim using `kubectl edit pvc <claim-name> -n <namespace> ` and increase the value found for `storage:` to the value needed. Make sure the volumes expand by checking the `kubectl get pvc <claim-name> -n <namespace> `.

1. Restart the associated services


### Scaling pods

!!!note
    When using Mircok8s substitute  `kubectl` with `microk8s.kubectl` in the below commands.

To scale pods, run the following command:

```
kubectl scale --replicas=<number> <resource> <name>
```

In this case, `<resource>` could be Deployment or Statefulset and `<name>` is the resource name.

Examples:

-   Scaling oxAuth:

    ```
    kubectl scale --replicas=2 deployment oxauth
    ```

-   Scaling oxTrust:

    ```
    kubectl scale --replicas=2 statefulset oxtrust
    ```
    
### Working with Jackrabbit

| Services         | Folder  / File                      |  Jackrabbit Repository                                  | Method                 |
| ---------------- | ----------------------------------- | ------------------------------------------------------- | ---------------------- |
| `oxAuth`         | `/opt/jans/jetty/oxauth/custom`     | `/repository/default/opt/jans/jetty/oxauth/custom`      | `PULL` from Jackrabbit |
| `oxTrust`        | `/opt/jans/jetty/identity/custom`   |  `/repository/default/opt/jans/jetty/identity/custom`   | `PULL` from Jackrabbit |
| `Casa`           | `/opt/jans/jetty/casa`              | `/repository/default/opt/jans/jetty/casa`               | `PULL` from Jackrabbit |

The above means that Jackrabbit will maintain the source folder on all replicas of a service. If one pushed a custom file to `/opt/jans/jetty/oxauth/custom` at one replica all other replicas would have this file.

#### oxTrust --> Jackrabbit --> oxShibboleth

| Services         | Folder  / File                      |  Jackrabbit Repository                                  | Method                 |
| ---------------- | ----------------------------------- | ------------------------------------------------------- | ---------------------- |
| `oxTrust`        | `/opt/shibboleth-idp`               |  `/repository/default/opt/shibboleth-idp`               | `PUSH` to Jackrabbit   |
| `oxShibboleth`   | `/opt/shibboleth-idp`               | `/repository/default/opt/shibboleth-idp`                | `PULL` from Jackrabbit |

#### oxAuth --> Jackrabbit --> Casa

| Services         | Folder  / File                      |  Jackrabbit Repository                                  | Method                 |
| ---------------- | ----------------------------------- | ------------------------------------------------------- | ---------------------- |
| `oxAuth `        | `/etc/certs/otp_configuration.json` |  `/repository/etc/certs/otp_configuration.json`         | `PUSH` to Jackrabbit   |
| `oxAuth `        | `/etc/certs/super_jans_creds.json`  |  `/repository/default/etc/certs/super_jans_creds.json`  | `PUSH` to Jackrabbit   |
| `Casa`           | `/etc/certs/otp_configuration.json` | `/repository/etc/certs/otp_configuration.json`          | `PULL` from Jackrabbit |
| `Casa`           | `/etc/certs/super_jans_creds.json`  | `/repository/default/etc/certs/super_jans_creds.json`   | `PULL` from Jackrabbit |

![svg](../img/kubernetes/cn-jackrabbit.svg)

=== "File managers"

    !!!note
        You can use any client to connect to Jackrabbit. We assume Janssen is installed in `jans` namespace

    1. Port forward Jackrabbit at `localhost` on port `8080`
    
        ```bash
            kubectl port-forward jackrabbit-0 --namespace jans 8080:8080
        ```
    
    
    1. Optional: If your managing VM is in the cloud you must forward the connection to the mac, linux or windows computer you are working from.
    
        ```bash
            ssh -i <key.pem> -L 8080:localhost:8080 user-of-managing-vm@ip-of-managing-vm
        ```
        
    1. Use any filemanager to connect to Jackrabbit. Here are some examples:
    
        === "Linux"
        
            Open file manager which maybe `Nautilus` and find `Connect to Server` place the address which should be `http://localhost:8080`. By default the username and password are `admin` if not changed in `etc/jans/conf/jca_password`.
        
        === "Windows"
        
            Open  `My PC` and inside the address that might read your `C` drive place the address which should be `http://localhost:8080`. By default the username and password are `admin` if not changed in `etc/jans/conf/jca_password`.
            
        === "Mac"
        
            Open `Finder` , `Go` then `Connect to Server` and place the address which should be `http://localhost:8080`. By default the username and password are `admin` if not changed in `etc/jans/conf/jca_password`. 
        
=== "Script"

    !!!warning
        Used for quick testing with Jackrabbit and should be avoided. 

    1. Copy files to Jackrabbit container at `/opt/webdav`
    
    1. Run `python3 /app/scripts/jca_sync.py` .


## Build pyjans-kubernetes installer

### Overview
[`pyjans-kubernetes.pyz`](https://github.com/JanssenFederation/cloud-native-edition/releases) is periodically released and does not need to be built manually. However, the process of building the installer package is listed [below](#build-pyjans-kubernetespyz-manually).

### Build `pyjans-kubernetes.pyz` manually

### Prerequisites

1.  Python 3.6+.
1.  Python `pip3` package.

### Installation

#### Standard Python package

1.  Create virtual environment and activate:

    ```sh
    python3 -m venv .venv
    source .venv/bin/activate
    ```

1.  Install the package:

    ```
    make install
    ```

    This command will install executable called `pyjans-kubernetes` available in virtual environment `PATH`.

#### Python zipapp

1.  Install [shiv](https://shiv.readthedocs.io/) using `pip3`:

    ```sh
    pip3 install shiv
    ```

1.  Install the package:

    ```sh
    make zipapp
    ```

    This command will generate executable called `pyjans-kubernetes.pyz` under the same directory.

## Architectural diagram of all Janssen services

![svg](../img/kubernetes/cn-general-arch-diagram.svg)

## Architectural diagram of oxPassport

![svg](../img/kubernetes/cn-oxpassport.svg)

## Architectural diagram of Casa

![svg](../img/kubernetes/cn-casa.svg)

## Architectural diagram of SCIM

![svg](../img/kubernetes/cn-scim.svg)


## Minimum Couchbase System Requirements for cloud deployments

!!!note
    Couchbase needs optimization in a production environment and must be tested to suit the organizational needs. 
 
| NAME                                     | # of nodes  | RAM(GiB) | Disk Space | CPU | Total RAM(GiB)                           | Total CPU |
| ---------------------------------------- | ----------- | -------  | ---------- | --- | ---------------------------------------- | --------- |
| Couchbase Index                          | 1           |  3       | 5Gi        | 1  | 3                                         | 1         |
| Couchbase Query                          | 1           |  -       | 5Gi        | 1  | -                                         | 1         |
| Couchbase Data                           | 1           |  3       | 5Gi        | 1  | 3                                         | 1         |
| Couchbase Search, Eventing and Analytics | 1           |  2       | 5Gi        | 1  | 2                                         | 1         |
| Grand Total                              |             | 7-8 GB (if query pod is allocated 1 GB)  | 20Gi | 4         |
