---
tags:
  - administration
  - kubernetes
  - operations
  - custom attributes
  - schema
  - claim
---

# Custom Attributes

## Overview
Making changes in the schema to add custom columns/attributes

1.  Download the `custom_schema.json` file:

    `wget https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/schema/custom_schema.json`

1.  Make your edits in `custom_schema.json`:

    For example we will add 4 attributes and define them as int, json, bool, and text

    Firstly, add the attribute names to the object class:
    ```
    "objectClasses": [
        {
        "kind": "AUXILIARY",
        "may": [
            "intAttribute",
            "jsonAttribute",
            "boolAttribute",
            "textAttribute"
        
    ```

1.  Describe the attributes inside `attributeTypes`. You can refer to [RFC 4517](https://datatracker.ietf.org/doc/html/rfc4517):

    ```
    "attributeTypes": [
        {
        "desc": "int desc",
        "equality": "integerMatch",
        "names": [
            "intAttribute"
        ],
        "oid": "jansAttr",
        "syntax": "1.3.6.1.4.1.1466.115.121.1.27",
        "x_origin": "Jans created attribute"
        },

    {
        "desc": "json desc",
        "equality": "caseIgnoreSubstringsMatch",
        "names": [
            "jsonAttribute"
        ],
        "multivalued": true,
        "oid": "jansAttr",
        "syntax": "1.3.6.1.4.1.1466.115.121.1.15",
        "x_origin": "Jans created attribute"
        },

    {
        "desc": "bool desc",
        "equality": "booleanMatch",
        "names": [
            "boolAttribute"
        ],
        "oid": "jansAttr",
        "syntax": "1.3.6.1.4.1.1466.115.121.1.7",
        "x_origin": "Jans created attribute"
        },

        {
            "desc": "text desc",
            "equality": "caseIgnoreMatch",
            "names": [
            "textAttribute"
            ],
            "oid": "jansAttr",
            "syntax": "1.3.6.1.4.1.1466.115.121.1.15",
            "x_origin": "Jans created attribute"
        }

    ],
    ```



1.  Create a configmap with the schema file:  

    `kubectl create cm custom-schema-cm --from-file=custom_schema.json -n <namespace>`


1.  Mount the configmap in your `override.yaml` under `persistence.volumes` and `persistence.volumeMounts`:

    ```
    persistence:
        volumeMounts:
            - name: schema
              mountPath: /app/schema/custom_schema.json
              subPath: custom_schema.json
        volumes:
            - name: schema
              configMap:
                name: custom-schema-cm 
    ```

1.  Run helm install or helm upgrade if Jans has been already installed.

    ```bash
    helm upgrade <helm-release-name> janssen/janssen -n <namespace> -f override.yaml --version=1.0.x
    ```