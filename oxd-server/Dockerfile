FROM openjdk:8-jre
MAINTAINER Michal Kepkowski

COPY target/oxd-server.jar /oxd-server.jar
COPY config/config_template.yml /config_template.yml
ADD config/config_gen.sh /config_gen.sh

ENTRYPOINT ["/config_gen.sh"]
CMD ["java", "-jar", "/oxd-server.jar","server","/config.yml"]
EXPOSE 8443 8444

