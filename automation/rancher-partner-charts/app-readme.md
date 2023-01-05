## Introduction
The Janssen Server is a container distribution of free open source software (FOSS) for identity and access management (IAM). SaaS, custom, open source and commercial web and mobile applications can leverage a Janssen Server for user authentication, identity information, and policy decisions.

Common use cases include:

- Single sign-on (SSO)   
- Mobile authentication    
- API access management  
- Two-factor authentication (2FA)
- Customer identity and access management (CIAM)   
- Identity federation      

### Free Open Source Software 
The Janssen Server is a FOSS platform for IAM.

### Open Web Standards
The Janssen Server can be deployed to support the following open standards for authentication, authorization, federated identity, and identity management:

- OAuth 2.0    
- OpenID Connect    
- User Managed Access 2.0 (UMA)    
- SAML 2.0   
- System for Cross-domain Identity Management (SCIM)    
- FIDO Universal 2nd Factor (U2F)
- FIDO 2.0 / WebAuthn
- Lightweight Directory Access Protocol (LDAP)   
- Remote Authentication Dial-In User Service (RADIUS)

### Important notes for installation:
- Make sure to enable `Customize Helm options before install` after clicking the initial `Install` on the top right. When you view your helm options, please uncheck the wait parameter as that conflicts with the post-install hook for the persistence image.

### Quick install on Rancher UI with Docker single node
- Install the nginx-ingress-controller chart.
- Install the OpenEBS chart.
- Install Janssen chart and specify your persistence as ldap.
