FROM java:8
MAINTAINER Davit Nikoghosyan <davit@gluu.org>
ADD oxd-start.sh /opt/oxd-server/bin/
ADD lsox.sh /opt/oxd-server/bin/
ADD oxd-server.yml /opt/oxd-server/conf/
ADD oxd-server.keystore /opt/oxd-server/conf/
ADD swagger.yaml /opt/oxd-server/conf/
ADD bcprov-jdk15on-1.54.jar /opt/oxd-server/lib/		
ADD oxd-server.jar /opt/oxd-server/lib/
ADD oxd-server.log /var/log/oxd-server/oxd-server.log
ADD config.sh /config.sh
ADD config_template.yml /config_template.yml
EXPOSE 8444
EXPOSE 8443
ENTRYPOINT ["/config.sh"]
