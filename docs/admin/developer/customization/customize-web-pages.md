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

All web pages are **xhtml** files.

### Default pages bundled in the `jans-auth.war` are:
* Login page: [login.xhtml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/login.xhtml)
* Authorization page: [authorize.xhtml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/authorize.xhtml)
* Logout page: [logout.xhtml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/logout.xhtml)
* Error page: [error.xhtml](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/error.xhtml)

### To override default pages listed above:
Put a modified `login.xhtml` or `authorize.xhtml` or `error.xhtml` or `logout.xhtml` under `/opt/jans/jetty/jans-auth/custom/pages/`

### Directory structure for customization
```
/opt/jans/jetty/jans-auth/
|-- custom
|   |-- i18n (resource bundles)
|   |-- libs (library files used by custom script)
|   |-- pages (web pages)
|   |-- static (images and css files)
```
### Adding a new web page for Person Authentication scripts
1. If `enterOTP.xhtml` is your webpage for step 2 of authentication, place under `/opt/jans/jetty/jans-auth/custom/pages/enterOTP.xhtml`
2. Reference it in the custom script as follows:
```
    def getPageForStep(self, configurationAttributes, step):
        # Used to specify the page you want to return for a given step
        if (step == 1):
          return "/auth/login.xhtml"
        if (step == 2)
          return "/auth/enterOTP.xhtml"
```


### Customized resource bundles:
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

### Custom CSS files:

1. Place the file in `/opt/jans/jetty/jans-auth/custom/static/stylesheet/theme.css`
2. Reference it in .xhtml file using the URL `https://your.jans.server/jans-auth/ext/resources/stylesheet/theme.css` or `/jans-auth/ext/resources/stylesheet/theme.css`

### Custom image files:
1. All images should be placed under `/opt/jans/jetty/jans-auth/custom/static/img`
2. Reference it in .xhtml file using the URL `https://your.jans.server/jans-auth/ext/resources/img/fileName.png` or `/jans-auth/ext/resources/img/fileName.jpg`

### Page layout, header, footer (xhtml Template) customization

Templates refers to the common interface layout and style. For example, a same banner, logo in common header and copyright information in footer.

1. `mkdir -p /opt/jans/jetty/jans-auth/custom/pages/WEB-INF/incl/layout/`    
2. Place a modified `template.xhtml` in the above location which will override the [default template file](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/webapp/WEB-INF/incl/layout/template.xhtml) from the war

### Example pages:

[Here](https://github.com/JanssenProject/jans/tree/main/jans-auth-server/server/src/main/webapp/auth) you will find several login pages for different authentication methods.
