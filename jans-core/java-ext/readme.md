To enable it you need to do:

    1. Put this library into /opt/jre/jre/lib/ext
    2. Check if inside faces-config.xml there is no <message-bundle> since we expects to use default javax.faces.Messages.properties bundle name
    3. Create new folder and put into /opt/jans/jetty/jans-server/custom/i18n/javax/faces it Messages.* from oxauth.war!/javax.faces-2.3.9.jar!/javax/faces/Messages.properties
    4. Restart jans-server service
