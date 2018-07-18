#!/bin/bash
apt-get update
apt-get install gettext-base -y

#--- SETTING DEFAULT VALUES ---
export SUPPORT_GOOGLE_LOGOUT=${SUPPORT_GOOGLE_LOGOUT:=true}
export USE_CLIENT_AUTHENTICATION_FOR_PAT=${USE_CLIENT_AUTHENTICATION_FOR_PAT:=true}
export TRUST_ALL_CERTS=${TRUST_ALL_CERTS:=true}
export $KEYSTORE_VALIDATE_CERT=${$KEYSTORE_VALIDATE_CERT:=false}

envsubst < /config_template.yml > /config.yml

java -jar /oxd-server.jar server /config.yml