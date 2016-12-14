All rights reserved -- Copyright 2015 Gluu Inc.

# oxd

oxd is a mediator, a service demon that listens on localhost, providing easy APIs that can be called by a web application to simplify using an external OAuth2 server for authentication or authorization. oxd is not a proxy--sometimes it makes API calls on behalf of an application, but other times it just forms the right URLs and returns them to the application.

oxd reduces OpenID Connect to five APIs: Register, Get Authorization URL, Get Tokens, Get User Info, and Logout.

There are both business and technical advantages to using oxd over the traditional model of calling federation APIs directly from within the application:

1. oxd consolidates the OAuth2 code in one package. If new vulnerabilities are discovered in OAuth2/OpenID Connect, oxd is the only component that needs to be updated. The oxd APIs remain the same, so you don’t have to change and regression test your applications;

2. oxd is written, maintained, and supported by developers who specialize in application security. Because of the complexity of the standards–and the liability associated with poor implementations–it makes sense to rely on professionals who have read the specifications in their entirety and understand how to properly implement the protocols;

3. Centralization reduces costs. By using oxd across your IT infrastructure for application security (as opposed to a handful of homegrown and third party OAuth2 implementations), the surface area for vulnerabilities, issue resolution, and support is significantly reduced. Plus you have someone to call when something goes wrong!

oxd is commercial software that is licensed by Gluu. Learn more on the [oxd website](https://oxd.gluu.org).

# Plugins

Gluu currently publishes oxd plugins, modules, and extensions for the following open source applications (more coming!):
- [Wordpress](https://oxd.gluu.org/docs/plugin/wordpress/)
- [Magento](https://oxd.gluu.org/docs/plugin/magento/)
- [Drupal](https://oxd.gluu.org/docs/plugin/drupal/)
- [OpenCart](https://oxd.gluu.org/docs/plugin/opencart/)
- [SuiteCRM](https://oxd.gluu.org/docs/plugin/suitecrm/)
- [SugarCRM](https://oxd.gluu.org/docs/plugin/sugarcrm/)
- [Roundcube](https://oxd.gluu.org/docs/plugin/roundcube/)

For a complete list of oxd plugins, check the [oxd documentations](http://oxd.gluu.org/docs)

# Libraries
oxd client libraries provide simple, flexible, powerful access to the oxd OpenID Connect and UMA authentication and authorization APIs.
- [Python](https://oxd.gluu.org/docs/libraries/python/index.md)
- [Java](https://oxd.gluu.org/docs/libraries/java/index.md)
- [Php](https://oxd.gluu.org/docs/libraries/php/index.md)
- [Node](https://oxd.gluu.org/docs/libraries/node/index.md)
- [Ruby](https://oxd.gluu.org/docs/libraries/rube/index.md)
- [C#](https://oxd.gluu.org/docs/libraries/csharp/index.md)

For a complete list of client libraries, check the [oxd documentations](http://oxd.gluu.org/docs)
