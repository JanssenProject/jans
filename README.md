
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

This will install docker, microk8s, helm and Janssen with the default settings the can be found inside [values.yaml](charts/jans/values.yaml). Please map the `ip` of the instance running ubuntu to `demoexample.jans.io` and then access the endpoints at your browser such in the example in the table below.


|Service           | Example endpoint                                                       |   
|------------------|------------------------------------------------------------------------|
|Auth server       | `https://demoexample.jans.io/.well-known/openid-configuration`        |
|fido2             | `https://demoexample.jans.io/.well-known/fido2-configuration`         |
|scim              | `https://demoexample.jans.io/.well-known/scim-configuration`          |   

For more information follow [here](charts/jans/README.md).