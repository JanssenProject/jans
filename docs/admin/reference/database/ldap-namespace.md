---
tags:
  - administration
  - reference
  - database
---

``` mermaid
graph LR
    A[ou=jans] --> B(ou=agama)
    B --> B1[ou=flows]
    B1 --> B11[agFlowQname=flow_name1]
    B1 --> B12[agFlowQname=flow_name2]
    B1 --> B13[agFlowQname=flow_name3]
    B1 --> B14[agFlowQname=flow_name4]

    B --> B2[ou=runs]
    B2 --> B21[jansId=....]
    B2 --> B22[jansId=....]

    A[ou=jans] --> C(ou=attributes)
    C --> C1[inum=.....]
    C --> C2[inum=.....]
    C --> Cn[inum=.....]

    A[ou=jans] --> D(ou=authorizations)
    D --> D1[jansId=...]
    D --> D2[jansId=...]
    D --> D3[jansId=...]
    D --> Dn[jansId=...]

    A[ou=jans] --> E(ou=cache)
    E --> E1[uuid.....]

    A[ou=jans] --> F(ou=ciba)

    A[ou=jans] --> G(ou=clients)
    G --> G1[inum=....]
    G1 --> G12[ou=uma_permission]
    G12 --> G121[jansTicket=.....]
    G12 --> G122[jansTicket=.....]
    
    G --> G2[inum=....]
    G2 --> G21[ou=uma_permission]
    G21 --> G211[jansTicket=.....]
    G21 --> G212[jansTicket=.....]

    G --> Gn[inum=....]

    A[ou=jans] --> H(ou=groups)
    H --> H1[inum=...]
    H --> H2[inum=...]
    H --> Hn[inum=...]


    A[ou=jans] --> I(ou=metric)
    

    A[ou=jans] --> J(ou=par)

    A[ou=jans] --> K(ou=people)
    K --> K1[inum=....]
    K1 --> K11[ou=fido2_register]
    K11 --> K111[jansId=....]
    K11 --> K112[jansId=....]
    K11 --> K112[jansId=....]

    K1 --> K12[ou=fido2_auth]
    K12 --> K121[jansId=....]
    K12 --> K122[jansId=....]
    K12 --> K123[jansId=....]

    K --> K2[inum=....]
    K --> K3[inum=....]
    K --> K4[inum=....]
    K --> K5[inum=....]

    A[ou=jans] --> L(ou=push)
    L --> L1[ou=application]
    L --> L2[ou=device]

    A[ou=jans] --> M(ou=resetPasswordRequests)
    

    A[ou=jans] --> N(ou=scope)
    N --> N1[inum=...]
    N --> N2[inum=...]
    N --> N3[inum=...]

    A[ou=jans] --> O(ou=scripts)
    O --> O1[inum=...]
    O --> O2[inum=...]

    A[ou=jans] --> P(ou=sector_identifiers)
    

    A[ou=jans] --> Q(ou=sessions)
    Q --> Q1[jansId=....]
    Q --> Q2[jansId=....]
    Q --> Q3[jansId=....]

    A[ou=jans] --> R(ou=stat)
    R --> R1[ou=yyyymm]
    R --> R2[ou=yyyymm]
    

    A[ou=jans] --> S(ou=tokens)
    S --> S1[ou=uma_rpt]
    S --> S2[tknCde=....]
    S --> S3[tknCde=....]
    S --> S4[tknCde=....]

    A[ou=jans] --> T(ou=trustRelationships)
    T --> T1[inum=...]
    T --> T2[inum=...]
    T --> T3[inum=...]

    A[ou=jans] --> U(ou=uma)
    U --> W[ou=pct]
    W --> W1[tknCde=...]
    W --> W2[tknCde=...]
    W --> W3[tknCde=...]
    

    U --> X[ou=resources]
    X --> X1[jansId=...]
    X --> X2[jansId=...]
    X --> X3[jansId=...]

    A[ou=jans] --> V(ou=configuration)
    V --> V1[ou=admin-ui]
    V --> V2[ou=casa]
    V --> V3[ou=jans-auth]
    V --> V4[ou=jans-config-api]
    V --> V5[ou=jans-fido2]
    V --> V6[ou=jans-scim]
``` 
