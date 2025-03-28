The resources may be set minimally to the below:

- 8-12 GB RAM based on the services deployed
- 8-10 CPU cores based on the services deployed
- 50GB hard-disk

Use the listing below for a detailed estimation of minimum required resources. The table contains the default resources recommendation per service. Depending on the use of each service the resources need may be increased or decreased.

| Service           | CPU Unit | RAM   | Disk Space | Processor Type | Required                           |
|-------------------|----------|-------|------------|----------------|------------------------------------|
| Auth server       | 2.5      | 2.5GB | N/A        | 64 Bit         | Yes                                |
| fido2             | 0.5      | 0.5GB | N/A        | 64 Bit         | No                                 |
| scim              | 1        | 1GB   | N/A        | 64 Bit         | No                                 |
| config - job      | 0.3      | 0.3GB | N/A        | 64 Bit         | Yes on fresh installs              |
| persistence - job | 0.3      | 0.3GB | N/A        | 64 Bit         | Yes on fresh installs              |
| nginx             | 1        | 1GB   | N/A        | 64 Bit         | Yes ALB/Istio not used             |
| auth-key-rotation | 0.3      | 0.3GB | N/A        | 64 Bit         | No [Strongly recommended]          |
| config-api        | 1        | 1GB   | N/A        | 64 Bit         | No                                 |
| casa              | 0.5      | 0.5GB | N/A        | 64 Bit         | No                                 |
| link              | 0.5      | 1GB   | N/A        | 64 Bit         | No                                 |
| saml              | 0.5      | 1GB   | N/A        | 64 Bit         | No                                 |
| kc-scheduler - job| 0.3      | 0.3GB | N/A        | 64 Bit         | No                                 |
| cleanup - job     | 0.3      | 0.3GB | N/A        | 64 Bit         | Yes                                |

Releases of images are in style 1.0.0-beta.0, 1.0.0-0
