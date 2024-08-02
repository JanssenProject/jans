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

Janssen Server allows customization and extension of current features. For 
example, extending the list of languages support by Janssen Server or may be
customizing the login page to show organizations logo and align styling to meet
the branding of the organization. 

All of above need some amount of custom assets, like custom CSS stylesheets,
logo image, etc to be available on the Janssen Server. All these are called
`Custom Assets`.

## Managing Custom Assets

Janssen Server configuration tools like CLI and TUI provide ability to add, 
update and delete custom assets. Refer to the 
[custom assets configuration guide](../../config-guide/custom-assets-configuration.md)
to learn how to manage custom assets.

 

#### Directory structure for customization



| Directory                      | Asset Type                      | Description                         |
|--------------------------------|-------------------------------------|-------------------------------------|
| /opt/jans/jetty/`<service-name>`/custom/i18n   | properties                         | Resource bundle file                |
| /opt/jans/jetty/`<service-name>`/custom/libs   | lib                                | java archive library              |
| /opt/jans/jetty/`<service-name>`/custom/pages | xhtml                              | Web pages                           |
| /opt/jans/jetty/`<service-name>`/custom/static | js, css, png, gif , jpg, jpeg | Static resources like Java-script, style-sheet and images |

## Customizing Web Pages

Janssen Server uses [xhtml pages](https://github.com/JanssenProject/jans/tree/main/jans-auth-server/server/src/main/webapp) to render the web interface needed in 
interactive web-flows, for example, password authentication flow. 

### Customizing Built-in Web Pages

In order to customize built-in web pages, follow the steps below:

- Create a copy of the relevant built-in xhtml page
- Change according to the need 
- Override the built-in page with the new page

To override the built-in page by the new page, add the new page as 
a [custom asset](../../config-guide/custom-assets-configuration.md) 
and make sure that the 
new page has the same name and extension as the built-in page. 
The Janssen Server will automatically use the custom page instead of the 
built-in page. For example, the default login web-page is rendered using 
[login.xhtml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/login.xhtml), just add a new custom `login.xhtml`. Same can be done 
for other built-in pages like [authorization page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/authorize.xhtml), [logout page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/logout.xhtml), [error page](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/error.xhtml).


### Adding New Web Pages

If the authentication/authorization flow requires new web pages, same can be 
added as [custom asset](../../config-guide/custom-assets-configuration.md) and
then can be referenced using relative path.

For instance, if `enterOTP.xhtml` is your webpage for step 2 of authentication, 
then upload it as a custom asset under relevant Janssen service and then 
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

<!-- 

#### Customized resource bundles:
1. Resource bundles that are present in the jans-auth.war are present in this [folder](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/resources/)

2. To override the defaults, custom `.properties` files should be placed in the following file under this path : `/opt/jans/jetty/jans-auth/custom/i18n/jans-auth.properties`
Resource bundle names to support other languages should be placed under the same folder `/opt/jans/jetty/jans-auth/custom/i18n/`. Some examples of file names are :
    * jans-auth_en.properties
    * jans-auth_bg.properties
    * jans-auth_de.properties
    * jans-auth_es.properties
    * jans-auth_fr.properties
    * jans-auth_it.properties
    * jans-auth_ru.properties
    * jans-auth_tr.properties

3. To add translation for a language that is not yet supported, create new properties file in resource folder and name it jans-auth_[language_code].properties, then add language code as supported-locale to the [faces-config.xml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/resources/faces-config.xml) present in the same folder.

-->

#### Custom CSS files:

1. Place the file in `/opt/jans/jetty/jans-auth/custom/static/stylesheet/theme.css`
2. Reference it in .xhtml file using the URL `https://your.jans.server/jans-auth/ext/resources/stylesheet/theme.css` or `/jans-auth/ext/resources/stylesheet/theme.css`

#### Custom image files:
1. All images should be placed under `/opt/jans/jetty/jans-auth/custom/static/img`
2. Reference it in .xhtml file using the URL `https://your.jans.server/jans-auth/ext/resources/img/fileName.png` or `/jans-auth/ext/resources/img/fileName.jpg`

#### Page layout, header, footer (xhtml Template) customization

Templates refers to the common interface layout and style. For example, a banner, logo in common header and copyright information in footer.

1. `mkdir -p /opt/jans/jetty/jans-auth/custom/pages/WEB-INF/incl/layout/`    
2. Place a modified `template.xhtml` in the above location which will override the [default template file](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/WEB-INF/incl/layout/template.xhtml) from the war

 