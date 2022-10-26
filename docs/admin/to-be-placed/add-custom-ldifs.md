## Contents:

- [Overview](#overview)
- [Using Kubernetes?](#kubernetes)

## Overview

This guide describes steps to load custom work to Janssen IDP.


#### Hardware configuration

For development and POC purposes, 4GB RAM and 10 GB HDD should be available for Janssen Server. For PROD deployments, please refer [installation guide](https://github.com/JanssenProject/jans/wiki#janssen-installation).
  

#### Prerequisites
- Existing Janssen server installed

#### Kubernetes

If you are using Kubernetes please follow this section. You may create several ldif files denoting each type such as `custom_attributes.ldif`, `custom_clients.ldif`..etc. Refer to the built-in [`ldifs`](https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/templates) for examples.

1. Create your custom ldif files. These can be clients, attributes, scopes or whatever custom work you need. The example will be adding a custom attribute and we will call this file `custom_attributes.ldif`.

  ```
  dn: inum=C9B1,ou=attributes,o=jans
  description: Maiden name of the End-User.Note that in some cultures, people can have multiple given names;all can be present, with the names being separated by space characters.
  displayName: Maiden Name
  inum: C9B1
  jansAttrEditTyp: user
  jansAttrEditTyp: admin
  jansAttrName: givenName
  jansAttrOrigin: jansPerson
  jansAttrTyp: string
  jansAttrViewTyp: user
  jansAttrViewTyp: admin
  jansClaimName: maiden_name
  jansSAML1URI: urn:mace:dir:attribute-def:maidenName
  jansSAML2URI: urn:oid:2.5.4.42
  jansStatus: active
  objectClass: top
  objectClass: jansAttr
  urn: urn:mace:dir:attribute-def:maidenName
  ```

2. Create a configmap or secret depending on the if the ldif holds and secret data such as a client secret. Here, we will be creating a configmap. 

```bash
kubectl create cm custom-attributes -n <namespace-of-jans> -f custom_attributes.ldif
# kubectl create cm custom-attributes -n jans -f custom_attributes.ldif
# using a secret
# kubectl create secret generic custom-attributes -n jans -f custom_attributes.ldif
```

3. Mount the created configmap or secret inside your `values.yaml`

  ```yaml
  persistence:
    volumes:
     - name: custom-attributes
        configMap:
          name:  custom-attributes
    #- name: custom-attributes
    #  secret:
    #    secretName: custom-attributes
    # -- Configure any additional volumesMounts that need to be attached to the containers
    volumeMounts:
       - mountPath: "/app/custom_ldif/custom_attributes.ldif"
         name:  custom-attributes
         subPath: custom_attributes.ldif
  ```

4. Run helm upgrade to activate the persistence job.

  ```bash
  helm upgrade <release-name> janssen/janssen -f values.yaml -n <jans-namespace>
  ```

Your custom work should be loaded to your persistence. This also persists any changes going forward as you upgrade.
