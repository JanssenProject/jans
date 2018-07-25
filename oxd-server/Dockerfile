FROM openjdk:8-jre
LABEL maintainer="Michal Kepkowski"

COPY target/oxd-server.jar /oxd-server.jar
COPY config/config_template.yml /config_template.yml
ADD config/config_gen.sh /config_gen.sh

RUN apt-get -qqy update && apt-get -qqy install gettext-base

ENTRYPOINT ["/config_gen.sh"]
EXPOSE 8443 8444

