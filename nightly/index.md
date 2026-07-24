# Build secure digital identity solutions with Janssen

**Enterprise-grade authentication and authorization platform** – Create robust identity management systems with OAuth 2.0, OpenID Connect, FIDO2, and modern security standards. Janssen provides everything you need to secure your applications and APIs.

[Get Started](https://docs.jans.io/nightly/janssen-server) [View on GitHub](https://github.com/JanssenProject/jans)

______________________________________________________________________

## Why Choose Janssen?

### Enterprise Security

Built with security at its core, featuring comprehensive audit logging, threat detection, and compliance controls. Advanced security features include brute force protection, account lockout policies, and detailed security event monitoring.

### Standards Compliant

Complete implementation of OAuth 2.0, OpenID Connect, SAML 2.0, FIDO2, SCIM, and UMA 2.0. Certified compatibility ensures seamless integration with existing systems and future-proof deployments.

### Cloud Ready

Native Kubernetes support with auto-scaling, service mesh compatibility, and cloud-agnostic deployment options. Designed for modern containerized environments with full observability.

### Highly Extensible

Powerful interception scripts, custom plugins, and comprehensive REST APIs. Build tailored authentication flows, integrate third-party services, and customize every aspect of the platform.

### User Experience

Casa self-service portal empowers users to manage their credentials, enroll devices, and handle account recovery. Intuitive interfaces reduce support burden while improving security.

### Visual Orchestration

Agama's drag-and-drop designer enables complex authentication workflows without coding. Create sophisticated multi-factor flows, integrate external services, and test in real-time.

______________________________________________________________________

## Core Components

**Enterprise Identity Platform**

The core authentication and authorization server supporting multiple protocols and advanced security features.

- OAuth 2.0 & OpenID Connect implementation
- Multi-factor authentication support
- Advanced session management
- Enterprise directory integration
- Comprehensive audit logging

[Learn more](https://docs.jans.io/nightly/janssen-server/index.md)

**Self-Service Portal**

User-friendly portal for credential management and self-service operations.

- 2FA device enrollment and management
- Password reset and account recovery
- Consent management
- Custom branding and localization
- Plugin ecosystem

[Explore Casa](https://docs.jans.io/nightly/casa/index.md)

**Authentication Orchestration**

Visual platform for designing and implementing complex authentication flows.

- Drag-and-drop flow designer
- Custom authentication logic
- Third-party integrations
- Real-time testing and deployment
- Version control support

[Start with Agama](https://docs.jans.io/nightly/agama/introduction/index.md)

**Policy Engine**

Cedar-based authorization engine for fine-grained access control decisions.

- Policy-as-code approach
- Real-time authorization decisions
- Audit trail and compliance
- API-first architecture
- Cloud-native deployment

[Discover Cedarling](https://docs.jans.io/nightly/cedarling/index.md)

______________________________________________________________________

## Quick Start Guide

### 1. Choose Your Deployment

Select the deployment method that best fits your environment and requirements.

```
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/startjanssendemo.sh && chmod u+x startjanssendemo.sh && ./startjanssendemo.sh
```

[Complete Kubernetes Setup Guide](https://docs.jans.io/nightly/janssen-server/install/helm-install/index.md)

**For Testing and Development Only**

```
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/start_janssen_aio_demo.sh && chmod u+x start_janssen_aio_demo.sh && sudo bash start_janssen_aio_demo.sh demoexample.jans.io MYSQL "" <VM_IP>
```

[Complete Docker Setup Guide](https://docs.jans.io/nightly/janssen-server/install/docker-install/quick-start/index.md)

**For Testing and Development Only**

```
curl https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-linux-setup/jans_setup/install.py > install.py
sudo python3 install.py
```

[Complete VM Setup Guide](https://docs.jans.io/nightly/janssen-server/install/vm-install/index.md)

### 2. Initial Configuration

Set up your identity platform with the configuration tools.

```
sudo /opt/jans/bin/jans-tui.py
```

```
sudo /opt/jans/bin/jans-cli.py
```

```
resource "jans_client" "example" {
  client_name     = "My Application"
  grant_types     = ["authorization_code"]
  redirect_uris   = ["https://myapp.com/callback"]
  response_types  = ["code"]
  scope           = "openid profile email"
}
```

### 3. Create Your First Client

Register an application using Terraform for infrastructure as code.

```
terraform {
  required_providers {
    jans = {
      source = "JanssenProject/jans"
    }
  }
}

provider "jans" {
  url = "https://your-domain"
}

resource "jans_client" "webapp" {
  client_name    = "Web Application"
  grant_types    = ["authorization_code", "refresh_token"]
  redirect_uris  = ["https://myapp.com/callback"]
  response_types = ["code"]
  scope         = "openid profile email"
}
```

### 4. Test Authentication

Verify your setup with a simple authentication flow.

```
https://your-domain/jans-auth/restv1/authorize?
  response_type=code&
  client_id=YOUR_CLIENT_ID&
  redirect_uri=YOUR_REDIRECT_URI&
  scope=openid profile
```

______________________________________________________________________

*Janssen is a Linux Foundation project that provides next-generation identity and access management, building upon years of experience in enterprise security.*
