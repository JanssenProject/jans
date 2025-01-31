---
tags:
 - administration
 - developer
 - customization
 - internationalization 
 - i18n
 - locale
 - css
 - images
 - header
 - footer
 - template
---

# Customization Using Custom Assets

Janssen Server allows customization and extension of current web page designs,
images, web templates etc. For 
example, extending the list of languages supported by Janssen Server or maybe
customizing the login page to show the organization's logo and align styling to 
meet the branding of the organization. 

All of the above need some amount of custom assets, like custom CSS stylesheets,
logo images, etc to be available on the Janssen Server. All these are called
`Custom Assets`.

## Custom Assets Accepted by Module

Listing of custom asset types that each Janssen Server module accepts:


### Config-API

| Path | Asset Type |
|----------------|-------------|
| /opt/jans/jetty/jans-config-api/custom/config	| *.xml
| /opt/jans/jetty/jans-config-api/custom/i18n	| *.properties
| /opt/jans/jetty/jans-config-api/custom/libs		| *.jar
| /opt/jans/jetty/jans-config-api/custom/pages	| *.xhtml
| /opt/jans/jetty/jans-config-api/custom/static | *.js, *.css, *.png, *.gif, *.jpg, *.jpeg

## Auth-Server



| Path | Asset Type |
|----------------|-------------|
| /opt/jans/jetty/jans-auth/custom/i18n	| *.properties
| /opt/jans/jetty/jans-auth/custom/libs	| *.jars
| /opt/jans/jetty/jans-auth/custom/pages| *.xhtml
| /opt/jans/jetty/jans-auth/custom/static	| *.js, *.css, *.png, *.gif, *.jpg, *.jpeg
| /etc/certs/jans-auth-keys.pkcs12	 | *.pkcs12

### Casa

| Path | Asset Type |
|----------------|-------------|
| /opt/jans/jetty/jans-casa/plugins		| *.jar
| /opt/jans/jetty/jans-casa/static		| *.js, *.css, *.png, *.gif, *.jpg, *.jpeg

### Agama

| Path | Asset Type |
|----------------|-------------|
| /opt/jans/jetty/jans-auth/agama		| engine templates
| /opt/jans/jetty/jans-auth/agama/fl		| *.js, *.css, *.png, *.gif, *.jpg, *.jpeg
| /opt/jans/jetty/jans-auth/agama/ftl | *.ftl, *.ftlh
| /opt/jans/jetty/jans-auth/agama/scripts | *.java *.groovy, *.gvy, *.gy, *.gsh

### Fido2

| Path | Asset Type |
|----------------|-------------|
| /etc/jans/conf/fido2/authenticator_cert| *.pem, *.crt
| /etc/jans/conf/fido2/mds/cert	| *.crt
| /etc/jans/conf/fido2/mds/toc | *.jwt
| /etc/jans/conf/fido2/server_metadata | *.json

### Lock

| Path | Asset Type |
|----------------|-------------|
| /opt/jans/jetty/jans-lock/custom/libs	| *.jar


### KeyCloak-link

| Path | Asset Type |
|----------------|-------------|
| /opt/jans/jetty/jans-keycloak-link/custom/libs/| *.jar
| /var/jans/keycloak-link-snapshots/	| *.txt


### Link

| Path | Asset Type |
|----------------|-------------|
| /opt/jans/jetty/jans-link/custom/libs	| *.jar
| /var/jans/link-snapshots/	| *.txt


## Managing Custom Assets

Janssen Server configuration tools like CLI and TUI provide the ability to add, 
update, and delete custom assets. Refer to the 
[custom assets configuration guide](../../config-guide/custom-assets-configuration.md)
to learn how to manage custom assets.

 
## Customizing Web Pages

Janssen Server uses [xhtml pages](https://github.com/JanssenProject/jans/tree/main/jans-auth-server/server/src/main/webapp) to render the web interface needed in 
interactive web-flows. For example, password authentication flow. 

It is possible to override the built-in `xhtml` pages or to add completely 
new pages.

### Customizing Built-in Web Pages

In order to customize built-in web pages, follow the steps below:

- Create a new `xhtml` page as a copy of the relevant built-in xhtml page
- Make changes to the code of the new page according to the need 
- Override the built-in page with the new page

To override the built-in page with the new page, add the new page as 
a [custom asset](#managing-custom-assets) 
and make sure that the 
new page has the same name and extension as the built-in page. 
The Janssen Server will automatically use the custom page instead of the 
built-in page. 

For example, the default login web page is rendered using 
[login.xhtml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/login.xhtml).
To override this page, add a new custom `login.xhtml`. Same can be done 
for other built-in pages like [authorization page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/authorize.xhtml), [logout page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/logout.xhtml), [error page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/error.xhtml).


### Adding New Web Pages

If the authentication/authorization flow requires new web pages, the same can be 
added as [custom asset](../../config-guide/custom-assets-configuration.md) and
then can be referenced using a relative path.

For instance, if `enterOTP.xhtml` is your webpage for step 2 of 
the authentication flow that is being implemented using a custom script,
then upload page as a custom asset under the relevant Janssen service and then 
reference it in the custom script as follows:

```
 def getPageForStep(self, configurationAttributes, step):
 # Used to specify the page you want to return for a given step
 if (step == 1):
 return "/auth/login.xhtml"  
 if (step == 2)
 return "/auth/enterOTP.xhtml"
```
#### Reference login pages:

[Here](https://github.com/JanssenProject/jans/tree/main/jans-auth-server/server/src/main/webapp/auth) you will find several login pages for different authentication methods.


#### Customized resource bundles:

Janssen Server provides language translation support using a set of 
[built-in resource bundles](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/resources/).

To override the defaults, the custom `.properties` files should be uploaded
as [custom assets](#managing-custom-assets). If the file name matches the 
existing resource bundle, then the custom bundle will override the built-in
resource bundle. 

Examples of file names are:

 * jans-auth_en.properties
 * jans-auth_bg.properties


To add translations for a language that is not yet supported, create a new 
properties file and name it 
jans-auth_[language_code].properties. Then add it to the Janssen Server as
[custom asset](#managing-custom-assets). Janssen Server will automatically
add the support for new language using the new resource bundle.


#### Custom CSS files:

Upload the custom CSS files as [custom assets](#managing-custom-assets) and then
reference it in `.xhtml` file using the URL `https://your.jans.server/jans-auth/ext/resources/stylesheet/theme.css` or `/jans-auth/ext/resources/stylesheet/theme.css`

#### Custom image files:
Upload the custom image files as [custom assets](#managing-custom-assets) and 
then reference it in `.xhtml` file using the URL 
`https://your.jans.server/jans-auth/ext/resources/img/fileName.png` or 
`/jans-auth/ext/resources/img/fileName.jpg`.

#### Page layout, header, footer (xhtml Template) customization

Templates refer to the common interface layout and style. For example, 
a banner, logo in the common header, and copyright information in the footer.

Upload the custom template `template.xhtml` file as 
[custom assets](#managing-custom-assets).This file will automatically override 
the built-in 
[default template file](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/WEB-INF/incl/layout/template.xhtml).

 
