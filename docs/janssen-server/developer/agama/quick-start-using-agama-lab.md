---
tags:
  - administration
  - developer
  - agama
  - agama-lab
---

# Quick Start Using Agama Lab

Agama Lab is an online visual editor that enables you to build authentication flows without writing code. This guide will help you get started with creating your first authentication flow using Agama Lab.

## What is Agama Lab?

Agama Lab is a web-based visual flow designer that allows you to:

- Create complex authentication workflows using drag-and-drop components
- Test flows in real-time
- Deploy flows directly to your Janssen Server
- Version control your authentication flows
- Share and collaborate on flow designs

## Getting Started

### 1. Access Agama Lab

Navigate to [Agama Lab](https://agama-lab.gluu.org/) in your web browser.

### 2. Sign In

You can sign in using your GitHub account or create a new account specifically for Agama Lab.

![Agama Lab Login](../../../assets/agama-lab-login.png)

### 3. Create Your First Project

1. Click on **"New Project"** to start creating your authentication flow
2. Enter a project name (e.g., "my-first-flow")
3. Provide a description for your project
4. Click **"Create"**

![Agama Lab New Project](../../../assets/agama-lab-new-proj.png)

## Building Your First Flow

### 1. Understanding the Interface

The Agama Lab interface consists of:

- **Canvas**: The main area where you design your flow
- **Component Palette**: Available nodes and components you can drag onto the canvas
- **Properties Panel**: Configure selected components
- **Flow Navigator**: Overview of your flow structure

### 2. Basic Flow Components

Common components you'll use:

- **Start Node**: Entry point of your flow
- **Decision Node**: Conditional logic branching
- **Action Node**: Perform specific actions (authentication, data collection, etc.)
- **End Node**: Flow completion points
- **RFAC Node**: Redirect and Fetch a Callback (for external authentication)

### 3. Creating a Simple Password Flow

Let's create a basic username/password authentication flow:

1. **Add a Form Node**:
   - Drag a "Form" component from the palette
   - Configure it to collect username and password
   - Set the template to use a login form

2. **Add Validation Logic**:
   - Drag a "Decision" node after the form
   - Configure it to validate credentials
   - Connect success and failure paths

3. **Add End Nodes**:
   - Add "Success" end node for successful authentication
   - Add "Failure" end node for failed attempts

![Agama Lab Flow Creation](../../../assets/agama-lab-flow-passwd-complete-flow.png)

### 4. Configuring Components

Each component has configurable properties:

- **Form Components**: Template selection, field definitions, validation rules
- **Decision Components**: Conditional logic, variable comparisons
- **Action Components**: Script execution, external service calls
- **RFAC Components**: External authentication provider configuration

## Testing Your Flow

### 1. Flow Validation

Before testing, ensure your flow:
- Has a clear start point
- All paths lead to end nodes
- Required properties are configured
- No orphaned components exist

### 2. Real-time Testing

Agama Lab provides built-in testing capabilities:

1. Click the **"Test"** button in the toolbar
2. Follow the flow execution step by step
3. Verify each component behaves as expected
4. Debug any issues using the flow debugger

### 3. Flow Debugging

Use the debugging features to:
- Set breakpoints at specific nodes
- Inspect variable values during execution
- Step through the flow logic
- Identify and fix issues

## Deploying Your Flow

### 1. Generate .gama Package

Once your flow is complete and tested:

1. Click **"Export"** in the toolbar
2. Select **"Generate .gama Package"**
3. Download the generated package file

### 2. Deploy to Janssen Server

Deploy your flow to Janssen Server using one of these methods:

#### Using TUI (Text-based UI)

1. Access Janssen TUI: `sudo /opt/jans/bin/jans-tui.py`
2. Navigate to `Auth Server` > `Agama`
3. Select `Upload Project`
4. Browse and select your .gama file
5. Deploy the project

#### Using CLI

```bash
# Upload the .gama package
sudo /opt/jans/bin/jans-cli.py --operation-id post-config-scripts-agama \
  --data /path/to/your-flow.gama

# Enable the flow
sudo /opt/jans/bin/jans-cli.py --operation-id put-config-scripts-agama-deployment \
  --url-suffix "your-flow-name" \
  --data '{"enabled": true}'
```

### 3. Configure Authentication Method

After deployment, configure your flow as an authentication method:

1. In TUI, navigate to `Auth Server` > `Authentication Methods`
2. Add your Agama flow as a new authentication method
3. Set the ACR (Authentication Context Class Reference) value
4. Configure any required parameters

## Advanced Features

### 1. External Authentication Integration

Use RFAC (Redirect and Fetch a Callback) nodes to integrate with external identity providers:

- Social login providers (Google, Facebook, etc.)
- Enterprise identity providers (SAML, OIDC)
- Custom authentication services

### 2. Custom Scripts Integration

Integrate custom Jython scripts within your flows:

- Data transformation
- External API calls
- Complex business logic
- Integration with existing systems

### 3. Multi-step Authentication

Create complex multi-factor authentication flows:

- Progressive profiling
- Risk-based authentication
- Adaptive authentication
- Step-up authentication

### 4. Flow Versioning

Manage different versions of your flows:

- Create branches for different environments
- Tag stable releases
- Roll back to previous versions
- Compare flow versions

## Best Practices

### 1. Flow Design

- Keep flows simple and intuitive
- Use clear naming conventions
- Document complex logic
- Test thoroughly before deployment

### 2. Error Handling

- Always provide error paths
- Include meaningful error messages
- Log important events for debugging
- Gracefully handle edge cases

### 3. Security Considerations

- Validate all user inputs
- Implement proper session management
- Use secure communication channels
- Follow authentication best practices

### 4. Performance Optimization

- Minimize external API calls
- Cache frequently used data
- Optimize flow logic
- Monitor flow performance

## Troubleshooting

### Common Issues

1. **Flow Not Loading**: Check .gama package format and deployment status
2. **Authentication Failures**: Verify flow logic and error handling
3. **Integration Issues**: Check external service configurations
4. **Performance Problems**: Review flow complexity and external dependencies

### Getting Help

- [Agama Documentation](../../agama/introduction.md)
- [Janssen Community Forum](https://github.com/JanssenProject/jans/discussions)
- [Agama Lab Support](https://agama-lab.gluu.org/support)

## Next Steps

After mastering the basics:

1. Explore advanced Agama features
2. Create custom templates and components
3. Integrate with enterprise systems
4. Contribute to the Agama community

## See Also

- [Agama Language Reference](../../agama/language-reference.md)
- [Agama Project Configuration](../../config-guide/auth-server-config/agama-project-configuration.md)
- [Authentication Method Configuration](../../config-guide/auth-server-config/authentication-method-config.md)
- [Custom Scripts](../scripts/README.md)