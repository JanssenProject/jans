---
tags:
  - administration
  - config-api
---

## Overview
[Jans Config Api](https://github.com/JanssenProject/jans/tree/main/jans-config-api) is a REST application that is developed using Weld 4.x (JSR-365) and JAX-RS. Its endpoint can be used to manage configuration and other properties of [Jans Auth Server](https://github.com/JanssenProject/jans/tree/main/jans-auth-server).

## Jans Config API Plugins
Jans Config API follow a flexible plugin architecture in which the new features can be added using extensions called plugins without altering the application itself. In this section, we will discuss the steps to develop and add plugins in Jans Config API.

The plugin architecture implemented in Jans Config API allows the deployer to add/remove new rest APIs (plugin) without changing the core application. 
A plugin contains one or more Rest API(s) packaged in a Java ARchive (jar file). It is added to Jans Config API by adding the plugin jar file path in the external **extraClasspath** of the jetty context file.

## Pre-requisites

The plugin developer should have an understanding of the following:

- **Jakarta EE Platform**: The plugin developer should know the Java programming language
- **Weld**: Weld is the reference implementation of CDI: Contexts and Dependency Injection for the Java EE Platform
- **JAX-RS**: JAX-RS for creating RESTful web services.
- **Maven**: Maven to build projects and manage dependencies. 
- **Web application container (Jetty)**

## Sample plugin

To help bootstrap the plugin development, we have put together a sample plugin.

1. Clone the Jans Config API project from [here](https://github.com/JanssenProject/jans/tree/main/jans-config-api).
2. Navigate under the plugins/sample folder.
3. This folder contains the sample plugins for reference.
4. `helloworld` folder for example contains the code for a basic plugin with GET endpoint sending `Hello World!` string response.
5. Take the time to explore `helloworld` which is a minimalistic plugin that showcases very basic aspects of plugin development.

### Exploring Hello World Plugin

#### beans.xml

The `resources/META-INF/beans.xml` is the CDI deployment descriptor required in `bean archive`. Deployment descriptor helps WELD to explore beans, interceptors, decorators, etc in the `bean archive`. Refer to the [WELD docs](https://docs.jboss.org/weld/reference/latest/en-US/html/ee.html#packaging-and-deployment) to learn more about deployment descriptors.

#### HelloWorldExtension.java

A CDI portable extension is a mechanism by which we can implement additional functionalities on top of the CDI container. In this sample plugin, we have created an extension called `HelloWorldExtension.java`, implementing `jakarta.enterprise.inject.spi.Extension`. An extension can observe lifecycle events and also can modify the container’s metamodel. Please refer to the [WELD documentation](https://docs.jboss.org/weld/reference/latest/en-US/html/extend.html#extend) for details.

We need to register our extension as a service provider by creating a file named `resources/META-INF/services/jakarta.enterprise.inject.spi.Extension` (as shown below).
> com.spl.plugin.helloworld.ext.HelloWorldExtension

#### HelloWorldApplication.java

The `com.spl.plugin.helloworld.rest.HelloWorldApplication.java` class is annotated with the `@ApplicationPath` annotation which identifies the application path that serves as the base URI for all resources of the plugin and is used to register JAX-RS resources and providers.

#### HelloWorldResource.java 

This is a sample JAX-RS resource with an endpoint returning `Hello World!` string as http response.

## Plugin Deployment

Jans Config API is offered as one of the several components of the Jans Auth Server. A plugin jar can be added to Jans Config API by following below steps.

1. On an installed Jans Auth Server with Jans Config API (as component) copy plugin jar to `/opt/jans/jetty/jans-config-api/custom/libs` location.
2. Add the location of plugin jar inside tag with name **extraClasspath** (multiple plugins can be added comma separated) of `/opt/jans/jetty/jans-config-api/webapps/jans-config-api.xml` file.

```
<?xml version="1.0"  encoding="ISO-8859-1"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_0.dtd">

<Configure class="org.eclipse.jetty.webapp.WebAppContext">
      <Set name="contextPath">/jans-config-api</Set>
      <Set name="war">
              <Property name="jetty.webapps" default="." />/jans-config-api.war
      </Set>
      <Set name="extractWAR">true</Set>

      <Set name="extraClasspath">/opt/jans/jetty/jans-config-api/custom/libs/helloWorldjar</Set>

</Configure>
```

3. Restart `jans-config-api` service.

```
systemctl restart jans-config-api.service
```
