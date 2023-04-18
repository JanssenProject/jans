## Overview
   This guide shows how to store `configmaps` and `secrets` externally in `AWS Secrets Manager` and `GCP Secret Manager`

## AWS

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
    cnAwsProfile: default #Choose based on your named profile 
    cnAwsSecretsReplicaRegions: [] #Optional if you want secrets to be replicated. [{"Region": "us-west-1"}, {"Region": "us-west-2"}]
```

Run `helm install` or `helm upgrade` if `Janssen` is already installed:
        
```bash
helm upgrade <helm-release-name> janssen/janssen -f override.yaml -n <namespace>
```

## GCP 

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
