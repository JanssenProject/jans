---
tags:
  - administration
  - kubernetes
  - operations
  - memory
  - dump
  - heap
---

# Memory Dump 

This document will demonstrate how to generate a memory dump so that you can analyze your memory usage.

## Services supported

This operation can be made in the following Java-based services:

| Service     | JAVA_OPTIONS environment variable | 
|-------------|-----------------------------------|
| auth-server | CN_AUTH_JAVA_OPTIONS              |
| casa        | CN_CASA_JAVA_OPTIONS              |
| config-api  | CN_CONFIG_API_JAVA_OPTIONS        |
| fido2       | CN_FIDO2_JAVA_OPTIONS             |
| link        | CN_LINK_JAVA_OPTIONS              |
| saml        | CN_SAML_JAVA_OPTIONS              |
| scim        | CN_SCIM_JAVA_OPTIONS              |


## Steps

Let's go through the steps needed to generate a dump in `config-api`:

1. Increase the pod's memory request and limit to something larger than the default.

1. Edit the config-api deployment and modify the JAVA_OPTIONS: 

    ```
            env:
            - name: CN_CONFIG_API_JAVA_OPTIONS
              value: -XX:MaxDirectMemorySize=400m -Xmx150m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -XX:MaxRAMPercentage=0
    ```

1. Modify the `command` key in the deployment to add the following:

    ```
              command:
                - /bin/sh
                - -c
                - |
                  /app/scripts/entrypoint.sh &
                  sleep 900000000
    ```


1. You can view the heap usage using the following command:
   `kubectl exec -n <namespace> pod-name -- jcmd 7 GC.heap_info` 

1. Now once the process meets its limit, the process should shut down without the pod getting an OOM, allowing you to get the dump from within the pod.

1. Copy the generated dump from the pod:
    ```
    kubectl cp -n <namespace> pod-name:tmp/java_pid7.hprof java_pid7.hprof
    ```

