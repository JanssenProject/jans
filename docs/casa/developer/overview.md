---
tags:
  - Casa
  - developer guide
---

# Developer guide

This page gives developers relevant pointers for several tasks including:

- Plugins development
- Configuration management
- Credentials enrollment
- Customization of Casa's authentication flow

## Plugins

A plugin is an artifact packaged in a Java ARchive (_jar_ file) that augments the functionalities available in your default Casa installation. The following is by no means an extensive list of things you can add via plugins:

- Menu items in user's menu, top-right dropdown menu, or admin's dashboard menu
- UI pages with arbitrary content (and backend-functionality!), this also applies for the admin dashboard
- Static files (e.g. Javascript, images, stylesheets, etc.)
- REST services
- Authentication mechanisms to be supported by the application

In addition to the above:

- Any plugin can have easy access to the underlying Jans Server database
- Plugins can onboard their own libraries (jar files) and classes

### Tools

Acquaintance with the following technologies is recommended:

- Java 11 or higher, maven
- ZK 9 framework
- PF4J framework
- HTML/CSS/Javascript
- The underlying database engine used by your Jans Server installation, e.g. PostgreSQL, MySQL, etc.

### Sample plugins

The best way to start learning Casa plugin development is by playing with the sample plugins you can find [here](
https://github.com/JanssenProject/jans/tree/vreplace-janssen-version/jans-casa/plugins/samples). Clone the repository (a shallow clone of `main` branch is fine), `cd` to one of the directories in the folder and run `mvn package`, then upload the resulting `jar-with-dependencies` through the administration console.

## Configuration management

Most aspects of Casa that are configurable through the admin console UI can be programmatically operated using the configuration API. A formal description  can be found [here](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/app/src/main/webapp/admin-api.yaml). Note all endpoints are protected by OAuth tokens which must have the `https://jans.io/casa.config` scope.

## Credentials enrollment

Casa has enrollment capabilities built-in but there are use cases where credential enrollment needs to happen elsewhere in your app ecosystem. A typical scenario is in a user registration application, where users are asked to enroll strong authentication credentials during account creation. To facilitate these tasks, Casa exposes [APIs](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/app/src/main/webapp/enrollment-api.yaml)  for enrolling the following types of authenticators:   

- Phone numbers for SMS OTP
- OTP apps or tokens  
- FIDO2 security keys

!!! Note
    Per spec FIDO 2 credentials can only be enrolled from a page belonging to the same domain or subdomain of your Gluu Server.

In addition to the above, the API also provides endpoints to query the number/type of credentials currently enrolled by a user as well as means to turn 2FA on and off.

## Customizing the authentication flow

The authentication experience the user faces when trying to access Casa is implemented in an [Agama](../../agama/introduction.md) project which is attached to the authentication server when Casa is installed.  As with all authentication flows in Janssen, they belong to (run in the context of) the `jans-auth` application. This distinction is important because `jans-auth` and `casa` are separate Java webapps executed on different Jetty instances.

In Casa flow, the user is requested to enter the username and password combination, then depending on how the application is configured, personal user settings, and access policies defined, a second factor may be requested.

While this may cater most companies requirements, sometimes there is the need to customize the authentication experience. In fact, Agama facilitates this by design. Here are some things companies would like to do:

- Alter the UI pages - e.g. look-and-feel, structure, etc. 
- Support more authentication methods
- Add links to the initial screen to take users to different authentication paths, for instance, to leverage social sites login
- Include an account registration process
- Include an extra final screen in case of password expiration
- Add a "forgot password" link
<!--
- Remove the password prompt (for passwordless authentication)
-->

!!! Important
    You might be tempted to take the Agama project archive, apply some editions, add files to it, repack, and redeploy it. While this might seem the easiest thing to do, it is also the worst thing to do. Authentication journeys can be extended/tailored by means of creating additional projects. Try not to hack/patch the original project.

### Casa ACR update

Given the warning above, there has to be a way to launch a different Agama flow than the one used by default to log into Casa. This can be achieved by supplying a value for Casa's `acr` startup variable: in VM-based installations, locate the file `/etc/default/casa` in and modify the variable as needed. The format is `agama_<qualified-name-of-your-flow>`. Then restart Casa.  

### Requisites

Regardless of the customization required, it is desirable to get acquaintance with Agama [framework](../../agama/introduction.md). This is a good time to go through the Agama developer guide pages found in the  Administration section of Jans Server docs. Specifically, several of the Agama [advanced usages](../../janssen-server/developer/agama/advanced-usages.md#advanced-usages) will help you materialize your requirements.

Extract [the Agama project](https://maven.jans.io/maven/io/jans/casa-agama/replace-janssen-version/casa-agama-vreplace-janssen-version-project.zip) to your development machine. It is useful to get an idea of how and what the out-of-the-box project does. Also, keep the [Freemarker](https://freemarker.apache.org/docs/index.html) manual at hand.

### Page customizations

The UI pages of the default Casa flow resemble the design of the Casa app itself. Also, modifications applied through the "custom branding" functionalities are automatically reflected in flow pages without any sort of intervention. This is neat, but if you need to go further, you will have to code the UI pages your own based on the existing ones.

For this purpose, create a new Agama project with one flow in it. Pick one of the pages you want to change from the original project and build your own - initially keep it really simple: a dummy page is OK. From your new flow, use the `Trigger` directive to launch flow `io.jans.casa.authn.main`. Add an `Override templates` directive to your `Trigger` so the page in Casa project is superseded by the page you are creating. This is explained [here](../../janssen-server/developer/agama/advanced-usages.md#template-overrides).

Pack your new project and deploy it. Wait for around 30 seconds and try to log into Casa to see the changes. Note you have to configure casa so your flow is launched, not the default one, ie. `io.jans.casa.authn.main`. This was explained [earlier](#casa-acr-update).

Do as many changes as needed to your page. Then pick another page to alter and feed your `Override templates` accordingly. Repeat until your are done. Recall there is no need to restart `jans-auth` or `casa`.

In some cases, the original look-and-feel may be satisfying but it's the text content what you would like to change. Agama engine supports localization and internationalization as explained [here](../../janssen-server/developer/agama/advanced-usages.md#localization-and-internationalization) so you can supply  translated messages in your own project and make templates use those. Note Casa is only bundled with a set of "default" labels out-of-the box and thus pages don't change content regardless of browser's language or location. By overriding templates and providing labels in several languages, you can achieve full localization/internationalization in the authentication UI.

### Support more authentication methods

This is probably the most common requirement. Visit this [page](./add-authn-methods.md) to learn more.

### Other forms of customization

Most forms of customization can be tackled using flow cancellation. Through cancellation, a flow can be aborted while running and the control returned to one of its callers.  Learn more about this topic [here](../../janssen-server/developer/agama/advanced-usages#cancellation).

As an example, let's assume you want to add a _"don't have an account? register here"_ button in the initial screen of Casa flow. Here's what you can do:

- Create a new Agama project with one flow in it. Copy the page that prompts username and password into your project. Below the login button, add a form containing markup like `<button type="submit" name="_abort">don't have an account? register here</button>`

- In your flow, let's call it `A`, use the `Trigger` directive to launch flow `io.jans.casa.authn.main`. Add an `Override templates` directive to your `Trigger` so the original login page in Casa project is superseded by the page you created

- Create another flow, let's call it `B`. This will be the "registration" flow which will present one or more screens to grab data in order to create an account for the user. Make `B` finish with `true` outcome if the account was successfully created, otherwise finish with `false`

- Back in flow `A`, add code to check if `io.jans.casa.authn.main` was aborted, if so, launch flow `B`. For simplicity, if `B` didn't go well, terminate `A`

- If `B` went well, show the original login form, that is, no registration button this time

- Add code for termination 


So flow `A` will end up looking more or less like this:

```
...
result = Trigger io.jans.casa.authn.main
    Override templates "kz1vc3/main.ftlh" "newPage.ftlh"
    
When result.aborted is true
    //registration button was clicked
    result = Trigger B
        
    When result.success is false
        Finish false
    
    result = Trigger io.jans.casa.authn.main

Finish result
```

Here we have taken a simplistic approach to make the example concise. In practice you may like to be more elaborated and include an _"already have an account?"_ button in the registration page, which could create a sort of navigational loop. Here the `Repeat` directive will emerge in your code for sure.

!!! Note
    When trying to customize an existing flow, familiarize with all possible paths the flow can take you to. Consider all possible screens and determine which require adjustments and the points at which cancellation is of use. Sometimes you will have to glance its `.flow` file (Agama code) to get a better idea.
