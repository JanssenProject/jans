#!/bin/bash

if [ -n "$1" ]; then 
   VER=$1
else
   echo "please specify version"
   exit 1
fi

if [ -n "$2" ]; then
   SOURCE=$2
else
   echo "please specify source"
   exit 2
fi

touch oxd-server.log
wget https://raw.githubusercontent.com/GluuFederation/oxd/$VER/oxd-server/src/main/bin/oxd-start.sh
wget https://raw.githubusercontent.com/GluuFederation/oxd/$VER/oxd-server/src/main/bin/lsox.sh 
wget https://github.com/GluuFederation/oxd/raw/$VER/oxd-server/src/main/resources/oxd-server.keystore
wget https://raw.githubusercontent.com/GluuFederation/oxd/$VER/oxd-server/src/main/resources/oxd-server.yml
wget https://raw.githubusercontent.com/GluuFederation/oxd/$VER/oxd-server/src/main/resources/swagger.yaml
wget https://github.com/GluuFederation/oxd/raw/$VER/docker/bcprov-jdk15on-1.54.jar
wget https://ox.gluu.org/maven/org/xdi/oxd-server/$SOURCE/oxd-server-$SOURCE.jar
wget https://raw.githubusercontent.com/GluuFederation/oxd/$VER/docker/config.sh
wget https://raw.githubusercontent.com/GluuFederation/oxd/$VER/docker/config_template.yml

