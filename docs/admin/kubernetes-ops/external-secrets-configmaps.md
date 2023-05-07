## Overview
   This guide shows how to store and retrieve jans `configmaps` and `secrets` externally in `AWS Secrets Manager` and `GCP Secret Manager`.  

!!! Note
    Configmaps and Secrets are stored as a collection of key-value pairs. A secret in AWS/GCP Secret Manager has a max size of 65536 bytes. So the collection of key-value pairs is splitted between secrets, and thus note that during secrets [retrieval](#retrieve-secrets) it's possible that a single key-value pair is to be splitted between 2 AWS/GCP secrets.

##  Create Secrets

### AWS

There are 2 types of Secrets in AWS:

 1. `String Secret` where the secret can be created and retrieved from the console.

 2. `Binary Secret` where the secret is binary-encoded and can be created and retrieved only using the CLI/SDK.

`Configmaps` are stored in a single `String Secret`. It follows the naming convention of `janssen_configs`

`Secrets` are splitted and stored in multiple `Binary Secrets` due to the max size limitation.

 - `Secrets` follows the naming convention of `janssen_secrets`, `janssen_secrets_1`, `janssen_secrets_2`..etc
 
 - Every single secret doesn't have to be a valid json, but instead the collection of all secrets should have a valid json. For example: 

      `janssen_secrets`: {"key1":"value1",

      `janssen_secrets_1`: "key2":"value2",
      
      `janssen_secrets_2`: "key3":"value3"}




#### Fresh Installation

You will need the `ACCESS_KEY_ID` and `SECRET_ACCESS_KEY` of an IAM user with a `SecretsManagerReadWrite` policy attached.

Add the following configuration to your `override.yaml`:
```yaml
global:
    configAdapterName: aws
    configSecretAdapter: aws 
config:
  configmap:
    cnAwsAccessKeyId: L30E10OME18S1220S221
    cnAwsSecretAccessKey: Z1A1M9A1L1EKALIeHHN~o
    cnAwsDefaultRegion: us-east-1 #Choose based on the desired region
    cnAwsSecretsEndpointUrl: https://secretsmanager.us-east-1.amazonaws.com #Choose based on the desired region
    cnAwsSecretsNamePrefix: janssen
    cnAwsProfile: janssen #Choose based on your aws named profile 
    cnAwsSecretsReplicaRegions: [] #Optional if you want secrets to be replicated. [{"Region": "us-west-1"}, {"Region": "us-west-2"}]
```

Run `helm install` or `helm upgrade` if `Janssen` is already installed:
        
```bash
helm upgrade <helm-release-name> janssen/janssen -f override.yaml -n <namespace>
```

#### Export/Migration

##### Configmaps 
- Get the json of the configmap
  ```bash
  kubectl get configmap -n <namespace> cn -o json 
  ```

- Configmaps in AWS are stored as `StringSecret`, they can be created and retrieved from the console:
  Click `Create Secret` > Choose `Other type of Secret` > Click on `Plaintext` tab > Paste the `json` of the key-value pairs.

##### Secrets
- Get the json of the secret
  ```bash
  kubectl get secret -n <namespace> cn -o json 
  ```

- Secrets in AWS are **splitted** across multiple `BinarySecrets` which is created and retrieved using the CLI/SDK

- Assuming the json is stored in a file named `binary-secrets.json`:
  ```bash
  aws secretsmanager create-secret --name <secret-name> --secret-binary fileb://binary-secrets.json --region <secret-region>
  ```


### GCP 

#### Fresh Installation
Make sure you enabled `Secret Manager API`.

You will need a `Service account` with the `roles/secretmanager.admin` role. This service account json should then be `base64` encoded.

Add the following configuration to your `override.yaml`:
```yaml
global:
  configAdapterName: google
  configSecretAdapter: google 
config:
  configmap:
    cnGoogleSecretManagerServiceAccount: SWFtTm90YVNlcnZpY2VBY2NvdW50Q2hhbmdlTWV0b09uZQo= #base64 encoded service account json
    cnGoogleProjectId: google-project-to-save-config-and-secrets-to
    cnSecretGoogleSecretVersionId: "latest" # Secret version to be used for secret configuration. Defaults to latest and should normally always stay that way. 
    cnSecretGoogleSecretNamePrefix: janssen
    cnGoogleSecretManagerPassPhrase: Test1234# #Passphrase for Janssen secret in Google Secret Manager. Used for encrypting and decrypting data from Google's Secret Manager. 
    cnConfigGoogleSecretVersionId: "latest" #Secret version to be used for configuration. Defaults to latest and should normally always stay that way.
    cnConfigGoogleSecretNamePrefix: janssen
```    

Run `helm install` or `helm upgrade` if `Janssen` is already installed:
        
```bash
helm upgrade <helm-release-name> janssen/janssen -f override.yaml -n <namespace>
```

#### Export/Migration 


Get the json of the secret/configmap
```bash
kubectl get configmap -n <namespace> cn -o json 
```

From the console, Go to `Secret Manager`> Click on `Create Secret` > Add a `name` > Upload a `json` file or add the json to the `Secret value` field > Create


## Retrieve Secrets 
### AWS
**String Secret**: To retrieve the secret value from the Console, click on the secret name and then click on `Retrieve Secret Value`

**Binary Secret**: To retrieve the secret value using the cli
```bash
aws secretsmanager get-secret-value --secret-id <secret-name> --query 'SecretBinary' --output text --region <secret-region>
```

Note that the secret is binary encoded, so in order to have a decoded value, you can run the following
```bash
aws secretsmanager get-secret-value --secret-id <secret-name> --query 'SecretBinary' --output text --region <secret-region> | base64 --decode
```

Repeat these commands across all the secrets, to get the full key-value pairs.

### GCP 

Review [this](https://cloud.google.com/secret-manager/docs/access-secret-version#secretmanager-access-secret-version-console) to check multiple ways to retrieve secrets stored in GCP Secret Manager.
